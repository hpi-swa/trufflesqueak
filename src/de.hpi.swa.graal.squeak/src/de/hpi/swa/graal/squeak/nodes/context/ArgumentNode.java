package de.hpi.swa.graal.squeak.nodes.context;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.NotProvided;
import de.hpi.swa.graal.squeak.nodes.SqueakNodeWithCode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public abstract class ArgumentNode extends SqueakNodeWithCode {
    protected final int argumentIndex;

    public static ArgumentNode create(final CompiledCodeObject code, final int argumentIndex) {
        return ArgumentNodeGen.create(code, argumentIndex);
    }

    protected ArgumentNode(final CompiledCodeObject code, final int argumentIndex) {
        super(code);
        this.argumentIndex = argumentIndex; // argumentIndex == 0 returns receiver
    }

    @Specialization(guards = {"argumentIndex <= code.getNumArgs()"})
    protected final Object doArgument(final VirtualFrame frame) {
        return FrameAccess.getArgument(frame, argumentIndex);
    }

    @Specialization(guards = {"argumentIndex > code.getNumArgs()"})
    protected static final Object doArgumentsExhausted() {
        return NotProvided.INSTANCE;
    }

    @Fallback
    protected static final Object doFail() {
        throw new SqueakException("Should never happend");
    }
}
