/*
 * Copyright (c) 2017-2021 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.interop;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;

/**
 * Converts an object to a TruffleSqueak object, returns `nil` if conversion is not possible.
 */
public abstract class ConvertToSqueakNode extends AbstractNode {

    public abstract Object executeConvert(Object value);

    @Specialization(guards = "lib.isBoolean(value)", limit = "1")
    protected static final boolean doBoolean(final Object value,
                    @CachedLibrary("value") final InteropLibrary lib) {
        try {
            return lib.asBoolean(value);
        } catch (final UnsupportedMessageException e) {
            CompilerDirectives.transferToInterpreter();
            e.printStackTrace();
            return false;
        }
    }

    @Specialization(guards = "lib.isString(value)", limit = "1")
    protected static final NativeObject doString(final Object value,
                    @CachedLibrary("value") final InteropLibrary lib,
                    @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
        try {
            return image.asByteString(lib.asString(value));
        } catch (final UnsupportedMessageException e) {
            CompilerDirectives.transferToInterpreter();
            e.printStackTrace();
            return image.asByteString("");
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
            CompilerDirectives.transferToInterpreter();
            e.printStackTrace();
            return 0L;
        }
    }

    @Specialization(guards = {"lib.fitsInDouble(value)", "!lib.fitsInLong(value)"}, limit = "1")
    protected static final double doDouble(final Object value,
                    @CachedLibrary("value") final InteropLibrary lib) {
        try {
            return lib.asDouble(value);
        } catch (final UnsupportedMessageException e) {
            CompilerDirectives.transferToInterpreter();
            e.printStackTrace();
            return 0D;
        }
    }

    @Fallback
    protected static final NilObject doFail(@SuppressWarnings("unused") final Object value) {
        return NilObject.SINGLETON; /* Return nil if conversion fails. */
    }
}
