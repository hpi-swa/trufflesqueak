package de.hpi.swa.graal.squeak.nodes.primitives.impl;

import java.util.Arrays;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.io.DisplayPoint;
import de.hpi.swa.graal.squeak.io.SqueakIOConstants;
import de.hpi.swa.graal.squeak.model.AbstractPointersObject;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.CompiledBlockObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.NotProvided;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.CHARACTER_SCANNER;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.ERROR_TABLE;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.FORM;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.SPECIAL_OBJECT;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.WeakPointersObject;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectReadNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectSizeNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectToObjectArrayNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectWriteNode;
import de.hpi.swa.graal.squeak.nodes.accessing.NativeObjectNodes.NativeObjectReadNode;
import de.hpi.swa.graal.squeak.nodes.accessing.NativeObjectNodes.NativeObjectWriteNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectInstSizeNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectSizeNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.QuaternaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.QuinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.SeptenaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;

public final class IOPrimitives extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return IOPrimitivesFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 90)
    protected abstract static class PrimMousePointNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        private static final DisplayPoint NULL_POINT = new DisplayPoint(0, 0);

        protected PrimMousePointNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "method.image.hasDisplay()")
        protected final PointersObject doMousePoint(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            return method.image.wrap(method.image.getDisplay().getLastMousePosition());
        }

        @Specialization(guards = "!method.image.hasDisplay()")
        protected final PointersObject doMousePointHeadless(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            return method.image.wrap(NULL_POINT);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 91)
    protected abstract static class PrimTestDisplayDepthNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        private static final int[] SUPPORTED_DEPTHS = new int[]{32}; // TODO: support all depths?
                                                                     // {1, 2, 4, 8, 16, 32}

        protected PrimTestDisplayDepthNode(final CompiledMethodObject method) {
            super(method);
        }

        @ExplodeLoop
        @Specialization
        protected final boolean doTest(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final long depth) {
            for (int i = 0; i < SUPPORTED_DEPTHS.length; i++) {
                if (SUPPORTED_DEPTHS[i] == depth) {
                    return method.image.sqTrue;
                }
            }
            return method.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 92)
    protected abstract static class PrimSetDisplayModeNode extends AbstractPrimitiveNode implements QuinaryPrimitive {

        protected PrimSetDisplayModeNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "method.image.hasDisplay()")
        protected final AbstractSqueakObject doSet(final AbstractSqueakObject receiver, final long depth, final long width, final long height, final boolean fullscreen) {
            method.image.getDisplay().adjustDisplay(depth, width, height, fullscreen);
            return receiver;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!method.image.hasDisplay()")
        protected static final AbstractSqueakObject doSetHeadless(final AbstractSqueakObject receiver, final long depth, final long width, final long height, final boolean fullscreen) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 93)
    protected abstract static class PrimInputSemaphoreNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimInputSemaphoreNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "method.image.hasDisplay()")
        protected final AbstractSqueakObject doSet(final AbstractSqueakObject receiver, final long semaIndex) {
            method.image.getDisplay().setInputSemaphoreIndex((int) semaIndex);
            return receiver;
        }

        @Specialization(guards = "!method.image.hasDisplay()")
        protected static final AbstractSqueakObject doSetHeadless(final AbstractSqueakObject receiver, @SuppressWarnings("unused") final long semaIndex) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 94)
    protected abstract static class PrimGetNextEventNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimGetNextEventNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "method.image.hasDisplay()")
        protected final PointersObject doGetNext(final PointersObject eventSensor, final ArrayObject targetArray) {
            targetArray.setStorage(method.image.getDisplay().getNextEvent());
            return eventSensor;
        }

        @Specialization(guards = "!method.image.hasDisplay()")
        protected static final PointersObject doGetNextHeadless(final PointersObject eventSensor, @SuppressWarnings("unused") final ArrayObject targetArray) {
            targetArray.setStorage(SqueakIOConstants.NULL_EVENT);
            return eventSensor;
        }
    }

    /** Primitive 96 (primitiveCopyBits) not in use anymore. */

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 97)
    protected abstract static class PrimSnapshotNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        public PrimSnapshotNode(final CompiledMethodObject method) {
            super(method);
        }

        @SuppressWarnings("unused")
        @Specialization
        public static final Object doSnapshot(final VirtualFrame frame, final PointersObject receiver) {
            // TODO: implement primitiveSnapshot
            throw new PrimitiveFailed();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 98)
    protected abstract static class PrimStoreImageSegmentNode extends AbstractPrimitiveNode implements QuaternaryPrimitive {

        protected PrimStoreImageSegmentNode(final CompiledMethodObject method) {
            super(method);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "segmentWordArray.isIntType()")
        protected static final AbstractSqueakObject doStore(final AbstractSqueakObject receiver, final ArrayObject rootsArray, final NativeObject segmentWordArray, final ArrayObject outPointerArray) {
            /**
             * TODO: implement primitive. In the meantime, pretend this primitive succeeds so that
             * some tests (e.g. BitmapStreamTests) run quickly.
             */
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 99)
    protected abstract static class PrimLoadImageSegmentNode extends AbstractPrimitiveNode implements TernaryPrimitive {

        protected PrimLoadImageSegmentNode(final CompiledMethodObject method) {
            super(method);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "segmentWordArray.isIntType()")
        protected final ArrayObject doLoad(final AbstractSqueakObject receiver, final NativeObject segmentWordArray, final ArrayObject outPointerArray) {
            /**
             * TODO: implement primitive. In the meantime, pretend this primitive succeeds so that
             * some tests (e.g. BitmapStreamTests) run quickly.
             */
            return method.image.newArrayEmpty();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 101)
    protected abstract static class PrimBeCursorNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimBeCursorNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "method.image.hasDisplay()")
        protected final PointersObject doCursor(final PointersObject receiver, @SuppressWarnings("unused") final NotProvided mask) {
            method.image.getDisplay().setCursor(validateAndExtractWords(receiver), null, extractDepth(receiver));
            return receiver;
        }

        @Specialization(guards = "method.image.hasDisplay()")
        protected final PointersObject doCursor(final PointersObject receiver, final PointersObject maskObject) {
            final int[] words = validateAndExtractWords(receiver);
            final int depth = extractDepth(receiver);
            if (depth == 1) {
                final int[] mask = ((NativeObject) maskObject.at0(FORM.BITS)).getIntStorage();
                method.image.getDisplay().setCursor(words, mask, 2);
            } else {
                method.image.getDisplay().setCursor(words, null, depth);
            }
            return receiver;
        }

        @Specialization(guards = "!method.image.hasDisplay()")
        protected static final PointersObject doCursorHeadless(final PointersObject receiver, @SuppressWarnings("unused") final NotProvided mask) {
            return receiver;
        }

        @Specialization(guards = "!method.image.hasDisplay()")
        protected static final PointersObject doCursorHeadless(final PointersObject receiver, @SuppressWarnings("unused") final PointersObject maskObject) {
            return receiver;
        }

        private int[] validateAndExtractWords(final PointersObject receiver) {
            final int[] words = ((NativeObject) receiver.at0(FORM.BITS)).getIntStorage();
            final long width = (long) receiver.at0(FORM.WIDTH);
            final long height = (long) receiver.at0(FORM.HEIGHT);
            if (width != SqueakIOConstants.CURSOR_WIDTH || height != SqueakIOConstants.CURSOR_HEIGHT) {
                CompilerDirectives.transferToInterpreter();
                method.image.printToStdErr("Unexpected cursor width:", width, "or height:", height, ". Proceeding with cropped cursor...");
                return Arrays.copyOf(words, SqueakIOConstants.CURSOR_WIDTH * SqueakIOConstants.CURSOR_HEIGHT);
            }
            return words;
        }

        private static int extractDepth(final PointersObject receiver) {
            return (int) (long) receiver.at0(FORM.DEPTH);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 102)
    protected abstract static class PrimBeDisplayNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimBeDisplayNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"method.image.hasDisplay()", "receiver.size() >= 4"})
        protected final boolean doDisplay(final PointersObject receiver) {
            method.image.specialObjectsArray.atput0Object(SPECIAL_OBJECT.THE_DISPLAY, receiver);
            method.image.getDisplay().open(receiver);
            return method.image.sqTrue;
        }

        @Specialization(guards = {"!method.image.hasDisplay()"})
        protected final boolean doDisplayHeadless(@SuppressWarnings("unused") final PointersObject receiver) {
            return method.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 103)
    protected abstract static class PrimScanCharactersNode extends AbstractPrimitiveNode implements SeptenaryPrimitive {
        private static final long END_OF_RUN = 257 - 1;
        private static final long CROSSED_X = 258 - 1;

        @Child private ArrayObjectReadNode readNode = ArrayObjectReadNode.create();
        @Child protected ArrayObjectSizeNode sizeNode = ArrayObjectSizeNode.create();

        protected PrimScanCharactersNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"startIndex > 0", "stopIndex > 0", "sourceString.isByteType()", "stopIndex <= sourceString.getByteLength()", "receiver.size() >= 4",
                        "sizeNode.execute(stops) >= 258", "hasCorrectSlots(receiver)"})
        protected final Object doScan(final PointersObject receiver, final long startIndex, final long stopIndex, final NativeObject sourceString, final long rightX,
                        final ArrayObject stops, final long kernData) {
            final ArrayObject scanXTable = (ArrayObject) receiver.at0(CHARACTER_SCANNER.XTABLE);
            final ArrayObject scanMap = (ArrayObject) receiver.at0(CHARACTER_SCANNER.MAP);
            final byte[] sourceBytes = sourceString.getByteStorage();

            final int maxGlyph = sizeNode.execute(scanXTable) - 2;
            long scanDestX = (long) receiver.at0(CHARACTER_SCANNER.DEST_X);
            long scanLastIndex = startIndex;
            while (scanLastIndex <= stopIndex) {
                final long ascii = sourceBytes[(int) (scanLastIndex - 1)] & 0xFF;
                final Object stopReason = readNode.execute(stops, ascii);
                if (stopReason != method.image.nil) {
                    storeStateInReceiver(receiver, scanDestX, scanLastIndex);
                    return stopReason;
                }
                if (ascii < 0 || sizeNode.execute(scanMap) <= ascii) {
                    throw PrimitiveFailed.andTransferToInterpreter();
                }
                final long glyphIndex = (long) readNode.execute(scanMap, ascii);
                if (glyphIndex < 0 || glyphIndex > maxGlyph) {
                    throw PrimitiveFailed.andTransferToInterpreter();
                }
                final long sourceX1;
                final long sourceX2;
                try {
                    sourceX1 = (long) readNode.execute(scanXTable, glyphIndex);
                    sourceX2 = (long) readNode.execute(scanXTable, glyphIndex + 1);
                } catch (final ClassCastException e) {
                    throw PrimitiveFailed.andTransferToInterpreter();
                }
                final long nextDestX = scanDestX + sourceX2 - sourceX1;
                if (nextDestX > rightX) {
                    storeStateInReceiver(receiver, scanDestX, scanLastIndex);
                    return readNode.execute(stops, CROSSED_X);
                }
                scanDestX = nextDestX + kernData;
                scanLastIndex++;
            }
            storeStateInReceiver(receiver, scanDestX, stopIndex);
            return readNode.execute(stops, END_OF_RUN);
        }

        private static void storeStateInReceiver(final PointersObject receiver, final long scanDestX, final long scanLastIndex) {
            receiver.atput0(CHARACTER_SCANNER.DEST_X, scanDestX);
            receiver.atput0(CHARACTER_SCANNER.LAST_INDEX, scanLastIndex);
        }

        protected final boolean hasCorrectSlots(final PointersObject receiver) {
            final Object scanMap = receiver.at0(CHARACTER_SCANNER.MAP);
            return receiver.at0(CHARACTER_SCANNER.DEST_X) instanceof Long && receiver.at0(CHARACTER_SCANNER.XTABLE) instanceof ArrayObject &&
                            scanMap instanceof ArrayObject && sizeNode.execute((ArrayObject) scanMap) == 256;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 105)
    protected abstract static class PrimStringReplaceNode extends AbstractPrimitiveNode implements QuinaryPrimitive {
        @Child private SqueakObjectInstSizeNode instSizeNode;
        @Child private SqueakObjectSizeNode sizeNode;
        @Child private ArrayObjectReadNode arrayObjectReadNode;
        @Child private ArrayObjectWriteNode arrayObjectWriteNode;
        @Child private NativeObjectReadNode nativeObjectReadNode;
        @Child private NativeObjectWriteNode nativeObjectWriteNode;
        @Child private ArrayObjectToObjectArrayNode getObjectArrayNode;

        protected PrimStringReplaceNode(final CompiledMethodObject method) {
            super(method);
        }

        public abstract Object executeReplace(VirtualFrame frame);

        @Specialization(guards = "!isSmallInteger(repl)")
        protected final Object replace(final LargeIntegerObject rcvr, final long start, final long stop, final long repl, final long replStart) {
            return doLargeInteger(rcvr, start, stop, asLargeInteger(repl), replStart);
        }

        @Specialization(guards = "inBounds(rcvr.instsize(), rcvr.size(), start, stop, repl.instsize(), repl.size(), replStart)")
        protected static final LargeIntegerObject doLargeInteger(final LargeIntegerObject rcvr, final long start, final long stop, final LargeIntegerObject repl, final long replStart) {
            rcvr.setBytes(repl.getBytes(), (int) replStart - 1, (int) start - 1, (int) (1 + stop - start));
            return rcvr;
        }

        @Specialization(guards = "inBounds(rcvr.instsize(), rcvr.size(), start, stop, repl.instsize(), repl.size(), replStart)")
        protected static final LargeIntegerObject doLargeIntegerFloat(final LargeIntegerObject rcvr, final long start, final long stop, final FloatObject repl, final long replStart) {
            System.arraycopy(repl.getBytes(), (int) replStart - 1, rcvr.getBytes(), (int) start - 1, (int) (1 + stop - start));
            return rcvr;
        }

        @Specialization(guards = {"repl.isByteType()", "inBounds(rcvr.instsize(), rcvr.size(), start, stop, repl.instsize(), repl.getByteLength(), replStart)"})
        protected static final LargeIntegerObject doLargeIntegerNative(final LargeIntegerObject rcvr, final long start, final long stop, final NativeObject repl, final long replStart) {
            rcvr.setBytes(repl.getByteStorage(), (int) replStart - 1, (int) start - 1, (int) (1 + stop - start));
            return rcvr;
        }

        @Specialization(guards = {"rcvr.haveSameStorageType(repl)", "inBounds(rcvr, start, stop, repl, replStart)"})
        protected final NativeObject doNative(final NativeObject rcvr, final long start, final long stop, final NativeObject repl, final long replStart) {
            final int repOff = (int) (replStart - start);
            for (int i = (int) (start - 1); i < stop; i++) {
                getNativeObjectWriteNode().execute(rcvr, i, getNativeObjectReadNode().execute(repl, repOff + i));
            }
            return rcvr;
        }

        @Specialization(guards = "!isSmallInteger(repl)")
        protected final NativeObject doNativeLargeInteger(final NativeObject rcvr, final long start, final long stop, final long repl, final long replStart) {
            return doNativeLargeInteger(rcvr, start, stop, asLargeInteger(repl), replStart);
        }

        @Specialization(guards = "inBounds(rcvr, start, stop, repl, replStart)")
        protected final NativeObject doNativeLargeInteger(final NativeObject rcvr, final long start, final long stop, final LargeIntegerObject repl, final long replStart) {
            final int repOff = (int) (replStart - start);
            for (int i = (int) (start - 1); i < stop; i++) {
                getNativeObjectWriteNode().execute(rcvr, i, repl.getNativeAt0(repOff + i));
            }
            return rcvr;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "inBounds(rcvr, start, stop, repl, replStart)")
        protected static final Object doNativeFloat(final NativeObject rcvr, final long start, final long stop, final FloatObject repl, final long replStart) {
            throw SqueakException.create("Not supported"); // TODO: check this is needed
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"rcvr.isEmptyType()", "repl.isEmptyType()", "inBounds(rcvr.instsize(), rcvr.getEmptyLength(), start, stop, repl.instsize(), repl.getEmptyLength(), replStart)"})
        protected static final ArrayObject doEmptyArrays(final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart) {
            return rcvr; // Nothing to do.
        }

        @Specialization(guards = {"rcvr.isEmptyType()", "repl.isAbstractSqueakObjectType()",
                        "inBounds(rcvr.instsize(), rcvr.getEmptyLength(), start, stop, repl.instsize(), repl.getAbstractSqueakObjectLength(), replStart)"})
        protected static final ArrayObject doEmptyArrayToSqueakObjects(final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart) {
            rcvr.transitionFromEmptyToAbstractSqueakObjects();
            return doArraysOfSqueakObjects(rcvr, start, stop, repl, replStart);
        }

        @Specialization(guards = {"rcvr.isEmptyType()", "repl.isBooleanType()", "inBounds(rcvr.instsize(), rcvr.getEmptyLength(), start, stop, repl.instsize(), repl.getBooleanLength(), replStart)"})
        protected static final ArrayObject doEmptyArrayToBooleans(final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart) {
            rcvr.transitionFromEmptyToBooleans();
            return doArraysOfBooleans(rcvr, start, stop, repl, replStart);
        }

        @Specialization(guards = {"rcvr.isEmptyType()", "repl.isCharType()", "inBounds(rcvr.instsize(), rcvr.getEmptyLength(), start, stop, repl.instsize(), repl.getCharLength(), replStart)"})
        protected static final ArrayObject doEmptyArrayToChars(final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart) {
            rcvr.transitionFromEmptyToChars();
            return doArraysOfChars(rcvr, start, stop, repl, replStart);
        }

        @Specialization(guards = {"rcvr.isEmptyType()", "repl.isLongType()", "inBounds(rcvr.instsize(), rcvr.getEmptyLength(), start, stop, repl.instsize(), repl.getLongLength(), replStart)"})
        protected static final ArrayObject doEmptyArrayToLongs(final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart) {
            rcvr.transitionFromEmptyToLongs();
            return doArraysOfLongs(rcvr, start, stop, repl, replStart);
        }

        @Specialization(guards = {"rcvr.isEmptyType()", "repl.isDoubleType()", "inBounds(rcvr.instsize(), rcvr.getEmptyLength(), start, stop, repl.instsize(), repl.getDoubleLength(), replStart)"})
        protected static final ArrayObject doEmptyArrayToDoubles(final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart) {
            rcvr.transitionFromEmptyToDoubles();
            return doArraysOfDoubles(rcvr, start, stop, repl, replStart);
        }

        @Specialization(guards = {"rcvr.isEmptyType()", "repl.isNativeObjectType()",
                        "inBounds(rcvr.instsize(), rcvr.getEmptyLength(), start, stop, repl.instsize(), repl.getNativeObjectLength(), replStart)"})
        protected static final ArrayObject doEmptyArrayToNatives(final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart) {
            rcvr.transitionFromEmptyToNatives();
            return doArraysOfNatives(rcvr, start, stop, repl, replStart);
        }

        @Specialization(guards = {"rcvr.isEmptyType()", "repl.isObjectType()", "inBounds(rcvr.instsize(), rcvr.getEmptyLength(), start, stop, repl.instsize(), repl.getObjectLength(), replStart)"})
        protected static final ArrayObject doEmptyArrayToObjects(final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart) {
            rcvr.transitionFromEmptyToObjects();
            return doArraysOfObjects(rcvr, start, stop, repl, replStart);
        }

        @Specialization(guards = {"rcvr.isAbstractSqueakObjectType()", "repl.isAbstractSqueakObjectType()",
                        "inBounds(rcvr.instsize(), rcvr.getAbstractSqueakObjectLength(), start, stop, repl.instsize(), repl.getAbstractSqueakObjectLength(), replStart)"})
        protected static final ArrayObject doArraysOfSqueakObjects(final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart) {
            System.arraycopy(repl.getAbstractSqueakObjectStorage(), (int) replStart - 1, rcvr.getAbstractSqueakObjectStorage(), (int) start - 1, (int) (1 + stop - start));
            return rcvr;
        }

        @Specialization(guards = {"rcvr.isAbstractSqueakObjectType()", "!repl.isAbstractSqueakObjectType()", "inBounds(rcvr, start, stop, repl, replStart)"})
        protected final ArrayObject doArraysOfSqueakObjectsTransition(final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart) {
            rcvr.transitionFromAbstractSqueakObjectsToObjects();
            replaceGeneric(rcvr.getObjectStorage(), start, stop, getGetObjectArrayNode().execute(repl), replStart);
            return rcvr;
        }

        @Specialization(guards = {"rcvr.isBooleanType()", "repl.isBooleanType()",
                        "inBounds(rcvr.instsize(), rcvr.getBooleanLength(), start, stop, repl.instsize(), repl.getBooleanLength(), replStart)"})
        protected static final ArrayObject doArraysOfBooleans(final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart) {
            System.arraycopy(repl.getBooleanStorage(), (int) replStart - 1, rcvr.getBooleanStorage(), (int) start - 1, (int) (1 + stop - start));
            return rcvr;
        }

        @Specialization(guards = {"rcvr.isBooleanType()", "!repl.isBooleanType()",
                        "inBounds(rcvr.instsize(), rcvr.getBooleanLength(), start, stop, repl.instsize(), repl.getBooleanLength(), replStart)"})
        protected final ArrayObject doArraysOfBooleansTransition(final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart) {
            rcvr.transitionFromBooleansToObjects();
            replaceGeneric(rcvr.getObjectStorage(), start, stop, getGetObjectArrayNode().execute(repl), replStart);
            return rcvr;
        }

        @Specialization(guards = {"rcvr.isCharType()", "repl.isCharType()", "inBounds(rcvr.instsize(), rcvr.getCharLength(), start, stop, repl.instsize(), repl.getCharLength(), replStart)"})
        protected static final ArrayObject doArraysOfChars(final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart) {
            System.arraycopy(repl.getCharStorage(), (int) replStart - 1, rcvr.getCharStorage(), (int) start - 1, (int) (1 + stop - start));
            return rcvr;
        }

        @Specialization(guards = {"rcvr.isCharType()", "!repl.isCharType()", "inBounds(rcvr, start, stop, repl, replStart)"})
        protected final ArrayObject doArraysOfCharsTransition(final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart) {
            rcvr.transitionFromCharsToObjects();
            replaceGeneric(rcvr.getObjectStorage(), start, stop, getGetObjectArrayNode().execute(repl), replStart);
            return rcvr;
        }

        @Specialization(guards = {"rcvr.isLongType()", "repl.isLongType()", "inBounds(rcvr.instsize(), rcvr.getLongLength(), start, stop, repl.instsize(), repl.getLongLength(), replStart)"})
        protected static final ArrayObject doArraysOfLongs(final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart) {
            System.arraycopy(repl.getLongStorage(), (int) replStart - 1, rcvr.getLongStorage(), (int) start - 1, (int) (1 + stop - start));
            return rcvr;
        }

        @Specialization(guards = {"rcvr.isLongType()", "!repl.isLongType()", "inBounds(rcvr, start, stop, repl, replStart)"})
        protected final ArrayObject doArraysOfLongsTransition(final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart) {
            rcvr.transitionFromLongsToObjects();
            replaceGeneric(rcvr.getObjectStorage(), start, stop, getGetObjectArrayNode().execute(repl), replStart);
            return rcvr;
        }

        @Specialization(guards = {"rcvr.isDoubleType()", "repl.isDoubleType()", "inBounds(rcvr.instsize(), rcvr.getDoubleLength(), start, stop, repl.instsize(), repl.getDoubleLength(), replStart)"})
        protected static final ArrayObject doArraysOfDoubles(final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart) {
            System.arraycopy(repl.getDoubleStorage(), (int) replStart - 1, rcvr.getDoubleStorage(), (int) start - 1, (int) (1 + stop - start));
            return rcvr;
        }

        @Specialization(guards = {"rcvr.isDoubleType()", "!repl.isDoubleType()", "inBounds(rcvr, start, stop, repl, replStart)"})
        protected final ArrayObject doArraysOfDoublesTransition(final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart) {
            rcvr.transitionFromDoublesToObjects();
            replaceGeneric(rcvr.getObjectStorage(), start, stop, getGetObjectArrayNode().execute(repl), replStart);
            return rcvr;
        }

        @Specialization(guards = {"rcvr.isNativeObjectType()", "repl.isNativeObjectType()",
                        "inBounds(rcvr.instsize(), rcvr.getNativeObjectLength(), start, stop, repl.instsize(), repl.getNativeObjectLength(), replStart)"})
        protected static final ArrayObject doArraysOfNatives(final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart) {
            System.arraycopy(repl.getNativeObjectStorage(), (int) replStart - 1, rcvr.getNativeObjectStorage(), (int) start - 1, (int) (1 + stop - start));
            return rcvr;
        }

        @Specialization(guards = {"rcvr.isNativeObjectType()", "!repl.isNativeObjectType()", "inBounds(rcvr, start, stop, repl, replStart)"})
        protected final ArrayObject doArraysOfNativesTransition(final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart) {
            rcvr.transitionFromNativesToObjects();
            replaceGeneric(rcvr.getObjectStorage(), start, stop, getGetObjectArrayNode().execute(repl), replStart);
            return rcvr;
        }

        @Specialization(guards = {"rcvr.isObjectType()", "repl.isObjectType()", "inBounds(rcvr.instsize(), rcvr.getObjectLength(), start, stop, repl.instsize(), repl.getObjectLength(), replStart)"})
        protected static final ArrayObject doArraysOfObjects(final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart) {
            System.arraycopy(repl.getObjectStorage(), (int) replStart - 1, rcvr.getObjectStorage(), (int) start - 1, (int) (1 + stop - start));
            return rcvr;
        }

        @Specialization(guards = {"rcvr.isObjectType()", "!repl.isObjectType()", "inBounds(rcvr, start, stop, repl, replStart)"})
        protected final ArrayObject doArraysOfObjectsNonObject(final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart) {
            System.arraycopy(getGetObjectArrayNode().execute(repl), (int) replStart - 1, rcvr.getObjectStorage(), (int) start - 1, (int) (1 + stop - start));
            return rcvr;
        }

        @Specialization(guards = {"rcvr.isEmptyType()", "inBounds(rcvr.instsize(), rcvr.getEmptyLength(), start, stop, repl.instsize(), repl.size(), replStart)"})
        protected final ArrayObject doEmptyArrayPointers(final ArrayObject rcvr, final long start, final long stop, final PointersObject repl, final long replStart) {
            return doArrayOfSpecializedPointers(rcvr, start, stop, repl, replStart);
        }

        @Specialization(guards = {"rcvr.isAbstractSqueakObjectType()", "inBounds(rcvr.instsize(), rcvr.getAbstractSqueakObjectLength(), start, stop, repl.instsize(), repl.size(), replStart)"})
        protected final ArrayObject doArrayOfSqueakObjectPointers(final ArrayObject rcvr, final long start, final long stop, final PointersObject repl, final long replStart) {
            return doArrayOfSpecializedPointers(rcvr, start, stop, repl, replStart);
        }

        @Specialization(guards = {"rcvr.isBooleanType()", "inBounds(rcvr.instsize(), rcvr.getBooleanLength(), start, stop, repl.instsize(), repl.size(), replStart)"})
        protected final ArrayObject doArrayOfBooleansPointers(final ArrayObject rcvr, final long start, final long stop, final PointersObject repl, final long replStart) {
            return doArrayOfSpecializedPointers(rcvr, start, stop, repl, replStart);
        }

        @Specialization(guards = {"rcvr.isCharType()", "inBounds(rcvr.instsize(), rcvr.getCharLength(), start, stop, repl.instsize(), repl.size(), replStart)"})
        protected final ArrayObject doArrayOfCharsPointers(final ArrayObject rcvr, final long start, final long stop, final PointersObject repl, final long replStart) {
            return doArrayOfSpecializedPointers(rcvr, start, stop, repl, replStart);
        }

        @Specialization(guards = {"rcvr.isLongType()", "inBounds(rcvr.instsize(), rcvr.getLongLength(), start, stop, repl.instsize(), repl.size(), replStart)"})
        protected final ArrayObject doArrayOfLongsPointers(final ArrayObject rcvr, final long start, final long stop, final PointersObject repl, final long replStart) {
            return doArrayOfSpecializedPointers(rcvr, start, stop, repl, replStart);
        }

        @Specialization(guards = {"rcvr.isDoubleType()", "inBounds(rcvr.instsize(), rcvr.getDoubleLength(), start, stop, repl.instsize(), repl.size(), replStart)"})
        protected final ArrayObject doArrayOfDoublesPointers(final ArrayObject rcvr, final long start, final long stop, final PointersObject repl, final long replStart) {
            return doArrayOfSpecializedPointers(rcvr, start, stop, repl, replStart);
        }

        @Specialization(guards = {"rcvr.isNativeObjectType()", "inBounds(rcvr.instsize(), rcvr.getNativeObjectLength(), start, stop, repl.instsize(), repl.size(), replStart)"})
        protected final ArrayObject doArrayOfDoublesNatives(final ArrayObject rcvr, final long start, final long stop, final PointersObject repl, final long replStart) {
            return doArrayOfSpecializedPointers(rcvr, start, stop, repl, replStart);
        }

        private ArrayObject doArrayOfSpecializedPointers(final ArrayObject rcvr, final long start, final long stop, final PointersObject repl, final long replStart) {
            final long repOff = replStart - start;
            for (int i = (int) (start - 1); i < stop; i++) {
                getArrayObjectWriteNode().execute(rcvr, i, repl.at0(repOff + i));
            }
            return rcvr;
        }

        @Specialization(guards = {"rcvr.isEmptyType()", "inBounds(rcvr.instsize(), rcvr.getEmptyLength(), start, stop, repl.instsize(), repl.size(), replStart)"})
        protected final ArrayObject doEmptyArrayWeakPointers(final ArrayObject rcvr, final long start, final long stop, final WeakPointersObject repl, final long replStart) {
            return doArrayOfSpecializedWeakPointers(rcvr, start, stop, repl, replStart);
        }

        @Specialization(guards = {"rcvr.isAbstractSqueakObjectType()", "inBounds(rcvr.instsize(), rcvr.getAbstractSqueakObjectLength(), start, stop, repl.instsize(), repl.size(), replStart)"})
        protected final ArrayObject doArrayOfSqueakObjectWeakPointers(final ArrayObject rcvr, final long start, final long stop, final WeakPointersObject repl, final long replStart) {
            return doArrayOfSpecializedWeakPointers(rcvr, start, stop, repl, replStart);
        }

        @Specialization(guards = {"rcvr.isBooleanType()", "inBounds(rcvr.instsize(), rcvr.getBooleanLength(), start, stop, repl.instsize(), repl.size(), replStart)"})
        protected final ArrayObject doArrayOfBooleansWeakPointers(final ArrayObject rcvr, final long start, final long stop, final WeakPointersObject repl, final long replStart) {
            return doArrayOfSpecializedWeakPointers(rcvr, start, stop, repl, replStart);
        }

        @Specialization(guards = {"rcvr.isCharType()", "inBounds(rcvr.instsize(), rcvr.getCharLength(), start, stop, repl.instsize(), repl.size(), replStart)"})
        protected final ArrayObject doArrayOfCharsWeakPointers(final ArrayObject rcvr, final long start, final long stop, final WeakPointersObject repl, final long replStart) {
            return doArrayOfSpecializedWeakPointers(rcvr, start, stop, repl, replStart);
        }

        @Specialization(guards = {"rcvr.isLongType()", "inBounds(rcvr.instsize(), rcvr.getLongLength(), start, stop, repl.instsize(), repl.size(), replStart)"})
        protected final ArrayObject doArrayOfLongsWeakPointers(final ArrayObject rcvr, final long start, final long stop, final WeakPointersObject repl, final long replStart) {
            return doArrayOfSpecializedWeakPointers(rcvr, start, stop, repl, replStart);
        }

        @Specialization(guards = {"rcvr.isDoubleType()", "inBounds(rcvr.instsize(), rcvr.getDoubleLength(), start, stop, repl.instsize(), repl.size(), replStart)"})
        protected final ArrayObject doArrayOfDoublesWeakPointers(final ArrayObject rcvr, final long start, final long stop, final WeakPointersObject repl, final long replStart) {
            return doArrayOfSpecializedWeakPointers(rcvr, start, stop, repl, replStart);
        }

        @Specialization(guards = {"rcvr.isNativeObjectType()", "inBounds(rcvr.instsize(), rcvr.getNativeObjectLength(), start, stop, repl.instsize(), repl.size(), replStart)"})
        protected final ArrayObject doArrayOfDoublesWeakNatives(final ArrayObject rcvr, final long start, final long stop, final WeakPointersObject repl, final long replStart) {
            return doArrayOfSpecializedWeakPointers(rcvr, start, stop, repl, replStart);
        }

        private ArrayObject doArrayOfSpecializedWeakPointers(final ArrayObject rcvr, final long start, final long stop, final WeakPointersObject repl, final long replStart) {
            final long repOff = replStart - start;
            for (int i = (int) (start - 1); i < stop; i++) {
                getArrayObjectWriteNode().execute(rcvr, i, repl.at0(repOff + i));
            }
            return rcvr;
        }

        @Specialization(guards = {"rcvr.isObjectType()", "inBounds(rcvr.instsize(), rcvr.getObjectLength(), start, stop, repl.instsize(), repl.size(), replStart)"})
        protected static final ArrayObject doArrayOfObjectsPointers(final ArrayObject rcvr, final long start, final long stop, final AbstractPointersObject repl, final long replStart) {
            System.arraycopy(repl.getPointers(), (int) replStart - 1, rcvr.getObjectStorage(), (int) start - 1, (int) (1 + stop - start));
            return rcvr;
        }

        @Specialization(guards = "inBounds(rcvr.instsize(), rcvr.size(), start, stop, repl.instsize(), repl.size(), replStart)")
        protected static final PointersObject doPointers(final PointersObject rcvr, final long start, final long stop, final PointersObject repl, final long replStart) {
            System.arraycopy(repl.getPointers(), (int) replStart - 1, rcvr.getPointers(), (int) start - 1, (int) (1 + stop - start));
            return rcvr;
        }

        @Specialization(guards = "inBounds(rcvr.instsize(), rcvr.size(), start, stop, repl.instsize(), repl.size(), replStart)")
        protected static final PointersObject doPointersWeakPointers(final PointersObject rcvr, final long start, final long stop, final WeakPointersObject repl, final long replStart) {
            final long repOff = replStart - start;
            for (int i = (int) (start - 1); i < stop; i++) {
                rcvr.atput0(i, repl.at0(repOff + i));
            }
            return rcvr;
        }

        @Specialization(guards = "inBounds(rcvr.instsize(), rcvr.size(), start, stop, repl.instsize(), repl.size(), replStart)")
        protected static final WeakPointersObject doWeakPointers(final WeakPointersObject rcvr, final long start, final long stop, final WeakPointersObject repl, final long replStart) {
            System.arraycopy(repl.getPointers(), (int) replStart - 1, rcvr.getPointers(), (int) start - 1, (int) (1 + stop - start));
            return rcvr;
        }

        @Specialization(guards = "inBounds(rcvr.instsize(), rcvr.size(), start, stop, repl.instsize(), repl.size(), replStart)")
        protected static final WeakPointersObject doWeakPointersPointers(final WeakPointersObject rcvr, final long start, final long stop, final PointersObject repl, final long replStart) {
            final long repOff = replStart - start;
            for (int i = (int) (start - 1); i < stop; i++) {
                rcvr.atput0(i, repl.at0(repOff + i));
            }
            return rcvr;
        }

        @Specialization(guards = "inBounds(rcvr, start, stop, repl, replStart)")
        protected final WeakPointersObject doWeakPointersArray(final WeakPointersObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart) {
            final long repOff = replStart - start;
            for (int i = (int) (start - 1); i < stop; i++) {
                rcvr.atput0(i, getArrayObjectReadNode().execute(repl, repOff + i));
            }
            return rcvr;
        }

        @Specialization(guards = "inBounds(rcvr.instsize(), rcvr.size(), start, stop, repl.instsize(), repl.size(), replStart)")
        protected static final CompiledBlockObject doBlock(final CompiledBlockObject rcvr, final long start, final long stop, final CompiledBlockObject repl, final long replStart) {
            final long repOff = replStart - start;
            for (int i = (int) (start - 1); i < stop; i++) {
                rcvr.atput0(i, repl.at0(repOff + i));
            }
            return rcvr;
        }

        @Specialization(guards = "inBounds(rcvr.instsize(), rcvr.size(), start, stop, repl.instsize(), repl.size(), replStart)")
        protected static final CompiledMethodObject doMethod(final CompiledMethodObject rcvr, final long start, final long stop, final CompiledMethodObject repl, final long replStart) {
            final long repOff = replStart - start;
            for (int i = (int) (start - 1); i < stop; i++) {
                rcvr.atput0(i, repl.at0(repOff + i));
            }
            return rcvr;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!inBounds(rcvr, start, stop, repl, replStart)")
        protected static final AbstractSqueakObject doBadIndex(final AbstractSqueakObject rcvr, final long start, final long stop, final AbstractSqueakObject repl, final long replStart) {
            throw new PrimitiveFailed(ERROR_TABLE.BAD_INDEX);
        }

        protected static final boolean inBounds(final AbstractPointersObject target, final long start, final long stop, final AbstractPointersObject repl, final long replStart) {
            return start >= 1 && start - 1 <= stop && stop + target.instsize() <= target.size() &&
                            replStart >= 1 && stop - start + replStart + repl.instsize() <= repl.size();
        }

        protected static final boolean inBounds(final int rcvrInstSize, final int rcvrSize, final long start, final long stop, final int replInstSize, final int replSize, final long replStart) {
            return start >= 1 && start - 1 <= stop && stop + rcvrInstSize <= rcvrSize && replStart >= 1 && stop - start + replStart + replInstSize <= replSize;
        }

        protected final boolean inBounds(final AbstractSqueakObject array, final long start, final long stop, final AbstractSqueakObject repl, final long replStart) {
            return start >= 1 && start - 1 <= stop && stop + getInstSizeNode().execute(array) <= getSizeNode().execute(array) &&
                            replStart >= 1 && stop - start + replStart + getInstSizeNode().execute(repl) <= getSizeNode().execute(repl);
        }

        private static void replaceGeneric(final Object[] dstArray, final long start, final long stop, final Object[] srcArray, final long replStart) {
            System.arraycopy(srcArray, (int) replStart - 1, dstArray, (int) start - 1, (int) (1 + stop - start));
        }

        private SqueakObjectInstSizeNode getInstSizeNode() {
            if (instSizeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                instSizeNode = insert(SqueakObjectInstSizeNode.create());
            }
            return instSizeNode;
        }

        private SqueakObjectSizeNode getSizeNode() {
            if (sizeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                sizeNode = insert(SqueakObjectSizeNode.create());
            }
            return sizeNode;
        }

        private ArrayObjectReadNode getArrayObjectReadNode() {
            if (arrayObjectReadNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                arrayObjectReadNode = insert(ArrayObjectReadNode.create());
            }
            return arrayObjectReadNode;
        }

        private ArrayObjectWriteNode getArrayObjectWriteNode() {
            if (arrayObjectWriteNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                arrayObjectWriteNode = insert(ArrayObjectWriteNode.create());
            }
            return arrayObjectWriteNode;
        }

        private NativeObjectReadNode getNativeObjectReadNode() {
            if (nativeObjectReadNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                nativeObjectReadNode = insert(NativeObjectReadNode.create());
            }
            return nativeObjectReadNode;
        }

        private NativeObjectWriteNode getNativeObjectWriteNode() {
            if (nativeObjectWriteNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                nativeObjectWriteNode = insert(NativeObjectWriteNode.create());
            }
            return nativeObjectWriteNode;
        }

        private ArrayObjectToObjectArrayNode getGetObjectArrayNode() {
            if (getObjectArrayNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getObjectArrayNode = insert(ArrayObjectToObjectArrayNode.create());
            }
            return getObjectArrayNode;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 106)
    protected abstract static class PrimScreenSizeNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimScreenSizeNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"hasVisibleDisplay(receiver)"})
        protected final PointersObject doSize(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            return method.image.wrap(method.image.getDisplay().getWindowSize());
        }

        @Specialization(guards = "!hasVisibleDisplay(receiver)")
        protected final PointersObject doSizeHeadless(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            return method.image.wrap(method.image.flags.getLastWindowSize());
        }

        // guard helper to work around code generation issue.
        protected final boolean hasVisibleDisplay(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            return method.image.hasDisplay() && method.image.getDisplay().isVisible();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 107)
    protected abstract static class PrimMouseButtonsNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimMouseButtonsNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "method.image.hasDisplay()")
        protected final long doMouseButtons(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            return method.image.wrap(method.image.getDisplay().getLastMouseButton());
        }

        @Specialization(guards = "!method.image.hasDisplay()")
        protected final long doMouseButtonsHeadless(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            return method.image.wrap(0L);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 108)
    protected abstract static class PrimKeyboardNextNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimKeyboardNextNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "method.image.hasDisplay()")
        protected final Object doNext(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            final int keyboardNext = method.image.getDisplay().keyboardNext();
            if (keyboardNext == 0) {
                return method.image.nil;
            } else {
                return method.image.wrap(keyboardNext);
            }
        }

        @Specialization(guards = "!method.image.hasDisplay()")
        protected final NilObject doNextHeadless(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            return method.image.nil;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 109)
    protected abstract static class PrimKeyboardPeekNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimKeyboardPeekNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "method.image.hasDisplay()")
        protected final Object doPeek(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            final int keyboardPeek = method.image.getDisplay().keyboardPeek();
            if (keyboardPeek == 0) {
                return method.image.nil;
            } else {
                return method.image.wrap(keyboardPeek);
            }
        }

        @Specialization(guards = "!method.image.hasDisplay()")
        protected final NilObject doPeekHeadless(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            return method.image.nil;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 126)
    protected abstract static class PrimDeferDisplayUpdatesNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        public PrimDeferDisplayUpdatesNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "method.image.hasDisplay()")
        protected final AbstractSqueakObject doDefer(final AbstractSqueakObject receiver, final boolean flag) {
            method.image.getDisplay().setDeferUpdates(flag);
            return receiver;
        }

        @Specialization(guards = "!method.image.hasDisplay()")
        protected static final AbstractSqueakObject doNothing(final AbstractSqueakObject receiver, @SuppressWarnings("unused") final boolean flag) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 127)
    protected abstract static class PrimShowDisplayRectNode extends AbstractPrimitiveNode implements QuinaryPrimitive {

        protected PrimShowDisplayRectNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"method.image.hasDisplay()", "left < right", "top < bottom"})
        protected final PointersObject doShow(final PointersObject receiver, final long left, final long right, final long top, final long bottom) {
            method.image.getDisplay().showDisplayRect((int) left, (int) right, (int) top, (int) bottom);
            return receiver;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!method.image.hasDisplay() || (left > right || top > bottom)"})
        protected static final PointersObject doDrawHeadless(final PointersObject receiver, final long left, final long right, final long top, final long bottom) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 133)
    protected abstract static class PrimSetInterruptKeyNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimSetInterruptKeyNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final AbstractSqueakObject set(final AbstractSqueakObject receiver) {
            // TODO: interrupt key is obsolete in image, but maybe still needed in the vm?
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 140)
    protected abstract static class PrimBeepNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimBeepNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "method.image.hasDisplay()")
        protected final AbstractSqueakObject doBeep(final AbstractSqueakObject receiver) {
            method.image.getDisplay().beep();
            return receiver;
        }

        @Specialization(guards = "!method.image.hasDisplay()")
        protected final AbstractSqueakObject doNothing(final AbstractSqueakObject receiver) {
            method.image.printToStdOut((char) 7);
            return receiver;
        }
    }
}
