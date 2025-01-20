/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.util;

import java.util.Arrays;
import java.util.function.Consumer;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameDescriptor.Builder;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.FrameMarker;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackPushNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetContextOrMarkerNode;

/**
 * TruffleSqueak frame argument layout.
 *
 * <pre>
 *                            +---------------------------------+
 * SENDER_OR_SENDER_MARKER -> | FrameMarker: virtual sender     |
 *                            | ContextObject: materialized     |
 *                            | nil: terminated / top-level     |
 *                            +---------------------------------+
 * CLOSURE_OR_NULL         -> | BlockClosure / null / OSR Frame |
 *                            +---------------------------------+
 * RECEIVER                -> | Object                          |
 *                            +---------------------------------+
 * ARGUMENTS_START         -> | argument0                       |
 *                            | argument1                       |
 *                            | ...                             |
 *                            | argument(nArgs-1)               |
 *                            | copiedValue1                    |
 *                            | copiedValue2                    |
 *                            | ...                             |
 *                            | copiedValue(nCopied-1)          |
 *                            +---------------------------------+
 * </pre>
 *
 * TruffleSqueak frame slot layout.
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
 * stackSlots         -> | Object[] (specialized)        |
 *                       +-------------------------------+
 * </pre>
 */
public final class FrameAccess {
    private enum ArgumentIndicies {
        SENDER_OR_SENDER_MARKER, // 0
        CLOSURE_OR_NULL, // 1
        RECEIVER, // 2
        ARGUMENTS_START, // 3
    }

    private enum SlotIndicies {
        THIS_MARKER,
        THIS_CONTEXT,
        INSTRUCTION_POINTER,
        STACK_POINTER,
        STACK_START,
    }

    private FrameAccess() {
    }

    /** Creates a new {@link FrameDescriptor} according to {@link SlotIndicies}. */
    public static FrameDescriptor newFrameDescriptor(final CompiledCodeObject code) {
        final int numStackSlots = code.getMaxNumStackSlots();
        final Builder builder = FrameDescriptor.newBuilder(4 + numStackSlots);
        builder.info(code);
        addDefaultSlots(builder);
        builder.addSlots(numStackSlots, FrameSlotKind.Illegal);
        return builder.build();
    }

    public static VirtualFrame newDummyFrame() {
        final Builder builder = FrameDescriptor.newBuilder(4);
        builder.info("dummy");
        addDefaultSlots(builder);
        return Truffle.getRuntime().createVirtualFrame(FrameAccess.newWith(NilObject.SINGLETON, null, NilObject.SINGLETON), builder.build());
    }

    private static void addDefaultSlots(final Builder builder) {
        builder.addSlot(FrameSlotKind.Static, null, null);  // SlotIndicies.THIS_MARKER
        builder.addSlot(FrameSlotKind.Illegal, null, null); // SlotIndicies.THIS_CONTEXT
        builder.addSlot(FrameSlotKind.Static, null, null);  // SlotIndicies.INSTRUCTION_POINTER
        builder.addSlot(FrameSlotKind.Static, null, null);  // SlotIndicies.STACK_POINTER
    }

    public static void copyAllSlots(final MaterializedFrame source, final MaterializedFrame destination) {
        source.copyTo(0, destination, 0, source.getFrameDescriptor().getNumberOfSlots());
    }

    /* Returns the code object matching the frame's descriptor. */
    public static CompiledCodeObject getCodeObject(final Frame frame) {
        return (CompiledCodeObject) frame.getFrameDescriptor().getInfo();
    }

    public static Object getSender(final Frame frame) {
        return frame.getArguments()[ArgumentIndicies.SENDER_OR_SENDER_MARKER.ordinal()];
    }

    public static ContextObject getSenderContext(final Frame frame) {
        return (ContextObject) getSender(frame);
    }

    public static void setSender(final Frame frame, final AbstractSqueakObject value) {
        frame.getArguments()[ArgumentIndicies.SENDER_OR_SENDER_MARKER.ordinal()] = value;
    }

