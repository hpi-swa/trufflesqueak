/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
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
        assert !activeContext.isDead() : "Terminated contexts cannot be resumed";
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        assert !activeContext.isDead() : "Terminated contexts cannot be resumed";
        activeContext.clearModifiedSender();
        final int pc = instructionPointerProfile.profile(activeContext.getInstructionPointerForBytecodeLoop());
        final int sp = stackPointerProfile.profile(activeContext.getStackPointer());
        if (CompilerDirectives.isPartialEvaluationConstant(pc) && CompilerDirectives.isPartialEvaluationConstant(sp)) {
            return interpreterNode.execute(activeContext.getTruffleFrame(), pc, sp);
        } else {
            return interpretBytecodeWithBoundary(pc, sp);
        }
    }

    @TruffleBoundary
    private Object interpretBytecodeWithBoundary(final int pc, final int sp) {
        return interpreterNode.execute(activeContext.getTruffleFrame(), pc, sp);
    }

    public ContextObject getActiveContext() {
        return activeContext;
    }

    public void setActiveContext(final ContextObject newActiveContext) {
        assert activeContext.getCodeObject() == newActiveContext.getCodeObject();
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
        return activeContext.toString();
    }
}
