package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.nodes.FrameAccess;
import de.hpi.swa.trufflesqueak.nodes.SqueakNodeWithCode;

public abstract class ArgumentNode extends SqueakNodeWithCode {
    private final int argumentIndex;

    public static ArgumentNode create(CompiledCodeObject code, int argumentIndex) {
        return ArgumentNodeGen.create(code, argumentIndex);
    }

    protected ArgumentNode(CompiledCodeObject code, int argumentIndex) {
        super(code);
        this.argumentIndex = argumentIndex;
    }

    @Specialization(guards = {"isVirtualized(frame)"})
    protected Object doVirtualized(VirtualFrame frame) {
        return frame.getArguments()[FrameAccess.RCVR_AND_ARGS_START + argumentIndex];
    }

    @Specialization(guards = {"!isVirtualized(frame)"})
    protected Object doUnvirtualized(VirtualFrame frame) {
        return getContext(frame).at0(CONTEXT.RECEIVER + argumentIndex);
    }
}
