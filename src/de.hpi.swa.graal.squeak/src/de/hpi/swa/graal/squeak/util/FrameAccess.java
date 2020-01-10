/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.util;

import java.util.Arrays;
import java.util.logging.Level;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameUtil;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.FrameMarker;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackPushNode;
import de.hpi.swa.graal.squeak.shared.SqueakLanguageConfig;

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
 *
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
public final class FrameAccess {
    private static final TruffleLogger LOG = TruffleLogger.getLogger(SqueakLanguageConfig.ID, FrameAccess.class);
    private static final boolean isLoggingEnabled = LOG.isLoggable(Level.FINE);

    private enum ArgumentIndicies {
        METHOD, // 0
        SENDER_OR_SENDER_MARKER, // 1
        CLOSURE_OR_NULL, // 2
        RECEIVER, // 3
        ARGUMENTS_START, // 4
    }

    private FrameAccess() {
    }

    public static CompiledMethodObject getMethod(final Frame frame) {
        return (CompiledMethodObject) frame.getArguments()[ArgumentIndicies.METHOD.ordinal()];
    }

    public static void setMethod(final Frame frame, final CompiledMethodObject method) {
        frame.getArguments()[ArgumentIndicies.METHOD.ordinal()] = method;
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

    public static void setClosure(final Frame frame, final BlockClosureObject closure) {
        frame.getArguments()[ArgumentIndicies.CLOSURE_OR_NULL.ordinal()] = closure;
    }

    public static CompiledCodeObject getBlockOrMethod(final Frame frame) {
        final BlockClosureObject closure = getClosure(frame);
        return closure != null ? closure.getCompiledBlock() : getMethod(frame);
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
        setMarker(frame, code, new FrameMarker());
    }

    /* Gets context or marker, lazily initializes the latter if necessary. */
    public static Object getContextOrMarker(final Frame frame, final CompiledCodeObject blockOrMethod) {
        final ContextObject context = getContext(frame, blockOrMethod);
        if (context != null) {
            return context;
        } else {
            final FrameMarker marker = getMarker(frame, blockOrMethod);
            if (marker != null) {
                return marker;
            } else {
                final FrameMarker newMarker = new FrameMarker();
                setMarker(frame, blockOrMethod, newMarker);
                return newMarker;
            }
        }
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

    public static int getInstructionPointer(final Frame frame, final CompiledCodeObject code) {
        return FrameUtil.getIntSafe(frame, code.getInstructionPointerSlot());
    }

    public static void setInstructionPointer(final Frame frame, final CompiledCodeObject code, final int value) {
        frame.setInt(code.getInstructionPointerSlot(), value);
    }

    public static int getStackPointer(final Frame frame, final CompiledCodeObject code) {
        return FrameUtil.getIntSafe(frame, code.getStackPointerSlot());
    }

    public static void setStackPointer(final Frame frame, final CompiledCodeObject code, final int value) {
        frame.setInt(code.getStackPointerSlot(), value);
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

    public static void terminate(final Frame frame, final CompiledCodeObject blockOrMethod) {
        FrameAccess.setInstructionPointer(frame, blockOrMethod, -1);
        FrameAccess.setSender(frame, NilObject.SINGLETON);
    }

    public static boolean isGraalSqueakFrame(final Frame frame) {
        final Object[] arguments = frame.getArguments();
        return arguments.length >= ArgumentIndicies.RECEIVER.ordinal() && arguments[ArgumentIndicies.METHOD.ordinal()] instanceof CompiledMethodObject;
    }

    public static Object[] newWith(final CompiledMethodObject method, final Object sender, final BlockClosureObject closure, final Object[] receiverAndArguments) {
        final int receiverAndArgumentsLength = receiverAndArguments.length;
        final Object[] frameArguments = new Object[ArgumentIndicies.RECEIVER.ordinal() + receiverAndArgumentsLength];
        assert method != null : "Method should never be null";
        assert sender != null : "Sender should never be null";
        assert receiverAndArgumentsLength > 0 : "At least a receiver must be provided";
        assert receiverAndArguments[0] != null : "Receiver should never be null";
        frameArguments[ArgumentIndicies.METHOD.ordinal()] = method;
        frameArguments[ArgumentIndicies.SENDER_OR_SENDER_MARKER.ordinal()] = sender;
        frameArguments[ArgumentIndicies.CLOSURE_OR_NULL.ordinal()] = closure;
        System.arraycopy(receiverAndArguments, 0, frameArguments, ArgumentIndicies.RECEIVER.ordinal(), receiverAndArgumentsLength);
        return frameArguments;
    }

    public static Object[] newDummyWith(final CompiledCodeObject code, final Object sender, final BlockClosureObject closure, final Object[] receiverAndArguments) {
        final int receiverAndArgumentsLength = receiverAndArguments.length;
        final Object[] frameArguments = new Object[ArgumentIndicies.RECEIVER.ordinal() + receiverAndArgumentsLength];
        assert sender != null : "Sender should never be null";
        assert receiverAndArgumentsLength > 0 : "At least a receiver must be provided";
        frameArguments[ArgumentIndicies.METHOD.ordinal()] = code;
        frameArguments[ArgumentIndicies.SENDER_OR_SENDER_MARKER.ordinal()] = sender;
        frameArguments[ArgumentIndicies.CLOSURE_OR_NULL.ordinal()] = closure;
        System.arraycopy(receiverAndArguments, 0, frameArguments, ArgumentIndicies.RECEIVER.ordinal(), receiverAndArgumentsLength);
        return frameArguments;
    }

    public static Object[] newClosureArguments(final BlockClosureObject closure, final Object senderOrMarker, final Object[] closureArguments) {
        final int numClosureArguments = closureArguments.length;
        final Object[] arguments = newClosureArgumentsTemplate(closure, senderOrMarker, numClosureArguments);
        System.arraycopy(closureArguments, 0, arguments, ArgumentIndicies.ARGUMENTS_START.ordinal(), numClosureArguments);
        return arguments;
    }

    /* Template because closure arguments still need to be filled in. */
    public static Object[] newClosureArgumentsTemplate(final BlockClosureObject closure, final Object senderOrMarker, final int numArgs) {
        final Object[] copied = closure.getCopied();
        final int numCopied = copied.length;
        assert closure.getCompiledBlock().getNumArgs() == numArgs : "number of required and provided block arguments do not match";
        final Object[] arguments = new Object[ArgumentIndicies.ARGUMENTS_START.ordinal() + numArgs + numCopied];
        arguments[ArgumentIndicies.METHOD.ordinal()] = closure.getCompiledBlock().getMethod();
        // Sender is thisContext (or marker)
        arguments[ArgumentIndicies.SENDER_OR_SENDER_MARKER.ordinal()] = senderOrMarker;
        arguments[ArgumentIndicies.CLOSURE_OR_NULL.ordinal()] = closure;
        arguments[ArgumentIndicies.RECEIVER.ordinal()] = closure.getReceiver();
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
    public static ContextObject findContextForMarker(final FrameMarker frameMarker) {
        if (isLoggingEnabled) {
            LOG.fine("Iterating frames to find a marker...");
        }
        final ContextObject context = (ContextObject) new FramesAndContextsIterator(
                        (bool, code) -> {
                            if (isLoggingEnabled) {
                                LOG.finer(() -> "..." + code);
                            }
                            return bool;
                        }, null).scanFor(frameMarker, NilObject.SINGLETON, NilObject.SINGLETON);
        if (context == null) {
            throw SqueakException.create("Could not find frame for:", frameMarker);
        } else {
            return context;
        }
    }
}
