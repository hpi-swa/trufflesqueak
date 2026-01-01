/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.dsl.TypeCast;
import com.oracle.truffle.api.dsl.TypeCheck;
import com.oracle.truffle.api.dsl.TypeSystem;

import de.hpi.swa.trufflesqueak.model.NilObject;

@TypeSystem
public class SqueakTypes {
    @TypeCheck(NilObject.class)
    public static final boolean isNilObject(final Object value) {
        return value == NilObject.SINGLETON;
    }

    @TypeCast(NilObject.class)
    public static final NilObject asNilObject(final Object value) {
        assert isNilObject(value);
        return NilObject.SINGLETON;
    }
}
