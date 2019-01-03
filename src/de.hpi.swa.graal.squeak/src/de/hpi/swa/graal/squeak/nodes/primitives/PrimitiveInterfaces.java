package de.hpi.swa.graal.squeak.nodes.primitives;

import com.oracle.truffle.api.dsl.Fallback;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;

public final class PrimitiveInterfaces {
    public interface AbstractPrimitive {
        int getNumArguments();
    }

    public interface UnaryPrimitiveWithoutFallback extends AbstractPrimitive {
        default int getNumArguments() {
            return 1;
        }
    }

    public interface UnaryPrimitive extends UnaryPrimitiveWithoutFallback {
        @Fallback
        default Object doFail(@SuppressWarnings("unused") final Object arg1) {
            throw new PrimitiveFailed();
        }
    }

    public interface BinaryPrimitiveWithoutFallback extends AbstractPrimitive {
        default int getNumArguments() {
            return 2;
        }
    }

    public interface BinaryPrimitive extends BinaryPrimitiveWithoutFallback {
        @SuppressWarnings("unused")
        @Fallback
        default Object doFail(final Object arg1, final Object arg2) {
            throw new PrimitiveFailed();
        }
    }

    public interface TernaryPrimitive extends AbstractPrimitive {
        default int getNumArguments() {
            return 3;
        }

        @SuppressWarnings("unused")
        @Fallback
        default Object doFail(final Object arg1, final Object arg2, final Object arg3) {
            throw new PrimitiveFailed();
        }
    }

    public interface QuaternaryPrimitive extends AbstractPrimitive {
        default int getNumArguments() {
            return 4;
        }

        @SuppressWarnings("unused")
        @Fallback
        default Object doFail(final Object arg1, final Object arg2, final Object arg3, final Object arg4) {
            throw new PrimitiveFailed();
        }
    }

    public interface QuinaryPrimitive extends AbstractPrimitive {
        default int getNumArguments() {
            return 5;
        }

        @SuppressWarnings("unused")
        @Fallback
        default Object doFail(final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5) {
            throw new PrimitiveFailed();
        }
    }

    public interface SenaryPrimitive extends AbstractPrimitive {
        default int getNumArguments() {
            return 6;
        }

        @SuppressWarnings("unused")
        @Fallback
        default Object doFail(final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6) {
            throw new PrimitiveFailed();
        }
    }

    public interface SeptenaryPrimitive extends AbstractPrimitive {
        default int getNumArguments() {
            return 7;
        }

        @SuppressWarnings("unused")
        @Fallback
        default Object doFail(final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6, final Object arg7) {
            throw new PrimitiveFailed();
        }
    }

    public interface OctonaryPrimitive extends AbstractPrimitive {
        default int getNumArguments() {
            return 8;
        }

        @SuppressWarnings("unused")
        @Fallback
        default Object doFail(final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6, final Object arg7, final Object arg8) {
            throw new PrimitiveFailed();
        }
    }
}
