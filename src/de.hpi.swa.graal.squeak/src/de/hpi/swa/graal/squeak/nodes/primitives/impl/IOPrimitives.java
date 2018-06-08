package de.hpi.swa.graal.squeak.nodes.primitives.impl;

import java.awt.AWTError;
import java.awt.Toolkit;
import java.util.List;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ValueProfile;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.exceptions.SqueakException;
import de.hpi.swa.graal.squeak.io.SqueakIOConstants;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.CompiledBlockObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NotProvided;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.ERROR_TABLE;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.FORM;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.WeakPointersObject;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAtPut0Node;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectInstSizeNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectSizeNode;
import de.hpi.swa.graal.squeak.nodes.accessing.NativeObjectNodes.NativeGetBytesNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.MiscellaneousPrimitives.SimulationPrimitiveNode;

public class IOPrimitives extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return IOPrimitivesFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 90)
    protected abstract static class PrimMousePointNode extends AbstractPrimitiveNode {

        protected PrimMousePointNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final Object doMousePoint() {
            return code.image.wrap(code.image.display.getLastMousePosition());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 91)
    protected abstract static class PrimTestDisplayDepthNode extends AbstractPrimitiveNode {
        private static final int[] SUPPORTED_DEPTHS = new int[]{32}; // TODO: support all depths?
                                                                     // {1, 2, 4, 8, 16, 32}

        protected PrimTestDisplayDepthNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final Object doTest(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final long depth) {
            for (int i = 0; i < SUPPORTED_DEPTHS.length; i++) {
                if (SUPPORTED_DEPTHS[i] == depth) {
                    return code.image.sqTrue;
                }
            }
            return code.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 92)
    protected abstract static class PrimSetDisplayModeNode extends AbstractPrimitiveNode {

        protected PrimSetDisplayModeNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final Object doSet(final AbstractSqueakObject receiver, final long depth, final long width, final long height, final boolean fullscreen) {
            code.image.display.adjustDisplay(depth, width, height, fullscreen);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 93)
    protected abstract static class PrimInputSemaphoreNode extends AbstractPrimitiveNode {

        protected PrimInputSemaphoreNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final Object doSet(final AbstractSqueakObject receiver, final long semaIndex) {
            code.image.display.setInputSemaphoreIndex((int) semaIndex);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 94)
    protected abstract static class PrimGetNextEventNode extends AbstractPrimitiveNode {

        protected PrimGetNextEventNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final Object doGetNext(final PointersObject eventSensor, final PointersObject targetArray) {
            final long[] nextEvent = code.image.display.getNextEvent();
            for (int i = 0; i < nextEvent.length; i++) {
                targetArray.atput0(i, nextEvent[i]);
            }
            return eventSensor;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 96)
    protected abstract static class PrimCopyBitsNode extends SimulationPrimitiveNode {

        protected PrimCopyBitsNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments, "BitBltPlugin", "primitiveCopyBits");
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 101)
    protected abstract static class PrimBeCursorNode extends AbstractPrimitiveNode {
        private final ValueProfile storageType = ValueProfile.createClassProfile();

        protected PrimBeCursorNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final Object doCursor(final PointersObject receiver, @SuppressWarnings("unused") final NotProvided mask) {
            code.image.display.setCursor(validateAndExtractWords(receiver), extractDepth(receiver));
            return receiver;
        }

        @Specialization
        protected final Object doCursor(final PointersObject receiver, final PointersObject maskObject) {
            final int[] words = validateAndExtractWords(receiver);
            final int depth = extractDepth(receiver);
            if (depth == 1) {
                final int[] mask = ((NativeObject) maskObject.at0(FORM.BITS)).getIntStorage(storageType);
                code.image.display.setCursor(mergeCursorWithMask(words, mask), 2);
            } else {
                code.image.display.setCursor(words, depth);
            }
            return receiver;
        }

        private int[] validateAndExtractWords(final PointersObject receiver) {
            final int[] words = ((NativeObject) receiver.at0(FORM.BITS)).getIntStorage(storageType);
            final long width = (long) receiver.at0(FORM.WIDTH);
            final long height = (long) receiver.at0(FORM.HEIGHT);
            if (width != SqueakIOConstants.CURSOR_WIDTH || height != SqueakIOConstants.CURSOR_HEIGHT) {
                throw new SqueakException("Unexpected cursor width: " + width + " or height: " + height);
            }
            return words;
        }

        private static int extractDepth(final PointersObject receiver) {
            return ((Long) receiver.at0(FORM.DEPTH)).intValue();
        }

        private static int[] mergeCursorWithMask(final int[] cursorWords, final int[] maskWords) {
            final int[] words = new int[SqueakIOConstants.CURSOR_HEIGHT];
            int cursorWord;
            int maskWord;
            int bit;
            int merged;
            for (int y = 0; y < SqueakIOConstants.CURSOR_HEIGHT; y++) {
                cursorWord = (int) (Integer.toUnsignedLong(cursorWords[y]));
                maskWord = (int) (Integer.toUnsignedLong(maskWords[y]));
                bit = 0x80000000;
                merged = 0;
                for (int x = 0; x < SqueakIOConstants.CURSOR_WIDTH; x++) {
                    merged = merged | ((maskWord & bit) >> x) | ((cursorWord & bit) >> (x + 1));
                    bit = bit >>> 1;
                }
                words[y] = merged;
            }
            return words;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 102)
    protected abstract static class PrimBeDisplayNode extends AbstractPrimitiveNode {

        protected PrimBeDisplayNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final boolean doDisplay(final PointersObject receiver) {
            if (receiver.size() < 4) {
                throw new PrimitiveFailed();
            }
            code.image.display.setSqDisplay(receiver);
            code.image.display.open();
            code.image.specialObjectsArray.atput0(SPECIAL_OBJECT_INDEX.TheDisplay, receiver);
            return code.image.sqTrue;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 105)
    protected abstract static class PrimStringReplaceNode extends AbstractPrimitiveNode {
        @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();
        @Child private SqueakObjectAtPut0Node atPut0Node = SqueakObjectAtPut0Node.create();
        @Child private SqueakObjectInstSizeNode instSizeNode = SqueakObjectInstSizeNode.create();
        @Child private SqueakObjectSizeNode sizeNode = SqueakObjectSizeNode.create();
        @Child private NativeGetBytesNode getBytesNode = NativeGetBytesNode.create();

        protected PrimStringReplaceNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Override
        public final Object executeWithArguments(final VirtualFrame frame, final Object... arguments) {
            try {
                return executeWithArgumentsSpecialized(frame, arguments);
            } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
                throw new PrimitiveFailed();
            }
        }

        @Override
        public final Object executePrimitive(final VirtualFrame frame) {
            try {
                return executeReplace(frame);
            } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
                throw new PrimitiveFailed();
            }
        }

        public abstract Object executeReplace(VirtualFrame frame);

        @Specialization(guards = "!isSmallInteger(repl)")
        protected final Object replace(final LargeIntegerObject rcvr, final long start, final long stop, final long repl, final long replStart) {
            final LargeIntegerObject largeInteger = asLargeInteger(repl);
            if (hasValidBounds(rcvr, start, stop, largeInteger, replStart)) {
                throw new PrimitiveFailed(ERROR_TABLE.BAD_INDEX);
            }
            final byte[] rcvrBytes = rcvr.getBytes();
            final byte[] replBytes = largeInteger.getBytes();
            final int repOff = (int) (replStart - start);
            for (int i = (int) (start - 1); i < stop; i++) {
                rcvrBytes[i] = replBytes[repOff + i];
            }
            rcvr.setBytes(rcvrBytes);
            return rcvr;
        }

        @Specialization(guards = "hasValidBounds(rcvr, start, stop, repl, replStart)")
        protected static final Object doLargeInteger(final LargeIntegerObject rcvr, final long start, final long stop, final LargeIntegerObject repl, final long replStart) {
            final byte[] rcvrBytes = rcvr.getBytes();
            final byte[] replBytes = repl.getBytes();
            final int repOff = (int) (replStart - start);
            for (int i = (int) (start - 1); i < stop; i++) {
                rcvrBytes[i] = replBytes[repOff + i];
            }
            rcvr.setBytes(rcvrBytes);
            return rcvr;
        }

        @Specialization(guards = "hasValidBounds(rcvr, start, stop, repl, replStart)")
        protected static final Object doLargeIntegerFloat(final LargeIntegerObject rcvr, final long start, final long stop, final FloatObject repl, final long replStart) {
            final byte[] rcvrBytes = rcvr.getBytes();
            final byte[] replBytes = repl.getBytes();
            final int repOff = (int) (replStart - start);
            for (int i = (int) (start - 1); i < stop; i++) {
                rcvrBytes[i] = replBytes[repOff + i];
            }
            rcvr.setBytes(rcvrBytes);
            return rcvr;
        }

        @Specialization(guards = "hasValidBounds(rcvr, start, stop, repl, replStart)")
        protected final Object doLargeIntegerNative(final LargeIntegerObject rcvr, final long start, final long stop, final NativeObject repl, final long replStart) {
            final byte[] rcvrBytes = rcvr.getBytes();
            final byte[] replBytes = getBytesNode.execute(repl);
            final int repOff = (int) (replStart - start);
            for (int i = (int) (start - 1); i < stop; i++) {
                rcvrBytes[i] = replBytes[repOff + i];
            }
            rcvr.setBytes(rcvrBytes);
            return rcvr;
        }

        @Specialization(guards = "hasValidBounds(rcvr, start, stop, repl, replStart)")
        protected final Object doNative(final NativeObject rcvr, final long start, final long stop, final NativeObject repl, final long replStart) {
            final int repOff = (int) (replStart - start);
            for (int i = (int) (start - 1); i < stop; i++) {
                atPut0Node.execute(rcvr, i, at0Node.execute(repl, repOff + i));
            }
            return rcvr;
        }

        @Specialization(guards = "hasValidBounds(rcvr, start, stop, repl, replStart)")
        protected final Object doNativeLargeInteger(final NativeObject rcvr, final long start, final long stop, final LargeIntegerObject repl, final long replStart) {
            final int repOff = (int) (replStart - start);
            for (int i = (int) (start - 1); i < stop; i++) {
                atPut0Node.execute(rcvr, i, repl.getNativeAt0(repOff + i));
            }
            return rcvr;
        }

        @Specialization(guards = "hasValidBounds(rcvr, start, stop, repl, replStart)")
        protected final Object doNativeFloat(final NativeObject rcvr, final long start, final long stop, final FloatObject repl, final long replStart) {
            final int repOff = (int) (replStart - start);
            for (int i = (int) (start - 1); i < stop; i++) {
                atPut0Node.execute(rcvr, i, repl.getNativeAt0(repOff + i));
            }
            return rcvr;
        }

        @Specialization(guards = "hasValidBounds(rcvr, start, stop, repl, replStart)")
        protected static final Object doPointers(final PointersObject rcvr, final long start, final long stop, final PointersObject repl, final long replStart) {
            final long repOff = replStart - start;
            for (int i = (int) (start - 1); i < stop; i++) {
                rcvr.atput0(i, repl.at0(repOff + i));
            }
            return rcvr;
        }

        @Specialization(guards = "hasValidBounds(rcvr, start, stop, repl, replStart)")
        protected static final Object doPointersWeakPointers(final PointersObject rcvr, final long start, final long stop, final WeakPointersObject repl, final long replStart) {
            final long repOff = replStart - start;
            for (int i = (int) (start - 1); i < stop; i++) {
                rcvr.atput0(i, repl.at0(repOff + i));
            }
            return rcvr;
        }

        @Specialization(guards = "hasValidBounds(rcvr, start, stop, repl, replStart)")
        protected static final Object doWeakPointers(final WeakPointersObject rcvr, final long start, final long stop, final WeakPointersObject repl, final long replStart) {
            final long repOff = replStart - start;
            for (int i = (int) (start - 1); i < stop; i++) {
                rcvr.atput0(i, repl.at0(repOff + i));
            }
            return rcvr;
        }

        @Specialization(guards = "hasValidBounds(rcvr, start, stop, repl, replStart)")
        protected static final Object doWeakPointersPointers(final WeakPointersObject rcvr, final long start, final long stop, final PointersObject repl, final long replStart) {
            final long repOff = replStart - start;
            for (int i = (int) (start - 1); i < stop; i++) {
                rcvr.atput0(i, repl.at0(repOff + i));
            }
            return rcvr;
        }

        @Specialization(guards = "hasValidBounds(rcvr, start, stop, repl, replStart)")
        protected static final Object doBlock(final CompiledBlockObject rcvr, final long start, final long stop, final CompiledBlockObject repl, final long replStart) {
            final long repOff = replStart - start;
            for (int i = (int) (start - 1); i < stop; i++) {
                rcvr.atput0(i, repl.at0(repOff + i));
            }
            return rcvr;
        }

        @Specialization(guards = "hasValidBounds(rcvr, start, stop, repl, replStart)")
        protected static final Object doMethod(final CompiledMethodObject rcvr, final long start, final long stop, final CompiledMethodObject repl, final long replStart) {
            final long repOff = replStart - start;
            for (int i = (int) (start - 1); i < stop; i++) {
                rcvr.atput0(i, repl.at0(repOff + i));
            }
            return rcvr;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!hasValidBounds(rcvr, start, stop, repl, replStart)")
        protected static final Object doBadIndex(final AbstractSqueakObject rcvr, final long start, final long stop, final AbstractSqueakObject repl, final long replStart) {
            throw new PrimitiveFailed(ERROR_TABLE.BAD_INDEX);
        }

        protected final boolean hasValidBounds(final AbstractSqueakObject array, final long start, final long stop, final AbstractSqueakObject repl, final long replStart) {
            return (start >= 1 && (start - 1) <= stop && (stop + instSizeNode.execute(array)) <= sizeNode.execute(array)) &&
                            (replStart >= 1 && (stop - start + replStart + instSizeNode.execute(repl) <= sizeNode.execute(repl)));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 106)
    protected abstract static class PrimScreenSizeNode extends AbstractPrimitiveNode {

        protected PrimScreenSizeNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final AbstractSqueakObject doSize(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            return code.image.wrap(code.image.display.getSize());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 107)
    protected abstract static class PrimMouseButtonsNode extends AbstractPrimitiveNode {

        protected PrimMouseButtonsNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final Object doMouseButtons(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            return code.image.wrap(code.image.display.getLastMouseButton());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 108)
    protected abstract static class PrimKeyboardNextNode extends AbstractPrimitiveNode {

        protected PrimKeyboardNextNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final Object doNext(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            final int keyboardNext = code.image.display.keyboardNext();
            if (keyboardNext == 0) {
                return code.image.nil;
            } else {
                return code.image.wrap(keyboardNext);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 109)
    protected abstract static class PrimKeyboardPeekNode extends AbstractPrimitiveNode {

        protected PrimKeyboardPeekNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final Object doPeek(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            final int keyboardPeek = code.image.display.keyboardPeek();
            if (keyboardPeek == 0) {
                return code.image.nil;
            } else {
                return code.image.wrap(keyboardPeek);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 126)
    protected abstract static class PrimDeferDisplayUpdatesNode extends AbstractPrimitiveNode {

        public PrimDeferDisplayUpdatesNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected static final AbstractSqueakObject doDefer(final AbstractSqueakObject receiver, @SuppressWarnings("unused") final boolean flag) {
            // TODO: uncomment: code.image.display.setDeferUpdates(flag);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 127)
    protected abstract static class PrimDrawRectNode extends AbstractPrimitiveNode {

        protected PrimDrawRectNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        protected static final boolean inBounds(final long left, final long right, final long top, final long bottom) {
            return (left <= right) && (top <= bottom);
        }

        @Specialization(guards = "inBounds(left, right, top, bottom)")
        protected final AbstractSqueakObject doDraw(final PointersObject receiver, final long left, final long right, final long top, final long bottom) {
            code.image.display.forceRect((int) left, (int) right, (int) top, (int) bottom);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 133)
    protected abstract static class PrimSetInterruptKeyNode extends AbstractPrimitiveNode {

        protected PrimSetInterruptKeyNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected static final AbstractSqueakObject set(final AbstractSqueakObject receiver) {
            // TODO: interrupt key is obsolete in image, but maybe still needed in the vm?
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 140)
    protected abstract static class PrimBeepNode extends AbstractPrimitiveNode {

        protected PrimBeepNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final AbstractSqueakObject doBeep(final AbstractSqueakObject receiver) {
            try {
                Toolkit.getDefaultToolkit().beep();
            } catch (AWTError e) {
                code.image.getError().println("BEEP (unable to find default AWT Toolkit).");
            }
            return receiver;
        }
    }
}
