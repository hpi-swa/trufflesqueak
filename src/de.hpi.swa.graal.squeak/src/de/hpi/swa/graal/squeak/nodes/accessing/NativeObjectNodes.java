package de.hpi.swa.graal.squeak.nodes.accessing;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ValueProfile;

import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.nodes.accessing.NativeObjectNodesFactory.NativeGetBytesNodeGen;

public final class NativeObjectNodes {

    public abstract static class NativeGetBytesNode extends Node {
        private final ValueProfile storageType = ValueProfile.createClassProfile();

        public static NativeGetBytesNode create() {
            return NativeGetBytesNodeGen.create();
        }

        @TruffleBoundary
        public final String executeAsString(final NativeObject obj) {
            return new String(execute(obj));
        }

        public abstract byte[] execute(NativeObject obj);

        @Specialization(guards = "obj.isByteType()")
        protected final byte[] doNativeBytes(final NativeObject obj) {
            return obj.getByteStorage(storageType);
        }

        @Specialization(guards = "obj.isShortType()")
        protected final byte[] doNativeShorts(final NativeObject obj) {
            return NativeObject.bytesFromShorts(obj.getShortStorage(storageType));
        }

        @Specialization(guards = "obj.isIntType()")
        protected final byte[] doNativeInts(final NativeObject obj) {
            return NativeObject.bytesFromInts(obj.getIntStorage(storageType));
        }

        @Specialization(guards = "obj.isLongType()")
        protected final byte[] doNativeLongs(final NativeObject obj) {
            return NativeObject.bytesFromLongs(obj.getLongStorage(storageType));
        }
    }

}
