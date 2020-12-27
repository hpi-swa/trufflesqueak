/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ValueProfile;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.nodes.AboutToReturnNodeFactory.AboutToReturnImplNodeGen;
import de.hpi.swa.trufflesqueak.nodes.context.TemporaryWriteMarkContextsNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetContextNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetContextOrMarkerNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchClosureNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public abstract class AboutToReturnNode extends AbstractNode {
    public static AboutToReturnNode create(final CompiledCodeObject code) {
        if (code.isUnwindMarked()) {
            return AboutToReturnImplNodeGen.create();
        } else {
            return new AboutToReturnNoopNode();
        }
    }

    public abstract void executeAboutToReturn(VirtualFrame frame, NonLocalReturn nlr);

    protected abstract static class AboutToReturnImplNode extends AboutToReturnNode {

        /*
         * Virtualized version of Context>>aboutToReturn:through:, more specifically
         * Context>>resume:through:. This is only called if code.isUnwindMarked(), so there is no
         * need to unwind contexts here as this is already happening when NonLocalReturns are
         * handled. Note that this however does not check if the current context isDead nor does it
         * terminate contexts (this may be a problem).
         */
        @Specialization(guards = {"!getContextNode.hasModifiedSender(frame)", "isNil(completeTempReadNode.executeRead(frame))"}, limit = "1")
        protected static final void doAboutToReturnVirtualized(final VirtualFrame frame, @SuppressWarnings("unused") final NonLocalReturn nlr,
                        @SuppressWarnings("unused") @Shared("getContextNode") @Cached final GetContextNode getContextNode,
                        @Cached("createTemporaryReadNode(frame, 0)") final FrameStackReadNode blockArgumentNode,
                        @SuppressWarnings("unused") @Cached("createTemporaryReadNode(frame, 1)") final FrameStackReadNode completeTempReadNode,
                        @Cached("create(frame, 1)") final TemporaryWriteMarkContextsNode completeTempWriteNode,
                        /*
                         * It is very likely that ensure block is constant, hence the ValueProfile.
                         */
                        @Cached("createIdentityProfile()") final ValueProfile blockProfile,
                        @Cached final GetContextOrMarkerNode getContextOrMarkerNode,
                        @Cached final DispatchClosureNode dispatchNode) {
            completeTempWriteNode.executeWrite(frame, BooleanObject.TRUE);
            final BlockClosureObject closure = (BlockClosureObject) blockArgumentNode.executeRead(frame);
            dispatchNode.execute(closure, FrameAccess.newClosureArgumentsTemplate(closure, blockProfile.profile(closure.getCompiledBlock()), getContextOrMarkerNode.execute(frame), 0));
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!getContextNode.hasModifiedSender(frame)", "!isNil(completeTempReadNode.executeRead(frame))"}, limit = "1")
        protected final void doAboutToReturnVirtualizedNothing(final VirtualFrame frame, final NonLocalReturn nlr,
                        @Shared("getContextNode") @Cached final GetContextNode getContextNode,
                        @Cached("createTemporaryReadNode(frame, 1)") final FrameStackReadNode completeTempReadNode) {
            // Nothing to do.
        }

        @Specialization(guards = {"getContextNode.hasModifiedSender(frame)"}, limit = "1")
        protected static final void doAboutToReturn(final VirtualFrame frame, final NonLocalReturn nlr,
                        @Shared("getContextNode") @Cached final GetContextNode getContextNode,
                        @Cached("createAboutToReturnSend()") final SendSelectorNode sendAboutToReturnNode) {
            assert nlr.getTargetContextOrMarker() instanceof ContextObject;
            sendAboutToReturnNode.executeSend(frame, getContextNode.execute(frame), nlr.getReturnValue(), nlr.getTargetContextOrMarker());
        }
    }

    protected static final class AboutToReturnNoopNode extends AboutToReturnNode {
        @Override
        public void executeAboutToReturn(final VirtualFrame frame, final NonLocalReturn nlr) {
            // Nothing to do.
        }
    }

    protected static final FrameStackReadNode createTemporaryReadNode(final VirtualFrame frame, final int tempIndex) {
        return FrameStackReadNode.create(frame, tempIndex, false);
    }

    protected static final SendSelectorNode createAboutToReturnSend() {
        return SendSelectorNode.create(SqueakLanguage.getContext().aboutToReturnSelector);
    }
}
