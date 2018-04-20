package de.hpi.swa.graal.squeak.nodes.context;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.nodes.SqueakNodeWithCode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameArgumentNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public abstract class ArgumentNode extends SqueakNodeWithCode {
    @CompilationFinal private final long argumentIndex;
    @Child private FrameArgumentNode frameArgumentNode;

    public static ArgumentNode create(final CompiledCodeObject code, final int argumentIndex) {
        return ArgumentNodeGen.create(code, argumentIndex);
    }

    protected ArgumentNode(final CompiledCodeObject code, final int argumentIndex) {
        super(code);
        frameArgumentNode = FrameArgumentNode.create(FrameAccess.RCVR_AND_ARGS_START + argumentIndex);
        this.argumentIndex = argumentIndex;
    }

    @Specialization(guards = {"isVirtualized(frame)"})
    protected Object doVirtualized(final VirtualFrame frame) {
        CompilerDirectives.ensureVirtualizedHere(frame);
        return frameArgumentNode.executeRead(frame);
    }

    @Specialization(guards = {"!isVirtualized(frame)"})
    protected Object doUnvirtualized(final VirtualFrame frame) {
        return getContext(frame).atStack(argumentIndex);
    }
}
