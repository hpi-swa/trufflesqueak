package de.hpi.swa.graal.squeak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
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
import de.hpi.swa.graal.squeak.nodes.plugins.BitBltPluginFactory.HandleReceiverAndBitmapHelperNodeGen;
import de.hpi.swa.graal.squeak.nodes.plugins.BitBltPluginFactory.PixelValueAtHelperNodeGen;
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
        private final ValueProfile halftoneFormStorageType = ValueProfile.createClassProfile();
        private final ValueProfile destinationBitsStorageType = ValueProfile.createClassProfile();
        @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();
        @Child private SimulationPrimitiveNode simulateNode;

        protected PrimCopyBitsNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
            simulateNode = SimulationPrimitiveNode.create(method, getClass().getSimpleName(), "primitiveCopyBits");
        }

        @Specialization(guards = {"hasCombinationRule(receiver, 3)", "hasNilSourceForm(receiver)"})
        protected final Object doCopyBitsCombiRule3NilSourceForm(final VirtualFrame frame, final PointersObject receiver) {
            final PointersObject destinationForm = (PointersObject) receiver.at0(BIT_BLT.DEST_FORM);
            final NativeObject destinationBits = (NativeObject) destinationForm.at0(FORM.BITS);
            final NativeObject halftoneForm = (NativeObject) receiver.at0(BIT_BLT.HALFTONE_FORM);
            final int[] fillArray = halftoneForm.getIntStorage(halftoneFormStorageType);
            if (fillArray.length != 1) {
                throw new SqueakException("Expected one fillValue only");
            }
            final int fillValue = fillArray[0];
            final long destinationDepth = (long) destinationForm.at0(FORM.DEPTH);
            if (destinationDepth != 32) { // fall back to simulation if not 32-bit
                return doSimulation(frame, receiver);
            }
            final int destinationWidth = (int) ((long) destinationForm.at0(FORM.WIDTH));
            final long destX = (long) receiver.at0(BIT_BLT.DEST_X);
            final long destY = (long) receiver.at0(BIT_BLT.DEST_Y);
            final long width = (long) receiver.at0(BIT_BLT.WIDTH);
            final long height = (long) receiver.at0(BIT_BLT.HEIGHT);
            final long clipX = (long) receiver.at0(BIT_BLT.CLIP_X);
            final long clipY = (long) receiver.at0(BIT_BLT.CLIP_Y);
            final long clipWidth = (long) receiver.at0(BIT_BLT.CLIP_WIDTH);
            final long clipHeight = (long) receiver.at0(BIT_BLT.CLIP_HEIGHT);

            final long[] clippedValues = clipRange(-1, 0, 0, 0, width, height, destX, destY, clipX, clipY, clipWidth, clipHeight);
            final long dx = clippedValues[2];
            final long dy = clippedValues[3];
            final int bbW = (int) clippedValues[4];
            final int bbH = (int) clippedValues[5];
            if (bbW <= 0 || bbH <= 0) {
                return receiver; // "zero width or height; noop"
            }
            final long endX = dx + bbW;
            final long endY = dy + bbH;

            final int[] ints = destinationBits.getIntStorage(destinationBitsStorageType);

            if (ints.length - 1 < (endY - 1) * destinationWidth + (endX - 1)) {
                throw new PrimitiveFailed(); // fail early in case of index out of bounce
            }

            for (int y = (int) dy; y < endY; y++) {
                for (int x = (int) dx; x < endX; x++) {
                    ints[y * destinationWidth + x] = fillValue;
                }
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

        protected static final boolean hasCombinationRule(final PointersObject target, final int ruleIndex) {
            return ruleIndex == (long) target.at0(BIT_BLT.COMBINATION_RULE);
        }

        protected final boolean hasDestinationFormDepth(final PointersObject target, final int ruleIndex) {
            return ruleIndex == (long) at0Node.execute(target.at0(BIT_BLT.DEST_FORM), FORM.DEPTH);
        }

        protected final boolean hasSourceFormDepth(final PointersObject target, final int ruleIndex) {
            return ruleIndex == (long) at0Node.execute(target.at0(BIT_BLT.SOURCE_FORM), FORM.DEPTH);
        }

        protected final boolean hasNilSourceForm(final PointersObject target) {
            return target.at0(BIT_BLT.SOURCE_FORM) == code.image.nil;
        }

        protected final boolean hasNilHalftoneForm(final PointersObject target) {
            return target.at0(BIT_BLT.HALFTONE_FORM) == code.image.nil;
        }
    }

    @ImportStatic(FORM.class)
    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitivePixelValueAt")
    protected abstract static class PrimPixelValueAtNode extends AbstractPrimitiveNode {
        @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();
        @Child protected SqueakObjectSizeNode sizeNode = SqueakObjectSizeNode.create();
        @Child private HandleReceiverAndBitmapHelperNode handleNode = HandleReceiverAndBitmapHelperNode.create();

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

    protected abstract static class HandleReceiverAndBitmapHelperNode extends Node {
        @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();
        @Child private PixelValueAtHelperNode pixelValueNode = PixelValueAtHelperNode.create();

        protected static HandleReceiverAndBitmapHelperNode create() {
            return HandleReceiverAndBitmapHelperNodeGen.create();
        }

        protected abstract long executeValueAt(PointersObject receiver, long xValue, long yValue, Object bitmap);

        @Specialization(guards = "bitmap.isByteType()")
        protected final long doBytes(final PointersObject receiver, final long xValue, final long yValue, final NativeObject bitmap) {
            final Object width = at0Node.execute(receiver, FORM.WIDTH);
            final Object height = at0Node.execute(receiver, FORM.HEIGHT);
            final Object depth = at0Node.execute(receiver, FORM.DEPTH);
            return pixelValueNode.executeValueAt(receiver, xValue, yValue, bitmap, width, height, depth);
        }

        @Specialization(guards = "bitmap.isIntType()")
        protected final long doInts(final PointersObject receiver, final long xValue, final long yValue, final NativeObject bitmap) {
            final Object width = at0Node.execute(receiver, FORM.WIDTH);
            final Object height = at0Node.execute(receiver, FORM.HEIGHT);
            final Object depth = at0Node.execute(receiver, FORM.DEPTH);
            return pixelValueNode.executeValueAt(receiver, xValue, yValue, bitmap, width, height, depth);
        }
    }

    protected abstract static class PixelValueAtHelperNode extends Node {
        private final ValueProfile byteProfile = ValueProfile.createClassProfile();
        private final ValueProfile intProfile = ValueProfile.createClassProfile();
        private final BranchProfile errorProfile = BranchProfile.create();

        protected static PixelValueAtHelperNode create() {
            return PixelValueAtHelperNodeGen.create();
        }

        protected abstract long executeValueAt(PointersObject receiver, long xValue, long yValue, NativeObject bitmap, Object width, Object height, Object depth);

        @SuppressWarnings("unused")
        @Specialization(guards = "xValue >= width || yValue >= height")
        protected static final long doQuickReturn(final PointersObject receiver, final long xValue, final long yValue, final NativeObject bitmap, final long width, final long height,
                        final long depth) {
            return 0L;
        }

        @Specialization(guards = "bitmap.isByteType()")
        protected final long doBytes(@SuppressWarnings("unused") final PointersObject receiver, final long xValue, final long yValue, final NativeObject bitmap, final long width,
                        final long height, final long depth) {
            final long ppW = 32 / depth;
            final long stride = (width + ppW - 1) / ppW;
            final byte[] bytes = bitmap.getByteStorage(byteProfile);
            if (bytes.length > stride * height * 4) { // bytes per word
                errorProfile.enter();
                throw new PrimitiveFailed();
            }
            final int offset = (int) ((yValue * stride) + xValue / ppW) * 4;
            final int word = bytes[offset + 3] << 24 | bytes[offset + 2] << 16 | bytes[offset + 1] << 8 | bytes[offset];
            final int mask = 0xFFFFFFFF >> (32 - depth);
            final long shift = 32 - (((xValue & (ppW - 1)) + 1) * depth);
            return (word >> shift) & mask;
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
            final int word = ints[(int) ((yValue * stride) + xValue / ppW)];
            final int mask = 0xFFFFFFFF >> (32 - depth);
            final long shift = 32 - (((xValue & (ppW - 1)) + 1) * depth);
            return ((word >> shift) & mask) & 0xffffffffL;
        }
    }

    /*
     * Primitive Helper Functions
     */

    // BitBltSimulation>>#clipRange
    private static long[] clipRange(final long sourceX,
                    final long sourceY,
                    final long sourceWidth,
                    final long sourceHeight,
                    final long width,
                    final long height,
                    final long destX,
                    final long destY,
                    final long clipX, final long clipY,
                    final long clipWidth, final long clipHeight) {
        long sx;
        long sy;
        long dx;
        long dy;
        long bbW;
        long bbH;

        if (destX >= clipX) {
            sx = sourceX;
            dx = destX;
            bbW = width;
        } else {
            sx = sourceX + (clipX - destX);
            bbW = width - (clipX - destX);
            dx = clipX;
        }

        if ((dx + bbW) > (clipX + clipWidth)) {
            bbW = bbW - ((dx + bbW) - (clipX + clipWidth));
        }

        // then in y
        if (destY >= clipY) {
            sy = sourceY;
            dy = destY;
            bbH = height;
        } else {
            sy = sourceY + clipY - destY;
            bbH = height - (clipY - destY);
            dy = clipY;
        }

        if ((dy + bbH) > (clipY + clipHeight)) {
            bbH = bbH - ((dy + bbH) - (clipY + clipHeight));
        }

        if (sourceX < 0) { // nosource signaled by negative `sourceX`
            return new long[]{sx, sy, dx, dy, bbW, bbH};
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
        return new long[]{sx, sy, dx, dy, bbW, bbH};
    }
}
