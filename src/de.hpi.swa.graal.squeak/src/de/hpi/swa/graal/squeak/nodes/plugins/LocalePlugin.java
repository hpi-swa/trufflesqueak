package de.hpi.swa.graal.squeak.nodes.plugins;

import java.util.List;
import java.util.Locale;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;

public class LocalePlugin extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return LocalePluginFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveCountry")
    protected abstract static class PrimCountryNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        protected PrimCountryNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final Object doCountry(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            final String country = Locale.getDefault().getCountry();
            if (country.isEmpty()) {
                throw new PrimitiveFailed();
            }
            return code.image.wrap(country);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveCurrencyNotation")
    protected abstract static class PrimCurrencyNotationNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        protected PrimCurrencyNotationNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object fail(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            throw new PrimitiveFailed(); // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveCurrencySymbol")
    protected abstract static class PrimCurrencySymbolNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        protected PrimCurrencySymbolNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object fail(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            throw new PrimitiveFailed(); // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveDaylightSavings")
    protected abstract static class PrimDaylightSavingsNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        protected PrimDaylightSavingsNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object fail(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            throw new PrimitiveFailed(); // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveDecimalSymbol")
    protected abstract static class PrimDecimalSymbolNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        protected PrimDecimalSymbolNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object fail(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            throw new PrimitiveFailed(); // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveDigitGroupingSymbol")
    protected abstract static class PrimDigitGroupingSymbolNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        protected PrimDigitGroupingSymbolNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object fail(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            throw new PrimitiveFailed(); // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveLanguage")
    protected abstract static class PrimLanguageNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        protected PrimLanguageNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final Object doLanguage(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            final String language = Locale.getDefault().getLanguage();
            if (language.isEmpty()) {
                throw new PrimitiveFailed();
            }
            return code.image.wrap(language);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveLongDateFormat")
    protected abstract static class PrimLongDateFormatNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        protected PrimLongDateFormatNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object fail(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            throw new PrimitiveFailed(); // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveMeasurementMetric")
    protected abstract static class PrimMeasurementMetricNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        protected PrimMeasurementMetricNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object fail(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            throw new PrimitiveFailed(); // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveShortDateFormat")
    protected abstract static class PrimShortDateFormatNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        protected PrimShortDateFormatNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object fail(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            throw new PrimitiveFailed(); // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveTimeFormat")
    protected abstract static class PrimTimeFormatNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        protected PrimTimeFormatNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object fail(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            throw new PrimitiveFailed(); // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveTimezoneOffset")
    protected abstract static class PrimTimezoneOffsetNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        protected PrimTimezoneOffsetNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object fail(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            throw new PrimitiveFailed(); // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveVMOffsetToUTC")
    protected abstract static class PrimVMOffsetToUTCNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        protected PrimVMOffsetToUTCNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object fail(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            throw new PrimitiveFailed(); // TODO: implement primitive
        }
    }
}
