package de.hpi.swa.graal.squeak.nodes.primitives.impl;

import java.util.Arrays;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.io.DisplayPoint;
import de.hpi.swa.graal.squeak.io.SqueakIOConstants;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.CompiledBlockObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NotProvided;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.CHARACTER_SCANNER;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.ERROR_TABLE;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.FORM;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.SPECIAL_OBJECT;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.WeakPointersObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithImage;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectSizeNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.GetObjectArrayNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ReadArrayObjectNode;
import de.hpi.swa.graal.squeak.nodes.accessing.NativeObjectNodes.NativeObjectReadNode;
import de.hpi.swa.graal.squeak.nodes.accessing.NativeObjectNodes.NativeObjectWriteNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAtPut0Node;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectInstSizeNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectSizeNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.QuinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.SeptenaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.IOPrimitivesFactory.PrimScanCharactersNodeFactory.ScanCharactersHelperNodeGen;

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

        @Specialization(guards = "code.image.hasDisplay()")
        protected final Object doMousePoint(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            return code.image.wrap(code.image.getDisplay().getLastMousePosition());
        }

        @Specialization(guards = "!code.image.hasDisplay()")
        protected final Object doMousePointHeadless(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            return code.image.wrap(NULL_POINT);
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
    @SqueakPrimitive(indices = 92)
    protected abstract static class PrimSetDisplayModeNode extends AbstractPrimitiveNode implements QuinaryPrimitive {

        protected PrimSetDisplayModeNode(final CompiledMethodObject method) {
            super(method);
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
    @SqueakPrimitive(indices = 93)
    protected abstract static class PrimInputSemaphoreNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimInputSemaphoreNode(final CompiledMethodObject method) {
            super(method);
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
    @SqueakPrimitive(indices = 94)
    protected abstract static class PrimGetNextEventNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimGetNextEventNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "code.image.hasDisplay()")
        protected final PointersObject doGetNext(final PointersObject eventSensor, final ArrayObject targetArray) {
            targetArray.setStorage(code.image.getDisplay().getNextEvent());
            return eventSensor;
        }

        @Specialization(guards = "!code.image.hasDisplay()")
        protected static final PointersObject doGetNextHeadless(final PointersObject eventSensor, @SuppressWarnings("unused") final ArrayObject targetArray) {
            targetArray.setStorage(SqueakIOConstants.NULL_EVENT);
            return eventSensor;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 96)
    protected abstract static class PrimCopyBitsNode extends SimulationPrimitiveNode {

        protected PrimCopyBitsNode(final CompiledMethodObject method) {
            super(method, "BitBltPlugin", "primitiveCopyBits");
        }

    }

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
    @SqueakPrimitive(indices = 101)
    protected abstract static class PrimBeCursorNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimBeCursorNode(final CompiledMethodObject method) {
            super(method);
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

        private int[] validateAndExtractWords(final PointersObject receiver) {
            final int[] words = ((NativeObject) receiver.at0(FORM.BITS)).getIntStorage();
            final long width = (long) receiver.at0(FORM.WIDTH);
            final long height = (long) receiver.at0(FORM.HEIGHT);
            if (width != SqueakIOConstants.CURSOR_WIDTH || height != SqueakIOConstants.CURSOR_HEIGHT) {
                CompilerDirectives.transferToInterpreter();
                code.image.printToStdErr("Unexpected cursor width:", width, "or height:", height, ". Proceeding with cropped cursor...");
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

        @Specialization(guards = {"code.image.hasDisplay()", "receiver.size() >= 4"})
        protected final boolean doDisplay(final PointersObject receiver) {
            code.image.specialObjectsArray.atput0Object(SPECIAL_OBJECT.THE_DISPLAY, receiver);
            code.image.getDisplay().open(receiver);
            return code.image.sqTrue;
        }

        @Specialization(guards = {"!code.image.hasDisplay()"})
        protected final boolean doDisplayHeadless(@SuppressWarnings("unused") final PointersObject receiver) {
            return code.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 103)
    protected abstract static class PrimScanCharactersNode extends AbstractPrimitiveNode implements SeptenaryPrimitive {
        @Child protected SqueakObjectSizeNode sizeNode = SqueakObjectSizeNode.create();
        @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();

        protected PrimScanCharactersNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"startIndex > 0", "stopIndex > 0", "sourceString.isByteType()", "receiver.size() >= 4", "sizeNode.execute(stops) >= 258"})
        protected final Object doScan(final PointersObject receiver, final long startIndex, final long stopIndex, final NativeObject sourceString, final long rightX,
                        final ArrayObject stops, final long kernData,
                        @Cached("createScanCharactersHelperNode()") final ScanCharactersHelperNode scanNode) {
            final Object scanDestX = at0Node.execute(receiver, CHARACTER_SCANNER.DEST_X);
            final Object scanXTable = at0Node.execute(receiver, CHARACTER_SCANNER.XTABLE);
            final Object scanMap = at0Node.execute(receiver, CHARACTER_SCANNER.MAP);
            return scanNode.executeScan(receiver, startIndex, stopIndex, sourceString.getByteStorage(), rightX, stops, kernData, scanDestX, scanXTable, scanMap);
        }

        protected final ScanCharactersHelperNode createScanCharactersHelperNode() {
            return ScanCharactersHelperNode.create(code.image);
        }

        protected abstract static class ScanCharactersHelperNode extends AbstractNodeWithImage {
            private static final long END_OF_RUN = 257 - 1;
            private static final long CROSSED_X = 258 - 1;

            @Child protected ArrayObjectSizeNode sizeNode = ArrayObjectSizeNode.create();
            @Child private SqueakObjectAtPut0Node atPut0Node = SqueakObjectAtPut0Node.create();
            @Child private ReadArrayObjectNode readNode = ReadArrayObjectNode.create();

            protected ScanCharactersHelperNode(final SqueakImageContext image) {
                super(image);
            }

            protected static ScanCharactersHelperNode create(final SqueakImageContext image) {
                return ScanCharactersHelperNodeGen.create(image);
            }

            protected abstract Object executeScan(PointersObject receiver, long startIndex, long stopIndex, byte[] sourceBytes, long rightX, ArrayObject stops, long kernData,
                            Object scanDestX, Object scanXTable, Object scanMap);

            @Specialization(guards = {"sizeNode.execute(scanMap) == 256", "stopIndex <= sourceBytes.length"})
            protected final Object doScan(final PointersObject receiver, final long startIndex, final long stopIndex, final byte[] sourceBytes, final long rightX,
                            final ArrayObject stops,
                            final long kernData, final long startScanDestX, final ArrayObject scanXTable, final ArrayObject scanMap) {
                final int maxGlyph = sizeNode.execute(scanXTable) - 2;
                long scanDestX = startScanDestX;
                long scanLastIndex = startIndex;
                while (scanLastIndex <= stopIndex) {
                    final long ascii = (sourceBytes[(int) (scanLastIndex - 1)] & 0xFF);
                    final Object stopReason = readNode.execute(stops, ascii);
                    if (stopReason != image.nil) {
                        storeStateInReceiver(receiver, scanDestX, scanLastIndex);
                        return stopReason;
                    }
                    if (ascii < 0 || sizeNode.execute(scanMap) <= ascii) {
                        throw new PrimitiveFailed();
                    }
                    final long glyphIndex = (long) readNode.execute(scanMap, ascii);
                    if (glyphIndex < 0 || glyphIndex > maxGlyph) {
                        throw new PrimitiveFailed();
                    }
                    final long sourceX1;
                    final long sourceX2;
                    try {
                        sourceX1 = (long) readNode.execute(scanXTable, glyphIndex);
                        sourceX2 = (long) readNode.execute(scanXTable, glyphIndex + 1);
                    } catch (ClassCastException e) {
                        throw new PrimitiveFailed();
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

            @Fallback
            @SuppressWarnings("unused")
            protected static final Object doFail(final PointersObject receiver, final long startIndex, final long stopIndex, final byte[] sourceBytes, final long rightX, final ArrayObject stops,
                            final long kernData, final Object scanDestX, final Object scanXTable, final Object scanMap) {
                throw new PrimitiveFailed();
            }

            private void storeStateInReceiver(final PointersObject receiver, final long scanDestX, final long scanLastIndex) {
                atPut0Node.execute(receiver, 0, scanDestX);
                atPut0Node.execute(receiver, 1, scanLastIndex);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 105)
    protected abstract static class PrimStringReplaceNode extends AbstractPrimitiveNode implements QuinaryPrimitive {
        @Child private SqueakObjectInstSizeNode instSizeNode = SqueakObjectInstSizeNode.create();
        @Child private SqueakObjectSizeNode sizeNode = SqueakObjectSizeNode.create();
        @Child private ReadArrayObjectNode readArrayObjectNode;
        @Child private NativeObjectReadNode readNativeObjectNode;
        @Child private NativeObjectWriteNode writeNativeObjectNode;
        @Child private GetObjectArrayNode getObjectArrayNode;

        protected PrimStringReplaceNode(final CompiledMethodObject method) {
            super(method);
        }

        public abstract Object executeReplace(VirtualFrame frame);

        @Specialization(guards = "!isSmallInteger(repl)")
        protected final Object replace(final LargeIntegerObject rcvr, final long start, final long stop, final long repl, final long replStart) {
            return doLargeInteger(rcvr, start, stop, asLargeInteger(repl), replStart);
        }

        @Specialization(guards = "inBounds(rcvr, start, stop, repl, replStart)")
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

        @Specialization(guards = "inBounds(rcvr, start, stop, repl, replStart)")
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

        @Specialization(guards = {"inBounds(rcvr, start, stop, repl, replStart)", "repl.isByteType()"})
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

        @Specialization(guards = {"inBounds(rcvr, start, stop, repl, replStart)", "rcvr.haveSameStorageType(repl)"})
        protected final Object doNative(final NativeObject rcvr, final long start, final long stop, final NativeObject repl, final long replStart) {
            final int repOff = (int) (replStart - start);
            for (int i = (int) (start - 1); i < stop; i++) {
                getWriteNativeObjectNode().execute(rcvr, i, getReadNativeObjectNode().execute(repl, repOff + i));
            }
            return rcvr;
        }

        @Specialization(guards = "!isSmallInteger(repl)")
        protected final Object doNativeLargeInteger(final NativeObject rcvr, final long start, final long stop, final long repl, final long replStart) {
            return doNativeLargeInteger(rcvr, start, stop, asLargeInteger(repl), replStart);
        }

        @Specialization(guards = "inBounds(rcvr, start, stop, repl, replStart)")
        protected final Object doNativeLargeInteger(final NativeObject rcvr, final long start, final long stop, final LargeIntegerObject repl, final long replStart) {
            final int repOff = (int) (replStart - start);
            for (int i = (int) (start - 1); i < stop; i++) {
                getWriteNativeObjectNode().execute(rcvr, i, repl.getNativeAt0(repOff + i));
            }
            return rcvr;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "inBounds(rcvr, start, stop, repl, replStart)")
        protected static final Object doNativeFloat(final NativeObject rcvr, final long start, final long stop, final FloatObject repl, final long replStart) {
            throw new SqueakException("Not supported"); // TODO: check this is needed
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"inBounds(rcvr, start, stop, repl, replStart)", "rcvr.isEmptyType()", "repl.isEmptyType()"})
        protected static final Object doEmptyArrays(final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart) {
            return rcvr; // Nothing to do.
        }

        @Specialization(guards = {"inBounds(rcvr, start, stop, repl, replStart)", "rcvr.isEmptyType()", "repl.isAbstractSqueakObjectType()"})
        protected static final Object doEmptyArrayToSqueakObjects(final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart) {
            rcvr.transitionFromEmptyToAbstractSqueakObjects();
            return doArraysOfSqueakObjects(rcvr, start, stop, repl, replStart);
        }

        @Specialization(guards = {"inBounds(rcvr, start, stop, repl, replStart)", "rcvr.isEmptyType()", "repl.isBooleanType()"})
        protected static final Object doEmptyArrayToBooleans(final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart) {
            rcvr.transitionFromEmptyToBooleans();
            return doArraysOfBooleans(rcvr, start, stop, repl, replStart);
        }

        @Specialization(guards = {"inBounds(rcvr, start, stop, repl, replStart)", "rcvr.isEmptyType()", "repl.isCharType()"})
        protected static final Object doEmptyArrayToChars(final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart) {
            rcvr.transitionFromEmptyToChars();
            return doArraysOfChars(rcvr, start, stop, repl, replStart);
        }

        @Specialization(guards = {"inBounds(rcvr, start, stop, repl, replStart)", "rcvr.isEmptyType()", "repl.isLongType()"})
        protected static final Object doEmptyArrayToLongs(final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart) {
            rcvr.transitionFromEmptyToLongs();
            return doArraysOfLongs(rcvr, start, stop, repl, replStart);
        }

        @Specialization(guards = {"inBounds(rcvr, start, stop, repl, replStart)", "rcvr.isEmptyType()", "repl.isDoubleType()"})
        protected static final Object doEmptyArrayToDoubles(final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart) {
            rcvr.transitionFromEmptyToDoubles();
            return doArraysOfDoubles(rcvr, start, stop, repl, replStart);
        }

        @Specialization(guards = {"inBounds(rcvr, start, stop, repl, replStart)", "rcvr.isEmptyType()", "repl.isObjectType()"})
        protected static final Object doEmptyArrayToObjects(final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart) {
            rcvr.transitionFromEmptyToObjects();
            return doArraysOfObjects(rcvr, start, stop, repl, replStart);
        }

        @Specialization(guards = {"inBounds(rcvr, start, stop, repl, replStart)", "rcvr.isAbstractSqueakObjectType()", "repl.isAbstractSqueakObjectType()"})
        protected static final Object doArraysOfSqueakObjects(final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart) {
            final int repOff = (int) (replStart - start);
            final AbstractSqueakObject[] dstArray = rcvr.getAbstractSqueakObjectStorage();
            final AbstractSqueakObject[] srcArray = repl.getAbstractSqueakObjectStorage();
            for (int i = (int) (start - 1); i < stop; i++) {
                dstArray[i] = srcArray[repOff + i];
            }
            return rcvr;
        }

        @Specialization(guards = {"inBounds(rcvr, start, stop, repl, replStart)", "rcvr.isAbstractSqueakObjectType()", "!repl.isAbstractSqueakObjectType()"})
        protected final Object doArraysOfSqueakObjectsTransition(final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart) {
            rcvr.transitionFromAbstractSqueakObjectsToObjects();
            replaceGeneric(rcvr.getObjectStorage(), start, stop, getGetObjectArrayNode().execute(repl), replStart);
            return rcvr;
        }

        @Specialization(guards = {"inBounds(rcvr, start, stop, repl, replStart)", "rcvr.isBooleanType()", "repl.isBooleanType()"})
        protected static final Object doArraysOfBooleans(final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart) {
            final int repOff = (int) (replStart - start);
            final byte[] dstLongs = rcvr.getBooleanStorage();
            final byte[] srcLongs = repl.getBooleanStorage();
            for (int i = (int) (start - 1); i < stop; i++) {
                dstLongs[i] = srcLongs[repOff + i];
            }
            return rcvr;
        }

        @Specialization(guards = {"inBounds(rcvr, start, stop, repl, replStart)", "rcvr.isCharType()", "repl.isCharType()"})
        protected static final Object doArraysOfChars(final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart) {
            final int repOff = (int) (replStart - start);
            final char[] dstLongs = rcvr.getCharStorage();
            final char[] srcLongs = repl.getCharStorage();
            for (int i = (int) (start - 1); i < stop; i++) {
                dstLongs[i] = srcLongs[repOff + i];
            }
            return rcvr;
        }

        @Specialization(guards = {"inBounds(rcvr, start, stop, repl, replStart)", "rcvr.isLongType()", "repl.isLongType()"})
        protected static final Object doArraysOfLongs(final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart) {
            final int repOff = (int) (replStart - start);
            final long[] dstLongs = rcvr.getLongStorage();
            final long[] srcLongs = repl.getLongStorage();
            for (int i = (int) (start - 1); i < stop; i++) {
                dstLongs[i] = srcLongs[repOff + i];
            }
            return rcvr;
        }

        @Specialization(guards = {"inBounds(rcvr, start, stop, repl, replStart)", "rcvr.isLongType()", "!repl.isLongType()"})
        protected final Object doArraysOfLongsTransition(final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart) {
            rcvr.transitionFromLongsToObjects();
            replaceGeneric(rcvr.getObjectStorage(), start, stop, getGetObjectArrayNode().execute(repl), replStart);
            return rcvr;
        }

        @Specialization(guards = {"inBounds(rcvr, start, stop, repl, replStart)", "rcvr.isDoubleType()", "repl.isDoubleType()"})
        protected static final Object doArraysOfDoubles(final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart) {
            final int repOff = (int) (replStart - start);
            final double[] dstDoubles = rcvr.getDoubleStorage();
            final double[] srcDoubles = repl.getDoubleStorage();
            for (int i = (int) (start - 1); i < stop; i++) {
                dstDoubles[i] = srcDoubles[repOff + i];
            }
            return rcvr;
        }

        @Specialization(guards = {"inBounds(rcvr, start, stop, repl, replStart)", "rcvr.isDoubleType()", "!repl.isDoubleType()"})
        protected final Object doArraysOfDoublesTransition(final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart) {
            rcvr.transitionFromDoublesToObjects();
            replaceGeneric(rcvr.getObjectStorage(), start, stop, getGetObjectArrayNode().execute(repl), replStart);
            return rcvr;
        }

        @Specialization(guards = {"inBounds(rcvr, start, stop, repl, replStart)", "rcvr.isObjectType()", "repl.isObjectType()"})
        protected static final Object doArraysOfObjects(final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart) {
            final int repOff = (int) (replStart - start);
            final Object[] dstArray = rcvr.getObjectStorage();
            final Object[] srcArray = repl.getObjectStorage();
            for (int i = (int) (start - 1); i < stop; i++) {
                dstArray[i] = srcArray[repOff + i];
            }
            return rcvr;
        }

        @Specialization(guards = {"inBounds(rcvr, start, stop, repl, replStart)", "rcvr.isObjectType()", "!repl.isObjectType()"})
        protected final Object doArraysOfObjectsNonObject(final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart) {
            final int repOff = (int) (replStart - start);
            final Object[] dstArray = rcvr.getObjectStorage();
            final Object[] srcArray = getGetObjectArrayNode().execute(repl);
            for (int i = (int) (start - 1); i < stop; i++) {
                dstArray[i] = srcArray[repOff + i];
            }
            return rcvr;
        }

        @Specialization(guards = {"inBounds(rcvr, start, stop, repl, replStart)", "rcvr.isEmptyType()"})
        protected static final Object doEmptyArrayPointers(final ArrayObject rcvr, final long start, final long stop, final PointersObject repl, final long replStart) {
            rcvr.transitionFromEmptyToObjects(); // TODO: could be more efficient?
            return doArrayOfObjectsPointers(rcvr, start, stop, repl, replStart);
        }

        @Specialization(guards = {"inBounds(rcvr, start, stop, repl, replStart)", "rcvr.isAbstractSqueakObjectType()"})
        protected static final Object doArrayOfSqueakObjectPointers(final ArrayObject rcvr, final long start, final long stop, final PointersObject repl, final long replStart) {
            rcvr.transitionFromAbstractSqueakObjectsToObjects();
            return doArrayOfObjectsPointers(rcvr, start, stop, repl, replStart);
        }

        @Specialization(guards = {"inBounds(rcvr, start, stop, repl, replStart)", "rcvr.isBooleanType()"})
        protected static final Object doArrayOfBooleansPointers(final ArrayObject rcvr, final long start, final long stop, final PointersObject repl, final long replStart) {
            rcvr.transitionFromBooleansToObjects();
            return doArrayOfObjectsPointers(rcvr, start, stop, repl, replStart);
        }

        @Specialization(guards = {"inBounds(rcvr, start, stop, repl, replStart)", "rcvr.isCharType()"})
        protected static final Object doArrayOfCharsPointers(final ArrayObject rcvr, final long start, final long stop, final PointersObject repl, final long replStart) {
            rcvr.transitionFromCharsToObjects();
            return doArrayOfObjectsPointers(rcvr, start, stop, repl, replStart);
        }

        @Specialization(guards = {"inBounds(rcvr, start, stop, repl, replStart)", "rcvr.isLongType()"})
        protected static final Object doArrayOfLongsPointers(final ArrayObject rcvr, final long start, final long stop, final PointersObject repl, final long replStart) {
            rcvr.transitionFromLongsToObjects();
            return doArrayOfObjectsPointers(rcvr, start, stop, repl, replStart);
        }

        @Specialization(guards = {"inBounds(rcvr, start, stop, repl, replStart)", "rcvr.isDoubleType()"})
        protected static final Object doArrayOfDoublesPointers(final ArrayObject rcvr, final long start, final long stop, final PointersObject repl, final long replStart) {
            rcvr.transitionFromDoublesToObjects();
            return doArrayOfObjectsPointers(rcvr, start, stop, repl, replStart);
        }

        @Specialization(guards = {"inBounds(rcvr, start, stop, repl, replStart)", "rcvr.isObjectType()"})
        protected static final Object doArrayOfObjectsPointers(final ArrayObject rcvr, final long start, final long stop, final PointersObject repl, final long replStart) {
            final int repOff = (int) (replStart - start);
            final Object[] dstArray = rcvr.getObjectStorage();
            for (int i = (int) (start - 1); i < stop; i++) {
                dstArray[i] = repl.at0(repOff + i);
            }
            return rcvr;
        }

        @Specialization(guards = {"inBounds(rcvr, start, stop, repl, replStart)", "rcvr.isEmptyType()"})
        protected static final Object doEmptyArrayWeakPointers(final ArrayObject rcvr, final long start, final long stop, final WeakPointersObject repl, final long replStart) {
            rcvr.transitionFromEmptyToObjects(); // TODO: could be more efficient?
            return doArrayOfObjectsWeakPointers(rcvr, start, stop, repl, replStart);
        }

        @Specialization(guards = {"inBounds(rcvr, start, stop, repl, replStart)", "rcvr.isAbstractSqueakObjectType()"})
        protected static final Object doArrayOfSqueakObjectWeakPointers(final ArrayObject rcvr, final long start, final long stop, final WeakPointersObject repl, final long replStart) {
            rcvr.transitionFromAbstractSqueakObjectsToObjects();
            return doArrayOfObjectsWeakPointers(rcvr, start, stop, repl, replStart);
        }

        @Specialization(guards = {"inBounds(rcvr, start, stop, repl, replStart)", "rcvr.isBooleanType()"})
        protected static final Object doArrayOfBooleansWeakPointers(final ArrayObject rcvr, final long start, final long stop, final WeakPointersObject repl, final long replStart) {
            rcvr.transitionFromBooleansToObjects();
            return doArrayOfObjectsWeakPointers(rcvr, start, stop, repl, replStart);
        }

        @Specialization(guards = {"inBounds(rcvr, start, stop, repl, replStart)", "rcvr.isCharType()"})
        protected static final Object doArrayOfCharsWeakPointers(final ArrayObject rcvr, final long start, final long stop, final WeakPointersObject repl, final long replStart) {
            rcvr.transitionFromCharsToObjects();
            return doArrayOfObjectsWeakPointers(rcvr, start, stop, repl, replStart);
        }

        @Specialization(guards = {"inBounds(rcvr, start, stop, repl, replStart)", "rcvr.isLongType()"})
        protected static final Object doArrayOfLongsWeakPointers(final ArrayObject rcvr, final long start, final long stop, final WeakPointersObject repl, final long replStart) {
            rcvr.transitionFromLongsToObjects();
            return doArrayOfObjectsWeakPointers(rcvr, start, stop, repl, replStart);
        }

        @Specialization(guards = {"inBounds(rcvr, start, stop, repl, replStart)", "rcvr.isDoubleType()"})
        protected static final Object doArrayOfDoublesWeakPointers(final ArrayObject rcvr, final long start, final long stop, final WeakPointersObject repl, final long replStart) {
            rcvr.transitionFromDoublesToObjects();
            return doArrayOfObjectsWeakPointers(rcvr, start, stop, repl, replStart);
        }

        @Specialization(guards = {"inBounds(rcvr, start, stop, repl, replStart)", "rcvr.isObjectType()"})
        protected static final Object doArrayOfObjectsWeakPointers(final ArrayObject rcvr, final long start, final long stop, final WeakPointersObject repl, final long replStart) {
            final int repOff = (int) (replStart - start);
            final Object[] dstArray = rcvr.getObjectStorage();
            for (int i = (int) (start - 1); i < stop; i++) {
                dstArray[i] = repl.at0(repOff + i);
            }
            return rcvr;
        }

        @Specialization(guards = "inBounds(rcvr, start, stop, repl, replStart)")
        protected static final Object doPointers(final PointersObject rcvr, final long start, final long stop, final PointersObject repl, final long replStart) {
            final long repOff = replStart - start;
            for (int i = (int) (start - 1); i < stop; i++) {
                rcvr.atput0(i, repl.at0(repOff + i));
            }
            return rcvr;
        }

        @Specialization(guards = "inBounds(rcvr, start, stop, repl, replStart)")
        protected static final Object doPointersWeakPointers(final PointersObject rcvr, final long start, final long stop, final WeakPointersObject repl, final long replStart) {
            final long repOff = replStart - start;
            for (int i = (int) (start - 1); i < stop; i++) {
                rcvr.atput0(i, repl.at0(repOff + i));
            }
            return rcvr;
        }

        @Specialization(guards = "inBounds(rcvr, start, stop, repl, replStart)")
        protected static final Object doWeakPointers(final WeakPointersObject rcvr, final long start, final long stop, final WeakPointersObject repl, final long replStart) {
            final long repOff = replStart - start;
            for (int i = (int) (start - 1); i < stop; i++) {
                rcvr.atput0(i, repl.at0(repOff + i));
            }
            return rcvr;
        }

        @Specialization(guards = "inBounds(rcvr, start, stop, repl, replStart)")
        protected final Object doWeakPointersArray(final WeakPointersObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart) {
            final long repOff = replStart - start;
            for (int i = (int) (start - 1); i < stop; i++) {
                rcvr.atput0(i, getReadArrayObjectNode().execute(repl, repOff + i));
            }
            return rcvr;
        }

        @Specialization(guards = "inBounds(rcvr, start, stop, repl, replStart)")
        protected static final Object doWeakPointersPointers(final WeakPointersObject rcvr, final long start, final long stop, final PointersObject repl, final long replStart) {
            final long repOff = replStart - start;
            for (int i = (int) (start - 1); i < stop; i++) {
                rcvr.atput0(i, repl.at0(repOff + i));
            }
            return rcvr;
        }

        @Specialization(guards = "inBounds(rcvr, start, stop, repl, replStart)")
        protected static final Object doBlock(final CompiledBlockObject rcvr, final long start, final long stop, final CompiledBlockObject repl, final long replStart) {
            final long repOff = replStart - start;
            for (int i = (int) (start - 1); i < stop; i++) {
                rcvr.atput0(i, repl.at0(repOff + i));
            }
            return rcvr;
        }

        @Specialization(guards = "inBounds(rcvr, start, stop, repl, replStart)")
        protected static final Object doMethod(final CompiledMethodObject rcvr, final long start, final long stop, final CompiledMethodObject repl, final long replStart) {
            final long repOff = replStart - start;
            for (int i = (int) (start - 1); i < stop; i++) {
                rcvr.atput0(i, repl.at0(repOff + i));
            }
            return rcvr;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!inBounds(rcvr, start, stop, repl, replStart)")
        protected static final Object doBadIndex(final AbstractSqueakObject rcvr, final long start, final long stop, final AbstractSqueakObject repl, final long replStart) {
            throw new PrimitiveFailed(ERROR_TABLE.BAD_INDEX);
        }

        protected final boolean inBounds(final AbstractSqueakObject array, final long start, final long stop, final AbstractSqueakObject repl, final long replStart) {
            return (start >= 1 && (start - 1) <= stop && (stop + instSizeNode.execute(array)) <= sizeNode.execute(array)) &&
                            (replStart >= 1 && (stop - start + replStart + instSizeNode.execute(repl) <= sizeNode.execute(repl)));
        }

        private static void replaceGeneric(final Object[] dstArray, final long start, final long stop, final Object[] srcArray, final long replStart) {
            final int repOff = (int) (replStart - start);
            for (int i = (int) (start - 1); i < stop; i++) {
                dstArray[i] = srcArray[repOff + i];
            }
        }

        private ReadArrayObjectNode getReadArrayObjectNode() {
            if (readArrayObjectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readArrayObjectNode = insert(ReadArrayObjectNode.create());
            }
            return readArrayObjectNode;
        }

        private NativeObjectReadNode getReadNativeObjectNode() {
            if (readNativeObjectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readNativeObjectNode = insert(NativeObjectReadNode.create());
            }
            return readNativeObjectNode;
        }

        private NativeObjectWriteNode getWriteNativeObjectNode() {
            if (writeNativeObjectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                writeNativeObjectNode = insert(NativeObjectWriteNode.create());
            }
            return writeNativeObjectNode;
        }

        private GetObjectArrayNode getGetObjectArrayNode() {
            if (getObjectArrayNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getObjectArrayNode = insert(GetObjectArrayNode.create());
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

        @Specialization(guards = "code.image.hasDisplay()")
        protected final AbstractSqueakObject doSize(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            return code.image.wrap(code.image.getDisplay().getWindowSize());
        }

        @Specialization(guards = "!code.image.hasDisplay()")
        protected final AbstractSqueakObject doSizeHeadless(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            return code.image.wrap(code.image.flags.getLastWindowSize());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 107)
    protected abstract static class PrimMouseButtonsNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimMouseButtonsNode(final CompiledMethodObject method) {
            super(method);
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
    @SqueakPrimitive(indices = 108)
    protected abstract static class PrimKeyboardNextNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimKeyboardNextNode(final CompiledMethodObject method) {
            super(method);
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
    @SqueakPrimitive(indices = 109)
    protected abstract static class PrimKeyboardPeekNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimKeyboardPeekNode(final CompiledMethodObject method) {
            super(method);
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
    @SqueakPrimitive(indices = 126)
    protected abstract static class PrimDeferDisplayUpdatesNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        public PrimDeferDisplayUpdatesNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "code.image.hasDisplay()")
        protected final AbstractSqueakObject doDefer(final AbstractSqueakObject receiver, final boolean flag) {
            code.image.getDisplay().setDeferUpdates(flag);
            return receiver;
        }

        @Specialization(guards = "!code.image.hasDisplay()")
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

        @Specialization(guards = {"code.image.hasDisplay()"})
        protected final AbstractSqueakObject doShow(final PointersObject receiver, final long left, final long right, final long top, final long bottom) {
            code.image.getDisplay().showDisplayRect((int) left, (int) right, (int) top, (int) bottom);
            return receiver;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!code.image.hasDisplay()"})
        protected static final AbstractSqueakObject doDrawHeadless(final PointersObject receiver, final long left, final long right, final long top, final long bottom) {
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
