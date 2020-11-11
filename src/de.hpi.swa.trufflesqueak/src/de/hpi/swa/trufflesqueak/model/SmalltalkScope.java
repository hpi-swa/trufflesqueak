/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import java.util.HashMap;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.interop.InteropArray;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.DICTIONARY;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.ENVIRONMENT;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.SMALLTALK_IMAGE;

@ExportLibrary(InteropLibrary.class)
@SuppressWarnings("static-method")
public final class SmalltalkScope implements TruffleObject {
    private final ClassObject smalltalkClass;
    private final HashMap<String, Object> bindings;
    private final InteropArray members;

    public SmalltalkScope(final PointersObject smalltalk) {
        smalltalkClass = smalltalk.getSqueakClass();
        final PointersObject environment = (PointersObject) smalltalk.instVarAt0Slow(SMALLTALK_IMAGE.GLOBALS);
        bindings = DICTIONARY.toJavaMap((PointersObject) environment.instVarAt0Slow(ENVIRONMENT.BINDINGS));
        members = new InteropArray(bindings.keySet().toArray());
    }

    @ExportMessage
    protected boolean hasLanguage() {
        return true;
    }

    @ExportMessage
    protected Class<? extends TruffleLanguage<?>> getLanguage() {
        return SqueakLanguage.class;
    }

    @ExportMessage
    protected boolean hasMembers() {
        return true;
    }

    @ExportMessage
    @TruffleBoundary
    protected Object readMember(final String member) throws UnknownIdentifierException {
        final Object value = bindings.get(member);
        if (value != null) {
            return value;
        }
        throw UnknownIdentifierException.create(member);
    }

    @ExportMessage
    @TruffleBoundary
    protected boolean isMemberReadable(final String member) {
        return bindings.containsKey(member);
    }

    @ExportMessage
    protected Object getMembers(@SuppressWarnings("unused") final boolean includeInternal) {
        return members;
    }

    @ExportMessage
    protected boolean hasMetaObject() {
        return true;
    }

    @ExportMessage
    protected Object getMetaObject() {
        return smalltalkClass;
    }

    @ExportMessage
    protected boolean isScope() {
        return true;
    }

    @ExportMessage
    @TruffleBoundary
    protected Object toDisplayString(@SuppressWarnings("unused") final boolean allowSideEffects) {
        return "Smalltalk";
    }
}
