package de.hpi.swa.graal.squeak.nodes.plugins;

import java.util.Arrays;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ValueProfile;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NotProvided;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.BIT_BLT;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.FORM;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectSizeNode;
import de.hpi.swa.graal.squeak.nodes.plugins.BitBltPluginFactory.PixelValueAtExtractHelperNodeGen;
import de.hpi.swa.graal.squeak.nodes.plugins.BitBltPluginFactory.CopyBitsClipHelperNodeGen;
import de.hpi.swa.graal.squeak.nodes.plugins.BitBltPluginFactory.CopyBitsExecuteHelperNodeGen;
import de.hpi.swa.graal.squeak.nodes.plugins.BitBltPluginFactory.CopyBitsExtractHelperNodeGen;
import de.hpi.swa.graal.squeak.nodes.plugins.BitBltPluginFactory.PixelValueAtExecuteHelperNodeGen;
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
            final long combinationRule = (long) at0Node.execute(receiver, BIT_BLT.COMBINATION_RULE);
            return combinationRule == 4 || combinationRule == 24 || combinationRule == 25 || combinationRule == 3;
        }

        protected boolean supportedDepth(final PointersObject receiver) {
            return hasSourceFormDepth(receiver, 32) && hasDestFormDepth(receiver, 32);
        }

        @Specialization(guards = {"supportedCombinationRule(receiver)", "supportedDepth(receiver)"})
        protected final Object doOptimized(@SuppressWarnings("unused") final VirtualFrame frame, final PointersObject receiver) {
            return extractNode.execute(receiver);
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
            Object sourceForm = target.at0(BIT_BLT.SOURCE_FORM);
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
        @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();
        @Child protected SqueakObjectSizeNode sizeNode = SqueakObjectSizeNode.create();
        @Child private PixelValueAtExtractHelperNode handleNode = PixelValueAtExtractHelperNode.create();

        public PrimPixelValueAtNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"xValue < 0 || yValue < 0"})
        protected static final long doQuickReturn(final PointersObject receiver, final long xValue, final long yValue) {
            return 0L;
        }

        @Specialization(guards = {"xValue >= 0", "yValue > 0", "sizeNode.execute(receiver) > OFFSET"})
        protected final long doValueAt(final PointersObject receiver, final long xValue, final long yValue) {
            return handleNode.executeValueAt(receiver, xValue, yValue, at0Node.execute(receiver, FORM.BITS));
        }
    }

    /*
     * Helper Nodes
     */
    protected abstract static class CopyBitsExtractHelperNode extends Node {
        @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();
        @Child private CopyBitsClipHelperNode clipNode = CopyBitsClipHelperNode.create();

        protected static CopyBitsExtractHelperNode create() {
            return CopyBitsExtractHelperNodeGen.create();
        }

        protected abstract PointersObject execute(PointersObject receiver);

        @Specialization(guards = {"hasSourceForm(receiver)"})
        protected final PointersObject executeWithSourceForm(final PointersObject receiver) {
            final PointersObject sourceForm = (PointersObject) receiver.at0(BIT_BLT.SOURCE_FORM);
            final Object sourceWidth = sourceForm.at0(FORM.WIDTH);
            final Object sourceHeight = sourceForm.at0(FORM.HEIGHT);

            final PointersObject destForm = (PointersObject) receiver.at0(BIT_BLT.DEST_FORM);
            final long destWidth = (long) destForm.at0(FORM.WIDTH);
            final long destHeight = (long) destForm.at0(FORM.HEIGHT);

            final long combinationRule = (long) receiver.at0(BIT_BLT.COMBINATION_RULE);

            if (destWidth * destHeight > destForm.getPointers().length) {
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

            return clipNode.executeClip(receiver, combinationRule, areaWidth, areaHeight, sourceForm, sourceX, sourceY, sourceWidth, sourceHeight, destForm, destX, destY, destWidth, clipX, clipY,
                            clipWidth,
                            clipHeight);
        }

        @Specialization()
        protected final PointersObject executeWithoutSourceForm(final PointersObject receiver) {
            final PointersObject destForm = (PointersObject) receiver.at0(BIT_BLT.DEST_FORM);
            final long destWidth = (long) destForm.at0(FORM.WIDTH);
            final long destHeight = (long) destForm.at0(FORM.HEIGHT);

            final long combinationRule = (long) receiver.at0(BIT_BLT.COMBINATION_RULE);

            if (destWidth * destHeight > destForm.getPointers().length) {
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

            return clipNode.executeClip(receiver, combinationRule, areaWidth, areaHeight, null, 0, 0, 0, 0, destForm, destX, destY, destWidth, clipX, clipY,
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

        protected abstract PointersObject executeClip(final PointersObject receiver,
                        final long combinationRule,
                        final Object areaWidth,
                        final Object areaHeight,
                        final PointersObject sourceForm,
                        final Object sourceX,
                        final Object sourceY,
                        final Object sourceWidth,
                        final Object sourceHeight,
                        final PointersObject destinationForm,
                        final Object destX,
                        final Object destY,
                        final Object destWidth,
                        final Object clipX,
                        final Object clipY,
                        final Object clipWidth,
                        final Object clipHeight);

        @Specialization(guards = {"sourceForm == null", "areaWidth > 0", "areaHeight > 0"})
        protected final PointersObject executeClipWithoutSourceForm(final PointersObject receiver,
                        final long combinationRule,
                        final long areaWidth,
                        final long areaHeight,
                        final PointersObject sourceForm,
                        final long sourceX,
                        final long sourceY,
                        final long sourceWidth,
                        @SuppressWarnings("unused") final long sourceHeight,
                        final PointersObject destForm,
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

            return executeNode.executeCopyBits(receiver, combinationRule, sourceForm, sourceX, sourceY, sourceWidth, destForm, dx, dy, destWidth, bbW, bbH);
        }

        @Specialization(guards = {"areaWidth > 0", "areaHeight > 0"})
        protected final PointersObject executeClipWithSourceForm(final PointersObject receiver,
                        final long combinationRule,
                        final long areaWidth,
                        final long areaHeight,
                        final PointersObject sourceForm,
                        final long sourceX,
                        final long sourceY,
                        final long sourceWidth,
                        final long sourceHeight,
                        final PointersObject destForm,
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

            return executeNode.executeCopyBits(receiver, combinationRule, sourceForm, sx, sy, sourceWidth, destForm, dx, dy, destWidth, bbW, bbH);
        }
    }

    protected abstract static class CopyBitsExecuteHelperNode extends Node {
        @CompilationFinal private final ValueProfile halftoneFormStorageType = ValueProfile.createClassProfile();
        @CompilationFinal private final ValueProfile destinationBitsStorageType = ValueProfile.createClassProfile();
        @CompilationFinal private final ValueProfile sourceBitsStorageType = ValueProfile.createClassProfile();

        @CompilationFinal protected final ValueProfile sourceBitsByteStorageType = ValueProfile.createClassProfile();

        protected static CopyBitsExecuteHelperNode create() {
            return CopyBitsExecuteHelperNodeGen.create();
        }

        protected abstract PointersObject executeCopyBits(final PointersObject receiver,
                        long combinationRule,
                        PointersObject sourceForm,
                        long sourceX,
                        long sourceY,
                        long sourceWidth,
                        PointersObject destForm,
                        long destX,
                        long destY,
                        long destWidth,
                        long areaWidth,
                        long areaHeight);

        @SuppressWarnings("unused")
        @Specialization(guards = {"combinationRule == 3", "sourceForm == null"})
        protected final PointersObject doCopyBitsCombiRule3NilSourceForm(final PointersObject receiver,
                        long combinationRule,
                        PointersObject sourceForm,
                        long sourceX,
                        long sourceY,
                        long sourceWidth,
                        PointersObject destForm,
                        long destX,
                        long destY,
                        long destWidth,
                        long areaWidth,
                        long areaHeight) {
            final PointersObject destinationForm = (PointersObject) receiver.at0(BIT_BLT.DEST_FORM);
            final NativeObject destinationBits = (NativeObject) destinationForm.at0(FORM.BITS);
            final NativeObject halftoneForm = (NativeObject) receiver.at0(BIT_BLT.HALFTONE_FORM);
            final int[] fillArray = halftoneForm.getIntStorage(halftoneFormStorageType);
            if (fillArray.length != 1) {
                throw new SqueakException("Expected one fillValue only");
            }
            final int fillValue = fillArray[0];
            final long destinationDepth = (long) destinationForm.at0(FORM.DEPTH);

            final long endX = destX + areaWidth;
            final long endY = destY + areaHeight;

            // TODO move to guard
            if (destinationBits.isByteType()) {
                throw new PrimitiveFailed();
            }
            final int[] ints = destinationBits.getIntStorage(destinationBitsStorageType);

            if (ints.length - 1 < (endY - 1) * destWidth + (endX - 1)) {
                throw new PrimitiveFailed(); // fail early in case of index out of bounds
            }

            for (long y = destY; y < endY; y++) {
                Arrays.fill(ints, (int) (y * destWidth + destX), (int) (y * destWidth + endX), fillValue);
            }
            return receiver;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"combinationRule == 4", "sourceForm == null"})
        protected final PointersObject doCopyBitsCombiRule4NilSourceForm(final PointersObject receiver,
                        long combinationRule,
                        PointersObject sourceForm,
                        long sourceX,
                        long sourceY,
                        long sourceWidth,
                        PointersObject destForm,
                        long destX,
                        long destY,
                        long destWidth,
                        long areaWidth,
                        long areaHeight) {
            final PointersObject destinationForm = (PointersObject) receiver.at0(BIT_BLT.DEST_FORM);
            final NativeObject destinationBits = (NativeObject) destinationForm.at0(FORM.BITS);
            final NativeObject halftoneForm = (NativeObject) receiver.at0(BIT_BLT.HALFTONE_FORM);
            final int[] fillArray = halftoneForm.getIntStorage(halftoneFormStorageType);
            if (fillArray.length != 1) {
                throw new SqueakException("Expected one fillValue only");
            }
            final int fillValue = fillArray[0];

            final long endX = destX + areaWidth;
            final long endY = destY + areaHeight;

            final int[] ints = destinationBits.getIntStorage(destinationBitsStorageType);

            if (ints.length - 1 < (endY - 1) * destWidth + (endX - 1)) {
                throw new PrimitiveFailed(); // fail early in case of index out of bounds
            }

            final int invertedFillValue = ~fillValue;

            for (long dy = destY; dy < destY + areaHeight; dy++) {
                final long destStart = dy * destWidth + destX;
                try {
                    for (long dx = destStart; dx < destStart + areaWidth; dx++) {
                        ints[(int) dx] = invertedFillValue & ints[(int) dx];
                    }
                } finally {
                    LoopNode.reportLoopCount(this, (int) areaWidth);
                }
            }

            return receiver;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"combinationRule == 24", "sourceForm == null"})
        protected final PointersObject doCopyBitsCombiRule24NilSourceForm(final PointersObject receiver,
                        long combinationRule,
                        PointersObject sourceForm,
                        long sourceX,
                        long sourceY,
                        long sourceWidth,
                        PointersObject destForm,
                        long destX,
                        long destY,
                        long destWidth,
                        long areaWidth,
                        long areaHeight) {
            final PointersObject destinationForm = (PointersObject) receiver.at0(BIT_BLT.DEST_FORM);
            final NativeObject destinationBits = (NativeObject) destinationForm.at0(FORM.BITS);
            final NativeObject halftoneForm = (NativeObject) receiver.at0(BIT_BLT.HALFTONE_FORM);
            final int[] fillArray = halftoneForm.getIntStorage(halftoneFormStorageType);
            if (fillArray.length != 1) {
                throw new SqueakException("Expected one fillValue only");
            }
            final int fillValue = fillArray[0];

            final int[] ints = destinationBits.getIntStorage(destinationBitsStorageType);

            final long endX = destX + areaWidth;
            final long endY = destY + areaHeight;

            if (ints.length - 1 < (endY - 1) * destWidth + (endX - 1)) {
                throw new PrimitiveFailed(); // fail early in case of index out of bounds
            }

            for (long y = destY; y < endY; y++) {
                for (long x = destX; x < endX; x++) {
                    final int index = (int) (y * destWidth + x);
                    ints[index] = alphaBlend24(fillValue, ints[index]);
                }
            }
            return receiver;
        }
    }

    protected abstract static class PixelValueAtExtractHelperNode extends Node {
        @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();
        @Child private PixelValueAtExecuteHelperNode executeNode = PixelValueAtExecuteHelperNode.create();

        protected static PixelValueAtExtractHelperNode create() {
            return PixelValueAtExtractHelperNodeGen.create();
        }

        protected abstract long executeValueAt(PointersObject receiver, long xValue, long yValue, Object bitmap);

        @Specialization(guards = "bitmap.isIntType()")
        protected final long doInts(final PointersObject receiver, final long xValue, final long yValue, final NativeObject bitmap) {
            final Object width = at0Node.execute(receiver, FORM.WIDTH);
            final Object height = at0Node.execute(receiver, FORM.HEIGHT);
            final Object depth = at0Node.execute(receiver, FORM.DEPTH);
            return executeNode.executeValueAt(receiver, xValue, yValue, bitmap, width, height, depth);
        }
    }

    protected abstract static class PixelValueAtExecuteHelperNode extends Node {
        private final ValueProfile intProfile = ValueProfile.createClassProfile();
        private final BranchProfile errorProfile = BranchProfile.create();

        protected static PixelValueAtExecuteHelperNode create() {
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
            final long ppW = 32 / depth;
            final long stride = (width + ppW - 1) / ppW;
            final int[] ints = bitmap.getIntStorage(intProfile);
            if (ints.length > stride * height) {
                errorProfile.enter();
                throw new PrimitiveFailed();
            }
            final int word = ints[(int) ((yValue * stride) + Math.floorDiv(xValue, ppW))];
            final int mask = 0xFFFFFFFF >> (32 - depth);
            final long shift = 32 - (((xValue & (ppW - 1)) + 1) * depth);
            return ((word >> shift) & mask) & 0xffffffffL;
        }
    }

    /*
     * Primitive Helper Functions
     */

    protected static int alphaBlend24(final int sourceWord, final int destinationWord) {
        final int alpha = sourceWord >> 24;
        if (alpha == 0)
            return destinationWord;
        if (alpha == 255)
            return sourceWord;

        final int unAlpha = 255 - alpha;

        // blend red and blue
        int blendRB = ((sourceWord & 0xFF00FF) * alpha) +
                        ((destinationWord & 0xFF00FF) * unAlpha) + 0xFF00FF;

        // blend alpha and green
        int blendAG = (((sourceWord >> 8 | 0xFF0000) & 0xFF00FF) * alpha) +
                        ((destinationWord >> 8 & 0xFF00FF) * unAlpha) + 0xFF00FF;

        blendRB = (blendRB + (blendRB - 0x10001 >> 8 & 0xFF00FF) >> 8) & 0xFF00FF;
        blendAG = (blendAG + (blendAG - 0x10001 >> 8 & 0xFF00FF) >> 8) & 0xFF00FF;
        return blendRB | (blendAG << 8);
    }
}
