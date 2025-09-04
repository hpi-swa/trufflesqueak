/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.primitives;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInterface;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;

public interface Primitive extends NodeInterface {
    interface Primitive0 extends Primitive {
        Object execute(VirtualFrame frame, Object receiver);
    }

    interface Primitive0WithFallback extends Primitive0 {
        @Fallback
        @SuppressWarnings("unused")
        default Object doPrimitiveFail0(final Object receiver) {
            throw PrimitiveFailed.GENERIC_ERROR;
        }
    }

    interface Primitive1 extends Primitive {
        Object execute(VirtualFrame frame, Object receiver, Object arg1);
    }

    interface Primitive1WithFallback extends Primitive1 {
        @Fallback
        @SuppressWarnings("unused")
        default Object doPrimitiveFail1(final Object receiver, final Object arg1) {
            throw PrimitiveFailed.GENERIC_ERROR;
        }
    }

    interface Primitive2 extends Primitive {
        Object execute(VirtualFrame frame, Object receiver, Object arg1, Object arg2);
    }

    interface Primitive2WithFallback extends Primitive2 {
        @Fallback
        @SuppressWarnings("unused")
        default Object doPrimitiveFail2(final Object receiver, final Object arg1, final Object arg2) {
            throw PrimitiveFailed.GENERIC_ERROR;
        }
    }

    interface Primitive3 extends Primitive {
        Object execute(VirtualFrame frame, Object receiver, Object arg1, Object arg2, Object arg3);
    }

    interface Primitive3WithFallback extends Primitive3 {
        @Fallback
        @SuppressWarnings("unused")
        default Object doPrimitiveFail3(final Object receiver, final Object arg1, final Object arg2, final Object arg3) {
            throw PrimitiveFailed.GENERIC_ERROR;
        }
    }

    interface Primitive4 extends Primitive {
        Object execute(VirtualFrame frame, Object receiver, Object arg1, Object arg2, Object arg3, Object arg4);
    }

    interface Primitive4WithFallback extends Primitive4 {
        @Fallback
        @SuppressWarnings("unused")
        default Object doPrimitiveFail4(final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4) {
            throw PrimitiveFailed.GENERIC_ERROR;
        }
    }

    interface Primitive5 extends Primitive {
        Object execute(VirtualFrame frame, Object receiver, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5);
    }

    interface Primitive5WithFallback extends Primitive5 {
        @Fallback
        @SuppressWarnings("unused")
        default Object doPrimitiveFail5(final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5) {
            throw PrimitiveFailed.GENERIC_ERROR;
        }
    }

    interface Primitive6 extends Primitive {
        Object execute(VirtualFrame frame, Object receiver, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6);
    }

    interface Primitive6WithFallback extends Primitive6 {
        @Fallback
        @SuppressWarnings("unused")
        default Object doPrimitiveFail6(final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6) {
            throw PrimitiveFailed.GENERIC_ERROR;
        }
    }

    interface Primitive7 extends Primitive {
        Object execute(VirtualFrame frame, Object receiver, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7);
    }

    interface Primitive7WithFallback extends Primitive7 {
        @Fallback
        @SuppressWarnings("unused")
        default Object doPrimitiveFail7(final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6, final Object arg7) {
            throw PrimitiveFailed.GENERIC_ERROR;
        }
    }

    interface Primitive8 extends Primitive {
        Object execute(VirtualFrame frame, Object receiver, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8);
    }

    interface Primitive8WithFallback extends Primitive8 {
        @Fallback
        @SuppressWarnings("unused")
        default Object doPrimitiveFail8(final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6, final Object arg7,
                        final Object arg8) {
            throw PrimitiveFailed.GENERIC_ERROR;
        }
    }

    interface Primitive9 extends Primitive {
        Object execute(VirtualFrame frame, Object receiver, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9);
    }

    interface Primitive9WithFallback extends Primitive9 {
        @Fallback
        @SuppressWarnings("unused")
        default Object doPrimitiveFail9(final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6, final Object arg7,
                        final Object arg8, final Object arg9) {
            throw PrimitiveFailed.GENERIC_ERROR;
        }
    }

    interface Primitive10 extends Primitive {
        Object execute(VirtualFrame frame, Object receiver, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10);
    }

    interface Primitive10WithFallback extends Primitive10 {
        @Fallback
        @SuppressWarnings("unused")
        default Object doPrimitiveFail10(final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6, final Object arg7,
                        final Object arg8, final Object arg9, final Object arg10) {
            throw PrimitiveFailed.GENERIC_ERROR;
        }
    }

    interface Primitive11 extends Primitive {
        Object execute(VirtualFrame frame, Object receiver, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7, Object arg8, Object arg9, Object arg10,
                        Object arg11);
    }

    interface Primitive11WithFallback extends Primitive11 {
        @Fallback
        @SuppressWarnings("unused")
        default Object doPrimitiveFail11(final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6, final Object arg7,
                        final Object arg8, final Object arg9, final Object arg10, final Object arg11) {
            throw PrimitiveFailed.GENERIC_ERROR;
        }
    }
}
