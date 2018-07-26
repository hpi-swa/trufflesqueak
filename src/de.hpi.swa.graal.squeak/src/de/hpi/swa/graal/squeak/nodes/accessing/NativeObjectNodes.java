package de.hpi.swa.graal.squeak.nodes.accessing;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.nodes.accessing.NativeObjectNodesFactory.NativeGetBytesNodeGen;

public final class NativeObjectNodes {
    public abstract static class NativeGetBytesNode extends Node {

        public static NativeGetBytesNode create() {
            return NativeGetBytesNodeGen.create();
        }

        @TruffleBoundary
        public final String executeAsString(final NativeObject obj) {
            return new String(execute(obj));
        }

        public abstract byte[] execute(NativeObject obj);

        @Specialization(guards = "obj.isByteType()")
        protected static final byte[] doNativeBytes(final NativeObject obj) {
            return obj.getByteStorage();
        }

        @Specialization(guards = "obj.isShortType()")
        protected static final byte[] doNativeShorts(final NativeObject obj) {
            return NativeObject.bytesFromShorts(obj.getShortStorage());
        }

        @Specialization(guards = "obj.isIntType()")
        protected static final byte[] doNativeInts(final NativeObject obj) {
            return NativeObject.bytesFromInts(obj.getIntStorage());
        }

        @Specialization(guards = "obj.isLongType()")
        protected static final byte[] doNativeLongs(final NativeObject obj) {
            return NativeObject.bytesFromLongs(obj.getLongStorage());
        }

        @Fallback
        protected static final byte[] doFail(final NativeObject object) {
            throw new SqueakException("Unexpected value:", object);
        }
    }
}
