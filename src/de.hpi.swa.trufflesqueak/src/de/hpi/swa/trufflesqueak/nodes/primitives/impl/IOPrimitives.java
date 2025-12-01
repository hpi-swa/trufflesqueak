/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.profiles.InlinedExactClassProfile;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.image.SqueakImageWriter;
import de.hpi.swa.trufflesqueak.io.SqueakDisplay;
import de.hpi.swa.trufflesqueak.io.SqueakIOConstants;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.FloatObject;
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
import de.hpi.swa.trufflesqueak.nodes.context.GetOrCreateContextWithFrameNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode.AbstractPrimitiveWithFrameNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive0;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive0WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive2WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive3WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive4WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive6WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.util.UnsafeUtils;

public final class IOPrimitives extends AbstractPrimitiveFactoryHolder {

    /* primitiveMousePoint (#90) no longer in use, support dropped in TruffleSqueak. */

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 91)
    protected abstract static class PrimTestDisplayDepthNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization
        protected final boolean doTest(@SuppressWarnings("unused") final Object receiver, final long depth) {
            if (getContext().hasDisplay()) {
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
    protected abstract static class PrimInputSemaphoreNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization
        protected final Object doSet(final Object receiver, final long semaIndex) {
            final SqueakImageContext image = getContext();
            if (image.hasDisplay()) {
                image.getDisplay().setInputSemaphoreIndex((int) semaIndex);
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 94)
    protected abstract static class PrimGetNextEventNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization
        protected final PointersObject doGetNext(final PointersObject eventSensor, final ArrayObject targetArray) {
            final SqueakImageContext image = getContext();
            if (image.hasDisplay()) {
                final SqueakDisplay display = image.getDisplay();
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
    protected abstract static class PrimSnapshotNode extends AbstractPrimitiveWithFrameNode implements Primitive0WithFallback {

        @Specialization
        public final boolean doSnapshot(final VirtualFrame frame, @SuppressWarnings("unused") final PointersObject receiver,
                        @Bind final Node node,
                        @Bind final SqueakImageContext image,
                        @Cached(inline = true) final GetOrCreateContextWithFrameNode getOrCreateContextNode) {
            writeImage(image, getOrCreateContextNode.executeGet(frame, node));
            /* Return false to signal that the image is not resuming. */
            return BooleanObject.FALSE;
        }

        @TruffleBoundary
        private void writeImage(final SqueakImageContext image, final ContextObject thisContext) {
            /* Ensure all forwarded objects are removed. */
            image.objectGraphUtils.unfollow();
            /* Push true on stack for saved snapshot. */
            thisContext.push(BooleanObject.TRUE);
            SqueakImageWriter.write(getContext(), thisContext);
            /* Pop true again. */
            thisContext.pop();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 98)
    protected abstract static class PrimStoreImageSegmentNode extends AbstractPrimitiveNode implements Primitive3WithFallback {

        @SuppressWarnings("unused")
        @Specialization(guards = "segmentWordArray.isIntType()")
        protected static final Object doStore(final Object receiver, final ArrayObject rootsArray, final NativeObject segmentWordArray, final ArrayObject outPointerArray) {
            /*
             * TODO: implement primitive. In the meantime, pretend this primitive succeeds so that
             * some tests (e.g. BitmapStreamTests) run quickly.
             */
            throw PrimitiveFailed.GENERIC_ERROR;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 99)
    protected abstract static class PrimLoadImageSegmentNode extends AbstractPrimitiveNode implements Primitive2WithFallback {

        @SuppressWarnings("unused")
        @Specialization(guards = "segmentWordArray.isIntType()")
        protected static final ArrayObject doLoad(final Object receiver, final NativeObject segmentWordArray, final ArrayObject outPointerArray) {
            /*
             * TODO: implement primitive. In the meantime, pretend this primitive succeeds so that
             * some tests (e.g. BitmapStreamTests) run quickly.
             */
            throw PrimitiveFailed.GENERIC_ERROR;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 101)
    protected abstract static class PrimBeCursor1Node extends AbstractPrimitiveNode implements Primitive0WithFallback {
        @Specialization
        protected final PointersObject doCursor(final PointersObject receiver,
                        @Bind final Node node,
                        @Cached final AbstractPointersObjectReadNode cursorReadNode,
                        @Cached final AbstractPointersObjectReadNode offsetReadNode) {
            final SqueakImageContext image = getContext();
            if (image.hasDisplay()) {
                final PointersObject offset = receiver.getFormOffset(cursorReadNode, node);
                final int offsetX = Math.abs(offsetReadNode.executeInt(node, offset, POINT.X));
                final int offsetY = Math.abs(offsetReadNode.executeInt(node, offset, POINT.Y));
                image.getDisplay().setCursor(receiver.getFormBits(cursorReadNode, node), null, receiver.getFormWidth(cursorReadNode, node), receiver.getFormHeight(cursorReadNode, node),
                                receiver.getFormDepth(cursorReadNode, node), offsetX, offsetY);
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 101)
    protected abstract static class PrimBeCursor2Node extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected final PointersObject doCursor(final PointersObject receiver, final PointersObject maskObject,
                        @Bind final Node node,
                        @Cached final AbstractPointersObjectReadNode cursorReadNode,
                        @Cached final AbstractPointersObjectReadNode offsetReadNode,
                        @Cached final InlinedConditionProfile depthProfile) {
            final SqueakImageContext image = getContext();
            if (image.hasDisplay()) {
                final int[] words = receiver.getFormBits(cursorReadNode, node);
                final int depth = receiver.getFormDepth(cursorReadNode, node);
                final int height = receiver.getFormHeight(cursorReadNode, node);
                final int width = receiver.getFormWidth(cursorReadNode, node);
                final PointersObject offset = receiver.getFormOffset(cursorReadNode, node);
                final int offsetX = Math.abs(offsetReadNode.executeInt(node, offset, POINT.X));
                final int offsetY = Math.abs(offsetReadNode.executeInt(node, offset, POINT.Y));
                final int[] mask;
                final int realDepth;
                if (depthProfile.profile(node, depth == 1)) {
                    mask = cursorReadNode.executeNative(node, maskObject, FORM.BITS).getIntStorage();
                    realDepth = 2;
                } else {
                    mask = null;
                    realDepth = depth;
                }
                image.getDisplay().setCursor(words, mask, width, height, realDepth, offsetX, offsetY);
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 102)
    protected abstract static class PrimBeDisplayNode extends AbstractPrimitiveNode implements Primitive0WithFallback {

        @Specialization(guards = {"receiver.size() >= 4"})
        protected final boolean doDisplay(final PointersObject receiver) {
            final SqueakImageContext image = getContext();
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
    protected abstract static class PrimScanCharactersNode extends AbstractPrimitiveNode implements Primitive6WithFallback {
        private static final long END_OF_RUN = 257 - 1;
        private static final long CROSSED_X = 258 - 1;

        @Specialization(guards = {"startIndex > 0", "stopIndex > 0", "sourceString.isByteType()", "stopIndex <= sourceString.getByteLength()", "receiver.size() >= 4",
                        "arraySizeNode.execute(node, stops) >= 258", "hasCorrectSlots(pointersReadNode, arraySizeNode, node, receiver)"}, limit = "1")
        protected static final Object doScan(final PointersObject receiver, final long startIndex, final long stopIndex, final NativeObject sourceString, final long rightX,
                        final ArrayObject stops, final long kernData,
                        @Bind final Node node,
                        @Cached final AbstractPointersObjectReadNode pointersReadNode,
                        @Cached final AbstractPointersObjectWriteNode pointersWriteNode,
                        @Cached final ArrayObjectSizeNode arraySizeNode,
                        @Cached final ArrayObjectReadNode arrayReadNode) {
            final ArrayObject scanXTable = pointersReadNode.executeArray(node, receiver, CHARACTER_SCANNER.XTABLE);
            final ArrayObject scanMap = pointersReadNode.executeArray(node, receiver, CHARACTER_SCANNER.MAP);

            final int maxGlyph = arraySizeNode.execute(node, scanXTable) - 2;
            long scanDestX = pointersReadNode.executeLong(node, receiver, CHARACTER_SCANNER.DEST_X);
            long scanLastIndex = startIndex;
            while (scanLastIndex <= stopIndex) {
                final long ascii = sourceString.getByte(scanLastIndex - 1) & 0xFF;
                final Object stopReason = arrayReadNode.execute(node, stops, ascii);
                if (stopReason != NilObject.SINGLETON) {
                    storeStateInReceiver(pointersWriteNode, node, receiver, scanDestX, scanLastIndex);
                    return stopReason;
                }
                if (arraySizeNode.execute(node, scanMap) <= ascii) {
                    throw PrimitiveFailed.andTransferToInterpreter();
                }
                final long glyphIndex = (long) arrayReadNode.execute(node, scanMap, ascii);
                if (glyphIndex < 0 || glyphIndex > maxGlyph) {
                    throw PrimitiveFailed.andTransferToInterpreter();
                }
                final long sourceX1;
                final long sourceX2;
                sourceX1 = (long) arrayReadNode.execute(node, scanXTable, glyphIndex);
                sourceX2 = (long) arrayReadNode.execute(node, scanXTable, glyphIndex + 1);
                final long nextDestX = scanDestX + sourceX2 - sourceX1;
                if (nextDestX > rightX) {
                    storeStateInReceiver(pointersWriteNode, node, receiver, scanDestX, scanLastIndex);
                    return arrayReadNode.execute(node, stops, CROSSED_X);
                }
                scanDestX = nextDestX + kernData;
                scanLastIndex++;
            }
            storeStateInReceiver(pointersWriteNode, node, receiver, scanDestX, stopIndex);
            return arrayReadNode.execute(node, stops, END_OF_RUN);
        }

        private static void storeStateInReceiver(final AbstractPointersObjectWriteNode writeNode, final Node inlineTarget, final PointersObject receiver, final long scanDestX,
                        final long scanLastIndex) {
            writeNode.execute(inlineTarget, receiver, CHARACTER_SCANNER.DEST_X, scanDestX);
            writeNode.execute(inlineTarget, receiver, CHARACTER_SCANNER.LAST_INDEX, scanLastIndex);
        }

        protected static final boolean hasCorrectSlots(final AbstractPointersObjectReadNode readNode, final ArrayObjectSizeNode arraySizeNode, final Node inlineTarget, final PointersObject receiver) {
            return readNode.execute(inlineTarget, receiver, CHARACTER_SCANNER.DEST_X) instanceof Long &&
                            readNode.execute(inlineTarget, receiver, CHARACTER_SCANNER.XTABLE) instanceof ArrayObject &&
                            readNode.execute(inlineTarget, receiver, CHARACTER_SCANNER.MAP) instanceof final ArrayObject scanMap && arraySizeNode.execute(inlineTarget, scanMap) == 256;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 105)
    protected abstract static class PrimStringReplaceNode extends AbstractPrimitiveNode implements Primitive4WithFallback {
        @Specialization
        protected static final NativeObject doNative(final NativeObject rcvr, final long start, final long stop, final Object repl, final long replStart,
                        @Bind final Node node,
                        @Cached final NativeObjectReplaceNode replaceNode) {
            replaceNode.execute(node, rcvr, start, stop, repl, replStart);
            return rcvr;
        }

        @Specialization
        protected static final ArrayObject doArray(final ArrayObject rcvr, final long start, final long stop, final Object repl, final long replStart,
                        @Bind final Node node,
                        @Cached final ArrayObjectReplaceNode replaceNode) {
            replaceNode.execute(node, rcvr, start, stop, repl, replStart);
            return rcvr;
        }

        @Specialization
        protected static final PointersObject doPointers(final PointersObject rcvr, final long start, final long stop, final Object repl, final long replStart,
                        @Bind final Node node,
                        @Cached final PointersObjectReplaceNode replaceNode) {
            replaceNode.execute(node, rcvr, start, stop, repl, replStart);
            return rcvr;
        }

        @Specialization
        protected static final VariablePointersObject doPointers(final VariablePointersObject rcvr, final long start, final long stop, final Object repl, final long replStart,
                        @Bind final Node node,
                        @Cached final VariablePointersObjectReplaceNode replaceNode) {
            replaceNode.execute(node, rcvr, start, stop, repl, replStart);
            return rcvr;
        }

        @Specialization
        protected static final WeakVariablePointersObject doWeakPointers(final WeakVariablePointersObject rcvr, final long start, final long stop, final Object repl, final long replStart,
                        @Bind final Node node,
                        @Cached final WeakPointersObjectReplaceNode replaceNode) {
            replaceNode.execute(node, rcvr, start, stop, repl, replStart);
            return rcvr;
        }

        @Specialization
        protected static final CompiledCodeObject doMethod(final CompiledCodeObject rcvr, final long start, final long stop, final CompiledCodeObject repl, final long replStart,
                        @Bind final Node node,
                        @Shared("errorProfile") @Cached final InlinedBranchProfile errorProfile) {
            if (!inBounds(rcvr.instsize(), rcvr.size(), start, stop, repl.instsize(), repl.size(), replStart)) {
                errorProfile.enter(node);
                throw PrimitiveFailed.BAD_INDEX;
            }
            final long repOff = replStart - start;
            for (long i = start - 1; i < stop; i++) {
                rcvr.atput0(i, repl.at0(repOff + i));
            }
            return rcvr;
        }

        /* FloatObject specialization used by Cuis 5.0. */
        @SuppressWarnings("unused")
        @Specialization(guards = {"repl.isIntType()"})
        protected static final FloatObject doFloat(final FloatObject rcvr, final long start, final long stop, final NativeObject repl, final long replStart,
                        @Bind final Node node,
                        @Shared("errorProfile") @Cached final InlinedBranchProfile errorProfile) {
            if (!inBounds(rcvr.instsize(), rcvr.size(), start, stop, repl.instsize(), repl.getIntLength(), replStart)) {
                errorProfile.enter(node);
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

        @GenerateInline
        @GenerateCached(false)
        protected abstract static class ArrayObjectReplaceNode extends AbstractNode {

            protected abstract void execute(Node node, ArrayObject rcvr, long start, long stop, Object repl, long replStart);

            @SuppressWarnings("unused")
            @Specialization(guards = {"rcvr.isEmptyType()", "repl.isEmptyType()"})
            protected static final void doEmptyArrays(final Node node, final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart,
                            @Shared("errorProfile") @Cached final InlinedBranchProfile errorProfile) {
                if (!inBounds(rcvr.getEmptyLength(), start, stop, repl.getEmptyLength(), replStart)) {
                    errorProfile.enter(node);
                    throw PrimitiveFailed.BAD_INDEX;
                }
                // Nothing to do.
            }

            @Specialization(guards = {"rcvr.isBooleanType()", "repl.isBooleanType()"})
            protected static final void doArraysOfBooleans(final Node node, final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart,
                            @Shared("errorProfile") @Cached final InlinedBranchProfile errorProfile) {
                if (inBounds(rcvr.getBooleanLength(), start, stop, repl.getBooleanLength(), replStart)) {
                    UnsafeUtils.copyBytes(repl.getBooleanStorage(), replStart - 1, rcvr.getBooleanStorage(), start - 1, 1 + stop - start);
                } else {
                    errorProfile.enter(node);
                    throw PrimitiveFailed.BAD_INDEX;
                }
            }

            @Specialization(guards = {"rcvr.isCharType()", "repl.isCharType()"})
            protected static final void doArraysOfChars(final Node node, final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart,
                            @Shared("errorProfile") @Cached final InlinedBranchProfile errorProfile) {
                if (inBounds(rcvr.getCharLength(), start, stop, repl.getCharLength(), replStart)) {
                    UnsafeUtils.copyChars(repl.getCharStorage(), replStart - 1, rcvr.getCharStorage(), start - 1, 1 + stop - start);
                } else {
                    errorProfile.enter(node);
                    throw PrimitiveFailed.BAD_INDEX;
                }
            }

            @Specialization(guards = {"rcvr.isLongType()", "repl.isLongType()"})
            protected static final void doArraysOfLongs(final Node node, final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart,
                            @Shared("errorProfile") @Cached final InlinedBranchProfile errorProfile) {
                if (inBounds(rcvr.getLongLength(), start, stop, repl.getLongLength(), replStart)) {
                    UnsafeUtils.copyLongs(repl.getLongStorage(), replStart - 1, rcvr.getLongStorage(), start - 1, 1 + stop - start);
                } else {
                    errorProfile.enter(node);
                    throw PrimitiveFailed.BAD_INDEX;
                }
            }

            @Specialization(guards = {"rcvr.isDoubleType()", "repl.isDoubleType()"})
            protected static final void doArraysOfDoubles(final Node node, final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart,
                            @Shared("errorProfile") @Cached final InlinedBranchProfile errorProfile) {
                if (inBounds(rcvr.getDoubleLength(), start, stop, repl.getDoubleLength(), replStart)) {
                    UnsafeUtils.copyDoubles(repl.getDoubleStorage(), replStart - 1, rcvr.getDoubleStorage(), start - 1, 1 + stop - start);
                } else {
                    errorProfile.enter(node);
                    throw PrimitiveFailed.BAD_INDEX;
                }
            }

            @Specialization(guards = {"rcvr.isObjectType()", "repl.isObjectType()"})
            protected static final void doArraysOfObjects(final Node node, final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart,
                            @Shared("errorProfile") @Cached final InlinedBranchProfile errorProfile) {
                try {
                    System.arraycopy(repl.getObjectStorage(), (int) replStart - 1, rcvr.getObjectStorage(), (int) start - 1, (int) (1 + stop - start));
                } catch (final Throwable e) {
                    errorProfile.enter(node);
                    throw PrimitiveFailed.BAD_INDEX;
                }
            }

            @Specialization(guards = {"!rcvr.hasSameStorageType(repl)"})
            protected static final void doArraysWithDifferenStorageTypes(final Node node, final ArrayObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart,
                            @Exclusive @Cached final ArrayObjectSizeNode sizeNode,
                            @Exclusive @Cached final ArrayObjectSizeNode replSizeNode,
                            @Cached final ArrayObjectReadNode readNode,
                            @Exclusive @Cached final ArrayObjectWriteNode writeNode,
                            @Exclusive @Cached final InlinedBranchProfile errorProfile) {
                if (!inBounds(sizeNode.execute(node, rcvr), start, stop, replSizeNode.execute(node, repl), replStart)) {
                    errorProfile.enter(node);
                    throw PrimitiveFailed.BAD_INDEX;
                }
                final long repOff = replStart - start;
                for (long i = start - 1; i < stop; i++) {
                    writeNode.execute(node, rcvr, i, readNode.execute(node, repl, repOff + i));
                }
            }

            @Specialization
            protected static final void doArrayObjectPointers(final Node node, final ArrayObject rcvr, final long start, final long stop, final VariablePointersObject repl, final long replStart,
                            @Exclusive @Cached final ArrayObjectSizeNode sizeNode,
                            @Cached final VariablePointersObjectReadNode readNode,
                            @Exclusive @Cached final ArrayObjectWriteNode writeNode,
                            @Exclusive @Cached final InlinedBranchProfile errorProfile) {
                if (!inBounds(rcvr.instsize(), sizeNode.execute(node, rcvr), start, stop, repl.instsize(), repl.size(), replStart)) {
                    errorProfile.enter(node);
                    throw PrimitiveFailed.BAD_INDEX;
                }
                final long repOff = replStart - start;
                for (long i = start - 1; i < stop; i++) {
                    writeNode.execute(node, rcvr, i, readNode.execute(node, repl, repOff + i));
                }
            }

            @Specialization
            protected static final void doArrayObjectWeakPointers(final Node node, final ArrayObject rcvr, final long start, final long stop, final WeakVariablePointersObject repl,
                            final long replStart,
                            @Exclusive @Cached final ArrayObjectSizeNode sizeNode,
                            @Cached final WeakVariablePointersObjectReadNode readNode,
                            @Exclusive @Cached final ArrayObjectWriteNode writeNode,
                            @Exclusive @Cached final InlinedBranchProfile errorProfile) {
                if (!inBounds(rcvr.instsize(), sizeNode.execute(node, rcvr), start, stop, repl.instsize(), repl.size(), replStart)) {
                    errorProfile.enter(node);
                    throw PrimitiveFailed.BAD_INDEX;
                }
                final long repOff = replStart - start;
                for (long i = start - 1; i < stop; i++) {
                    writeNode.execute(node, rcvr, i, readNode.execute(node, repl, repOff + i));
                }
            }

            @SuppressWarnings("unused")
            @Fallback
            protected static final void doFail(final Node node, final ArrayObject rcvr, final long start, final long stop, final Object repl, final long replStart) {
                throw PrimitiveFailed.GENERIC_ERROR;
            }
        }

        @GenerateInline
        @GenerateCached(false)
        protected abstract static class NativeObjectReplaceNode extends AbstractNode {
            protected abstract void execute(Node node, NativeObject rcvr, long start, long stop, Object repl, long replStart);

            @Specialization(guards = {"rcvr.isByteType()", "repl.isByteType()"})
            protected static final void doNativeBytes(final Node node, final NativeObject rcvr, final long start, final long stop, final NativeObject repl, final long replStart,
                            @Shared("errorProfile") @Cached final InlinedBranchProfile errorProfile) {
                if (inBounds(rcvr.getByteLength(), start, stop, repl.getByteLength(), replStart)) {
                    UnsafeUtils.copyBytes(repl.getByteStorage(), replStart - 1, rcvr.getByteStorage(), start - 1, 1 + stop - start);
                } else {
                    errorProfile.enter(node);
                    throw PrimitiveFailed.BAD_INDEX;
                }
            }

            @Specialization(guards = {"rcvr.isShortType()", "repl.isShortType()"})
            protected static final void doNativeShorts(final Node node, final NativeObject rcvr, final long start, final long stop, final NativeObject repl, final long replStart,
                            @Shared("errorProfile") @Cached final InlinedBranchProfile errorProfile) {
                if (inBounds(rcvr.getShortLength(), start, stop, repl.getShortLength(), replStart)) {
                    UnsafeUtils.copyShorts(repl.getShortStorage(), replStart - 1, rcvr.getShortStorage(), start - 1, 1 + stop - start);
                } else {
                    errorProfile.enter(node);
                    throw PrimitiveFailed.BAD_INDEX;
                }
            }

            @Specialization(guards = {"rcvr.isIntType()", "repl.isIntType()"})
            protected static final void doNativeInts(final Node node, final NativeObject rcvr, final long start, final long stop, final NativeObject repl, final long replStart,
                            @Shared("errorProfile") @Cached final InlinedBranchProfile errorProfile) {
                if (inBounds(rcvr.getIntLength(), start, stop, repl.getIntLength(), replStart)) {
                    UnsafeUtils.copyInts(repl.getIntStorage(), replStart - 1, rcvr.getIntStorage(), start - 1, 1 + stop - start);
                } else {
                    errorProfile.enter(node);
                    throw PrimitiveFailed.BAD_INDEX;
                }
            }

            @Specialization(guards = {"rcvr.isLongType()", "repl.isLongType()"})
            protected static final void doNativeLongs(final Node node, final NativeObject rcvr, final long start, final long stop, final NativeObject repl, final long replStart,
                            @Shared("errorProfile") @Cached final InlinedBranchProfile errorProfile) {
                if (inBounds(rcvr.getLongLength(), start, stop, repl.getLongLength(), replStart)) {
                    UnsafeUtils.copyLongs(repl.getLongStorage(), replStart - 1, rcvr.getLongStorage(), start - 1, 1 + stop - start);
                } else {
                    errorProfile.enter(node);
                    throw PrimitiveFailed.BAD_INDEX;
                }
            }

            @SuppressWarnings("unused")
            @Fallback
            protected static final void doFail(final NativeObject rcvr, final long start, final long stop, final Object repl, final long replStart) {
                throw PrimitiveFailed.GENERIC_ERROR;
            }
        }

        @GenerateInline
        @GenerateCached(false)
        protected abstract static class PointersObjectReplaceNode extends AbstractNode {
            protected abstract void execute(Node node, PointersObject rcvr, long start, long stop, Object repl, long replStart);

            @Specialization
            protected static final void doPointers(final Node node, final PointersObject rcvr, final long start, final long stop, final VariablePointersObject repl, final long replStart,
                            @Shared("rcvrSizeNode") @Cached final AbstractPointersObjectInstSizeNode rcvrSizeNode,
                            @Exclusive @Cached final AbstractPointersObjectInstSizeNode replSizeNode,
                            @Cached final AbstractPointersObjectReadNode readNode,
                            @Shared("writeNode") @Cached final AbstractPointersObjectWriteNode writeNode,
                            @Shared("errorProfile") @Cached final InlinedBranchProfile errorProfile) {
                final int rcvrSize = rcvrSizeNode.execute(node, rcvr);
                final int replSize = replSizeNode.execute(node, repl);
                if (inBounds(rcvrSize, rcvrSize, start, stop, replSize, replSize, replStart)) {
                    final long repOff = replStart - start;
                    for (long i = start - 1; i < stop; i++) {
                        writeNode.execute(node, rcvr, i, readNode.execute(node, repl, repOff + i));
                    }
                } else {
                    errorProfile.enter(node);
                    throw PrimitiveFailed.BAD_INDEX;
                }
            }

            @Specialization
            protected static final void doPointersArray(final Node node, final PointersObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart,
                            @Shared("rcvrSizeNode") @Cached final AbstractPointersObjectInstSizeNode rcvrSizeNode,
                            @Cached final ArrayObjectSizeNode sizeNode,
                            @Cached final ArrayObjectReadNode readNode,
                            @Shared("writeNode") @Cached final AbstractPointersObjectWriteNode writeNode,
                            @Shared("errorProfile") @Cached final InlinedBranchProfile errorProfile) {
                final int rcvrSize = rcvrSizeNode.execute(node, rcvr);
                if (!inBounds(rcvrSize, rcvrSize, start, stop, repl.instsize(), sizeNode.execute(node, repl), replStart)) {
                    errorProfile.enter(node);
                    throw PrimitiveFailed.BAD_INDEX;
                }
                final long repOff = replStart - start;
                for (long i = start - 1; i < stop; i++) {
                    writeNode.execute(node, rcvr, i, readNode.execute(node, repl, repOff + i));
                }
            }

            @SuppressWarnings("unused")
            @Fallback
            protected static final void doFail(final PointersObject rcvr, final long start, final long stop, final Object repl, final long replStart) {
                throw PrimitiveFailed.GENERIC_ERROR;
            }
        }

        @GenerateInline
        @GenerateCached(false)
        protected abstract static class VariablePointersObjectReplaceNode extends AbstractNode {
            protected abstract void execute(Node node, VariablePointersObject rcvr, long start, long stop, Object repl, long replStart);

            @Specialization
            protected static final void doVariablePointers(final Node node, final VariablePointersObject rcvr, final long start, final long stop, final VariablePointersObject repl,
                            final long replStart,
                            @Shared("rcvrInstSizeNode") @Cached final AbstractPointersObjectInstSizeNode rcvrInstSizeNode,
                            @Exclusive @Cached final AbstractPointersObjectInstSizeNode replInstSizeNode,
                            @Cached final VariablePointersObjectReadNode readNode,
                            @Shared("writeNode") @Cached final VariablePointersObjectWriteNode writeNode,
                            @Shared("errorProfile") @Cached final InlinedBranchProfile errorProfile) {
                final int rcvrInstSize = rcvrInstSizeNode.execute(node, rcvr);
                final int replInstSize = replInstSizeNode.execute(node, repl);
                if (!inBounds(rcvrInstSize, rcvrInstSize + rcvr.getVariablePartSize(), start, stop, replInstSize, replInstSize + repl.getVariablePartSize(), replStart)) {
                    errorProfile.enter(node);
                    throw PrimitiveFailed.BAD_INDEX;
                }
                final long repOff = replStart - start;
                for (long i = start - 1; i < stop; i++) {
                    writeNode.execute(node, rcvr, i, readNode.execute(node, repl, repOff + i));
                }
            }

            @Specialization
            protected static final void doVariablePointersArray(final Node node, final VariablePointersObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart,
                            @Shared("rcvrInstSizeNode") @Cached final AbstractPointersObjectInstSizeNode rcvrInstSizeNode,
                            @Cached final ArrayObjectSizeNode sizeNode,
                            @Cached final ArrayObjectReadNode readNode,
                            @Shared("writeNode") @Cached final VariablePointersObjectWriteNode writeNode,
                            @Shared("errorProfile") @Cached final InlinedBranchProfile errorProfile) {
                final int rcvrInstSize = rcvrInstSizeNode.execute(node, rcvr);
                if (!inBounds(rcvrInstSize, rcvrInstSize + rcvr.getVariablePartSize(), start, stop, repl.instsize(), sizeNode.execute(node, repl), replStart)) {
                    errorProfile.enter(node);
                    throw PrimitiveFailed.BAD_INDEX;
                }
                final long repOff = replStart - start;
                for (long i = start - 1; i < stop; i++) {
                    writeNode.execute(node, rcvr, i, readNode.execute(node, repl, repOff + i));
                }
            }

            @SuppressWarnings("unused")
            @Fallback
            protected static final void doFail(final VariablePointersObject rcvr, final long start, final long stop, final Object repl, final long replStart) {
                throw PrimitiveFailed.GENERIC_ERROR;
            }
        }

        @GenerateInline
        @GenerateCached(false)
        protected abstract static class WeakPointersObjectReplaceNode extends AbstractNode {
            protected abstract void execute(Node node, WeakVariablePointersObject rcvr, long start, long stop, Object repl, long replStart);

            @Specialization
            protected static final void doWeakPointers(final Node node, final WeakVariablePointersObject rcvr, final long start, final long stop, final WeakVariablePointersObject repl,
                            final long replStart,
                            @Shared("rcvrInstSizeNode") @Cached final AbstractPointersObjectInstSizeNode rcvrInstSizeNode,
                            @Exclusive @Cached final AbstractPointersObjectInstSizeNode replInstSizeNode,
                            @Cached final WeakVariablePointersObjectReadNode readNode,
                            @Shared("writeNode") @Cached final WeakVariablePointersObjectWriteNode writeNode,
                            @Shared("errorProfile") @Cached final InlinedBranchProfile errorProfile) {
                final int rcvrInstSize = rcvrInstSizeNode.execute(node, rcvr);
                final int replInstSize = replInstSizeNode.execute(node, repl);
                if (!inBounds(rcvrInstSize, rcvrInstSize + rcvr.getVariablePartSize(), start, stop, replInstSize, replInstSize + repl.getVariablePartSize(), replStart)) {
                    errorProfile.enter(node);
                    throw PrimitiveFailed.BAD_INDEX;
                }
                final long repOff = replStart - start;
                for (long i = start - 1; i < stop; i++) {
                    writeNode.execute(node, rcvr, i, readNode.execute(node, repl, repOff + i));
                }
            }

            @Specialization
            protected static final void doWeakPointersArray(final Node node, final WeakVariablePointersObject rcvr, final long start, final long stop, final ArrayObject repl, final long replStart,
                            @Shared("rcvrInstSizeNode") @Cached final AbstractPointersObjectInstSizeNode rcvrInstSizeNode,
                            @Cached final ArrayObjectSizeNode sizeNode,
                            @Cached final ArrayObjectReadNode readNode,
                            @Shared("writeNode") @Cached final WeakVariablePointersObjectWriteNode writeNode,
                            @Shared("errorProfile") @Cached final InlinedBranchProfile errorProfile) {
                final int rcvrInstSize = rcvrInstSizeNode.execute(node, rcvr);
                if (!inBounds(rcvrInstSize, rcvrInstSize + rcvr.getVariablePartSize(), start, stop, repl.instsize(), sizeNode.execute(node, repl), replStart)) {
                    errorProfile.enter(node);
                    throw PrimitiveFailed.BAD_INDEX;
                }
                final long repOff = replStart - start;
                for (long i = start - 1; i < stop; i++) {
                    writeNode.execute(node, rcvr, i, readNode.execute(node, repl, repOff + i));
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
    public abstract static class PrimScreenSizeNode extends AbstractPrimitiveNode implements Primitive0 {
        @Specialization
        protected static final Object doScreenSize(@SuppressWarnings("unused") final Object receiver,
                        @Bind final Node node,
                        @Cached final AbstractPointersObjectWriteNode writeNode) {
            final long x;
            final long y;
            final SqueakImageContext image = getContext(node);
            if (image.hasDisplay() && image.getDisplay().isVisible()) {
                x = image.getDisplay().getWindowWidth();
                y = image.getDisplay().getWindowHeight();
            } else {
                x = image.flags.getSnapshotScreenWidth();
                y = image.flags.getSnapshotScreenHeight();
            }
            return image.asPoint(writeNode, node, x, y);
        }
    }

    /* primitiveMouseButtons (#107) no longer in use, support dropped in TruffleSqueak. */

    /* primitiveKbd(Next|Peek) (#108|#109) no longer in use, support dropped in TruffleSqueak. */

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 126)
    protected abstract static class PrimDeferDisplayUpdatesNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization
        protected final Object doDefer(final Object receiver, final boolean flag,
                        @Bind final Node node,
                        @Cached final InlinedExactClassProfile displayProfile) {
            final SqueakImageContext image = getContext();
            if (image.hasDisplay()) {
                displayProfile.profile(node, image.getDisplay()).setDeferUpdates(flag);
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 127)
    protected abstract static class PrimShowDisplayRectNode extends AbstractPrimitiveNode implements Primitive4WithFallback {

        @Specialization
        protected final PointersObject doShow(final PointersObject receiver, final long left, final long right, final long top, final long bottom) {
            final SqueakImageContext image = getContext();
            if (image.hasDisplay() && left < right && top < bottom) {
                image.getDisplay().showDisplayRect((int) left, (int) top, (int) right, (int) bottom);
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 133)
    protected abstract static class PrimSetInterruptKeyNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization
        protected static final Object set(final Object receiver, @SuppressWarnings("unused") final long keycode) {
            // TODO: interrupt key is obsolete in image, but maybe still needed in the vm?
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 140)
    protected abstract static class PrimBeepNode extends AbstractPrimitiveNode implements Primitive0 {

        @Specialization
        protected final Object doBeep(final Object receiver) {
            final SqueakImageContext image = getContext();
            if (image.hasDisplay()) {
                SqueakDisplay.beep();
            } else {
                printBeepCharacter(image);
            }
            return receiver;
        }

        @TruffleBoundary
        private static void printBeepCharacter(final SqueakImageContext image) {
            final OutputStream out = image.env.out();
            try {
                out.write((char) 7);
                out.flush();
            } catch (IOException e) {
                throw PrimitiveFailed.andTransferToInterpreterWithError(e);
            }
        }
    }

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return IOPrimitivesFactory.getFactories();
    }
}
