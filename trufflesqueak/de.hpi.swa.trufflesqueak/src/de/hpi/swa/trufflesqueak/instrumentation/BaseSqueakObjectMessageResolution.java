package de.hpi.swa.trufflesqueak.instrumentation;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.AbstractPointersObject;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;

// refer to com.oracle.truffle.api.interop.Message for documentation

@MessageResolution(receiverType = BaseSqueakObject.class)
public class BaseSqueakObjectMessageResolution {

    @Resolve(message = "WRITE")
    public abstract static class BaseSqueakObjectWriteNode extends Node {
        @SuppressWarnings("unused")
        public Object access(BaseSqueakObject receiver, Object name, Object value) {
            throw new RuntimeException("not yet implemented");
        }
    }

    @Resolve(message = "READ")
    public abstract static class BaseSqueakObjectReadNode extends Node {
        public Object access(BaseSqueakObject receiver, int index) {
            System.err.println("Object>>READ " + receiver.toString() + " " + index);
            if (receiver instanceof AbstractPointersObject) {
                return ((AbstractPointersObject) receiver).at0(index).toString();
            }
            return JavaInterop.asTruffleObject(null);
        }

        public Object access(BaseSqueakObject receiver, String name) {
            System.err.println("Object>>READ " + receiver.toString() + " " + name);
            if (name.equals("a")) {
                return 42;
            } else if (name.equals("b")) {
                return 43;
            } else if (name.equals("c")) {
                return 44;
            }
            throw UnknownIdentifierException.raise("foo");
        }
    }

    @Resolve(message = "HAS_SIZE")
    public abstract static class BaseSqueakObjectHasSizeNode extends Node {
        public Object access(BaseSqueakObject receiver) {
            return (receiver instanceof AbstractPointersObject);
        }
    }

    @Resolve(message = "GET_SIZE")
    public abstract static class BaseSqueakObjectGetSizeNode extends Node {
        public Object access(BaseSqueakObject receiver) {
            if (receiver instanceof AbstractPointersObject) {
                return ((AbstractPointersObject) receiver).size();
            }
            return 0;
        }
    }

    @Resolve(message = "INVOKE")
    public abstract static class BaseSqueakObjectInvokeNode extends Node {
        @SuppressWarnings("unused")
        public Object access(BaseSqueakObject receiver, String name, Object[] arguments) {
            System.err.println("Object>>INVOKE " + receiver.toString() + " " + name.toString());
            return "BaseSqueakObjectInvokeNode";
        }
    }

    @Resolve(message = "KEY_INFO")
    public abstract static class BaseSqueakObjectPropertyInfoNode extends Node {
        public int access(BaseSqueakObject receiver, Object name) {
            System.err.println("Object>>KEYS_INFO " + receiver.toString() + " " + name.toString());
            return 0;
        }
    }

    @Resolve(message = "KEYS")
    public abstract static class BaseSqueakObjectPropertiesNode extends Node {
        @TruffleBoundary
        private static Object obtainKeys(BaseSqueakObject receiver) {
            return JavaInterop.asTruffleObject(receiver.toString());
        }

        public Object access(BaseSqueakObject receiver) {
            return obtainKeys(receiver);
        }
    }
}
