package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.WordsObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;

public class FloatArrayPlugin extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return FloatArrayPluginFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveAddFloatArray", numArguments = 2)
    public static abstract class PrimAddFloatArrayNode extends AbstractPrimitiveNode {

        public PrimAddFloatArrayNode(CompiledMethodObject method) {
            super(method);
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveAddScalar", numArguments = 2)
    public static abstract class PrimAddScalarNode extends AbstractPrimitiveNode {

        public PrimAddScalarNode(CompiledMethodObject method) {
            super(method);
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveAt", numArguments = 2)
    public static abstract class PrimFloatArrayAtNode extends AbstractPrimitiveNode {

        public PrimFloatArrayAtNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected double doAt(WordsObject receiver, long index) {
            return Float.intBitsToFloat(receiver.getInt(((int) index) - 1));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveAtPut", numArguments = 3)
    public static abstract class PrimFloatArrayAtPutNode extends AbstractPrimitiveNode {

        public PrimFloatArrayAtPutNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected double doAtPut(WordsObject receiver, long index, double value) {
            receiver.setInt(((int) index) - 1, Float.floatToIntBits((float) value));
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveDivFloatArray", numArguments = 2)
    public static abstract class PrimDivFloatArrayNode extends AbstractPrimitiveNode {

        public PrimDivFloatArrayNode(CompiledMethodObject method) {
            super(method);
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveDivScalar", numArguments = 2)
    public static abstract class PrimDivScalarNode extends AbstractPrimitiveNode {

        public PrimDivScalarNode(CompiledMethodObject method) {
            super(method);
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveDotProduct", numArguments = 2)
    public static abstract class PrimDotProductNode extends AbstractPrimitiveNode {

        public PrimDotProductNode(CompiledMethodObject method) {
            super(method);
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveEqual", numArguments = 2)
    public static abstract class PrimFloatAarrayEqualNode extends AbstractPrimitiveNode {

        public PrimFloatAarrayEqualNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected boolean doEqual(WordsObject receiver, WordsObject other) {
            int[] words = receiver.getWords();
            int wordsLength = words.length;
            int[] otherWords = other.getWords();
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
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveHashArray")
    public static abstract class PrimHashArrayNode extends AbstractPrimitiveNode {

        public PrimHashArrayNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected long doHash(WordsObject receiver) {
            int[] words = receiver.getWords();
            long hash = 0;
            for (int word : words) {
                hash += word;
            }
            return hash & 0x1fffffff;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveLength", numArguments = 2)
    public static abstract class PrimFloatArrayLengthNode extends AbstractPrimitiveNode {

        public PrimFloatArrayLengthNode(CompiledMethodObject method) {
            super(method);
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveMulFloatArray", numArguments = 2)
    public static abstract class PrimMulFloatArrayNode extends AbstractPrimitiveNode {

        public PrimMulFloatArrayNode(CompiledMethodObject method) {
            super(method);
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveMulScalar", numArguments = 2)
    public static abstract class PrimMulScalarNode extends AbstractPrimitiveNode {

        public PrimMulScalarNode(CompiledMethodObject method) {
            super(method);
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveNormalize", numArguments = 2)
    public static abstract class PrimFloatArrayNormalizeNode extends AbstractPrimitiveNode {

        public PrimFloatArrayNormalizeNode(CompiledMethodObject method) {
            super(method);
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSubFloatArray", numArguments = 2)
    public static abstract class PrimSubFloatArrayNode extends AbstractPrimitiveNode {

        public PrimSubFloatArrayNode(CompiledMethodObject method) {
            super(method);
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSubScalar", numArguments = 2)
    public static abstract class PrimSubScalar extends AbstractPrimitiveNode {

        public PrimSubScalar(CompiledMethodObject method) {
            super(method);
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSum")
    public static abstract class PrimFloatArraySumNode extends AbstractPrimitiveNode {

        public PrimFloatArraySumNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected double doSum(WordsObject receiver) {
            int[] words = receiver.getWords();
            double sum = 0;
            for (int word : words) {
                sum += Float.intBitsToFloat(word);
            }
            return sum;
        }
    }
}
