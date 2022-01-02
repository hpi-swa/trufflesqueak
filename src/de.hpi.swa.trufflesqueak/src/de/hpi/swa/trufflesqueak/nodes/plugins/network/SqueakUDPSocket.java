/*
 * Copyright (c) 2017-2022 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2022 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.NetworkChannel;
import java.nio.channels.SelectionKey;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

final class SqueakUDPSocket extends SqueakSocket {

    private final DatagramChannel channel;

    @TruffleBoundary
    SqueakUDPSocket() throws IOException {
        super();
        channel = DatagramChannel.open();
        channel.configureBlocking(false);
    }

    @Override
    protected NetworkChannel asNetworkChannel() {
        return channel;
    }

    @Override
    @TruffleBoundary
    protected byte[] getLocalAddress() throws IOException {
        if (listening) {
            return Resolver.getLoopbackAddress();
        }

        return castAddress(channel.getLocalAddress()).getAddress().getAddress();
    }

    @Override
    @TruffleBoundary
    protected long getLocalPort() throws IOException {
        final SocketAddress address = channel.getLocalAddress();
        return castAddress(address).getPort();
    }

    @Override
    @TruffleBoundary
    protected byte[] getRemoteAddress() throws IOException {
        final SocketAddress address = channel.getRemoteAddress();
        if (channel.isConnected()) {
            return castAddress(address).getAddress().getAddress();
        }
        return Resolver.getAnyLocalAddress();
    }

    @Override
    @TruffleBoundary
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
    @TruffleBoundary
    protected Status getStatus() {
        if (listening) {
            return Status.WaitingForConnection;
        }

        if (channel.isConnected()) {
            return Status.Connected;
        }

        return Status.Unconnected;
    }

    @Override
    @TruffleBoundary
    protected void connectTo(final String address, final long port) throws IOException {
        channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        channel.connect(new InetSocketAddress(address, (int) port));
    }

    @Override
    @TruffleBoundary
    protected void listenOn(final long port, final long backlogSize) throws IOException {
        listening = true;
        channel.bind(new InetSocketAddress((int) port));
        channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    }

    @Override
    @TruffleBoundary
    protected SqueakSocket accept() {
        throw new UnsupportedOperationException("accept() on UDP socket");
    }

    @Override
    protected boolean isSendDone() {
        return true;
    }

    @Override
    @TruffleBoundary
    protected long sendDataTo(final ByteBuffer data, final SelectionKey key) throws IOException {
        final DatagramChannel to = (DatagramChannel) key.channel();
        return to.send(data, to.getRemoteAddress());
    }

    @Override
    @TruffleBoundary
    protected long receiveDataFrom(final SelectionKey key, final ByteBuffer data) throws IOException {
        final DatagramChannel from = (DatagramChannel) key.channel();
        from.receive(data);
        return data.position();
    }

    @Override
    @TruffleBoundary
    protected void close() throws IOException {
        super.close();
        channel.close();
    }
}
