package de.hpi.swa.graal.squeak.nodes.plugins;

import java.util.function.LongBinaryOperator;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.NotProvided;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.BIT_BLT;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.FORM;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithCode;
import de.hpi.swa.graal.squeak.nodes.plugins.BitBltPluginCopyBitsHelpersFactory.CopyBitsClipHelperNodeGen;
import de.hpi.swa.graal.squeak.nodes.plugins.BitBltPluginCopyBitsHelpersFactory.CopyBitsEnsureDepthAndExecuteHelperNodeGen;
import de.hpi.swa.graal.squeak.nodes.plugins.BitBltPluginCopyBitsHelpersFactory.CopyBitsExecuteHelperNodeGen;
import de.hpi.swa.graal.squeak.nodes.plugins.BitBltPluginCopyBitsHelpersFactory.CopyBitsExtractHelperNodeGen;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.SimulationPrimitiveNode;

public final class BitBltPluginCopyBitsHelpers {

    private BitBltPluginCopyBitsHelpers() {
    }

    protected abstract static class CopyBitsEnsureDepthAndExecuteHelperNode extends AbstractNodeWithCode {
        @Child private CopyBitsExtractHelperNode extractNode;
        @Child private SimulationPrimitiveNode simulateNode;

        protected static CopyBitsEnsureDepthAndExecuteHelperNode create(final CompiledCodeObject code) {
            return CopyBitsEnsureDepthAndExecuteHelperNodeGen.create(code);
        }

        protected CopyBitsEnsureDepthAndExecuteHelperNode(final CompiledCodeObject code) {
            super(code);
        }

        protected final Object executeExtract(final VirtualFrame frame, final PointersObject receiver, final Object sourceForm, final Object destForm) {
            return executeExtract(frame, receiver, receiver.at0(BIT_BLT.COMBINATION_RULE), sourceForm, destForm);
        }

        protected abstract Object executeExtract(VirtualFrame frame, PointersObject receiver, Object combinationRule, Object sourceForm, Object destForm);

        @Specialization(guards = {"is32BitForm(sourceForm)", "is32BitForm(destForm)", "isSupported(combinationRule, sourceForm, destForm)"})
        protected final Object executeWithSourceForm(final PointersObject receiver, final long combinationRule, final PointersObject sourceForm, final PointersObject destForm) {
            getExtractNode().executeExtract(receiver, combinationRule,
                            sourceForm.at0(FORM.BITS), sourceForm.at0(FORM.WIDTH), sourceForm.at0(FORM.HEIGHT),
                            destForm.at0(FORM.BITS), destForm.at0(FORM.WIDTH), destForm.at0(FORM.HEIGHT));
            return receiver;
        }

        @Specialization(guards = {"is32BitForm(destForm)", "isSupported(combinationRule, sourceForm, destForm)"})
        protected final Object executeWithoutSourceForm(final PointersObject receiver, final long combinationRule, final NilObject sourceForm, final PointersObject destForm) {
            getExtractNode().executeExtract(receiver, combinationRule, sourceForm, sourceForm, sourceForm, destForm.at0(FORM.BITS), destForm.at0(FORM.WIDTH), destForm.at0(FORM.HEIGHT));
            return receiver;
        }

        @SuppressWarnings("unused")
        @Fallback
        protected final Object doSimulation(final VirtualFrame frame, final PointersObject receiver, final Object combinationRule, final Object sourceForm, final Object destForm) {
            return getSimulationPrimitiveNode().executeWithArguments(frame, receiver,
                            NotProvided.INSTANCE, NotProvided.INSTANCE, NotProvided.INSTANCE, NotProvided.INSTANCE, NotProvided.INSTANCE, NotProvided.INSTANCE);
        }

        private CopyBitsExtractHelperNode getExtractNode() {
            if (extractNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                extractNode = insert(CopyBitsExtractHelperNode.create());
            }
            return extractNode;
        }

