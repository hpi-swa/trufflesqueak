/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.util;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;

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
import de.hpi.swa.trufflesqueak.model.ContextObject.FrameHandling;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.context.GetOrCreateContextWithoutFrameNode;

/**
 * TruffleSqueak frame argument layout.
 *
 * <pre>
 *                            +---------------------------------+
 * SENDER                  -> | ContextObject                   |
 *                            | nil (end of sender chain)       |
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
 * <p>
 * TruffleSqueak frame slot layout.
 *
 * <pre>
 *                       +-------------------------------+
 * thisContext        -> | ContextObject                 |
 *                       +-------------------------------+
 * instructionPointer -> | int (< 0 if terminated        |
 *                       +-------------------------------+
 * stackPointer       -> | int                           |
 *                       +-------------------------------+
 * stackSlots         -> | Object[] (specialized)        |
 *                       +-------------------------------+
 * </pre>
 */
public final class FrameAccess {

    private static final class ArgumentIndices {
        private static final int SENDER = 0;
        private static final int CLOSURE_OR_NULL = 1;
        private static final int RECEIVER = 2;
        private static final int ARGUMENTS_START = 3;
    }

    private static final class SlotIndices {
        private static final int THIS_CONTEXT = 0;
        private static final int INSTRUCTION_POINTER = 1;
        private static final int STACK_POINTER = 2;
        private static final int STACK_START = 3;
    }

    private FrameAccess() {
    }

    /**
     * Creates a new {@link FrameDescriptor} according to {@link SlotIndices}.
     */
    public static FrameDescriptor newFrameDescriptor(final CompiledCodeObject code, final int numStackSlots) {
        final int numSlots = SlotIndices.STACK_START + numStackSlots;
        final Builder builder = FrameDescriptor.newBuilder(numSlots).info(code);
        builder.addSlots(numSlots, FrameSlotKind.Static);
        return builder.build();
    }

    public static void copyAllSlots(final MaterializedFrame source, final MaterializedFrame destination) {
        source.copyTo(0, destination, 0, source.getFrameDescriptor().getNumberOfSlots());
    }

    /* Returns the code object matching the frame's descriptor. */
    public static CompiledCodeObject getCodeObject(final Frame frame) {
        return (CompiledCodeObject) frame.getFrameDescriptor().getInfo();
    }

    public static AbstractSqueakObject getSender(final Frame frame) {
        return (AbstractSqueakObject) frame.getArguments()[ArgumentIndices.SENDER];
    }

    public static ContextObject getSenderContext(final Frame frame) {
        return (ContextObject) getSender(frame);
    }

    public static void setSender(final Frame frame, final AbstractSqueakObject value) {
        frame.getArguments()[ArgumentIndices.SENDER] = value;
    }

    public static BlockClosureObject getClosure(final Frame frame) {
        return (BlockClosureObject) frame.getArguments()[ArgumentIndices.CLOSURE_OR_NULL];
    }

    public static boolean hasClosure(final Frame frame) {
        return frame.getArguments()[ArgumentIndices.CLOSURE_OR_NULL] instanceof BlockClosureObject;
    }

    public static void setClosure(final Frame frame, final BlockClosureObject closure) {
        frame.getArguments()[ArgumentIndices.CLOSURE_OR_NULL] = closure;
    }

    public static Object[] storeParentFrameInArguments(final VirtualFrame parentFrame) {
        assert !hasClosure(parentFrame);
        final Object[] arguments = parentFrame.getArguments();
        arguments[ArgumentIndices.CLOSURE_OR_NULL] = parentFrame;
        return arguments;
    }

    public static Frame restoreParentFrameFromArguments(final Object[] arguments) {
        final Object frame = arguments[ArgumentIndices.CLOSURE_OR_NULL];
        arguments[ArgumentIndices.CLOSURE_OR_NULL] = null;
        return (Frame) frame;
    }

    public static Object getReceiver(final Frame frame) {
        return frame.getArguments()[ArgumentIndices.RECEIVER];
    }

    public static void setReceiver(final Frame frame, final Object receiver) {
        frame.getArguments()[ArgumentIndices.RECEIVER] = receiver;
    }

