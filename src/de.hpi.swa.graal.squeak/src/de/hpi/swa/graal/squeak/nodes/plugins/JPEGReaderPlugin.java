/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.QuinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.SenaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitiveWithoutFallback;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;

public final class JPEGReaderPlugin extends AbstractPrimitiveFactoryHolder {

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveColorConvertGrayscaleMCU")
    protected abstract static class PrimColorConvertGrayscaleMCUNode extends AbstractPrimitiveNode implements QuinaryPrimitive {

        protected PrimColorConvertGrayscaleMCUNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"bits.isIntType()", "residualArray.isIntType()", "residualArray.getIntLength() == 3"})
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doColor(final Object receiver, final ArrayObject componentArray, final NativeObject bits, final NativeObject residualArray, final long mask) {
            return method.image.jpegReader.primitiveColorConvertGrayscaleMCU(receiver, componentArray, bits, residualArray, mask);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveColorConvertMCU")
    protected abstract static class PrimColorConvertMCUNode extends AbstractPrimitiveNode implements QuinaryPrimitive {

        protected PrimColorConvertMCUNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"componentArray.size() == 3", "bits.isIntType()", "residualArray.isIntType()", "residualArray.getIntLength() == 3"})
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doColor(final Object receiver, final PointersObject componentArray, final NativeObject bits, final NativeObject residualArray, final long mask) {
            return method.image.jpegReader.primitiveColorConvertMCU(receiver, componentArray, bits, residualArray, mask);
        }
    }

    @ImportStatic(JPEGReader.class)
    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDecodeMCU")
    protected abstract static class PrimDecodeMCUNode extends AbstractPrimitiveNode implements SenaryPrimitive {

        protected PrimDecodeMCUNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"sampleBuffer.isIntType()", "sampleBuffer.getIntLength() == DCTSize2", "comp.size() >= MinComponentSize", "dcTableValue.isIntType()", "acTableValue.isIntType()",
                        "jpegStream.size() >= 5"})
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doColor(final Object receiver, final NativeObject sampleBuffer, final PointersObject comp, final NativeObject dcTableValue, final NativeObject acTableValue,
                        final PointersObject jpegStream) {
            return method.image.jpegReader.primitiveDecodeMCU(receiver, sampleBuffer, comp, dcTableValue, acTableValue, jpegStream);
        }
    }

    @ImportStatic(JPEGReader.class)
    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIdctInt")
    protected abstract static class PrimIdctIntNode extends AbstractPrimitiveNode implements TernaryPrimitive {

        protected PrimIdctIntNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"anArray.isIntType()", "anArray.getIntLength() == DCTSize2", "qt.isIntType()", "qt.getIntLength() == DCTSize2"})
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected static final Object doColor(final Object receiver, final NativeObject anArray, final NativeObject qt) {
            return JPEGReader.primitiveIdctInt(receiver, anArray, qt);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primGetModuleName")
    public abstract static class PrimGetModuleNameNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        public PrimGetModuleNameNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final NativeObject doGet(@SuppressWarnings("unused") final Object rcvr) {
            return method.image.asByteString(JPEGReader.moduleName);
        }
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return JPEGReaderPluginFactory.getFactories();
    }
}
