package de.hpi.swa.graal.squeak.nodes.context.stack;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;

public abstract class StackPopNode extends AbstractStackPopNode {
    public static StackPopNode create(final CompiledCodeObject code) {
        return StackPopNodeGen.create(code);
    }

    protected StackPopNode(final CompiledCodeObject code) {
        super(code);
    }

    @Specialization(guards = {"isVirtualized(frame)"})
    public final Object doPopVirtualized(final VirtualFrame frame) {
        final int sp = frameStackPointer(frame);
        assert sp >= 0 : "Bad stack pointer";
        if (sp > 0) {
            setFrameStackPointer(frame, sp - 1);
        }
        return atStackAndClear(frame, sp);
    }

    @Fallback
    protected final Object doPop(final VirtualFrame frame) {
        final ContextObject context = getContext(frame);
        final long sp = context.getStackPointer();
        if (sp > 0) {
            context.setStackPointer(sp - 1);
        }
        return atStackAndClear(context, sp);
    }
}
