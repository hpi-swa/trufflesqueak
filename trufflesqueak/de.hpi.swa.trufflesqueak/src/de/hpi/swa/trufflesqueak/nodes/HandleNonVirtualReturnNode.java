package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

@ImportStatic(FrameAccess.class)
public abstract class HandleNonVirtualReturnNode extends AbstractNodeWithCode {

    public static HandleNonVirtualReturnNode create(CompiledCodeObject code) {
        return HandleNonVirtualReturnNodeGen.create(code);
    }

    public HandleNonVirtualReturnNode(CompiledCodeObject code) {
        super(code);
    }

    public abstract Object executeHandle(VirtualFrame frame, NonVirtualReturn nvr);

    @Specialization(guards = "isVirtualized(frame)")
    protected Object handleVirtualized(VirtualFrame frame, NonVirtualReturn nvr) {
        if (nvr.getTargetContext().getFrameMarker() == getFrameMarker(frame)) {
            return nvr.getReturnValue();
        } else {
            throw nvr;
        }
    }

    @Specialization(guards = "!isVirtualized(frame)")
    protected Object handle(VirtualFrame frame, NonVirtualReturn nvr) {
        if (nvr.getTargetContext() == getContext(frame)) {
            return nvr.getReturnValue();
        } else {
            throw nvr;
        }
    }
}
