/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.plugins.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.logging.Level;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BooleanObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.QuaternaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.QuinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.SenaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.SeptenaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitiveWithoutFallback;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.graal.squeak.shared.SqueakLanguageConfig;
import de.hpi.swa.graal.squeak.util.NotProvided;

public final class SocketPlugin extends AbstractPrimitiveFactoryHolder {
    private static final TruffleLogger LOG = TruffleLogger.getLogger(SqueakLanguageConfig.ID, SocketPlugin.class);

    protected static final byte[] LOCAL_HOST_NAME = getLocalHostName().getBytes();

    private static String getLocalHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (final UnknownHostException e) {
            e.printStackTrace();
            return "unknown";
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveResolverStatus")
    protected abstract static class PrimResolverStatusNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        @Specialization
        protected static long doWork(@SuppressWarnings("unused") final Object receiver) {
            return Resolver.Status.Ready.id();
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(names = "primitiveInitializeNetwork")
    protected abstract static class PrimInitializeNetworkNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        @Specialization
        protected static Object doWork(final Object receiver) {
            return receiver;
        }
    }

    @TruffleBoundary
    private static SqueakSocket getSocketOrPrimFail(final SqueakImageContext image, final long socketHandle) {
        final SqueakSocket socket = image.socketPluginHandles.get(socketHandle);
        if (socket == null) {
            throw PrimitiveFailed.andTransferToInterpreter();
        }
        return socket;
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveResolverStartNameLookup")
    protected abstract static class PrimResolverStartNameLookupNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        /**
         * Look up the given host name in the Domain Name Server to find its address. This call is
         * asynchronous. To get the results, wait for it to complete or time out and then use
         * primNameLookupResult.
         */
        @Specialization(guards = "hostName.isByteType()")
        @TruffleBoundary
        protected static final Object doWork(final Object receiver, final NativeObject hostName) {
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

        /**
         * Look up the given host address in the Domain Name Server to find its name. This call is
         * asynchronous. To get the results, wait for it to complete or time out and then use
         * primAddressLookupResult.
         */
        @Specialization(guards = "address.isByteType()")
        @TruffleBoundary
        protected static final Object doWork(final Object receiver, final NativeObject address) {
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
    protected abstract static class PrimResolverNameLookupResultNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        /**
         * Return the host address found by the last host name lookup. Returns nil if the last
         * lookup was unsuccessful.
         */
        @Specialization
        @TruffleBoundary
        protected static final AbstractSqueakObject doWork(@SuppressWarnings("unused") final Object receiver,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            final byte[] lastNameLookup = Resolver.lastHostNameLookupResult();
            LOG.finer(() -> "Name Lookup Result: " + Resolver.addressBytesToString(lastNameLookup));
            return lastNameLookup == null ? NilObject.SINGLETON : image.asByteArray(lastNameLookup);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveResolverAddressLookupResult")
    protected abstract static class PrimResolverAddressLookupResultNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        /**
         * Return the host name found by the last host address lookup. Returns nil if the last
         * lookup was unsuccessful.
         */
        @Specialization
        protected static final AbstractSqueakObject doWork(@SuppressWarnings("unused") final Object receiver,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            final String lastAddressLookup = Resolver.lastAddressLookUpResult();
            LOG.finer(() -> ">> Address Lookup Result: " + lastAddressLookup);
            return lastAddressLookup == null ? NilObject.SINGLETON : image.asByteString(lastAddressLookup);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveResolverLocalAddress")
    protected abstract static class PrimResolverLocalAddressNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        @Specialization
        @TruffleBoundary
        protected static final AbstractSqueakObject doWork(@SuppressWarnings("unused") final Object receiver,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            final byte[] address = Resolver.getLoopbackAddress();
            LOG.finer(() -> "Local Address: " + Resolver.addressBytesToString(address));
            return image.asByteArray(address);
        }
    }

    @GenerateNodeFactory
    @ImportStatic(SocketPlugin.class)
    @SqueakPrimitive(names = "primitiveResolverHostNameResult")
    protected abstract static class PrimResolverHostNameResultNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        @Specialization(guards = {"targetString.isByteType()", "targetString.getByteLength() >= LOCAL_HOST_NAME.length"})
        protected static final Object doResult(@SuppressWarnings("unused") final Object receiver, final NativeObject targetString) {
            System.arraycopy(LOCAL_HOST_NAME, 0, targetString.getByteStorage(), 0, LOCAL_HOST_NAME.length);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveResolverHostNameSize")
    protected abstract static class PrimResolverHostNameSizeNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        @Specialization
        protected static final long doSize(@SuppressWarnings("unused") final Object receiver) {
            return LOCAL_HOST_NAME.length;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketLocalPort")
    protected abstract static class PrimSocketLocalPortNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        /** Return the local port for this socket, or zero if no port has yet been assigned. */
        @Specialization
        @TruffleBoundary
        protected static final long doLocalPort(@SuppressWarnings("unused") final Object receiver, final long socketID,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            try {
                return getSocketOrPrimFail(image, socketID).getLocalPort();
            } catch (final IOException e) {
                LOG.log(Level.FINE, "Retrieving local port failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketListenWithOrWithoutBacklog")
    protected abstract static class PrimSocketListenWithOrWithoutBacklogNode extends AbstractPrimitiveNode implements QuaternaryPrimitive {
        /**
         * Listen for a connection on the given port. This is an asynchronous call; query the socket
         * status to discover if and when the connection is actually completed.
         */
        @Specialization
        @TruffleBoundary
        protected static final Object doListen(final Object receiver,
                        final long socketID,
                        final long port,
                        @SuppressWarnings("unused") final NotProvided backlogSize,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            try {
                getSocketOrPrimFail(image, socketID).listenOn(port, 0L);
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
        protected static final Object doListen(final Object receiver,
                        final long socketID,
                        final long port,
                        @SuppressWarnings("unused") final long backlogSize,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            try {
                getSocketOrPrimFail(image, socketID).listenOn(port, backlogSize);
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
        /**
         * Set up the socket to listen on the given port. Will be used in conjunction with #accept
         * only.
         */
        @Specialization(guards = "interfaceAddress.isByteType()")
        @TruffleBoundary
        protected static final Object doListen(final Object receiver,
                        final long socketID,
                        final long port,
                        @SuppressWarnings("unused") final long backlogSize,
                        @SuppressWarnings("unused") final NativeObject interfaceAddress,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            try {
                getSocketOrPrimFail(image, socketID).listenOn(port, backlogSize);
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
        @Specialization(guards = "option.isByteType()")
        @TruffleBoundary
        protected static final ArrayObject doSet(@SuppressWarnings("unused") final Object receiver, final long socketID, final NativeObject option, final NativeObject value,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            try {
                final SqueakSocket socket = getSocketOrPrimFail(image, socketID);
                return setSocketOption(image, socket, option.asStringUnsafe(), value.asStringUnsafe());
            } catch (final IOException e) {
                LOG.log(Level.FINE, "Set socket option failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }

        private static ArrayObject setSocketOption(final SqueakImageContext image, final SqueakSocket socket, final String option, final String value) throws IOException {
            if (socket.supportsOption(option)) {
                socket.setOption(option, value);
                return image.asArrayOfObjects(0L, image.asByteString(value));
            }
            return image.asArrayOfObjects(1L, image.asByteString("0"));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketConnectToPort")
    protected abstract static class PrimSocketConnectToPortNode extends AbstractPrimitiveNode implements QuaternaryPrimitive {
        @Specialization(guards = "hostAddress.isByteType()")
        @TruffleBoundary
        protected static final long doConntext(
                        @SuppressWarnings("unused") final Object receiver, final long socketID,
                        final NativeObject hostAddress, final long port,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            try {
                final SqueakSocket socket = getSocketOrPrimFail(image, socketID);
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
        @Specialization
        @TruffleBoundary
        protected static final long doStatus(@SuppressWarnings("unused") final Object receiver, final long socketID,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            if (!image.socketPluginHandles.containsKey(socketID)) {
                return SqueakSocket.Status.Unconnected.id();
            }

            try {
                final SqueakSocket socket = getSocketOrPrimFail(image, socketID);
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
        @Specialization
        @TruffleBoundary
        protected static final AbstractSqueakObject doAddress(@SuppressWarnings("unused") final Object receiver, final long socketID,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            try {
                return image.asByteArray(getSocketOrPrimFail(image, socketID).getRemoteAddress());
            } catch (final IOException e) {
                LOG.log(Level.FINE, "Retrieving remote address failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketRemotePort")
    protected abstract static class PrimSocketRemotePortNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        @Specialization
        @TruffleBoundary
        protected static final long doRemotePort(@SuppressWarnings("unused") final Object receiver, final long socketID,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            try {
                return getSocketOrPrimFail(image, socketID).getRemotePort();
            } catch (final IOException e) {
                LOG.log(Level.FINE, "Retrieving remote port failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketGetOptions")
    protected abstract static class PrimSocketGetOptionsNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        /**
         * Get some option information on this socket. Refer to the UNIX man pages for valid SO,
         * TCP, IP, UDP options. In case of doubt refer to the source code. TCP_NODELAY,
         * SO_KEEPALIVE are valid options for example returns an array containing the error code and
         * the option value.
         */
        @Specialization(guards = "option.isByteType()")
        @TruffleBoundary
        protected static final Object doGetOption(@SuppressWarnings("unused") final Object receiver, final long socketID, final NativeObject option,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            try {
                final SqueakSocket socket = getSocketOrPrimFail(image, socketID);
                final String value = socket.getOption(option.asStringUnsafe());
                return image.asArrayOfObjects(0L, image.asByteString(value));
            } catch (final IOException e) {
                LOG.log(Level.FINE, "Retrieving socket option failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketReceiveDataAvailable")
    protected abstract static class PrimSocketReceiveDataAvailableNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        @Specialization
        @TruffleBoundary
        protected static final boolean doDataAvailable(@SuppressWarnings("unused") final Object receiver, final long socketID,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            try {
                return getSocketOrPrimFail(image, socketID).isDataAvailable();
            } catch (final IOException e) {
                LOG.log(Level.FINE, "Checking for available data failed", e);
                return BooleanObject.FALSE;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketError")
    protected abstract static class PrimSocketErrorNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        @Specialization
        @TruffleBoundary
        @SuppressWarnings("unused")
        protected static long doWork(final Object receiver, final long socketID) {
            return 0L;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketLocalAddress")
    protected abstract static class PrimSocketLocalAddressNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        @Specialization
        @TruffleBoundary
        protected static final AbstractSqueakObject doLocalAddress(@SuppressWarnings("unused") final Object receiver, final long socketID,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            try {
                final SqueakSocket socket = getSocketOrPrimFail(image, socketID);
                return image.asByteArray(socket.getLocalAddress());
            } catch (final IOException e) {
                LOG.log(Level.FINE, "Retrieving local address failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketSendDataBufCount")
    protected abstract static class PrimSocketSendDataBufCountNode extends AbstractPrimitiveNode implements QuinaryPrimitive {
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
        protected static final long doCount(
                        @SuppressWarnings("unused") final Object receiver,
                        final long socketID,
                        final NativeObject buffer,
                        final long startIndex,
                        final long count,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {

            try {
                final SqueakSocket socket = getSocketOrPrimFail(image, socketID);
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
        @Specialization
        @TruffleBoundary
        protected static final Object doClose(final Object receiver, final long socketID,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            try {
                getSocketOrPrimFail(image, socketID).close();
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
        @Specialization
        @TruffleBoundary
        protected static final Object doAbort(final Object receiver, final long socketID,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            try {
                getSocketOrPrimFail(image, socketID).close();
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
        @Specialization
        @TruffleBoundary
        protected static final Object doSendDone(@SuppressWarnings("unused") final Object receiver, final long socketID,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            try {
                return BooleanObject.wrap(getSocketOrPrimFail(image, socketID).isSendDone());
            } catch (final IOException e) {
                LOG.log(Level.FINE, "Checking completed send failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketReceiveDataBufCount")
    protected abstract static class PrimSocketReceiveDataBufCountNode extends AbstractPrimitiveNode implements QuinaryPrimitive {
        /**
         * Receive data from the given socket into the given array starting at the given index.
         * Return the number of bytes read or zero if no data is available.
         */
        @Specialization(guards = "buffer.isByteType()")
        @TruffleBoundary
        protected static final long doCount(
                        @SuppressWarnings("unused") final Object receiver, final long socketID,
                        final NativeObject buffer, final long startIndex, final long count,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            try {
                final SqueakSocket socket = getSocketOrPrimFail(image, socketID);
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
        @Specialization
        @TruffleBoundary
        protected static final long doDestroy(@SuppressWarnings("unused") final Object receiver, final long socketID,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            try {
                final SqueakSocket socket = image.socketPluginHandles.removeKey(socketID);
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
        @SuppressWarnings("unused")
        @TruffleBoundary
        @Specialization
        protected static final long doWork(final PointersObject receiver,
                        final long netType,
                        final long socketType,
                        final long rcvBufSize,
                        final long semaphoreIndex,
                        final long aReadSemaphore,
                        final long aWriteSemaphore,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {

            try {
                final SqueakSocket.Type type = SqueakSocket.Type.fromId(socketType);
                final SqueakSocket socket = SqueakSocket.create(type);
                image.socketPluginHandles.put(socket.handle(), socket);
                return socket.handle();
            } catch (final IOException e) {
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketAccept3Semaphores")
    protected abstract static class PrimSocketAccept3SemaphoresNode extends AbstractPrimitiveNode implements SeptenaryPrimitive {
        @SuppressWarnings("unused")
        @TruffleBoundary
        @Specialization
        protected static final long doWork(final Object receiver,
                        final long socketID,
                        final long receiveBufferSize,
                        final long sendBufSize,
                        final long semaphoreIndex,
                        final long readSemaphoreIndex,
                        final long writeSemaphoreIndex,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            try {
                final SqueakSocket socket = getSocketOrPrimFail(image, socketID);
                final SqueakSocket accepted = socket.accept();
                image.socketPluginHandles.put(accepted.handle(), accepted);
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
