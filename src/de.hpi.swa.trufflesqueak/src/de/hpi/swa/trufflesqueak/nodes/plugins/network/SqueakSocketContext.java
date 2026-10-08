/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.concurrent.locks.LockSupport;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.nodes.interrupts.CheckForInterruptsState;
import de.hpi.swa.trufflesqueak.util.LogUtils;

public final class SqueakSocketContext {

    // Squeak internal lookup flags
    public static final int SQ_SOCKET_NUMERIC = (1 << 0);
    public static final int SQ_SOCKET_PASSIVE = (1 << 1);

    // Squeak family constants
    public static final int SQ_FAMILY_UNSPEC = 0;
    public static final int SQ_FAMILY_INET4 = 2;
    public static final int SQ_FAMILY_INET6 = 3;

    // Squeak socket type constants
    public static final int SQ_TYPE_STREAM = 1;
    public static final int SQ_TYPE_DGRAM = 2;

    // Squeak protocol constants
    public static final int SQ_PROTOCOL_TCP = 1;
    public static final int SQ_PROTOCOL_UDP = 2;

    private final Selector selector;
    private final int sessionID;
    private final Resolver resolver;

    private final byte[] localHostName;
    private final boolean hasSocketAccess;

    public SqueakSocketContext() {
        try {
            selector = Selector.open();
        } catch (final IOException e) {
            throw SqueakException.create("Failed to open NIO selector", e);
        }

        boolean socketAccess = false;
        String hostName = "unknown";
        try {
            hostName = InetAddress.getLocalHost().getHostName();
            socketAccess = true;
        } catch (final SecurityException | UnknownHostException e) {
            LogUtils.MAIN.warning(e.toString());
        }
        localHostName = hostName.getBytes();
        hasSocketAccess = socketAccess;

        // Generate a unique session ID. OSVM treats 0 as uninitialized.
        final int id = (int) System.nanoTime();
        sessionID = id == 0 ? 1 : id;

        resolver = new Resolver(this);
    }

    public int getSessionID() {
        return sessionID;
    }

    public Resolver getResolver() {
        return resolver;
    }

    public byte[] getLocalHostName() {
        return localHostName;
    }

    public boolean hasSocketAccess() {
        return hasSocketAccess;
    }

    public void pollEvents(final long parkNanos, final CheckForInterruptsState interrupts) {
        final long ms = parkNanos / 1_000_000;
        try {
            if (ms > 0) {
                selector.select(ms);
            } else {
                selector.selectNow();
                LockSupport.parkNanos(parkNanos);
            }

            for (final SelectionKey key : selector.selectedKeys()) {
                try {
                    if (key.isValid() && key.attachment() instanceof SqueakSocket socket) {
                        socket.handleReadyOps(key, interrupts);
                    }
                } catch (final CancelledKeyException e) {
                    // Ignore channels closed concurrently during iteration
                }
            }
            selector.selectedKeys().clear();
        } catch (final IOException | CancelledKeyException e) {
            // Safely ignore dropped connections or closed channels
        }
    }

    public void register(final SelectableChannel channel, final int ops, final SqueakSocket socket) throws ClosedChannelException {
        if (channel == null) {
            return;
        }
        selector.wakeup();
        channel.register(selector, ops, socket);
    }

    public void resumeInterest(final SelectableChannel channel, final int ops) {
        if (channel == null) {
            return;
        }
        try {
            final SelectionKey key = channel.keyFor(selector);
            if (key != null && key.isValid()) {
                key.interestOps(key.interestOps() | (ops & channel.validOps()));
                selector.wakeup();
            }
        } catch (final CancelledKeyException e) {
            // Ignored, the channel was concurrently closed by Squeak
        }
    }
}
