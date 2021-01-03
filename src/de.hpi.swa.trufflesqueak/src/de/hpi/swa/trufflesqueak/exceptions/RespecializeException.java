/*
 * Copyright (c) 2020-2021 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.exceptions;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.nodes.SlowPathException;

public final class RespecializeException extends SlowPathException {
    public static final RespecializeException SINGLETON = new RespecializeException();

    private static final long serialVersionUID = 1L;

    private RespecializeException() {
    }

    public static RespecializeException transferToInterpreterInvalidateAndThrow() throws RespecializeException {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        throw SINGLETON;
    }
}
