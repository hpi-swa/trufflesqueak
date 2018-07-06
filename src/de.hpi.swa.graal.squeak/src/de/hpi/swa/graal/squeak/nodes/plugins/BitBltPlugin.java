package de.hpi.swa.graal.squeak.nodes.plugins;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
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
import de.hpi.swa.graal.squeak.nodes.plugins.BitBltPluginFactory.CopyNodeGen;
import de.hpi.swa.graal.squeak.nodes.plugins.BitBltPluginFactory.BlendNodeGen;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.MiscellaneousPrimitives.SimulationPrimitiveNode;

public final class BitBltPlugin extends AbstractPrimitiveFactoryHolder {

    protected static final class BitBltData {
        final PointersObject destinationForm;
        public final int destX;
        public final int destY;
        public final int areaWidth;
        public final int areaHeight;
        public final int destinationWidth;

        public BitBltData(final PointersObject receiver) {
            destinationForm = (PointersObject) receiver.at0(BIT_BLT.DEST_FORM);
            destinationWidth = (int) ((long) destinationForm.at0(FORM.WIDTH));

            final int _destX = (int) (long) receiver.at0(BIT_BLT.DEST_X);
            final int _destY = (int) (long) receiver.at0(BIT_BLT.DEST_Y);
            final int _areaWidth = (int) (long) receiver.at0(BIT_BLT.WIDTH);
            final int _areaHeight = (int) (long) receiver.at0(BIT_BLT.HEIGHT);

            final int clipX = (int) (long) receiver.at0(BIT_BLT.CLIP_X);
            final int clipY = (int) (long) receiver.at0(BIT_BLT.CLIP_Y);
            final int clipWidth = (int) (long) receiver.at0(BIT_BLT.CLIP_WIDTH);
            final int clipHeight = (int) (long) receiver.at0(BIT_BLT.CLIP_HEIGHT);

            // adapted inline copy of BilBltSimulation>>clipRange for the nil sourceForm case
            // it's inline so we can final inst vars
            final int dx;
            final int dy;
            int bbW;
            int bbH;

            if (_destX >= clipX) {
                dx = _destX;
                bbW = _areaWidth;
            } else {
                bbW = _areaWidth - (clipX - _destX);
                dx = clipX;
            }

            if ((dx + bbW) > (clipX + clipWidth)) {
                bbW = bbW - ((dx + bbW) - (clipX + clipWidth));
            }

            // then in y
            if (_destY >= clipY) {
                dy = _destY;
                bbH = _areaHeight;
            } else {
                bbH = _areaHeight - (clipY - _destY);
                dy = clipY;
            }

            if ((dy + bbH) > (clipY + clipHeight)) {
                bbH = bbH - ((dy + bbH) - (clipY + clipHeight));
            }

            areaWidth = bbW;
            areaHeight = bbH;
            destX = dx;
            destY = dy;
        }

        public boolean hasValidArea() {
            return areaWidth > 0 && areaHeight > 0;
        }
    }

    protected static final class BitBltDataWithSourceForm {

        final PointersObject sourceForm;
        final PointersObject destinationForm;
        public final int sourceX;
        public final int sourceY;
        public final int sourceWidth;
        public final int sourceHeight;
        public final int destinationWidth;
        public final int destinationHeight;
        public final int destX;
        public final int destY;
        public final int areaWidth;
        public final int areaHeight;