    public static BlockClosureObject getClosure(final Frame frame) {
        return (BlockClosureObject) frame.getArguments()[ArgumentIndicies.CLOSURE_OR_NULL.ordinal()];
    }

    public static boolean hasClosure(final Frame frame) {
        return frame.getArguments()[ArgumentIndicies.CLOSURE_OR_NULL.ordinal()] instanceof BlockClosureObject;
    }

    public static void setClosure(final Frame frame, final BlockClosureObject closure) {
        frame.getArguments()[ArgumentIndicies.CLOSURE_OR_NULL.ordinal()] = closure;
    }

    public static Object[] storeParentFrameInArguments(final VirtualFrame parentFrame) {
        assert !hasClosure(parentFrame);
        final Object[] arguments = parentFrame.getArguments();
        arguments[ArgumentIndicies.CLOSURE_OR_NULL.ordinal()] = parentFrame;
        return arguments;
    }

    public static Frame restoreParentFrameFromArguments(final Object[] arguments) {
        final Object frame = arguments[ArgumentIndicies.CLOSURE_OR_NULL.ordinal()];
        arguments[ArgumentIndicies.CLOSURE_OR_NULL.ordinal()] = null;
        return (Frame) frame;
    }

    public static Object getReceiver(final Frame frame) {
        return frame.getArguments()[ArgumentIndicies.RECEIVER.ordinal()];
    }

    public static void setReceiver(final Frame frame, final Object receiver) {
        frame.getArguments()[ArgumentIndicies.RECEIVER.ordinal()] = receiver;
    }

    public static Object getArgument(final Frame frame, final int index) {
        return frame.getArguments()[ArgumentIndicies.RECEIVER.ordinal() + index];
    }

    public static int getReceiverStartIndex() {
        return ArgumentIndicies.RECEIVER.ordinal();
    }

    public static int getArgumentStartIndex() {
        return ArgumentIndicies.ARGUMENTS_START.ordinal();
    }

    public static int getNumArguments(final Frame frame) {
        return frame.getArguments().length - getArgumentStartIndex();
    }

    public static Object[] getReceiverAndArguments(final Frame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return Arrays.copyOfRange(frame.getArguments(), getReceiverStartIndex(), frame.getArguments().length);
    }

    public static FrameMarker getMarker(final Frame frame) {
        return (FrameMarker) frame.getObjectStatic(SlotIndicies.THIS_MARKER.ordinal());
    }

    public static void setMarker(final Frame frame, final FrameMarker marker) {
        frame.setObjectStatic(SlotIndicies.THIS_MARKER.ordinal(), marker);
    }

    public static void initializeMarker(final Frame frame) {
        setMarker(frame, new FrameMarker());
    }

    public static Object getContextOrMarkerSlow(final VirtualFrame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return GetContextOrMarkerNode.getNotProfiled(frame);
    }

    public static ContextObject getContext(final Frame frame) {
        return (ContextObject) frame.getObject(SlotIndicies.THIS_CONTEXT.ordinal());
    }

    public static boolean hasEscapedContext(final VirtualFrame frame) {
        final ContextObject context = getContext(frame);
        return context != null && context.hasEscaped();
    }

    public static boolean hasModifiedSender(final VirtualFrame frame) {
        final ContextObject context = getContext(frame);
        return context != null && context.hasModifiedSender();
    }

    public static void setContext(final Frame frame, final ContextObject context) {
        assert getContext(frame) == null : "ContextObject already allocated";
        frame.getFrameDescriptor().setSlotKind(SlotIndicies.THIS_CONTEXT.ordinal(), FrameSlotKind.Object);
        frame.setObject(SlotIndicies.THIS_CONTEXT.ordinal(), context);
    }

    public static int getInstructionPointer(final Frame frame) {
        return frame.getIntStatic(SlotIndicies.INSTRUCTION_POINTER.ordinal());
    }

