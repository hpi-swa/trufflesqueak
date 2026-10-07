/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins.network;

import static java.net.StandardSocketOptions.SO_REUSEADDR;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.NetworkChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;

import de.hpi.swa.trufflesqueak.util.LogUtils;

final class SqueakTCPSocket extends SqueakSocket {
    // clientChannel acts as a configuration prototype before bind/connect.
    // For servers, this is destroyed during listenOn(), and later reused to hold the pending accepted connection.
    private SocketChannel clientChannel;
    private ServerSocketChannel serverChannel;

    private InetSocketAddress boundAddress;
    private boolean remoteClosed = false;

    private int peekedByte = -1; // -1 indicates no byte is currently peeked

    protected SqueakTCPSocket(final SqueakSocketContext context, final long netType, final long statusSema, final long readSema, final long writeSema) throws IOException {
        super(context, netType, statusSema, readSema, writeSema);
        clientChannel = configure(SocketChannel.open());
    }

    // Used internally when accepting new connections
    private SqueakTCPSocket(final SqueakSocketContext context, final long netType, final SocketChannel clientChannel, final long statusSema, final long readSema, final long writeSema)
                    throws IOException {
        super(context, netType, statusSema, readSema, writeSema);
        this.clientChannel = configure(clientChannel);
        context.register(this.clientChannel, SelectionKey.OP_READ | SelectionKey.OP_WRITE, this);
    }

    private static <T extends SelectableChannel & NetworkChannel> T configure(final T channel) throws IOException {
        channel.configureBlocking(false);
        channel.setOption(SO_REUSEADDR, true);
        return channel;
    }

    @Override
    protected NetworkChannel asNetworkChannel() {
        return listening ? serverChannel : clientChannel;
    }

    @Override
    protected SelectableChannel asSelectableChannel() {
        return listening ? serverChannel : clientChannel;
    }

    private InetSocketAddress getLocalSocketAddress() throws IOException {
        maybeCompleteConnection();
        final NetworkChannel channel = asNetworkChannel();
        return coerceToNetType(channel == null ? null : castAddress(channel.getLocalAddress()));
    }

    @Override
    protected byte[] getLocalAddress() throws IOException {
        final InetSocketAddress address = getLocalSocketAddress();
        if (address != null) {
            return address.getAddress().getAddress();
        }
        // Fallback for unbound sockets
        return listening ? getResolver().getLoopbackAddress() : getResolver().getAnyLocalAddress();
    }

    @Override
    protected long getLocalPort() throws IOException {
        final InetSocketAddress address = getLocalSocketAddress();
        return address == null ? 0L : address.getPort();
    }

    @Override
    protected byte[] getRemoteAddress() throws IOException {
        maybeCompleteConnection();
        if (listening) {
            return getResolver().getAnyLocalAddress();
        }
        if (clientIsConnected()) {
            return castAddress(clientChannel.getRemoteAddress()).getAddress().getAddress();
        }
        return getResolver().getAnyLocalAddress();
    }

    @Override
    protected long getRemotePort() throws IOException {
        maybeCompleteConnection();
        if (clientIsConnected()) {
            return castAddress(clientChannel.getRemoteAddress()).getPort();
        }
        return 0L;
    }

    private boolean clientIsConnected() {
        return clientChannel != null && clientChannel.isConnected();
    }

    @Override
    protected Status getStatus() throws IOException {
        final Status status = listening ? serverStatus() : clientStatus();
        LogUtils.SOCKET.finer(() -> this + " " + status);
        return status;
    }

    private Status serverStatus() throws IOException {
        if (clientChannel != null) {
            return Status.Connected;
        }

        if (serverChannel == null || !serverChannel.isOpen() || !serverChannel.socket().isBound()) {
            return Status.Unconnected;
        }

        clientChannel = serverChannel.accept();
        if (clientChannel != null) {
            configure(clientChannel);
            context.register(clientChannel, SelectionKey.OP_READ | SelectionKey.OP_WRITE, this);
            return Status.Connected;
        }

        context.resumeInterest(serverChannel, SelectionKey.OP_ACCEPT);
        return Status.WaitingForConnection;
    }

    private Status clientStatus() {
        if (clientChannel == null || !clientChannel.isOpen()) {
            return Status.Unconnected;
        }

        maybeCompleteConnection();

        if (clientChannel.isConnectionPending()) {
            context.resumeInterest(clientChannel, SelectionKey.OP_CONNECT);
            return Status.WaitingForConnection;
        }

        final Socket socket = clientChannel.socket();

        if (!socket.isConnected()) {
            return Status.Unconnected;
        }

        if (remoteClosed || socket.isInputShutdown()) {
            return Status.OtherEndClosed;
        }

        if (socket.isOutputShutdown()) {
            return Status.ThisEndClosed;
        }

        return Status.Connected;
    }

