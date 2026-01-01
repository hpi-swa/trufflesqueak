/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.NetworkChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import de.hpi.swa.trufflesqueak.util.LogUtils;

final class SqueakTCPSocket extends SqueakSocket {
    private SocketChannel clientChannel;
    private ServerSocketChannel serverChannel;

    protected SqueakTCPSocket() throws IOException {
        super();
    }

    private SqueakTCPSocket(final SocketChannel clientChannel) throws IOException {
        super();
        this.clientChannel = clientChannel;
        this.clientChannel.configureBlocking(false);
        this.clientChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    }

    @Override
    protected NetworkChannel asNetworkChannel() {
        return listening ? serverChannel : clientChannel;
    }

    @Override
    protected byte[] getLocalAddress() throws IOException {
        if (listening) {
            return Resolver.getLoopbackAddress();
        }

        return castAddress(clientChannel.getLocalAddress()).getAddress().getAddress();
    }

    @Override
    protected long getLocalPort() throws IOException {
        final SocketAddress address = (listening ? serverChannel : clientChannel).getLocalAddress();
        return castAddress(address).getPort();
    }

    @Override
    protected byte[] getRemoteAddress() throws IOException {
        return listening ? getServerRemoteAddress() : getClientRemoteAddress();
    }

    private static byte[] getServerRemoteAddress() {
        return Resolver.getAnyLocalAddress();
    }

    private byte[] getClientRemoteAddress() throws IOException {
        if (clientChannel == null || !clientChannel.isConnected()) {
            return Resolver.getAnyLocalAddress();
        }

        final SocketAddress address = clientChannel.getRemoteAddress();
        return castAddress(address).getAddress().getAddress();
    }

    @Override
    protected long getRemotePort() throws IOException {
        if (clientChannel != null && clientChannel.isConnected()) {
            return castAddress(clientChannel.getRemoteAddress()).getPort();
        }
        return 0L;
    }

    @Override
    protected Status getStatus() throws IOException {
        if (selector.isOpen()) {
            selector.selectNow();
        }

        final Status status = listening ? serverStatus() : clientStatus();
        LogUtils.SOCKET.finer(() -> this + " " + status);
        return status;
    }

    private Status serverStatus() throws IOException {
        if (clientChannel != null) {
            return Status.Connected;
        }

        final Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
        while (keys.hasNext()) {
            if (keys.next().isAcceptable()) {
                clientChannel = serverChannel.accept();
                clientChannel.configureBlocking(false);
                clientChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                keys.remove();
                return Status.Connected;
            }
        }

        return Status.WaitingForConnection;
    }

    private Status clientStatus() throws IOException {
        if (clientChannel == null) {
            return Status.Unconnected;
        }

        maybeCompleteConnection();
        final Socket socket = clientChannel.socket();

        if (socket.isInputShutdown()) {
            return Status.OtherEndClosed;
        }

        if (socket.isOutputShutdown()) {
            return Status.ThisEndClosed;
        }

        if (!socket.isConnected()) {
            return Status.Unconnected;
        }

        if (socket.isClosed()) {
            return Status.ThisEndClosed;
        }

        return Status.Connected;
    }

    private void maybeCompleteConnection() throws IOException {
        while (clientChannel.isConnectionPending()) {
            clientChannel.finishConnect();
        }
    }

    @Override
    protected void connectTo(final String address, final long port) throws IOException {
        clientChannel = SocketChannel.open();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_WRITE | SelectionKey.OP_READ);
        clientChannel.connect(new InetSocketAddress(address, (int) port));
    }

    @Override
    protected void listenOn(final long port, final long backlogSize) throws IOException {
        listening = true;
        serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress((int) port), (int) backlogSize);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    @Override
    protected SqueakSocket accept() throws IOException {
        if (listening && clientChannel != null) {
            clientChannel.keyFor(selector).cancel();
            final SqueakSocket created = new SqueakTCPSocket(clientChannel);
            clientChannel = null;
            return created;
        }

        return null;
    }

    @Override
    protected boolean isSendDone() throws IOException {
        selector.selectNow();
        return selector.selectedKeys().stream().anyMatch(SelectionKey::isWritable);
    }

    @Override
    protected long sendDataTo(final ByteBuffer data, final SelectionKey key) throws IOException {
        final SocketChannel channel = (SocketChannel) key.channel();
        if (!channel.isConnected()) {
            throw new IOException("Client not connected");
        }
        return channel.write(data);
    }

    @Override
    protected long receiveDataFrom(final SelectionKey key, final ByteBuffer data) throws IOException {
        final SocketChannel channel = (SocketChannel) key.channel();
        final long read = channel.read(data);

        if (read == -1) {
            channel.shutdownInput();
            key.cancel();
            return 0;
        }

        return read;
    }

    @Override
    protected void close() throws IOException {
        super.close();
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (clientChannel != null) {
            clientChannel.close();
        }
    }
}