        private SimulationPrimitiveNode getSimulationPrimitiveNode() {
            if (simulateNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                simulateNode = insert(SimulationPrimitiveNode.create((CompiledMethodObject) code, "BitBltPlugin", "primitiveCopyBits"));
            }
            return simulateNode;
        }

        protected boolean isSupported(final long combinationRule, final Object sourceForm, final PointersObject destForm) {
            if (sourceForm == destForm) {
                return false; // Overlaps are not supported.
            }
            if (combinationRule == 24) {
                // TODO: Fix combinationRule 24 with source form.
                // return true; // All combiRules implemented with and without sourceForms.
                return sourceForm == code.image.nil;
            }
            if (sourceForm != code.image.nil) {
                // All combiRules implemented with sourceForms:
                return combinationRule == 25;
            } else {
                // All combiRules implemented without sourceForms:
                return combinationRule == 3 || combinationRule == 4;
            }
        }

        protected static final boolean is32BitForm(final PointersObject target) {
            return (long) target.at0(FORM.DEPTH) == 32;
        }
    }

    protected abstract static class CopyBitsExtractHelperNode extends Node {
        @Child private CopyBitsClipHelperNode clipNode = CopyBitsClipHelperNode.create();
        @Child private SimulationPrimitiveNode simulateNode;

        protected static CopyBitsExtractHelperNode create() {
            return CopyBitsExtractHelperNodeGen.create();
        }

        protected abstract void executeExtract(PointersObject receiver, long combinationRule, Object sourceBits, Object sourceWidth, Object sourceHeight, Object destBits, Object destWidth,
                        Object destHeight);

        @Specialization(guards = {"sourceBits.isIntType()", "destBits.isIntType()"})
        protected final void doExtractSourceAndDest(final PointersObject receiver, final long combinationRule,
                        final NativeObject sourceBits, final long sourceWidth, final long sourceHeight,
                        final NativeObject destBits, final long destWidth, final long destHeight) {
            final long sourceX = receiver.at0(BIT_BLT.SOURCE_X) != receiver.image.nil ? (long) receiver.at0(BIT_BLT.SOURCE_X) : 0;
            final long sourceY = receiver.at0(BIT_BLT.SOURCE_Y) != receiver.image.nil ? (long) receiver.at0(BIT_BLT.SOURCE_Y) : 0;
            loadBitBltFromWarping(receiver, combinationRule, sourceBits.getIntStorage(), sourceX, sourceY, sourceWidth, sourceHeight, destBits, destWidth, destHeight);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"destBits.isIntType()"})
        protected final void doExtractDest(final PointersObject receiver, final long combinationRule,
                        final NilObject sourceBits, final NilObject sourceWidth, final NilObject sourceHeight,
                        final NativeObject destBits, final long destWidth, final long destHeight) {
            loadBitBltFromWarping(receiver, combinationRule, null, 0, 0, 0, 0, destBits, destWidth, destHeight);
        }

        private void loadBitBltFromWarping(final PointersObject receiver, final long combinationRule, final int[] sourceWords, final long sourceX, final long sourceY, final long sourceWidth,
                        final long sourceHeight,
                        final NativeObject destBits, final long destWidth,
                        final long destHeight) {
            final long areaWidth = fetchIntOrFloatOfObjectIfNil(receiver, BIT_BLT.WIDTH, destWidth);
            final long areaHeight = fetchIntOrFloatOfObjectIfNil(receiver, BIT_BLT.HEIGHT, destHeight);
            final int[] destWords = destBits.getIntStorage();
            final long destX = fetchIntOrFloatOfObjectIfNil(receiver, BIT_BLT.DEST_X, 0);
            final long destY = fetchIntOrFloatOfObjectIfNil(receiver, BIT_BLT.DEST_Y, 0);
            long clipX = fetchIntOrFloatOfObjectIfNil(receiver, BIT_BLT.CLIP_X, 0);
            long clipY = fetchIntOrFloatOfObjectIfNil(receiver, BIT_BLT.CLIP_Y, 0);
            long clipWidth = (long) receiver.at0(BIT_BLT.CLIP_WIDTH);
            long clipHeight = (long) receiver.at0(BIT_BLT.CLIP_HEIGHT);
            if (clipX < 0) {
                clipWidth = clipWidth + clipX;
                clipX = 0;
            }
            if (clipY < 0) {
                clipWidth = clipHeight + clipY;
                clipY = 0;
            }
            if (clipX + clipWidth > destWidth) {
                clipWidth = destWidth - clipX;
            }
            if (clipY + clipHeight > destHeight) {
                clipHeight = destHeight - clipY;
            }
            clipNode.executeClip(receiver, combinationRule, areaWidth, areaHeight, sourceWords, sourceX, sourceY, sourceWidth, sourceHeight, destWords, destX, destY, destWidth, destHeight,
                            clipX, clipY, clipWidth, clipHeight);
        }

