/*
 * Copyright (c) 2017-2021 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.util;

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
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject.SLOT_IDENTIFIER;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.FrameMarker;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameSlotReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackPushNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetContextOrMarkerNode;

/**
 * TruffleSqueak frame argument layout.
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
 * stackSlots[]       -> | Object[] (specialized)        |
 *                       +-------------------------------+
 * </pre>
 */
public final class FrameAccess {
    private enum ArgumentIndicies {
        CODE_OBJECT, // 0
        SENDER_OR_SENDER_MARKER, // 1
        CLOSURE_OR_NULL, // 2
        RECEIVER, // 3
        ARGUMENTS_START, // 4
    }

    private FrameAccess() {
    }

    public static CompiledCodeObject getCodeObject(final Frame frame) {
        return (CompiledCodeObject) frame.getArguments()[ArgumentIndicies.CODE_OBJECT.ordinal()];
    }

    public static void setCodeObject(final Frame frame, final CompiledCodeObject method) {
        frame.getArguments()[ArgumentIndicies.CODE_OBJECT.ordinal()] = method;
    }

    /* Returns the code object matching the frame's descriptor. */
    public static CompiledCodeObject getMethodOrBlock(final Frame frame) {
        final BlockClosureObject closure = getClosure(frame);
        if (closure == null) {
            return getCodeObject(frame);
        } else {
            return closure.getCompiledBlock();
        }
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
        return getClosure(frame) != null;
    }

