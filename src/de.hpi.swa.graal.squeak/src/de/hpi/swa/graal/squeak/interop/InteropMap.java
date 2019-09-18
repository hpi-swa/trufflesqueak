/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.interop;

import java.util.Map;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.DICTIONARY;

@ExportLibrary(InteropLibrary.class)
public final class InteropMap implements TruffleObject {
    private final Map<String, Object> map;

    public InteropMap(final PointersObject squeakDictionary) {
        map = DICTIONARY.toJavaMap(squeakDictionary);
    }

    public InteropMap(final Map<String, Object> map) {
        this.map = map;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean hasMembers() {
        return true;
    }

    @ExportMessage
    public boolean isMemberReadable(final String key) {
        return map.containsKey(key);
    }

    @ExportMessage
    public Object getMembers(@SuppressWarnings("unused") final boolean includeInternal) {
        return new InteropArray(map.keySet().toArray());
    }

    @ExportMessage
    public Object readMember(final String key) {
        return map.get(key);
    }
}