    public static void setInstructionPointer(final Frame frame, final int value) {
        frame.setIntStatic(SlotIndicies.INSTRUCTION_POINTER.ordinal(), value);
    }

    public static int getStackPointer(final Frame frame) {
        return frame.getIntStatic(SlotIndicies.STACK_POINTER.ordinal());
    }

    public static void setStackPointer(final Frame frame, final int value) {
        frame.setIntStatic(SlotIndicies.STACK_POINTER.ordinal(), value);
    }

    public static int toStackSlotIndex(final Frame frame, final int index) {
        assert frame.getArguments().length - getArgumentStartIndex() <= index;
        return SlotIndicies.STACK_START.ordinal() + index;
    }

    public static Object getStackValue(final Frame frame, final int stackIndex, final int numArguments) {
        if (stackIndex < numArguments) {
            return getArgument(frame, stackIndex);
        } else {
            return getSlotValue(frame, toStackSlotIndex(frame, stackIndex));
        }
    }

    public static int getNumStackSlots(final Frame frame) {
        return frame.getFrameDescriptor().getNumberOfSlots() - SlotIndicies.STACK_START.ordinal();
    }

    /* Iterates used stack slots (may not be ordered). */
    public static void iterateStackSlots(final Frame frame, final Consumer<Integer> action) {
        // All slots after fourth slot for stackPointer
        for (int i = SlotIndicies.STACK_START.ordinal(); i < frame.getFrameDescriptor().getNumberOfSlots(); i++) {
            action.accept(i);
        }
    }

    public static Object getSlotValue(final Frame frame, final int slotIndex) {
        final int numberOfSlots = frame.getFrameDescriptor().getNumberOfSlots();
        if (slotIndex < numberOfSlots) {
            return frame.getValue(slotIndex);
        } else {
            final int auxiliarySlotIndex = frame.getFrameDescriptor().findOrAddAuxiliarySlot(slotIndex);
            return frame.getAuxiliarySlot(auxiliarySlotIndex);
        }
    }

    /** Write to a frame slot (slow operation), prefer {@link FrameStackPushNode}. */
    public static void setStackSlot(final Frame frame, final int stackIndex, final Object value) {
        setSlot(frame, SlotIndicies.STACK_START.ordinal() + stackIndex, value);
    }

    public static void setSlot(final Frame frame, final int slotIndex, final Object value) {
        final FrameDescriptor frameDescriptor = frame.getFrameDescriptor();
        assert SlotIndicies.STACK_START.ordinal() <= slotIndex && slotIndex <= SlotIndicies.STACK_START.ordinal() + getCodeObject(frame).getSqueakContextSize();
        final int numberOfSlots = frame.getFrameDescriptor().getNumberOfSlots();
        if (slotIndex < numberOfSlots) {
            final FrameSlotKind frameSlotKind = frameDescriptor.getSlotKind(slotIndex);
            final boolean isIllegal = frameSlotKind == FrameSlotKind.Illegal;
            if (value instanceof final Boolean b && (isIllegal || frameSlotKind == FrameSlotKind.Boolean)) {
                frameDescriptor.setSlotKind(slotIndex, FrameSlotKind.Boolean);
                frame.setBoolean(slotIndex, b);
            } else if (value instanceof final Long l && (isIllegal || frameSlotKind == FrameSlotKind.Long)) {
                frameDescriptor.setSlotKind(slotIndex, FrameSlotKind.Long);
                frame.setLong(slotIndex, l);
            } else if (value instanceof final Double d && (isIllegal || frameSlotKind == FrameSlotKind.Double)) {
                frameDescriptor.setSlotKind(slotIndex, FrameSlotKind.Double);
                frame.setDouble(slotIndex, d);
            } else {
                frameDescriptor.setSlotKind(slotIndex, FrameSlotKind.Object);
                frame.setObject(slotIndex, value);
            }
        } else {
            final int auxiliarySlotIndex = frame.getFrameDescriptor().findOrAddAuxiliarySlot(slotIndex);
            frame.setAuxiliarySlot(auxiliarySlotIndex, value);
        }
    }