    public static Object getArgument(final Frame frame, final int index) {
        return frame.getArguments()[ArgumentIndices.RECEIVER + index];
    }

    public static int getReceiverStartIndex() {
        return ArgumentIndices.RECEIVER;
    }

    public static int getArgumentStartIndex() {
        return ArgumentIndices.ARGUMENTS_START;
    }

    public static int getNumArguments(final Frame frame) {
        return frame.getArguments().length - getArgumentStartIndex();
    }

    public static Object[] getReceiverAndArguments(final Frame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return Arrays.copyOfRange(frame.getArguments(), getReceiverStartIndex(), frame.getArguments().length);
    }

    public static ContextObject getContext(final Frame frame) {
        return (ContextObject) frame.getObjectStatic(SlotIndices.THIS_CONTEXT);
    }

    public static boolean hasModifiedSender(final VirtualFrame frame) {
        final ContextObject context = getContext(frame);
        return context != null && context.hasModifiedSender();
    }

    public static void setContext(final Frame frame, final ContextObject context) {
        assert getContext(frame) == null || getContext(frame) == context : "ContextObject already allocated";
        frame.setObjectStatic(SlotIndices.THIS_CONTEXT, context);
    }

    public static int getInstructionPointer(final Frame frame) {
        return frame.getIntStatic(SlotIndices.INSTRUCTION_POINTER);
    }

    public static boolean isDead(final Frame frame) {
        return getInstructionPointer(frame) < ContextObject.NIL_PC_THRESHOLD;
    }

    public static void setInstructionPointer(final Frame frame, final int value) {
        frame.setIntStatic(SlotIndices.INSTRUCTION_POINTER, value);
    }

    public static int getStackPointer(final Frame frame) {
        return frame.getIntStatic(SlotIndices.STACK_POINTER);
    }

    public static void setStackPointer(final Frame frame, final int value) {
        frame.setIntStatic(SlotIndices.STACK_POINTER, value);
    }

    public static void externalizePCAndSP(final VirtualFrame frame, final int pc, final int sp) {
        setInstructionPointer(frame, pc);
        setStackPointer(frame, sp);
    }

    public static int internalizePC(final VirtualFrame frame, final int pc) {
        final int framePC = getInstructionPointer(frame);
        if (pc != framePC) {
            CompilerDirectives.transferToInterpreter();
            return framePC;
        } else {
            return pc;
        }
    }

    public static int getStackStart() {
        return SlotIndices.STACK_START;
    }

    public static int getNumStackSlots(final Frame frame) {
        return frame.getFrameDescriptor().getNumberOfSlots() - SlotIndices.STACK_START;
    }

    public static boolean slotsAreNotNilled(final Frame frame) {
        return getInstructionPointer(frame) == ContextObject.NIL_PC_STACK_NOT_NIL_VALUE;
    }

    public static void setSlotsAreNilled(final Frame frame) {
        setInstructionPointer(frame, ContextObject.NIL_PC_STACK_NIL_VALUE);
    }

    /**
     * The stack of a dead frame is unreachable. Nil it out here.
     * <p>
     * From Squeak6.0-22104 class comment for Context - eem 4/4/2017 17:45
     * <p>
     * "Contexts refer to the context in which they were created via the sender inst var. An
     * execution stack is made up of a linked list of contexts, linked through their sender inst
     * var. Returning involves returning back to the sender. When a context is returned from, its
     * sender and pc are nilled, and, if the context is still referred to, the virtual machine
     * guarantees to preserve only the arguments after a return."
     */
    private static void clearStackSlots(final Frame frame) {
        assert isDead(frame);
        /*
         * Replace all initialized stack slots with null, removing spurious references from the
         * stack.
         */
        if (slotsAreNotNilled(frame)) {
            setSlotsAreNilled(frame);
            final FrameDescriptor frameDescriptor = frame.getFrameDescriptor();
            for (int slotIndex = SlotIndices.STACK_START; slotIndex < frameDescriptor.getNumberOfSlots(); slotIndex++) {
                frame.setObjectStatic(slotIndex, null);
            }
        }
    }

