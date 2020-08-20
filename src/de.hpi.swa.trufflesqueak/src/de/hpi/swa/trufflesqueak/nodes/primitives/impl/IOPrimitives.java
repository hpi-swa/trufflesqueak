/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleOptions;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.image.SqueakImageWriter;
import de.hpi.swa.trufflesqueak.io.SqueakDisplayInterface;
import de.hpi.swa.trufflesqueak.io.SqueakIOConstants;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.FloatObject;
import de.hpi.swa.trufflesqueak.model.LargeIntegerObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.VariablePointersObject;
import de.hpi.swa.trufflesqueak.model.WeakVariablePointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.CHARACTER_SCANNER;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.FORM;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.POINT;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.SPECIAL_OBJECT;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectInstSizeNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.VariablePointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.VariablePointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.WeakVariablePointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.WeakVariablePointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodes.ArrayObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodes.ArrayObjectSizeNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodes.ArrayObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetOrCreateContextNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveInterfaces.QuaternaryPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveInterfaces.QuinaryPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveInterfaces.SeptenaryPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitiveWithoutFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.util.NotProvided;

public final class IOPrimitives extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return IOPrimitivesFactory.getFactories();
    }

    /* primitiveMousePoint (#90) no longer in use, support dropped in TruffleSqueak. */

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 91)
    protected abstract static class PrimTestDisplayDepthNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        @Specialization
        protected static final boolean doTest(@SuppressWarnings("unused") final Object receiver, final long depth,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            if (image.hasDisplay()) {
                // TODO: support all depths ({1, 2, 4, 8, 16, 32} and negative values)?
                return BooleanObject.wrap(depth == 32);
            } else {
                return BooleanObject.wrap(depth % 2 == 0);
            }
        }
    }

    /* primitiveSetDisplayMode (#92) no longer in use, support dropped in TruffleSqueak. */

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 93)
    protected abstract static class PrimInputSemaphoreNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        @Specialization
        protected static final Object doSet(final Object receiver, final long semaIndex,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            if (image.hasDisplay()) {
                image.getDisplay().setInputSemaphoreIndex((int) semaIndex);
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 94)
    protected abstract static class PrimGetNextEventNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        @Specialization
        protected static final PointersObject doGetNext(final PointersObject eventSensor, final ArrayObject targetArray,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image,
                        @Cached("createIdentityProfile()") final ValueProfile displayProfile) {
            if (image.hasDisplay()) {
                final SqueakDisplayInterface display = displayProfile.profile(image.getDisplay());
                if (TruffleOptions.AOT) {
                    display.pollEvents();
                }
                final long[] event = display.getNextEvent();
                targetArray.setStorage(event != null ? event : SqueakIOConstants.NONE_EVENT);
            } else {
                targetArray.setStorage(SqueakIOConstants.NONE_EVENT);
            }
            return eventSensor;
        }
    }

    /** Primitive 96 (primitiveCopyBits) not in use anymore. */

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 97)
    protected abstract static class PrimSnapshotNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        @Specialization
        public static final boolean doSnapshot(final VirtualFrame frame, @SuppressWarnings("unused") final PointersObject receiver,
                        @Cached("create(true)") final GetOrCreateContextNode getOrCreateContextNode,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            writeImage(getOrCreateContextNode.executeGet(frame), image);
            /* Return false to signal that the image is not resuming. */
            return BooleanObject.FALSE;
        }

        @TruffleBoundary
        private static void writeImage(final ContextObject thisContext, final SqueakImageContext image) {
            /* Push true on stack for saved snapshot. */
            thisContext.push(BooleanObject.TRUE);
            SqueakImageWriter.write(image, thisContext);
            /* Pop true again. */
            thisContext.pop();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 98)
    protected abstract static class PrimStoreImageSegmentNode extends AbstractPrimitiveNode implements QuaternaryPrimitive {

        @SuppressWarnings("unused")
        @Specialization(guards = "segmentWordArray.isIntType()")
        protected static final Object doStore(final Object receiver, final ArrayObject rootsArray, final NativeObject segmentWordArray, final ArrayObject outPointerArray) {
            /**
             * TODO: implement primitive. In the meantime, pretend this primitive succeeds so that
             * some tests (e.g. BitmapStreamTests) run quickly.
             */
            throw PrimitiveFailed.GENERIC_ERROR;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 99)
    protected abstract static class PrimLoadImageSegmentNode extends AbstractPrimitiveNode implements TernaryPrimitive {

        @SuppressWarnings("unused")
        @Specialization(guards = "segmentWordArray.isIntType()")
        protected static final ArrayObject doLoad(final Object receiver, final NativeObject segmentWordArray, final ArrayObject outPointerArray) {
            /**
             * TODO: implement primitive. In the meantime, pretend this primitive succeeds so that
             * some tests (e.g. BitmapStreamTests) run quickly.
             */
            throw PrimitiveFailed.GENERIC_ERROR;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 101)
    protected abstract static class PrimBeCursorNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        @Child private AbstractPointersObjectReadNode cursorReadNode = AbstractPointersObjectReadNode.create();
        @Child private AbstractPointersObjectReadNode offsetReadNode = AbstractPointersObjectReadNode.create();

        @Specialization
        protected final PointersObject doCursor(final PointersObject receiver, @SuppressWarnings("unused") final NotProvided mask,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            if (image.hasDisplay()) {
                final PointersObject offset = receiver.getFormOffset(cursorReadNode);
                final int offsetX = Math.abs(offsetReadNode.executeInt(offset, POINT.X));
                final int offsetY = Math.abs(offsetReadNode.executeInt(offset, POINT.Y));
                image.getDisplay().setCursor(receiver.getFormBits(cursorReadNode), null, receiver.getFormWidth(cursorReadNode), receiver.getFormHeight(cursorReadNode),
                                receiver.getFormDepth(cursorReadNode), offsetX, offsetY);
            }
            return receiver;
        }

        @Specialization
        protected final PointersObject doCursor(final PointersObject receiver, final PointersObject maskObject,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image,
                        @Cached final ConditionProfile depthProfile) {
            if (image.hasDisplay()) {
                final int[] words = receiver.getFormBits(cursorReadNode);
                final int depth = receiver.getFormDepth(cursorReadNode);
                final int height = receiver.getFormHeight(cursorReadNode);
                final int width = receiver.getFormWidth(cursorReadNode);
                final PointersObject offset = receiver.getFormOffset(cursorReadNode);
                final int offsetX = Math.abs(offsetReadNode.executeInt(offset, POINT.X));
                final int offsetY = Math.abs(offsetReadNode.executeInt(offset, POINT.Y));
                if (depthProfile.profile(depth == 1)) {
                    final int[] mask = cursorReadNode.executeNative(maskObject, FORM.BITS).getIntStorage();
                    image.getDisplay().setCursor(words, mask, width, height, 2, offsetX, offsetY);
                } else {
                    image.getDisplay().setCursor(words, null, width, height, depth, offsetX, offsetY);
                }
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 102)
    protected abstract static class PrimBeDisplayNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        @Specialization(guards = {"receiver.size() >= 4"})
        protected static final boolean doDisplay(final PointersObject receiver,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            if (image.hasDisplay()) {
                image.setSpecialObject(SPECIAL_OBJECT.THE_DISPLAY, receiver);
                image.getDisplay().open(receiver);
                return BooleanObject.TRUE;
            } else {
                return BooleanObject.FALSE;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 103)
    protected abstract static class PrimScanCharactersNode extends AbstractPrimitiveNode implements SeptenaryPrimitive {
        private static final long END_OF_RUN = 257 - 1;
        private static final long CROSSED_X = 258 - 1;

        @Child private ArrayObjectReadNode arrayReadNode = ArrayObjectReadNode.create();
        @Child protected ArrayObjectSizeNode arraySizeNode = ArrayObjectSizeNode.create();
        @Child protected AbstractPointersObjectReadNode pointersReadNode = AbstractPointersObjectReadNode.create();
        @Child private AbstractPointersObjectWriteNode pointersWriteNode = AbstractPointersObjectWriteNode.create();

        @Specialization(guards = {"startIndex > 0", "stopIndex > 0", "sourceString.isByteType()", "stopIndex <= sourceString.getByteLength()", "receiver.size() >= 4",
                        "arraySizeNode.execute(stops) >= 258", "hasCorrectSlots(receiver)"})
        protected final Object doScan(final PointersObject receiver, final long startIndex, final long stopIndex, final NativeObject sourceString, final long rightX,
                        final ArrayObject stops, final long kernData) {
            final ArrayObject scanXTable = pointersReadNode.executeArray(receiver, CHARACTER_SCANNER.XTABLE);
            final ArrayObject scanMap = pointersReadNode.executeArray(receiver, CHARACTER_SCANNER.MAP);

            final int maxGlyph = arraySizeNode.execute(scanXTable) - 2;
            long scanDestX = pointersReadNode.executeLong(receiver, CHARACTER_SCANNER.DEST_X);
            long scanLastIndex = startIndex;
            while (scanLastIndex <= stopIndex) {
                final long ascii = sourceString.getByte(scanLastIndex - 1) & 0xFF;
                final Object stopReason = arrayReadNode.execute(stops, ascii);
                if (stopReason != NilObject.SINGLETON) {
                    storeStateInReceiver(receiver, scanDestX, scanLastIndex);
                    return stopReason;
                }
                if (ascii < 0 || arraySizeNode.execute(scanMap) <= ascii) {
                    throw PrimitiveFailed.andTransferToInterpreter();
                }
                final long glyphIndex = (long) arrayReadNode.execute(scanMap, ascii);
                if (glyphIndex < 0 || glyphIndex > maxGlyph) {
                    throw PrimitiveFailed.andTransferToInterpreter();
                }
                final long sourceX1;
                final long sourceX2;
                sourceX1 = (long) arrayReadNode.execute(scanXTable, glyphIndex);
                sourceX2 = (long) arrayReadNode.execute(scanXTable, glyphIndex + 1);
                final long nextDestX = scanDestX + sourceX2 - sourceX1;
                if (nextDestX > rightX) {
                    storeStateInReceiver(receiver, scanDestX, scanLastIndex);
                    return arrayReadNode.execute(stops, CROSSED_X);
                }
                scanDestX = nextDestX + kernData;
                scanLastIndex++;
            }
            storeStateInReceiver(receiver, scanDestX, stopIndex);
            return arrayReadNode.execute(stops, END_OF_RUN);
        }

        private void storeStateInReceiver(final PointersObject receiver, final long scanDestX, final long scanLastIndex) {
            pointersWriteNode.execute(receiver, CHARACTER_SCANNER.DEST_X, scanDestX);
            pointersWriteNode.execute(receiver, CHARACTER_SCANNER.LAST_INDEX, scanLastIndex);
        }

        protected final boolean hasCorrectSlots(final PointersObject receiver) {
            final Object scanMap = pointersReadNode.execute(receiver, CHARACTER_SCANNER.MAP);
            return pointersReadNode.execute(receiver, CHARACTER_SCANNER.DEST_X) instanceof Long && pointersReadNode.execute(receiver, CHARACTER_SCANNER.XTABLE) instanceof ArrayObject &&
                            scanMap instanceof ArrayObject && arraySizeNode.execute((ArrayObject) scanMap) == 256;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 105)
    protected abstract static class PrimStringReplaceNode extends AbstractPrimitiveNode implements QuinaryPrimitive {
        @Specialization
        protected static final NativeObject doNative(final NativeObject rcvr, final long start, final long stop, final Object repl, final long replStart,
                        @Cached final NativeObjectReplaceNode replaceNode) {
            replaceNode.execute(rcvr, start, stop, repl, replStart);
            return rcvr;
        }

        @Specialization
        protected static final ArrayObject doArray(final ArrayObject rcvr, final long start, final long stop, final Object repl, final long replStart,
                        @Cached final ArrayObjectReplaceNode replaceNode) {
            replaceNode.execute(rcvr, start, stop, repl, replStart);
            return rcvr;
        }

        @Specialization
        protected static final LargeIntegerObject doLarge(final LargeIntegerObject rcvr, final long start, final long stop, final Object repl, final long replStart,
                        @Cached final LargeIntegerObjectReplaceNode replaceNode) {
            replaceNode.execute(rcvr, start, stop, repl, replStart);
            return rcvr;
        }

        @Specialization
        protected static final PointersObject doPointers(final PointersObject rcvr, final long start, final long stop, final Object repl, final long replStart,
                        @Cached final PointersObjectReplaceNode replaceNode) {
            replaceNode.execute(rcvr, start, stop, repl, replStart);
            return rcvr;
        }

        @Specialization
        protected static final VariablePointersObject doPointers(final VariablePointersObject rcvr, final long start, final long stop, final Object repl, final long replStart,
                        @Cached final VariablePointersObjectReplaceNode replaceNode) {
            replaceNode.execute(rcvr, start, stop, repl, replStart);
            return rcvr;
        }

        @Specialization
        protected static final WeakVariablePointersObject doWeakPointers(final WeakVariablePointersObject rcvr, final long start, final long stop, final Object repl, final long replStart,
                        @Cached final WeakPointersObjectReplaceNode replaceNode) {
            replaceNode.execute(rcvr, start, stop, repl, replStart);
            return rcvr;
        }

        @Specialization
        protected static final CompiledCodeObject doMethod(final CompiledCodeObject rcvr, final long start, final long stop, final CompiledCodeObject repl, final long replStart,
                        @Shared("errorProfile") @Cached final BranchProfile errorProfile) {
            if (!inBounds(rcvr.instsize(), rcvr.size(), start, stop, repl.instsize(), repl.size(), replStart)) {
                errorProfile.enter();
                throw PrimitiveFailed.BAD_INDEX;
            }
            final long repOff = replStart - start;
            for (int i = (int) (start - 1); i < stop; i++) {
                rcvr.atput0(i, repl.at0(repOff + i));
            }
            return rcvr;
        }

        /* FloatObject specialization used by Cuis 5.0. */
        @SuppressWarnings("unused")
        @Specialization(guards = {"repl.isIntType()"})
        protected static final FloatObject doFloat(final FloatObject rcvr, final long start, final long stop, final NativeObject repl, final long replStart,
                        @Shared("errorProfile") @Cached final BranchProfile errorProfile) {
            if (!inBounds(rcvr.instsize(), rcvr.size(), start, stop, repl.instsize(), repl.getIntLength(), replStart)) {
                errorProfile.enter();
                throw PrimitiveFailed.GENERIC_ERROR;
            }
            rcvr.setHigh(Integer.toUnsignedLong(repl.getInt(replStart)));
            rcvr.setLow(Integer.toUnsignedLong(repl.getInt(replStart - 1)));
            return rcvr;
        }

        private static boolean inBounds(final int arrayLength, final long start, final long stop, final int replLength, final long replStart) {
            return start >= 1 && start - 1 <= stop && stop <= arrayLength && replStart >= 1 && stop - start + replStart <= replLength;
        }

        private static boolean inBounds(final int arrayInstSize, final int arrayLength, final long start, final long stop, final int replInstSize, final int replLength, final long replStart) {
            return start >= 1 && start - 1 <= stop && stop + arrayInstSize <= arrayLength && replStart >= 1 && stop - start + replStart + replInstSize <= replLength;
        }

        protected abstract static class ArrayObjectReplaceNode extends AbstractNode {
            @Child private ArrayObjectSizeNode sizeNode;

            protected abstract void execute(ArrayObject rcvr, long start, long stop, Object repl, long replStart);

            @SuppressWarnings("unused")
            @Specialization(guards = {"rcvr.isEmptyType()", "repl.isEmptyType()"})
            protected final void doEmptyArrays(final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart,
                            @Shared("errorProfile") @Cached final BranchProfile errorProfile) {
                if (!inBounds(getSizeNode().execute(rcvr), start, stop, getSizeNode().execute(repl), replStart)) {
                    errorProfile.enter();
                    throw PrimitiveFailed.BAD_INDEX;
                }
                // Nothing to do.
            }

            @Specialization(guards = {"rcvr.isBooleanType()", "repl.isBooleanType()"})
            protected static final void doArraysOfBooleans(final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart,
                            @Shared("errorProfile") @Cached final BranchProfile errorProfile) {
                try {
                    System.arraycopy(repl.getBooleanStorage(), (int) replStart - 1, rcvr.getBooleanStorage(), (int) start - 1, (int) (1 + stop - start));
                } catch (final IndexOutOfBoundsException e) {
                    errorProfile.enter();
                    throw PrimitiveFailed.BAD_INDEX;
                }
            }

            @Specialization(guards = {"rcvr.isCharType()", "repl.isCharType()"})
            protected static final void doArraysOfChars(final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart,
                            @Shared("errorProfile") @Cached final BranchProfile errorProfile) {
                try {
                    System.arraycopy(repl.getCharStorage(), (int) replStart - 1, rcvr.getCharStorage(), (int) start - 1, (int) (1 + stop - start));
                } catch (final IndexOutOfBoundsException e) {
                    errorProfile.enter();
                    throw PrimitiveFailed.BAD_INDEX;
                }
            }

            @Specialization(guards = {"rcvr.isLongType()", "repl.isLongType()"})
            protected static final void doArraysOfLongs(final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart,
                            @Shared("errorProfile") @Cached final BranchProfile errorProfile) {
                try {
                    System.arraycopy(repl.getLongStorage(), (int) replStart - 1, rcvr.getLongStorage(), (int) start - 1, (int) (1 + stop - start));
                } catch (final IndexOutOfBoundsException e) {
                    errorProfile.enter();
                    throw PrimitiveFailed.BAD_INDEX;
                }
            }

            @Specialization(guards = {"rcvr.isDoubleType()", "repl.isDoubleType()"})
            protected static final void doArraysOfDoubles(final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart,
                            @Shared("errorProfile") @Cached final BranchProfile errorProfile) {
                try {
                    System.arraycopy(repl.getDoubleStorage(), (int) replStart - 1, rcvr.getDoubleStorage(), (int) start - 1, (int) (1 + stop - start));
                } catch (final IndexOutOfBoundsException e) {
                    errorProfile.enter();
                    throw PrimitiveFailed.BAD_INDEX;
                }
            }

            @Specialization(guards = {"rcvr.isObjectType()", "repl.isObjectType()"})
            protected static final void doArraysOfObjects(final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart,
                            @Shared("errorProfile") @Cached final BranchProfile errorProfile) {
                try {
                    System.arraycopy(repl.getObjectStorage(), (int) replStart - 1, rcvr.getObjectStorage(), (int) start - 1, (int) (1 + stop - start));
                } catch (final IndexOutOfBoundsException e) {
                    errorProfile.enter();
                    throw PrimitiveFailed.BAD_INDEX;
                }
            }

            @Specialization(guards = {"!rcvr.hasSameStorageType(repl)"})
            protected final void doArraysWithDifferenStorageTypes(final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart,
                            @Cached final ArrayObjectReadNode readNode,
                            @Shared("arrayWriteNode") @Cached final ArrayObjectWriteNode writeNode,
                            @Shared("errorProfile") @Cached final BranchProfile errorProfile) {
                if (!inBounds(getSizeNode().execute(rcvr), start, stop, getSizeNode().execute(repl), replStart)) {
                    errorProfile.enter();
                    throw PrimitiveFailed.BAD_INDEX;
                }
                final long repOff = replStart - start;
                for (int i = (int) (start - 1); i < stop; i++) {
                    writeNode.execute(rcvr, i, readNode.execute(repl, repOff + i));
                }
            }

            @Specialization
            protected final void doArrayObjectPointers(final ArrayObject rcvr, final long start, final long stop, final VariablePointersObject repl, final long replStart,
                            @Cached final VariablePointersObjectReadNode readNode,
                            @Shared("arrayWriteNode") @Cached final ArrayObjectWriteNode writeNode,
                            @Shared("errorProfile") @Cached final BranchProfile errorProfile) {
                if (!inBounds(rcvr.instsize(), getSizeNode().execute(rcvr), start, stop, repl.instsize(), repl.size(), replStart)) {
                    errorProfile.enter();
                    throw PrimitiveFailed.BAD_INDEX;
                }
                final int repOff = (int) (replStart - start);
                for (int i = (int) (start - 1); i < stop; i++) {
                    writeNode.execute(rcvr, i, readNode.execute(repl, repOff + i));
                }
            }

            @Specialization
            protected final void doArrayObjectWeakPointers(final ArrayObject rcvr, final long start, final long stop, final WeakVariablePointersObject repl, final long replStart,
                            @Cached final WeakVariablePointersObjectReadNode readNode,
                            @Shared("arrayWriteNode") @Cached final ArrayObjectWriteNode writeNode,
                            @Shared("errorProfile") @Cached final BranchProfile errorProfile) {
                if (!inBounds(rcvr.instsize(), getSizeNode().execute(rcvr), start, stop, repl.instsize(), repl.size(), replStart)) {
                    errorProfile.enter();
                    throw PrimitiveFailed.BAD_INDEX;
                }
                final int repOff = (int) (replStart - start);
                for (int i = (int) (start - 1); i < stop; i++) {
                    writeNode.execute(rcvr, i, readNode.execute(repl, repOff + i));
                }
            }

            @SuppressWarnings("unused")
            @Fallback
            protected static final void doFail(final ArrayObject rcvr, final long start, final long stop, final Object repl, final long replStart) {
                throw PrimitiveFailed.GENERIC_ERROR;
            }

            protected final ArrayObjectSizeNode getSizeNode() {
                if (sizeNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    sizeNode = insert(ArrayObjectSizeNode.create());
                }
                return sizeNode;
            }
        }

        protected abstract static class LargeIntegerObjectReplaceNode extends AbstractNode {
            protected abstract void execute(LargeIntegerObject rcvr, long start, long stop, Object repl, long replStart);

            @Specialization
            protected static final void doLargeInteger(final LargeIntegerObject rcvr, final long start, final long stop, final LargeIntegerObject repl, final long replStart,
                            @Shared("errorProfile") @Cached final BranchProfile errorProfile,
                            @Cached final ConditionProfile fitsEntirelyProfile) {
                if (fitsEntirelyProfile.profile(inBoundsEntirely(rcvr.instsize(), rcvr.size(), start, stop, repl.instsize(), repl.size(), replStart))) {
                    rcvr.replaceInternalValue(repl);
                } else {
                    if (inBounds(rcvr.size(), start, stop, repl.size(), replStart)) {
                        rcvr.setBytes(repl, (int) replStart - 1, (int) start - 1, (int) (1 + stop - start));
                    } else {
                        errorProfile.enter();
                        throw PrimitiveFailed.BAD_INDEX;
                    }
                }
            }

            @Specialization
            protected static final void doLargeIntegerFloat(final LargeIntegerObject rcvr, final long start, final long stop, final FloatObject repl, final long replStart,
                            @Shared("errorProfile") @Cached final BranchProfile errorProfile,
                            @Cached final ConditionProfile fitsEntirelyProfile) {
                if (fitsEntirelyProfile.profile(inBoundsEntirely(rcvr.instsize(), rcvr.size(), start, stop, repl.instsize(), repl.size(), replStart))) {
                    rcvr.setBytes(repl.getBytes());
                } else {
                    if (inBounds(rcvr.size(), start, stop, repl.size(), replStart)) {
                        rcvr.setBytes(repl.getBytes(), (int) replStart - 1, (int) start - 1, (int) (1 + stop - start));
                    } else {
                        errorProfile.enter();
                        throw PrimitiveFailed.BAD_INDEX;
                    }
                }
            }

            @Specialization(guards = {"repl.isByteType()"})
            protected static final void doLargeIntegerNative(final LargeIntegerObject rcvr, final long start, final long stop, final NativeObject repl, final long replStart,
                            @Shared("errorProfile") @Cached final BranchProfile errorProfile,
                            @Cached final ConditionProfile fitsEntirelyProfile) {
                if (fitsEntirelyProfile.profile(inBoundsEntirely(rcvr.instsize(), rcvr.size(), start, stop, repl.instsize(), repl.getByteLength(), replStart))) {
                    rcvr.setBytes(repl.getByteStorage());
                } else {
                    if (inBounds(rcvr.size(), start, stop, repl.getByteLength(), replStart)) {
                        rcvr.setBytes(repl.getByteStorage(), (int) replStart - 1, (int) start - 1, (int) (1 + stop - start));
                    } else {
                        errorProfile.enter();
                        throw PrimitiveFailed.BAD_INDEX;
                    }
                }
            }

            @SuppressWarnings("unused")
            @Fallback
            protected static final void doFail(final LargeIntegerObject rcvr, final long start, final long stop, final Object repl, final long replStart) {
                throw PrimitiveFailed.GENERIC_ERROR;
            }

            /* For specializing Integer>>copy:to:. */
            private static boolean inBoundsEntirely(final int rcvrInstSize, final int rcvrSize, final long start, final long stop, final int replInstSize, final int replSize, final long replStart) {
                return start == 1 && replStart == 1 && stop == replSize + replInstSize && stop == rcvrSize + rcvrInstSize;
            }
        }

        protected abstract static class NativeObjectReplaceNode extends AbstractNode {
            protected abstract void execute(NativeObject rcvr, long start, long stop, Object repl, long replStart);

            @Specialization(guards = {"rcvr.isByteType()", "repl.isByteType()"})
            protected static final void doNativeBytes(final NativeObject rcvr, final long start, final long stop, final NativeObject repl, final long replStart,
                            @Shared("errorProfile") @Cached final BranchProfile errorProfile) {
                try {
                    System.arraycopy(repl.getByteStorage(), (int) replStart - 1, rcvr.getByteStorage(), (int) start - 1, (int) (1 + stop - start));
                } catch (final IndexOutOfBoundsException e) {
                    errorProfile.enter();
                    throw PrimitiveFailed.BAD_INDEX;
                }
            }

            @Specialization(guards = {"rcvr.isShortType()", "repl.isShortType()"})
            protected static final void doNativeShorts(final NativeObject rcvr, final long start, final long stop, final NativeObject repl, final long replStart,
                            @Shared("errorProfile") @Cached final BranchProfile errorProfile) {
                try {
                    System.arraycopy(repl.getShortStorage(), (int) replStart - 1, rcvr.getShortStorage(), (int) start - 1, (int) (1 + stop - start));
                } catch (final IndexOutOfBoundsException e) {
                    errorProfile.enter();
                    throw PrimitiveFailed.BAD_INDEX;
                }
            }

            @Specialization(guards = {"rcvr.isIntType()", "repl.isIntType()"})
            protected static final void doNativeInts(final NativeObject rcvr, final long start, final long stop, final NativeObject repl, final long replStart,
                            @Shared("errorProfile") @Cached final BranchProfile errorProfile) {
                try {
                    System.arraycopy(repl.getIntStorage(), (int) replStart - 1, rcvr.getIntStorage(), (int) start - 1, (int) (1 + stop - start));
                } catch (final IndexOutOfBoundsException e) {
                    errorProfile.enter();
                    throw PrimitiveFailed.BAD_INDEX;
                }
            }

            @Specialization(guards = {"rcvr.isLongType()", "repl.isLongType()"})
            protected static final void doNativeLongs(final NativeObject rcvr, final long start, final long stop, final NativeObject repl, final long replStart,
                            @Shared("errorProfile") @Cached final BranchProfile errorProfile) {
                try {
                    System.arraycopy(repl.getLongStorage(), (int) replStart - 1, rcvr.getLongStorage(), (int) start - 1, (int) (1 + stop - start));
                } catch (final IndexOutOfBoundsException e) {
                    errorProfile.enter();
                    throw PrimitiveFailed.BAD_INDEX;
                }
            }

            @Specialization(guards = {"rcvr.isByteType()"})
            protected static final void doNativeLargeInteger(final NativeObject rcvr, final long start, final long stop, final LargeIntegerObject repl, final long replStart,
                            @Shared("errorProfile") @Cached final BranchProfile errorProfile) {
                try {
                    System.arraycopy(repl.getBytes(), (int) replStart - 1, rcvr.getByteStorage(), (int) start - 1, (int) (1 + stop - start));
                } catch (final IndexOutOfBoundsException e) {
                    errorProfile.enter();
                    throw PrimitiveFailed.BAD_INDEX;
                }
            }

            @SuppressWarnings("unused")
            @Fallback
            protected static final void doFail(final NativeObject rcvr, final long start, final long stop, final Object repl, final long replStart) {
                throw PrimitiveFailed.GENERIC_ERROR;
            }
        }

        protected abstract static class PointersObjectReplaceNode extends AbstractNode {
            protected abstract void execute(PointersObject rcvr, long start, long stop, Object repl, long replStart);

            @Specialization
            protected static final void doPointers(final PointersObject rcvr, final long start, final long stop, final VariablePointersObject repl, final long replStart,
                            @Cached final AbstractPointersObjectInstSizeNode rcvrSizeNode,
                            @Cached final AbstractPointersObjectInstSizeNode replSizeNode,
                            @Cached final AbstractPointersObjectReadNode readNode,
                            @Cached final AbstractPointersObjectWriteNode writeNode,
                            @Shared("errorProfile") @Cached final BranchProfile errorProfile) {
                final int rcvrSize = rcvrSizeNode.execute(rcvr);
                final int replSize = replSizeNode.execute(repl);
                if (inBounds(rcvrSize, rcvrSize, start, stop, replSize, replSize, replStart)) {
                    final int repOff = (int) (replStart - start);
                    for (int i = (int) (start - 1); i < stop; i++) {
                        writeNode.execute(rcvr, i, readNode.execute(repl, repOff + i));
                    }
                } else {
                    errorProfile.enter();
                    throw PrimitiveFailed.BAD_INDEX;
                }
            }

            @Specialization
            protected static final void doPointersArray(final PointersObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart,
                            @Cached final AbstractPointersObjectInstSizeNode rcvrSizeNode,
                            @Cached final ArrayObjectSizeNode sizeNode,
                            @Cached final ArrayObjectReadNode readNode,
                            @Cached final AbstractPointersObjectWriteNode writeNode,
                            @Shared("errorProfile") @Cached final BranchProfile errorProfile) {
                final int rcvrSize = rcvrSizeNode.execute(rcvr);
                if (!inBounds(rcvrSize, rcvrSize, start, stop, repl.instsize(), sizeNode.execute(repl), replStart)) {
                    errorProfile.enter();
                    throw PrimitiveFailed.BAD_INDEX;
                }
                final long repOff = replStart - start;
                for (int i = (int) (start - 1); i < stop; i++) {
                    writeNode.execute(rcvr, i, readNode.execute(repl, repOff + i));
                }
            }

            @SuppressWarnings("unused")
            @Fallback
            protected static final void doFail(final PointersObject rcvr, final long start, final long stop, final Object repl, final long replStart) {
                throw PrimitiveFailed.GENERIC_ERROR;
            }
        }

        protected abstract static class VariablePointersObjectReplaceNode extends AbstractNode {
            protected abstract void execute(VariablePointersObject rcvr, long start, long stop, Object repl, long replStart);

            @Specialization
            protected static final void doVariablePointers(final VariablePointersObject rcvr, final long start, final long stop, final VariablePointersObject repl, final long replStart,
                            @Cached final AbstractPointersObjectInstSizeNode rcvrInstSizeNode,
                            @Cached final AbstractPointersObjectInstSizeNode replInstSizeNode,
                            @Cached final VariablePointersObjectReadNode readNode,
                            @Cached final VariablePointersObjectWriteNode writeNode,
                            @Shared("errorProfile") @Cached final BranchProfile errorProfile) {
                final int rcvrInstSize = rcvrInstSizeNode.execute(rcvr);
                final int replInstSize = replInstSizeNode.execute(repl);
                if (!inBounds(rcvrInstSize, rcvrInstSize + rcvr.getVariablePartSize(), start, stop, replInstSize, replInstSize + repl.getVariablePartSize(), replStart)) {
                    errorProfile.enter();
                    throw PrimitiveFailed.BAD_INDEX;
                }
                final int repOff = (int) (replStart - start);
                for (int i = (int) (start - 1); i < stop; i++) {
                    writeNode.execute(rcvr, i, readNode.execute(repl, repOff + i));
                }
            }

            @Specialization
            protected static final void doVariablePointersArray(final VariablePointersObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart,
                            @Cached final AbstractPointersObjectInstSizeNode rcvrInstSizeNode,
                            @Cached final ArrayObjectSizeNode sizeNode,
                            @Cached final ArrayObjectReadNode readNode,
                            @Cached final VariablePointersObjectWriteNode writeNode,
                            @Shared("errorProfile") @Cached final BranchProfile errorProfile) {
                final int rcvrInstSize = rcvrInstSizeNode.execute(rcvr);
                if (!inBounds(rcvrInstSize, rcvrInstSize + rcvr.getVariablePartSize(), start, stop, repl.instsize(), sizeNode.execute(repl), replStart)) {
                    errorProfile.enter();
                    throw PrimitiveFailed.BAD_INDEX;
                }
                final long repOff = replStart - start;
                for (int i = (int) (start - 1); i < stop; i++) {
                    writeNode.execute(rcvr, i, readNode.execute(repl, repOff + i));
                }
            }

            @SuppressWarnings("unused")
            @Fallback
            protected static final void doFail(final VariablePointersObject rcvr, final long start, final long stop, final Object repl, final long replStart) {
                throw PrimitiveFailed.GENERIC_ERROR;
            }
        }

        protected abstract static class WeakPointersObjectReplaceNode extends AbstractNode {
            protected abstract void execute(WeakVariablePointersObject rcvr, long start, long stop, Object repl, long replStart);

            @Specialization
            protected static final void doWeakPointers(final WeakVariablePointersObject rcvr, final long start, final long stop, final WeakVariablePointersObject repl, final long replStart,
                            @Cached final AbstractPointersObjectInstSizeNode rcvrInstSizeNode,
                            @Cached final AbstractPointersObjectInstSizeNode replInstSizeNode,
                            @Cached final WeakVariablePointersObjectReadNode readNode,
                            @Cached final WeakVariablePointersObjectWriteNode writeNode,
                            @Shared("errorProfile") @Cached final BranchProfile errorProfile) {
                final int rcvrInstSize = rcvrInstSizeNode.execute(rcvr);
                final int replInstSize = replInstSizeNode.execute(repl);
                if (!inBounds(rcvrInstSize, rcvrInstSize + rcvr.getVariablePartSize(), start, stop, replInstSize, replInstSize + repl.getVariablePartSize(), replStart)) {
                    errorProfile.enter();
                    throw PrimitiveFailed.BAD_INDEX;
                }
                final int repOff = (int) (replStart - start);
                for (int i = (int) (start - 1); i < stop; i++) {
                    writeNode.execute(rcvr, i, readNode.execute(repl, repOff + i));
                }
            }

            @Specialization
            protected static final void doWeakPointersArray(final WeakVariablePointersObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart,
                            @Cached final AbstractPointersObjectInstSizeNode rcvrInstSizeNode,
                            @Cached final ArrayObjectSizeNode sizeNode,
                            @Cached final ArrayObjectReadNode readNode,
                            @Cached final WeakVariablePointersObjectWriteNode writeNode,
                            @Shared("errorProfile") @Cached final BranchProfile errorProfile) {
                final int rcvrInstSize = rcvrInstSizeNode.execute(rcvr);
                if (!inBounds(rcvrInstSize, rcvrInstSize + rcvr.getVariablePartSize(), start, stop, repl.instsize(), sizeNode.execute(repl), replStart)) {
                    errorProfile.enter();
                    throw PrimitiveFailed.BAD_INDEX;
                }
                final long repOff = replStart - start;
                for (int i = (int) (start - 1); i < stop; i++) {
                    writeNode.execute(rcvr, i, readNode.execute(repl, repOff + i));
                }
            }

            @SuppressWarnings("unused")
            @Fallback
            protected static final void doFail(final WeakVariablePointersObject rcvr, final long start, final long stop, final Object repl, final long replStart) {
                throw PrimitiveFailed.GENERIC_ERROR;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 106)
    protected abstract static class PrimScreenSizeNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        @Specialization
        protected static final PointersObject doSize(@SuppressWarnings("unused") final Object receiver,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image,
                        @Cached final AbstractPointersObjectWriteNode writeNode) {
            return image.asPoint(writeNode, image.getWindowSize());
        }
    }

    /* primitiveMouseButtons (#107) no longer in use, support dropped in TruffleSqueak. */

    /* primitiveKbd(Next|Peek) (#108|#109) no longer in use, support dropped in TruffleSqueak. */

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 126)
    protected abstract static class PrimDeferDisplayUpdatesNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        @Specialization
        protected static final Object doDefer(final Object receiver, final boolean flag,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image,
                        @Cached("createIdentityProfile()") final ValueProfile displayProfile) {
            if (image.hasDisplay()) {
                displayProfile.profile(image.getDisplay()).setDeferUpdates(flag);
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 127)
    protected abstract static class PrimShowDisplayRectNode extends AbstractPrimitiveNode implements QuinaryPrimitive {

        @Specialization
        protected static final PointersObject doShow(final PointersObject receiver, final long left, final long right, final long top, final long bottom,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            if (image.hasDisplay() && left < right && top < bottom) {
                image.getDisplay().showDisplayRect((int) left, (int) right, (int) top, (int) bottom);
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 133)
    protected abstract static class PrimSetInterruptKeyNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        @Specialization
        protected static final Object set(final Object receiver, @SuppressWarnings("unused") final long keycode) {
            // TODO: interrupt key is obsolete in image, but maybe still needed in the vm?
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 140)
    protected abstract static class PrimBeepNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        @Specialization
        protected static final Object doBeep(final Object receiver,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            if (image.hasDisplay()) {
                image.getDisplay().beep();
            } else {
                image.printToStdOut((char) 7);
            }
            return receiver;
        }
    }
}
