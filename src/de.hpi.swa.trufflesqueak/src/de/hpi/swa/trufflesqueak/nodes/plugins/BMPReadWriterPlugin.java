/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive4WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;

public final class BMPReadWriterPlugin extends AbstractPrimitiveFactoryHolder {

    protected abstract static class AbstractBMPPluginNode extends AbstractPrimitiveNode {
        protected static final boolean inBounds(final long formBitsIndex, final long width, final NativeObject formBits, final NativeObject pixelLine) {
            return formBitsIndex + width <= formBits.getIntLength() && width * 3 <= pixelLine.getByteLength();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveRead24BmpLine")
    protected abstract static class PrimRead24BmpLineNode extends AbstractBMPPluginNode implements Primitive4WithFallback {
        @Specialization(guards = {"pixelLine.isTruffleStringType()", "formBits.isIntType()", "inBounds(formBitsIndex, width, formBits, pixelLine)"})
        protected static final Object doRead(final Object receiver, final NativeObject pixelLine, final NativeObject formBits, final long formBitsIndex, final long width) {
            final byte[] bytes = pixelLine.getByteStorage();
            final int[] ints = formBits.getIntStorage();
            final int bitsStartIndex = (int) formBitsIndex - 1;
            for (int i = 0; i < width; i++) {
                final int pixelIndex = i * 3;
                final int rgb = bytes[pixelIndex] & 0xFF | (bytes[pixelIndex + 1] & 0xFF) << 8 | (bytes[pixelIndex + 2] & 0xFF) << 16;
                ints[bitsStartIndex + i] = rgb == 0 ? 0xFF000001 : rgb | 0xFF000000;
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveWrite24BmpLine")
    protected abstract static class PrimWrite24BmpLineNode extends AbstractBMPPluginNode implements Primitive4WithFallback {
        @Specialization(guards = {"pixelLine.isTruffleStringType()", "formBits.isIntType()", "inBounds(formBitsIndex, width, formBits, pixelLine)"})
        protected static final Object doWrite(final Object receiver, final NativeObject pixelLine, final NativeObject formBits, final long formBitsIndex, final long width) {
            final byte[] bytes = pixelLine.getByteStorage();
            final int[] ints = formBits.getIntStorage();
            final int bitsStartIndex = (int) formBitsIndex - 1;
            for (int i = 0; i < width; i++) {
                final int rgb = ints[bitsStartIndex + i] & 0xFFFFFF;
                final int pixelIndex = i * 3;
                bytes[pixelIndex] = (byte) (rgb & 0xFF);
                bytes[pixelIndex + 1] = (byte) (rgb >> 8 & 0xFF);
                bytes[pixelIndex + 2] = (byte) (rgb >> 16 & 0xFF);
            }
            return receiver;
        }
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return BMPReadWriterPluginFactory.getFactories();
    }
}
