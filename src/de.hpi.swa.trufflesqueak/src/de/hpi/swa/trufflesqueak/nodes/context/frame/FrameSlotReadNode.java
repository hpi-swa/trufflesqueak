/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.context.frame;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameUtil;

import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameSlotReadNodeGen.FrameSlotReadClearNodeGen;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameSlotReadNodeGen.FrameSlotReadNoClearNodeGen;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

@ImportStatic(FrameSlotKind.class)
public abstract class FrameSlotReadNode extends AbstractFrameSlotNode {

    public static FrameSlotReadNode create(final FrameSlot frameSlot) {
        return FrameSlotReadNoClearNodeGen.create(frameSlot);
    }

    public static FrameSlotReadNode create(final Frame frame, final int index) {
        // Only clear stack values, not receiver, arguments, or temporary variables.
        final CompiledCodeObject code;
        final int initialSP;
        final BlockClosureObject closure = FrameAccess.getClosure(frame);
        if (closure == null) {
            code = FrameAccess.getMethod(frame);
            initialSP = code.getNumTemps();
        } else {
            code = closure.getCompiledBlock();
            initialSP = code.getNumArgsAndCopied();
        }
        if (index >= initialSP) {
            return FrameSlotReadClearNodeGen.create(code.getStackSlot(index));
        } else {
            return FrameSlotReadNoClearNodeGen.create(code.getStackSlot(index));
        }
    }

    public final Object executeRead(final Frame frame) {
        final Object value = executeReadUnsafe(frame);
        assert value != null : "Unexpected `null` value";
        return value;
    }

    /* Unsafe as it may return `null` values. */
    public abstract Object executeReadUnsafe(Frame frame);

    @Specialization(guards = "frame.isBoolean(getSlot())")
    protected final boolean readBoolean(final Frame frame) {
        return FrameUtil.getBooleanSafe(frame, getSlot());
    }

    @Specialization(guards = "frame.isLong(getSlot())")
    protected final long readLong(final Frame frame) {
        return FrameUtil.getLongSafe(frame, getSlot());
    }

    @Specialization(guards = "frame.isDouble(getSlot())")
    protected final double readDouble(final Frame frame) {
        return FrameUtil.getDoubleSafe(frame, getSlot());
    }

    protected abstract static class FrameSlotReadNoClearNode extends FrameSlotReadNode {

        @Specialization(replaces = {"readBoolean", "readLong", "readDouble"})
        protected final Object readObject(final Frame frame) {
            if (!frame.isObject(getSlot())) {
                /*
                 * The FrameSlotKind has been set to Object, so from now on all writes to the slot
                 * will be Object writes. However, now we are in a frame that still has an old
                 * non-Object value. This is a slow-path operation: we read the non-Object value,
                 * and write it immediately as an Object value so that we do not hit this path again
                 * multiple times for the same slot of the same frame.
                 */
                CompilerDirectives.transferToInterpreter();
                final Object value = frame.getValue(getSlot());
                assert value != null : "Unexpected `null` value";
                frame.setObject(getSlot(), value);
                return value;
            } else {
                return FrameUtil.getObjectSafe(frame, getSlot());
            }
        }
    }

    protected abstract static class FrameSlotReadClearNode extends FrameSlotReadNode {

        @Specialization(replaces = {"readBoolean", "readLong", "readDouble"})
        protected final Object readAndClearObject(final Frame frame) {
            final Object value;
            if (!frame.isObject(getSlot())) {
                /*
                 * The FrameSlotKind has been set to Object, so from now on all writes to the slot
                 * will be Object writes. However, now we are in a frame that still has an old
                 * non-Object value. This is a slow-path operation: we read the non-Object value,
                 * and clear it immediately as an Object value so that we do not hit this path again
                 * multiple times for the same slot of the same frame.
                 */
                CompilerDirectives.transferToInterpreter();
                value = frame.getValue(getSlot());
            } else {
                value = FrameUtil.getObjectSafe(frame, getSlot());
            }
            frame.setObject(getSlot(), null);
            return value;
        }
    }
}
