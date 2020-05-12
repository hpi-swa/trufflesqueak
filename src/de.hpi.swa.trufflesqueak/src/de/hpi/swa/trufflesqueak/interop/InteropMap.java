/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.interop;

import java.util.Map;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.DICTIONARY;

@ExportLibrary(InteropLibrary.class)
public final class InteropMap implements TruffleObject {
    private final Map<String, Object> map;

    public InteropMap(final PointersObject squeakDictionary) {
        map = DICTIONARY.toJavaMap(squeakDictionary);
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean hasMembers() {
        return true;
    }

    @TruffleBoundary
    @ExportMessage
    public boolean isMemberReadable(final String key) {
        return map.containsKey(key);
    }

    @TruffleBoundary
    @ExportMessage
    public Object getMembers(@SuppressWarnings("unused") final boolean includeInternal) {
        return new InteropArray(map.keySet().toArray());
    }

    @TruffleBoundary
    @ExportMessage
    public Object readMember(final String key) {
        return map.get(key);
    }
}
