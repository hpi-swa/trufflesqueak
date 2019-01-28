package de.hpi.swa.graal.squeak.nodes.plugins.network;

import de.hpi.swa.graal.squeak.model.ArrayObject;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.List;

import org.graalvm.collections.EconomicMap;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NotProvided;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.QuaternaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.QuinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.SenaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.SeptenaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;

public final class SocketPlugin extends AbstractPrimitiveFactoryHolder {

    private static final EconomicMap<Long, SqSocket> SOCKETS = EconomicMap.create();
    private static final boolean debugPrints = false;

    protected abstract static class AbstractSocketPluginPrimitiveNode extends AbstractPrimitiveNode {

        protected AbstractSocketPluginPrimitiveNode(final CompiledMethodObject method) {
            super(method);
        }

        protected final void error(final Object o) {
            code.image.printToStdErr(o);
        }

        protected final void print(final Object o) {
            if (debugPrints) {
                code.image.printToStdOut(o);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveResolverStatus")
    protected abstract static class PrimResolverStatusNode extends AbstractSocketPluginPrimitiveNode implements UnaryPrimitive {
        protected PrimResolverStatusNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static long doWork(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            return Resolver.Status.Ready.id();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveInitializeNetwork")
    protected abstract static class PrimInitializeNetworkNode extends AbstractSocketPluginPrimitiveNode implements UnaryPrimitive {
        protected PrimInitializeNetworkNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static AbstractSqueakObject doWork(final AbstractSqueakObject receiver) {
            return receiver;
        }
    }

    @TruffleBoundary
    private static SqSocket getSocketOrPrimFail(final long socketHandle) {
        final SqSocket socket = SOCKETS.get(socketHandle);
        if (socket == null) {
            throw new PrimitiveFailed();
        }
        return socket;
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveResolverStartNameLookup")
    protected abstract static class PrimResolverStartNameLookupNode extends AbstractSocketPluginPrimitiveNode implements BinaryPrimitive {
        protected PrimResolverStartNameLookupNode(final CompiledMethodObject method) {
            super(method);
        }

        /**
         * Look up the given host name in the Domain Name Server to find its address. This call is
         * asynchronous. To get the results, wait for it to complete or time out and then use
         * primNameLookupResult.
         */
        @Specialization(guards = "hostName.isByteType()")
        @TruffleBoundary
        protected final Object doWork(final Object receiver, final NativeObject hostName) {
            try {
                print(">> Starting lookup for host name " + hostName);
                Resolver.startHostNameLookUp(hostName.asString());
            } catch (final UnknownHostException e) {
                error(e);
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveResolverStartAddressLookup")
    protected abstract static class PrimResolverStartAddressLookupNode extends AbstractSocketPluginPrimitiveNode implements BinaryPrimitive {

        protected PrimResolverStartAddressLookupNode(final CompiledMethodObject method) {
            super(method);
        }

        /**
         * Look up the given host address in the Domain Name Server to find its name. This call is
         * asynchronous. To get the results, wait for it to complete or time out and then use
         * primAddressLookupResult.
         */
        @Specialization(guards = "address.isByteType()")
        @TruffleBoundary
        protected final Object doWork(final Object receiver, final NativeObject address) {
            try {
                print("Starting lookup for address " + address);
                Resolver.startAddressLookUp(address.getByteStorage());
            } catch (final UnknownHostException e) {
                error(e);
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveResolverNameLookupResult")
    protected abstract static class PrimResolverNameLookupResultNode extends AbstractSocketPluginPrimitiveNode implements UnaryPrimitive {

        protected PrimResolverNameLookupResultNode(final CompiledMethodObject method) {
            super(method);
        }

        /**
         * Return the host address found by the last host name lookup. Returns nil if the last
         * lookup was unsuccessful.
         */
        @Specialization
        @TruffleBoundary
        protected final AbstractSqueakObject doWork(
                        @SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            final byte[] lastNameLookup = Resolver.lastHostNameLookupResult();
            print(">> Name Lookup Result: " + Resolver.addressBytesToString(lastNameLookup));
            return lastNameLookup == null ? code.image.nil : code.image.wrap(lastNameLookup);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveResolverAddressLookupResult")
    protected abstract static class PrimResolverAddressLookupResultNode extends AbstractSocketPluginPrimitiveNode implements UnaryPrimitive {
        protected PrimResolverAddressLookupResultNode(final CompiledMethodObject method) {
            super(method);
        }

        /**
         * Return the host name found by the last host address lookup. Returns nil if the last
         * lookup was unsuccessful.
         */
        @Specialization
        protected final AbstractSqueakObject doWork(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            final String lastAddressLookup = Resolver.lastAddressLookUpResult();
            print(">> Address Lookup Result: " + lastAddressLookup);
            return lastAddressLookup == null ? code.image.nil : code.image.wrap(lastAddressLookup);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveResolverLocalAddress")
    protected abstract static class PrimResolverLocalAddressNode extends AbstractSocketPluginPrimitiveNode implements UnaryPrimitive {
        protected PrimResolverLocalAddressNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary
        protected final AbstractSqueakObject doWork(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            final byte[] address = Resolver.getLoopbackAddress();
            print(">> Local Address: " + Resolver.addressBytesToString(address));
            return code.image.wrap(address);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketLocalPort")
    protected abstract static class PrimSocketLocalPortNode extends AbstractSocketPluginPrimitiveNode implements BinaryPrimitive {
        protected PrimSocketLocalPortNode(final CompiledMethodObject method) {
            super(method);
        }

        /** Return the local port for this socket, or zero if no port has yet been assigned. */
        @Specialization
        @TruffleBoundary
        protected final long doWork(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final long socketID) {
            try {
                return getSocketOrPrimFail(socketID).getLocalPort();
            } catch (final IOException e) {
                error(e);
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketListenWithOrWithoutBacklog")
    protected abstract static class PrimSocketListenWithOrWithoutBacklogNode extends AbstractSocketPluginPrimitiveNode implements QuaternaryPrimitive {
        protected PrimSocketListenWithOrWithoutBacklogNode(final CompiledMethodObject method) {
            super(method);
        }

        /**
         * Listen for a connection on the given port. This is an asynchronous call; query the socket
         * status to discover if and when the connection is actually completed.
         */
        @Specialization
        @TruffleBoundary
        protected final AbstractSqueakObject doWork(final AbstractSqueakObject receiver,
                        final long socketID,
                        final long port,
                        @SuppressWarnings("unused") final NotProvided backlogSize) {
            try {
                getSocketOrPrimFail(socketID).listenOn(port, 0L);
                return receiver;
            } catch (IOException e) {
                error(e);
                throw new PrimitiveFailed();
            }
        }

        /**
         * Set up the socket to listen on the given port. Will be used in conjunction with #accept
         * only.
         */
        @Specialization
        @TruffleBoundary
        protected final AbstractSqueakObject doWork(final AbstractSqueakObject receiver,
                        final long socketID,
                        final long port,
                        @SuppressWarnings("unused") final long backlogSize) {
            try {
                getSocketOrPrimFail(socketID).listenOn(port, backlogSize);
                return receiver;
            } catch (IOException e) {
                error(e);
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketListenOnPortBacklogInterface")
    protected abstract static class PrimSocketListenOnPortBacklogInterfaceNode extends AbstractSocketPluginPrimitiveNode implements QuinaryPrimitive {
        protected PrimSocketListenOnPortBacklogInterfaceNode(final CompiledMethodObject method) {
            super(method);
        }

        /**
         * Set up the socket to listen on the given port. Will be used in conjunction with #accept
         * only.
         */
        @Specialization(guards = "interfaceAddress.isByteType()")
        @TruffleBoundary
        protected final AbstractSqueakObject doWork(final AbstractSqueakObject receiver,
                        final long socketID,
                        final long port,
                        @SuppressWarnings("unused") final long backlogSize,
                        @SuppressWarnings("unused") final NativeObject interfaceAddress) {
            try {
                getSocketOrPrimFail(socketID).listenOn(port, backlogSize);
                return receiver;
            } catch (IOException e) {
                error(e);
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketSetOptions")
    protected abstract static class PrimSocketSetOptionsNode extends AbstractSocketPluginPrimitiveNode implements QuaternaryPrimitive {
        protected PrimSocketSetOptionsNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "option.isByteType()")
        @TruffleBoundary
        protected final ArrayObject doWork(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final long socketID, final NativeObject option, final NativeObject value) {
            try {
                final SqSocket socket = getSocketOrPrimFail(socketID);
                return setSocketOption(socket, option.asString(), value.asString());
            } catch (final IOException e) {
                error(e);
                throw new PrimitiveFailed();
            }
        }

        private ArrayObject setSocketOption(final SqSocket socket, final String option, final String value) throws IOException {
            if (socket.supportsOption(option)) {
                socket.setOption(option, value);
                return code.image.wrap(0, value);
            }

            return code.image.wrap(1, "0");
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketConnectToPort")
    protected abstract static class PrimSocketConnectToPortNode extends AbstractSocketPluginPrimitiveNode implements QuaternaryPrimitive {
        protected PrimSocketConnectToPortNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "hostAddress.isByteType()")
        @TruffleBoundary
        protected final long doWork(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final long socketID, final NativeObject hostAddress, final long port) {
            try {
                final SqSocket socket = getSocketOrPrimFail(socketID);
                final String host = Resolver.addressBytesToString(hostAddress.getByteStorage());
                socket.connectTo(host, (int) port);
            } catch (final IOException e) {
                error(e);
                throw new PrimitiveFailed();
            }
            return 0;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketConnectionStatus")
    protected abstract static class PrimSocketConnectionStatusNode extends AbstractSocketPluginPrimitiveNode implements BinaryPrimitive {
        protected PrimSocketConnectionStatusNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary
        protected final long doWork(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final long socketID) {
            if (!SOCKETS.containsKey(socketID)) {
                return SqSocket.Status.Unconnected.id();
            }

            try {
                final SqSocket socket = getSocketOrPrimFail(socketID);
                return socket.getStatus().id();
            } catch (IOException e) {
                error(e);
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketRemoteAddress")
    protected abstract static class PrimSocketRemoteAddressNode extends AbstractSocketPluginPrimitiveNode implements BinaryPrimitive {
        protected PrimSocketRemoteAddressNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary
        protected final AbstractSqueakObject doWork(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final long socketID) {
            try {
                return code.image.wrap(getSocketOrPrimFail(socketID).getRemoteAddress());
            } catch (final IOException e) {
                error(e);
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketRemotePort")
    protected abstract static class PrimSocketRemotePortNode extends AbstractSocketPluginPrimitiveNode implements BinaryPrimitive {
        protected PrimSocketRemotePortNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary
        protected final long doWork(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final long socketID) {
            try {
                return getSocketOrPrimFail(socketID).getRemotePort();
            } catch (final IOException e) {
                error(e);
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketGetOptions")
    protected abstract static class PrimSocketGetOptionsNode extends AbstractSocketPluginPrimitiveNode implements TernaryPrimitive {
        protected PrimSocketGetOptionsNode(final CompiledMethodObject method) {
            super(method);
        }

        /**
         * Get some option information on this socket. Refer to the UNIX man pages for valid SO,
         * TCP, IP, UDP options. In case of doubt refer to the source code. TCP_NODELAY,
         * SO_KEEPALIVE are valid options for example returns an array containing the error code and
         * the option value.
         */
        @Specialization(guards = "option.isByteType()")
        @TruffleBoundary
        protected final Object doWork(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final long socketID, final NativeObject option) {
            try {
                final SqSocket socket = getSocketOrPrimFail(socketID);
                final String value = socket.getOption(option.asString());
                return code.image.wrap(0, value);
            } catch (final IOException e) {
                error(e);
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketReceiveDataAvailable")
    protected abstract static class PrimSocketReceiveDataAvailableNode extends AbstractSocketPluginPrimitiveNode implements BinaryPrimitive {
        protected PrimSocketReceiveDataAvailableNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary
        protected final boolean doWork(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final long socketID) {
            try {
                return getSocketOrPrimFail(socketID).isDataAvailable();
            } catch (final IOException e) {
                error(e);
                return code.image.sqFalse;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketError")
    protected abstract static class PrimSocketErrorNode extends AbstractSocketPluginPrimitiveNode implements BinaryPrimitive {
        protected PrimSocketErrorNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary
        @SuppressWarnings("unused")
        protected static long doWork(final AbstractSqueakObject receiver, final long socketID) {
            return 0L;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketLocalAddress")
    protected abstract static class PrimSocketLocalAddressNode extends AbstractSocketPluginPrimitiveNode implements BinaryPrimitive {
        protected PrimSocketLocalAddressNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary
        protected final AbstractSqueakObject doWork(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final long socketID) {
            try {
                final SqSocket socket = getSocketOrPrimFail(socketID);
                return code.image.wrap(socket.getLocalAddress());
            } catch (final IOException e) {
                error(e);
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketSendDataBufCount")
    protected abstract static class PrimSocketSendDataBufCountNode extends AbstractSocketPluginPrimitiveNode implements QuinaryPrimitive {
        protected PrimSocketSendDataBufCountNode(final CompiledMethodObject method) {
            super(method);
        }

        /**
         * Send data to the remote host through the given socket starting with the given byte index
         * of the given byte array. The data sent is 'pushed' immediately. Return the number of
         * bytes of data actually sent; any remaining data should be re-submitted for sending after
         * the current send operation has completed. Note: In general, it many take several sendData
         * calls to transmit a large data array since the data is sent in send-buffer-sized chunks.
         * The size of the send buffer is determined when the socket is created.
         */
        @Specialization(guards = "buffer.isByteType()")
        @TruffleBoundary
        protected final long doWork(@SuppressWarnings("unused") final AbstractSqueakObject receiver,
                        final long socketID,
                        final NativeObject buffer,
                        final long startIndex,
                        final long count) {

            try {
                final SqSocket socket = getSocketOrPrimFail(socketID);
                return socket.sendData(ByteBuffer.wrap(buffer.getByteStorage(), (int) startIndex - 1, (int) count));
            } catch (final IOException e) {
                error(e);
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketCloseConnection")
    protected abstract static class PrimSocketCloseConnectionNode extends AbstractSocketPluginPrimitiveNode implements BinaryPrimitive {
        protected PrimSocketCloseConnectionNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary
        protected final AbstractSqueakObject doWork(final AbstractSqueakObject receiver, final long socketID) {
            try {
                getSocketOrPrimFail(socketID).close();
                return receiver;
            } catch (IOException e) {
                error(e);
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketAbortConnection")
    protected abstract static class PrimSocketAbortConnectionNode extends AbstractSocketPluginPrimitiveNode implements BinaryPrimitive {
        protected PrimSocketAbortConnectionNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary
        protected final AbstractSqueakObject doWork(final AbstractSqueakObject receiver, final long socketID) {
            try {
                getSocketOrPrimFail(socketID).close();
                return receiver;
            } catch (IOException e) {
                error(e);
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketSendDone")
    protected abstract static class PrimSocketSendDoneNode extends AbstractSocketPluginPrimitiveNode implements BinaryPrimitive {
        protected PrimSocketSendDoneNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary
        protected final Object doWork(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final long socketID) {
            try {
                return code.image.wrap(getSocketOrPrimFail(socketID).isSendDone());
            } catch (final IOException e) {
                error(e);
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketReceiveDataBufCount")
    protected abstract static class PrimSocketReceiveDataBufCountNode extends AbstractSocketPluginPrimitiveNode implements QuinaryPrimitive {
        protected PrimSocketReceiveDataBufCountNode(final CompiledMethodObject method) {
            super(method);
        }

        /**
         * Receive data from the given socket into the given array starting at the given index.
         * Return the number of bytes read or zero if no data is available.
         */
        @Specialization(guards = "buffer.isByteType()")
        @TruffleBoundary
        protected final long doWork(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final long socketID, final NativeObject buffer, final long startIndex, final long count) {
            try {
                final SqSocket socket = getSocketOrPrimFail(socketID);
                return socket.receiveData(ByteBuffer.wrap(buffer.getByteStorage(), (int) startIndex - 1, (int) count));
            } catch (IOException e) {
                error(e);
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketDestroy")
    protected abstract static class PrimSocketDestroyNode extends AbstractSocketPluginPrimitiveNode implements BinaryPrimitive {
        protected PrimSocketDestroyNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary
        protected final long doWork(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final long socketID) {
            try {
                final SqSocket socket = SOCKETS.removeKey(socketID);
                if (socket != null) {
                    socket.close();
                }
                return 0;
            } catch (IOException e) {
                error(e);
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketCreate3Semaphores")
    protected abstract static class PrimSocketCreate3SemaphoresNode extends AbstractSocketPluginPrimitiveNode implements SeptenaryPrimitive {
        protected PrimSocketCreate3SemaphoresNode(final CompiledMethodObject method) {
            super(method);
        }

        @SuppressWarnings("unused")
        @TruffleBoundary
        @Specialization
        protected final long doWork(final PointersObject receiver,
                        final long netType,
                        final long socketType,
                        final long rcvBufSize,
                        final long semaphoreIndex,
                        final long aReadSemaphore,
                        final long aWriteSemaphore) {

            try {
                final SqSocket.Type type = SqSocket.Type.fromId(socketType);
                final SqSocket socket = SqSocket.create(type);
                SOCKETS.put(socket.handle(), socket);
                return socket.handle();
            } catch (final IOException e) {
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketAccept3Semaphores")
    protected abstract static class PrimSocketAccept3SemaphoresNode extends AbstractSocketPluginPrimitiveNode implements SeptenaryPrimitive {
        protected PrimSocketAccept3SemaphoresNode(final CompiledMethodObject method) {
            super(method);
        }

        @SuppressWarnings("unused")
        @TruffleBoundary
        @Specialization
        protected final long doWork(final AbstractSqueakObject receiver,
                        final long socketID,
                        final long receiveBufferSize,
                        final long sendBufSize,
                        final long semaphoreIndex,
                        final long readSemaphoreIndex,
                        final long writeSemaphoreIndex) {
            try {
                final SqSocket socket = getSocketOrPrimFail(socketID);
                final SqSocket accepted = socket.accept();
                SOCKETS.put(accepted.handle(), accepted);
                return accepted.handle();
            } catch (IOException e) {
                error(e);
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketCreate")
    protected abstract static class PrimSocketCreateNode extends AbstractSocketPluginPrimitiveNode implements SenaryPrimitive {
        protected PrimSocketCreateNode(final CompiledMethodObject method) {
            super(method);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected final long doWork(final PointersObject receiver,
                        final long netType,
                        final long socketType,
                        final long rcvBufSize,
                        final long sendBufSize,
                        final long semaphoreIndex) {
            error("TODO: primitiveSocketCreate");
            throw new PrimitiveFailed();
        }

    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return SocketPluginFactory.getFactories();
    }
}
