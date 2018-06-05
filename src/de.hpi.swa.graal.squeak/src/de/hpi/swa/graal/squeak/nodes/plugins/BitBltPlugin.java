package de.hpi.swa.graal.squeak.nodes.plugins;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ValueProfile;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.exceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NotProvided;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.BIT_BLT;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.FORM;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAt0Node;
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
        @CompilationFinal private final ValueProfile halftoneFormStorageType = ValueProfile.createClassProfile();
        @CompilationFinal private final ValueProfile destinationBitsStorageType = ValueProfile.createClassProfile();
        @CompilationFinal private final ValueProfile sourceBitsStorageType = ValueProfile.createClassProfile();
        @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();
        @Child private SimulationPrimitiveNode simulateNode;

        PrintWriter measurements;
        static final boolean measure = true;

        protected PrimCopyBitsNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
            simulateNode = SimulationPrimitiveNode.create(method, getClass().getSimpleName(), "primitiveCopyBits");

            if (measure) {
                try {
                    measurements = new PrintWriter("measure-" + Long.toString(System.currentTimeMillis()) + ".csv", "UTF-8");
                    measurements.println("combinationRule,hasDest,hasSource,hasHalftone,hasColormap,sourceDepth,destDepth,width,height,time");
                } catch (Exception e) {
                    e.printStackTrace(System.out);
                }
            }
        }

        @Specialization(guards = {"disableWhileMeasuring()", "hasCombinationRule(receiver, 3)", "hasNilSourceForm(receiver)"})
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

        @Specialization(guards = {"disableWhileMeasuring()", "hasCombinationRule(receiver, 24)", "hasNilSourceForm(receiver)"})
        protected final Object doCopyBitsCombiRule24NilSourceForm(final VirtualFrame frame, final PointersObject receiver) {
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
                    int index = y * destinationWidth + x;
                    ints[index] = alphaBlend24(fillValue, ints[index]);
                }
            }
            return receiver;
        }

        /**
         * Draw call used by desktop background with form
         */
        @Specialization(guards = {"disableWhileMeasuring()", "hasCombinationRule(receiver, 25)", "!hasNilSourceForm(receiver)", "hasSourceFormDepth(receiver, 32)", "hasNilColormap(receiver)"})
        protected final Object doCopyBitsCombiRule25WithSourceForm(final PointersObject receiver) {
            final PointersObject destinationForm = (PointersObject) receiver.at0(BIT_BLT.DEST_FORM);
            final NativeObject destinationBits = (NativeObject) destinationForm.at0(FORM.BITS);

            final PointersObject sourceForm = (PointersObject) receiver.at0(BIT_BLT.SOURCE_FORM);
            final NativeObject sourceBits = (NativeObject) sourceForm.at0(FORM.BITS);

            final long sourceX = (long) receiver.at0(BIT_BLT.SOURCE_X);
            final long sourceY = (long) receiver.at0(BIT_BLT.SOURCE_Y);
            final int sourceWidth = (int) ((long) sourceForm.at0(FORM.WIDTH));
            final int sourceHeight = (int) ((long) sourceForm.at0(FORM.HEIGHT));

            final int destinationWidth = (int) ((long) destinationForm.at0(FORM.WIDTH));
            final int destinationHeight = (int) ((long) destinationForm.at0(FORM.HEIGHT));
            final long destX = (long) receiver.at0(BIT_BLT.DEST_X);
            final long destY = (long) receiver.at0(BIT_BLT.DEST_Y);
            final long width = (long) receiver.at0(BIT_BLT.WIDTH);
            final long height = (long) receiver.at0(BIT_BLT.HEIGHT);

            final long clipX = (long) receiver.at0(BIT_BLT.CLIP_X);
            final long clipY = (long) receiver.at0(BIT_BLT.CLIP_Y);
            final long clipWidth = (long) receiver.at0(BIT_BLT.CLIP_WIDTH);
            final long clipHeight = (long) receiver.at0(BIT_BLT.CLIP_HEIGHT);
            final long[] clippedValues = clipRange(sourceX, sourceY, sourceWidth, sourceHeight, width, height, destX, destY, clipX, clipY, clipWidth, clipHeight);

            final long clippedSourceX = clippedValues[0];
            final long clippedSourceY = clippedValues[1];
            final long clippedX = clippedValues[2];
            final long clippedY = clippedValues[3];
            final long clippedWidth = clippedValues[4];
            final long clippedHeight = clippedValues[5];

            final int[] dest = destinationBits.getIntStorage(destinationBitsStorageType);

            // request to unhibernate
            if (destinationWidth * destinationHeight > dest.length) {
                throw new PrimitiveFailed();
            }

            if (sourceBits.isByteType()) {
                final byte[] source = sourceBits.getByteStorage(sourceBitsStorageType);
                // request to unhibernate
                if (sourceWidth * sourceHeight > source.length) {
                    throw new PrimitiveFailed();
                }

                for (int dy = (int) clippedY, sy = (int) clippedSourceY; dy < clippedY + clippedHeight; dy++, sy++) {
                    for (int dx = (int) clippedX, sx = (int) clippedSourceX; dx < clippedX + clippedWidth; dx++, sx++) {
                        dest[dy * destinationWidth + dx] = source[sy * sourceWidth + sx];
                    }
                }
            } else {
                final int[] source = sourceBits.getIntStorage(sourceBitsStorageType);
                // request to unhibernate
                if (sourceWidth * sourceHeight > source.length) {
                    throw new PrimitiveFailed();
                }

                for (int dy = (int) clippedY, sy = (int) clippedSourceY; dy < clippedY + clippedHeight; dy++, sy++) {
                    for (int dx = (int) clippedX, sx = (int) clippedSourceX; dx < clippedX + clippedWidth; dx++, sx++) {
                        dest[dy * destinationWidth + dx] = source[sy * sourceWidth + sx];
                    }
                }
            }

            return receiver;
        }

        @Fallback
        protected final Object doSimulation(final VirtualFrame frame, final Object receiver) {
            PointersObject p = (PointersObject) receiver;

            if (!measure) {
                return simulateNode.executeWithArguments(frame, receiver, NotProvided.INSTANCE, NotProvided.INSTANCE, NotProvided.INSTANCE, NotProvided.INSTANCE, NotProvided.INSTANCE,
                                NotProvided.INSTANCE, NotProvided.INSTANCE, NotProvided.INSTANCE);
            } else {
                long now = System.currentTimeMillis();
                Object res = simulateNode.executeWithArguments(frame, receiver, NotProvided.INSTANCE, NotProvided.INSTANCE, NotProvided.INSTANCE, NotProvided.INSTANCE, NotProvided.INSTANCE,
                                NotProvided.INSTANCE, NotProvided.INSTANCE, NotProvided.INSTANCE);

                long delta = System.currentTimeMillis() - now;

                Object dest = p.at0(BIT_BLT.DEST_FORM);
                Object source = p.at0(BIT_BLT.SOURCE_FORM);
                final long width = (long) p.at0(BIT_BLT.WIDTH);
                final long height = (long) p.at0(BIT_BLT.HEIGHT);
                final long sourceDepth = source != code.image.nil ? (long) at0Node.execute(source, FORM.DEPTH) : -1;
                final long destDepth = dest != code.image.nil ? (long) at0Node.execute(dest, FORM.DEPTH) : -1;

                String type = p.at0(BIT_BLT.COMBINATION_RULE).toString() + "," +
                                Boolean.toString(dest != code.image.nil) + "," +
                                Boolean.toString(source != code.image.nil) + "," +
                                Boolean.toString(p.at0(BIT_BLT.HALFTONE_FORM) != code.image.nil) + "," +
                                Boolean.toString(p.at0(BIT_BLT.COLOR_MAP) != code.image.nil) + "," +
                                Long.toString(sourceDepth) + "," +
                                Long.toString(destDepth) + "," +
                                Long.toString(width) + "," +
                                Long.toString(height) + "," +
                                Long.toString(delta);

                measurements.println(type);
                measurements.flush();
                return res;
            }
        }

        protected static final int alphaBlend24(int sourceWord, int destinationWord) {
            int alpha = sourceWord >> 24;
            if (alpha == 0)
                return destinationWord;
            if (alpha == 255)
                return sourceWord;

            int unAlpha = 255 - alpha;

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

        /*
         * Guard Helpers
         */
        protected static final boolean disableWhileMeasuring() {
            return !measure;
        }

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

        protected final boolean hasNilColormap(final PointersObject target) {
            return target.at0(BIT_BLT.COLOR_MAP) == code.image.nil;
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
