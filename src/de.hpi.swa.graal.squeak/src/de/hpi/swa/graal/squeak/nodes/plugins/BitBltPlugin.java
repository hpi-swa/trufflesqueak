package de.hpi.swa.graal.squeak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NotProvided;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.BIT_BLT;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.FORM;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.graal.squeak.nodes.plugins.BitBltPluginFactory.CopyBitsClipHelperNodeGen;
import de.hpi.swa.graal.squeak.nodes.plugins.BitBltPluginFactory.CopyBitsExecuteHelperNodeGen;
import de.hpi.swa.graal.squeak.nodes.plugins.BitBltPluginFactory.CopyBitsExtractHelperNodeGen;
import de.hpi.swa.graal.squeak.nodes.plugins.BitBltPluginFactory.PixelValueAtExecuteHelperNodeGen;
import de.hpi.swa.graal.squeak.nodes.plugins.BitBltPluginFactory.PixelValueAtExtractHelperNodeGen;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.MiscellaneousPrimitives.SimulationPrimitiveNode;

public final class BitBltPlugin extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return BitBltPluginFactory.getFactories();
    }

    @Override
    public boolean useSimulationAsFallback() {
        return true;
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveCopyBits")
    protected abstract static class PrimCopyBitsNode extends AbstractPrimitiveNode {

        @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();
        @Child private SimulationPrimitiveNode simulateNode;
        @Child private CopyBitsExtractHelperNode extractNode;

        protected PrimCopyBitsNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
            simulateNode = SimulationPrimitiveNode.create(method, getClass().getSimpleName(), "primitiveCopyBits");
            extractNode = CopyBitsExtractHelperNode.create();
        }

        protected boolean supportedCombinationRule(final PointersObject receiver) {
            final long combinationRule = (long) receiver.at0(BIT_BLT.COMBINATION_RULE);
            if (combinationRule == 24) {
                return true;
            }
            final Object sourceForm = receiver.at0(BIT_BLT.SOURCE_FORM);
            final boolean hasSourceForm = sourceForm != receiver.image.nil;
            if (combinationRule == 4 && !hasSourceForm) {
                return true;
            }
            // (See TODO below)
            // final boolean noOverlap = !hasSourceForm || !(receiver.at0(BIT_BLT.DEST_FORM) ==
            // sourceForm);
            // return noOverlap && combinationRule == 3 && !hasSourceForm;
            return false;
        }

        protected boolean supportedDepth(final PointersObject receiver) {
            return hasSourceFormDepth(receiver, 32) && hasDestFormDepth(receiver, 32);
        }

        @Specialization(guards = {"supportedCombinationRule(receiver)", "supportedDepth(receiver)"})
        protected final Object doOptimized(final VirtualFrame frame, final PointersObject receiver) {
            try {
                extractNode.executeExtract(receiver);
            } catch (UnsupportedSpecializationException e) {
                code.image.printToStdErr(e);
                return doSimulation(frame, receiver);
            }
            return receiver;
        }

        @Fallback
        protected final Object doSimulation(final VirtualFrame frame, final Object receiver) {
            return simulateNode.executeWithArguments(frame, receiver, NotProvided.INSTANCE, NotProvided.INSTANCE, NotProvided.INSTANCE, NotProvided.INSTANCE, NotProvided.INSTANCE,
                            NotProvided.INSTANCE, NotProvided.INSTANCE, NotProvided.INSTANCE);
        }

        /*
         * Guard Helpers
         */

        protected final boolean hasDestFormDepth(final PointersObject target, final int depth) {
            return depth == (long) at0Node.execute(target.at0(BIT_BLT.DEST_FORM), FORM.DEPTH);
        }

        protected final boolean hasSourceFormDepth(final PointersObject target, final int depth) {
            final Object sourceForm = target.at0(BIT_BLT.SOURCE_FORM);
            return sourceForm == target.image.nil || depth == (long) at0Node.execute(sourceForm, FORM.DEPTH);
        }

        protected final boolean hasNilSourceForm(final PointersObject target) {
            return target.at0(BIT_BLT.SOURCE_FORM) == code.image.nil;
        }

        protected final boolean hasNilHalftoneForm(final PointersObject target) {
            return target.at0(BIT_BLT.HALFTONE_FORM) == code.image.nil;
        }

        protected final boolean hasNilColormap(final PointersObject target) {
            return target.at0(BIT_BLT.COLOR_MAP) == code.image.nil;
        }
    }

    @ImportStatic(FORM.class)
    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitivePixelValueAt")
    protected abstract static class PrimPixelValueAtNode extends AbstractPrimitiveNode {
        @Child private PixelValueAtExtractHelperNode handleNode = PixelValueAtExtractHelperNode.create();

        public PrimPixelValueAtNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"xValue < 0 || yValue < 0"})
        protected static final long doQuickReturn(final PointersObject receiver, final long xValue, final long yValue) {
            return 0L;
        }

        @Specialization(guards = {"xValue >= 0", "yValue >= 0", "receiver.size() > OFFSET"})
        protected final long doValueAt(final PointersObject receiver, final long xValue, final long yValue) {
            return handleNode.executeValueAt(receiver, xValue, yValue, receiver.at0(FORM.BITS));
        }
    }

    /*
     * Helper Nodes
     */
    protected abstract static class CopyBitsExtractHelperNode extends Node {
        @Child private CopyBitsClipHelperNode clipNode = CopyBitsClipHelperNode.create();

        protected static CopyBitsExtractHelperNode create() {
            return CopyBitsExtractHelperNodeGen.create();
        }

        protected abstract void executeExtract(PointersObject receiver);

        @Specialization(guards = {"hasSourceForm(receiver)"})
        protected final void executeWithSourceForm(final PointersObject receiver) {
            final PointersObject sourceForm = (PointersObject) receiver.at0(BIT_BLT.SOURCE_FORM);
            final long sourceWidth = (long) sourceForm.at0(FORM.WIDTH);
            final long sourceHeight = (long) sourceForm.at0(FORM.HEIGHT);

            final PointersObject destForm = (PointersObject) receiver.at0(BIT_BLT.DEST_FORM);
            final long destWidth = (long) destForm.at0(FORM.WIDTH);
            final long destHeight = (long) destForm.at0(FORM.HEIGHT);

            final long combinationRule = (long) receiver.at0(BIT_BLT.COMBINATION_RULE);

            final NativeObject destBits = (NativeObject) destForm.at0(FORM.BITS);
            final int[] destWords = destBits.getIntStorage();
            if (!destBits.isIntType() || destWidth * destHeight > destWords.length) {
                throw new PrimitiveFailed();
            }

            final NativeObject sourceBits = (NativeObject) sourceForm.at0(FORM.BITS);
            if (!sourceBits.isIntType() || sourceWidth * sourceHeight > sourceBits.getIntStorage().length) {
                throw new PrimitiveFailed();
            }

            final Object destX = receiver.at0(BIT_BLT.DEST_X);
            final Object destY = receiver.at0(BIT_BLT.DEST_Y);
            final Object sourceX = receiver.at0(BIT_BLT.SOURCE_X);
            final Object sourceY = receiver.at0(BIT_BLT.SOURCE_Y);
            final Object areaWidth = receiver.at0(BIT_BLT.WIDTH);
            final Object areaHeight = receiver.at0(BIT_BLT.HEIGHT);

            final Object clipX = receiver.at0(BIT_BLT.CLIP_X);
            final Object clipY = receiver.at0(BIT_BLT.CLIP_Y);
            final Object clipWidth = receiver.at0(BIT_BLT.CLIP_WIDTH);
            final Object clipHeight = receiver.at0(BIT_BLT.CLIP_HEIGHT);

            clipNode.executeClip(receiver, combinationRule, areaWidth, areaHeight, sourceForm, sourceX, sourceY, sourceWidth, sourceHeight, destWords, destX, destY, destWidth, clipX, clipY,
                            clipWidth,
                            clipHeight);
        }

        @Fallback
        protected final void executeWithoutSourceForm(final PointersObject receiver) {
            final PointersObject destForm = (PointersObject) receiver.at0(BIT_BLT.DEST_FORM);
            final long destWidth = (long) destForm.at0(FORM.WIDTH);
            final long destHeight = (long) destForm.at0(FORM.HEIGHT);

            final long combinationRule = (long) receiver.at0(BIT_BLT.COMBINATION_RULE);

            final NativeObject destBits = (NativeObject) destForm.at0(FORM.BITS);
            final int[] destWords = destBits.getIntStorage();
            if (!destBits.isIntType() || destWidth * destHeight > destWords.length) {
                throw new PrimitiveFailed();
            }

            final Object destX = receiver.at0(BIT_BLT.DEST_X);
            final Object destY = receiver.at0(BIT_BLT.DEST_Y);
            final Object areaWidth = receiver.at0(BIT_BLT.WIDTH);
            final Object areaHeight = receiver.at0(BIT_BLT.HEIGHT);

            final Object clipX = receiver.at0(BIT_BLT.CLIP_X);
            final Object clipY = receiver.at0(BIT_BLT.CLIP_Y);
            final Object clipWidth = receiver.at0(BIT_BLT.CLIP_WIDTH);
            final Object clipHeight = receiver.at0(BIT_BLT.CLIP_HEIGHT);

            clipNode.executeClip(receiver, combinationRule, areaWidth, areaHeight, null, 0L, 0L, 0L, 0L, destWords, destX, destY, destWidth, clipX, clipY,
                            clipWidth,
                            clipHeight);
        }

        protected static final boolean hasSourceForm(final PointersObject target) {
            return target.at0(BIT_BLT.SOURCE_FORM) != target.image.nil;
        }
    }

    protected abstract static class CopyBitsClipHelperNode extends Node {
        @Child private CopyBitsExecuteHelperNode executeNode = CopyBitsExecuteHelperNode.create();

        protected static CopyBitsClipHelperNode create() {
            return CopyBitsClipHelperNodeGen.create();
        }

        protected abstract void executeClip(PointersObject receiver,
                        long combinationRule,
                        Object areaWidth,
                        Object areaHeight,
                        PointersObject sourceForm,
                        Object sourceX,
                        Object sourceY,
                        Object sourceWidth,
                        Object sourceHeight,
                        int[] destWords,
                        Object destX,
                        Object destY,
                        Object destWidth,
                        Object clipX,
                        Object clipY,
                        Object clipWidth,
                        Object clipHeight);

        @Specialization(guards = {"sourceForm == null"})
        protected final void executeClipWithoutSourceForm(final PointersObject receiver,
                        final long combinationRule,
                        final long areaWidth,
                        final long areaHeight,
                        final PointersObject sourceForm,
                        final long sourceX,
                        final long sourceY,
                        final long sourceWidth,
                        @SuppressWarnings("unused") final long sourceHeight,
                        final int[] destWords,
                        final long destX,
                        final long destY,
                        final long destWidth,
                        final long clipX,
                        final long clipY,
                        final long clipWidth,
                        final long clipHeight) {
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

            executeNode.executeCopyBits(receiver, combinationRule, sourceForm, sourceX, sourceY, sourceWidth, destWords, dx, dy, destWidth, bbW, bbH);
        }

        @Specialization()
        protected final void executeClipWithSourceForm(final PointersObject receiver,
                        final long combinationRule,
                        final long areaWidth,
                        final long areaHeight,
                        final PointersObject sourceForm,
                        final long sourceX,
                        final long sourceY,
                        final long sourceWidth,
                        final long sourceHeight,
                        final int[] destWords,
                        final long destX,
                        final long destY,
                        final long destWidth,
                        final long clipX,
                        final long clipY,
                        final long clipWidth,
                        final long clipHeight) {
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

            executeNode.executeCopyBits(receiver, combinationRule, sourceForm, sx, sy, sourceWidth, destWords, dx, dy, destWidth, bbW, bbH);
        }
    }

    protected abstract static class CopyBitsExecuteHelperNode extends Node {
        protected static CopyBitsExecuteHelperNode create() {
            return CopyBitsExecuteHelperNodeGen.create();
        }

        protected abstract void executeCopyBits(PointersObject receiver,
                        long combinationRule,
                        PointersObject sourceForm,
                        long sourceX,
                        long sourceY,
                        long sourceWidth,
                        int[] destWords,
                        long destX,
                        long destY,
                        long destWidth,
                        long areaWidth,
                        long areaHeight);

        protected static boolean invalidArea(final long areaWidth, final long areaHeight) {
            return areaWidth <= 0 || areaHeight <= 0;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"invalidArea(areaWidth, areaHeight)"})
        protected static final void executeInvalidArea(final PointersObject receiver,
                        final long combinationRule,
                        final PointersObject sourceForm,
                        final long sourceX,
                        final long sourceY,
                        final long sourceWidth,
                        final int[] destWords,
                        final long destX,
                        final long destY,
                        final long destWidth,
                        final long areaWidth,
                        final long areaHeight) {
            // do nothing, just return receiver
        }

        // FIXME: this method is currently not used, as it caused major artifacts.
        // To re-enable this rule, simply go all the way up to "supportedCombinationRules" and
        // uncomment the code marked with a TODO.
        @SuppressWarnings("unused")
        @Specialization(guards = {"combinationRule == 3", "sourceForm == null", "!invalidArea(areaWidth, areaHeight)"})
        protected final void doCopyBitsCombiRule3NilSourceForm(final PointersObject receiver,
                        final long combinationRule,
                        final PointersObject sourceForm,
                        final long sourceX,
                        final long sourceY,
                        final long sourceWidth,
                        final int[] destWords,
                        final long destX,
                        final long destY,
                        final long destWidth,
                        final long areaWidth,
                        final long areaHeight) {
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
        @Specialization(guards = {"combinationRule == 4", "sourceForm == null", "!invalidArea(areaWidth, areaHeight)"})
        protected final void doCopyBitsCombiRule4NilSourceForm(final PointersObject receiver,
                        final long combinationRule,
                        final PointersObject sourceForm,
                        final long sourceX,
                        final long sourceY,
                        final long sourceWidth,
                        final int[] destWords,
                        final long destX,
                        final long destY,
                        final long destWidth,
                        final long areaWidth,
                        final long areaHeight) {
            final int fillValue = extractFillValueFromHalftoneForm(receiver);

            final long endX = destX + areaWidth;
            final long endY = destY + areaHeight;

            if (destWords.length - 1 < (endY - 1) * destWidth + (endX - 1)) {
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
        @Specialization(guards = {"combinationRule == 24", "sourceForm == null", "!invalidArea(areaWidth, areaHeight)"})
        protected final void doCopyBitsCombiRule24NilSourceForm(final PointersObject receiver,
                        final long combinationRule,
                        final PointersObject sourceForm,
                        final long sourceX,
                        final long sourceY,
                        final long sourceWidth,
                        final int[] destWords,
                        final long destX,
                        final long destY,
                        final long destWidth,
                        final long areaWidth,
                        final long areaHeight) {
            final long fillValue = Integer.toUnsignedLong(extractFillValueFromHalftoneForm(receiver));
            final long endX = destX + areaWidth;
            final long endY = destY + areaHeight;
            final int innerIterations = (int) (endX - destX);
            for (long y = destY; y < endY; y++) {
                try {
                    for (long x = destX; x < endX; x++) {
                        final int index = (int) (y * destWidth + x);
                        destWords[index] = (int) alphaBlend24(fillValue, Integer.toUnsignedLong(destWords[index]));
                    }
                } finally {
                    LoopNode.reportLoopCount(this, innerIterations);
                }
            }
        }

        @Specialization(guards = {"combinationRule == 24", "sourceForm != null", "!invalidArea(areaWidth, areaHeight)"})
        protected final void doCopyBitsCombiRule24WithSourceForm(@SuppressWarnings("unused") final PointersObject receiver,
                        @SuppressWarnings("unused") final long combinationRule,
                        final PointersObject sourceForm,
                        final long sourceX,
                        final long sourceY,
                        final long sourceWidth,
                        final int[] destWords,
                        final long destX,
                        final long destY,
                        final long destWidth,
                        final long areaWidth,
                        final long areaHeight) {
            final int[] sourceInts = ((NativeObject) sourceForm.at0(FORM.BITS)).getIntStorage();
            final int innerIterations = (int) areaWidth;
            for (long dy = destY, sy = sourceY; dy < destY + areaHeight; dy++, sy++) {
                final long sourceStart = sy * sourceWidth + sourceX;
                final long destStart = dy * destWidth + destX;
                try {
                    for (int dx = (int) destStart, sx = (int) sourceStart; dx < destStart + areaWidth; dx++, sx++) {
                        destWords[dx] = (int) alphaBlend24(Integer.toUnsignedLong(sourceInts[sx]), Integer.toUnsignedLong(destWords[dx]));
                    }
                } finally {
                    LoopNode.reportLoopCount(this, innerIterations);
                }
            }
        }
    }

    protected abstract static class PixelValueAtExtractHelperNode extends Node {
        @Child private PixelValueAtExecuteHelperNode executeNode = PixelValueAtExecuteHelperNode.create();

        private static PixelValueAtExtractHelperNode create() {
            return PixelValueAtExtractHelperNodeGen.create();
        }

        protected abstract long executeValueAt(PointersObject receiver, long xValue, long yValue, Object bitmap);

        @Specialization(guards = "bitmap.isIntType()")
        protected final long doInts(final PointersObject receiver, final long xValue, final long yValue, final NativeObject bitmap) {
            final Object width = receiver.at0(FORM.WIDTH);
            final Object height = receiver.at0(FORM.HEIGHT);
            final Object depth = receiver.at0(FORM.DEPTH);
            return executeNode.executeValueAt(receiver, xValue, yValue, bitmap, width, height, depth);
        }
    }

    protected abstract static class PixelValueAtExecuteHelperNode extends Node {
        private final BranchProfile errorProfile = BranchProfile.create();

        private static PixelValueAtExecuteHelperNode create() {
            return PixelValueAtExecuteHelperNodeGen.create();
        }

        protected abstract long executeValueAt(PointersObject receiver, long xValue, long yValue, NativeObject bitmap, Object width, Object height, Object depth);

        @SuppressWarnings("unused")
        @Specialization(guards = "xValue >= width || yValue >= height")
        protected static final long doQuickReturn(final PointersObject receiver, final long xValue, final long yValue, final NativeObject bitmap, final long width, final long height,
                        final long depth) {
            return 0L;
        }

        @Specialization(guards = "bitmap.isIntType()")
        protected final long doInts(@SuppressWarnings("unused") final PointersObject receiver, final long xValue, final long yValue, final NativeObject bitmap, final long width,
                        final long height, final long depth) {
            final long ppW = Math.floorDiv(32, depth);
            final long stride = Math.floorDiv(width + ppW - 1, ppW);
            final int[] ints = bitmap.getIntStorage();
            if (ints.length > stride * height) {
                errorProfile.enter();
                throw new PrimitiveFailed();
            }
            final int index = (int) ((yValue * stride) + Math.floorDiv(xValue, ppW));
            final int word = ints[index];
            final long mask = 0xFFFFFFFFL >> (32 - depth);
            final long shift = 32 - (((xValue & (ppW - 1)) + 1) * depth);
            return ((word >> shift) & mask) & 0xffffffffL;
        }
    }

    /*
     * Primitive Helper Functions
     */

    protected static long alphaBlend24(final long sourceWord, final long destinationWord) {
        final long alpha = sourceWord >> 24;
        if (alpha == 0) {
            return destinationWord;
        }
        if (alpha == 255) {
            return sourceWord;
        }

        final long unAlpha = 255 - alpha;

        // blend red and blue
        long blendRB = ((sourceWord & 0xFF00FF) * alpha) +
                        ((destinationWord & 0xFF00FF) * unAlpha) + 0xFF00FF;

        // blend alpha and green
        long blendAG = ((((sourceWord >> 8) | 0xFF0000) & 0xFF00FF) * alpha) +
                        (((destinationWord >> 8) & 0xFF00FF) * unAlpha) + 0xFF00FF;

        blendRB = (blendRB + (((blendRB - 0x10001) >> 8) & 0xFF00FF) >> 8) & 0xFF00FF;
        blendAG = (blendAG + (((blendAG - 0x10001) >> 8) & 0xFF00FF) >> 8) & 0xFF00FF;
        return blendRB | (blendAG << 8);
    }

    protected static int extractFillValueFromHalftoneForm(final PointersObject receiver) {
        final NativeObject halftoneForm = (NativeObject) receiver.at0(BIT_BLT.HALFTONE_FORM);
        final int[] fillArray = halftoneForm.getIntStorage();
        if (fillArray.length != 1) {
            throw new SqueakException("Expected exactly one fillValue.");
        }
        return fillArray[0];
    }
}
