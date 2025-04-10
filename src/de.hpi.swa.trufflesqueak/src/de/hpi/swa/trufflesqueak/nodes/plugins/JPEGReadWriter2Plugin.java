/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import javax.imageio.ImageIO;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleOptions;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.FORM;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive0;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive3WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive5WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive6WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.util.MiscUtils;
import de.hpi.swa.trufflesqueak.util.VarHandleUtils;

public final class JPEGReadWriter2Plugin extends AbstractPrimitiveFactoryHolder {
    @GenerateNodeFactory
    @SqueakPrimitive(names = "primImageHeight")
    protected abstract static class PrimImageHeightNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "aJPEGDecompressStruct.isByteType()")
        protected static final long doHeight(@SuppressWarnings("unused") final Object receiver, final NativeObject aJPEGDecompressStruct) {
            return VarHandleUtils.getLong(aJPEGDecompressStruct.getByteStorage(), 0);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primImageNumComponents")
    protected abstract static class PrimImageNumComponentsNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @SuppressWarnings("unused")
        @Specialization(guards = "aJPEGDecompressStruct.isByteType()")
        protected static final long doNum(final Object receiver, final NativeObject aJPEGDecompressStruct) {
            return 3L; /* MiscUtils.COLOR_MODEL_32BIT.getNumComponents() */
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primImageWidth")
    protected abstract static class PrimImageWidthNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "aJPEGDecompressStruct.isByteType()")
        protected static final long doWidth(@SuppressWarnings("unused") final Object receiver, final NativeObject aJPEGDecompressStruct) {
            return VarHandleUtils.getLong(aJPEGDecompressStruct.getByteStorage(), 1);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primJPEGCompressStructSize")
    protected abstract static class PrimJPEGCompressStructSizeNode extends AbstractPrimitiveNode implements Primitive0 {
        @Specialization
        protected static final Object doSize(@SuppressWarnings("unused") final Object receiver) {
            return 0L;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primJPEGDecompressStructSize")
    protected abstract static class PrimJPEGDecompressStructSizeNode extends AbstractPrimitiveNode implements Primitive0 {
        @Specialization
        protected static final long doSize(@SuppressWarnings("unused") final Object receiver) {
            return 16L;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primJPEGErrorMgr2StructSize")
    protected abstract static class PrimJPEGErrorMgr2StructSizeNode extends AbstractPrimitiveNode implements Primitive0 {
        @Specialization
        protected static final long doSize(@SuppressWarnings("unused") final Object receiver) {
            return 0L;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primJPEGPluginIsPresent")
    protected abstract static class PrimJPEGPluginIsPresentNode extends AbstractPrimitiveNode implements Primitive0 {
        @Specialization
        protected static final Object doIsPresent(@SuppressWarnings("unused") final Object receiver) {
            return BooleanObject.TRUE;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primJPEGReadHeaderfromByteArrayerrorMgr")
    protected abstract static class PrimJPEGReadHeaderfromByteArrayerrorMgrNode extends AbstractPrimitiveNode implements Primitive3WithFallback {
        @Specialization(guards = {"aJPEGDecompressStruct.isByteType()", "source.isByteType()"})
        protected static final Object doReadHeader(final Object receiver, final NativeObject aJPEGDecompressStruct, final NativeObject source,
                        @SuppressWarnings("unused") final NativeObject aJPEGErrorMgr2Struct) {
            if (TruffleOptions.AOT) { /* ImageIO not yet working properly when AOT-compiled. */
                throw PrimitiveFailed.GENERIC_ERROR;
            }
            final BufferedImage image = readImageOrPrimFail(source);
            VarHandleUtils.putLong(aJPEGDecompressStruct.getByteStorage(), 0, image.getHeight());
            VarHandleUtils.putLong(aJPEGDecompressStruct.getByteStorage(), 1, image.getWidth());
            return receiver;
        }

        @TruffleBoundary
        private static BufferedImage readImageOrPrimFail(final NativeObject source) {
            try {
                return ImageIO.read(new ByteArrayInputStream(source.getByteStorage()));
            } catch (final IOException e) {
                e.printStackTrace();
                throw PrimitiveFailed.GENERIC_ERROR;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primJPEGReadImagefromByteArrayonFormdoDitheringerrorMgr")
    protected abstract static class PrimJPEGReadImagefromByteArrayonFormdoDitheringerrorMgrNode extends AbstractPrimitiveNode implements Primitive5WithFallback {
        @SuppressWarnings("unused")
        @Specialization(guards = {"aJPEGDecompressStruct.isByteType()", "source.isByteType()"})
        protected static final Object doRead(final Object receiver, final NativeObject aJPEGDecompressStruct, final NativeObject source, final PointersObject form,
                        final boolean ditherFlag, final NativeObject aJPEGErrorMgr2Struct,
                        @Bind final Node node,
                        @Cached final AbstractPointersObjectReadNode readNode) {
            if (TruffleOptions.AOT) { /* ImageIO not yet working properly when AOT-compiled. */
                throw PrimitiveFailed.GENERIC_ERROR;
            }
            final NativeObject bits = readNode.executeNative(node, form, FORM.BITS);
            final int width = readNode.executeInt(node, form, FORM.WIDTH);
            final int height = readNode.executeInt(node, form, FORM.HEIGHT);
            final long depth = Math.abs(readNode.executeLong(node, form, FORM.DEPTH));
            if (!bits.isIntType() || depth != 32) {
                throw PrimitiveFailed.andTransferToInterpreter();
            }
            readImageOrPrimFail(source, bits, width, height);
            return receiver;
        }

        @TruffleBoundary
        private static void readImageOrPrimFail(final NativeObject source, final NativeObject bits, final int width, final int height) {
            try {
                final BufferedImage image = ImageIO.read(new ByteArrayInputStream(source.getByteStorage()));
                image.getRGB(0, 0, width, height, bits.getIntStorage(), 0, width);
            } catch (final IOException e) {
                e.printStackTrace();
                throw PrimitiveFailed.GENERIC_ERROR;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primJPEGWriteImageonByteArrayformqualityprogressiveJPEGerrorMgr")
    protected abstract static class PrimJPEGWriteImageonByteArrayformqualityprogressiveJPEGerrorMgrNode extends AbstractPrimitiveNode implements Primitive6WithFallback {
        @SuppressWarnings("unused")
        @Specialization(guards = {"aJPEGCompressStruct.isByteType()", "destination.isByteType()"})
        protected static final long doWrite(final Object receiver, final NativeObject aJPEGCompressStruct, final NativeObject destination, final PointersObject form,
                        final long quality, final boolean progressiveFlag, final NativeObject aJPEGErrorMgr2Struct,
                        @Bind final Node node,
                        @Cached final AbstractPointersObjectReadNode readNode) {
            if (TruffleOptions.AOT) { /* ImageIO not yet working properly when AOT-compiled. */
                throw PrimitiveFailed.GENERIC_ERROR;
            }
            final NativeObject bits = readNode.executeNative(node, form, FORM.BITS);
            final int width = readNode.executeInt(node, form, FORM.WIDTH);
            final int height = readNode.executeInt(node, form, FORM.HEIGHT);
            final long depth = readNode.executeLong(node, form, FORM.DEPTH);
            if (!bits.isIntType() || depth != 32) {
                throw PrimitiveFailed.andTransferToInterpreter();
            }
            final WrappedByteArray output = new WrappedByteArray(destination.getByteStorage());
            final BufferedImage image = MiscUtils.new32BitBufferedImage(bits.getIntStorage(), width, height, false);
            writeImage(output, image);
            return output.count;
        }

        @TruffleBoundary
        private static void writeImage(final WrappedByteArray output, final BufferedImage image) {
            try {
                if (!ImageIO.write(image, "jpeg", output)) {
                    output.count = 0;
                }
            } catch (final IOException e) {
                e.printStackTrace();
                output.count = 0;
            }
        }

        private static final class WrappedByteArray extends OutputStream {
            private final byte[] buf;
            private int count;

            private WrappedByteArray(final byte[] buf) {
                this.buf = buf;
            }

            @Override
            public void write(final int b) throws IOException {
                if (count + 1 - buf.length > 0) {
                    throw new IOException("Buffer too small");
                }
                buf[count] = (byte) b;
                count += 1;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primSupports8BitGrayscaleJPEGs")
    protected abstract static class PrimSupports8BitGrayscaleJPEGsNode extends AbstractPrimitiveNode implements Primitive0 {
        @Specialization
        protected static final boolean doSupports(@SuppressWarnings("unused") final Object receiver) {
            return BooleanObject.FALSE;
        }
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return JPEGReadWriter2PluginFactory.getFactories();
    }
}