    public static boolean hasUnusedAuxiliarySlots(final Frame frame) {
        for (final int slotIndex : frame.getFrameDescriptor().getAuxiliarySlots().values()) {
            if (frame.getAuxiliarySlot(slotIndex) != null) {
                return false;
            }
        }
        return true;
    }

    public static void terminate(final Frame frame) {
        setInstructionPointer(frame, ContextObject.NIL_PC_VALUE);
        setSender(frame, NilObject.SINGLETON);
    }

    public static boolean isTruffleSqueakFrame(final Frame frame) {
        return frame.getArguments().length >= ArgumentIndicies.RECEIVER.ordinal() && frame.getFrameDescriptor().getInfo() instanceof CompiledCodeObject;
    }

    public static Object[] newWith(final Object sender, final BlockClosureObject closure, final Object[] receiverAndArguments) {
        final int receiverAndArgumentsLength = receiverAndArguments.length;
        final Object[] frameArguments = new Object[ArgumentIndicies.RECEIVER.ordinal() + receiverAndArgumentsLength];
        assert sender != null : "Sender should never be null";
        assert receiverAndArgumentsLength > 0 : "At least a receiver must be provided";
        assert receiverAndArguments[0] != null : "Receiver should never be null";
        frameArguments[ArgumentIndicies.SENDER_OR_SENDER_MARKER.ordinal()] = sender;
        frameArguments[ArgumentIndicies.CLOSURE_OR_NULL.ordinal()] = closure;
        ArrayUtils.arraycopy(receiverAndArguments, 0, frameArguments, ArgumentIndicies.RECEIVER.ordinal(), receiverAndArgumentsLength);
        return frameArguments;
    }

    public static Object[] newWith(final Object sender, final BlockClosureObject closure, final Object receiver) {
        assert sender != null && receiver != null : "Sender and receiver should never be null";
        final Object[] frameArguments = new Object[ArgumentIndicies.ARGUMENTS_START.ordinal()];
        frameArguments[ArgumentIndicies.SENDER_OR_SENDER_MARKER.ordinal()] = sender;
        frameArguments[ArgumentIndicies.CLOSURE_OR_NULL.ordinal()] = closure;
        frameArguments[ArgumentIndicies.RECEIVER.ordinal()] = receiver;
        return frameArguments;
    }

    public static Object[] newWith(final Object sender, final BlockClosureObject closure, final Object receiver, final Object arg1) {
        assert sender != null && receiver != null : "Sender and receiver should never be null";
        final Object[] frameArguments = new Object[ArgumentIndicies.ARGUMENTS_START.ordinal() + 1];
        frameArguments[ArgumentIndicies.SENDER_OR_SENDER_MARKER.ordinal()] = sender;
        frameArguments[ArgumentIndicies.CLOSURE_OR_NULL.ordinal()] = closure;
        frameArguments[ArgumentIndicies.RECEIVER.ordinal()] = receiver;
        frameArguments[ArgumentIndicies.ARGUMENTS_START.ordinal()] = arg1;
        return frameArguments;
    }

    public static Object[] newWith(final Object sender, final BlockClosureObject closure, final Object receiver, final Object arg1, final Object arg2) {
        assert sender != null && receiver != null : "Sender and receiver should never be null";
        final Object[] frameArguments = new Object[ArgumentIndicies.ARGUMENTS_START.ordinal() + 2];
        frameArguments[ArgumentIndicies.SENDER_OR_SENDER_MARKER.ordinal()] = sender;
        frameArguments[ArgumentIndicies.CLOSURE_OR_NULL.ordinal()] = closure;
        frameArguments[ArgumentIndicies.RECEIVER.ordinal()] = receiver;
        frameArguments[ArgumentIndicies.ARGUMENTS_START.ordinal()] = arg1;
        frameArguments[ArgumentIndicies.ARGUMENTS_START.ordinal() + 1] = arg2;
        return frameArguments;
    }

