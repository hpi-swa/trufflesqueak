/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.interop;

import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;

import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;

/**
 * Converts an object to a GraalSqueak object, returns `nil` if conversion is not possible.
 */
public abstract class ConvertToSqueakNode extends AbstractNode {

    public abstract Object executeConvert(Object value);

    @Specialization(guards = "lib.isBoolean(value)", limit = "1")
    protected static final boolean doBoolean(final Object value,
                    @CachedLibrary("value") final InteropLibrary lib) {
        try {
            return lib.asBoolean(value);
        } catch (final UnsupportedMessageException e) {
            throw SqueakException.illegalState(e);
        }
    }

    @Specialization(guards = "lib.isString(value)", limit = "1")
    protected static final NativeObject doString(final Object value,
                    @CachedLibrary("value") final InteropLibrary lib,
                    @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
        try {
            return image.asByteString(lib.asString(value));
        } catch (final UnsupportedMessageException e) {
            throw SqueakException.illegalState(e);
        }
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "lib.isNull(value)", limit = "1")
    protected static final NilObject doNull(final Object value,
                    @CachedLibrary("value") final InteropLibrary lib) {
        return NilObject.SINGLETON;
    }

    @Specialization(guards = "lib.fitsInLong(value)", limit = "1")
    protected static final long doLong(final Object value,
                    @CachedLibrary("value") final InteropLibrary lib) {
        try {
            return lib.asLong(value);
        } catch (final UnsupportedMessageException e) {
            throw SqueakException.illegalState(e);
        }
    }

    @Specialization(guards = {"lib.fitsInDouble(value)", "!lib.fitsInLong(value)"}, limit = "1")
    protected static final double doDouble(final Object value,
                    @CachedLibrary("value") final InteropLibrary lib) {
        try {
            return lib.asDouble(value);
        } catch (final UnsupportedMessageException e) {
            throw SqueakException.illegalState(e);
        }
    }

    @Fallback
    protected static final NilObject doFail(@SuppressWarnings("unused") final Object value) {
        return NilObject.SINGLETON; /* Return nil if conversion fails. */
    }
}
