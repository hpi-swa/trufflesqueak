package de.hpi.swa.graal.squeak.util;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameUtil;

import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.FrameMarker;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackWriteNode;

public final class FrameAccess {
    /**
     * GraalSqueak frame argument layout.
     *
     * <pre>
     *                            +-------------------------------+
     * METHOD                  -> | CompiledMethodObject          |
     *                            +-------------------------------+
     * SENDER_OR_SENDER_MARKER -> | FrameMarker: virtual sender   |
     *                            | ContextObject: materialized   |
     *                            | nil: terminated / top-level   |
     *                            +-------------------------------+
     * CLOSURE_OR_NULL         -> | BlockClosure / null           |
     *                            +-------------------------------+
     * RECEIVER                -> | Object                        |
     *                            +-------------------------------+
     * ARGUMENTS_START         -> | argument0                     |
     *                            | argument1                     |
     *                            | ...                           |
     *                            | argument(nArgs-1)             |
     *                            | copiedValue1                  |
     *                            | copiedValue2                  |
     *                            | ...                           |
     *                            | copiedValue(nCopied-1)        |
     *                            +-------------------------------+
     * </pre>
     */
    public static final int METHOD = 0;
    public static final int SENDER_OR_SENDER_MARKER = 1;
    public static final int CLOSURE_OR_NULL = 2;
    public static final int RECEIVER = 3;
    public static final int ARGUMENTS_START = 4;

    /**
     * GraalSqueak frame slot layout.
     *
     * <pre>
     *                       +-------------------------------+
     * thisMarker         -> | FrameMarker                   |
     *                       +-------------------------------+
     * thisContext        -> | ContextObject / null          |
     *                       +-------------------------------+
     * instructionPointer -> | int (-1 if terminated)        |
     *                       +-------------------------------+
     * stackPointer       -> | int                           |
     *                       +-------------------------------+
     * stackSlots[]       -> | Object[] (specialized)        |
     *                       +-------------------------------+
     * </pre>
     */

    private FrameAccess() {
    }

    public static CompiledMethodObject getMethod(final Frame frame) {
        return (CompiledMethodObject) frame.getArguments()[METHOD];
    }

    public static Object getSender(final Frame frame) {
        return frame.getArguments()[SENDER_OR_SENDER_MARKER];
    }

    public static ContextObject getSenderContext(final Frame frame) {
        return (ContextObject) getSender(frame);
    }

    public static void setSender(final Frame frame, final AbstractSqueakObject value) {
        frame.getArguments()[SENDER_OR_SENDER_MARKER] = value;
    }

    public static BlockClosureObject getClosure(final Frame frame) {
        return (BlockClosureObject) frame.getArguments()[CLOSURE_OR_NULL];
    }

    public static CompiledCodeObject getBlockOrMethod(final Frame frame) {
        final BlockClosureObject closure = getClosure(frame);
        return closure != null ? closure.getCompiledBlock() : getMethod(frame);
    }

    public static Object getReceiver(final Frame frame) {
        return frame.getArguments()[RECEIVER];
    }

    public static Object getArgument(final Frame frame, final int index) {
        return frame.getArguments()[RECEIVER + index];
    }

    public static Object[] getReceiverAndArguments(final Frame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return Arrays.copyOfRange(frame.getArguments(), RECEIVER, frame.getArguments().length);
    }

    public static FrameMarker getMarker(final Frame frame) {
        return getMarker(frame, getBlockOrMethod(frame));
    }

    public static FrameMarker getMarker(final Frame frame, final CompiledCodeObject blockOrMethod) {
        return (FrameMarker) FrameUtil.getObjectSafe(frame, blockOrMethod.getThisMarkerSlot());
    }

    public static void setMarker(final Frame frame, final CompiledCodeObject code, final FrameMarker marker) {
        frame.setObject(code.getThisMarkerSlot(), marker);
    }

    public static void initializeMarker(final Frame frame, final CompiledCodeObject code) {
        setMarker(frame, code, new FrameMarker(frame));
    }

    public static void setMarker(final Frame frame, final FrameMarker marker) {
        setMarker(frame, getBlockOrMethod(frame), marker);
    }

    public static ContextObject getContext(final Frame frame) {
        return getContext(frame, getBlockOrMethod(frame));
    }

    public static ContextObject getContext(final Frame frame, final CompiledCodeObject blockOrMethod) {
        return (ContextObject) FrameUtil.getObjectSafe(frame, blockOrMethod.getThisContextSlot());
    }

    public static void setContext(final Frame frame, final CompiledCodeObject blockOrMethod, final ContextObject context) {
        final FrameSlot thisContextSlot = blockOrMethod.getThisContextSlot();
        assert getContext(frame, blockOrMethod) == null : "ContextObject already allocated";
        blockOrMethod.getFrameDescriptor().setFrameSlotKind(thisContextSlot, FrameSlotKind.Object);
        frame.setObject(thisContextSlot, context);
    }

    public static int getInstructionPointer(final Frame frame) {
        return getInstructionPointer(frame, getBlockOrMethod(frame));
    }

    public static int getInstructionPointer(final Frame frame, final CompiledCodeObject code) {
        return FrameUtil.getIntSafe(frame, code.getInstructionPointerSlot());
    }

    public static void setInstructionPointer(final Frame frame, final CompiledCodeObject code, final int value) {
        frame.setInt(code.getInstructionPointerSlot(), value);
    }