    private void maybeCompleteConnection() {
        if (clientChannel != null && clientChannel.isConnectionPending()) {
            try {
                if (clientChannel.finishConnect()) {
                    context.resumeInterest(clientChannel, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                }
            } catch (IOException e) {
                // Any IOException here means the OS definitively aborted the handshake.
                socketError = POSIX_ECONNREFUSED;
                try {
                    clientChannel.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    @Override
    protected void bindTo(final String address, final int port) throws IOException {
        boundAddress = socketAddressFor(address, port);
    }

    @Override
    protected void connectTo(final String address, final long port) throws IOException {
        if (boundAddress != null && clientChannel != null) {
            clientChannel.bind(boundAddress);
        }
        context.register(clientChannel, SelectionKey.OP_CONNECT | SelectionKey.OP_WRITE | SelectionKey.OP_READ, this);
        clientChannel.connect(new InetSocketAddress(address, (int) port));
    }

    @Override
    protected void listenBacklog(final long backlogSize) throws IOException {
        listening = true;
        serverChannel = configure(ServerSocketChannel.open());

        if (clientChannel != null) {
            for (final SocketOption<?> option : clientChannel.supportedOptions()) {
                if (serverChannel.supportedOptions().contains(option)) {
                    transferOption(clientChannel, serverChannel, option);
                }
            }
            clientChannel.close();
            clientChannel = null;
        }

        if (boundAddress == null) {
            boundAddress = socketAddressFor(null, 0);
        }

        serverChannel.bind(boundAddress, (int) backlogSize);
        context.register(serverChannel, SelectionKey.OP_ACCEPT, this);
    }

    @Override
    protected void listenOn(final String address, final long port, final long backlogSize) throws IOException {
        bindTo(address, (int) port);
        listenBacklog(backlogSize);
    }

    private static <T> void transferOption(final NetworkChannel from, final NetworkChannel to, final SocketOption<T> opt) {
        try {
            to.setOption(opt, from.getOption(opt));
        } catch (Exception e) {
            // Safely ignore if a specific option cannot be read or transferred
        }
    }

    @Override
    protected SqueakSocket accept(final long acceptStatusSema, final long acceptReadSema, final long acceptWriteSema) throws IOException {
        if (listening) {
            SocketChannel accepted = clientChannel;
            clientChannel = null;

            if (accepted == null && serverChannel != null) {
                accepted = serverChannel.accept();
            }

            if (accepted != null) {
                final SqueakSocket created = new SqueakTCPSocket(context, netType, accepted, acceptStatusSema, acceptReadSema, acceptWriteSema);
                if (serverChannel != null && serverChannel.isOpen()) {
                    context.resumeInterest(serverChannel, SelectionKey.OP_ACCEPT);
                }
                return created;
            }
        }
        return null;
    }

    @Override
    protected boolean isOutputShutdown() {
        return clientChannel == null || remoteClosed || clientChannel.socket().isOutputShutdown();
    }

    @Override
    protected long sendDataTo(final ByteBuffer data) throws IOException {
        maybeCompleteConnection();
        if (!clientIsConnected()) {
            throw new IOException("Client not connected");
        }

        try {
            return clientChannel.write(data);
        } catch (final IOException e) {
            remoteClosed = true;
            try {
                clientChannel.shutdownOutput();
            } catch (final IOException ignored) {
            }
            throw e;
        }
    }

    @Override
    protected boolean isInputShutdown() {
        return clientChannel == null || remoteClosed || clientChannel.socket().isInputShutdown();
    }

    @Override
    protected boolean isDataAvailable() {
        if (peekedByte != -1) {
            return true;
        }
        if (remoteClosed || clientChannel == null || !clientChannel.isOpen()) {
            return false;
        }
        maybeCompleteConnection();
        if (!clientIsConnected()) {
            return false;
        }

        if (dataAvailable) {
            final ByteBuffer buf = ByteBuffer.allocate(1);
            final int read;
            try {
                read = clientChannel.read(buf);
            } catch (final IOException e) {
                remoteClosed = true;
                dataAvailable = false;
                try {
                    clientChannel.shutdownInput();
                } catch (final IOException ignored) {
                    // Channel is already broken; ignore shutdown failures.
                }
                LogUtils.SOCKET.log(Level.FINE, "Checking for available data failed", e);
                return false;
            }

            if (read > 0) {
                peekedByte = buf.get(0) & 0xFF; // Store the unsigned byte
                return true;
            } else if (read == -1) { // EOF detected
                remoteClosed = true;
                dataAvailable = false;
                try {
                    clientChannel.shutdownInput();
                } catch (final IOException ignored) {
                    // Channel is already broken; ignore shutdown failures.
                }
                return false;
            } else { // read == 0 (Spurious wakeup)
                dataAvailable = false;
                if (!isInputShutdown()) {
                    context.resumeInterest(clientChannel, SelectionKey.OP_READ);
                }
                return false;
            }
        }
        return false;
    }

    @Override
    protected long receiveDataFrom(final ByteBuffer data) throws IOException {
        maybeCompleteConnection();
        if (!clientIsConnected()) {
            return 0;
        }

        long totalRead = 0;

        // Drain the peeked byte first if we have one
        if (peekedByte != -1 && data.hasRemaining()) {
            data.put((byte) peekedByte);
            peekedByte = -1;
            totalRead++;
        }

        // Read the rest directly from the channel
        if (data.hasRemaining() && !remoteClosed) {
            try {
                final int read = clientChannel.read(data);
                if (read == -1) {
                    remoteClosed = true;
                    try {
                        clientChannel.shutdownInput();
                    } catch (final IOException ignored) {
                        // Channel is already broken; ignore shutdown failures.
                    }
                } else if (read > 0) {
                    totalRead += read;
                }
            } catch (final IOException e) {
                remoteClosed = true;
                try {
                    clientChannel.shutdownInput();
                } catch (final IOException ignored) {
                    // Channel is already broken; ignore shutdown failures.
                }
                throw e;
            }
        }

        return totalRead;
    }

    @Override
    protected void close() throws IOException {
        peekedByte = -1; // Reset peek state on close
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (clientChannel != null) {
            clientChannel.close();
        }
    }
}
