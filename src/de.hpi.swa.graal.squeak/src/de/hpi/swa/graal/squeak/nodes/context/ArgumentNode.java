package de.hpi.swa.graal.squeak.nodes.context;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.NotProvided;
import de.hpi.swa.graal.squeak.nodes.SqueakNodeWithCode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameArgumentNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public abstract class ArgumentNode extends SqueakNodeWithCode {
    protected final int argumentIndex;
    @Child private FrameArgumentNode frameArgumentNode;

    public static ArgumentNode create(final CompiledCodeObject code, final int argumentIndex) {
        return ArgumentNodeGen.create(code, argumentIndex);
    }

    protected ArgumentNode(final CompiledCodeObject code, final int argumentIndex) {
        super(code);
        this.argumentIndex = argumentIndex; // argumentIndex == 0 returns receiver
    }

    @Specialization(guards = {"isVirtualized(frame)", "argumentIndex <= code.getNumArgs()"})
    protected static final Object doVirtualized(final VirtualFrame frame,
                    @Cached("createFrameArgumentNode()") final FrameArgumentNode argumentNode) {
        return argumentNode.executeRead(frame);
    }

    @Specialization(guards = {"!isVirtualized(frame)", "argumentIndex <= code.getNumArgs()"})
    protected final Object doUnvirtualized(final VirtualFrame frame) {
        return getContext(frame).atStack(argumentIndex);
    }

    @Specialization(guards = {"argumentIndex > code.getNumArgs()"})
    protected static final Object doArgumentsExhausted() {
        return NotProvided.INSTANCE;
    }

    @Fallback
    protected static final Object doFail() {
        throw new SqueakException("Should never happend");
    }

    protected FrameArgumentNode createFrameArgumentNode() {
        return FrameArgumentNode.create(FrameAccess.RECEIVER + argumentIndex);
    }
}