    /**
     * Iterates over the active stack slot indices of a given frame.
     * <p>
     * <strong>Important boundary:</strong> This method strictly iterates from the stack start up to
     * the current stack pointer (exclusive). It <em>does not</em> yield indices at or beyond the
     * active stack pointer.
     * <p>
     * <strong>Write Access Required:</strong> If the frame is dead (terminated), this method will
     * modify the frame by explicitly clearing the remaining slots. Therefore, the {@code frame}
     * must not be read-only in this state (e.g., it requires {@code READ_WRITE} access if obtained
     * via a Truffle {@code FrameInstance}).
     *
     * @param frame the frame to inspect (must be writable if the frame is dead)
     * @param sp the current stack pointer defining the upper boundary
     * @param action the action to perform on each valid slot index
     */
    public static void iterateStackSlots(final Frame frame, final int sp, final Consumer<Integer> action) {
        if (isDead(frame)) {
            clearStackSlots(frame);
        } else {
            /* Iterate defined stack slots only. */
            final int slotLimit = Integer.min(SlotIndices.STACK_START + sp, frame.getFrameDescriptor().getNumberOfSlots());
            for (int slotIndex = SlotIndices.STACK_START; slotIndex < slotLimit; slotIndex++) {
                action.accept(slotIndex);
            }
        }
    }

    /**
     * Iterates over the live stack objects of a given frame, passing them to the provided action.
     * <p>
     * <strong>Important boundary:</strong> The {@code action} is strictly applied to objects from
     * the stack start up to the current stack pointer (exclusive). It <em>does not</em> pass
     * objects located at or beyond the stack pointer to the action.
     * <p>
     * <strong>Write Access Required:</strong> If {@code frameHandling} is set to
     * {@link FrameHandling#SCRUB}, this method will modify the frame by nilling out unreachable
     * stack entries (or the entire stack if dead). The {@code frame} must not be read-only when
     * scrubbing is enabled (e.g., it requires {@code READ_WRITE} access).
     *
     * @param frame the frame to inspect (must be writable if scrubbing)
     * @param frameHandling the handling policy (e.g., SCAN or SCRUB)
     * @param action the action to perform on each live stack object
     */
    public static void iterateStackObjects(final Frame frame, final FrameHandling frameHandling, final Consumer<Object> action) {
        if (isDead(frame)) {
            if (frameHandling == FrameHandling.SCRUB) {
                clearStackSlots(frame);
            }
        } else {
            /* Iterate defined stack slots only. */
            final int sp = getStackPointer(frame);
            final FrameDescriptor frameDescriptor = frame.getFrameDescriptor();
            final int slotCount = frameDescriptor.getNumberOfSlots();
            final int slotLimit = Integer.min(SlotIndices.STACK_START + sp, slotCount);
            for (int slotIndex = SlotIndices.STACK_START; slotIndex < slotLimit; slotIndex++) {
                action.accept(frame.getObjectStatic(slotIndex));
            }
            /* Nil unreachable stack entries. */
            if (frameHandling == FrameHandling.SCRUB) {
                for (int slotIndex = slotLimit; slotIndex < slotCount; slotIndex++) {
                    frame.setObjectStatic(slotIndex, null);
                }
            }
        }
    }

    /**
     * Iterates over the live stack objects of a given frame, allowing the provided function to
     * optionally replace their values.
     * <p>
     * <strong>Important boundary:</strong> This method strictly iterates from the stack start up to
     * the current stack pointer (exclusive). It <em>does not</em> evaluate or replace objects
     * located at or beyond the stack pointer.
     * <p>
     * <strong>Write Access Required:</strong> Because this method explicitly modifies frame slots
     * (either by applying a replacement, or by clearing slots when {@code clearStackSlots} is
     * {@code true} on a dead frame), the {@code frame} must not be a read-only instance under any
     * circumstances (e.g., it requires {@code READ_WRITE} access).
     *
     * @param frame the frame to inspect (must be writable)
     * @param clearStackSlots whether to nil out the stack if the frame is dead
     * @param action a function that returns the replacement object, or null to keep the original
     */
    public static void iterateStackObjectsWithReplacement(final Frame frame, final boolean clearStackSlots, final Function<Object, Object> action) {
        if (isDead(frame)) {
            if (clearStackSlots) {
                clearStackSlots(frame);
            }
        } else {
            /* Iterate defined stack slots only. */
            final int sp = getStackPointer(frame);
            final FrameDescriptor frameDescriptor = frame.getFrameDescriptor();
            final int slotCount = frameDescriptor.getNumberOfSlots();
            final int slotLimit = Integer.min(SlotIndices.STACK_START + sp, slotCount);
            for (int slotIndex = SlotIndices.STACK_START; slotIndex < slotLimit; slotIndex++) {
                final Object replacement = action.apply(frame.getObjectStatic(slotIndex));
                if (replacement != null) {
                    frame.setObjectStatic(slotIndex, replacement);
                }
            }
        }
    }

