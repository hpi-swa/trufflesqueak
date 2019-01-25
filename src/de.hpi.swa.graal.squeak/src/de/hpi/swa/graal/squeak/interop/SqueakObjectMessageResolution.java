package de.hpi.swa.graal.squeak.interop;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.KeyInfo;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.FrameMarker;
import de.hpi.swa.graal.squeak.nodes.DispatchNode;
import de.hpi.swa.graal.squeak.nodes.NewObjectNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAtPut0Node;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectSizeNode;
import de.hpi.swa.graal.squeak.util.ArrayUtils;

@MessageResolution(receiverType = AbstractSqueakObject.class)
public final class SqueakObjectMessageResolution {

    @Resolve(message = "WRITE")
    public abstract static class SqueakObjectWriteNode extends Node {
        @Child private SqueakObjectAtPut0Node atput0Node = SqueakObjectAtPut0Node.create();

        @SuppressWarnings("unused")
        protected final Object access(final AbstractSqueakObject receiver, final int index, final Object value) {
            atput0Node.execute(receiver, index, value);
            return value;
        }
    }

    @Resolve(message = "READ")
    public abstract static class SqueakObjectReadNode extends Node {
        @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();

        protected final Object access(final AbstractSqueakObject receiver, final int index) {
            return at0Node.execute(receiver, index);
        }

        protected static final Object access(final AbstractSqueakObject receiver, final String identifier) {
            return receiver.getSqueakClass().lookup(toSelector(identifier));
        }
    }

    @Resolve(message = "HAS_SIZE")
    public abstract static class SqueakObjectHasSizeNode extends Node {
        protected static final Object access(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            return true;
        }

        protected static final Object access(@SuppressWarnings("unused") final FrameMarker marker) {
            return false;
        }
    }

    @Resolve(message = "HAS_KEYS")
    public abstract static class SqueakObjectHasKeysNode extends Node {
        protected static final Object access(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            return true;
        }
    }

    @Resolve(message = "GET_SIZE")
    public abstract static class SqueakObjectGetSizeNode extends Node {
        @Child private SqueakObjectSizeNode sizeNode = SqueakObjectSizeNode.create();

        protected final Object access(final AbstractSqueakObject receiver) {
            return sizeNode.execute(receiver);
        }
    }

    @Resolve(message = "INVOKE")
    public abstract static class SqueakObjectInvokeNode extends Node {
        @Child private DispatchNode dispatchNode = DispatchNode.create();

        protected final Object access(final VirtualFrame frame, final AbstractSqueakObject receiver, final String identifier, final Object[] arguments) {
            final CompiledMethodObject method = (CompiledMethodObject) receiver.getSqueakClass().lookup(toSelector(identifier));
            return dispatchNode.executeDispatch(frame, method, ArrayUtils.copyWithFirst(arguments, receiver), null);
        }
    }

    @Resolve(message = "IS_EXECUTABLE")
    public abstract static class SqueakObjectExecutableNode extends Node {
        protected static final boolean access(@SuppressWarnings("unused") final CompiledMethodObject receiver) {
            return true;
        }

        protected static final boolean access(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            if (receiver instanceof CompiledMethodObject) {
                return true;
            } else {
                return false;
            }
        }
    }

    @Resolve(message = "EXECUTE")
    public abstract static class SqueakObjectExecuteNode extends Node {
        @Child private DispatchNode dispatchNode = DispatchNode.create();

        protected final Object access(final VirtualFrame frame, final CompiledMethodObject receiver, final Object[] arguments) {
            return dispatchNode.executeDispatch(frame, receiver, arguments, null);
        }
    }

    @Resolve(message = "NEW")
    public abstract static class SqueakObjectNewNode extends Node {

        protected static final Object access(final ClassObject receiver, final Object[] arguments) {
            final NewObjectNode newObjectNode = NewObjectNode.create(receiver.image);
            final int numArguments = arguments.length;
            if (numArguments == 0) {
                return newObjectNode.execute(receiver);
            } else {
                return newObjectNode.execute(receiver, (int) arguments[0]);
            }
        }
    }

    @Resolve(message = "KEY_INFO")
    public abstract static class SqueakObjectPropertyInfoNode extends Node {
        @SuppressWarnings("unused")
        protected static final int access(final AbstractSqueakObject receiver, final int index) {
            return KeyInfo.READABLE;
        }

        protected static final int access(final AbstractSqueakObject receiver, final String identifier) {
            final CompiledMethodObject method = (CompiledMethodObject) receiver.getSqueakClass().lookup(toSelector(identifier));
            if (method.getCompiledInSelector() == receiver.image.doesNotUnderstand) {
                return KeyInfo.NONE;
            } else {
                return KeyInfo.INVOCABLE;
            }
        }
    }

    @Resolve(message = "KEYS")
    public abstract static class SqueakObjectPropertiesNode extends Node {
        protected static final TruffleObject access(final AbstractSqueakObject receiver) {
            return new InteropArray(receiver.getSqueakClass().listMethods());
        }
    }

    /**
     * Converts an interop identifier to a Smalltalk selector. Most languages do not allow colons in
     * identifiers, so treat underscores as colons as well.
     *
     * @param identifier for interop
     * @return Smalltalk selector
     */
    private static String toSelector(final String identifier) {
        return identifier.replace('_', ':');
    }
}
