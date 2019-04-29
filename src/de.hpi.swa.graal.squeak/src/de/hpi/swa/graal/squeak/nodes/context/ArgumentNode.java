package de.hpi.swa.graal.squeak.nodes.context;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.NotProvided;
import de.hpi.swa.graal.squeak.nodes.SqueakNodeWithCode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public abstract class ArgumentNode extends SqueakNodeWithCode {
    protected final int argumentIndex;

    protected ArgumentNode(final CompiledCodeObject code, final int argumentIndex) {
        super(code);
        this.argumentIndex = argumentIndex; // argumentIndex == 0 returns receiver
    }

    public static ArgumentNode create(final CompiledCodeObject code, final int argumentIndex) {
        return ArgumentNodeGen.create(code, argumentIndex);
    }

    @Specialization(guards = {"argumentIndex <= code.getNumArgs()"})
    protected final Object doArgument(final VirtualFrame frame) {
        return FrameAccess.getArgument(frame, argumentIndex);
    }

    @Specialization(guards = {"argumentIndex > code.getNumArgs()"})
    protected static final Object doArgumentsExhausted() {
        return NotProvided.SINGLETON;
    }
}
