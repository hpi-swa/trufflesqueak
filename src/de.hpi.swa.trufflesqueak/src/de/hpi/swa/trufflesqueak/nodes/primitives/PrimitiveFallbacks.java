/*
 * Copyright (c) 2017-2024 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2024 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.primitives;

import com.oracle.truffle.api.dsl.Fallback;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;

public final class PrimitiveFallbacks {
    public interface UnaryPrimitiveFallback {
        @SuppressWarnings("unused")
        @Fallback
        default Object doPrimitiveFail1(final Object arg1) {
            throw PrimitiveFailed.GENERIC_ERROR;
        }
    }

    public interface BinaryPrimitiveFallback {
        @SuppressWarnings("unused")
        @Fallback
        default Object doPrimitiveFail2(final Object arg1, final Object arg2) {
            throw PrimitiveFailed.GENERIC_ERROR;
        }
    }

    public interface TernaryPrimitiveFallback {
        @SuppressWarnings("unused")
        @Fallback
        default Object doPrimitiveFail3(final Object arg1, final Object arg2, final Object arg3) {
            throw PrimitiveFailed.GENERIC_ERROR;
        }
    }

    public interface QuaternaryPrimitiveFallback {
        @SuppressWarnings("unused")
        @Fallback
        default Object doPrimitiveFail4(final Object arg1, final Object arg2, final Object arg3, final Object arg4) {
            throw PrimitiveFailed.GENERIC_ERROR;
        }
    }

    public interface QuinaryPrimitiveFallback {
        @SuppressWarnings("unused")
        @Fallback
        default Object doPrimitiveFail5(final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5) {
            throw PrimitiveFailed.GENERIC_ERROR;
        }
    }

    public interface SenaryPrimitiveFallback {
        @SuppressWarnings("unused")
        @Fallback
        default Object doPrimitiveFail6(final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6) {
            throw PrimitiveFailed.GENERIC_ERROR;
        }
    }

    public interface SeptenaryPrimitiveFallback {
        @SuppressWarnings("unused")
        @Fallback
        default Object doPrimitiveFail7(final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6, final Object arg7) {
            throw PrimitiveFailed.GENERIC_ERROR;
        }
    }

    public interface OctonaryPrimitiveFallback {
        @SuppressWarnings("unused")
        @Fallback
        default Object doPrimitiveFail8(final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6, final Object arg7, final Object arg8) {
            throw PrimitiveFailed.GENERIC_ERROR;
        }
    }

    public interface NonaryPrimitiveFallback {
        @SuppressWarnings("unused")
        @Fallback
        default Object doPrimitiveFail9(final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6, final Object arg7, final Object arg8,
                        final Object arg9) {
            throw PrimitiveFailed.GENERIC_ERROR;
        }
    }

    public interface DecimaryPrimitiveFallback {
        @SuppressWarnings("unused")
        @Fallback
        default Object doPrimitiveFail10(final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6, final Object arg7, final Object arg8,
                        final Object arg9, final Object arg10) {
            throw PrimitiveFailed.GENERIC_ERROR;
        }
    }

    public interface UndecimaryPrimitiveFallback {
        @SuppressWarnings("unused")
        @Fallback
        default Object doPrimitiveFail11(final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6, final Object arg7, final Object arg8,
                        final Object arg9, final Object arg10, final Object arg11) {
            throw PrimitiveFailed.GENERIC_ERROR;
        }
    }

    public interface DuodecimaryPrimitiveFallback {
        @SuppressWarnings("unused")
        @Fallback
        default Object doPrimitiveFail12(final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6, final Object arg7, final Object arg,
                        final Object arg9, final Object arg10, final Object arg11, final Object arg12) {
            throw PrimitiveFailed.GENERIC_ERROR;
        }
    }
}
