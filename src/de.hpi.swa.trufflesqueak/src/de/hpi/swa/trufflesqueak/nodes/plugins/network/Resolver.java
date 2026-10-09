/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins.network;

import static de.hpi.swa.trufflesqueak.nodes.plugins.network.SqueakSocketContext.SQ_FAMILY_INET4;
import static de.hpi.swa.trufflesqueak.nodes.plugins.network.SqueakSocketContext.SQ_FAMILY_INET6;
import static de.hpi.swa.trufflesqueak.nodes.plugins.network.SqueakSocketContext.SQ_FAMILY_UNSPEC;
import static de.hpi.swa.trufflesqueak.nodes.plugins.network.SqueakSocketContext.SQ_PROTOCOL_TCP;
import static de.hpi.swa.trufflesqueak.nodes.plugins.network.SqueakSocketContext.SQ_PROTOCOL_UDP;
import static de.hpi.swa.trufflesqueak.nodes.plugins.network.SqueakSocketContext.SQ_SOCKET_NUMERIC;
import static de.hpi.swa.trufflesqueak.nodes.plugins.network.SqueakSocketContext.SQ_SOCKET_PASSIVE;
import static de.hpi.swa.trufflesqueak.nodes.plugins.network.SqueakSocketContext.SQ_TYPE_DGRAM;
import static de.hpi.swa.trufflesqueak.nodes.plugins.network.SqueakSocketContext.SQ_TYPE_STREAM;

import java.io.UncheckedIOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import de.hpi.swa.trufflesqueak.nodes.plugins.network.SqueakOpaqueSocketAddress.AddressInfo;
import de.hpi.swa.trufflesqueak.util.LogUtils;

public final class Resolver {

    enum Status {
        Uninitialized(0),
        Ready(1),
        Busy(2),
        Error(3);

        private final long id;

        Status(final long id) {
            this.id = id;
        }

        long id() {
            return id;
        }
    }

    private final SqueakSocketContext context;

    private boolean initialized;
    private volatile AsyncSession activeLegacySession;
    private Runnable statusChangeCallback;

    private List<AddressInfo> currentAddressInfoList;
    private String lastHostNameInfo = "";
    private String lastServiceInfo = "";

    private final InetAddress[] wildcardAddresses;
    private final InetAddress[] loopbackAddresses;

    Resolver(final SqueakSocketContext context) {
        this.context = context;

        this.wildcardAddresses = new InetAddress[]{
                        new InetSocketAddress("0.0.0.0", 0).getAddress(),
                        new InetSocketAddress("::", 0).getAddress()
        };

        this.loopbackAddresses = new InetAddress[]{
                        new InetSocketAddress("127.0.0.1", 0).getAddress(),
                        new InetSocketAddress("::1", 0).getAddress()
        };
    }

    // Wrapper to hold the asynchronous job
    private record AsyncSession(CompletableFuture<List<AddressInfo>> future) {
        Status getStatus() {
            if (!future.isDone()) {
                return Status.Busy;
            }
            if (future.isCompletedExceptionally()) {
                return Status.Error;
            }
            return Status.Ready;
        }
    }

