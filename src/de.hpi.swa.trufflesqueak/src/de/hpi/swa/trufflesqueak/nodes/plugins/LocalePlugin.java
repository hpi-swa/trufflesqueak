/*
 * Copyright (c) 2017-2022 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2022 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.text.NumberFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;

public class LocalePlugin extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return LocalePluginFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveCountry")
    protected abstract static class PrimCountryNode extends AbstractPrimitiveNode {
        @Specialization
        protected final NativeObject doCountry(@SuppressWarnings("unused") final Object receiver) {
            final String country = getCountry();
            if (country.isEmpty()) {
                throw PrimitiveFailed.andTransferToInterpreter();
            } else {
                return getContext().asByteString(country);
            }
        }

        @TruffleBoundary
        private static String getCountry() {
            return Locale.getDefault().getCountry();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveCurrencyNotation")
    protected abstract static class PrimCurrencyNotationNode extends AbstractPrimitiveNode {
        @Specialization
        protected static final Object fail(@SuppressWarnings("unused") final Object receiver) {
            throw PrimitiveFailed.GENERIC_ERROR; // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveCurrencySymbol")
    protected abstract static class PrimCurrencySymbolNode extends AbstractPrimitiveNode {
        @Specialization
        protected final NativeObject doCurrencySymbol(@SuppressWarnings("unused") final Object receiver) {
            return getContext().asByteString(getCurrencyCode());
        }

        @TruffleBoundary
        private static String getCurrencyCode() {
            return NumberFormat.getCurrencyInstance().getCurrency().getCurrencyCode();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDaylightSavings")
    protected abstract static class PrimDaylightSavingsNode extends AbstractPrimitiveNode {
        @Specialization
        protected static final boolean doDaylightSavings(@SuppressWarnings("unused") final Object receiver) {
            return BooleanObject.wrap(inDaylightTime());
        }

        @TruffleBoundary
        private static boolean inDaylightTime() {
            return TimeZone.getDefault().inDaylightTime(new Date());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDecimalSymbol")
    protected abstract static class PrimDecimalSymbolNode extends AbstractPrimitiveNode {
        @Specialization
        protected static final Object fail(@SuppressWarnings("unused") final Object receiver) {
            throw PrimitiveFailed.GENERIC_ERROR; // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDigitGroupingSymbol")
    protected abstract static class PrimDigitGroupingSymbolNode extends AbstractPrimitiveNode {
        @Specialization
        protected static final Object fail(@SuppressWarnings("unused") final Object receiver) {
            throw PrimitiveFailed.GENERIC_ERROR; // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveLanguage")
    protected abstract static class PrimLanguageNode extends AbstractPrimitiveNode {
        @Specialization
        protected final NativeObject doLanguage(@SuppressWarnings("unused") final Object receiver) {
            final String language = getLanguageString();
            if (language.isEmpty()) {
                throw PrimitiveFailed.andTransferToInterpreter();
            } else {
                return getContext().asByteString(language);
            }
        }

        @TruffleBoundary
        private static String getLanguageString() {
            return Locale.getDefault().getLanguage();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveLongDateFormat")
    protected abstract static class PrimLongDateFormatNode extends AbstractPrimitiveNode {
        @Specialization
        protected static final Object fail(@SuppressWarnings("unused") final Object receiver) {
            throw PrimitiveFailed.GENERIC_ERROR; // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveMeasurementMetric")
    protected abstract static class PrimMeasurementMetricNode extends AbstractPrimitiveNode {
        @Specialization
        protected static final Object fail(@SuppressWarnings("unused") final Object receiver) {
            throw PrimitiveFailed.GENERIC_ERROR; // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveShortDateFormat")
    protected abstract static class PrimShortDateFormatNode extends AbstractPrimitiveNode {
        @Specialization
        protected static final Object fail(@SuppressWarnings("unused") final Object receiver) {
            throw PrimitiveFailed.GENERIC_ERROR; // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveTimeFormat")
    protected abstract static class PrimTimeFormatNode extends AbstractPrimitiveNode {
        @Specialization
        protected static final Object fail(@SuppressWarnings("unused") final Object receiver) {
            throw PrimitiveFailed.GENERIC_ERROR; // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveTimezoneOffset")
    protected abstract static class PrimTimezoneOffsetNode extends AbstractPrimitiveNode {
        @Specialization
        protected static final long doTimezoneOffset(@SuppressWarnings("unused") final Object receiver) {
            return getRawOffset() / 60 / 1000;
        }

        @TruffleBoundary
        private static int getRawOffset() {
            return TimeZone.getDefault().getRawOffset();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveVMOffsetToUTC")
    protected abstract static class PrimVMOffsetToUTCNode extends AbstractPrimitiveNode {
        @Specialization
        protected static final long doVMOffsetToUTC(@SuppressWarnings("unused") final Object receiver) {
            return 0L;
        }
    }
}
