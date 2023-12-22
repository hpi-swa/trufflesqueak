/*
 * Copyright (c) 2017-2023 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2023 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.exceptions.Returns;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.FrameMarker;
import de.hpi.swa.trufflesqueak.nodes.HandleNonLocalReturnNodeFactory.AboutToReturnNodeGen;
import de.hpi.swa.trufflesqueak.nodes.HandleNonLocalReturnNodeFactory.NormalReturnNodeGen;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackWriteNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetContextOrMarkerNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetOrCreateContextNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchClosureNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

@SuppressWarnings("truffle-inlining")
public abstract class HandleNonLocalReturnNode extends AbstractNode {
    public static HandleNonLocalReturnNode create(final CompiledCodeObject code) {
        if (code.isUnwindMarked()) {
            return AboutToReturnNodeGen.create();
        } else {
            return NormalReturnNodeGen.create();
        }
    }

    public abstract ControlFlowException execute(VirtualFrame frame, NonLocalReturn nlr);

    @ImportStatic(FrameStackReadNode.class)
    public abstract static class AboutToReturnNode extends HandleNonLocalReturnNode {
        // @Specialization(guards = {"!hasModifiedSender(frame)"})
        // protected static final ControlFlowException doAboutToReturnVirtualized(final VirtualFrame
        // frame,
        // @SuppressWarnings("unused") final NonLocalReturn nlr,
        // @Cached("createTemporaryReadNode(frame, 1)") final FrameStackReadNode
        // completeTempReadNode,
        // @Cached final DispatchVirtualAboutToReturnNode dispatchVirtualAboutToReturnNode) {
        // dispatchVirtualAboutToReturnNode.execute(frame, completeTempReadNode.executeRead(frame)
        // ==
        // NilObject.SINGLETON);
        // throw CompilerDirectives.shouldNotReachHere();
        // }

        @Specialization// (guards = {"hasModifiedSender(frame)"})
        protected static final ControlFlowException doAboutToReturn(final VirtualFrame frame, final NonLocalReturn nlr,
                        @Bind("this") final Node node,
                        @Cached final GetOrCreateContextNode getOrCreateContextNode,
                        @Cached("createAboutToReturnSend()") final SendSelectorNode sendAboutToReturnNode) {
            // assert nlr.getTargetContextOrMarker() instanceof ContextObject;
            final ContextObject context;
            if (nlr.getTargetContextOrMarker() instanceof final FrameMarker fm) {
                context = fm.getMaterializedContext();
            } else {
                context = nlr.getTargetContext();
            }
            sendAboutToReturnNode.executeSend(frame, new Object[]{getOrCreateContextNode.executeGet(frame, node), nlr.getReturnValue(), context});
            throw CompilerDirectives.shouldNotReachHere();
        }
    }

    protected abstract static class DispatchVirtualAboutToReturnNode extends AbstractNode {

        protected abstract void execute(VirtualFrame frame, boolean completeTempIsNil);

        /*
         * Virtualized version of Context>>aboutToReturn:through:, more specifically
         * Context>>resume:through:. This is only called if code.isUnwindMarked(), so there is no
         * need to unwind contexts here as this is already happening when NonLocalReturns are
         * handled. Note that this however does not check if the current context isDead nor does it
         * terminate contexts (this may be a problem).
         */
        @Specialization(guards = {"completeTempIsNil"})
        protected static final void doVirtualized(final VirtualFrame frame, @SuppressWarnings("unused") final boolean completeTempIsNil,
                        @Bind("this") final Node node,
                        @Cached("createTemporaryReadNode(frame, 0)") final FrameStackReadNode blockArgumentNode,
                        @Cached("create(frame, 1)") final FrameStackWriteNode completeTempWriteNode,
                        @Cached final GetContextOrMarkerNode getContextOrMarkerNode,
                        @Cached final DispatchClosureNode dispatchNode) {
            completeTempWriteNode.executeWrite(frame, BooleanObject.TRUE);
            final BlockClosureObject closure = (BlockClosureObject) blockArgumentNode.executeRead(frame);
            dispatchNode.execute(node, closure, FrameAccess.newClosureArgumentsTemplate(closure, getContextOrMarkerNode.execute(frame), 0));
        }

        @Specialization(guards = {"!completeTempIsNil"})
        protected static final void doAboutToReturnVirtualizedNothing(@SuppressWarnings("unused") final boolean completeTempIsNil) {
            // Nothing to do.
        }
    }

    protected abstract static class NormalReturnNode extends HandleNonLocalReturnNode {
        @Specialization(guards = {"!hasModifiedSender(frame)"})
        protected static final ControlFlowException doAboutToReturnVirtualized(final VirtualFrame frame, final NonLocalReturn nlr) {
            FrameAccess.terminate(frame);
            throw nlr;
        }

        @Specialization(guards = {"hasModifiedSender(frame)"})
        protected static final ControlFlowException doAboutToReturn(final VirtualFrame frame, final NonLocalReturn nlr) {
            // Sender might have changed.
            final ContextObject newSender = FrameAccess.getSenderContext(frame);
            final ContextObject target = (ContextObject) nlr.getTargetContextOrMarker();
            FrameAccess.terminate(frame);
            // TODO: `target == newSender` may could use special handling?
            throw new Returns.NonVirtualReturn(nlr.getReturnValue(), target, newSender);
        }
    }

    @NeverDefault
    protected static final SendSelectorNode createAboutToReturnSend() {
        return SendSelectorNode.create(SqueakImageContext.getSlow().aboutToReturnSelector);
    }
}
