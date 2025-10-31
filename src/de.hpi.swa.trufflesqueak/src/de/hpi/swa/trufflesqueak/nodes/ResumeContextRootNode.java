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
import com.oracle.truffle.api.profiles.IntValueProfile;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ContextObject;

public final class ResumeContextRootNode extends AbstractRootNode {
    private ContextObject activeContext;
    private final IntValueProfile instructionPointerProfile = IntValueProfile.createIdentityProfile();

    public ResumeContextRootNode(final SqueakImageContext image, final ContextObject context) {
        super(image, context.getCodeObject());
        activeContext = context;
        assert !context.isDead() : "Terminated contexts cannot be resumed";
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        try {
            assert !activeContext.isDead() : "Terminated contexts cannot be resumed";
            activeContext.clearModifiedSender();
            final int pc = instructionPointerProfile.profile(activeContext.getInstructionPointerForBytecodeLoop());
            if (CompilerDirectives.isPartialEvaluationConstant(pc)) {
                return executeBytecodeNode.execute(activeContext.getTruffleFrame(), pc);
            } else {
                return interpretBytecodeWithBoundary(pc);
            }
        } finally {
            SqueakImageContext.get(this).lastSeenContext = null; // Stop materialization here.
        }
    }

    @TruffleBoundary
    private Object interpretBytecodeWithBoundary(final int pc) {
        return executeBytecodeNode.execute(activeContext.getTruffleFrame(), pc);
    }

    public ContextObject getActiveContext() {
        return activeContext;
    }

    public void setActiveContext(final ContextObject newActiveContext) {
        assert activeContext.getCodeObject() == newActiveContext.getCodeObject();
        activeContext = newActiveContext;
    }

    @Override
    protected boolean isInstrumentable() {
        return false;
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return activeContext.toString();
    }
}
