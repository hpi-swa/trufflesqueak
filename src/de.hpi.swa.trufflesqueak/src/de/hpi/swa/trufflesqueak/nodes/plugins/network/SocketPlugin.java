/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
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
import de.hpi.swa.trufflesqueak.nodes.plugins.network.SqueakOpaqueSocketAddress.AddressInfo;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive0;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive2WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive3WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive4WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive5WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive6WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive7WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.util.LogUtils;
import de.hpi.swa.trufflesqueak.util.UnsafeUtils;

public final class SocketPlugin extends AbstractPrimitiveFactoryHolder {
    private static final boolean HAS_SOCKET_ACCESS;
    static final byte[] LOCAL_HOST_NAME;

    static {
        boolean hasSocketAccess = false;
        String localHostName = "unknown";
        try {
            localHostName = InetAddress.getLocalHost().getHostName();
            hasSocketAccess = true;
        } catch (final SecurityException | UnknownHostException e) {
            LogUtils.MAIN.warning(e.toString());
        }
        HAS_SOCKET_ACCESS = hasSocketAccess;
        LOCAL_HOST_NAME = localHostName.getBytes();
    }

    protected abstract static class AbstractNetworkPrimitiveNode extends AbstractPrimitiveNode {
        protected final Resolver getResolver() {
            return getContext().squeakSocketContext.getResolver();
        }

