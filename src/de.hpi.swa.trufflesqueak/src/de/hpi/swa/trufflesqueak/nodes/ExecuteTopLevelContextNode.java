/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes;

import java.util.logging.Level;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.RootNode;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.exceptions.Returns.CannotReturnToTarget;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.Returns.TopLevelReturn;
import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.io.SqueakDisplay;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.nodes.context.CreateCannotReturnContextNode;
import de.hpi.swa.trufflesqueak.nodes.process.GetNextActiveContextNode;
import de.hpi.swa.trufflesqueak.shared.SqueakLanguageConfig;
import de.hpi.swa.trufflesqueak.util.DebugUtils;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.LogUtils;

@NodeInfo(language = SqueakLanguageConfig.ID)
public final class ExecuteTopLevelContextNode extends RootNode {
    private static final FrameDescriptor TOP_LEVEL_FRAME_DESCRIPTOR = new FrameDescriptor();

    private final SqueakImageContext image;
    private final boolean isImageResuming;
    private ContextObject initialContext;

    @Child private IndirectCallNode callNode = IndirectCallNode.create();
    @Child private GetNextActiveContextNode getNextActiveContextNode = GetNextActiveContextNode.create();
    @Child private CreateCannotReturnContextNode createCannotReturnContextNode;

    private ExecuteTopLevelContextNode(final SqueakImageContext image, final SqueakLanguage language, final ContextObject context, final boolean isImageResuming) {
        super(language, TOP_LEVEL_FRAME_DESCRIPTOR);
        this.image = image;
        initialContext = context;
        this.isImageResuming = isImageResuming;
        createCannotReturnContextNode = new CreateCannotReturnContextNode(image);
    }

    public static ExecuteTopLevelContextNode create(final SqueakImageContext image, final SqueakLanguage language, final ContextObject context, final boolean isImageResuming) {
        return new ExecuteTopLevelContextNode(image, language, context, isImageResuming);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        try {
            executeLoop();
        } catch (final TopLevelReturn e) {
            return e.getReturnValue();
        } finally {
            if (isImageResuming) {
                image.interrupt.shutdown();
                final SqueakDisplay display = image.getDisplay();
                if (display != null) {
                    display.close();
                }
            }
        }
        throw SqueakException.create("Top level context did not return");
    }

    private void executeLoop() {
        ContextObject activeContext = initialContext;
        if (isImageResuming) {
            /*
             * Free initialContext if resuming an image. Headless code execution requests can be
             * cached by Truffle. Therefore, they must keep their initialContext, so that they can
             * be restarted.
             */
            initialContext = null;
        } else {
            ensureCachedContextCanRunAgain(activeContext);
        }
        while (true) {
            final AbstractSqueakObject sender = activeContext.getSender();
            assert sender == NilObject.SINGLETON || ((ContextObject) sender).hasTruffleFrame();
            try {
                try {
                    image.resetContextStackDepth();
                    final Object result = callNode.call(activeContext.getCallTarget());
                    activeContext = returnTo(activeContext, sender, result);
                    LogUtils.SCHEDULING.log(Level.FINE, "Local Return on top-level: {0}", activeContext);
                } catch (final NonLocalReturn nlr) {
                    activeContext = commonNLReturn(sender, nlr);
                    LogUtils.SCHEDULING.log(Level.FINE, "Non Local Return on top-level: {0}", activeContext);
                } catch (final NonVirtualReturn nvr) {
                    activeContext = commonNVReturn(nvr);
                    LogUtils.SCHEDULING.log(Level.FINE, "Non Virtual Return on top-level: {0}", activeContext);
                } catch (final CannotReturnToTarget cr) {
                    activeContext = createCannotReturnContextNode.execute(cr.getTargetContext(), cr.getReturnValue());
                    LogUtils.SCHEDULING.log(Level.FINE, "Cannot Return on top-level: {0}", activeContext);
                }
            } catch (final ProcessSwitch ps) {
                activeContext = getNextActiveContextNode.execute();
                LogUtils.SCHEDULING.log(Level.FINE, "Process Switch: {0}", activeContext);
            }
        }
    }

    @TruffleBoundary
    private ContextObject sendCannotReturnOrReturnToTopLevel(final ContextObject returningContext, final Object returnValue) {
        /* Exit the interpreter loop if the target is the context that started the loop. */
        if (returningContext != null && returningContext == initialContext) {
            throw returnToTopLevel(returningContext, returnValue);
        }
        return createCannotReturnContextNode.execute(returningContext, returnValue);
    }

    @TruffleBoundary
    private ContextObject returnTo(final ContextObject returningContext, final AbstractSqueakObject sender, final Object returnValue) {
        /* Normal returns end up here. */
        assert (sender instanceof ContextObject) || sender == NilObject.SINGLETON;
        if ((sender instanceof final ContextObject senderContext) && !senderContext.isDead()) {
            senderContext.push(returnValue);
            return senderContext;
        }
        /*
         * The returningContext was terminated on the fast path, breaking the sender chain. If the
         * target is dead, we must restore the link so the exception handler can walk the stack.
         */
        if (sender instanceof ContextObject senderContext && senderContext.isDead()) {
            returningContext.setSenderUnsafe(senderContext);
        }
        return sendCannotReturnOrReturnToTopLevel(returningContext, returnValue);
    }

    @TruffleBoundary
    private static ContextObject commonNVReturn(final NonVirtualReturn nvr) {
        /*
         * Normal returns with modified senders end up here. The return Context has already been
         * validated.
         */
        final Object returnValue = nvr.getReturnValue();
        final ContextObject returnContext = nvr.getTargetContext();
        returnContext.push(returnValue);
        return returnContext;
    }

    @TruffleBoundary
    private static ContextObject commonNLReturn(final AbstractSqueakObject sender, final NonLocalReturn nlr) {
        /*
         * Non-local returns with no intervening unwind-blocks end up here. The home Context has
         * already been validated.
         */
        final ContextObject homeContext = nlr.getTargetContext();
        final Object returnValue = nlr.getReturnValue();
        /* Terminate the Contexts on sender chain. */
        ContextObject context = (ContextObject) sender;
        while (context != homeContext) {
            final ContextObject currentSender = (ContextObject) context.getSender();
            context.terminate();
            context = currentSender;
        }
        homeContext.push(returnValue);
        return homeContext;
    }

    private static TopLevelReturn returnToTopLevel(final ContextObject targetContext, final Object returnValue) {
        assert "DoIt".equals(targetContext.getCodeObject().getCompiledInSelector().asStringUnsafe()) : DebugUtils.getSqStackTrace(targetContext);
        throw new TopLevelReturn(returnValue);
    }

    private static void ensureCachedContextCanRunAgain(final ContextObject activeContext) {
        if (activeContext.getInstructionPointerForBytecodeLoop() != 0) {
            /*
             * Reset instruction pointer and stack pointer of the context (see {@link
             * EnterCodeNode#initializeSlots}) in case it has previously been executed and needs to
             * run again, for example because the Source has been cached.
             */
            assert !activeContext.hasClosure() : "activeContext is expected to have no closure";
            final CompiledCodeObject method = activeContext.getCodeObject();
            final MaterializedFrame truffleFrame = activeContext.getTruffleFrame();
            FrameAccess.setInstructionPointer(truffleFrame, 0);
            FrameAccess.setStackPointer(truffleFrame, method.getNumTemps());
        }
    }

    @Override
    public String getName() {
        return "<" + SqueakLanguageConfig.ID + "-toplevel>";
    }

    @Override
    public boolean isInternal() {
        return true;
    }
}
