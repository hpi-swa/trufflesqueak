package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.nodes.SqueakNodeWithCode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameArgumentNode;

public abstract class ArgumentNode extends SqueakNodeWithCode {
    @CompilationFinal private final int argumentIndex;
    @Child private FrameArgumentNode frameArgumentNode;

    public static ArgumentNode create(CompiledCodeObject code, int argumentIndex) {
        return ArgumentNodeGen.create(code, argumentIndex);
    }

    protected ArgumentNode(CompiledCodeObject code, int argumentIndex) {
        super(code);
        frameArgumentNode = FrameArgumentNode.create(argumentIndex);
        this.argumentIndex = argumentIndex;
    }

    @Specialization(guards = {"isVirtualized(frame, code)"})
    protected Object doVirtualized(VirtualFrame frame) {
        return frameArgumentNode.executeRead(frame);
    }

    @Specialization(guards = {"!isVirtualized(frame, code)"})
    protected Object doUnvirtualized(VirtualFrame frame) {
        return getContext(frame).at0(CONTEXT.RECEIVER + argumentIndex);
    }
}
