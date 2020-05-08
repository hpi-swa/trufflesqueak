/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.RootNode;

import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;

@NodeInfo(cost = NodeCost.NONE)
public abstract class ResumeContextNode extends Node {
    @Child private ExecuteContextNode executeContextNode;

    protected ResumeContextNode(final CompiledCodeObject code) {
        executeContextNode = ExecuteContextNode.create(code, true);
    }

    protected abstract Object executeResume(ContextObject context);

    @Specialization(guards = "context.getInstructionPointerForBytecodeLoop() == 0")
    protected final Object doResumeAtStart(final ContextObject context) {
        return executeContextNode.executeResumeAtStart(context.getTruffleFrame());
    }

    /* Avoid compilation of contexts that are not resumed from the start. */
    @TruffleBoundary
    @Specialization(guards = "context.getInstructionPointerForBytecodeLoop() > 0")
    protected final Object doResumeInMiddle(final ContextObject context) {
        final long initialPC = context.getInstructionPointerForBytecodeLoop();
        return executeContextNode.executeResumeInMiddle(context.getTruffleFrame(), initialPC);
    }

    @NodeInfo(cost = NodeCost.NONE)
    public static final class ResumeContextRootNode extends RootNode {
        private ContextObject activeContext;

        @Child private ResumeContextNode executeContextNode;

        protected ResumeContextRootNode(final SqueakLanguage language, final ContextObject context) {
            super(language, context.getTruffleFrame().getFrameDescriptor());
            activeContext = context;
            executeContextNode = ResumeContextNodeGen.create(context.getBlockOrMethod());
        }

        public static ResumeContextRootNode create(final SqueakLanguage language, final ContextObject activeContext) {
            return new ResumeContextRootNode(language, activeContext);
        }

        @Override
        public Object execute(final VirtualFrame frame) {
            return executeContextNode.executeResume(activeContext);
        }

        public ContextObject getActiveContext() {
            return activeContext;
        }

        public void setActiveContext(final ContextObject activeContext) {
            this.activeContext = activeContext;
        }

        @Override
        protected boolean isInstrumentable() {
            return false;
        }

        @Override
        public String getName() {
            CompilerAsserts.neverPartOfCompilation();
            return activeContext.toString();
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return activeContext.toString();
        }

        @Override
        public boolean isCloningAllowed() {
            return true;
        }
    }
}