        BitBltDataWithSourceForm(final PointersObject receiver) {
            sourceForm = (PointersObject) receiver.at0(BIT_BLT.SOURCE_FORM);
            destinationForm = (PointersObject) receiver.at0(BIT_BLT.DEST_FORM);
            sourceWidth = (int) ((long) sourceForm.at0(FORM.WIDTH));
            sourceHeight = (int) ((long) sourceForm.at0(FORM.HEIGHT));
            destinationWidth = (int) ((long) destinationForm.at0(FORM.WIDTH));
            destinationHeight = (int) ((long) destinationForm.at0(FORM.HEIGHT));

            // these are the unclipped values as passed in
            final int _sourceX = (int) (long) receiver.at0(BIT_BLT.SOURCE_X);
            final int _sourceY = (int) (long) receiver.at0(BIT_BLT.SOURCE_Y);

            final int _destX = (int) (long) receiver.at0(BIT_BLT.DEST_X);
            final int _destY = (int) (long) receiver.at0(BIT_BLT.DEST_Y);
            final int _areaWidth = (int) (long) receiver.at0(BIT_BLT.WIDTH);
            final int _areaHeight = (int) (long) receiver.at0(BIT_BLT.HEIGHT);

            final int clipX = (int) (long) receiver.at0(BIT_BLT.CLIP_X);
            final int clipY = (int) (long) receiver.at0(BIT_BLT.CLIP_Y);
            final int clipWidth = (int) (long) receiver.at0(BIT_BLT.CLIP_WIDTH);
            final int clipHeight = (int) (long) receiver.at0(BIT_BLT.CLIP_HEIGHT);

            // inline version of BitBltSimulation>>clipRange to we can have final inst vars
            int sx;
            int sy;
            int dx;
            int dy;
            int bbW;
            int bbH;

            if (_destX >= clipX) {
                sx = _sourceX;
                dx = _destX;
                bbW = _areaWidth;
            } else {
                sx = _sourceX + (clipX - _destX);
                bbW = _areaWidth - (clipX - _destX);
                dx = clipX;
            }

            if ((dx + bbW) > (clipX + clipWidth)) {
                bbW = bbW - ((dx + bbW) - (clipX + clipWidth));
            }

            // then in y
            if (_destY >= clipY) {
                sy = _sourceY;
                dy = _destY;
                bbH = _areaHeight;
            } else {
                sy = _sourceY + clipY - _destY;
                bbH = _areaHeight - (clipY - _destY);
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

            sourceX = sx;
            sourceY = sy;
            destX = dx;
            destY = dy;
            areaWidth = bbW;
            areaHeight = bbH;
        }

        boolean hasValidArea() {
            return areaWidth > 0 && areaHeight > 0;
        }
    }

    static PrintWriter measurements;

