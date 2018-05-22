package de.hpi.swa.graal.squeak.instrumentation;

import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.FrameMarker;
import de.hpi.swa.graal.squeak.nodes.SqueakObjectAt0Node;
import de.hpi.swa.graal.squeak.nodes.SqueakObjectSizeNode;

// refer to com.oracle.truffle.api.interop.Message for documentation

@MessageResolution(receiverType = AbstractSqueakObject.class)
public class SqueakObjectMessageResolution {

    @Resolve(message = "WRITE")
    public abstract static class BaseSqueakObjectWriteNode extends Node {
        @SuppressWarnings("unused")
        public Object access(final AbstractSqueakObject receiver, final Object name, final Object value) {
            throw new RuntimeException("Not yet implemented");
        }
    }

    @Resolve(message = "READ")
    public abstract static class BaseSqueakObjectReadNode extends Node {
        @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();

        public Object access(final AbstractSqueakObject receiver, final int index) {
            return at0Node.execute(receiver, index);
        }
    }

    @Resolve(message = "HAS_SIZE")
    public abstract static class BaseSqueakObjectHasSizeNode extends Node {
        public Object access(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            return true;
        }

        public Object access(@SuppressWarnings("unused") final FrameMarker marker) {
            return false;
        }
    }

    @Resolve(message = "HAS_KEYS")
    public abstract static class BaseSqueakObjectHasKeysNode extends Node {
        public Object access(@SuppressWarnings("unused") final Object receiver) {
            return false;
        }
    }

    @Resolve(message = "GET_SIZE")
    public abstract static class BaseSqueakObjectGetSizeNode extends Node {
        @Child private SqueakObjectSizeNode sizeNode = SqueakObjectSizeNode.create();

        public Object access(final AbstractSqueakObject receiver) {
            return sizeNode.execute(receiver);
        }
    }

    @Resolve(message = "INVOKE")
    public abstract static class BaseSqueakObjectInvokeNode extends Node {
        @SuppressWarnings("unused")
        public Object access(final AbstractSqueakObject receiver, final String name, final Object[] arguments) {
            return "BaseSqueakObjectInvokeNode";
        }
    }

    @Resolve(message = "KEY_INFO")
    public abstract static class BaseSqueakObjectPropertyInfoNode extends Node {
        @SuppressWarnings("unused")
        public int access(final AbstractSqueakObject receiver, final Object name) {
            return 0;
        }
    }

    @Resolve(message = "KEYS")
    public abstract static class BaseSqueakObjectPropertiesNode extends Node {
        public Object access(@SuppressWarnings("unused") final Object receiver) {
            return null; // FIXME
        }
    }
}
