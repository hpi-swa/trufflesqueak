/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveInterfaces.QuinaryPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveInterfaces.SenaryPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitiveWithoutFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;

public final class JPEGReaderPlugin extends AbstractPrimitiveFactoryHolder {
    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveColorConvertGrayscaleMCU")
    protected abstract static class PrimColorConvertGrayscaleMCUNode extends AbstractPrimitiveNode implements QuinaryPrimitive {
        @Specialization(guards = {"bits.isIntType()", "residualArray.isIntType()", "residualArray.getIntLength() == 3"})
        protected static final Object doColor(final Object receiver, final ArrayObject componentArray, final NativeObject bits, final NativeObject residualArray, final long mask,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            image.jpegReader.primitiveColorConvertGrayscaleMCU(componentArray, bits, residualArray, mask);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveColorConvertMCU")
    protected abstract static class PrimColorConvertMCUNode extends AbstractPrimitiveNode implements QuinaryPrimitive {
        @Specialization(guards = {"componentArray.size() == 3", "bits.isIntType()", "residualArray.isIntType()", "residualArray.getIntLength() == 3"})
        protected static final Object doColor(final Object receiver, final PointersObject componentArray, final NativeObject bits, final NativeObject residualArray, final long mask,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            image.jpegReader.primitiveColorConvertMCU(componentArray, bits, residualArray, mask);
            return receiver;
        }
    }

    @ImportStatic(JPEGReader.class)
    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDecodeMCU")
    protected abstract static class PrimDecodeMCUNode extends AbstractPrimitiveNode implements SenaryPrimitive {
        @Specialization(guards = {"sampleBuffer.isIntType()", "sampleBuffer.getIntLength() == DCTSize2", "comp.size() >= MinComponentSize", "dcTableValue.isIntType()", "acTableValue.isIntType()",
                        "jpegStream.size() >= 5"})
        protected static final Object doColor(final Object receiver, final NativeObject sampleBuffer, final PointersObject comp, final NativeObject dcTableValue, final NativeObject acTableValue,
                        final PointersObject jpegStream,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            image.jpegReader.primitiveDecodeMCU(sampleBuffer, comp, dcTableValue, acTableValue, jpegStream);
            return receiver;
        }
    }

    @ImportStatic(JPEGReader.class)
    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIdctInt")
    protected abstract static class PrimIdctIntNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        @Specialization(guards = {"anArray.isIntType()", "anArray.getIntLength() == DCTSize2", "qt.isIntType()", "qt.getIntLength() == DCTSize2"})
        protected static final Object doColor(final Object receiver, final NativeObject anArray, final NativeObject qt) {
            JPEGReader.primitiveIdctInt(anArray, qt);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primGetModuleName")
    public abstract static class PrimGetModuleNameNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        @Specialization
        protected static final NativeObject doGet(@SuppressWarnings("unused") final Object rcvr,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return image.asByteString(JPEGReader.moduleName);
        }
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return JPEGReaderPluginFactory.getFactories();
    }
}