    static void createMeasurements() {
        try {
            measurements = new PrintWriter("measure-" + Long.toString(System.currentTimeMillis()) + ".csv", "UTF-8");
            measurements.println("combinationRule,hasDest,hasSource,hasHalftone,hasColormap,sourceDepth,destDepth,width,height,time");
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    static void measureEntry(final PointersObject p, final long delta, final SqueakObjectAt0Node at0Node) {
        final Object dest = p.at0(BIT_BLT.DEST_FORM);
        final Object source = p.at0(BIT_BLT.SOURCE_FORM);
        final long width = (long) p.at0(BIT_BLT.WIDTH);
        final long height = (long) p.at0(BIT_BLT.HEIGHT);
        final long sourceDepth = source != p.image.nil ? (long) at0Node.execute(source, FORM.DEPTH) : -1;
        final long destDepth = dest != p.image.nil ? (long) at0Node.execute(dest, FORM.DEPTH) : -1;

        // @formatter:off
        measurements
            .append(p.at0(BIT_BLT.COMBINATION_RULE).toString()).append(',')
            .append(Boolean.toString(dest != p.image.nil)).append(',')
            .append(Boolean.toString(source != p.image.nil)).append(',')
            .append(Boolean.toString(p.at0(BIT_BLT.HALFTONE_FORM) != p.image.nil)).append(',')
            .append(Boolean.toString(p.at0(BIT_BLT.COLOR_MAP) != p.image.nil)).append(',')
            .append(Long.toString(sourceDepth)).append(',')
            .append(Long.toString(destDepth)).append(',')
            .append(Long.toString(width)).append(',')
            .append(Long.toString(height)).append(',')
            .append(Long.toString(delta))
            .flush();
        // @formatter:on
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return BitBltPluginFactory.getFactories();
    }

    @Override
    public boolean useSimulationAsFallback() {
        return true;
    }

    public abstract static class CopyNode extends Node {

        @CompilationFinal protected final ValueProfile sourceBitsByteStorageType = ValueProfile.createClassProfile();
        @CompilationFinal protected final ValueProfile sourceBitsIntStorageType = ValueProfile.createClassProfile();

        protected static CopyNode create() {
            return CopyNodeGen.create();
        }

        public abstract void executeFill(NativeObject sourceBits, int[] dest, BitBltDataWithSourceForm data);

        @Specialization(guards = {"sourceBits.isByteType()"})
        protected void doFillBytes(final NativeObject sourceBits, final int[] dest, final BitBltDataWithSourceForm data) {
            final byte[] source = sourceBits.getByteStorage(sourceBitsByteStorageType);

            // request to unhibernate
            if (data.sourceWidth * data.sourceHeight > source.length) {
                throw new PrimitiveFailed();
            }

            for (int dy = data.destY, sy = data.sourceY; dy < data.destY + data.areaHeight; dy++, sy++) {
                final int sourceStart = sy * data.sourceWidth + data.sourceX;
                final int destStart = dy * data.destinationWidth + data.destX;
                System.arraycopy(source, sourceStart, dest, destStart, data.areaWidth);
            }
        }

        @Specialization(guards = {"sourceBits.isIntType()"})
        protected void doFillInts(final NativeObject sourceBits, final int[] dest, final BitBltDataWithSourceForm data) {
            final int[] source = sourceBits.getIntStorage(sourceBitsIntStorageType);

            // request to unhibernate
            if (data.sourceWidth * data.sourceHeight > source.length) {
                System.out.println("fill ints");
                throw new PrimitiveFailed();
            }

            for (int dy = data.destY, sy = data.sourceY; dy < data.destY + data.areaHeight; dy++, sy++) {
                final int sourceStart = sy * data.sourceWidth + data.sourceX;
                final int destStart = dy * data.destinationWidth + data.destX;
                System.arraycopy(source, sourceStart, dest, destStart, data.areaWidth);
            }
        }
    }

    public abstract static class BlendNode extends Node {

        @CompilationFinal protected final ValueProfile sourceBitsByteStorageType = ValueProfile.createClassProfile();
        @CompilationFinal protected final ValueProfile sourceBitsIntStorageType = ValueProfile.createClassProfile();
        @CompilationFinal protected final ValueProfile destinationBitsStorageType = ValueProfile.createClassProfile();

        protected static BlendNode create() {
            return BlendNodeGen.create();
        }

        public abstract void executeBlend(NativeObject sourceBits, int[] dest, BitBltDataWithSourceForm data);

        @Specialization(guards = {"sourceBits.isByteType()"})
        protected void doBlendBytes(final NativeObject sourceBits, final int[] dest, final BitBltDataWithSourceForm data) {
            final byte[] source = sourceBits.getByteStorage(sourceBitsByteStorageType);

            // request to unhibernate
            if (data.sourceWidth * data.sourceHeight > source.length) {
                throw new PrimitiveFailed();
            }

            for (int dy = data.destY, sy = data.sourceY; dy < data.destY + data.areaHeight; dy++, sy++) {
                final int sourceStart = sy * data.sourceWidth + data.sourceX;
                final int destStart = dy * data.destinationWidth + data.destX;
                try {
                    for (int dx = destStart, sx = sourceStart; dx < destStart + data.areaWidth; dx++, sx++) {
                        dest[dx] = alphaBlend24(source[sx], dest[dx]);
                    }
                } finally {
                    LoopNode.reportLoopCount(this, data.areaWidth);
                }
            }
        }

        @Specialization(guards = {"sourceBits.isIntType()"})
        protected void doBlendInts(final NativeObject sourceBits, final int[] dest, final BitBltDataWithSourceForm data) {
            final int[] source = sourceBits.getIntStorage(sourceBitsIntStorageType);
            // request to unhibernate
            if (data.sourceWidth * data.sourceHeight > source.length) {
                System.out.println("fill ints");
                throw new PrimitiveFailed();
            }

            for (int dy = data.destY, sy = data.sourceY; dy < data.destY + data.areaHeight; dy++, sy++) {
                final int sourceStart = sy * data.sourceWidth + data.sourceX;
                final int destStart = dy * data.destinationWidth + data.destX;
                try {
                    for (int dx = destStart, sx = sourceStart; dx < destStart + data.areaWidth; dx++, sx++) {
                        dest[dx] = alphaBlend24(source[sx], dest[dx]);
                    }
                } finally {
                    LoopNode.reportLoopCount(this, data.areaWidth);
                }
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveCopyBits")
    protected abstract static class PrimCopyBitsNode extends AbstractPrimitiveNode {
        @CompilationFinal private final ValueProfile halftoneFormStorageType = ValueProfile.createClassProfile();
        @CompilationFinal private final ValueProfile destinationBitsStorageType = ValueProfile.createClassProfile();
        @CompilationFinal private final ValueProfile sourceBitsStorageType = ValueProfile.createClassProfile();

        @CompilationFinal protected final ValueProfile sourceBitsByteStorageType = ValueProfile.createClassProfile();

        @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();
        @Child private SimulationPrimitiveNode simulateNode;
        @Child private CopyNode fillNode = CopyNode.create();
        @Child private BlendNode blendNode = BlendNode.create();

        static final boolean measure = false;

        protected PrimCopyBitsNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
            simulateNode = SimulationPrimitiveNode.create(method, getClass().getSimpleName(), "primitiveCopyBits");

            if (measure && measurements == null) {
                createMeasurements();
            }
        }

        @Specialization(guards = {"disableWhileMeasuring()", "hasCombinationRule(receiver, 3)",
                        "hasNilSourceForm(receiver)"})
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

            final BitBltData data = new BitBltData(receiver);
            if (!data.hasValidArea()) {
                return receiver;
            }

            final int endX = data.destX + data.areaWidth;
            final int endY = data.destY + data.areaHeight;

            final int[] ints = destinationBits.getIntStorage(destinationBitsStorageType);

            if (ints.length - 1 < (endY - 1) * data.destinationWidth + (endX - 1)) {
                throw new PrimitiveFailed(); // fail early in case of index out of bounds
            }

            for (int y = data.destY; y < endY; y++) {
                Arrays.fill(ints, y * data.destinationWidth + data.destX, y * data.destinationWidth + endX, fillValue);
            }
            return receiver;
        }

        @Specialization(guards = {"disableWhileMeasuring()", "hasCombinationRule(receiver, 4)",
                        "hasNilSourceForm(receiver)"})
        protected final Object doCopyBitsCombiRule4NilSourceForm(final VirtualFrame frame, final PointersObject receiver) {
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

            final BitBltData data = new BitBltData(receiver);
            if (!data.hasValidArea()) {
                return receiver;
            }

            final int endX = data.destX + data.areaWidth;
            final int endY = data.destY + data.areaHeight;

            final int[] ints = destinationBits.getIntStorage(destinationBitsStorageType);

            if (ints.length - 1 < (endY - 1) * data.destinationWidth + (endX - 1)) {
                throw new PrimitiveFailed(); // fail early in case of index out of bounds
            }

            final int invertedFillValue = ~fillValue;

            for (int dy = data.destY; dy < data.destY + data.areaHeight; dy++) {
                final int destStart = dy * data.destinationWidth + data.destX;
                try {
                    for (int dx = destStart; dx < destStart + data.areaWidth; dx++) {
                        ints[dx] = invertedFillValue & ints[dx];
                    }
                } finally {
                    LoopNode.reportLoopCount(this, data.areaWidth);
                }
            }

            return receiver;
        }

        @Specialization(guards = {"disableWhileMeasuring()", "hasCombinationRule(receiver, 24)",
                        "hasNilSourceForm(receiver)"})
        protected final Object doCopyBitsCombiRule24NilSourceForm(final VirtualFrame frame, final PointersObject receiver) {
            final PointersObject destinationForm = (PointersObject) receiver.at0(BIT_BLT.DEST_FORM);
            final NativeObject destinationBits = (NativeObject) destinationForm.at0(FORM.BITS);
            final NativeObject halftoneForm = (NativeObject) receiver.at0(BIT_BLT.HALFTONE_FORM);
            final int[] fillArray = halftoneForm.getIntStorage(halftoneFormStorageType);
            if (fillArray.length != 1) {
                throw new SqueakException("Expected one fillValue only");
            }
            final int fillValue = fillArray[0];
            final int destinationDepth = (int) (long) destinationForm.at0(FORM.DEPTH);
            if (destinationDepth != 32) { // fall back to simulation if not 32-bit
                return doSimulation(frame, receiver);
            }

            final BitBltData data = new BitBltData(receiver);
            if (!data.hasValidArea()) {
                return receiver;
            }

            final int[] ints = destinationBits.getIntStorage(destinationBitsStorageType);

            final int endX = data.destX + data.areaWidth;
            final int endY = data.destY + data.areaHeight;

            if (ints.length - 1 < (endY - 1) * data.destinationWidth + (endX - 1)) {
                throw new PrimitiveFailed(); // fail early in case of index out of bounds
            }

            for (int y = data.destY; y < endY; y++) {
                for (int x = data.destX; x < endX; x++) {
                    final int index = y * data.destinationWidth + x;
                    ints[index] = alphaBlend24(fillValue, ints[index]);
                }
            }
            return receiver;
        }

        @Specialization(guards = {"disableWhileMeasuring()", "hasCombinationRule(receiver, 24)",
                        "!hasNilSourceForm(receiver)"})
        protected final Object doCopyBitsCombiRule24WithSourceForm(final VirtualFrame frame, final PointersObject receiver) {
            final PointersObject destinationForm = (PointersObject) receiver.at0(BIT_BLT.DEST_FORM);
            final PointersObject sourceForm = (PointersObject) receiver.at0(BIT_BLT.SOURCE_FORM);
            final NativeObject destinationBits = (NativeObject) destinationForm.at0(FORM.BITS);
            final NativeObject sourceBits = (NativeObject) sourceForm.at0(FORM.BITS);
            final int destinationDepth = (int) (long) destinationForm.at0(FORM.DEPTH);
            if (destinationDepth != 32) { // fall back to simulation if not 32-bit
                return doSimulation(frame, receiver);
            }

            final BitBltDataWithSourceForm data = new BitBltDataWithSourceForm(receiver);
            if (!data.hasValidArea()) {
                return receiver;
            }

            final int[] dest = destinationBits.getIntStorage(destinationBitsStorageType);
            blendNode.executeBlend(sourceBits, dest, data);

            return receiver;
        }

        /**
         * Draw call used by desktop background with form
         */
        @Specialization(guards = {"disableWhileMeasuring()",
                        "hasCombinationRule(receiver, 25)",
                        "!hasNilSourceForm(receiver)",
                        "hasDestinationFormDepth(receiver, 32)",
                        "hasSourceFormDepth(receiver, 32)",
                        "hasNilColormap(receiver)"})
        protected final Object doCopyBitsCombiRule25WithSourceForm(final PointersObject receiver) {
            final PointersObject destinationForm = (PointersObject) receiver.at0(BIT_BLT.DEST_FORM);
            final NativeObject destinationBits = (NativeObject) destinationForm.at0(FORM.BITS);

            final PointersObject sourceForm = (PointersObject) receiver.at0(BIT_BLT.SOURCE_FORM);
            final NativeObject sourceBits = (NativeObject) sourceForm.at0(FORM.BITS);

            final BitBltDataWithSourceForm data = new BitBltDataWithSourceForm(receiver);

            if (!data.hasValidArea()) {
                return receiver;
            }

            final int[] dest = destinationBits.getIntStorage(destinationBitsStorageType);

            // request to unhibernate
            if (data.destinationWidth * data.destinationHeight > dest.length) {
                throw new PrimitiveFailed();
            }

            fillNode.executeFill(sourceBits, dest, data);

            return receiver;
        }

        @Fallback
        protected final Object doSimulation(final VirtualFrame frame, final Object receiver) {
            final PointersObject p = (PointersObject) receiver;

            if (!measure) {
                return simulateNode.executeWithArguments(frame, receiver, NotProvided.INSTANCE, NotProvided.INSTANCE, NotProvided.INSTANCE, NotProvided.INSTANCE, NotProvided.INSTANCE,
                                NotProvided.INSTANCE, NotProvided.INSTANCE, NotProvided.INSTANCE);
            } else {
                final long now = System.currentTimeMillis();
                final Object res = simulateNode.executeWithArguments(frame, receiver, NotProvided.INSTANCE, NotProvided.INSTANCE, NotProvided.INSTANCE, NotProvided.INSTANCE, NotProvided.INSTANCE,
                                NotProvided.INSTANCE, NotProvided.INSTANCE, NotProvided.INSTANCE);

                final long delta = System.currentTimeMillis() - now;

                measureEntry(p, delta, at0Node);
                return res;
            }
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

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveDisplayString")
    protected abstract static class PrimDisplayStringNode extends AbstractPrimitiveNode {
        @CompilationFinal private final ValueProfile halftoneFormStorageType = ValueProfile.createClassProfile();
        @CompilationFinal private final ValueProfile destinationBitsStorageType = ValueProfile.createClassProfile();
        @CompilationFinal private final ValueProfile sourceBitsStorageType = ValueProfile.createClassProfile();
        @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();
        @Child private SimulationPrimitiveNode simulateNode;
        @Child private CopyNode fillNode = CopyNode.create();

        static final boolean measure = false;

        protected PrimDisplayStringNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
            simulateNode = SimulationPrimitiveNode.create(method, getClass().getSimpleName(), "primitiveDisplayString");

            if (measure && measurements == null) {
                createMeasurements();
            }
        }

        @Specialization(guards = {"disableWhileMeasuring()"})
        protected final Object doDisplayStringDummy(final VirtualFrame frame, final PointersObject receiver) {
            return doSimulation(frame, receiver);
        }

        @Fallback
        protected final Object doSimulation(final VirtualFrame frame, final Object receiver) {
            final PointersObject p = (PointersObject) receiver;

            if (!measure) {
                return simulateNode.executeWithArguments(frame, receiver, NotProvided.INSTANCE, NotProvided.INSTANCE, NotProvided.INSTANCE, NotProvided.INSTANCE, NotProvided.INSTANCE,
                                NotProvided.INSTANCE, NotProvided.INSTANCE, NotProvided.INSTANCE);
            } else {
                final long now = System.currentTimeMillis();
                final Object res = simulateNode.executeWithArguments(frame, receiver, NotProvided.INSTANCE, NotProvided.INSTANCE, NotProvided.INSTANCE, NotProvided.INSTANCE, NotProvided.INSTANCE,
                                NotProvided.INSTANCE, NotProvided.INSTANCE, NotProvided.INSTANCE);

                final long delta = System.currentTimeMillis() - now;

                measureEntry(p, delta, at0Node);
                return res;
            }
        }

        /*
         * Guard Helpers
         */
        protected static final boolean disableWhileMeasuring() {
            return !measure;
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
