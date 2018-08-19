package de.hpi.swa.graal.squeak.nodes.primitives.impl;

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.io.DisplayPoint;
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
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithImage;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAtPut0Node;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectInstSizeNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectSizeNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.IOPrimitivesFactory.PrimScanCharactersNodeFactory.ScanCharactersHelperNodeGen;

public final class IOPrimitives extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return IOPrimitivesFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 90)
    protected abstract static class PrimMousePointNode extends AbstractPrimitiveNode {
        private static final DisplayPoint NULL_POINT = new DisplayPoint(0, 0);

        protected PrimMousePointNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = "code.image.hasDisplay()")
        protected final Object doMousePoint() {
            return code.image.wrap(code.image.getDisplay().getLastMousePosition());
        }

        @Specialization(guards = "!code.image.hasDisplay()")
        protected final Object doMousePointHeadless() {
            return code.image.wrap(NULL_POINT);
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

        @Specialization(guards = "code.image.hasDisplay()")
        protected final Object doSet(final AbstractSqueakObject receiver, final long depth, final long width, final long height, final boolean fullscreen) {
            code.image.getDisplay().adjustDisplay(depth, width, height, fullscreen);
            return receiver;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!code.image.hasDisplay()")
        protected static final Object doSetHeadless(final AbstractSqueakObject receiver, final long depth, final long width, final long height, final boolean fullscreen) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 93)
    protected abstract static class PrimInputSemaphoreNode extends AbstractPrimitiveNode {

        protected PrimInputSemaphoreNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = "code.image.hasDisplay()")
        protected final Object doSet(final AbstractSqueakObject receiver, final long semaIndex) {
            code.image.getDisplay().setInputSemaphoreIndex((int) semaIndex);
            return receiver;
        }

        @Specialization(guards = "!code.image.hasDisplay()")
        protected static final Object doSetHeadless(final AbstractSqueakObject receiver, @SuppressWarnings("unused") final long semaIndex) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 94)
    protected abstract static class PrimGetNextEventNode extends AbstractPrimitiveNode {

        protected PrimGetNextEventNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = "code.image.hasDisplay()")
        protected final PointersObject doGetNext(final PointersObject eventSensor, final PointersObject targetArray) {
            storeEventInto(targetArray, code.image.getDisplay().getNextEvent());
            return eventSensor;
        }

        @Specialization(guards = "!code.image.hasDisplay()")
        protected static final PointersObject doGetNextHeadless(final PointersObject eventSensor, @SuppressWarnings("unused") final PointersObject targetArray) {
            storeEventInto(targetArray, SqueakIOConstants.NULL_EVENT);
            return eventSensor;
        }

        // Cannot use `@ExplodeLoop` here.
        private static void storeEventInto(final PointersObject targetArray, final long[] nextEvent) {
            for (int i = 0; i < nextEvent.length; i++) {
                targetArray.atput0(i, nextEvent[i]);
            }
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

        protected PrimBeCursorNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = "code.image.hasDisplay()")
        protected final Object doCursor(final PointersObject receiver, @SuppressWarnings("unused") final NotProvided mask) {
            code.image.getDisplay().setCursor(validateAndExtractWords(receiver), null, extractDepth(receiver));
            return receiver;
        }

        @Specialization(guards = "code.image.hasDisplay()")
        protected final Object doCursor(final PointersObject receiver, final PointersObject maskObject) {
            final int[] words = validateAndExtractWords(receiver);
            final int depth = extractDepth(receiver);
            if (depth == 1) {
                final int[] mask = ((NativeObject) maskObject.at0(FORM.BITS)).getIntStorage();
                code.image.getDisplay().setCursor(words, mask, 2);
            } else {
                code.image.getDisplay().setCursor(words, null, depth);
            }
            return receiver;
        }

        @Specialization(guards = "!code.image.hasDisplay()")
        protected static final Object doCursorHeadless(final PointersObject receiver, @SuppressWarnings("unused") final NotProvided mask) {
            return receiver;
        }

        @Specialization(guards = "!code.image.hasDisplay()")
        protected static final Object doCursorHeadless(final PointersObject receiver, @SuppressWarnings("unused") final PointersObject maskObject) {
            return receiver;
        }

        private static int[] validateAndExtractWords(final PointersObject receiver) {
            final int[] words = ((NativeObject) receiver.at0(FORM.BITS)).getIntStorage();
            final long width = (long) receiver.at0(FORM.WIDTH);
            final long height = (long) receiver.at0(FORM.HEIGHT);
            if (width != SqueakIOConstants.CURSOR_WIDTH || height != SqueakIOConstants.CURSOR_HEIGHT) {
                throw new SqueakException("Unexpected cursor width:", width, "or height:", height);
            }
            return words;
        }

        private static int extractDepth(final PointersObject receiver) {
            return (int) (long) receiver.at0(FORM.DEPTH);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 102)
    protected abstract static class PrimBeDisplayNode extends AbstractPrimitiveNode {

        protected PrimBeDisplayNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = {"code.image.hasDisplay()", "receiver.size() >= 4"})
        protected final boolean doDisplay(final PointersObject receiver) {
            code.image.specialObjectsArray.atput0(SPECIAL_OBJECT_INDEX.TheDisplay, receiver);
            code.image.getDisplay().open(receiver);
            return code.image.sqTrue;
        }

        @Specialization(guards = {"!code.image.hasDisplay()"})
        protected final boolean doDisplayHeadless(@SuppressWarnings("unused") final PointersObject receiver) {
            return code.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 103)
    protected abstract static class PrimScanCharactersNode extends AbstractPrimitiveNode {
        @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();
        @Child private ScanCharactersHelperNode scanNode;

        protected PrimScanCharactersNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
            scanNode = ScanCharactersHelperNode.create(method.image);
        }

        @Specialization(guards = {"startIndex > 0", "stopIndex > 0", "sourceString.isByteType()", "receiver.size() >= 4", "stops.size() >= 258"})
        protected final Object doScan(final PointersObject receiver, final long startIndex, final long stopIndex, final NativeObject sourceString, final long rightX,
                        final PointersObject stops, final long kernData) {
            final Object scanDestX = at0Node.execute(receiver, 0);
            final Object scanXTable = at0Node.execute(receiver, 2);
            final Object scanMap = at0Node.execute(receiver, 3);
            return scanNode.executeScan(receiver, startIndex, stopIndex, sourceString.getByteStorage(), rightX, stops, kernData, scanDestX, scanXTable, scanMap);
        }

        protected abstract static class ScanCharactersHelperNode extends AbstractNodeWithImage {
            private static final long END_OF_RUN = 257 - 1;
            private static final long CROSSED_X = 258 - 1;

            @Child private SqueakObjectAtPut0Node atPut0Node = SqueakObjectAtPut0Node.create();

            protected static ScanCharactersHelperNode create(final SqueakImageContext image) {
                return ScanCharactersHelperNodeGen.create(image);
            }

            protected abstract Object executeScan(PointersObject receiver, long startIndex, long stopIndex, byte[] sourceBytes, long rightX, PointersObject stops, long kernData,
                            Object scanDestX, Object scanXTable, Object scanMap);

            protected ScanCharactersHelperNode(final SqueakImageContext image) {
                super(image);
            }

            @Specialization(guards = {"scanMap.size() == 256", "stopIndex <= sourceBytes.length"})
            protected final Object doScan(final PointersObject receiver, final long startIndex, final long stopIndex, final byte[] sourceBytes, final long rightX, final PointersObject stops,
                            final long kernData, final long startScanDestX, final PointersObject scanXTable, final PointersObject scanMap) {
                final int maxGlyph = scanXTable.size() - 2;
                long scanDestX = startScanDestX;
                long scanLastIndex = startIndex;
                while (scanLastIndex <= stopIndex) {
                    final long ascii = (sourceBytes[(int) (scanLastIndex - 1)] & 0xFF);
                    final Object stopReason = stops.at0(ascii);
                    if (stopReason != image.nil) {
                        storeStateInReceiver(receiver, scanDestX, scanLastIndex);
                        return stopReason;
                    }
                    final long glyphIndex;
                    try {
                        glyphIndex = (long) scanMap.at0(ascii);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        throw new PrimitiveFailed();
                    }
                    if (glyphIndex < 0 || glyphIndex > maxGlyph) {
                        throw new PrimitiveFailed();
                    }
                    final long sourceX1;
                    final long sourceX2;
                    try {
                        sourceX1 = (long) scanXTable.at0(glyphIndex);
                        sourceX2 = (long) scanXTable.at0(glyphIndex + 1);
                    } catch (ClassCastException e) {
                        throw new PrimitiveFailed();
                    }
                    final long nextDestX = scanDestX + sourceX2 - sourceX1;
                    if (nextDestX > rightX) {
                        storeStateInReceiver(receiver, scanDestX, scanLastIndex);
                        return stops.at0(CROSSED_X);
                    }
                    scanDestX = nextDestX + kernData;
                    scanLastIndex++;
                }
                storeStateInReceiver(receiver, scanDestX, stopIndex);
                return stops.at0(END_OF_RUN);
            }

            private void storeStateInReceiver(final PointersObject receiver, final long scanDestX, final long scanLastIndex) {
                atPut0Node.execute(receiver, 0, scanDestX);
                atPut0Node.execute(receiver, 1, scanLastIndex);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 105)
    protected abstract static class PrimStringReplaceNode extends AbstractPrimitiveNode {
        @Child private SqueakObjectInstSizeNode instSizeNode = SqueakObjectInstSizeNode.create();
        @Child private SqueakObjectSizeNode sizeNode = SqueakObjectSizeNode.create();
        @Child private SqueakObjectAtPut0Node atPut0Node;

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
            return doLargeInteger(rcvr, start, stop, asLargeInteger(repl), replStart);
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

        @Specialization(guards = {"hasValidBounds(rcvr, start, stop, repl, replStart)", "repl.isByteType()"})
        protected static final Object doLargeIntegerNative(final LargeIntegerObject rcvr, final long start, final long stop, final NativeObject repl, final long replStart) {
            final byte[] rcvrBytes = rcvr.getBytes();
            final byte[] replBytes = repl.getByteStorage();
            final int repOff = (int) (replStart - start);
            for (int i = (int) (start - 1); i < stop; i++) {
                rcvrBytes[i] = replBytes[repOff + i];
            }
            rcvr.setBytes(rcvrBytes);
            return rcvr;
        }

        @Specialization(guards = "hasValidBounds(rcvr, start, stop, repl, replStart)")
        protected final Object doNative(final NativeObject rcvr, final long start, final long stop, final NativeObject repl, final long replStart,
                        @Cached("create()") final SqueakObjectAt0Node at0Node) {
            final int repOff = (int) (replStart - start);
            for (int i = (int) (start - 1); i < stop; i++) {
                getAtPut0Node().execute(rcvr, i, at0Node.execute(repl, repOff + i));
            }
            return rcvr;
        }

        @Specialization(guards = "!isSmallInteger(repl)")
        protected final Object doNativeLargeInteger(final NativeObject rcvr, final long start, final long stop, final long repl, final long replStart) {
            return doNativeLargeInteger(rcvr, start, stop, asLargeInteger(repl), replStart);
        }

        @Specialization(guards = "hasValidBounds(rcvr, start, stop, repl, replStart)")
        protected final Object doNativePointers(final NativeObject rcvr, final long start, final long stop, final PointersObject repl, final long replStart) {
            final int repOff = (int) (replStart - start);
            for (int i = (int) (start - 1); i < stop; i++) {
                getAtPut0Node().execute(rcvr, i, repl.at0(repOff + i));
            }
            return rcvr;
        }

        @Specialization(guards = "hasValidBounds(rcvr, start, stop, repl, replStart)")
        protected final Object doNativeLargeInteger(final NativeObject rcvr, final long start, final long stop, final LargeIntegerObject repl, final long replStart) {
            final int repOff = (int) (replStart - start);
            for (int i = (int) (start - 1); i < stop; i++) {
                getAtPut0Node().execute(rcvr, i, repl.getNativeAt0(repOff + i));
            }
            return rcvr;
        }

        @Specialization(guards = "hasValidBounds(rcvr, start, stop, repl, replStart)")
        protected final Object doNativeFloat(final NativeObject rcvr, final long start, final long stop, final FloatObject repl, final long replStart) {
            final int repOff = (int) (replStart - start);
            for (int i = (int) (start - 1); i < stop; i++) {
                getAtPut0Node().execute(rcvr, i, repl.getNativeAt0(repOff + i));
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

        private SqueakObjectAtPut0Node getAtPut0Node() {
            if (atPut0Node == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                atPut0Node = insert(SqueakObjectAtPut0Node.create());
            }
            return atPut0Node;
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
            return code.image.wrap(code.image.flags.getLastWindowSize());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 107)
    protected abstract static class PrimMouseButtonsNode extends AbstractPrimitiveNode {

        protected PrimMouseButtonsNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = "code.image.hasDisplay()")
        protected final Object doMouseButtons(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            return code.image.wrap(code.image.getDisplay().getLastMouseButton());
        }

        @Specialization(guards = "!code.image.hasDisplay()")
        protected final Object doMouseButtonsHeadless(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            return code.image.wrap(0L);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 108)
    protected abstract static class PrimKeyboardNextNode extends AbstractPrimitiveNode {

        protected PrimKeyboardNextNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = "code.image.hasDisplay()")
        protected final Object doNext(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            final int keyboardNext = code.image.getDisplay().keyboardNext();
            if (keyboardNext == 0) {
                return code.image.nil;
            } else {
                return code.image.wrap(keyboardNext);
            }
        }

        @Specialization(guards = "!code.image.hasDisplay()")
        protected final Object doNextHeadless(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            return code.image.nil;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 109)
    protected abstract static class PrimKeyboardPeekNode extends AbstractPrimitiveNode {

        protected PrimKeyboardPeekNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = "code.image.hasDisplay()")
        protected final Object doPeek(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            final int keyboardPeek = code.image.getDisplay().keyboardPeek();
            if (keyboardPeek == 0) {
                return code.image.nil;
            } else {
                return code.image.wrap(keyboardPeek);
            }
        }

        @Specialization(guards = "!code.image.hasDisplay()")
        protected final Object doPeekHeadless(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            return code.image.nil;
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

        @Specialization(guards = {"code.image.hasDisplay()", "inBounds(left, right, top, bottom)"})
        protected final AbstractSqueakObject doDraw(final PointersObject receiver, final long left, final long right, final long top, final long bottom) {
            code.image.getDisplay().forceRect((int) left, (int) right, (int) top, (int) bottom);
            return receiver;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!code.image.hasDisplay()", "inBounds(left, right, top, bottom)"})
        protected static final AbstractSqueakObject doDrawHeadless(final PointersObject receiver, final long left, final long right, final long top, final long bottom) {
            return receiver;
        }

        protected static final boolean inBounds(final long left, final long right, final long top, final long bottom) {
            return (left <= right) && (top <= bottom);
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

        @Specialization(guards = "code.image.hasDisplay()")
        protected final AbstractSqueakObject doBeep(final AbstractSqueakObject receiver) {
            code.image.getDisplay().beep();
            return receiver;
        }

        @Specialization(guards = "!code.image.hasDisplay()")
        protected final AbstractSqueakObject doNothing(final AbstractSqueakObject receiver) {
            code.image.printToStdOut((char) 7);
            return receiver;
        }
    }
}
