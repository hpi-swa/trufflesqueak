/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.logging.Level;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive7WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive0;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive3WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive4WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive5WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive6WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive2WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.util.LogUtils;
import de.hpi.swa.trufflesqueak.util.UnsafeUtils;

public final class SocketPlugin extends AbstractPrimitiveFactoryHolder {
    private static final boolean HAS_SOCKET_ACCESS;
    protected static final byte[] LOCAL_HOST_NAME;

    static {
        boolean hasSocketAccess = false;
        String localHostName = "unknown";
        try {
            localHostName = InetAddress.getLocalHost().getHostName();
            hasSocketAccess = true;
        } catch (final SecurityException | UnknownHostException e) {
            e.printStackTrace();
        }
        HAS_SOCKET_ACCESS = hasSocketAccess;
        LOCAL_HOST_NAME = localHostName.getBytes();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveHasSocketAccess")
    protected abstract static class PrimHasSocketAccessNode extends AbstractPrimitiveNode implements Primitive0 {
        @Specialization
        protected static boolean hasSocketAccess(@SuppressWarnings("unused") final Object receiver) {
            return BooleanObject.wrap(HAS_SOCKET_ACCESS);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveResolverStatus")
    protected abstract static class PrimResolverStatusNode extends AbstractPrimitiveNode implements Primitive0 {
        @Specialization
        protected static long doWork(@SuppressWarnings("unused") final Object receiver) {
            return Resolver.Status.Ready.id();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveInitializeNetwork")
    protected abstract static class PrimInitializeNetworkNode extends AbstractPrimitiveNode implements Primitive0 {
        @Specialization
        protected static Object doWork(final Object receiver) {
            return receiver;
        }
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    private static SqueakSocket getSocketOrPrimFail(final PointersObject socketHandle) {
        final Object socket = socketHandle.getHiddenObject();
        if (socket instanceof final SqueakSocket o) {
            return o;
        } else {
            throw PrimitiveFailed.andTransferToInterpreter();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveResolverStartNameLookup")
    protected abstract static class PrimResolverStartNameLookupNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        /**
         * Look up the given host name in the Domain Name Server to find its address. This call is
         * asynchronous. To get the results, wait for it to complete or time out and then use
         * primNameLookupResult.
         */
        @Specialization(guards = "hostName.isByteType()")
        protected static final Object doWork(final Object receiver, final NativeObject hostName) {
            try {
                LogUtils.SOCKET.finer(() -> "Starting lookup for host name " + hostName);
                Resolver.startHostNameLookUp(hostName.asStringUnsafe());
            } catch (final UnknownHostException e) {
                LogUtils.SOCKET.log(Level.FINE, "Host name lookup failed", e);
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveResolverStartAddressLookup")
    protected abstract static class PrimResolverStartAddressLookupNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        /**
         * Look up the given host address in the Domain Name Server to find its name. This call is
         * asynchronous. To get the results, wait for it to complete or time out and then use
         * primAddressLookupResult.
         */
        @Specialization(guards = "address.isByteType()")
        protected static final Object doWork(final Object receiver, final NativeObject address) {
            try {
                LogUtils.SOCKET.finer(() -> "Starting lookup for address " + address);
                Resolver.startAddressLookUp(address.getByteStorage());
            } catch (final UnknownHostException e) {
                LogUtils.SOCKET.log(Level.FINE, "Address lookup failed", e);
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveResolverNameLookupResult")
    protected abstract static class PrimResolverNameLookupResultNode extends AbstractPrimitiveNode implements Primitive0 {

        /**
         * Return the host address found by the last host name lookup. Returns nil if the last
         * lookup was unsuccessful.
         */
        @Specialization
        protected static final AbstractSqueakObject doWork(@SuppressWarnings("unused") final Object receiver,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile hasResultProfile) {
            final byte[] lastNameLookup = Resolver.lastHostNameLookupResult();
            LogUtils.SOCKET.finer(() -> "Name Lookup Result: " + Resolver.addressBytesToString(lastNameLookup));
            return hasResultProfile.profile(node, lastNameLookup == null) ? NilObject.SINGLETON : getContext(node).asByteArray(lastNameLookup);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveResolverAddressLookupResult")
    protected abstract static class PrimResolverAddressLookupResultNode extends AbstractPrimitiveNode implements Primitive0 {
        /**
         * Return the host name found by the last host address lookup. Returns nil if the last
         * lookup was unsuccessful.
         */
        @Specialization
        protected final AbstractSqueakObject doWork(@SuppressWarnings("unused") final Object receiver) {
            final String lastAddressLookup = Resolver.lastAddressLookUpResult();
            LogUtils.SOCKET.finer(() -> ">> Address Lookup Result: " + lastAddressLookup);
            return lastAddressLookup == null ? NilObject.SINGLETON : getContext().asByteString(lastAddressLookup);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveResolverLocalAddress")
    protected abstract static class PrimResolverLocalAddressNode extends AbstractPrimitiveNode implements Primitive0 {
        @Specialization
        protected final AbstractSqueakObject doWork(@SuppressWarnings("unused") final Object receiver) {
            final byte[] address = Resolver.getLoopbackAddress();
            LogUtils.SOCKET.finer(() -> "Local Address: " + Resolver.addressBytesToString(address));
            return getContext().asByteArray(address);
        }
    }

    @GenerateNodeFactory
    @ImportStatic(SocketPlugin.class)
    @SqueakPrimitive(names = "primitiveResolverHostNameResult")
    protected abstract static class PrimResolverHostNameResultNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"targetString.isByteType()", "targetString.getByteLength() >= LOCAL_HOST_NAME.length"})
        protected static final Object doResult(@SuppressWarnings("unused") final Object receiver, final NativeObject targetString) {
            UnsafeUtils.copyBytes(LOCAL_HOST_NAME, 0, targetString.getByteStorage(), 0, LOCAL_HOST_NAME.length);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveResolverHostNameSize")
    protected abstract static class PrimResolverHostNameSizeNode extends AbstractPrimitiveNode implements Primitive0 {
        @Specialization
        protected static final long doSize(@SuppressWarnings("unused") final Object receiver) {
            return LOCAL_HOST_NAME.length;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketLocalPort")
    protected abstract static class PrimSocketLocalPortNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        /** Return the local port for this socket, or zero if no port has yet been assigned. */
        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected static final long doLocalPort(@SuppressWarnings("unused") final Object receiver, final PointersObject sd) {
            try {
                return getSocketOrPrimFail(sd).getLocalPort();
            } catch (final IOException e) {
                LogUtils.SOCKET.log(Level.FINE, "Retrieving local port failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketListenWithOrWithoutBacklog")
    protected abstract static class PrimSocketListenWithOrWithoutBacklog3Node extends AbstractPrimitiveNode implements Primitive2WithFallback {
        /**
         * Listen for a connection on the given port. This is an asynchronous call; query the socket
         * status to discover if and when the connection is actually completed.
         */
        @Specialization
        protected static final Object doListen(final Object receiver,
                        final PointersObject sd,
                        final long port) {
            try {
                listenOn(sd, port);
            } catch (final IOException e) {
                LogUtils.SOCKET.log(Level.FINE, "Listen failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
            return receiver;
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private static void listenOn(final PointersObject sd, final long port) throws IOException {
            getSocketOrPrimFail(sd).listenOn(port, 0L);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketListenWithOrWithoutBacklog")
    protected abstract static class PrimSocketListenWithOrWithoutBacklog4Node extends AbstractPrimitiveNode implements Primitive3WithFallback {
        /**
         * Set up the socket to listen on the given port. Will be used in conjunction with #accept
         * only.
         */
        @Specialization
        protected static final Object doListen(final Object receiver,
                        final PointersObject sd,
                        final long port,
                        final long backlogSize) {
            try {
                listenOn(sd, port, backlogSize);
            } catch (final IOException e) {
                LogUtils.SOCKET.log(Level.FINE, "Listen failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
            return receiver;
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private static void listenOn(final PointersObject sd, final long port, final long backlogSize) throws IOException {
            getSocketOrPrimFail(sd).listenOn(port, backlogSize);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketListenOnPortBacklogInterface")
    protected abstract static class PrimSocketListenOnPortBacklogInterfaceNode extends AbstractPrimitiveNode implements Primitive4WithFallback {
        /**
         * Set up the socket to listen on the given port. Will be used in conjunction with #accept
         * only.
         */
        @Specialization(guards = "interfaceAddress.isByteType()")
        protected static final Object doListen(final Object receiver,
                        final PointersObject sd,
                        final long port,
                        final long backlogSize,
                        @SuppressWarnings("unused") final NativeObject interfaceAddress) {
            try {
                listenOn(sd, port, backlogSize);
            } catch (final IOException e) {
                LogUtils.SOCKET.log(Level.FINE, "Listen failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
            return receiver;
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private static void listenOn(final PointersObject sd, final long port, final long backlogSize) throws IOException {
            getSocketOrPrimFail(sd).listenOn(port, backlogSize);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketSetOptions")
    protected abstract static class PrimSocketSetOptionsNode extends AbstractPrimitiveNode implements Primitive3WithFallback {
        @Specialization(guards = "option.isByteType()")
        protected final ArrayObject doSet(@SuppressWarnings("unused") final Object receiver, final PointersObject sd, final NativeObject option, final NativeObject value) {
            try {
                return setSocketOption(getContext(), getSocketOrPrimFail(sd), option.asStringUnsafe(), value.asStringUnsafe());
            } catch (final IOException e) {
                LogUtils.SOCKET.log(Level.FINE, "Set socket option failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }

        @TruffleBoundary
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
    protected abstract static class PrimSocketConnectToPortNode extends AbstractPrimitiveNode implements Primitive3WithFallback {
        @Specialization(guards = "hostAddress.isByteType()")
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected static final long doConnectToPort(
                        @SuppressWarnings("unused") final Object receiver, final PointersObject sd,
                        final NativeObject hostAddress, final long port) {
            try {
                final SqueakSocket socket = getSocketOrPrimFail(sd);
                final String host = Resolver.addressBytesToString(hostAddress.getByteStorage());
                socket.connectTo(host, (int) port);
            } catch (final IOException e) {
                LogUtils.SOCKET.log(Level.FINE, "Socket connect failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
            return 0L;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketConnectionStatus")
    protected abstract static class PrimSocketConnectionStatusNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected static final long doStatus(@SuppressWarnings("unused") final Object receiver, final PointersObject sd) {
            try {
                return getSocketOrPrimFail(sd).getStatus().id();
            } catch (final IOException e) {
                LogUtils.SOCKET.log(Level.FINE, "Retrieving socket status failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketRemoteAddress")
    protected abstract static class PrimSocketRemoteAddressNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected final AbstractSqueakObject doAddress(@SuppressWarnings("unused") final Object receiver, final PointersObject sd) {
            try {
                return getContext().asByteArray(getRemoteAddress(sd));
            } catch (final IOException e) {
                LogUtils.SOCKET.log(Level.FINE, "Retrieving remote address failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private static byte[] getRemoteAddress(final PointersObject sd) throws IOException {
            return getSocketOrPrimFail(sd).getRemoteAddress();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketRemotePort")
    protected abstract static class PrimSocketRemotePortNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected static final long doRemotePort(@SuppressWarnings("unused") final Object receiver, final PointersObject sd) {
            try {
                return getSocketOrPrimFail(sd).getRemotePort();
            } catch (final IOException e) {
                LogUtils.SOCKET.log(Level.FINE, "Retrieving remote port failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketGetOptions")
    protected abstract static class PrimSocketGetOptionsNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        /**
         * Get some option information on this socket. Refer to the UNIX man pages for valid SO,
         * TCP, IP, UDP options. In case of doubt refer to the source code. TCP_NODELAY,
         * SO_KEEPALIVE are valid options for example returns an array containing the error code and
         * the option value.
         */
        @Specialization(guards = "option.isByteType()")
        protected final Object doGetOption(@SuppressWarnings("unused") final Object receiver, final PointersObject sd, final NativeObject option) {
            final SqueakImageContext image = getContext();
            try {
                return image.asArrayOfObjects(0L, image.asByteString(getOption(sd, option)));
            } catch (final IOException e) {
                LogUtils.SOCKET.log(Level.FINE, "Retrieving socket option failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private static String getOption(final PointersObject sd, final NativeObject option) throws IOException {
            return getSocketOrPrimFail(sd).getOption(option.asStringUnsafe());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketReceiveDataAvailable")
    protected abstract static class PrimSocketReceiveDataAvailableNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected static final boolean doDataAvailable(@SuppressWarnings("unused") final Object receiver, final PointersObject sd) {
            try {
                return getSocketOrPrimFail(sd).isDataAvailable();
            } catch (final IOException e) {
                LogUtils.SOCKET.log(Level.FINE, "Checking for available data failed", e);
                return BooleanObject.FALSE;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketError")
    protected abstract static class PrimSocketErrorNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        @SuppressWarnings("unused")
        protected static long doWork(final Object receiver, final PointersObject sd) {
            return 0L;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketLocalAddress")
    protected abstract static class PrimSocketLocalAddressNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected final AbstractSqueakObject doLocalAddress(@SuppressWarnings("unused") final Object receiver, final PointersObject sd) {
            try {
                return getContext().asByteArray(getLocalAddress(sd));
            } catch (final IOException e) {
                LogUtils.SOCKET.log(Level.FINE, "Retrieving local address failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private static byte[] getLocalAddress(final PointersObject sd) throws IOException {
            return getSocketOrPrimFail(sd).getLocalAddress();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketSendDataBufCount")
    protected abstract static class PrimSocketSendDataBufCountNode extends AbstractPrimitiveNode implements Primitive4WithFallback {
        /**
         * Send data to the remote host through the given socket starting with the given byte index
         * of the given byte array. The data sent is 'pushed' immediately. Return the number of
         * bytes of data actually sent; any remaining data should be re-submitted for sending after
         * the current send operation has completed. Note: In general, it many take several sendData
         * calls to transmit a large data array since the data is sent in send-buffer-sized chunks.
         * The size of the send buffer is determined when the socket is created.
         */
        @Specialization(guards = "buffer.isByteType()")
        protected static final long doCount(
                        @SuppressWarnings("unused") final Object receiver,
                        final PointersObject sd,
                        final NativeObject buffer,
                        final long startIndex,
                        final long count) {

            try {
                return sendData(sd, buffer.getByteStorage(), (int) startIndex - 1, (int) count);
            } catch (final IOException e) {
                LogUtils.SOCKET.log(Level.FINE, "Sending data failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private static long sendData(final PointersObject sd, final byte[] data, final int start, final int count) throws IOException {
            return getSocketOrPrimFail(sd).sendData(data, start, count);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketCloseConnection")
    protected abstract static class PrimSocketCloseConnectionNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final Object doClose(final Object receiver, final PointersObject sd) {
            try {
                close(sd);
            } catch (final IOException e) {
                LogUtils.SOCKET.log(Level.FINE, "Closing socket failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketAbortConnection")
    protected abstract static class PrimSocketAbortConnectionNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final Object doAbort(final Object receiver, final PointersObject sd) {
            try {
                close(sd);
            } catch (final IOException e) {
                LogUtils.SOCKET.log(Level.FINE, "Closing socket failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketSendDone")
    protected abstract static class PrimSocketSendDoneNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final Object doSendDone(@SuppressWarnings("unused") final Object receiver, final PointersObject sd) {
            try {
                return BooleanObject.wrap(isSendDone(sd));
            } catch (final IOException e) {
                LogUtils.SOCKET.log(Level.FINE, "Checking completed send failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private static boolean isSendDone(final PointersObject sd) throws IOException {
            return getSocketOrPrimFail(sd).isSendDone();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketReceiveDataBufCount")
    protected abstract static class PrimSocketReceiveDataBufCountNode extends AbstractPrimitiveNode implements Primitive4WithFallback {
        /**
         * Receive data from the given socket into the given array starting at the given index.
         * Return the number of bytes read or zero if no data is available.
         */
        @Specialization(guards = "buffer.isByteType()")
        protected static final long doCount(
                        @SuppressWarnings("unused") final Object receiver, final PointersObject sd,
                        final NativeObject buffer, final long startIndex, final long count) {
            try {
                return receiveData(sd, buffer.getByteStorage(), (int) startIndex - 1, (int) count);
            } catch (final IOException e) {
                LogUtils.SOCKET.log(Level.FINE, "Receiving data failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "buffer.isIntType()")
        protected static final long doCountInt(
                        final Object receiver, final PointersObject sd,
                        final NativeObject buffer, final long startIndex, final long count) {
            // TODO: not yet implemented
            throw PrimitiveFailed.andTransferToInterpreter();
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private static long receiveData(final PointersObject sd, final byte[] data, final int start, final int count) throws IOException {
            return getSocketOrPrimFail(sd).receiveData(data, start, count);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketDestroy")
    protected abstract static class PrimSocketDestroyNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final long doDestroy(@SuppressWarnings("unused") final Object receiver, final PointersObject sd) {
            try {
                close(sd);
            } catch (final IOException e) {
                LogUtils.SOCKET.log(Level.FINE, "Destroying socket failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
            return 0L;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketCreate3Semaphores")
    protected abstract static class PrimSocketCreate3SemaphoresNode extends AbstractPrimitiveNode implements Primitive7WithFallback {
        @SuppressWarnings("unused")
        @Specialization
        protected static final PointersObject doWork(final PointersObject receiver,
                        final long netType,
                        final long socketType,
                        final long rcvBufSize,
                        final long sendBufSize,
                        final long semaphoreIndex,
                        final long aReadSemaphore,
                        final long aWriteSemaphore,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile socketTypeProfile) {

            final SqueakSocket socket;
            try {
                if (socketTypeProfile.profile(node, socketType == 1)) {
                    socket = createSqueakUDPSocket();
                } else {
                    assert socketType == 0;
                    socket = createSqueakTCPSocket();
                }
            } catch (final IOException e) {
                throw PrimitiveFailed.andTransferToInterpreter();
            }
            return PointersObject.newHandleWithHiddenObject(getContext(node), socket);
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private static SqueakUDPSocket createSqueakUDPSocket() throws IOException {
            return new SqueakUDPSocket();
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private static SqueakTCPSocket createSqueakTCPSocket() throws IOException {
            return new SqueakTCPSocket();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketAccept3Semaphores")
    protected abstract static class PrimSocketAccept3SemaphoresNode extends AbstractPrimitiveNode implements Primitive6WithFallback {
        @SuppressWarnings("unused")
        @Specialization
        protected final PointersObject doAccept(final Object receiver,
                        final PointersObject sd,
                        final long receiveBufferSize,
                        final long sendBufSize,
                        final long semaphoreIndex,
                        final long readSemaphoreIndex,
                        final long writeSemaphoreIndex) {
            try {
                return PointersObject.newHandleWithHiddenObject(getContext(), accept(sd));
            } catch (final IOException e) {
                LogUtils.SOCKET.log(Level.FINE, "Accepting socket failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private static SqueakSocket accept(final PointersObject sd) throws IOException {
            return getSocketOrPrimFail(sd).accept();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketCreate")
    protected abstract static class PrimSocketCreateNode extends AbstractPrimitiveNode implements Primitive5WithFallback {
        @SuppressWarnings("unused")
        @Specialization
        protected static long doWork(final PointersObject receiver,
                        final long netType,
                        final long socketType,
                        final long rcvBufSize,
                        final long sendBufSize,
                        final long semaphoreIndex) {
            // TODO: primitiveSocketCreate
            throw PrimitiveFailed.andTransferToInterpreter();
        }
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    private static void close(final PointersObject sd) throws IOException {
        getSocketOrPrimFail(sd).close();
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return SocketPluginFactory.getFactories();
    }
}
