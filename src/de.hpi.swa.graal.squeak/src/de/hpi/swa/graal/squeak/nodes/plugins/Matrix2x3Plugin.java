package de.hpi.swa.graal.squeak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.profiles.ValueProfile;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;

public class Matrix2x3Plugin extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return Matrix2x3PluginFactory.getFactories();
    }

    protected abstract static class AbstractMatrix2x3PrimitiveNode extends AbstractPrimitiveNode {
        @CompilationFinal protected static final int MATRIX_SIZE = 6;
        @CompilationFinal protected static final int FLOAT_ONE = Float.floatToIntBits(1.0F);

        @CompilationFinal private final ValueProfile storageType = ValueProfile.createClassProfile();

        public AbstractMatrix2x3PrimitiveNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        protected final int[] loadMatrix(final NativeObject object) {
            final int[] ints = object.getIntStorage(storageType);
            if (ints.length != MATRIX_SIZE) {
                throw new PrimitiveFailed();
            }
            return ints;
        }

        @ExplodeLoop
        protected final float[] loadMatrixAsFloat(final NativeObject object) {
            final int[] ints = loadMatrix(object);
            final float[] floats = new float[MATRIX_SIZE];
            for (int i = 0; i < MATRIX_SIZE; i++) {
                floats[i] = Float.intBitsToFloat(ints[i]);
            }
            return floats;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveComposeMatrix")
    protected abstract static class PrimComposeMatrixNode extends AbstractMatrix2x3PrimitiveNode {
        protected PrimComposeMatrixNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = {"receiver.isIntType()", "aTransformation.isIntType()", "result.isIntType()"})
        protected final Object doCompose(final NativeObject receiver, final NativeObject aTransformation, final NativeObject result) {
            final float[] m1 = loadMatrixAsFloat(receiver);
            final float[] m2 = loadMatrixAsFloat(aTransformation);
            final int[] m3 = loadMatrix(result);
            m3[0] = Float.floatToRawIntBits((m1[0] * m2[0]) + (m1[1] * m2[3]));
            m3[1] = Float.floatToRawIntBits((m1[0] * m2[1]) + (m1[1] * m2[4]));
            m3[2] = Float.floatToRawIntBits((m1[0] * m2[2]) + (m1[1] * m2[5]) + m1[2]);
            m3[3] = Float.floatToRawIntBits((m1[3] * m2[0]) + (m1[4] * m2[3]));
            m3[4] = Float.floatToRawIntBits((m1[3] * m2[1]) + (m1[4] * m2[4]));
            m3[5] = Float.floatToRawIntBits((m1[3] * m2[2]) + (m1[4] * m2[5]) + m1[5]);
            return result;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveInvertPoint")
    protected abstract static class PrimInvertPointNode extends AbstractMatrix2x3PrimitiveNode {
        protected PrimInvertPointNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected static final Object fail(@SuppressWarnings("unused") final Object receiver) {
            throw new PrimitiveFailed(); // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveInvertRectInto")
    protected abstract static class PrimInvertRectIntoNode extends AbstractMatrix2x3PrimitiveNode {
        protected PrimInvertRectIntoNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected static final Object fail(@SuppressWarnings("unused") final Object receiver) {
            throw new PrimitiveFailed(); // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveIsIdentity")
    protected abstract static class PrimIsIdentityNode extends AbstractMatrix2x3PrimitiveNode {
        protected PrimIsIdentityNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = "receiver.isIntType()")
        protected final Object doIdentity(final NativeObject receiver) {
            final int[] ints = loadMatrix(receiver);
            if (ints[0] == FLOAT_ONE && ints[1] == 0 && ints[2] == 0 && ints[3] == 0 && ints[4] == FLOAT_ONE && ints[5] == 0) {
                return code.image.sqTrue;
            } else {
                return code.image.sqFalse;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveIsPureTranslation")
    protected abstract static class PrimIsPureTranslationNode extends AbstractMatrix2x3PrimitiveNode {
        protected PrimIsPureTranslationNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = "receiver.isIntType()")
        protected final Object doPure(final NativeObject receiver) {
            final int[] ints = loadMatrix(receiver);
            if (ints[0] == FLOAT_ONE && ints[1] == 0 && ints[3] == 0 && ints[4] == FLOAT_ONE) {
                return code.image.sqTrue;
            } else {
                return code.image.sqFalse;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveTransformPoint")
    protected abstract static class PrimTransformPointNode extends AbstractMatrix2x3PrimitiveNode {
        protected PrimTransformPointNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected static final Object fail(@SuppressWarnings("unused") final Object receiver) {
            throw new PrimitiveFailed(); // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveTransformRectInto")
    protected abstract static class PrimTransformRectIntoNode extends AbstractMatrix2x3PrimitiveNode {
        protected PrimTransformRectIntoNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected static final Object fail(@SuppressWarnings("unused") final Object receiver) {
            throw new PrimitiveFailed(); // TODO: implement primitive
        }
    }
}
