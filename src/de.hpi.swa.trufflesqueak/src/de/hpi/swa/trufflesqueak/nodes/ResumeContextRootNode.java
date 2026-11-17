/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.profiles.IntValueProfile;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class ResumeContextRootNode extends AbstractRootNode {
    private ContextObject activeContext;
    private final IntValueProfile instructionPointerProfile = IntValueProfile.createIdentityProfile();
    private final IntValueProfile stackPointerProfile = IntValueProfile.createIdentityProfile();

    public ResumeContextRootNode(final SqueakImageContext image, final ContextObject context) {
        super(image, context.getMethodOrBlock());
        activeContext = context;
        assert !activeContext.isDead() : "Terminated contexts cannot be resumed";
    }

    public ResumeContextRootNode(final ResumeContextRootNode original) {
        super(original);
        activeContext = original.activeContext;
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        final ContextObject context = activeContext;
        /*
         * This root node is cached per method and outlives the context it resumes. Release the
         * context so that it, and all objects referenced from its frame, can be garbage collected
         * once it has returned.
         */
        activeContext = null;
        assert context != null && !context.isDead() : "Terminated contexts cannot be resumed";
        /*
         * Make it so ObjectGraphUtils can find the active context when executing strictly within
         * the active context.
         */
        FrameAccess.setContext(frame, context);
        /*
         * Reset the fast-path flag to indicate this Context has resumed execution. Future external
         * modifications to this Context's instruction pointer will now trigger a proper stack
         * check. See AbstractInterpreterNode#ensureContextReceiverIsNotActive
         */
        context.markAsPotentiallyActiveOnTruffleStack();
        context.clearModifiedSender();
        final int pc = instructionPointerProfile.profile(context.getInstructionPointerForBytecodeLoop());
        final int sp = stackPointerProfile.profile(context.getStackPointer());
        if (CompilerDirectives.isPartialEvaluationConstant(pc) && CompilerDirectives.isPartialEvaluationConstant(sp)) {
            return interpreterNode.execute(context.getTruffleFrame(), pc, sp);
        } else {
            return interpretBytecodeWithBoundary(context, pc, sp);
        }
    }

    @TruffleBoundary
    private Object interpretBytecodeWithBoundary(final ContextObject context, final int pc, final int sp) {
        return interpreterNode.execute(context.getTruffleFrame(), pc, sp);
    }

    public void setActiveContext(final ContextObject newActiveContext) {
        assert getFrameDescriptor() == newActiveContext.getMethodOrBlock().getFrameDescriptor();
        activeContext = newActiveContext;
    }

    @Override
    protected boolean isInstrumentable() {
        return false;
    }

    @Override
    protected RootNode cloneUninitialized() {
        return new ResumeContextRootNode(this);
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return getCode().toString();
    }
}