    public static void setClosure(final Frame frame, final BlockClosureObject closure) {
        frame.getArguments()[ArgumentIndicies.CLOSURE_OR_NULL.ordinal()] = closure;
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

    public static int getArgumentStartIndex() {
        return ArgumentIndicies.ARGUMENTS_START.ordinal();
    }

    public static void setArgumentIfInRange(final Frame frame, final int index, final Object value) {
        assert index >= 0;
        final Object[] frameArguments = frame.getArguments();
        final int argumentIndex = ArgumentIndicies.ARGUMENTS_START.ordinal() + index;
        if (argumentIndex < frameArguments.length) {
            frameArguments[argumentIndex] = value;
        }
    }

    public static Object[] getReceiverAndArguments(final Frame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return Arrays.copyOfRange(frame.getArguments(), ArgumentIndicies.RECEIVER.ordinal(), frame.getArguments().length);
    }

    public static FrameSlot getMarkerSlot(final Frame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return frame.getFrameDescriptor().findFrameSlot(SLOT_IDENTIFIER.THIS_MARKER);
    }

    public static FrameMarker getMarkerSlow(final Frame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return getMarker(frame, getMethodOrBlock(frame));
    }

    public static FrameMarker getMarker(final Frame frame, final FrameSlot thisMarkerSlot) {
        return (FrameMarker) FrameUtil.getObjectSafe(frame, thisMarkerSlot);
    }

    public static FrameMarker getMarker(final Frame frame, final CompiledCodeObject blockOrMethod) {
        return getMarker(frame, blockOrMethod.getThisMarkerSlot());
    }

    public static void setMarker(final Frame frame, final FrameSlot thisMarkerSlot, final FrameMarker marker) {
        frame.setObject(thisMarkerSlot, marker);
    }

    public static void initializeMarker(final Frame frame, final CompiledCodeObject code) {
        setMarker(frame, code.getThisMarkerSlot(), new FrameMarker());
    }

    public static Object getContextOrMarkerSlow(final VirtualFrame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return GetContextOrMarkerNode.getNotProfiled(frame);
    }

    public static FrameSlot getContextSlot(final Frame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return frame.getFrameDescriptor().findFrameSlot(SLOT_IDENTIFIER.THIS_CONTEXT);
    }

    public static ContextObject getContextSlow(final Frame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return getContext(frame, getMethodOrBlock(frame));
    }

    public static ContextObject getContext(final Frame frame, final FrameSlot thisContextSlot) {
        return (ContextObject) FrameUtil.getObjectSafe(frame, thisContextSlot);
    }

    public static ContextObject getContext(final Frame frame, final CompiledCodeObject blockOrMethod) {
        return getContext(frame, blockOrMethod.getThisContextSlot());
    }

    public static void setContext(final Frame frame, final CompiledCodeObject blockOrMethod, final ContextObject context) {
        final FrameSlot thisContextSlot = blockOrMethod.getThisContextSlot();
        assert getContext(frame, blockOrMethod) == null : "ContextObject already allocated";
        blockOrMethod.getFrameDescriptor().setFrameSlotKind(thisContextSlot, FrameSlotKind.Object);
        frame.setObject(thisContextSlot, context);
    }

    public static FrameSlot getInstructionPointerSlot(final Frame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return frame.getFrameDescriptor().findFrameSlot(SLOT_IDENTIFIER.INSTRUCTION_POINTER);
    }

    public static int getInstructionPointerSlow(final Frame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return FrameUtil.getIntSafe(frame, getMethodOrBlock(frame).getInstructionPointerSlot());
    }

    public static int getInstructionPointer(final Frame frame, final CompiledCodeObject code) {
        return FrameUtil.getIntSafe(frame, code.getInstructionPointerSlot());
    }

    public static void setInstructionPointer(final Frame frame, final FrameSlot instructionPointerSlot, final int value) {
        frame.setInt(instructionPointerSlot, value);
    }

    public static void setInstructionPointer(final Frame frame, final CompiledCodeObject code, final int value) {
        setInstructionPointer(frame, code.getInstructionPointerSlot(), value);
    }

    public static FrameSlot getStackPointerSlot(final Frame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return frame.getFrameDescriptor().findFrameSlot(SLOT_IDENTIFIER.STACK_POINTER);
    }

    public static int getStackPointerSlow(final Frame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return FrameUtil.getIntSafe(frame, getMethodOrBlock(frame).getStackPointerSlot());
    }

    public static int getStackPointer(final Frame frame, final FrameSlot stackPointerSlot) {
        return FrameUtil.getIntSafe(frame, stackPointerSlot);
    }

    public static int getStackPointer(final Frame frame, final CompiledCodeObject code) {
        return getStackPointer(frame, code.getStackPointerSlot());
    }

    public static void setStackPointer(final Frame frame, final FrameSlot stackPointerSlot, final int value) {
        frame.setInt(stackPointerSlot, value);
    }

    public static void setStackPointer(final Frame frame, final CompiledCodeObject code, final int value) {
        setStackPointer(frame, code.getStackPointerSlot(), value);
    }

    public static FrameSlot getStackSlotSlow(final Frame frame, final int index) {
        CompilerAsserts.neverPartOfCompilation();
        return getMethodOrBlock(frame).getStackSlot(index);
    }

    /** Write to a frame slot (slow operation), prefer {@link FrameStackPushNode}. */
    public static void setStackSlot(final Frame frame, final FrameSlot frameSlot, final Object value) {
        final FrameDescriptor frameDescriptor = frame.getFrameDescriptor();
        assert frame.getFrameDescriptor().getSlots().contains(frameSlot);
        final FrameSlotKind frameSlotKind = frameDescriptor.getFrameSlotKind(frameSlot);
        final boolean isIllegal = frameSlotKind == FrameSlotKind.Illegal;
        if (value instanceof Boolean && (isIllegal || frameSlotKind == FrameSlotKind.Boolean)) {
            frameDescriptor.setFrameSlotKind(frameSlot, FrameSlotKind.Boolean);
            frame.setBoolean(frameSlot, (boolean) value);
        } else if (value instanceof Long && (isIllegal || frameSlotKind == FrameSlotKind.Long)) {
            frameDescriptor.setFrameSlotKind(frameSlot, FrameSlotKind.Long);
            frame.setLong(frameSlot, (long) value);
        } else if (value instanceof Double && (isIllegal || frameSlotKind == FrameSlotKind.Double)) {
            frameDescriptor.setFrameSlotKind(frameSlot, FrameSlotKind.Double);
            frame.setDouble(frameSlot, (double) value);
        } else {
            frameDescriptor.setFrameSlotKind(frameSlot, FrameSlotKind.Object);
            frame.setObject(frameSlot, value);
        }
    }

    public static void terminate(final Frame frame, final FrameSlot instructionPointerSlot) {
        setInstructionPointer(frame, instructionPointerSlot, ContextObject.NIL_PC_VALUE);
        setSender(frame, NilObject.SINGLETON);
    }

    public static boolean isTruffleSqueakFrame(final Frame frame) {
        final Object[] arguments = frame.getArguments();
        return arguments.length >= ArgumentIndicies.RECEIVER.ordinal() && arguments[ArgumentIndicies.CODE_OBJECT.ordinal()] instanceof CompiledCodeObject;
    }

    public static Object[] newWith(final CompiledCodeObject method, final Object sender, final BlockClosureObject closure, final Object[] receiverAndArguments) {
        final int receiverAndArgumentsLength = receiverAndArguments.length;
        final Object[] frameArguments = new Object[ArgumentIndicies.RECEIVER.ordinal() + receiverAndArgumentsLength];
        assert method != null : "Method should never be null";
        assert sender != null : "Sender should never be null";
        assert receiverAndArgumentsLength > 0 : "At least a receiver must be provided";
        assert receiverAndArguments[0] != null : "Receiver should never be null";
        frameArguments[ArgumentIndicies.CODE_OBJECT.ordinal()] = method;
        frameArguments[ArgumentIndicies.SENDER_OR_SENDER_MARKER.ordinal()] = sender;
        assertCodeAndClosure(closure, method);
        frameArguments[ArgumentIndicies.CLOSURE_OR_NULL.ordinal()] = closure;
        System.arraycopy(receiverAndArguments, 0, frameArguments, ArgumentIndicies.RECEIVER.ordinal(), receiverAndArgumentsLength);
        return frameArguments;
    }

    @ExplodeLoop
    public static Object[] newWith(final VirtualFrame frame, final CompiledCodeObject method, final Object sender, final FrameSlotReadNode[] receiverAndArgumentsNodes) {
        final int numReceiverAndArguments = receiverAndArgumentsNodes.length;
        CompilerAsserts.partialEvaluationConstant(numReceiverAndArguments);
        final Object[] frameArguments = new Object[ArgumentIndicies.RECEIVER.ordinal() + numReceiverAndArguments];
        assert method != null : "Method should never be null";
        assert sender != null : "Sender should never be null";
        assert numReceiverAndArguments > 0 : "At least a receiver must be provided";
        frameArguments[ArgumentIndicies.CODE_OBJECT.ordinal()] = method;
        frameArguments[ArgumentIndicies.SENDER_OR_SENDER_MARKER.ordinal()] = sender;
        frameArguments[ArgumentIndicies.CLOSURE_OR_NULL.ordinal()] = null;
        for (int i = 0; i < numReceiverAndArguments; i++) {
            frameArguments[ArgumentIndicies.RECEIVER.ordinal() + i] = receiverAndArgumentsNodes[i].executeRead(frame);
        }
        return frameArguments;
    }

    @ExplodeLoop
    public static Object[] newWith(final VirtualFrame frame, final CompiledCodeObject method, final Object sender, final Object receiver, final FrameSlotReadNode[] argumentsNodes) {
        final int argumentCount = argumentsNodes.length;
        CompilerAsserts.partialEvaluationConstant(argumentCount);
        final Object[] frameArguments = new Object[ArgumentIndicies.ARGUMENTS_START.ordinal() + argumentCount];
        assert method != null : "Method should never be null";
        assert sender != null : "Sender should never be null";
        frameArguments[ArgumentIndicies.CODE_OBJECT.ordinal()] = method;
        frameArguments[ArgumentIndicies.SENDER_OR_SENDER_MARKER.ordinal()] = sender;
        frameArguments[ArgumentIndicies.CLOSURE_OR_NULL.ordinal()] = null;
        frameArguments[ArgumentIndicies.RECEIVER.ordinal()] = receiver;
        for (int i = 0; i < argumentCount; i++) {
            frameArguments[ArgumentIndicies.ARGUMENTS_START.ordinal() + i] = argumentsNodes[i].executeRead(frame);
        }
        return frameArguments;
    }

    public static Object[] newDNUWith(final CompiledCodeObject method, final Object sender, final Object receiver, final PointersObject message) {
        final Object[] frameArguments = new Object[ArgumentIndicies.RECEIVER.ordinal() + 2];
        assert method != null : "Method should never be null";
        assert sender != null : "Sender should never be null";
        frameArguments[ArgumentIndicies.CODE_OBJECT.ordinal()] = method;
        frameArguments[ArgumentIndicies.SENDER_OR_SENDER_MARKER.ordinal()] = sender;
        frameArguments[ArgumentIndicies.CLOSURE_OR_NULL.ordinal()] = null;
        frameArguments[ArgumentIndicies.RECEIVER.ordinal()] = receiver;
        frameArguments[ArgumentIndicies.RECEIVER.ordinal() + 1] = message;
        return frameArguments;
    }

    public static Object[] newOAMWith(final CompiledCodeObject method, final Object sender, final Object object, final NativeObject selector, final ArrayObject arguments, final Object receiver) {
        final Object[] frameArguments = new Object[ArgumentIndicies.RECEIVER.ordinal() + 4];
        assert method != null : "Method should never be null";
        assert sender != null : "Sender should never be null";
        frameArguments[ArgumentIndicies.CODE_OBJECT.ordinal()] = method;
        frameArguments[ArgumentIndicies.SENDER_OR_SENDER_MARKER.ordinal()] = sender;
        frameArguments[ArgumentIndicies.CLOSURE_OR_NULL.ordinal()] = null;
        frameArguments[ArgumentIndicies.RECEIVER.ordinal()] = object;
        frameArguments[ArgumentIndicies.RECEIVER.ordinal() + 1] = selector;
        frameArguments[ArgumentIndicies.RECEIVER.ordinal() + 2] = arguments;
        frameArguments[ArgumentIndicies.RECEIVER.ordinal() + 3] = receiver;
        return frameArguments;
    }

    public static Object[] newDummyWith(final CompiledCodeObject code, final Object sender, final BlockClosureObject closure, final Object[] receiverAndArguments) {
        final int receiverAndArgumentsLength = receiverAndArguments.length;
        final Object[] frameArguments = new Object[ArgumentIndicies.RECEIVER.ordinal() + receiverAndArgumentsLength];
        assert sender != null : "Sender should never be null";
        assert receiverAndArgumentsLength > 0 : "At least a receiver must be provided";
        frameArguments[ArgumentIndicies.CODE_OBJECT.ordinal()] = code;
        frameArguments[ArgumentIndicies.SENDER_OR_SENDER_MARKER.ordinal()] = sender;
        assertCodeAndClosure(closure, code);
        frameArguments[ArgumentIndicies.CLOSURE_OR_NULL.ordinal()] = closure;
        System.arraycopy(receiverAndArguments, 0, frameArguments, ArgumentIndicies.RECEIVER.ordinal(), receiverAndArgumentsLength);
        return frameArguments;
    }

    /* Template because closure arguments still need to be filled in. */
    public static Object[] newClosureArgumentsTemplate(final BlockClosureObject closure, final Object senderOrMarker, final int numArgs) {
        return newClosureArgumentsTemplate(closure, closure.getCompiledBlock().getOuterMethod(), senderOrMarker, numArgs);
    }

    public static Object[] newFullClosureArgumentsTemplate(final BlockClosureObject closure, final Object senderOrMarker, final int numArgs) {
        return newClosureArgumentsTemplate(closure, closure.getCompiledBlock(), senderOrMarker, numArgs);
    }

    /* Template because closure arguments still need to be filled in. */
    public static Object[] newClosureArgumentsTemplate(final BlockClosureObject closure, final CompiledCodeObject code, final Object senderOrMarker, final int numArgs) {
        final Object[] copied = closure.getCopiedValues();
        final int numCopied = copied.length;
        assert closure.getNumArgs() == numArgs : "number of required and provided block arguments do not match";
        final Object[] arguments = new Object[ArgumentIndicies.ARGUMENTS_START.ordinal() + numArgs + numCopied];
        arguments[ArgumentIndicies.CODE_OBJECT.ordinal()] = code;
        // Sender is thisContext (or marker)
        arguments[ArgumentIndicies.SENDER_OR_SENDER_MARKER.ordinal()] = senderOrMarker;
        arguments[ArgumentIndicies.CLOSURE_OR_NULL.ordinal()] = closure;
        arguments[ArgumentIndicies.RECEIVER.ordinal()] = closure.getReceiver();
        assert arguments[ArgumentIndicies.RECEIVER.ordinal()] != null;
        System.arraycopy(copied, 0, arguments, ArgumentIndicies.ARGUMENTS_START.ordinal() + numArgs, numCopied);
        return arguments;
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
            if (frameMarker == getMarkerSlow(current)) {
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

    private static void assertCodeAndClosure(final BlockClosureObject closure, final CompiledCodeObject code) {
        assert closure == null || code.isCompiledBlock() || code == closure.getCompiledBlock() ||
                        code == closure.getCompiledBlock().getOuterMethod() : "Frame's code object must be closure's outer method";
    }
}
