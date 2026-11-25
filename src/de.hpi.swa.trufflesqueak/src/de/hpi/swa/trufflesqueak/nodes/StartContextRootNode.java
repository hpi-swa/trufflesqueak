/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.profiles.BranchProfile;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.exceptions.Returns.CannotReturnToTarget;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.interrupts.CheckForInterruptsNode.CheckForInterruptsQuickNode;
import de.hpi.swa.trufflesqueak.shared.SqueakLanguageConfig;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.MiscUtils;

@NodeInfo(language = SqueakLanguageConfig.ID)
public final class StartContextRootNode extends AbstractRootNode {
    private final BranchProfile getOrCreateContextProfile = BranchProfile.create();

    private final int initialPC;
    @CompilationFinal private int initialSP;
    @CompilationFinal private byte numArgs;
    @CompilationFinal private Assumption doesNotNeedThisContext;

    @Child private CheckForInterruptsQuickNode interruptHandlerNode;

    public StartContextRootNode(final SqueakImageContext image, final CompiledCodeObject code) {
        super(image, code);
        initialPC = code.getStartPCZeroBased();
        interruptHandlerNode = CheckForInterruptsQuickNode.createForSend(code);
    }

    public StartContextRootNode(final StartContextRootNode original) {
        super(original);
        initialPC = original.initialPC;
        initialSP = original.initialSP;
        numArgs = original.numArgs;
        doesNotNeedThisContext = original.doesNotNeedThisContext;
        interruptHandlerNode = original.interruptHandlerNode; // Stateless singleton
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        ensureInitialized(frame);
        initializeFrame(frame);
        final SqueakImageContext image = SqueakImageContext.get(this);
        final boolean inInterpreterOrCompilationRoot = CompilerDirectives.inInterpreter() || CompilerDirectives.inCompilationRoot();
        try {
            if (inInterpreterOrCompilationRoot) {
                if (image.enteringContextExceedsDepth()) {
                    CompilerDirectives.transferToInterpreter();
                    // Suspend current context and throw ProcessSwitch to unwind Java stack and resume
                    final ContextObject activeContext = FrameAccess.getOrCreateContextWithFrame(frame);
                    AbstractPointersObjectWriteNode.executeUncached(image.getActiveProcessSlow(), PROCESS.SUSPENDED_CONTEXT, activeContext);
                    throw ProcessSwitch.SINGLETON;
                } else {
                    /*
                     * Only check for interrupts when entering a non-inlined activation. The check
                     * reads `shouldTrigger` with opaque semantics, which Graal models as a kill of
                     * all memory locations. Doing this in every inlined activation prevents read
                     * elimination across inlined calls (e.g., in recursive code). Interrupts are
                     * still detected promptly: every compilation root performs the check, loops
                     * check on back-edges, and recursion always reaches a non-inlined call.
                     */
                    interruptHandlerNode.execute(frame, image);
                }
            }
            return interpreterNode.execute(frame, initialPC, initialSP);
        } catch (final NonVirtualReturn | ProcessSwitch | CannotReturnToTarget nvr) {
            getOrCreateContextProfile.enter();
            FrameAccess.getOrCreateContextWithFrame(frame);
            throw nvr;
        } finally {
            if (inInterpreterOrCompilationRoot) {
                image.exitingContext();
            }
        }
    }

    private void ensureInitialized(final VirtualFrame frame) {
        if (doesNotNeedThisContext == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            numArgs = MiscUtils.toByteExact(FrameAccess.getNumArguments(frame));
            final CompiledCodeObject code = getCode();
            doesNotNeedThisContext = code.getDoesNotNeedThisContextAssumption();
            if (!FrameAccess.hasClosure(frame)) {
                initialSP = code.getNumTemps();
                assert numArgs == code.getNumArgs();
            } else {
                final BlockClosureObject closure = FrameAccess.getClosure(frame);
                initialSP = closure.getInitialSP();
                assert numArgs == closure.getNumArgs() + closure.getNumCopied();
            }
        }
    }

    @ExplodeLoop
    private void initializeFrame(final VirtualFrame frame) {
        if (!doesNotNeedThisContext.isValid()) {
            ContextObject.createMaterialized(frame.materialize());
        } else if (CompilerDirectives.inCompiledCode()) {
            /*
             * Eagerly allocate a (frameless) context in compiled code. Otherwise, the first send
             * lazily creates one and stores it in the frame, and at every control-flow merge or
             * loop back-edge the context slot is either null or a virtual context, which forces
             * partial escape analysis to materialize the context (even if all sends were inlined
             * and the sender is never used). With an eager context, the slot holds the same virtual
             * object on all paths, so it is only materialized on paths where it actually escapes.
             */
            ContextObject.createVirtual(frame);
        }
        FrameAccess.setInstructionPointer(frame, initialPC);
        FrameAccess.setStackPointer(frame, initialSP);

        int slotIndex = FrameAccess.getStackStart();
        // Copy arguments onto stack
        final Object[] arguments = frame.getArguments();
        for (int i = 0; i < numArgs; i++) {
            frame.setObjectStatic(slotIndex++, arguments[FrameAccess.getArgumentStartIndex() + i]);
        }
        // Initialize remaining temporary variables with nil in newContext.
        final int numTempSlots = initialSP - numArgs;
        for (int i = 0; i < numTempSlots; i++) {
            frame.setObjectStatic(slotIndex++, NilObject.SINGLETON);
        }
    }

    @Override
    protected RootNode cloneUninitialized() {
        return new StartContextRootNode(this);
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return getCode().toString();
    }
}
