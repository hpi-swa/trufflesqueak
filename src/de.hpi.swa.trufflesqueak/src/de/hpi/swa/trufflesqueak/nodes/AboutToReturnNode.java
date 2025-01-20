/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DenyReplace;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnadoptableNode;

import de.hpi.swa.trufflesqueak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.nodes.AboutToReturnNodeFactory.AboutToReturnImplNodeGen;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackWriteNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetContextOrMarkerNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchClosureNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector2NodeFactory.Dispatch2NodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector2Node.Dispatch2Node;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

@SuppressWarnings("truffle-inlining")
public abstract class AboutToReturnNode extends AbstractNode {
    public static AboutToReturnNode create(final CompiledCodeObject code) {
        if (code.isUnwindMarked()) {
            return AboutToReturnImplNodeGen.create();
        } else {
            return AboutToReturnNoopNode.SINGLETON;
        }
    }

    public abstract void executeAboutToReturn(VirtualFrame frame, NonLocalReturn nlr);

    @ImportStatic(FrameStackReadNode.class)
    protected abstract static class AboutToReturnImplNode extends AboutToReturnNode {

        /*
         * Virtualized version of Context>>aboutToReturn:through:, more specifically
         * Context>>resume:through:. This is only called if code.isUnwindMarked(), so there is no
         * need to unwind contexts here as this is already happening when NonLocalReturns are
         * handled. Note that this however does not check if the current context isDead nor does it
         * terminate contexts (this may be a problem).
         */
        @Specialization(guards = {"!hasModifiedSender(frame)", "isNil(completeTempReadNode.executeRead(frame))"}, limit = "1")
        protected static final void doAboutToReturnVirtualized(final VirtualFrame frame, @SuppressWarnings("unused") final NonLocalReturn nlr,
                        @Bind final Node node,
                        @Cached("createTemporaryReadNode(frame, 0)") final FrameStackReadNode blockArgumentNode,
                        @SuppressWarnings("unused") @Cached("createTemporaryReadNode(frame, 1)") final FrameStackReadNode completeTempReadNode,
                        @Cached("create(frame, 1)") final FrameStackWriteNode completeTempWriteNode,
                        @Cached final GetContextOrMarkerNode getContextOrMarkerNode,
                        @Cached final DispatchClosureNode dispatchNode) {
            completeTempWriteNode.executeWrite(frame, BooleanObject.TRUE);
            final BlockClosureObject closure = (BlockClosureObject) blockArgumentNode.executeRead(frame);
            dispatchNode.execute(node, closure, FrameAccess.newClosureArgumentsTemplate(closure, getContextOrMarkerNode.execute(frame), 0));
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!hasModifiedSender(frame)", "!isNil(completeTempReadNode.executeRead(frame))"}, limit = "1")
        protected final void doAboutToReturnVirtualizedNothing(final VirtualFrame frame, final NonLocalReturn nlr,
                        @Cached("createTemporaryReadNode(frame, 1)") final FrameStackReadNode completeTempReadNode) {
            // Nothing to do.
        }

        @Specialization(guards = {"hasModifiedSender(frame)"})
        protected static final void doAboutToReturn(final VirtualFrame frame, final NonLocalReturn nlr,
                        @Cached("createAboutToReturnSend()") final Dispatch2Node sendAboutToReturnNode) {
            assert nlr.getTargetContextOrMarker() instanceof ContextObject;
            sendAboutToReturnNode.execute(frame, FrameAccess.getContext(frame), nlr.getReturnValue(), nlr.getTargetContextOrMarker());
        }
    }

    @DenyReplace
    private static final class AboutToReturnNoopNode extends AboutToReturnNode implements UnadoptableNode {
        private static final AboutToReturnNoopNode SINGLETON = new AboutToReturnNoopNode();

        @Override
        public void executeAboutToReturn(final VirtualFrame frame, final NonLocalReturn nlr) {
            // Nothing to do.
        }

        @Override
        public Node copy() {
            return SINGLETON;
        }

        @Override
        public Node deepCopy() {
            return copy();
        }
    }

    @NeverDefault
    protected static final Dispatch2Node createAboutToReturnSend() {
        return Dispatch2NodeGen.create(SqueakImageContext.getSlow().aboutToReturnSelector);
    }
}
