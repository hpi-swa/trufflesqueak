/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.primitives;

import com.oracle.truffle.api.dsl.Fallback;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;

public final class PrimitiveInterfaces {
    public interface AbstractPrimitive {
        int getNumArguments();
    }

    public interface UnaryPrimitiveWithoutFallback extends AbstractPrimitive {
        @Override
        default int getNumArguments() {
            return 1;
        }
    }

    public interface UnaryPrimitive extends UnaryPrimitiveWithoutFallback {
        @Fallback
        default Object doPrimitiveFail(@SuppressWarnings("unused") final Object arg1) {
            throw PrimitiveFailed.GENERIC_ERROR;
        }
    }

    public interface BinaryPrimitiveWithoutFallback extends AbstractPrimitive {
        @Override
        default int getNumArguments() {
            return 2;
        }
    }

    public interface BinaryPrimitive extends BinaryPrimitiveWithoutFallback {
        @SuppressWarnings("unused")
        @Fallback
        default Object doPrimitiveFail(final Object arg1, final Object arg2) {
            throw PrimitiveFailed.GENERIC_ERROR;
        }
    }

    public interface TernaryPrimitiveWithoutFallback extends AbstractPrimitive {
        @Override
        default int getNumArguments() {
            return 3;
        }
    }

    public interface TernaryPrimitive extends TernaryPrimitiveWithoutFallback {
        @SuppressWarnings("unused")
        @Fallback
        default Object doPrimitiveFail(final Object arg1, final Object arg2, final Object arg3) {
            throw PrimitiveFailed.GENERIC_ERROR;
        }
    }

    public interface QuaternaryPrimitiveWithoutFallback extends AbstractPrimitive {
        @Override
        default int getNumArguments() {
            return 4;
        }
    }

    public interface QuaternaryPrimitive extends QuaternaryPrimitiveWithoutFallback {
        @SuppressWarnings("unused")
        @Fallback
        default Object doPrimitiveFail(final Object arg1, final Object arg2, final Object arg3, final Object arg4) {
            throw PrimitiveFailed.GENERIC_ERROR;
        }
    }

    public interface QuinaryPrimitive extends AbstractPrimitive {
        @Override
        default int getNumArguments() {
            return 5;
        }

        @SuppressWarnings("unused")
        @Fallback
        default Object doPrimitiveFail(final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5) {
            throw PrimitiveFailed.GENERIC_ERROR;
        }
    }

    public interface SenaryPrimitive extends AbstractPrimitive {
        @Override
        default int getNumArguments() {
            return 6;
        }

        @SuppressWarnings("unused")
        @Fallback
        default Object doPrimitiveFail(final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6) {
            throw PrimitiveFailed.GENERIC_ERROR;
        }
    }

    public interface SeptenaryPrimitive extends AbstractPrimitive {
        @Override
        default int getNumArguments() {
            return 7;
        }

        @SuppressWarnings("unused")
        @Fallback
        default Object doPrimitiveFail(final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6, final Object arg7) {
            throw PrimitiveFailed.GENERIC_ERROR;
        }
    }

    public interface OctonaryPrimitive extends AbstractPrimitive {
        @Override
        default int getNumArguments() {
            return 8;
        }

        @SuppressWarnings("unused")
        @Fallback
        default Object doPrimitiveFail(final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6, final Object arg7, final Object arg8) {
            throw PrimitiveFailed.GENERIC_ERROR;
        }
    }
}