    public static Object[] newWith(final Object sender, final BlockClosureObject closure, final Object receiver, final Object arg1, final Object arg2, final Object arg3) {
        assert sender != null && receiver != null : "Sender and receiver should never be null";
        final Object[] frameArguments = new Object[ArgumentIndicies.ARGUMENTS_START.ordinal() + 3];
        frameArguments[ArgumentIndicies.SENDER_OR_SENDER_MARKER.ordinal()] = sender;
        frameArguments[ArgumentIndicies.CLOSURE_OR_NULL.ordinal()] = closure;
        frameArguments[ArgumentIndicies.RECEIVER.ordinal()] = receiver;
        frameArguments[ArgumentIndicies.ARGUMENTS_START.ordinal()] = arg1;
        frameArguments[ArgumentIndicies.ARGUMENTS_START.ordinal() + 1] = arg2;
        frameArguments[ArgumentIndicies.ARGUMENTS_START.ordinal() + 2] = arg3;
        return frameArguments;
    }

    public static Object[] newWith(final Object sender, final BlockClosureObject closure, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4) {
        assert sender != null && receiver != null : "Sender and receiver should never be null";
        final Object[] frameArguments = new Object[ArgumentIndicies.ARGUMENTS_START.ordinal() + 4];
        frameArguments[ArgumentIndicies.SENDER_OR_SENDER_MARKER.ordinal()] = sender;
        frameArguments[ArgumentIndicies.CLOSURE_OR_NULL.ordinal()] = closure;
        frameArguments[ArgumentIndicies.RECEIVER.ordinal()] = receiver;
        frameArguments[ArgumentIndicies.ARGUMENTS_START.ordinal()] = arg1;
        frameArguments[ArgumentIndicies.ARGUMENTS_START.ordinal() + 1] = arg2;
        frameArguments[ArgumentIndicies.ARGUMENTS_START.ordinal() + 2] = arg3;
        frameArguments[ArgumentIndicies.ARGUMENTS_START.ordinal() + 3] = arg4;
        return frameArguments;
    }

    public static Object[] newWith(final Object sender, final BlockClosureObject closure, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4,
                    final Object arg5) {
        assert sender != null && receiver != null : "Sender and receiver should never be null";
        final Object[] frameArguments = new Object[ArgumentIndicies.ARGUMENTS_START.ordinal() + 5];
        frameArguments[ArgumentIndicies.SENDER_OR_SENDER_MARKER.ordinal()] = sender;
        frameArguments[ArgumentIndicies.CLOSURE_OR_NULL.ordinal()] = closure;
        frameArguments[ArgumentIndicies.RECEIVER.ordinal()] = receiver;
        frameArguments[ArgumentIndicies.ARGUMENTS_START.ordinal()] = arg1;
        frameArguments[ArgumentIndicies.ARGUMENTS_START.ordinal() + 1] = arg2;
        frameArguments[ArgumentIndicies.ARGUMENTS_START.ordinal() + 2] = arg3;
        frameArguments[ArgumentIndicies.ARGUMENTS_START.ordinal() + 3] = arg4;
        frameArguments[ArgumentIndicies.ARGUMENTS_START.ordinal() + 4] = arg5;
        return frameArguments;
    }

    public static Object[] newWith(final Object sender, final BlockClosureObject closure, final Object receiver, final Object[] arguments) {
        assert sender != null && receiver != null : "Sender and receiver should never be null";
        final Object[] frameArguments = new Object[ArgumentIndicies.ARGUMENTS_START.ordinal() + arguments.length];
        frameArguments[ArgumentIndicies.SENDER_OR_SENDER_MARKER.ordinal()] = sender;
        frameArguments[ArgumentIndicies.CLOSURE_OR_NULL.ordinal()] = closure;
        frameArguments[ArgumentIndicies.RECEIVER.ordinal()] = receiver;
        ArrayUtils.arraycopy(arguments, 0, frameArguments, ArgumentIndicies.ARGUMENTS_START.ordinal(), arguments.length);
        return frameArguments;
    }