    public static Object getSlotValue(final Frame frame, final int slotIndex) {
        return frame.getObjectStatic(slotIndex);
    }

    public static void setSlotValue(final Frame frame, final int slotIndex, final Object value) {
        frame.setObjectStatic(slotIndex, value);
    }

    public static Object getStackValue(final Frame frame, final int sp) {
        return getSlotValue(frame, SlotIndices.STACK_START + sp);
    }

    public static void setStackValue(final Frame frame, final int sp, final Object value) {
        setSlotValue(frame, SlotIndices.STACK_START + sp, value);
    }

    public static boolean hasUnusedAuxiliarySlots(final Frame frame) {
        for (final int slotIndex : frame.getFrameDescriptor().getAuxiliarySlots().values()) {
            if (frame.getAuxiliarySlot(slotIndex) != null) {
                return false;
            }
        }
        return true;
    }

    public static void terminateFrame(final Frame frame) {
        setInstructionPointer(frame, ContextObject.NIL_PC_STACK_NOT_NIL_VALUE);
        setSender(frame, NilObject.SINGLETON);
    }

    public static boolean isTruffleSqueakFrame(final Frame frame) {
        return frame.getFrameDescriptor().getInfo() instanceof CompiledCodeObject;
    }

    public static Object[] newWith(final AbstractSqueakObject sender, final BlockClosureObject closure, final Object[] receiverAndArguments) {
        final int receiverAndArgumentsLength = receiverAndArguments.length;
        final Object[] frameArguments = new Object[ArgumentIndices.RECEIVER + receiverAndArgumentsLength];
        assert sender != null : "Sender should never be null";
        assert receiverAndArgumentsLength > 0 : "At least a receiver must be provided";
        assert receiverAndArguments[0] != null : "Receiver should never be null";
        frameArguments[ArgumentIndices.SENDER] = sender;
        frameArguments[ArgumentIndices.CLOSURE_OR_NULL] = closure;
        ArrayUtils.arraycopy(receiverAndArguments, 0, frameArguments, ArgumentIndices.RECEIVER, receiverAndArgumentsLength);
        return frameArguments;
    }

    public static Object[] newWith(final AbstractSqueakObject sender, final BlockClosureObject closure, final Object receiver) {
        assert sender != null && receiver != null : "Sender and receiver should never be null";
        final Object[] frameArguments = new Object[ArgumentIndices.ARGUMENTS_START];
        frameArguments[ArgumentIndices.SENDER] = sender;
        frameArguments[ArgumentIndices.CLOSURE_OR_NULL] = closure;
        frameArguments[ArgumentIndices.RECEIVER] = receiver;
        return frameArguments;
    }

    public static Object[] newWith(final AbstractSqueakObject sender, final BlockClosureObject closure, final Object receiver, final Object arg1) {
        assert sender != null && receiver != null : "Sender and receiver should never be null";
        final Object[] frameArguments = new Object[ArgumentIndices.ARGUMENTS_START + 1];
        frameArguments[ArgumentIndices.SENDER] = sender;
        frameArguments[ArgumentIndices.CLOSURE_OR_NULL] = closure;
        frameArguments[ArgumentIndices.RECEIVER] = receiver;
        frameArguments[ArgumentIndices.ARGUMENTS_START] = arg1;
        return frameArguments;
    }

