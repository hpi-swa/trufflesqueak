package de.hpi.swa.graal.squeak.interop;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.nodes.Node;

@MessageResolution(receiverType = InteropArray.class)
public final class InteropArray implements TruffleObject {

    private final Object[] keys;

    public InteropArray(final Object[] keys) {
        this.keys = keys;
    }

    @Resolve(message = "HAS_SIZE")
    abstract static class HasSize extends Node {
        public Object access(@SuppressWarnings("unused") final InteropArray receiver) {
            return true;
        }
    }

    @Resolve(message = "GET_SIZE")
    abstract static class GetSize extends Node {
        public Object access(final InteropArray receiver) {
            return receiver.keys.length;
        }
    }

    @Resolve(message = "READ")
    abstract static class Read extends Node {
        public Object access(final InteropArray receiver, final int index) {
            try {
                final Object key = receiver.keys[index];
                assert key instanceof String;
                return key;
            } catch (IndexOutOfBoundsException e) {
                CompilerDirectives.transferToInterpreter();
                throw UnknownIdentifierException.raise(String.valueOf(index));
            }
        }
    }

    @Override
    public ForeignAccess getForeignAccess() {
        return InteropArrayForeign.ACCESS;
    }

    static boolean isInstance(final TruffleObject object) {
        return object instanceof InteropArray;
    }
}
