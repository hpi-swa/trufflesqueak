package de.hpi.swa.graal.squeak.interop;

import java.util.Map;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.KeyInfo;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.model.ObjectLayouts.DICTIONARY;
import de.hpi.swa.graal.squeak.model.PointersObject;

@MessageResolution(receiverType = InteropMap.class)
public final class InteropMap implements TruffleObject {
    private final Map<Object, Object> map;

    public InteropMap(final PointersObject squeakDictionary) {
        this.map = DICTIONARY.toJavaMap(squeakDictionary);
    }

    public InteropMap(final Map<Object, Object> map) {
        this.map = map;
    }

    @Resolve(message = "HAS_KEYS")
    abstract static class HasKeys extends Node {
        public Object access(@SuppressWarnings("unused") final InteropMap receiver) {
            return true;
        }
    }

    @Resolve(message = "KEY_INFO")
    abstract static class KeyInfoNode extends Node {
        @TruffleBoundary
        public Object access(final InteropMap receiver, final Object key) {
            if (receiver.map.containsKey(key)) {
                return KeyInfo.READABLE;
            } else {
                return KeyInfo.NONE;
            }
        }
    }

    @Resolve(message = "KEYS")
    abstract static class GetSize extends Node {
        @TruffleBoundary
        public Object access(final InteropMap receiver) {
            return new InteropArray(receiver.map.keySet().toArray());
        }
    }

    @Resolve(message = "READ")
    abstract static class Read extends Node {
        @TruffleBoundary
        public Object access(final InteropMap receiver, final Object key) {
            return receiver.map.get(key);
        }
    }

    @Override
    public ForeignAccess getForeignAccess() {
        return InteropMapForeign.ACCESS;
    }

    static boolean isInstance(final TruffleObject object) {
        return object instanceof InteropMap;
    }
}
