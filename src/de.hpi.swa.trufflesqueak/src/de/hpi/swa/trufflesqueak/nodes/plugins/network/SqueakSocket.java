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
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.NetworkChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.util.LogUtils;

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

    protected final Selector selector;

    protected boolean listening;

    protected SqueakSocket() throws IOException {
        selector = Selector.open();
        listening = false;
    }

    protected abstract NetworkChannel asNetworkChannel();

    protected abstract byte[] getLocalAddress() throws IOException;

    protected abstract long getLocalPort() throws IOException;

    protected abstract byte[] getRemoteAddress() throws IOException;

    protected abstract long getRemotePort() throws IOException;

    protected abstract Status getStatus() throws IOException;

    protected abstract void connectTo(String address, long port) throws IOException;

    protected abstract void listenOn(long port, long backlogSize) throws IOException;

    protected abstract SqueakSocket accept() throws IOException;

    protected abstract boolean isSendDone() throws IOException;

    protected final long sendData(final byte[] data, final int start, final int count) throws IOException {
        final ByteBuffer buffer = ByteBuffer.wrap(data, start, count);
        selector.selectNow();
        final Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
        while (keys.hasNext()) {
            final SelectionKey key = keys.next();
            if (key.isWritable()) {
                final long written = sendDataTo(buffer, key);
                LogUtils.SOCKET.finer(() -> this + " written: " + written);
                keys.remove();
                return written;
            }
        }

        throw new IOException("No writable key found");
    }

    protected abstract long sendDataTo(ByteBuffer data, SelectionKey key) throws IOException;

    protected final boolean isDataAvailable() throws IOException {
        selector.selectNow();
        final Set<SelectionKey> keys = selector.selectedKeys();
        for (final SelectionKey key : keys) {
            if (key.isReadable()) {
                LogUtils.SOCKET.finer(() -> this + " data available");
                return true;
            }
        }

        LogUtils.SOCKET.finer(() -> this + " no data available");
        return false;
    }

    protected final long receiveData(final byte[] data, final int start, final int count) throws IOException {
        final ByteBuffer buffer = ByteBuffer.wrap(data, start, count);
        selector.selectNow();
        final Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
        while (keys.hasNext()) {
            final SelectionKey key = keys.next();

            if (key.isReadable()) {
                final long received = receiveDataFrom(key, buffer);
                LogUtils.SOCKET.finer(() -> this + " received: " + received);
                keys.remove();
                return received;
            }
        }
        return 0;
    }

    protected abstract long receiveDataFrom(SelectionKey key, ByteBuffer data) throws IOException;

    protected final boolean supportsOption(final String name) {
        return asNetworkChannel().supportedOptions().stream().anyMatch(o -> o.name().equals(name));
    }

    protected final String getOption(final String name) throws IOException {
        final SocketOption<?> option = socketOptionFromString(name);
        final Object value = asNetworkChannel().getOption(option);
        if (value instanceof final Boolean b) {
            return b ? "1" : "0";
        }
        return String.valueOf(value);
    }

    protected final void setOption(final String name, final String value) throws IOException {
        final Boolean enabled = "1".equals(value);
        final SocketOption<?> option = socketOptionFromString(name);
        sneakySetOption(option, enabled);
    }

    private SocketOption<?> socketOptionFromString(final String name) {
        return asNetworkChannel().supportedOptions().stream().filter(o -> o.name().equals(name)).findFirst().orElseThrow(() -> new UnsupportedOperationException("Unknown socket option: " + name));
    }

    @SuppressWarnings("unchecked")
    private <T> void sneakySetOption(final SocketOption<T> opt, final Object value) throws IOException {
        asNetworkChannel().setOption(opt, (T) value);
    }

    protected void close() throws IOException {
        selector.close();
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
}