        protected final SqueakSocketContext getSocketContext() {
            return getContext().squeakSocketContext;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveHasSocketAccess")
    protected abstract static class PrimHasSocketAccessNode extends AbstractNetworkPrimitiveNode implements Primitive0 {
        @Specialization
        protected static boolean hasSocketAccess(@SuppressWarnings("unused") final Object receiver) {
            return BooleanObject.wrap(HAS_SOCKET_ACCESS);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveInitializeNetwork")
    protected abstract static class PrimInitializeNetwork1Node extends AbstractNetworkPrimitiveNode implements Primitive1 {
        @Specialization
        protected final Object doWork(final Object receiver, final long resolverSemaIndex) {
            if (resolverSemaIndex > 0) {
                final int semaIndex = (int) resolverSemaIndex;
                final SqueakImageContext image = getContext();
                getResolver().setStatusChangeCallback(() ->
                        image.interrupt.signalSemaphoreWithIndex(semaIndex)
                );
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveResolverLocalAddress")
    protected abstract static class PrimResolverLocalAddressNode extends AbstractNetworkPrimitiveNode implements Primitive0 {
        @Specialization
        protected final AbstractSqueakObject doWork(@SuppressWarnings("unused") final Object receiver) {
            final byte[] address = getResolver().getLoopbackAddress();
            LogUtils.SOCKET.finer(() -> "Local Address: " + SqueakOpaqueSocketAddress.getIpAddressString(address, getSocketContext().getSessionID()));
            return getContext().asByteArray(address);
        }
    }

    @GenerateNodeFactory
    @ImportStatic(SocketPlugin.class)
    @SqueakPrimitive(names = "primitiveResolverHostNameResult")
    protected abstract static class PrimResolverHostNameResultNode extends AbstractNetworkPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"targetString.isByteType()", "targetString.getByteLength() >= LOCAL_HOST_NAME.length"})
        protected static final Object doResult(@SuppressWarnings("unused") final Object receiver, final NativeObject targetString) {
            UnsafeUtils.copyBytes(LOCAL_HOST_NAME, 0, targetString.getByteStorage(), 0, LOCAL_HOST_NAME.length);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveResolverHostNameSize")
    protected abstract static class PrimResolverHostNameSizeNode extends AbstractNetworkPrimitiveNode implements Primitive0 {
        @Specialization
        protected static final long doSize(@SuppressWarnings("unused") final Object receiver) {
            return LOCAL_HOST_NAME.length;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveResolverGetAddressInfo")
    protected abstract static class PrimResolverGetAddressInfoNode extends AbstractNetworkPrimitiveNode implements Primitive6WithFallback {
        @Specialization
        protected final Object doWork(final Object receiver, final Object hostName, final Object servName, final long flags, final long family, final long type, final long protocol) {
            final String host = (hostName instanceof NativeObject no && no.isByteType()) ? no.asStringUnsafe() : null;
            final String serv = (servName instanceof NativeObject no && no.isByteType()) ? no.asStringUnsafe() : null;

            getResolver().setGlobalAddressInfoResult(host, serv, (int) flags, (int) family, (int) type, (int) protocol);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveResolverGetAddressInfoSize")
    protected abstract static class PrimResolverGetAddressInfoSizeNode extends AbstractNetworkPrimitiveNode implements Primitive0 {
        @Specialization
        protected final long doSize(@SuppressWarnings("unused") final Object receiver) {
            return getResolver().getGlobalAddressInfoSize();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveResolverGetAddressInfoFamily")
    protected abstract static class PrimResolverGetAddressInfoFamilyNode extends AbstractNetworkPrimitiveNode implements Primitive0 {
        @Specialization
        protected final long doFamily(@SuppressWarnings("unused") final Object receiver) {
            return getResolver().getGlobalAddressInfoFamily();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveResolverGetAddressInfoType")
    protected abstract static class PrimResolverGetAddressInfoTypeNode extends AbstractNetworkPrimitiveNode implements Primitive0 {
        @Specialization
        protected final long doType(@SuppressWarnings("unused") final Object receiver) {
            return getResolver().getGlobalAddressInfoType();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveResolverGetAddressInfoProtocol")
    protected abstract static class PrimResolverGetAddressInfoProtocolNode extends AbstractNetworkPrimitiveNode implements Primitive0 {
        @Specialization
        protected final long doProtocol(@SuppressWarnings("unused") final Object receiver) {
            return getResolver().getGlobalAddressInfoProtocol();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveResolverGetAddressInfoResult")
    protected abstract static class PrimResolverGetAddressInfoResultNode extends AbstractNetworkPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "socketAddress.isByteType()")
        protected final Object doResult(final Object receiver, final NativeObject socketAddress) {
            final byte[] bytes = getResolver().getGlobalAddressInfoResultBytes();
            if (bytes == null) {
                throw PrimitiveFailed.andTransferToInterpreter();
            }
            final byte[] buffer = socketAddress.getByteStorage();
            System.arraycopy(bytes, 0, buffer, 0, Math.min(bytes.length, buffer.length));
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveResolverGetAddressInfoNext")
    protected abstract static class PrimResolverGetAddressInfoNextNode extends AbstractNetworkPrimitiveNode implements Primitive0 {
        @Specialization
        protected final Object doNext(@SuppressWarnings("unused") final Object receiver) {
            return BooleanObject.wrap(getResolver().advanceGlobalAddressInfo());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveResolverGetNameInfo")
    protected abstract static class PrimResolverGetNameInfoNode extends AbstractNetworkPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = "address.isByteType()")
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doWork(final Object receiver, final NativeObject address, final long flags) {
            getResolver().getNameInfo(address.getByteStorage(), (int) flags);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveResolverGetNameInfoHostSize")
    protected abstract static class PrimResolverGetNameInfoHostSizeNode extends AbstractNetworkPrimitiveNode implements Primitive0 {
        @Specialization
        protected final long doSize(@SuppressWarnings("unused") final Object receiver) {
            return getResolver().getNameInfoHostSize();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveResolverGetNameInfoHostResult")
    protected abstract static class PrimResolverGetNameInfoHostResultNode extends AbstractNetworkPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "targetString.isByteType()")
        protected final Object doResult(final Object receiver, final NativeObject targetString) {
            final byte[] bytes = getResolver().getNameInfoHostResult();
            System.arraycopy(bytes, 0, targetString.getByteStorage(), 0, Math.min(bytes.length, targetString.getByteLength()));
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveResolverGetNameInfoServiceSize")
    protected abstract static class PrimResolverGetNameInfoServiceSizeNode extends AbstractNetworkPrimitiveNode implements Primitive0 {
        @Specialization
        protected final long doSize(@SuppressWarnings("unused") final Object receiver) {
            return getResolver().getNameInfoServiceSize();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveResolverGetNameInfoServiceResult")
    protected abstract static class PrimResolverGetNameInfoServiceResultNode extends AbstractNetworkPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "targetString.isByteType()")
        protected final Object doResult(final Object receiver, final NativeObject targetString) {
            final byte[] bytes = getResolver().getNameInfoServiceResult();
            System.arraycopy(bytes, 0, targetString.getByteStorage(), 0, Math.min(bytes.length, targetString.getByteLength()));
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
    @SqueakPrimitive(names = "primitiveSocketLocalPort")
    protected abstract static class PrimSocketLocalPortNode extends AbstractNetworkPrimitiveNode implements Primitive1WithFallback {
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
    @SqueakPrimitive(names = "primitiveSocketLocalAddressResult")
    protected abstract static class PrimSocketLocalAddressResultNode extends AbstractNetworkPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = "address.isByteType()")
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doResult(final Object receiver, final PointersObject sd, final NativeObject address) {
            try {
                final InetAddress localAddr = InetAddress.getByAddress(getSocketOrPrimFail(sd).getLocalAddress());
                final AddressInfo info = new AddressInfo(localAddr, (int) getSocketOrPrimFail(sd).getLocalPort(), 0, 0, 0);
                final byte[] opaqueBytes = info.toSockaddrBytes(getSocketContext().getSessionID());
                System.arraycopy(opaqueBytes, 0, address.getByteStorage(), 0, Math.min(opaqueBytes.length, address.getByteLength()));
            } catch (final Exception e) {
                throw PrimitiveFailed.andTransferToInterpreter();
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketRemoteAddressResult")
    protected abstract static class PrimSocketRemoteAddressResultNode extends AbstractNetworkPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = "address.isByteType()")
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doResult(final Object receiver, final PointersObject sd, final NativeObject address) {
            try {
                final InetAddress remoteAddr = InetAddress.getByAddress(getSocketOrPrimFail(sd).getRemoteAddress());
                final AddressInfo info = new AddressInfo(remoteAddr, (int) getSocketOrPrimFail(sd).getRemotePort(), 0, 0, 0);
                final byte[] opaqueBytes = info.toSockaddrBytes(getSocketContext().getSessionID());
                System.arraycopy(opaqueBytes, 0, address.getByteStorage(), 0, Math.min(opaqueBytes.length, address.getByteLength()));
            } catch (final Exception e) {
                throw PrimitiveFailed.andTransferToInterpreter();
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketBindTo")
    protected abstract static class PrimSocketBindToNode extends AbstractNetworkPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = "address.isByteType()")
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doBind(final Object receiver, final PointersObject sd, final NativeObject address) {
            try {
                final InetSocketAddress socketAddress = SqueakOpaqueSocketAddress.unpack(address.getByteStorage(), getSocketContext().getSessionID());
                getSocketOrPrimFail(sd).bindTo(socketAddress.getAddress().getHostAddress(), socketAddress.getPort());
            } catch (final Exception e) {
                LogUtils.SOCKET.log(Level.FINE, "Socket bindTo failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketBindToPort")
    protected abstract static class PrimSocketBindToPortNode extends AbstractNetworkPrimitiveNode implements Primitive3WithFallback {
        @Specialization(guards = "address.isByteType()")
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doBind(final Object receiver, final PointersObject sd, final NativeObject address, final long port) {
            try {
                final SqueakSocket socket = getSocketOrPrimFail(sd);
                final String host = SqueakOpaqueSocketAddress.getIpAddressString(address.getByteStorage(), getSocketContext().getSessionID());
                socket.bindTo(host, (int) port);
            } catch (final IOException e) {
                LogUtils.SOCKET.log(Level.FINE, "Socket bindToPort failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketListenOnPort")
    protected abstract static class PrimSocketListenOnPortNode extends AbstractNetworkPrimitiveNode implements Primitive2WithFallback {
        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected static final Object doListen(final Object receiver, final PointersObject sd, final long port) {
            try {
                getSocketOrPrimFail(sd).listenOn(null, port, 1L);
            } catch (final IOException e) {
                throw PrimitiveFailed.andTransferToInterpreter();
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketListenWithBacklog")
    protected abstract static class PrimSocketListenWithBacklogNode extends AbstractNetworkPrimitiveNode implements Primitive2WithFallback {
        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected static final Object doListen(final Object receiver, final PointersObject sd, final long backlog) {
            try {
                getSocketOrPrimFail(sd).listenBacklog(backlog);
            } catch (final IOException e) {
                throw PrimitiveFailed.andTransferToInterpreter();
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketListenWithOrWithoutBacklog")
    protected abstract static class PrimSocketListenWithOrWithoutBacklog3Node extends AbstractNetworkPrimitiveNode implements Primitive2WithFallback {
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
            getSocketOrPrimFail(sd).listenOn(null, port, 0L);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketListenWithOrWithoutBacklog")
    protected abstract static class PrimSocketListenWithOrWithoutBacklog4Node extends AbstractNetworkPrimitiveNode implements Primitive3WithFallback {
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
            getSocketOrPrimFail(sd).listenOn(null, port, backlogSize);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketListenOnPortBacklogInterface")
    protected abstract static class PrimSocketListenOnPortBacklogInterfaceNode extends AbstractNetworkPrimitiveNode implements Primitive4WithFallback {
        /**
         * Set up the socket to listen on the given port. Will be used in conjunction with #accept
         * only.
         */
        @Specialization(guards = "interfaceAddress.isByteType()")
        protected final Object doListen(final Object receiver, final PointersObject sd, final long port, final long backlogSize, final NativeObject interfaceAddress) {
            try {
                final String ifaceStr = SqueakOpaqueSocketAddress.getIpAddressString(interfaceAddress.getByteStorage(), getSocketContext().getSessionID());
                listenOn(sd, ifaceStr, port, backlogSize);
            } catch (final IOException e) {
                LogUtils.SOCKET.log(Level.FINE, "Listen failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
            return receiver;
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private static void listenOn(final PointersObject sd, final String address, final long port, final long backlogSize) throws IOException {
            getSocketOrPrimFail(sd).listenOn(address, port, backlogSize);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketSetOptions")
    protected abstract static class PrimSocketSetOptionsNode extends AbstractNetworkPrimitiveNode implements Primitive3WithFallback {
        @Specialization(guards = "option.isByteType()")
        protected final ArrayObject doSet(@SuppressWarnings("unused") final Object receiver, final PointersObject sd, final NativeObject option, final NativeObject value) {
            return setSocketOption(getContext(), getSocketOrPrimFail(sd), option.asStringUnsafe(), value.asStringUnsafe());
        }

        @TruffleBoundary
        private static ArrayObject setSocketOption(final SqueakImageContext image, final SqueakSocket socket, final String option, final String value) {
            try {
                if (socket.supportsOption(option)) {
                    socket.setOption(option, value);
                    return image.asArrayOfObjects(0L, image.asByteString(value));
                }
            } catch (final Exception e) {
                // Safely absorb UnsupportedOperationException for options like SO_REUSEPORT on Mac
            }
            return image.asArrayOfObjects(1L, image.asByteString("0"));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketConnectTo")
    protected abstract static class PrimSocketConnectToNode extends AbstractNetworkPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = "address.isByteType()")
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doConnect(final Object receiver, final PointersObject sd, final NativeObject address) {
            try {
                final InetSocketAddress socketAddress = SqueakOpaqueSocketAddress.unpack(address.getByteStorage(), getSocketContext().getSessionID());
                getSocketOrPrimFail(sd).connectTo(socketAddress.getAddress().getHostAddress(), socketAddress.getPort());
            } catch (final Exception e) {
                LogUtils.SOCKET.log(Level.FINE, "Socket connectTo failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketConnectToPort")
    protected abstract static class PrimSocketConnectToPortNode extends AbstractNetworkPrimitiveNode implements Primitive3WithFallback {
        @Specialization(guards = "hostAddress.isByteType()")
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final long doConnectToPort(
                        @SuppressWarnings("unused") final Object receiver, final PointersObject sd,
                        final NativeObject hostAddress, final long port) {
            try {
                final SqueakSocket socket = getSocketOrPrimFail(sd);
                final String host = SqueakOpaqueSocketAddress.getIpAddressString(hostAddress.getByteStorage(), getSocketContext().getSessionID());
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
    protected abstract static class PrimSocketConnectionStatusNode extends AbstractNetworkPrimitiveNode implements Primitive1WithFallback {
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
    @SqueakPrimitive(names = "primitiveSocketLocalAddressSize")
    protected abstract static class PrimSocketLocalAddressSizeNode extends AbstractNetworkPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected static final long doSize(@SuppressWarnings("unused") final Object receiver, final PointersObject sd) {
            try {
                return SqueakOpaqueSocketAddress.getOpaqueAddressSize(getSocketOrPrimFail(sd).getLocalAddress());
            } catch (final Exception e) {
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketRemoteAddressSize")
    protected abstract static class PrimSocketRemoteAddressSizeNode extends AbstractNetworkPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected static final long doSize(@SuppressWarnings("unused") final Object receiver, final PointersObject sd) {
            try {
                return SqueakOpaqueSocketAddress.getOpaqueAddressSize(getSocketOrPrimFail(sd).getRemoteAddress());
            } catch (final Exception e) {
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketAddressGetPort")
    protected abstract static class PrimSocketAddressGetPortNode extends AbstractNetworkPrimitiveNode implements Primitive0 {
        @Specialization(guards = "address.isByteType()")
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final long doGetPort(final NativeObject address) {
            try {
                return SqueakOpaqueSocketAddress.getPort(address.getByteStorage(), getSocketContext().getSessionID());
            } catch (final Exception e) {
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketAddressSetPort")
    protected abstract static class PrimSocketAddressSetPortNode extends AbstractNetworkPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "address.isByteType()")
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doSetPort(final NativeObject address, final long port) {
            try {
                SqueakOpaqueSocketAddress.setPort(address.getByteStorage(), (int) port, getSocketContext().getSessionID());
            } catch (final Exception e) {
                throw PrimitiveFailed.andTransferToInterpreter();
            }
            return address;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketRemoteAddress")
    protected abstract static class PrimSocketRemoteAddressNode extends AbstractNetworkPrimitiveNode implements Primitive1WithFallback {
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
    protected abstract static class PrimSocketRemotePortNode extends AbstractNetworkPrimitiveNode implements Primitive1WithFallback {
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
    protected abstract static class PrimSocketGetOptionsNode extends AbstractNetworkPrimitiveNode implements Primitive2WithFallback {
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
    protected abstract static class PrimSocketReceiveDataAvailableNode extends AbstractNetworkPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected static final boolean doDataAvailable(@SuppressWarnings("unused") final Object receiver, final PointersObject sd) {
            try {
                return getSocketOrPrimFail(sd).isDataAvailable();
            } catch (final IOException e) {
                LogUtils.SOCKET.log(Level.FINE, "Checking for available data failed", e);
                return false;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketError")
    protected abstract static class PrimSocketErrorNode extends AbstractNetworkPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected static long doWork(final Object receiver, final PointersObject sd) {
            try {
                return getSocketOrPrimFail(sd).socketError;
            } catch (PrimitiveFailed e) {
                throw e;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketLocalAddress")
    protected abstract static class PrimSocketLocalAddressNode extends AbstractNetworkPrimitiveNode implements Primitive1WithFallback {
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
    protected abstract static class PrimSocketSendDataBufCountNode extends AbstractNetworkPrimitiveNode implements Primitive4WithFallback {
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
    protected abstract static class PrimSocketCloseConnectionNode extends AbstractNetworkPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final Object doClose(final Object receiver, final PointersObject sd) {
            try {
                getSocketOrPrimFail(sd).close();
            } catch (final IOException e) {
                LogUtils.SOCKET.log(Level.FINE, "Closing socket failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketAbortConnection")
    protected abstract static class PrimSocketAbortConnectionNode extends AbstractNetworkPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final Object doAbort(final Object receiver, final PointersObject sd) {
            try {
                getSocketOrPrimFail(sd).close();
            } catch (final IOException e) {
                LogUtils.SOCKET.log(Level.FINE, "Aborting socket connection failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketSendDone")
    protected abstract static class PrimSocketSendDoneNode extends AbstractNetworkPrimitiveNode implements Primitive1WithFallback {
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
    protected abstract static class PrimSocketReceiveDataBufCountNode extends AbstractNetworkPrimitiveNode implements Primitive4WithFallback {
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
    protected abstract static class PrimSocketDestroyNode extends AbstractNetworkPrimitiveNode implements Primitive1WithFallback {
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
    protected abstract static class PrimSocketCreate3SemaphoresNode extends AbstractNetworkPrimitiveNode implements Primitive7WithFallback {
        @SuppressWarnings("unused")
        @Specialization
        protected final PointersObject doWork(final PointersObject receiver,
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
                    socket = createSqueakUDPSocket(getSocketContext(), netType, semaphoreIndex, aReadSemaphore, aWriteSemaphore);
                } else {
                    socket = createSqueakTCPSocket(getSocketContext(), netType, semaphoreIndex, aReadSemaphore, aWriteSemaphore);
                }
            } catch (final IOException e) {
                throw PrimitiveFailed.andTransferToInterpreter();
            }
            return PointersObject.newHandleWithHiddenObject(getContext(node), socket);
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private static SqueakUDPSocket createSqueakUDPSocket(final SqueakSocketContext socketContext, final long netType, final long statusSema, final long readSema, final long writeSema) throws IOException {
            return new SqueakUDPSocket(socketContext, netType, statusSema, readSema, writeSema);
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private static SqueakTCPSocket createSqueakTCPSocket(final SqueakSocketContext socketContext, final long netType, final long statusSema, final long readSema, final long writeSema) throws IOException {
            return new SqueakTCPSocket(socketContext, netType, statusSema, readSema, writeSema);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketAccept3Semaphores")
    protected abstract static class PrimSocketAccept3SemaphoresNode extends AbstractNetworkPrimitiveNode implements Primitive6WithFallback {
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
                return PointersObject.newHandleWithHiddenObject(getContext(), accept(sd, semaphoreIndex, readSemaphoreIndex, writeSemaphoreIndex));
            } catch (final IOException e) {
                LogUtils.SOCKET.log(Level.FINE, "Accepting socket failed", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private static SqueakSocket accept(final PointersObject sd, final long statusSema, final long readSema, final long writeSema) throws IOException {
            final SqueakSocket socket = getSocketOrPrimFail(sd).accept(statusSema, readSema, writeSema);
            if (socket == null) {
                throw PrimitiveFailed.andTransferToInterpreter();
            }
            return socket;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSocketCreate")
    protected abstract static class PrimSocketCreateNode extends AbstractNetworkPrimitiveNode implements Primitive5WithFallback {
        @SuppressWarnings("unused")
        @Specialization
        protected final PointersObject doWork(final PointersObject receiver,
                         final long netType,
                         final long socketType,
                         final long rcvBufSize,
                         final long sendBufSize,
                         final long semaphoreIndex,
                         @Bind final Node node,
                         @Cached final InlinedConditionProfile socketTypeProfile) {
            final SqueakSocket socket;
            try {
                if (socketTypeProfile.profile(node, socketType == 1)) {
                    socket = createSqueakUDPSocket(getSocketContext(), netType, semaphoreIndex, semaphoreIndex, semaphoreIndex);
                } else {
                    socket = createSqueakTCPSocket(getSocketContext(), netType, semaphoreIndex, semaphoreIndex, semaphoreIndex);
                }
            } catch (final IOException e) {
                throw PrimitiveFailed.andTransferToInterpreter();
            }
            return PointersObject.newHandleWithHiddenObject(getContext(node), socket);
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private static SqueakUDPSocket createSqueakUDPSocket(final SqueakSocketContext socketContext, final long netType, final long statusSema, final long readSema, final long writeSema) throws IOException {
            return new SqueakUDPSocket(socketContext, netType, statusSema, readSema, writeSema);
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private static SqueakTCPSocket createSqueakTCPSocket(final SqueakSocketContext socketContext, final long netType, final long statusSema, final long readSema, final long writeSema) throws IOException {
            return new SqueakTCPSocket(socketContext, netType, statusSema, readSema, writeSema);
        }
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    private static void close(final PointersObject sd) throws IOException {
        getSocketOrPrimFail(sd).close();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveResolverStatus")
    protected abstract static class PrimResolverStatusNode extends AbstractNetworkPrimitiveNode implements Primitive0 {
        @Specialization
        protected long doWork(@SuppressWarnings("unused") final Object receiver) {
            return getResolver().getLegacyStatus().id();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveResolverStartNameLookup")
    protected abstract static class PrimResolverStartNameLookupNode extends AbstractNetworkPrimitiveNode implements Primitive1WithFallback {
        /**
         * Look up the given host name in the Domain Name Server to find its address. This call is
         * asynchronous. To get the results, wait for it to complete or time out and then use
         * primNameLookupResult.
         */
        @Specialization(guards = "hostName.isByteType()")
        protected final Object doWork(final Object receiver, final NativeObject hostName) {
            LogUtils.SOCKET.finer(() -> "Starting lookup for host name " + hostName);
            getResolver().startHostNameLookUp(hostName.asStringUnsafe());
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveResolverStartAddressLookup")
    protected abstract static class PrimResolverStartAddressLookupNode extends AbstractNetworkPrimitiveNode implements Primitive1WithFallback {
        /**
         * Look up the given host address in the Domain Name Server to find its name. This call is
         * asynchronous. To get the results, wait for it to complete or time out and then use
         * primAddressLookupResult.
         */
        @Specialization(guards = "address.isByteType()")
        protected final Object doWork(final Object receiver, final NativeObject address) {
            LogUtils.SOCKET.finer(() -> "Starting lookup for address " + address);
            getResolver().startAddressLookUp(address.getByteStorage());
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveResolverNameLookupResult")
    protected abstract static class PrimResolverNameLookupResultNode extends AbstractNetworkPrimitiveNode implements Primitive0 {
        /**
         * Return the host address found by the last host name lookup. Returns nil if the last
         * lookup was unsuccessful.
         */
        @Specialization
        protected final AbstractSqueakObject doWork(@SuppressWarnings("unused") final Object receiver,
                                                           @Bind final Node node,
                                                           @Cached final InlinedConditionProfile hasResultProfile) {
            final byte[] lastNameLookup = getResolver().lastHostNameLookupResult();
            LogUtils.SOCKET.finer(() -> "Name Lookup Result: " + SqueakOpaqueSocketAddress.getIpAddressString(lastNameLookup, getSocketContext().getSessionID()));
            return hasResultProfile.profile(node, lastNameLookup == null) ? NilObject.SINGLETON : getContext(node).asByteArray(lastNameLookup);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveResolverAddressLookupResult")
    protected abstract static class PrimResolverAddressLookupResultNode extends AbstractNetworkPrimitiveNode implements Primitive0 {
        /**
         * Return the host name found by the last host address lookup. Returns nil if the last
         * lookup was unsuccessful.
         */
        @Specialization
        protected final AbstractSqueakObject doWork(@SuppressWarnings("unused") final Object receiver) {
            final String lastAddressLookup = getResolver().lastAddressLookUpResult();
            LogUtils.SOCKET.finer(() -> ">> Address Lookup Result: " + lastAddressLookup);
            return lastAddressLookup == null ? NilObject.SINGLETON : getContext().asByteString(lastAddressLookup);
        }
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return SocketPluginFactory.getFactories();
    }
}
