package de.hpi.swa.graal.squeak.nodes.plugins;

import java.util.List;
import java.util.Locale;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitiveWithoutFallback;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;

public class LocalePlugin extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return LocalePluginFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveCountry")
    protected abstract static class PrimCountryNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        protected PrimCountryNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final Object doCountry(@SuppressWarnings("unused") final Object receiver) {
            final String country = Locale.getDefault().getCountry();
            if (country.isEmpty()) {
                throw new PrimitiveFailed();
            }
            return method.image.asByteString(country);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveCurrencyNotation")
    protected abstract static class PrimCurrencyNotationNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        protected PrimCurrencyNotationNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object fail(@SuppressWarnings("unused") final Object receiver) {
            throw new PrimitiveFailed(); // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveCurrencySymbol")
    protected abstract static class PrimCurrencySymbolNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        protected PrimCurrencySymbolNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object fail(@SuppressWarnings("unused") final Object receiver) {
            throw new PrimitiveFailed(); // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDaylightSavings")
    protected abstract static class PrimDaylightSavingsNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        protected PrimDaylightSavingsNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object fail(@SuppressWarnings("unused") final Object receiver) {
            throw new PrimitiveFailed(); // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDecimalSymbol")
    protected abstract static class PrimDecimalSymbolNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        protected PrimDecimalSymbolNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object fail(@SuppressWarnings("unused") final Object receiver) {
            throw new PrimitiveFailed(); // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDigitGroupingSymbol")
    protected abstract static class PrimDigitGroupingSymbolNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        protected PrimDigitGroupingSymbolNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object fail(@SuppressWarnings("unused") final Object receiver) {
            throw new PrimitiveFailed(); // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveLanguage")
    protected abstract static class PrimLanguageNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        protected PrimLanguageNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final Object doLanguage(@SuppressWarnings("unused") final Object receiver) {
            final String language = Locale.getDefault().getLanguage();
            if (language.isEmpty()) {
                throw new PrimitiveFailed();
            }
            return method.image.asByteString(language);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveLongDateFormat")
    protected abstract static class PrimLongDateFormatNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        protected PrimLongDateFormatNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object fail(@SuppressWarnings("unused") final Object receiver) {
            throw new PrimitiveFailed(); // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveMeasurementMetric")
    protected abstract static class PrimMeasurementMetricNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        protected PrimMeasurementMetricNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object fail(@SuppressWarnings("unused") final Object receiver) {
            throw new PrimitiveFailed(); // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveShortDateFormat")
    protected abstract static class PrimShortDateFormatNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        protected PrimShortDateFormatNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object fail(@SuppressWarnings("unused") final Object receiver) {
            throw new PrimitiveFailed(); // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveTimeFormat")
    protected abstract static class PrimTimeFormatNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        protected PrimTimeFormatNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object fail(@SuppressWarnings("unused") final Object receiver) {
            throw new PrimitiveFailed(); // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveTimezoneOffset")
    protected abstract static class PrimTimezoneOffsetNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        protected PrimTimezoneOffsetNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object fail(@SuppressWarnings("unused") final Object receiver) {
            throw new PrimitiveFailed(); // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveVMOffsetToUTC")
    protected abstract static class PrimVMOffsetToUTCNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        protected PrimVMOffsetToUTCNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object fail(@SuppressWarnings("unused") final Object receiver) {
            throw new PrimitiveFailed(); // TODO: implement primitive
        }
    }
}
