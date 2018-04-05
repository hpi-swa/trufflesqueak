package de.hpi.swa.trufflesqueak.instrumentation;

import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.util.FrameMarker;

// refer to com.oracle.truffle.api.interop.Message for documentation

@MessageResolution(receiverType = BaseSqueakObject.class)
public class BaseSqueakObjectMessageResolution {

    @Resolve(message = "WRITE")
    public abstract static class BaseSqueakObjectWriteNode extends Node {
        @SuppressWarnings("unused")
        public Object access(BaseSqueakObject receiver, Object name, Object value) {
            throw new RuntimeException("Not yet implemented");
        }
    }

    @Resolve(message = "READ")
    public abstract static class BaseSqueakObjectReadNode extends Node {
        public Object access(BaseSqueakObject receiver, int index) {
            return receiver.at0(index);
        }
    }

    @Resolve(message = "HAS_SIZE")
    public abstract static class BaseSqueakObjectHasSizeNode extends Node {
        public Object access(@SuppressWarnings("unused") BaseSqueakObject receiver) {
            return true;
        }

        public Object access(@SuppressWarnings("unused") FrameMarker marker) {
            return false;
        }
    }

    @Resolve(message = "HAS_KEYS")
    public abstract static class BaseSqueakObjectHasKeysNode extends Node {
        public Object access(@SuppressWarnings("unused") Object receiver) {
            return false;
        }
    }

    @Resolve(message = "GET_SIZE")
    public abstract static class BaseSqueakObjectGetSizeNode extends Node {
        public Object access(BaseSqueakObject receiver) {
            return receiver.size();
        }
    }

    @Resolve(message = "INVOKE")
    public abstract static class BaseSqueakObjectInvokeNode extends Node {
        @SuppressWarnings("unused")
        public Object access(BaseSqueakObject receiver, String name, Object[] arguments) {
            return "BaseSqueakObjectInvokeNode";
        }
    }

    @Resolve(message = "KEY_INFO")
    public abstract static class BaseSqueakObjectPropertyInfoNode extends Node {
        @SuppressWarnings("unused")
        public int access(BaseSqueakObject receiver, Object name) {
            return 0;
        }
    }

    @Resolve(message = "KEYS")
    public abstract static class BaseSqueakObjectPropertiesNode extends Node {
        public Object access(@SuppressWarnings("unused") Object receiver) {
            return null; // FIXME
        }
    }
}
