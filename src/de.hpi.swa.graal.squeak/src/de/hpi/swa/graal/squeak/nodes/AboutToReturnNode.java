/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ValueProfile;

import de.hpi.swa.graal.squeak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.BooleanObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.nodes.context.TemporaryWriteMarkContextsNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameSlotReadNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public abstract class AboutToReturnNode extends AbstractNodeWithCode {
    protected AboutToReturnNode(final CompiledCodeObject code) {
        super(code);
    }

    public static AboutToReturnNode create(final CompiledCodeObject code) {
        return AboutToReturnNodeGen.create(code);
    }

    public abstract void executeAboutToReturn(VirtualFrame frame, NonLocalReturn nlr);

    /*
     * Virtualized version of Context>>aboutToReturn:through:, more specifically
     * Context>>resume:through:. This is only called if code.isUnwindMarked(), so there is no need
     * to unwind contexts here as this is already happening when NonLocalReturns are handled. Note
     * that this however does not check if the current context isDead nor does it terminate contexts
     * (this may be a problem).
     */
    @Specialization(guards = {"code.isUnwindMarked()", "!hasModifiedSender(frame)", "isNil(completeTempReadNode.executeRead(frame))"}, limit = "1")
    protected final void doAboutToReturnVirtualized(final VirtualFrame frame, @SuppressWarnings("unused") final NonLocalReturn nlr,
                    @Cached("createTemporaryReadNode(0)") final FrameSlotReadNode blockArgumentNode,
                    @SuppressWarnings("unused") @Cached("createTemporaryReadNode(1)") final FrameSlotReadNode completeTempReadNode,
                    @Cached("create(code, 1)") final TemporaryWriteMarkContextsNode completeTempWriteNode,
                    /* It is very likely that ensure block is constant, hence the ValueProfile. */
                    @Cached("createIdentityProfile()") final ValueProfile blockProfile,
                    @Cached final DispatchClosureNode dispatchNode) {
        completeTempWriteNode.executeWrite(frame, BooleanObject.TRUE);
        final BlockClosureObject closure = (BlockClosureObject) blockArgumentNode.executeRead(frame);
        dispatchNode.execute(closure, FrameAccess.newClosureArgumentsTemplate(closure, blockProfile.profile(closure.getCompiledBlock()), getContextOrMarker(frame), 0));
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"code.isUnwindMarked()", "!hasModifiedSender(frame)", "!isNil(completeTempReadNode.executeRead(frame))"}, limit = "1")
    protected final void doAboutToReturnVirtualizedNothing(final VirtualFrame frame, final NonLocalReturn nlr,
                    @Cached("createTemporaryReadNode(1)") final FrameSlotReadNode completeTempReadNode) {
        // Nothing to do.
    }

    @Specialization(guards = {"code.isUnwindMarked()", "hasModifiedSender(frame)"})
    protected final void doAboutToReturn(final VirtualFrame frame, final NonLocalReturn nlr,
                    @Cached("createAboutToReturnSend()") final SendSelectorNode sendAboutToReturnNode) {
        sendAboutToReturnNode.executeSend(frame, getContext(frame), nlr.getReturnValue(), nlr.getTargetContextOrMarker());
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!code.isUnwindMarked()"})
    protected final void doNothing(final VirtualFrame frame, final NonLocalReturn nlr) {
        // Nothing to do.
    }

    protected final FrameSlotReadNode createTemporaryReadNode(final int tempIndex) {
        return FrameSlotReadNode.create(code.getStackSlot(tempIndex));
    }

    protected final SendSelectorNode createAboutToReturnSend() {
        return SendSelectorNode.create(code, code.image.aboutToReturnSelector);
    }
}
