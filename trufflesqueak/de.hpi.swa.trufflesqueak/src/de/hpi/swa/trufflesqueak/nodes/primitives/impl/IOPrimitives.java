package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.awt.AWTError;
import java.awt.Toolkit;
import java.util.List;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.exceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.LargeIntegerObject;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.FORM;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.WordsObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.MiscellaneousPrimitives.PrimBitBltSimulateNode;
import de.hpi.swa.trufflesqueak.util.SqueakDisplay;

public class IOPrimitives extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return IOPrimitivesFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 90)
    protected static abstract class PrimMousePointNode extends AbstractPrimitiveNode {

        protected PrimMousePointNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object mousePoint() {
            return code.image.wrap(code.image.display.getLastMousePosition());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 94, numArguments = 2)
    protected static abstract class PrimGetNextEventNode extends AbstractPrimitiveNode {

        protected PrimGetNextEventNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object doGetNext(PointersObject eventSensor, ListObject targetArray) {
            long[] nextEvent = code.image.display.getNextEvent();
            for (int i = 0; i < SqueakDisplay.EVENT_SIZE; i++) {
                targetArray.atput0(i, nextEvent[i]);
            }
            return eventSensor;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 96, variableArguments = true)
    protected static abstract class PrimCopyBitsNode extends PrimBitBltSimulateNode {

        protected PrimCopyBitsNode(CompiledMethodObject method) {
            super(method, "BitBltPlugin", "primitiveCopyBits");
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 101, variableArguments = true)
    protected static abstract class PrimBeCursorNode extends AbstractPrimitiveNode {

        protected PrimBeCursorNode(CompiledMethodObject method) {
            super(method);
        }

        @Override
        public final Object executeWithArguments(VirtualFrame frame, Object... rcvrAndArgs) {
            return beCursor(rcvrAndArgs);
        }

        @Specialization
        protected Object beCursor(Object[] rcvrAndArgs) {
            Object cursorObject = rcvrAndArgs[0];
            if (!(cursorObject instanceof PointersObject)) {
                throw new SqueakException("Unexpected cursorObject: " + cursorObject.toString());
            }
            PointersObject cursor = (PointersObject) cursorObject;
            int[] words = ((WordsObject) cursor.at0(FORM.BITS)).getWords();
            long width = (long) cursor.at0(FORM.WIDTH);
            long height = (long) cursor.at0(FORM.HEIGHT);
            if (width != SqueakDisplay.CURSOR_WIDTH || height != SqueakDisplay.CURSOR_HEIGHT) {
                throw new SqueakException("Unexpected cursor width: " + width + " or height: " + height);
            }
            long depth = (long) cursor.at0(FORM.DEPTH);
            if (depth != 1) {
                throw new SqueakException("Unexpected cursor depth: " + depth);
            }
            if (rcvrAndArgs.length == 2) {
                Object maskObject = rcvrAndArgs[1];
                if (!(maskObject instanceof PointersObject)) {
                    throw new SqueakException("Unexpected maskObject: " + maskObject.toString());
                }
                int[] mask = ((WordsObject) ((PointersObject) maskObject).at0(FORM.BITS)).getWords();
                words = mergeCursorWithMask(words, mask);
            }
            code.image.display.setCursor(words);
            return rcvrAndArgs[0];
        }

        private static int[] mergeCursorWithMask(int[] cursorWords, int[] maskWords) {
            int[] words = new int[16];
            for (int i = 0; i < words.length; i++) {
                words[i] = cursorWords[i] ^= ~maskWords[i];
            }
            return words;
        }

        // TODO: properly support arbitrary-sized 32 bit ARGB forms
// private static int[] mergeCursorWithMask(int[] cursorWords, int[] maskWords) {
// int[] words = new int[16];
// int cursorWord, maskWord, bit, merged;
// for (int y = 0; y < SqueakDisplay.CURSOR_HEIGHT; y++) {
// cursorWord = cursorWords[y];
// maskWord = maskWords[y];
// bit = 0x80000000;
// merged = 0;
// for (int x = 0; x < SqueakDisplay.CURSOR_WIDTH; x++) {
// merged = merged | ((maskWord & bit) >> x) | ((cursorWord & bit) >> (x + 1));
// bit = bit >>> 1;
// }
// words[y] = merged;
// }
// return words;
// }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 102)
    protected static abstract class PrimBeDisplayNode extends AbstractPrimitiveNode {

        protected PrimBeDisplayNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected boolean beDisplay(PointersObject receiver) {
            if (receiver.size() < 4) {
                throw new PrimitiveFailed();
            }
            code.image.display.setSqDisplay(receiver);
            code.image.display.open();
            code.image.specialObjectsArray.atput0(SPECIAL_OBJECT_INDEX.TheDisplay, receiver);
            return true;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 105, numArguments = 5)
    protected static abstract class PrimReplaceFromToNode extends AbstractPrimitiveNode {
        protected PrimReplaceFromToNode(CompiledMethodObject method) {
            super(method);
        }

        @Override
        public final Object executeWithArguments(VirtualFrame frame, Object... arguments) {
            try {
                return executeWithArgumentsSpecialized(frame, arguments);
            } catch (IndexOutOfBoundsException e) {
                throw new PrimitiveFailed();
            }
        }

        @Override
        public final Object executePrimitive(VirtualFrame frame) {
            try {
                return executeReplace(frame);
            } catch (IndexOutOfBoundsException e) {
                throw new PrimitiveFailed();
            }
        }

        public abstract Object executeReplace(VirtualFrame frame);

        @Specialization
        protected Object replace(LargeIntegerObject rcvr, long start, long stop, LargeIntegerObject repl, long replStart) {
            return replaceInLarge(rcvr, start, stop, repl.getBytes(), replStart);
        }

        @Specialization
        protected Object replace(LargeIntegerObject rcvr, long start, long stop, long repl, long replStart) {
            return replaceInLarge(rcvr, start, stop, LargeIntegerObject.valueOf(code, repl).getBytes(), replStart);
        }

        @Specialization
        protected Object replace(LargeIntegerObject rcvr, long start, long stop, NativeObject repl, long replStart) {
            return replaceInLarge(rcvr, start, stop, repl.getBytes(), replStart);
        }

        private static Object replaceInLarge(LargeIntegerObject rcvr, long start, long stop, byte[] replBytes, long replStart) {
            byte[] rcvrBytes = rcvr.getBytes();
            int repOff = (int) (replStart - start);
            for (int i = (int) (start - 1); i < stop; i++) {
                rcvrBytes[i] = replBytes[repOff + i];
            }
            rcvr.setBytes(rcvrBytes);
            return rcvr;
        }

        @Specialization
        protected Object replace(NativeObject rcvr, long start, long stop, LargeIntegerObject repl, long replStart) {
            int repOff = (int) (replStart - start);
            byte[] replBytes = repl.getBytes();
            for (int i = (int) (start - 1); i < stop; i++) {
                rcvr.setNativeAt0(i, replBytes[repOff + i]);
            }
            return rcvr;
        }

        @Specialization
        protected Object replace(NativeObject rcvr, long start, long stop, NativeObject repl, long replStart) {
            int repOff = (int) (replStart - start);
            for (int i = (int) (start - 1); i < stop; i++) {
                rcvr.setNativeAt0(i, repl.getNativeAt0(repOff + i));
            }
            return rcvr;
        }

        @Specialization
        protected Object replace(ListObject rcvr, long start, long stop, ListObject repl, long replStart) {
            long repOff = replStart - start;
            for (int i = (int) (start - 1); i < stop; i++) {
                rcvr.atput0(i, repl.at0(repOff + i));
            }
            return rcvr;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 106)
    protected static abstract class PrimScreenSizeNode extends AbstractPrimitiveNode {

        protected PrimScreenSizeNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected BaseSqueakObject get(@SuppressWarnings("unused") BaseSqueakObject receiver) {
            return code.image.wrap(code.image.display.getSize());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 107)
    protected static abstract class PrimMouseButtonsNode extends AbstractPrimitiveNode {

        protected PrimMouseButtonsNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object get(@SuppressWarnings("unused") BaseSqueakObject receiver) {
            return code.image.wrap(code.image.display.getLastMouseButton());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 108)
    protected static abstract class PrimKeyboardNextNode extends AbstractPrimitiveNode {

        protected PrimKeyboardNextNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object get(@SuppressWarnings("unused") BaseSqueakObject receiver) {
            int keyboardNext = code.image.display.keyboardNext();
            if (keyboardNext == 0) {
                return code.image.nil;
            } else {
                return code.image.wrap(keyboardNext);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 109)
    protected static abstract class PrimKeyboardPeekNode extends AbstractPrimitiveNode {

        protected PrimKeyboardPeekNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object get(@SuppressWarnings("unused") BaseSqueakObject receiver) {
            int keyboardPeek = code.image.display.keyboardPeek();
            if (keyboardPeek == 0) {
                return code.image.nil;
            } else {
                return code.image.wrap(keyboardPeek);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 126, numArguments = 2)
    protected static abstract class PrimDeferDisplayUpdatesNode extends AbstractPrimitiveNode {

        public PrimDeferDisplayUpdatesNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected BaseSqueakObject doDefer(BaseSqueakObject receiver, boolean flag) {
            code.image.display.setDeferUpdates(flag);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 127, numArguments = 5)
    protected static abstract class PrimDrawRectNode extends AbstractPrimitiveNode {

        protected PrimDrawRectNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected BaseSqueakObject get(BaseSqueakObject receiver, long left, long right, long top, long bottom) {
            if (receiver != code.image.specialObjectsArray.at0(SPECIAL_OBJECT_INDEX.TheDisplay)) {
                return code.image.nil;
            }
            if (!((left <= right) && (top <= bottom))) {
                return code.image.nil;
            }
            code.image.display.forceRect((int) left, (int) right, (int) top, (int) bottom);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 133)
    protected static abstract class PrimSetInterruptKeyNode extends AbstractPrimitiveNode {

        protected PrimSetInterruptKeyNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected BaseSqueakObject set(BaseSqueakObject receiver) {
            // TODO: interrupt key is obsolete in image, but maybe still needed in the vm?
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 140)
    protected static abstract class PrimBeepNode extends AbstractPrimitiveNode {

        protected PrimBeepNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected BaseSqueakObject doBeep(BaseSqueakObject receiver) {
            try {
                Toolkit.getDefaultToolkit().beep();
            } catch (AWTError e) {
                code.image.getError().println("BEEP (unable to find default AWT Toolkit).");
            }
            return receiver;
        }
    }
}
