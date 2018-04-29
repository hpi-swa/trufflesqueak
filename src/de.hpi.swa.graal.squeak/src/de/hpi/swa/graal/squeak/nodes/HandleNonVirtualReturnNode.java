package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.FrameMarker;
import de.hpi.swa.graal.squeak.util.FrameAccess;

@ImportStatic(FrameAccess.class)
public abstract class HandleNonVirtualReturnNode extends AbstractNodeWithCode {

    public static HandleNonVirtualReturnNode create(final CompiledCodeObject code) {
        return HandleNonVirtualReturnNodeGen.create(code);
    }

    public HandleNonVirtualReturnNode(final CompiledCodeObject code) {
        super(code);
    }

    public abstract Object executeHandle(VirtualFrame frame, NonVirtualReturn nvr);

    @Specialization(guards = "isVirtualized(frame)")
    protected Object handleVirtualized(final VirtualFrame frame, final NonVirtualReturn nvr) {
        final FrameMarker frameMarker = getFrameMarker(frame);
        if (nvr.getTargetContext().getFrameMarker() == frameMarker) {
            return nvr.getReturnValue();
        } else {
            throw nvr;
        }
    }

    @Specialization(guards = "!isVirtualized(frame)")
    protected Object handle(final VirtualFrame frame, final NonVirtualReturn nvr) {
        final ContextObject context = getContext(frame);
        if (nvr.getTargetContext().getFrameMarker() == context.getFrameMarker()) {
            return nvr.getReturnValue();
        } else {
            throw nvr;
        }
    }
}