    public static Object[] newWith(final AbstractSqueakObject sender, final BlockClosureObject closure, final Object receiver, final Object arg1, final Object arg2) {
        assert sender != null && receiver != null : "Sender and receiver should never be null";
        final Object[] frameArguments = new Object[ArgumentIndices.ARGUMENTS_START + 2];
        frameArguments[ArgumentIndices.SENDER] = sender;
        frameArguments[ArgumentIndices.CLOSURE_OR_NULL] = closure;
        frameArguments[ArgumentIndices.RECEIVER] = receiver;
        frameArguments[ArgumentIndices.ARGUMENTS_START] = arg1;
        frameArguments[ArgumentIndices.ARGUMENTS_START + 1] = arg2;
        return frameArguments;
    }

    public static Object[] newWith(final AbstractSqueakObject sender, final BlockClosureObject closure, final Object receiver, final Object arg1, final Object arg2, final Object arg3) {
        assert sender != null && receiver != null : "Sender and receiver should never be null";
        final Object[] frameArguments = new Object[ArgumentIndices.ARGUMENTS_START + 3];
        frameArguments[ArgumentIndices.SENDER] = sender;
        frameArguments[ArgumentIndices.CLOSURE_OR_NULL] = closure;
        frameArguments[ArgumentIndices.RECEIVER] = receiver;
        frameArguments[ArgumentIndices.ARGUMENTS_START] = arg1;
        frameArguments[ArgumentIndices.ARGUMENTS_START + 1] = arg2;
        frameArguments[ArgumentIndices.ARGUMENTS_START + 2] = arg3;
        return frameArguments;
    }

    public static Object[] newWith(final AbstractSqueakObject sender, final BlockClosureObject closure, final Object receiver, final Object arg1, final Object arg2, final Object arg3,
                    final Object arg4) {
        assert sender != null && receiver != null : "Sender and receiver should never be null";
        final Object[] frameArguments = new Object[ArgumentIndices.ARGUMENTS_START + 4];
        frameArguments[ArgumentIndices.SENDER] = sender;
        frameArguments[ArgumentIndices.CLOSURE_OR_NULL] = closure;
        frameArguments[ArgumentIndices.RECEIVER] = receiver;
        frameArguments[ArgumentIndices.ARGUMENTS_START] = arg1;
        frameArguments[ArgumentIndices.ARGUMENTS_START + 1] = arg2;
        frameArguments[ArgumentIndices.ARGUMENTS_START + 2] = arg3;
        frameArguments[ArgumentIndices.ARGUMENTS_START + 3] = arg4;
        return frameArguments;
    }

    public static Object[] newWith(final AbstractSqueakObject sender, final BlockClosureObject closure, final Object receiver, final Object arg1, final Object arg2, final Object arg3,
                    final Object arg4, final Object arg5) {
        assert sender != null && receiver != null : "Sender and receiver should never be null";
        final Object[] frameArguments = new Object[ArgumentIndices.ARGUMENTS_START + 5];
        frameArguments[ArgumentIndices.SENDER] = sender;
        frameArguments[ArgumentIndices.CLOSURE_OR_NULL] = closure;
        frameArguments[ArgumentIndices.RECEIVER] = receiver;
        frameArguments[ArgumentIndices.ARGUMENTS_START] = arg1;
        frameArguments[ArgumentIndices.ARGUMENTS_START + 1] = arg2;
        frameArguments[ArgumentIndices.ARGUMENTS_START + 2] = arg3;
        frameArguments[ArgumentIndices.ARGUMENTS_START + 3] = arg4;
        frameArguments[ArgumentIndices.ARGUMENTS_START + 4] = arg5;
        return frameArguments;
    }

    public static Object[] newWith(final AbstractSqueakObject sender, final BlockClosureObject closure, final Object receiver, final Object[] arguments) {
        assert sender != null && receiver != null : "Sender and receiver should never be null";
        final Object[] frameArguments = new Object[ArgumentIndices.ARGUMENTS_START + arguments.length];
        frameArguments[ArgumentIndices.SENDER] = sender;
        frameArguments[ArgumentIndices.CLOSURE_OR_NULL] = closure;
        frameArguments[ArgumentIndices.RECEIVER] = receiver;
        ArrayUtils.arraycopy(arguments, 0, frameArguments, ArgumentIndices.ARGUMENTS_START, arguments.length);
        return frameArguments;
    }

