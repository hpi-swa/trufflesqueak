/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
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

final class SqueakUDPSocket extends SqueakSocket {

    private final DatagramChannel channel;

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
    protected byte[] getLocalAddress() throws IOException {
        if (listening) {
            return Resolver.getLoopbackAddress();
        }

        return castAddress(channel.getLocalAddress()).getAddress().getAddress();
    }

    @Override
    protected long getLocalPort() throws IOException {
        final SocketAddress address = channel.getLocalAddress();
        return castAddress(address).getPort();
    }

    @Override
    protected byte[] getRemoteAddress() throws IOException {
        final SocketAddress address = channel.getRemoteAddress();
        if (channel.isConnected()) {
            return castAddress(address).getAddress().getAddress();
        }
        return Resolver.getAnyLocalAddress();
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
        if (listening) {
            return Status.WaitingForConnection;
        }

        if (channel.isConnected()) {
            return Status.Connected;
        }

        return Status.Unconnected;
    }

    @Override
    protected void connectTo(final String address, final long port) throws IOException {
        channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        channel.connect(new InetSocketAddress(address, (int) port));
    }

    @Override
    protected void listenOn(final long port, final long backlogSize) throws IOException {
        listening = true;
        channel.bind(new InetSocketAddress((int) port));
        channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    }

    @Override
    protected SqueakSocket accept() {
        throw new UnsupportedOperationException("accept() on UDP socket");
    }

    @Override
    protected boolean isSendDone() {
        return true;
    }

    @Override
    protected long sendDataTo(final ByteBuffer data, final SelectionKey key) throws IOException {
        final DatagramChannel to = (DatagramChannel) key.channel();
        return to.send(data, to.getRemoteAddress());
    }

    @Override
    protected long receiveDataFrom(final SelectionKey key, final ByteBuffer data) throws IOException {
        final DatagramChannel from = (DatagramChannel) key.channel();
        from.receive(data);
        return data.position();
    }

    @Override
    protected void close() throws IOException {
        super.close();
        channel.close();
    }
}
