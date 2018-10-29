package de.hpi.swa.graal.squeak.nodes.plugins;

import java.util.List;

import org.graalvm.collections.EconomicMap;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.QuaternaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.SenaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;

public final class SqueakSSL extends AbstractPrimitiveFactoryHolder {
    private static final EconomicMap<Long, SSLImpl> SSL_HANDLES = EconomicMap.create();

    private static final class SSLImpl {
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAccept")
    protected abstract static class PrimAcceptNode extends AbstractPrimitiveNode implements SenaryPrimitive {
        protected PrimAcceptNode(final CompiledMethodObject method) {
            super(method);
        }

        // Primitive. Starts or continues a server handshake using the provided data.
        // Will eventually produce output to be sent to the server.
        // Returns:
        // > 0 - Number of bytes to be sent to the server
        // 0 - Success. The connection is established.
        // -1 - More input is required.
        // < -1 - Other errors
        @Specialization
        @SuppressWarnings("unused")
        protected static Object doAccept(final AbstractSqueakObject receiver,
                        final long sslHandle,
                        final Object srcbuf,
                        final Object start,
                        final Object length,
                        final Object dstbuf) {
            throw new PrimitiveFailed();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveConnect")
    protected abstract static class PrimConnectNode extends AbstractPrimitiveNode implements SenaryPrimitive {
        protected PrimConnectNode(final CompiledMethodObject method) {
            super(method);
        }

        // Primitive. Starts or continues a client handshake using the provided data.
        // Will eventually produce output to be sent to the server.
        // Returns:
        // > 0 - Number of bytes to be sent to the server
        // 0 - Success. The connection is established.
        // -1 - More input is required.
        // < -1 - Other errors
        @Specialization
        @SuppressWarnings("unused")
        protected static Object doConnect(final AbstractSqueakObject receiver,
                        final long sslHandle,
                        final Object srcbuf,
                        final Object start,
                        final Object length,
                        final Object dstbuf) {
            throw new PrimitiveFailed();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDecrypt")
    protected abstract static class PrimDecryptNode extends AbstractPrimitiveNode implements SenaryPrimitive {
        protected PrimDecryptNode(final CompiledMethodObject method) {
            super(method);
        }

        // Takes incoming data for decryption and continues to decrypt data.
        // Returns the number of bytes produced in the output
        @Specialization
        @SuppressWarnings("unused")
        protected static Object doDecrypt(final AbstractSqueakObject receiver,
                        final long sslHandle,
                        final Object srcbuf,
                        final Object start,
                        final Object length,
                        final Object dstbuf) {
            throw new PrimitiveFailed();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveEncrypt")
    protected abstract static class PrimEncryptNode extends AbstractPrimitiveNode implements SenaryPrimitive {
        protected PrimEncryptNode(final CompiledMethodObject method) {
            super(method);
        }

        // Encrypts the incoming buffer into the result buffer.
        // Returns the number of bytes produced as a result
        @Specialization
        @SuppressWarnings("unused")
        protected static Object doEncrypt(final AbstractSqueakObject receiver,
                        final long sslHandle,
                        final Object srcbuf,
                        final Object start,
                        final Object length,
                        final Object dstbuf) {
            throw new PrimitiveFailed();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetIntProperty")
    protected abstract static class PrimGetIntPropertyNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        protected PrimGetIntPropertyNode(final CompiledMethodObject method) {
            super(method);
        }

        // Returns a string property from an SSL session
        @Specialization
        @SuppressWarnings("unused")
        protected static Object doGet(final AbstractSqueakObject receiver, final long sslHandle, final Object propID) {
            throw new PrimitiveFailed();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetStringProperty")
    protected abstract static class PrimGetStringPropertyNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        protected PrimGetStringPropertyNode(final CompiledMethodObject method) {
            super(method);
        }

        // Returns a string property from an SSL session
        @Specialization
        @SuppressWarnings("unused")
        protected static Object doGet(final AbstractSqueakObject receiver, final long sslHandle, final Object propID) {
            throw new PrimitiveFailed();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSetIntProperty")
    protected abstract static class PrimSetIntPropertyNode extends AbstractPrimitiveNode implements QuaternaryPrimitive {
        protected PrimSetIntPropertyNode(final CompiledMethodObject method) {
            super(method);
        }

        // Sets a string property in an SSL session
        @Specialization
        @SuppressWarnings("unused")
        protected static Object doGet(final AbstractSqueakObject receiver,
                        final long sslHandle,
                        final Object propID,
                        final Object anInteger) {
            throw new PrimitiveFailed();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSetStringProperty")
    protected abstract static class PrimSetStringPropertyNode extends AbstractPrimitiveNode implements QuaternaryPrimitive {
        protected PrimSetStringPropertyNode(final CompiledMethodObject method) {
            super(method);
        }

        // Sets a string property in an SSL session
        @Specialization
        @SuppressWarnings("unused")
        protected static Object doSet(final AbstractSqueakObject receiver,
                        final long sslHandle,
                        final Object propID,
                        final Object aString) {
            throw new PrimitiveFailed();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveCreate")
    protected abstract static class PrimCreateNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        protected PrimCreateNode(final CompiledMethodObject method) {
            super(method);
        }

        // Creates and returns a new SSL handle
        @Specialization
        @TruffleBoundary
        @SuppressWarnings("unused")
        protected long doCreate(final AbstractSqueakObject receiver) {
            final SSLImpl ssl = new SSLImpl();
            final long handle = ssl.hashCode();
            SSL_HANDLES.put(handle, ssl);
            return handle;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDestroy")
    protected abstract static class PrimDestroyNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimDestroyNode(final CompiledMethodObject method) {
            super(method);
        }

        // Destroys the SSL session handle
        @Specialization
        @SuppressWarnings("unused")
        protected static Object doDestroy(final AbstractSqueakObject receiver, final long sslHandle) {
            throw new PrimitiveFailed();
        }
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return SqueakSSLFactory.getFactories();
    }
}
