/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import com.oracle.truffle.api.strings.MutableTruffleString;
import com.oracle.truffle.api.strings.TruffleString;
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
        protected static final Object doRead(final Object receiver, final NativeObject pixelLine, final NativeObject formBits, final long formBitsIndex, final long width, @Cached TruffleString.ReadByteNode readByteNode) {
            final int[] ints = formBits.getIntStorage();
            final int bitsStartIndex = (int) formBitsIndex - 1;
            for (int i = 0; i < width; i++) {
                final int pixelIndex = i * 3;
                final int byte0 = pixelLine.readByteTruffleString(pixelIndex, readByteNode);
                final int byte1 = pixelLine.readByteTruffleString(pixelIndex + 1, readByteNode);
                final int byte2 = pixelLine.readByteTruffleString(pixelIndex + 2, readByteNode);
                final int rgb = byte0 & 0xFF | (byte1 & 0xFF) << 8 | (byte2 & 0xFF) << 16;
                ints[bitsStartIndex + i] = rgb == 0 ? 0xFF000001 : rgb | 0xFF000000;
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveWrite24BmpLine")
    protected abstract static class PrimWrite24BmpLineNode extends AbstractBMPPluginNode implements Primitive4WithFallback {
        @Specialization(guards = {"pixelLine.isTruffleStringType()", "formBits.isIntType()", "inBounds(formBitsIndex, width, formBits, pixelLine)"})
        protected static final Object doWrite(final Object receiver, final NativeObject pixelLine, final NativeObject formBits, final long formBitsIndex, final long width, @Cached MutableTruffleString.WriteByteNode writeByteNode) {
            final int[] ints = formBits.getIntStorage();
            final int bitsStartIndex = (int) formBitsIndex - 1;
            for (int i = 0; i < width; i++) {
                final int rgb = ints[bitsStartIndex + i] & 0xFFFFFF;
                final int pixelIndex = i * 3;
                pixelLine.writeByteTruffleString(pixelIndex, (byte) (rgb & 0xFF), writeByteNode);
                pixelLine.writeByteTruffleString(pixelIndex + 1, (byte) (rgb >> 8 & 0xFF), writeByteNode);
                pixelLine.writeByteTruffleString(pixelIndex + 2, (byte) (rgb >> 16 & 0xFF), writeByteNode);
            }
            return receiver;
        }
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return BMPReadWriterPluginFactory.getFactories();
    }
}