        private static long fetchIntOrFloatOfObjectIfNil(final PointersObject object, final int index, final long fallback) {
            final Object value = object.at0(index);
            if (value instanceof Long) {
                return (long) value;
            } else if (value instanceof Double) {
                return (long) (double) value;
            } else if (value == object.image.nil) {
                return fallback;
            } else {
                throw new SqueakException("Unexpected value:", object);
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!sourceBits.isIntType() || !destBits.isIntType()"})
        protected static final void doPrimitiveFail(final PointersObject receiver, final long combinationRule,
                        final NativeObject sourceBits, final Object sourceWidth, final Object sourceHeight,
                        final NativeObject destBits, final long destWidth, final long destHeight) {
            /*
             * At least one form needs to be unhibernated by Smalltalk fallback code --> primitive
             * fail required here.
             */
            throw new PrimitiveFailed();
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!destBits.isIntType()"})
        protected static final void doPrimitiveFail(final PointersObject receiver, final long combinationRule,
                        final NilObject sourceBits, final Object sourceWidth, final Object sourceHeight,
                        final NativeObject destBits, final Object destWidth, final Object destHeight) {
            /*
             * destForm needs to be unhibernated by Smalltalk fallback code --> primitive fail
             * required here.
             */
            throw new PrimitiveFailed();
        }

        @Fallback
        protected static final void doFail(final PointersObject receiver, final long combinationRule,
                        final Object sourceBits, final Object sourceWidth, final Object sourceHeight,
                        final Object destBits, final Object destWidth, final Object destHeight) {
            // This could probably just throw a PrimitiveFailed.
            throw new SqueakException("Unexpected values:", receiver, combinationRule, sourceBits, sourceWidth, sourceHeight, destBits, destWidth, destHeight);
        }
    }

    protected abstract static class CopyBitsClipHelperNode extends Node {
        @Child private CopyBitsExecuteHelperNode executeNode = CopyBitsExecuteHelperNode.create();

        protected static CopyBitsClipHelperNode create() {
            return CopyBitsClipHelperNodeGen.create();
        }

        protected abstract void executeClip(PointersObject receiver, long combinationRule, long areaWidth, long areaHeight,
                        int[] sourceWords, long sourceX, long sourceY, long sourceWidth, long sourceHeight,
                        int[] destWords, long destX, long destY, long destWidth, long destHeight,
                        long clipX, long clipY, long clipWidth, long clipHeight);

        @Specialization(guards = {"sourceWords == null", "hasEnoughWords(destWords, destWidth, destHeight)"})
        protected final void executeClipWithoutSourceForm(final PointersObject receiver, final long combinationRule, final long areaWidth, final long areaHeight,
                        final int[] sourceWords, final long sourceX, final long sourceY, final long sourceWidth, @SuppressWarnings("unused") final long sourceHeight,
                        final int[] destWords, final long destX, final long destY, final long destWidth, @SuppressWarnings("unused") final long destHeight,
                        final long clipX, final long clipY, final long clipWidth, final long clipHeight) {
            // adapted copy of BilBltSimulation>>clipRange for the nil sourceForm case
            final long dx;
            final long dy;
            long bbW;
            long bbH;
            if (destX >= clipX) {
                dx = destX;
                bbW = areaWidth;
            } else {
                bbW = areaWidth - (clipX - destX);
                dx = clipX;
            }
            if ((dx + bbW) > (clipX + clipWidth)) {
                bbW = bbW - ((dx + bbW) - (clipX + clipWidth));
            }
            // then in y
            if (destY >= clipY) {
                dy = destY;
                bbH = areaHeight;
            } else {
                bbH = areaHeight - (clipY - destY);
                dy = clipY;
            }
            if ((dy + bbH) > (clipY + clipHeight)) {
                bbH = bbH - ((dy + bbH) - (clipY + clipHeight));
            }
            executeNode.executeCopyBits(receiver, combinationRule, sourceWords, sourceX, sourceY, sourceWidth, destWords, dx, dy, destWidth, bbW, bbH);
        }

        @Specialization(guards = {"sourceWords != null", "hasEnoughWords(sourceWords, sourceWidth, sourceHeight)", "hasEnoughWords(destWords, destWidth, destHeight)"})
        protected final void executeClipWithSourceForm(final PointersObject receiver, final long combinationRule, final long areaWidth, final long areaHeight,
                        final int[] sourceWords, final long sourceX, final long sourceY, final long sourceWidth, final long sourceHeight,
                        final int[] destWords, final long destX, final long destY, final long destWidth, @SuppressWarnings("unused") final long destHeight,
                        final long clipX, final long clipY, final long clipWidth, final long clipHeight) {
            long sx;
            long sy;
            long dx;
            long dy;
            long bbW;
            long bbH;
            if (destX >= clipX) {
                sx = sourceX;
                dx = destX;
                bbW = areaWidth;
            } else {
                sx = sourceX + (clipX - destX);
                bbW = areaWidth - (clipX - destX);
                dx = clipX;
            }
            if ((dx + bbW) > (clipX + clipWidth)) {
                bbW = bbW - ((dx + bbW) - (clipX + clipWidth));
            }
            // then in y
            if (destY >= clipY) {
                sy = sourceY;
                dy = destY;
                bbH = areaHeight;
            } else {
                sy = sourceY + clipY - destY;
                bbH = areaHeight - (clipY - destY);
                dy = clipY;
            }
            if ((dy + bbH) > (clipY + clipHeight)) {
                bbH = bbH - ((dy + bbH) - (clipY + clipHeight));
            }
            if (sx < 0) {
                dx = dx - sx;
                bbW = bbW + sx;
                sx = 0;
            }
            if (sx + bbW > sourceWidth) {
                bbW = bbW - (sx + bbW - sourceWidth);
            }
            if (sy < 0) {
                dy = dy - sy;
                bbH = bbH + sy;
                sy = 0;
            }
            if (sy + bbH > sourceHeight) {
                bbH = bbH - (sy + bbH - sourceHeight);
            }
            executeNode.executeCopyBits(receiver, combinationRule, sourceWords, sx, sy, sourceWidth, destWords, dx, dy, destWidth, bbW, bbH);
        }

        @SuppressWarnings("unused")
        @Fallback
        protected static final void doFail(final PointersObject receiver, final long combinationRule, final long areaWidth, final long areaHeight,
                        final int[] sourceWords, final long sourceX, final long sourceY, final long sourceWidth, final long sourceHeight,
                        final int[] destWords, final long destX, final long destY, final long destWidth, final long destHeight,
                        final long clipX, final long clipY, final long clipWidth, final long clipHeight) {
            receiver.image.printToStdErr("not enough words!");
            throw new PrimitiveFailed(); // not enough words!
        }

        protected static final boolean hasEnoughWords(final int[] words, final long width, final long height) {
            return words.length >= width * height;
        }
    }

    protected abstract static class CopyBitsExecuteHelperNode extends Node {
        private final BranchProfile errorProfile = BranchProfile.create();

        protected static CopyBitsExecuteHelperNode create() {
            return CopyBitsExecuteHelperNodeGen.create();
        }

        protected abstract void executeCopyBits(PointersObject receiver, long combinationRule,
                        int[] sourceForm, Object sourceX, Object sourceY, Object sourceWidth,
                        int[] destWords, long destX, long destY, long destWidth,
                        long areaWidth, long areaHeight);

        protected static boolean invalidArea(final long areaWidth, final long areaHeight) {
            return areaWidth <= 0 || areaHeight <= 0;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"invalidArea(areaWidth, areaHeight)"})
        protected static final void executeInvalidArea(final PointersObject receiver, final long combinationRule,
                        final int[] sourceWords, final Object sourceX, final Object sourceY, final Object sourceWidth,
                        final int[] destWords, final long destX, final long destY, final long destWidth,
                        final long areaWidth, final long areaHeight) {
            // do nothing, just return receiver
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"combinationRule == 3", "sourceWords == null", "!invalidArea(areaWidth, areaHeight)"})
        protected final void doCopyBitsCombiRule3NilSourceForm(final PointersObject receiver, final long combinationRule,
                        final int[] sourceWords, final Object sourceX, final Object sourceY, final Object sourceWidth,
                        final int[] destWords, final long destX, final long destY, final long destWidth,
                        final long areaWidth, final long areaHeight) {
            final int fillValue = extractFillValueFromHalftoneForm(receiver);
            final int innerIterations = (int) areaWidth;
            for (long y = destY; y < destY + areaHeight; y++) {
                final long destStart = y * destWidth + destX;
                try {
                    for (int dx = (int) destStart; dx < destStart + areaWidth; dx++) {
                        destWords[dx] = fillValue;
                    }
                } finally {
                    LoopNode.reportLoopCount(this, innerIterations);
                }
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"combinationRule == 4", "sourceWords == null", "!invalidArea(areaWidth, areaHeight)"})
        protected final void doCopyBitsCombiRule4NilSourceForm(final PointersObject receiver, final long combinationRule,
                        final int[] sourceWords, final Object sourceX, final Object sourceY, final Object sourceWidth,
                        final int[] destWords, final long destX, final long destY, final long destWidth,
                        final long areaWidth, final long areaHeight) {
            final int fillValue = extractFillValueFromHalftoneForm(receiver);
            final long endX = destX + areaWidth;
            final long endY = destY + areaHeight;
            if (destWords.length - 1 < (endY - 1) * destWidth + (endX - 1)) {
                errorProfile.enter();
                throw new PrimitiveFailed(); // fail early in case of index out of bounds
            }
            final int invertedFillValue = ~fillValue;
            final int innerIterations = (int) areaWidth;
            for (long dy = destY; dy < destY + areaHeight; dy++) {
                final long destStart = dy * destWidth + destX;
                try {
                    for (long dx = destStart; dx < destStart + areaWidth; dx++) {
                        destWords[(int) dx] = invertedFillValue & destWords[(int) dx];
                    }
                } finally {
                    LoopNode.reportLoopCount(this, innerIterations);
                }
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"combinationRule == 24", "sourceWords == null", "!invalidArea(areaWidth, areaHeight)"})
        protected final void doCopyBitsCombiRule24NilSourceForm(final PointersObject receiver, final long combinationRule,
                        final int[] sourceWords, final Object sourceX, final Object sourceY, final Object sourceWidth,
                        final int[] destWords, final long destX, final long destY, final long destWidth,
                        final long areaWidth, final long areaHeight) {
            final long fillValue = Integer.toUnsignedLong(extractFillValueFromHalftoneForm(receiver));
            final long endX = destX + areaWidth;
            final long endY = destY + areaHeight;
            final int innerIterations = (int) (endX - destX);
            for (long y = destY; y < endY; y++) {
                try {
                    for (long x = destX; x < endX; x++) {
                        final int index = (int) (y * destWidth + x);
                        destWords[index] = (int) BitBltPluginHelpers.alphaBlend24(fillValue, Integer.toUnsignedLong(destWords[index]));
                    }
                } finally {
                    LoopNode.reportLoopCount(this, innerIterations);
                }
            }
        }

        @Specialization(guards = {"combinationRule == 24", "sourceWords != null", "!invalidArea(areaWidth, areaHeight)"})
        protected final void doCopyBitsCombiRule24WithSourceForm(@SuppressWarnings("unused") final PointersObject receiver, @SuppressWarnings("unused") final long combinationRule,
                        final int[] sourceWords, final long sourceX, final long sourceY, final long sourceWidth,
                        final int[] destWords, final long destX, final long destY, final long destWidth,
                        final long areaWidth, final long areaHeight) {
            doSimpleLoop(BitBltPluginHelpers::alphaBlend24, sourceWords, sourceX, sourceY, sourceWidth, destWords, destX, destY, destWidth, areaWidth, areaHeight);
        }

        @Specialization(guards = {"combinationRule == 25", "sourceWords != null", "!invalidArea(areaWidth, areaHeight)"})
        protected final void doCopyBitsCombiRule25WithSourceForm(@SuppressWarnings("unused") final PointersObject receiver, @SuppressWarnings("unused") final long combinationRule,
                        final int[] sourceWords, final long sourceX, final long sourceY, final long sourceWidth,
                        final int[] destWords, final long destX, final long destY, final long destWidth,
                        final long areaWidth, final long areaHeight) {
            doSimpleLoop(BitBltPluginHelpers::pixPaint25, sourceWords, sourceX, sourceY, sourceWidth, destWords, destX, destY, destWidth, areaWidth, areaHeight);
        }

        @Specialization(guards = {"combinationRule == 26", "sourceWords != null", "!invalidArea(areaWidth, areaHeight)"})
        protected final void doCopyBitsCombiRule26WithSourceForm(@SuppressWarnings("unused") final PointersObject receiver, @SuppressWarnings("unused") final long combinationRule,
                        final int[] sourceWords, final long sourceX, final long sourceY, final long sourceWidth,
                        final int[] destWords, final long destX, final long destY, final long destWidth,
                        final long areaWidth, final long areaHeight) {
            doSimpleLoop(BitBltPluginHelpers::pixMask26, sourceWords, sourceX, sourceY, sourceWidth, destWords, destX, destY, destWidth, areaWidth, areaHeight);
        }

        private void doSimpleLoop(final LongBinaryOperator op, final int[] sourceWords, final long sourceX, final long sourceY, final long sourceWidth,
                        final int[] destWords, final long destX, final long destY, final long destWidth,
                        final long areaWidth, final long areaHeight) {
            for (int y = 0; y < areaHeight; y++) {
                final int sourceStart = (int) ((sourceY + y) * sourceWidth + sourceX);
                final int destStart = (int) ((destY + y) * destWidth + destX);
                try {
                    for (int x = 0; x < areaWidth; x++) {
                        destWords[destStart + x] = (int) op.applyAsLong(Integer.toUnsignedLong(sourceWords[sourceStart + x]), Integer.toUnsignedLong(destWords[destStart + x]));
                    }
                } finally {
                    LoopNode.reportLoopCount(this, (int) areaWidth);
                }
            }
        }

        @Fallback
        protected static final void doFail(final PointersObject receiver, final long combinationRule,
                        final int[] sourceForm, final Object sourceX, final Object sourceY, final Object sourceWidth,
                        final int[] destWords, final long destX, final long destY, final long destWidth,
                        final long areaWidth, final long areaHeight) {
            throw new SqueakException("Unsupported operation reached:", receiver, combinationRule,
                            sourceForm, sourceX, sourceY, sourceWidth, destWords, destX, destY, destWidth, areaWidth, areaHeight);
        }
    }

    private static int extractFillValueFromHalftoneForm(final PointersObject receiver) {
        final NativeObject halftoneForm = (NativeObject) receiver.at0(BIT_BLT.HALFTONE_FORM);
        final int[] fillArray = halftoneForm.getIntStorage();
        if (fillArray.length != 1) {
            throw new SqueakException("Expected exactly one fillValue.");
        }
        return fillArray[0];
    }
}
