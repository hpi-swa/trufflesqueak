/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive0;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive4WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive5WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive2WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;

public final class JPEGReaderPlugin extends AbstractPrimitiveFactoryHolder {
    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveColorConvertGrayscaleMCU")
    protected abstract static class PrimColorConvertGrayscaleMCUNode extends AbstractPrimitiveNode implements Primitive4WithFallback {
        @Specialization(guards = {"bits.isIntType()", "residualArray.isIntType()", "residualArray.getIntLength() == 3"})
        protected final Object doColor(final Object receiver, final ArrayObject componentArray, final NativeObject bits, final NativeObject residualArray, final long mask) {
            getContext().jpegReader.primitiveColorConvertGrayscaleMCU(componentArray, bits, residualArray, mask);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveColorConvertMCU")
    protected abstract static class PrimColorConvertMCUNode extends AbstractPrimitiveNode implements Primitive4WithFallback {
        @Specialization(guards = {"componentArray.size() == 3", "bits.isIntType()", "residualArray.isIntType()", "residualArray.getIntLength() == 3"})
        protected final Object doColor(final Object receiver, final PointersObject componentArray, final NativeObject bits, final NativeObject residualArray, final long mask) {
            getContext().jpegReader.primitiveColorConvertMCU(componentArray, bits, residualArray, mask);
            return receiver;
        }
    }

    @ImportStatic(JPEGReader.class)
    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDecodeMCU")
    protected abstract static class PrimDecodeMCUNode extends AbstractPrimitiveNode implements Primitive5WithFallback {
        @Specialization(guards = {"sampleBuffer.isIntType()", "sampleBuffer.getIntLength() == DCTSize2", "comp.size() >= MinComponentSize", "dcTableValue.isIntType()", "acTableValue.isIntType()",
                        "jpegStream.size() >= 5"})
        protected final Object doColor(final Object receiver, final NativeObject sampleBuffer, final PointersObject comp, final NativeObject dcTableValue, final NativeObject acTableValue,
                        final PointersObject jpegStream) {
            getContext().jpegReader.primitiveDecodeMCU(sampleBuffer, comp, dcTableValue, acTableValue, jpegStream);
            return receiver;
        }
    }

    @ImportStatic(JPEGReader.class)
    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIdctInt")
    protected abstract static class PrimIdctIntNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = {"anArray.isIntType()", "anArray.getIntLength() == DCTSize2", "qt.isIntType()", "qt.getIntLength() == DCTSize2"})
        protected static final Object doColor(final Object receiver, final NativeObject anArray, final NativeObject qt) {
            JPEGReader.primitiveIdctInt(anArray, qt);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primGetModuleName")
    public abstract static class PrimGetModuleNameNode extends AbstractPrimitiveNode implements Primitive0 {
        @Specialization
        protected final NativeObject doGet(@SuppressWarnings("unused") final Object rcvr) {
            return getContext().asByteString(JPEGReader.moduleName);
        }
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return JPEGReaderPluginFactory.getFactories();
    }
}
