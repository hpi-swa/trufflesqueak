/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins.network;

import static java.net.StandardProtocolFamily.INET;
import static java.net.StandardProtocolFamily.INET6;

import static de.hpi.swa.trufflesqueak.nodes.plugins.network.SqueakSocketContext.SQ_FAMILY_INET4;
import static de.hpi.swa.trufflesqueak.nodes.plugins.network.SqueakSocketContext.SQ_FAMILY_INET6;

import java.io.IOException;
import java.net.BindException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.ProtocolFamily;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.NetworkChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.nodes.interrupts.CheckForInterruptsState;
import de.hpi.swa.trufflesqueak.util.LogUtils;
import de.hpi.swa.trufflesqueak.util.OS;

public abstract class SqueakSocket {

    enum Status {
        InvalidSocket(-1),
        Unconnected(0),
        WaitingForConnection(1),
        Connected(2),
        OtherEndClosed(3),
        ThisEndClosed(4);

        private final long id;

        Status(final long id) {
            this.id = id;
        }

        long id() {
            return id;
        }
    }

    // Standard POSIX error mappings
    protected static final int POSIX_EADDRINUSE = resolveErrno(48, 98, 10048);    // Address alread in use
    protected static final int POSIX_ECONNRESET = resolveErrno(54, 104, 10054);   // Connection reset by peer
    protected static final int POSIX_ECONNREFUSED = resolveErrno(61, 111, 10061); // Connection refused
    protected static final int POSIX_EHOSTUNREACH = resolveErrno(65, 113, 10065); // No route to host
    protected static final int POSIX_EIO = resolveErrno(5, 5, 10022);             // Input/output error
    protected static final int POSIX_EPERM = resolveErrno(1, 1, 10013);           // Operation not permitted
    protected static final int POSIX_ETIMEDOUT = resolveErrno(60, 110, 10060);    // Connection timed out
    protected static final int POSIX_EWOULDBLOCK = resolveErrno(35, 11, 10035);   // Resource temporarily unavailable

    public int socketError;

    protected final SqueakSocketContext context;

    protected boolean listening;

    protected final long netType;
    protected final long statusSema;
    protected final long readSema;
    protected final long writeSema;

    protected volatile boolean dataAvailable;
    protected volatile boolean writeReady = true;

    protected SqueakSocket(final SqueakSocketContext context, final long netType, final long statusSema, final long readSema, final long writeSema) {
        this.context = context;
        this.netType = netType;
        listening = false;
        this.statusSema = statusSema;
        this.readSema = readSema;
        this.writeSema = writeSema;
    }

    private static int resolveErrno(final int mac, final int linux, final int win) {
        if (OS.isWindows()) {
            return win;
        } else if (OS.isMacOS()) {
            return mac;
        }
        return linux; // Default to standard POSIX/Linux
    }

    protected Resolver getResolver() {
        return context.getResolver();
    }

    protected boolean specifiesProtocolFamily() {
        return (netType == SQ_FAMILY_INET6) || (netType == SQ_FAMILY_INET4);
    }

    protected ProtocolFamily getProtocolFamily() {
        return netType == SQ_FAMILY_INET6 ? INET6 : INET;
    }

    protected abstract NetworkChannel asNetworkChannel();

    protected abstract SelectableChannel asSelectableChannel();

    protected abstract InetSocketAddress getLocalSocketAddress() throws IOException;

    protected final byte[] getLocalAddress() throws IOException {
        return getLocalSocketAddress().getAddress().getAddress();
    }

    protected abstract long getLocalPort() throws IOException;

    protected abstract InetSocketAddress getRemoteSocketAddress() throws IOException;

    protected final byte[] getRemoteAddress() throws IOException {
        return getRemoteSocketAddress().getAddress().getAddress();
    }

    protected abstract long getRemotePort() throws IOException;

    protected abstract Status getStatus() throws IOException;

    protected abstract void connectTo(String address, long port) throws IOException;

    protected abstract void bindTo(String address, int port) throws IOException;

    protected abstract void listenBacklog(long backlogSize) throws IOException;

    protected abstract void listenOn(String address, long port, long backlogSize) throws IOException;

    protected abstract SqueakSocket accept(long acceptStatusSema, long acceptReadSema, long acceptWriteSema) throws IOException;

    protected abstract boolean isInputShutdown();

    protected abstract boolean isOutputShutdown();

    protected abstract void close() throws IOException;

    protected static int mapExceptionToErrno(final IOException e) {
        // Fast paths for specific Java network exceptions
        if (e instanceof BindException) {
            return POSIX_EADDRINUSE;
        }
        if (e instanceof SocketTimeoutException) {
            return POSIX_ETIMEDOUT;
        }
        if (e instanceof NoRouteToHostException) {
            return POSIX_EHOSTUNREACH;
        }

        final String msg = e.getMessage();
        if (msg == null) {
            return POSIX_EIO; // Use EIO (5) for generic errors
        }

        // String matching fallbacks for generic IOExceptions
        if (msg.contains("Connection reset") || msg.contains("Broken pipe")) {
            return POSIX_ECONNRESET;
        }
        if (msg.contains("Connection refused")) {
            return POSIX_ECONNREFUSED;
        }
        if (msg.contains("Address already in use")) {
            return POSIX_EADDRINUSE;
        }
        if (msg.contains("timed out")) {
            return POSIX_ETIMEDOUT;
        }
        if (msg.contains("Host is unreachable") || msg.contains("No route to host")) {
            return POSIX_EHOSTUNREACH;
        }
        if (msg.contains("Resource temporarily unavailable")) {
            return POSIX_EWOULDBLOCK;
        }

        return POSIX_EIO;
    }