    private AsyncSession executeAsyncLookup(final Supplier<List<AddressInfo>> lookupTask) {
        final CompletableFuture<List<AddressInfo>> future = new CompletableFuture<>();
        final AsyncSession session = new AsyncSession(future);

        future.whenComplete((@SuppressWarnings("unused") final List<AddressInfo> res, @SuppressWarnings("unused") final Throwable ex) -> triggerStatusChange());

        CompletableFuture.runAsync(() -> {
            try {
                future.complete(lookupTask.get());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });

        return session;
    }

    void initialize() {
        initialized = true;
        statusChangeCallback = null;
        currentAddressInfoList = null;
        lastHostNameInfo = "";
    }

    void setStatusChangeCallback(final Runnable callback) {
        statusChangeCallback = callback;
    }

    private void triggerStatusChange() {
        final Runnable cb = statusChangeCallback;
        if (cb != null) {
            cb.run();
        }
    }

    InetAddress getAnyLocalInetAddress(final long netType) {
        // wildcardAddresses[0] is IPv4, [1] is IPv6
        return netType == SQ_FAMILY_INET6 ? wildcardAddresses[1] : wildcardAddresses[0];
    }

    InetAddress getLoopbackInetAddress(final long netType) {
        // loopbackAddresses[0] is IPv4, [1] is IPv6
        return netType == SQ_FAMILY_INET6 ? loopbackAddresses[1] : loopbackAddresses[0];
    }

    @TruffleBoundary
    void setGlobalAddressInfoResult(final String hostName, final String serviceName, final int flags, final int family, final int type, final int protocol) {
        try {
            currentAddressInfoList = performGetAddressInfo(hostName, serviceName, flags, family, type, protocol);
        } catch (Exception e) {
            currentAddressInfoList = null;
        } finally {
            triggerStatusChange();
        }
    }

    @TruffleBoundary
    long getGlobalAddressInfoSize() {
        if (currentAddressInfoList != null && !currentAddressInfoList.isEmpty()) {
            return currentAddressInfoList.getFirst().getStructSize() + 8;
        }
        return -1L; // Squeak expects < 0 to signal end of list
    }

    @TruffleBoundary
    long getGlobalAddressInfoFamily() {
        if (currentAddressInfoList != null && !currentAddressInfoList.isEmpty()) {
            return currentAddressInfoList.getFirst().family();
        }
        return 0L;
    }

    @TruffleBoundary
    long getGlobalAddressInfoType() {
        if (currentAddressInfoList != null && !currentAddressInfoList.isEmpty()) {
            return currentAddressInfoList.getFirst().type();
        }
        return 0L;
    }

    @TruffleBoundary
    long getGlobalAddressInfoProtocol() {
        if (currentAddressInfoList != null && !currentAddressInfoList.isEmpty()) {
            return currentAddressInfoList.getFirst().protocol();
        }
        return 0L;
    }

    @TruffleBoundary
    byte[] getGlobalAddressInfoResultBytes() {
        if (currentAddressInfoList != null && !currentAddressInfoList.isEmpty()) {
            return currentAddressInfoList.getFirst().toSockaddrBytes(context.getSessionID());
        }
        return null;
    }

    @TruffleBoundary
    boolean advanceGlobalAddressInfo() {
        if (currentAddressInfoList != null && !currentAddressInfoList.isEmpty()) {
            currentAddressInfoList.removeFirst();
            return !currentAddressInfoList.isEmpty();
        }
        return false;
    }

    /* -------------------------------------------------------------------------
     * Asynchronous Parallel Address Lookup Engine
     * ------------------------------------------------------------------------- */

    private static boolean isIPLiteral(final String host) {
        if (host == null) {
            return false;
        }
        for (int i = 0; i < host.length(); i++) {
            final char c = host.charAt(i);
            if (!Character.isDigit(c) && c != '.' && c != ':' &&
                            !(c >= 'a' && c <= 'f') && !(c >= 'A' && c <= 'F')) {
                return false;
            }
        }
        return true;
    }

    private static int parseServiceNameToPort(final String serviceName, final int type, final int protocol) throws UnknownHostException {
        if (serviceName == null || serviceName.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(serviceName);
        } catch (NumberFormatException e) {
            int port = -1;

            final boolean isUdp = (protocol == SQ_PROTOCOL_UDP) || (type == SQ_TYPE_DGRAM);
            final boolean isTcp = (protocol == SQ_PROTOCOL_TCP) || (type == SQ_TYPE_STREAM);

            // Query exactly what Squeak asked for
            if (isUdp && !isTcp) {
                port = CachedServicesResolver.resolvePort(serviceName, "udp");
            } else if (isTcp && !isUdp) {
                port = CachedServicesResolver.resolvePort(serviceName, "tcp");
            } else {
                // If unspecified (0), try TCP first, then UDP
                port = CachedServicesResolver.resolvePort(serviceName, "tcp");
                if (port == -1) {
                    port = CachedServicesResolver.resolvePort(serviceName, "udp");
                }
            }

            if (port > 0) {
                return port;
            }

            LogUtils.SOCKET.warning("Unresolvable service name: " + serviceName);
            throw new UnknownHostException("Unknown service: " + serviceName);
        }
    }

    private InetAddress[] resolveAddresses(final String hostName, final int flags) throws UnknownHostException {
        // Resolve addresses based on hostName and SQ_SOCKET_PASSIVE flag
        final InetAddress[] addresses;
        if (hostName == null || hostName.isEmpty()) {
            if ((flags & SQ_SOCKET_PASSIVE) != 0) {
                // Wildcard (Any) addresses for binding a server
                addresses = wildcardAddresses;
            } else {
                // Loopback addresses for local connections
                addresses = loopbackAddresses;
            }
        } else if ("localhost".equals(hostName)) {
            // Loopback addresses for local connections
            addresses = loopbackAddresses;
        } else {
            // Standard DNS lookup
            addresses = InetAddress.getAllByName(hostName);
        }
        return addresses;
    }

    private List<AddressInfo> performGetAddressInfo(final String hostName, final String serviceName, final int flags, final int family, final int type, final int protocol)
                    throws UnknownHostException {
        // Strict Numeric Validation
        if ((flags & SQ_SOCKET_NUMERIC) != 0 && hostName != null && !hostName.isEmpty() && !isIPLiteral(hostName)) {
            throw new UnknownHostException("Host is not numeric");
        }

        // Parse service name to a port using the requested type and protocol
        final int port = parseServiceNameToPort(serviceName, type, protocol);

        // Resolve addresses based on hostName and SQ_SOCKET_PASSIVE flag
        final InetAddress[] addresses = resolveAddresses(hostName, flags);

        final List<AddressInfo> results = new ArrayList<>();
        for (final InetAddress address : addresses) {
            final int sqFamily = (address instanceof Inet4Address) ? SQ_FAMILY_INET4 : SQ_FAMILY_INET6;

            // Filter using SQUEAK constants
            if (family == SQ_FAMILY_UNSPEC || family == sqFamily) {
                final boolean addTcp = (type == 0 || type == SQ_TYPE_STREAM) && (protocol == 0 || protocol == SQ_PROTOCOL_TCP);
                final boolean addUdp = (type == 0 || type == SQ_TYPE_DGRAM) && (protocol == 0 || protocol == SQ_PROTOCOL_UDP);

                if (addTcp) {
                    results.add(new AddressInfo(address, port, sqFamily, SQ_TYPE_STREAM, SQ_PROTOCOL_TCP));
                }
                if (addUdp) {
                    results.add(new AddressInfo(address, port, sqFamily, SQ_TYPE_DGRAM, SQ_PROTOCOL_UDP));
                }

                // Fallback for RAW sockets or unknown exact matches
                if (!addTcp && !addUdp) {
                    results.add(new AddressInfo(address, port, sqFamily, type, protocol));
                }
            }
        }

        if (results.isEmpty()) {
            throw new UnknownHostException("No addresses found for family");
        }
        return results;
    }

    @TruffleBoundary
    private static AddressInfo peekAddressInfo(final AsyncSession session) {
        if (session != null && session.getStatus() == Status.Ready) {
            try {
                final List<AddressInfo> results = session.future().get();
                if (!results.isEmpty()) {
                    return results.getFirst();
                }
            } catch (Exception e) {
                // Safely ignore and fall through to return null
            }
        }
        return null;
    }

    /* -------------------------------------------------------------------------
     * getNameInfo (Reverse Lookups)
     * ------------------------------------------------------------------------- */

    @TruffleBoundary
    void getNameInfo(final byte[] opaqueArray, final int flags) {
        try {
            final InetSocketAddress addr = SqueakOpaqueSocketAddress.unpack(opaqueArray, context.getSessionID());
            final InetAddress ip = addr.getAddress();

            if ((flags & SQ_SOCKET_NUMERIC) != 0 || ip.isAnyLocalAddress()) {
                if (ip.isAnyLocalAddress() && ip instanceof Inet6Address) {
                    lastHostNameInfo = "::";
                } else {
                    lastHostNameInfo = ip.getHostAddress();
                }
            } else {
                lastHostNameInfo = ip.getHostName();
            }
            lastServiceInfo = String.valueOf(addr.getPort());
        } catch (Exception e) {
            lastHostNameInfo = "";
            lastServiceInfo = "";
        } finally {
            triggerStatusChange();
        }
    }

    @TruffleBoundary
    int getNameInfoHostSize() {
        return lastHostNameInfo.length();
    }

    @TruffleBoundary
    byte[] getNameInfoHostResult() {
        return lastHostNameInfo.getBytes();
    }

    @TruffleBoundary
    int getNameInfoServiceSize() {
        return lastServiceInfo.length();
    }

    @TruffleBoundary
    byte[] getNameInfoServiceResult() {
        return lastServiceInfo.getBytes();
    }

    /* -------------------------------------------------------------------------
     * Legacy Protocol Methods
     * ------------------------------------------------------------------------- */

    @TruffleBoundary
    byte[] lastHostNameLookupResult() {
        final AddressInfo info = peekAddressInfo(activeLegacySession);
        return info != null ? info.address().getAddress() : null;
    }

    @TruffleBoundary
    void startHostNameLookUp(final String hostName) {
        activeLegacySession = executeAsyncLookup(() -> {
            try {
                return performGetAddressInfo(hostName, null, 0, SQ_FAMILY_INET4, 0, 0);
            } catch (final UnknownHostException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    @TruffleBoundary
    void startAddressLookUp(final byte[] address) {
        activeLegacySession = executeAsyncLookup(() -> {
            try {
                final InetAddress inetAddress = InetAddress.getByAddress(address);

                // Force the blocking reverse-DNS lookup on the background thread.
                inetAddress.getHostName();

                final List<AddressInfo> results = new ArrayList<>();
                results.add(new AddressInfo(inetAddress, 0, SQ_FAMILY_INET4, 0, 0));
                return results;
            } catch (final UnknownHostException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    @TruffleBoundary
    String lastAddressLookUpResult() {
        final AddressInfo info = peekAddressInfo(activeLegacySession);
        return info != null ? info.address().getHostName() : null;
    }

    /**
     * Provide a status for legacy polling loops (primitiveResolverStatus) and for initialization
     * probing at start up.
     */
    @TruffleBoundary
    Status getLegacyStatus() {
        if (!initialized) {
            return Status.Uninitialized;
        }

        if (activeLegacySession != null) {
            final Status status = activeLegacySession.getStatus();
            if (status == Status.Busy || status == Status.Error) {
                return status;
            }
        }

        return Status.Ready;
    }
}
