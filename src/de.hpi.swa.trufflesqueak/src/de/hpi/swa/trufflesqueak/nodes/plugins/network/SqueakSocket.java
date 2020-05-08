/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.plugins.network;

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

import com.oracle.truffle.api.TruffleLogger;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.shared.SqueakLanguageConfig;

public abstract class SqueakSocket {

    enum Status {
        InvalidSocket(-1),
        Unconnected(0),
        WaitingForConnection(1),
        Connected(2),
        OtherEndClosed(3),
        ThisEndClosed(4);

        private long id;

        Status(final long id) {
            this.id = id;
        }

        long id() {
            return id;
        }
    }

    enum Type {
        TCP(0),
        UDP(1);

        private long id;

        Type(final long id) {
            this.id = id;
        }

        static Type fromId(final long id) {
            for (final Type type : values()) {
                if (type.id == id) {
                    return type;
                }
            }
            throw SqueakException.create("Unknown SocketType: " + id);
        }
    }

    private static final TruffleLogger LOG = TruffleLogger.getLogger(SqueakLanguageConfig.ID, SqueakSocket.class);

    protected final long handle;
    protected final Selector selector;

    protected boolean listening;

    protected SqueakSocket() throws IOException {
        handle = System.identityHashCode(this);
        selector = Selector.open();
        listening = false;
    }

    protected final long handle() {
        return handle;
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

    protected final long sendData(final ByteBuffer buffer) throws IOException {
        selector.selectNow();
        final Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
        while (keys.hasNext()) {
            final SelectionKey key = keys.next();
            if (key.isWritable()) {
                final long written = sendDataTo(buffer, key);
                LOG.finer(() -> handle + " written: " + written);
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
                LOG.finer(() -> handle + " data available");
                return true;
            }
        }

        LOG.finer(() -> handle + " no data available");
        return false;
    }

    protected final long receiveData(final ByteBuffer buffer) throws IOException {
        selector.selectNow();
        final Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
        while (keys.hasNext()) {
            final SelectionKey key = keys.next();

            if (key.isReadable()) {
                final long received = receiveDataFrom(key, buffer);
                LOG.finer(() -> handle + " received: " + received);
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
        if (value instanceof Boolean) {
            return (boolean) value ? "1" : "0";
        }
        return String.valueOf(value);
    }

    protected final void setOption(final String name, final String value) throws IOException {
        final Boolean enabled = "1".equals(value);
        final SocketOption<?> option = socketOptionFromString(name);
        sneakySetOption(option, enabled);
    }

    protected final SocketOption<?> socketOptionFromString(final String name) {
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

        if (address instanceof InetSocketAddress) {
            return (InetSocketAddress) address;
        }
        throw SqueakException.create("Unknown address type");
    }

    protected static SqueakSocket create(final SqueakSocket.Type socketType) throws IOException {
        switch (socketType) {
            case TCP:
                return new SqueakTCPSocket();
            case UDP:
                return new SqueakUDPSocket();
            default:
                throw SqueakException.create("Unknown SocketType");
        }
    }
}
