/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins.network;

import static java.net.StandardSocketOptions.SO_BROADCAST;
import static java.net.StandardSocketOptions.SO_REUSEADDR;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.NetworkChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

final class SqueakUDPSocket extends SqueakSocket {

    private final DatagramChannel channel;
    private InetSocketAddress remoteAddress;

    SqueakUDPSocket(final SqueakSocketContext context, final long netType, final long statusSema, final long readSema, final long writeSema) throws IOException {
        super(context, netType, statusSema, readSema, writeSema);
        channel = specifiesProtocolFamily() ? DatagramChannel.open(getProtocolFamily()) : DatagramChannel.open();
        channel.configureBlocking(false);
        try {
            channel.setOption(SO_REUSEADDR, true);
            channel.setOption(SO_BROADCAST, true);
        } catch (IOException ignored) {
        }
    }

    @Override
    protected NetworkChannel asNetworkChannel() {
        return channel;
    }

    @Override
    protected SelectableChannel asSelectableChannel() {
        return channel;
    }

    @Override
    protected InetSocketAddress getLocalSocketAddress() throws IOException {
        final InetSocketAddress address = castAddress(channel.getLocalAddress());
        if (address != null) {
            return address;
        }
        // Fallback for unbound sockets
        return listening ? loopbackAddressFor(0) : socketAddressFor(null, 0);
    }

    /** Return the local port for this socket, or zero if no port has yet been assigned. */
    @Override
    protected long getLocalPort() throws IOException {
        final InetSocketAddress address = castAddress(channel.getLocalAddress());
        return address == null ? 0L : address.getPort();
    }

    @Override
    protected InetSocketAddress getRemoteSocketAddress() throws IOException {
        if (listening || !channel.isConnected()) {
            return socketAddressFor(null, 0);
        }
        return castAddress(channel.getRemoteAddress());
    }

    @Override
    protected long getRemotePort() throws IOException {
        if (listening) {
            return 0L;
        }

        if (channel.isConnected()) {
            return castAddress(channel.getRemoteAddress()).getPort();
        }

        return 0L;
    }

    @Override
    protected Status getStatus() {
        return Status.Connected;
    }

    @Override
    protected void connectTo(final String address, final long port) throws IOException {
        context.register(channel, SelectionKey.OP_READ | SelectionKey.OP_WRITE, this);
        remoteAddress = new InetSocketAddress(address, (int) port);
        try {
            channel.connect(remoteAddress);
        } catch (Exception e) {
            // Silently ignore connection failures (expected for broadcast addresses).
            // sendDataTo will fall back to using the connectionless send() method.
        }
    }

    @Override
    protected void bindTo(final String address, final int port) throws IOException {
        if (!channel.socket().isBound()) {
            channel.bind(socketAddressFor(address, port));
        }
    }

    @Override
    protected void listenBacklog(final long backlogSize) throws IOException {
        listening = true;
        context.register(channel, SelectionKey.OP_READ | SelectionKey.OP_WRITE, this);
    }

    @Override
    protected void listenOn(final String address, final long port, final long backlogSize) throws IOException {
        bindTo(address, (int) port);
        listenBacklog(backlogSize);
    }

    @Override
    protected SqueakSocket accept(final long acceptStatusSema, final long acceptReadSema, final long acceptWriteSema) {
        throw new UnsupportedOperationException("accept() on UDP socket");
    }

    @Override
    protected boolean isInputShutdown() {
        return false;
    }

    @Override
    protected boolean isOutputShutdown() {
        return false;
    }

    @Override
    protected long sendDataTo(final ByteBuffer data) throws IOException {
        if (channel.isConnected()) {
            return channel.write(data);
        }
        if (remoteAddress != null) {
            return channel.send(data, remoteAddress);
        }
        return 0;
    }

    @Override
    protected long receiveDataFrom(final ByteBuffer data) {
        final int initialPosition = data.position();

        try {
            final SocketAddress address = channel.receive(data);
            if (address == null) {
                return 0; // Spurious wakeup, no bytes read
            }
        } catch (IOException e) {
            // Absorb ICMP Port Unreachable and other connectionless errors.
            // Returning 0 clears the OS readable flag and breaks the Squeak spin-loop.
            return 0;
        }

        return data.position() - initialPosition;
    }

    @Override
    protected void close() throws IOException {
        channel.close();
    }
}