    public static int getStackPointer(final Frame frame) {
        return getStackPointer(frame, getBlockOrMethod(frame));
    }

    public static int getStackPointer(final Frame frame, final CompiledCodeObject code) {
        return FrameUtil.getIntSafe(frame, code.getStackPointerSlot());
    }

    public static void setStackPointer(final Frame frame, final CompiledCodeObject code, final int value) {
        frame.setInt(code.getStackPointerSlot(), value);
    }

    public static int getStackSize(final Frame frame) {
        return getBlockOrMethod(frame).getSqueakContextSize();
    }

    /** Write to a frame slot (slow operation), prefer {@link FrameStackWriteNode}. */
    public static void setStackSlot(final Frame frame, final FrameSlot frameSlot, final Object value) {
        final FrameDescriptor frameDescriptor = frame.getFrameDescriptor();
        if (value instanceof Boolean) {
            frameDescriptor.setFrameSlotKind(frameSlot, FrameSlotKind.Boolean);
            frame.setBoolean(frameSlot, (boolean) value);
        } else if (value instanceof Long) {
            frameDescriptor.setFrameSlotKind(frameSlot, FrameSlotKind.Long);
            frame.setLong(frameSlot, (long) value);
        } else if (value instanceof Double) {
            frameDescriptor.setFrameSlotKind(frameSlot, FrameSlotKind.Double);
            frame.setDouble(frameSlot, (double) value);
        } else {
            frameDescriptor.setFrameSlotKind(frameSlot, FrameSlotKind.Object);
            frame.setObject(frameSlot, value);
        }
    }

    public static void terminate(final Frame frame, final CompiledCodeObject blockOrMethod) {
        FrameAccess.setInstructionPointer(frame, blockOrMethod, -1);
        FrameAccess.setSender(frame, blockOrMethod.image.nil);
    }

    public static boolean isGraalSqueakFrame(final Frame frame) {
        final Object[] arguments = frame.getArguments();
        return arguments.length >= RECEIVER && arguments[METHOD] instanceof CompiledMethodObject;
    }

    public static boolean matchesContextOrMarker(final FrameMarker frameMarker, final Object contextOrMarker) {
        return contextOrMarker == frameMarker || (contextOrMarker instanceof ContextObject && ((ContextObject) contextOrMarker).getFrameMarker() == frameMarker);
    }

    public static Object[] newWith(final CompiledMethodObject method, final Object sender, final BlockClosureObject closure, final Object[] arguments) {
        final Object[] frameArguments = new Object[RECEIVER + arguments.length];
        assert method != null : "Method should never be null";
        assert sender != null : "Sender should never be null";
        assert arguments.length > 0 : "At least a receiver must be provided";
        assert arguments[0] != null : "Receiver should never be null";
        frameArguments[METHOD] = method;
        frameArguments[SENDER_OR_SENDER_MARKER] = sender;
        frameArguments[CLOSURE_OR_NULL] = closure;
        for (int i = 0; i < arguments.length; i++) {
            frameArguments[RECEIVER + i] = arguments[i];
        }
        return frameArguments;
    }

    public static Object[] newDummyWith(final CompiledCodeObject code, final Object sender, final BlockClosureObject closure, final Object[] arguments) {
        final Object[] frameArguments = new Object[RECEIVER + arguments.length];
        assert sender != null : "Sender should never be null";
        assert arguments.length > 0 : "At least a receiver must be provided";
        frameArguments[METHOD] = code;
        frameArguments[SENDER_OR_SENDER_MARKER] = sender;
        frameArguments[CLOSURE_OR_NULL] = closure;
        for (int i = 0; i < arguments.length; i++) {
            frameArguments[RECEIVER + i] = arguments[i];
        }
        return frameArguments;
    }

    public static Object[] newBlockArguments(final BlockClosureObject block, final Object senderOrMarker, final Object[] objects) {
        final int numObjects = objects.length;
        final Object[] copied = block.getStack();
        final int numCopied = copied.length;
        assert block.getCompiledBlock().getNumArgs() == numObjects && block.getCopied().length == numCopied : "number of required and provided block arguments do not match";
        final Object[] arguments = new Object[FrameAccess.ARGUMENTS_START + numObjects + numCopied];
        arguments[FrameAccess.METHOD] = block.getCompiledBlock().getMethod();
        // Sender is thisContext (or marker)
        arguments[FrameAccess.SENDER_OR_SENDER_MARKER] = senderOrMarker;
        arguments[FrameAccess.CLOSURE_OR_NULL] = block;
        arguments[FrameAccess.RECEIVER] = block.getReceiver();
        for (int i = 0; i < numObjects; i++) {
            arguments[FrameAccess.ARGUMENTS_START + i] = objects[i];
        }
        for (int i = 0; i < numCopied; i++) {
            arguments[FrameAccess.ARGUMENTS_START + numObjects + i] = copied[i];
        }
        return arguments;
    }

    @TruffleBoundary
    public static Frame findFrameForMarker(final FrameMarker frameMarker) {
        CompilerDirectives.bailout("Finding materializable frames should never be part of compiled code as it triggers deopts");
        return Truffle.getRuntime().iterateFrames(frameInstance -> {
            final Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.READ_ONLY);
            if (!isGraalSqueakFrame(current)) {
                return null;
            }
            if (frameMarker == getMarker(current)) {
                return frameInstance.getFrame(FrameInstance.FrameAccess.MATERIALIZE);
            }
            return null;
        });
    }
}