    protected boolean isSendDone() {
        try {
            return writeReady && getStatus() == Status.Connected;
        } catch (final IOException e) {
            return false;
        }
    }

    protected final long sendData(final byte[] data, final int start, final int count) {
        final ByteBuffer buffer = ByteBuffer.wrap(data, start, count);
        try {
            final long written = sendDataTo(buffer);
            if (written == 0 && count > 0) {
                writeReady = false; // Buffer is full, flag it to wait for OP_WRITE
            }
            LogUtils.SOCKET.finer(() -> this + " written: " + written);
            socketError = 0;
            return written;
        } catch (final IOException e) {
            socketError = mapExceptionToErrno(e);
            writeReady = false;
            return 0;
        } finally {
            if (!isOutputShutdown()) {
                context.resumeInterest(asSelectableChannel(), SelectionKey.OP_WRITE);
            }
        }
    }

    protected abstract long sendDataTo(ByteBuffer data) throws IOException;

    protected boolean isDataAvailable() {
        return dataAvailable && !isInputShutdown();
    }

    protected final long receiveData(final byte[] data, final int start, final int count) {
        final ByteBuffer buffer = ByteBuffer.wrap(data, start, count);
        try {
            final long received = receiveDataFrom(buffer);
            LogUtils.SOCKET.finer(() -> this + " received: " + received);
            socketError = 0;
            return received;
        } catch (final IOException e) {
            socketError = mapExceptionToErrno(e);
            return 0;
        } finally {
            dataAvailable = false;
            if (!isInputShutdown()) {
                context.resumeInterest(asSelectableChannel(), SelectionKey.OP_READ);
            }
        }
    }

    protected abstract long receiveDataFrom(ByteBuffer data) throws IOException;

    protected final boolean supportsOption(final String name) {
        return asNetworkChannel().supportedOptions().stream().anyMatch(o -> o.name().equals(name));
    }

    protected final String getOption(final String name) throws IOException {
        final SocketOption<?> option = socketOptionFromString(name);
        final Object value = asNetworkChannel().getOption(option);
        if (value instanceof Boolean b) {
            return b ? "1" : "0";
        }
        return String.valueOf(value);
    }

    protected final void setOption(final String name, final String value) throws IOException {
        final Boolean enabled = "1".equals(value);
        final SocketOption<?> option = socketOptionFromString(name);
        setOptionUnchecked(option, enabled);
    }

    private SocketOption<?> socketOptionFromString(final String name) {
        return asNetworkChannel().supportedOptions().stream().filter(o -> o.name().equals(name)).findFirst().orElseThrow(() -> new UnsupportedOperationException("Unknown socket option: " + name));
    }

    @SuppressWarnings("unchecked")
    private <T> void setOptionUnchecked(final SocketOption<T> opt, final Object value) throws IOException {
        asNetworkChannel().setOption(opt, (T) value);
    }

    protected void handleReadyOps(final SelectionKey key, final CheckForInterruptsState interrupts) {
        final int ready = key.readyOps();
        int currentOps = key.interestOps();

        if ((ready & (SelectionKey.OP_ACCEPT | SelectionKey.OP_CONNECT)) != 0 && statusSema > 0) {
            interrupts.signalSemaphoreWithIndex((int) statusSema);
            currentOps &= ~(SelectionKey.OP_ACCEPT | SelectionKey.OP_CONNECT);
        }
        if ((ready & SelectionKey.OP_READ) != 0) {
            dataAvailable = true;
            if (readSema > 0) {
                interrupts.signalSemaphoreWithIndex((int) readSema);
            }
            currentOps &= ~SelectionKey.OP_READ;
        }
        if ((ready & SelectionKey.OP_WRITE) != 0) {
            writeReady = true; // Buffer is ready for writing again
            if (writeSema > 0) {
                interrupts.signalSemaphoreWithIndex((int) writeSema);
            }
            currentOps &= ~SelectionKey.OP_WRITE;
        }

        key.interestOps(currentOps);
    }

    protected InetSocketAddress socketAddressFor(final String address, final long port) {
        if (address == null || address.isEmpty()) {
            return new InetSocketAddress(getResolver().getAnyLocalInetAddress(netType), (int) port);
        }
        return new InetSocketAddress(address, (int) port);
    }

    protected InetSocketAddress loopbackAddressFor(final long port) {
        return new InetSocketAddress(getResolver().getLoopbackInetAddress(netType), (int) port);
    }

    protected static InetSocketAddress castAddress(final SocketAddress address) {
        if (address == null) {
            return null;
        }

        if (address instanceof final InetSocketAddress o) {
            return o;
        }
        throw SqueakException.create("Unknown address type");
    }

    protected InetSocketAddress coerceToNetType(final InetSocketAddress addr) {
        if (addr != null && addr.getAddress() != null) {
            final InetAddress ip = addr.getAddress();

            // If Squeak requested IPv4 but Java promoted it to IPv6
            if (netType == SQ_FAMILY_INET4 && ip instanceof Inet6Address) {
                if (ip.isAnyLocalAddress()) {
                    return new InetSocketAddress("0.0.0.0", addr.getPort());
                } else if (ip.isLoopbackAddress()) {
                    return new InetSocketAddress("127.0.0.1", addr.getPort());
                }
            } else if (netType == SQ_FAMILY_INET6 && ip instanceof Inet4Address) {
                // Squeak requested IPv6 but Java returned IPv4
                if (ip.isAnyLocalAddress()) {
                    return new InetSocketAddress("::", addr.getPort());
                } else if (ip.isLoopbackAddress()) {
                    return new InetSocketAddress("::1", addr.getPort());
                }
            }
        }
        return addr;
    }
}
