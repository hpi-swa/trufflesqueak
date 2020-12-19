/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.dsl.TypeCast;
import com.oracle.truffle.api.dsl.TypeCheck;
import com.oracle.truffle.api.dsl.TypeSystem;

import de.hpi.swa.trufflesqueak.model.InteropSenderMarker;
import de.hpi.swa.trufflesqueak.model.NilObject;

@TypeSystem
public class SqueakTypes {
    @TypeCheck(InteropSenderMarker.class)
    public static final boolean isInteropSenderMarker(final Object value) {
        return value == InteropSenderMarker.SINGLETON;
    }

    @TypeCast(InteropSenderMarker.class)
    public static final InteropSenderMarker asInteropSenderMarker(final Object value) {
        assert isInteropSenderMarker(value);
        return InteropSenderMarker.SINGLETON;
    }

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
