package de.hpi.swa.graal.squeak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;

public final class BMPReadWriterPlugin extends AbstractPrimitiveFactoryHolder {

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveRead24BmpLine")
    protected abstract static class PrimRead24BmpLineNode extends AbstractPrimitiveNode {

        protected PrimRead24BmpLineNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = {"pixelLine.isByteType()", "formBits.isIntType()"})
        protected static final Object doRead(final PointersObject receiver, final NativeObject pixelLine, final NativeObject formBits, final long formBitsIndex, final long width) {
            final byte[] bytes = pixelLine.getByteStorage();
            final int[] ints = formBits.getIntStorage();
            int pixelIndex = 0;
            int bitsIndex = (int) formBitsIndex - 1;
            int rgb = 0;
            for (int j = 0; j < width; j++) {
                rgb = (bytes[pixelIndex++] & 0xFF) | ((bytes[pixelIndex++] & 0xFF) << 8) | ((bytes[pixelIndex++] & 0xFF) << 16);
                if (rgb == 0) {
                    ints[bitsIndex++] = 0xFF000001;
                } else {
                    ints[bitsIndex++] = rgb | 0xFF000000;
                }
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveWrite24BmpLine")
    protected abstract static class PrimWrite24BmpLineNode extends AbstractPrimitiveNode {

        protected PrimWrite24BmpLineNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = {"pixelLine.isByteType()", "formBits.isIntType()"})
        protected static final Object doWrite(final AbstractSqueakObject receiver, final NativeObject pixelLine, final NativeObject formBits, final long formBitsIndex, final long width) {
            final byte[] bytes = pixelLine.getByteStorage();
            final int[] ints = formBits.getIntStorage();
            int pixelIndex = 0;
            int bitsIndex = (int) formBitsIndex - 1;
            int rgb = 0;
            for (int j = 0; j < width; j++) {
                rgb = ints[bitsIndex++] & 0xFFFFFF;
                bytes[pixelIndex++] = (byte) (rgb & 0xFF);
                bytes[pixelIndex++] = (byte) ((rgb >> 8) & 0xFF);
                bytes[pixelIndex++] = (byte) ((rgb >> 16) & 0xFF);
            }
            return receiver;
        }
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return BMPReadWriterPluginFactory.getFactories();
    }
}
