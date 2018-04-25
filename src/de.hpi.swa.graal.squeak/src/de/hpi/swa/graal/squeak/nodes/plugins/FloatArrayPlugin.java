package de.hpi.swa.graal.squeak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;

public class FloatArrayPlugin extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return FloatArrayPluginFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveAddFloatArray", numArguments = 2) // TODO: implement primitive
    public abstract static class PrimAddFloatArrayNode extends AbstractPrimitiveNode {

        public PrimAddFloatArrayNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveAddScalar", numArguments = 2) // TODO: implement primitive
    public abstract static class PrimAddScalarNode extends AbstractPrimitiveNode {

        public PrimAddScalarNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveAt", numArguments = 2)
    public abstract static class PrimFloatArrayAtNode extends AbstractPrimitiveNode {

        public PrimFloatArrayAtNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected double doAt(final NativeObject receiver, final long index) {
            return Float.intBitsToFloat(receiver.getInt(((int) index) - 1));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveAtPut", numArguments = 3)
    public abstract static class PrimFloatArrayAtPutNode extends AbstractPrimitiveNode {

        public PrimFloatArrayAtPutNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected double doDouble(final NativeObject receiver, final long index, final double value) {
            receiver.setInt(((int) index) - 1, Float.floatToRawIntBits((float) value));
            return value;
        }

        @Specialization
        protected double doFloat(final NativeObject receiver, final long index, final FloatObject value) {
            return doDouble(receiver, index, value.getValue());
        }

        @Specialization
        protected double doFloat(final NativeObject receiver, final long index, final long value) {
            return doDouble(receiver, index, value);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveDivFloatArray", numArguments = 2) // TODO: implement primitive
    public abstract static class PrimDivFloatArrayNode extends AbstractPrimitiveNode {

        public PrimDivFloatArrayNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveDivScalar", numArguments = 2) // TODO: implement primitive
    public abstract static class PrimDivScalarNode extends AbstractPrimitiveNode {

        public PrimDivScalarNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveDotProduct", numArguments = 2) // TODO: implement primitive
    public abstract static class PrimDotProductNode extends AbstractPrimitiveNode {

        public PrimDotProductNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveEqual", numArguments = 2)
    public abstract static class PrimFloatArrayEqualNode extends AbstractPrimitiveNode {

        public PrimFloatArrayEqualNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected boolean doEqual(final NativeObject receiver, final NativeObject other) {
            final int[] words = receiver.getWords();
            final int wordsLength = words.length;
            final int[] otherWords = other.getWords();
            if (wordsLength != otherWords.length) {
                return code.image.sqFalse;
            }
            for (int i = 0; i < wordsLength; i++) {
                if (words[i] != otherWords[i]) {
                    return code.image.sqFalse;
                }
            }
            return code.image.sqTrue;
        }

        /*
         * Specialization for quick nil checks.
         */
        @SuppressWarnings("unused")
        @Specialization
        protected boolean doNilCase(final NativeObject receiver, final NilObject other) {
            return code.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveHashArray")
    public abstract static class PrimHashArrayNode extends AbstractPrimitiveNode {

        public PrimHashArrayNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected long doHash(final NativeObject receiver) {
            final int[] words = receiver.getWords();
            long hash = 0;
            for (int word : words) {
                hash += word;
            }
            return hash & 0x1fffffff;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveLength", numArguments = 2) // TODO: implement primitive
    public abstract static class PrimFloatArrayLengthNode extends AbstractPrimitiveNode {

        public PrimFloatArrayLengthNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveMulFloatArray", numArguments = 2) // TODO: implement primitive
    public abstract static class PrimMulFloatArrayNode extends AbstractPrimitiveNode {

        public PrimMulFloatArrayNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveMulScalar", numArguments = 2) // TODO: implement primitive
    public abstract static class PrimMulScalarNode extends AbstractPrimitiveNode {

        public PrimMulScalarNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveNormalize", numArguments = 2) // TODO: implement primitive
    public abstract static class PrimFloatArrayNormalizeNode extends AbstractPrimitiveNode {

        public PrimFloatArrayNormalizeNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSubFloatArray", numArguments = 2) // TODO: implement primitive
    public abstract static class PrimSubFloatArrayNode extends AbstractPrimitiveNode {

        public PrimSubFloatArrayNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSubScalar", numArguments = 2) // TODO: implement primitive
    public abstract static class PrimSubScalarNode extends AbstractPrimitiveNode {

        public PrimSubScalarNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSum")
    public abstract static class PrimFloatArraySumNode extends AbstractPrimitiveNode {

        public PrimFloatArraySumNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected double doSum(final NativeObject receiver) {
            final int[] words = receiver.getWords();
            double sum = 0;
            for (int word : words) {
                sum += Float.intBitsToFloat(word);
            }
            return sum;
        }
    }
}
