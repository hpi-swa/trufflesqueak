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
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;

public class LocalePlugin extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return LocalePluginFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveCountry")
    protected abstract static class PrimCountryNode extends AbstractPrimitiveNode {
        protected PrimCountryNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final Object doCountry(@SuppressWarnings("unused") final Object receiver) {
            final String country = Locale.getDefault().getCountry();
            if (country.isEmpty()) {
                throw new PrimitiveFailed();
            }
            return code.image.wrap(country);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveCurrencyNotation")
    protected abstract static class PrimCurrencyNotationNode extends AbstractPrimitiveNode {
        protected PrimCurrencyNotationNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected static final Object fail(@SuppressWarnings("unused") final Object receiver) {
            throw new PrimitiveFailed(); // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveCurrencySymbol")
    protected abstract static class PrimCurrencySymbolNode extends AbstractPrimitiveNode {
        protected PrimCurrencySymbolNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected static final Object fail(@SuppressWarnings("unused") final Object receiver) {
            throw new PrimitiveFailed(); // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveDaylightSavings")
    protected abstract static class PrimDaylightSavingsNode extends AbstractPrimitiveNode {
        protected PrimDaylightSavingsNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected static final Object fail(@SuppressWarnings("unused") final Object receiver) {
            throw new PrimitiveFailed(); // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveDecimalSymbol")
    protected abstract static class PrimDecimalSymbolNode extends AbstractPrimitiveNode {
        protected PrimDecimalSymbolNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected static final Object fail(@SuppressWarnings("unused") final Object receiver) {
            throw new PrimitiveFailed(); // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveDigitGroupingSymbol")
    protected abstract static class PrimDigitGroupingSymbolNode extends AbstractPrimitiveNode {
        protected PrimDigitGroupingSymbolNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected static final Object fail(@SuppressWarnings("unused") final Object receiver) {
            throw new PrimitiveFailed(); // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveLanguage")
    protected abstract static class PrimLanguageNode extends AbstractPrimitiveNode {
        protected PrimLanguageNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final Object doLanguage(@SuppressWarnings("unused") final Object receiver) {
            final String language = Locale.getDefault().getLanguage();
            if (language.isEmpty()) {
                throw new PrimitiveFailed();
            }
            return code.image.wrap(language);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveLongDateFormat")
    protected abstract static class PrimLongDateFormatNode extends AbstractPrimitiveNode {
        protected PrimLongDateFormatNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected static final Object fail(@SuppressWarnings("unused") final Object receiver) {
            throw new PrimitiveFailed(); // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveMeasurementMetric")
    protected abstract static class PrimMeasurementMetricNode extends AbstractPrimitiveNode {
        protected PrimMeasurementMetricNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected static final Object fail(@SuppressWarnings("unused") final Object receiver) {
            throw new PrimitiveFailed(); // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveShortDateFormat")
    protected abstract static class PrimShortDateFormatNode extends AbstractPrimitiveNode {
        protected PrimShortDateFormatNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected static final Object fail(@SuppressWarnings("unused") final Object receiver) {
            throw new PrimitiveFailed(); // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveTimeFormat")
    protected abstract static class PrimTimeFormatNode extends AbstractPrimitiveNode {
        protected PrimTimeFormatNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected static final Object fail(@SuppressWarnings("unused") final Object receiver) {
            throw new PrimitiveFailed(); // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveTimezoneOffset")
    protected abstract static class PrimTimezoneOffsetNode extends AbstractPrimitiveNode {
        protected PrimTimezoneOffsetNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected static final Object fail(@SuppressWarnings("unused") final Object receiver) {
            throw new PrimitiveFailed(); // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveVMOffsetToUTC")
    protected abstract static class PrimVMOffsetToUTCNode extends AbstractPrimitiveNode {
        protected PrimVMOffsetToUTCNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected static final Object fail(@SuppressWarnings("unused") final Object receiver) {
            throw new PrimitiveFailed(); // TODO: implement primitive
        }
    }
}
