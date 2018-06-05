package de.hpi.swa.graal.squeak.nodes.plugins;

import java.util.List;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.TreeMap;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NotProvided;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;

public final class SqueakSSLPlugin extends AbstractPrimitiveFactoryHolder {

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveAccept")
    protected abstract static class PrimAcceptNode extends AbstractPrimitiveNode {
        protected PrimAcceptNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected Object doWork(final Object receiver,
                        final Object sslHandle,
                        final Object srcbuf,
                        final Object start,
                        final Object length,
                        final Object dstbuf) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveConnect")
    protected abstract static class PrimConnectNode extends AbstractPrimitiveNode {
        protected PrimConnectNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected Object doWork(final Object receiver,
                        final Object sslHandle,
                        final Object srcbuf,
                        final Object start,
                        final Object length,
                        final Object dstbuf) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveDecrypt")
    protected abstract static class PrimDecryptNode extends AbstractPrimitiveNode {
        protected PrimDecryptNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected Object doWork(final Object receiver,
                        final Object sslHandle,
                        final Object srcbuf,
                        final Object start,
                        final Object length,
                        final Object dstbuf) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveEncrypt")
    protected abstract static class PrimEncryptNode extends AbstractPrimitiveNode {
        protected PrimEncryptNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected Object doWork(final Object receiver,
                        final Object sslHandle,
                        final Object srcbuf,
                        final Object start,
                        final Object length,
                        final Object dstbuf) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveGetIntProperty")
    protected abstract static class PrimGetIntPropertyNode extends AbstractPrimitiveNode {
        protected PrimGetIntPropertyNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected Object doWork(final Object receiver, final Object sslHandle, final Object propID) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveGetStringProperty")
    protected abstract static class PrimGetStringPropertyNode extends AbstractPrimitiveNode {
        protected PrimGetStringPropertyNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected Object doWork(final Object receiver, final Object sslHandle, final Object propID) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSetIntProperty")
    protected abstract static class PrimSetIntPropertyNode extends AbstractPrimitiveNode {
        protected PrimSetIntPropertyNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected Object doWork(final Object receiver,
                        final Object sslHandle,
                        final Object propID,
                        final Object anInteger) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSetStringProperty")
    protected abstract static class PrimSetStringPropertyNode extends AbstractPrimitiveNode {
        protected PrimSetStringPropertyNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected Object doWork(final Object receiver,
                        final Object sslHandle,
                        final Object propID,
                        final Object aString) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveCreate")
    protected abstract static class PrimCreateNode extends AbstractPrimitiveNode {
        protected PrimCreateNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected Object doWork(final Object receiver) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveDestroy")
    protected abstract static class PrimDestroyNode extends AbstractPrimitiveNode {
        protected PrimDestroyNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected Object doWork(final Object receiver, final Object sslHandle) {
            return receiver;
        }
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return SqueakSSLPluginFactory.getFactories();
    }
}
