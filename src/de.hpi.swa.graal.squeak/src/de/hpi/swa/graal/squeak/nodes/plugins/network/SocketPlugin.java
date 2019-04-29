package de.hpi.swa.graal.squeak.nodes.plugins.network;

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.logging.Level;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
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
import de.hpi.swa.graal.squeak.shared.SqueakLanguageConfig;

public final class SocketPlugin extends AbstractPrimitiveFactoryHolder {
    private static final TruffleLogger LOG = TruffleLogger.getLogger(SqueakLanguageConfig.ID, SocketPlugin.class);

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveResolverStatus")
    protected abstract static class PrimResolverStatusNode extends AbstractPrimitiveNode implements UnaryPrimitive {
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
    protected abstract static class PrimInitializeNetworkNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        protected PrimInitializeNetworkNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static AbstractSqueakObject doWork(final AbstractSqueakObject receiver) {
            return receiver;
        }
    }

    @TruffleBoundary
    private static SqueakSocket getSocketOrPrimFail(final CompiledMethodObject method, final long socketHandle) {
        final SqueakSocket socket = method.image.socketPluginHandles.get(socketHandle);
        if (socket == null) {
            throw PrimitiveFailed.andTransferToInterpreter();
        }
        return socket;
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveResolverStartNameLookup")
    protected abstract static class PrimResolverStartNameLookupNode extends AbstractPrimitiveNode implements BinaryPrimitive {
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
        protected static Object doWork(final Object receiver, final NativeObject hostName) {
            try {
                LOG.finer(() -> "Starting lookup for host name " + hostName);
                Resolver.startHostNameLookUp(hostName.asStringUnsafe());
            } catch (final UnknownHostException e) {
                LOG.log(Level.FINE, "Host name lookup failed", e);
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveResolverStartAddressLookup")
    protected abstract static class PrimResolverStartAddressLookupNode extends AbstractPrimitiveNode implements BinaryPrimitive {

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
        protected static Object doWork(final Object receiver, final NativeObject address) {
            try {
                LOG.finer(() -> "Starting lookup for address " + address);
                Resolver.startAddressLookUp(address.getByteStorage());
            } catch (final UnknownHostException e) {
                LOG.log(Level.FINE, "Address lookup failed", e);
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveResolverNameLookupResult")
    protected abstract static class PrimResolverNameLookupResultNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimResolverNameLookupResultNode(final CompiledMethodObject method) {
            super(method);
        }

        /**
         * Return the host address found by the last host name lookup. Returns nil if the last
         * lookup was unsuccessful.
         */
        @Specialization
        @TruffleBoundary
        protected final AbstractSqueakObject doWork(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            final byte[] lastNameLookup = Resolver.lastHostNameLookupResult();
            LOG.finer(() -> "Name Lookup Result: " + Resolver.addressBytesToString(lastNameLookup));
            return lastNameLookup == null ? NilObject.SINGLETON : method.image.asByteArray(lastNameLookup);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveResolverAddressLookupResult")
    protected abstract static class PrimResolverAddressLookupResultNode extends AbstractPrimitiveNode implements UnaryPrimitive {
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
            LOG.finer(() -> ">> Address Lookup Result: " + lastAddressLookup);
            return lastAddressLookup == null ? NilObject.SINGLETON : method.image.asByteString(lastAddressLookup);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveResolverLocalAddress")
    protected abstract static class PrimResolverLocalAddressNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        protected PrimResolverLocalAddressNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary
        protected final AbstractSqueakObject doWork(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            final byte[] address = Resolver.getLoopbackAddress();
            LOG.finer(() -> "Local Address: " + Resolver.addressBytesToString(address));
            return method.image.asByteArray(address);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveResolverHostNameSize")
    protected abstract static class PrimResolverHostNameSizeNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        protected PrimResolverHostNameSizeNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final AbstractSqueakObject doSize(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            throw new PrimitiveFailed(); // Signals that IPv6 is not available.
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketLocalPort")
    protected abstract static class PrimSocketLocalPortNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimSocketLocalPortNode(final CompiledMethodObject method) {
            super(method);
        }

        /** Return the local port for this socket, or zero if no port has yet been assigned. */
        @Specialization
        @TruffleBoundary
        protected long doLocalPort(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final long socketID) {
            try {
                return getSocketOrPrimFail(method, socketID).getLocalPort();
            } catch (final IOException e) {
                LOG.log(Level.FINE, "Retrieving local port failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketListenWithOrWithoutBacklog")
    protected abstract static class PrimSocketListenWithOrWithoutBacklogNode extends AbstractPrimitiveNode implements QuaternaryPrimitive {
        protected PrimSocketListenWithOrWithoutBacklogNode(final CompiledMethodObject method) {
            super(method);
        }

        /**
         * Listen for a connection on the given port. This is an asynchronous call; query the socket
         * status to discover if and when the connection is actually completed.
         */
        @Specialization
        @TruffleBoundary
        protected AbstractSqueakObject doListen(final AbstractSqueakObject receiver,
                        final long socketID,
                        final long port,
                        @SuppressWarnings("unused") final NotProvided backlogSize) {
            try {
                getSocketOrPrimFail(method, socketID).listenOn(port, 0L);
                return receiver;
            } catch (final IOException e) {
                LOG.log(Level.FINE, "Listen failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }

        /**
         * Set up the socket to listen on the given port. Will be used in conjunction with #accept
         * only.
         */
        @Specialization
        @TruffleBoundary
        protected AbstractSqueakObject doListen(final AbstractSqueakObject receiver,
                        final long socketID,
                        final long port,
                        @SuppressWarnings("unused") final long backlogSize) {
            try {
                getSocketOrPrimFail(method, socketID).listenOn(port, backlogSize);
                return receiver;
            } catch (final IOException e) {
                LOG.log(Level.FINE, "Listen failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketListenOnPortBacklogInterface")
    protected abstract static class PrimSocketListenOnPortBacklogInterfaceNode extends AbstractPrimitiveNode implements QuinaryPrimitive {
        protected PrimSocketListenOnPortBacklogInterfaceNode(final CompiledMethodObject method) {
            super(method);
        }

        /**
         * Set up the socket to listen on the given port. Will be used in conjunction with #accept
         * only.
         */
        @Specialization(guards = "interfaceAddress.isByteType()")
        @TruffleBoundary
        protected AbstractSqueakObject doListen(final AbstractSqueakObject receiver,
                        final long socketID,
                        final long port,
                        @SuppressWarnings("unused") final long backlogSize,
                        @SuppressWarnings("unused") final NativeObject interfaceAddress) {
            try {
                getSocketOrPrimFail(method, socketID).listenOn(port, backlogSize);
                return receiver;
            } catch (final IOException e) {
                LOG.log(Level.FINE, "Listen failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketSetOptions")
    protected abstract static class PrimSocketSetOptionsNode extends AbstractPrimitiveNode implements QuaternaryPrimitive {
        protected PrimSocketSetOptionsNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "option.isByteType()")
        @TruffleBoundary
        protected final ArrayObject doSet(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final long socketID, final NativeObject option, final NativeObject value) {
            try {
                final SqueakSocket socket = getSocketOrPrimFail(method, socketID);
                return setSocketOption(socket, option.asStringUnsafe(), value.asStringUnsafe());
            } catch (final IOException e) {
                LOG.log(Level.FINE, "Set socket option failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }

        private ArrayObject setSocketOption(final SqueakSocket socket, final String option, final String value) throws IOException {
            if (socket.supportsOption(option)) {
                socket.setOption(option, value);
                return method.image.asArrayOfObjects(0L, method.image.asByteString(value));
            }
            return method.image.asArrayOfObjects(1L, method.image.asByteString("0"));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketConnectToPort")
    protected abstract static class PrimSocketConnectToPortNode extends AbstractPrimitiveNode implements QuaternaryPrimitive {
        protected PrimSocketConnectToPortNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "hostAddress.isByteType()")
        @TruffleBoundary
        protected long doConntext(
                        @SuppressWarnings("unused") final AbstractSqueakObject receiver, final long socketID,
                        final NativeObject hostAddress, final long port) {
            try {
                final SqueakSocket socket = getSocketOrPrimFail(method, socketID);
                final String host = Resolver.addressBytesToString(hostAddress.getByteStorage());
                socket.connectTo(host, (int) port);
            } catch (final IOException e) {
                LOG.log(Level.FINE, "Socket connect failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
            return 0L;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketConnectionStatus")
    protected abstract static class PrimSocketConnectionStatusNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimSocketConnectionStatusNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary
        protected long doStatus(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final long socketID) {
            if (!method.image.socketPluginHandles.containsKey(socketID)) {
                return SqueakSocket.Status.Unconnected.id();
            }

            try {
                final SqueakSocket socket = getSocketOrPrimFail(method, socketID);
                return socket.getStatus().id();
            } catch (final IOException e) {
                LOG.log(Level.FINE, "Retrieving socket status failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketRemoteAddress")
    protected abstract static class PrimSocketRemoteAddressNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimSocketRemoteAddressNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary
        protected AbstractSqueakObject doAddress(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final long socketID) {
            try {
                return method.image.asByteArray(getSocketOrPrimFail(method, socketID).getRemoteAddress());
            } catch (final IOException e) {
                LOG.log(Level.FINE, "Retrieving remote address failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketRemotePort")
    protected abstract static class PrimSocketRemotePortNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimSocketRemotePortNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary
        protected long doRemotePort(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final long socketID) {
            try {
                return getSocketOrPrimFail(method, socketID).getRemotePort();
            } catch (final IOException e) {
                LOG.log(Level.FINE, "Retrieving remote port failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketGetOptions")
    protected abstract static class PrimSocketGetOptionsNode extends AbstractPrimitiveNode implements TernaryPrimitive {
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
        protected final Object doGetOption(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final long socketID, final NativeObject option) {
            try {
                final SqueakSocket socket = getSocketOrPrimFail(method, socketID);
                final String value = socket.getOption(option.asStringUnsafe());
                return method.image.asArrayOfObjects(0L, method.image.asByteString(value));
            } catch (final IOException e) {
                LOG.log(Level.FINE, "Retrieving socket option failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketReceiveDataAvailable")
    protected abstract static class PrimSocketReceiveDataAvailableNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimSocketReceiveDataAvailableNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary
        protected final boolean doDataAvailable(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final long socketID) {
            try {
                return getSocketOrPrimFail(method, socketID).isDataAvailable();
            } catch (final IOException e) {
                LOG.log(Level.FINE, "Checking for available data failed", e);
                return method.image.sqFalse;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketError")
    protected abstract static class PrimSocketErrorNode extends AbstractPrimitiveNode implements BinaryPrimitive {
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
    protected abstract static class PrimSocketLocalAddressNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimSocketLocalAddressNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary
        protected final AbstractSqueakObject doLocalAddress(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final long socketID) {
            try {
                final SqueakSocket socket = getSocketOrPrimFail(method, socketID);
                return method.image.asByteArray(socket.getLocalAddress());
            } catch (final IOException e) {
                LOG.log(Level.FINE, "Retrieving local address failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketSendDataBufCount")
    protected abstract static class PrimSocketSendDataBufCountNode extends AbstractPrimitiveNode implements QuinaryPrimitive {
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
        protected long doCount(
                        @SuppressWarnings("unused") final AbstractSqueakObject receiver,
                        final long socketID,
                        final NativeObject buffer,
                        final long startIndex,
                        final long count) {

            try {
                final SqueakSocket socket = getSocketOrPrimFail(method, socketID);
                return socket.sendData(ByteBuffer.wrap(buffer.getByteStorage(), (int) startIndex - 1, (int) count));
            } catch (final IOException e) {
                LOG.log(Level.FINE, "Sending data failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketCloseConnection")
    protected abstract static class PrimSocketCloseConnectionNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimSocketCloseConnectionNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary
        protected AbstractSqueakObject doClose(final AbstractSqueakObject receiver, final long socketID) {
            try {
                getSocketOrPrimFail(method, socketID).close();
                return receiver;
            } catch (final IOException e) {
                LOG.log(Level.FINE, "Closing socket failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketAbortConnection")
    protected abstract static class PrimSocketAbortConnectionNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimSocketAbortConnectionNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary
        protected AbstractSqueakObject doAbort(final AbstractSqueakObject receiver, final long socketID) {
            try {
                getSocketOrPrimFail(method, socketID).close();
                return receiver;
            } catch (final IOException e) {
                LOG.log(Level.FINE, "Closing socket failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketSendDone")
    protected abstract static class PrimSocketSendDoneNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimSocketSendDoneNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary
        protected final Object doSendDone(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final long socketID) {
            try {
                return method.image.asBoolean(getSocketOrPrimFail(method, socketID).isSendDone());
            } catch (final IOException e) {
                LOG.log(Level.FINE, "Checking completed send failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketReceiveDataBufCount")
    protected abstract static class PrimSocketReceiveDataBufCountNode extends AbstractPrimitiveNode implements QuinaryPrimitive {
        protected PrimSocketReceiveDataBufCountNode(final CompiledMethodObject method) {
            super(method);
        }

        /**
         * Receive data from the given socket into the given array starting at the given index.
         * Return the number of bytes read or zero if no data is available.
         */
        @Specialization(guards = "buffer.isByteType()")
        @TruffleBoundary
        protected long doCOunt(
                        @SuppressWarnings("unused") final AbstractSqueakObject receiver, final long socketID,
                        final NativeObject buffer, final long startIndex, final long count) {
            try {
                final SqueakSocket socket = getSocketOrPrimFail(method, socketID);
                return socket.receiveData(ByteBuffer.wrap(buffer.getByteStorage(), (int) startIndex - 1, (int) count));
            } catch (final IOException e) {
                LOG.log(Level.FINE, "Receiving data failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketDestroy")
    protected abstract static class PrimSocketDestroyNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimSocketDestroyNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary
        protected long doDestroy(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final long socketID) {
            try {
                final SqueakSocket socket = method.image.socketPluginHandles.removeKey(socketID);
                if (socket != null) {
                    socket.close();
                }
                return 0L;
            } catch (final IOException e) {
                LOG.log(Level.FINE, "Destroying socket failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketCreate3Semaphores")
    protected abstract static class PrimSocketCreate3SemaphoresNode extends AbstractPrimitiveNode implements SeptenaryPrimitive {
        protected PrimSocketCreate3SemaphoresNode(final CompiledMethodObject method) {
            super(method);
        }

        @SuppressWarnings("unused")
        @TruffleBoundary
        @Specialization
        protected long doWork(final PointersObject receiver,
                        final long netType,
                        final long socketType,
                        final long rcvBufSize,
                        final long semaphoreIndex,
                        final long aReadSemaphore,
                        final long aWriteSemaphore) {

            try {
                final SqueakSocket.Type type = SqueakSocket.Type.fromId(socketType);
                final SqueakSocket socket = SqueakSocket.create(type);
                method.image.socketPluginHandles.put(socket.handle(), socket);
                return socket.handle();
            } catch (final IOException e) {
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketAccept3Semaphores")
    protected abstract static class PrimSocketAccept3SemaphoresNode extends AbstractPrimitiveNode implements SeptenaryPrimitive {
        protected PrimSocketAccept3SemaphoresNode(final CompiledMethodObject method) {
            super(method);
        }

        @SuppressWarnings("unused")
        @TruffleBoundary
        @Specialization
        protected long doWork(final AbstractSqueakObject receiver,
                        final long socketID,
                        final long receiveBufferSize,
                        final long sendBufSize,
                        final long semaphoreIndex,
                        final long readSemaphoreIndex,
                        final long writeSemaphoreIndex) {
            try {
                final SqueakSocket socket = getSocketOrPrimFail(method, socketID);
                final SqueakSocket accepted = socket.accept();
                method.image.socketPluginHandles.put(accepted.handle(), accepted);
                return accepted.handle();
            } catch (final IOException e) {
                LOG.log(Level.FINE, "Accepting socket failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketCreate")
    protected abstract static class PrimSocketCreateNode extends AbstractPrimitiveNode implements SenaryPrimitive {
        protected PrimSocketCreateNode(final CompiledMethodObject method) {
            super(method);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected static long doWork(final PointersObject receiver,
                        final long netType,
                        final long socketType,
                        final long rcvBufSize,
                        final long sendBufSize,
                        final long semaphoreIndex) {

            LOG.warning("TODO: primitiveSocketCreate");
            throw PrimitiveFailed.andTransferToInterpreter();
        }

    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return SocketPluginFactory.getFactories();
    }
}