    public static Object[] newWith(final int numArgs) {
        final Object[] frameArguments = new Object[ArgumentIndicies.ARGUMENTS_START.ordinal() + numArgs];
        frameArguments[ArgumentIndicies.SENDER_OR_SENDER_MARKER.ordinal()] = NilObject.SINGLETON;
        return frameArguments;
    }

    public static Object[] newDNUWith(final Object sender, final Object receiver, final PointersObject message) {
        return newWith(sender, null, receiver, message);
    }

    public static Object[] newOAMWith(final Object sender, final Object object, final NativeObject selector, final ArrayObject arguments, final Object receiver) {
        return newWith(sender, null, object, selector, arguments, receiver);
    }

    /* Template because closure arguments still need to be filled in. */
    public static Object[] newClosureArgumentsTemplate(final BlockClosureObject closure, final Object senderOrMarker, final int numArgs) {
        final Object[] copied = closure.getCopiedValues();
        final int numCopied = copied.length;
        assert closure.getNumArgs() == numArgs : "number of required and provided block arguments do not match";
        final Object[] arguments = new Object[ArgumentIndicies.ARGUMENTS_START.ordinal() + numArgs + numCopied];
        // Sender is thisContext (or marker)
        arguments[ArgumentIndicies.SENDER_OR_SENDER_MARKER.ordinal()] = senderOrMarker;
        arguments[ArgumentIndicies.CLOSURE_OR_NULL.ordinal()] = closure;
        arguments[ArgumentIndicies.RECEIVER.ordinal()] = closure.getReceiver();
        assert arguments[ArgumentIndicies.RECEIVER.ordinal()] != null;
        ArrayUtils.arraycopy(copied, 0, arguments, ArgumentIndicies.ARGUMENTS_START.ordinal() + numArgs, numCopied);
        return arguments;
    }

    public static Object[] createFrameArguments(final VirtualFrame frame, final BlockClosureObject closure, final GetContextOrMarkerNode getContextOrMarkerNode) {
        return FrameAccess.newClosureArgumentsTemplate(closure, getContextOrMarkerNode.execute(frame), 0);
    }

    public static Object[] createFrameArguments(final VirtualFrame frame, final BlockClosureObject closure, final GetContextOrMarkerNode getContextOrMarkerNode, final Object arg1) {
        final Object[] frameArguments = FrameAccess.newClosureArgumentsTemplate(closure, getContextOrMarkerNode.execute(frame), 1);
        frameArguments[FrameAccess.getArgumentStartIndex()] = arg1;
        return frameArguments;
    }

    public static int expectedArgumentSize(final int numArgsAndCopied) {
        return ArgumentIndicies.ARGUMENTS_START.ordinal() + numArgsAndCopied;
    }

    public static void assertSenderNotNull(final Frame frame) {
        assert getSender(frame) != null : "Sender should not be null";
    }

    public static void assertReceiverNotNull(final Frame frame) {
        assert getReceiver(frame) != null : "Receiver should not be null";
    }

    @TruffleBoundary
    public static MaterializedFrame findFrameForMarker(final FrameMarker frameMarker) {
        CompilerDirectives.bailout("Finding materializable frames should never be part of compiled code as it triggers deopts");
        LogUtils.ITERATE_FRAMES.fine("Iterating frames to find a marker...");
        final Frame frame = Truffle.getRuntime().iterateFrames(frameInstance -> {
            final Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.READ_ONLY);
            if (!isTruffleSqueakFrame(current)) {
                return null;
            }
            LogUtils.ITERATE_FRAMES.fine(() -> "..." + FrameAccess.getCodeObject(current).toString());
            if (frameMarker == getMarker(current)) {
                return frameInstance.getFrame(FrameInstance.FrameAccess.MATERIALIZE);
            }
            return null;
        });
        if (frame == null) {
            throw SqueakException.create("Could not find frame for:", frameMarker);
        } else {
            return frame.materialize();
        }
    }
}