    public static Object[] newWith(final int numArgs) {
        final Object[] frameArguments = new Object[ArgumentIndices.ARGUMENTS_START + numArgs];
        frameArguments[ArgumentIndices.SENDER] = NilObject.SINGLETON;
        return frameArguments;
    }

    public static Object[] newDNUWith(final AbstractSqueakObject sender, final Object receiver, final PointersObject message) {
        return newWith(sender, null, receiver, message);
    }

    public static Object[] newOAMWith(final AbstractSqueakObject sender, final Object object, final NativeObject selector, final ArrayObject arguments, final Object receiver) {
        return newWith(sender, null, object, selector, arguments, receiver);
    }

    /* Template because closure arguments still need to be filled in. */
    public static Object[] newClosureArgumentsTemplate(final BlockClosureObject closure, final ContextObject sender, final int numArgs) {
        final Object[] copied = closure.getCopiedValues();
        final int numCopied = copied.length;
        assert closure.getNumArgs() == numArgs : "number of required and provided block arguments do not match";
        final Object[] arguments = new Object[ArgumentIndices.ARGUMENTS_START + numArgs + numCopied];
        arguments[ArgumentIndices.SENDER] = sender;
        arguments[ArgumentIndices.CLOSURE_OR_NULL] = closure;
        arguments[ArgumentIndices.RECEIVER] = closure.getReceiver();
        assert arguments[ArgumentIndices.RECEIVER] != null;
        ArrayUtils.arraycopy(copied, 0, arguments, ArgumentIndices.ARGUMENTS_START + numArgs, numCopied);
        return arguments;
    }

    public static Object[] createFrameArguments(final VirtualFrame frame, final BlockClosureObject closure, final GetOrCreateContextWithoutFrameNode getOrCreateContextWithoutFrameNode) {
        return FrameAccess.newClosureArgumentsTemplate(closure, getOrCreateContextWithoutFrameNode.execute(frame), 0);
    }

    public static Object[] createFrameArguments(final VirtualFrame frame, final BlockClosureObject closure, final GetOrCreateContextWithoutFrameNode getOrCreateContextWithoutFrameNode,
                    final Object arg1) {
        final Object[] frameArguments = FrameAccess.newClosureArgumentsTemplate(closure, getOrCreateContextWithoutFrameNode.execute(frame), 1);
        frameArguments[FrameAccess.getArgumentStartIndex()] = arg1;
        return frameArguments;
    }

    public static int expectedArgumentSize(final int numArgsAndCopied) {
        return ArgumentIndices.ARGUMENTS_START + numArgsAndCopied;
    }

    public static void assertSenderNotNull(final Frame frame) {
        assert getSender(frame) != null : "Sender should not be null";
    }

    public static void assertReceiverNotNull(final Frame frame) {
        assert getReceiver(frame) != null : "Receiver should not be null";
    }

    @TruffleBoundary
    public static MaterializedFrame findFrameForContext(final ContextObject context) {
        CompilerDirectives.bailout("Finding materializable frames should never be part of compiled code as it triggers deopts");
        LogUtils.ITERATE_FRAMES.fine("Iterating frames to find a ContextObject...");
        final MaterializedFrame frame = Truffle.getRuntime().iterateFrames(frameInstance -> {
            final Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.READ_ONLY);
            if (isTruffleSqueakFrame(current)) {
                LogUtils.ITERATE_FRAMES.fine(() -> "..." + FrameAccess.getCodeObject(current).toString());
                final ContextObject contextObject = getContext(current);
                if (context == contextObject) {
                    assert contextObject == null || !contextObject.hasTruffleFrame() : "Redundant frame lookup";
                    return frameInstance.getFrame(FrameInstance.FrameAccess.MATERIALIZE).materialize();
                }
            }
            return null; // continue with next frame
        });
        if (frame != null) {
            return frame;
        } else {
            throw SqueakException.create("Could not find frame for:", context);
        }
    }
}
