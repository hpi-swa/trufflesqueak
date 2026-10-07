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
import static de.hpi.swa.trufflesqueak.nodes.plugins.network.SqueakSocketContext.SQ_SOCKET_NUMERIC;
import static de.hpi.swa.trufflesqueak.nodes.plugins.network.SqueakSocketContext.SQ_SOCKET_PASSIVE;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
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

    private static final long LEGACY_HOST_LOOKUP_HANDLE = -1L;
    private static final long LEGACY_ADDRESS_LOOKUP_HANDLE = -2L;

    private final SqueakSocketContext context;

    private final AtomicLong handleGenerator = new AtomicLong(1);
    private final ConcurrentHashMap<Long, AsyncSession> lookupSessions = new ConcurrentHashMap<>();

    private Runnable statusChangeCallback;

    private List<AddressInfo> currentAddressInfoList = null;
    private String lastHostNameInfo = "";
    private String lastServiceInfo = "";

    private InetAddress anyLocalAddress;
    private InetAddress loopbackAddress;

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

    private long executeAsyncLookup(final long handle, final Supplier<List<AddressInfo>> lookupTask) {
        final CompletableFuture<List<AddressInfo>> future = new CompletableFuture<>();
        lookupSessions.put(handle, new AsyncSession(future));

        future.whenComplete((@SuppressWarnings("unused") final List<AddressInfo> res, @SuppressWarnings("unused") final Throwable ex) -> triggerStatusChange());

        CompletableFuture.runAsync(() -> {
            try {
                future.complete(lookupTask.get());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });

        return handle;
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

    byte[] getAnyLocalAddress() {
        if (anyLocalAddress == null) {
            anyLocalAddress = new InetSocketAddress(0).getAddress();
        }
        return anyLocalAddress.getAddress();
    }

    @TruffleBoundary
    byte[] getLoopbackAddress() {
        if (loopbackAddress == null) {
            loopbackAddress = InetAddress.getLoopbackAddress();
        }
        return loopbackAddress.getAddress();
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

    private List<AddressInfo> performGetAddressInfo(final String hostName, final String serviceName, final int flags, final int family, final int type, final int protocol)
                    throws UnknownHostException {
        // Strict Numeric Validation
        if ((flags & SQ_SOCKET_NUMERIC) != 0 && hostName != null && !hostName.isEmpty()) {
            if (!isIPLiteral(hostName)) {
                throw new UnknownHostException("Host is not numeric");
            }
        }

        // Parse service name to a port
        int port = 0;
        if (serviceName != null && !serviceName.isEmpty()) {
            try {
                port = Integer.parseInt(serviceName);
            } catch (NumberFormatException e) {
                // Parse common named services
                port = switch (serviceName.toLowerCase()) {
                    case "http" -> 80;
                    case "https" -> 443;
                    case "ftp" -> 21;
                    case "ssh" -> 22;
                    case "smtp" -> 25;
                    default -> {
                        final String errorMsg = "Unparseable service name: " + serviceName;
                        LogUtils.SOCKET.warning(errorMsg);
                        throw new IllegalArgumentException(errorMsg);
                    }
                };
            }
        }

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

        final List<AddressInfo> results = new ArrayList<>();
        for (final InetAddress address : addresses) {
            final int sqFamily = (address instanceof Inet4Address) ? SQ_FAMILY_INET4 : SQ_FAMILY_INET6;

            // Filter using SQUEAK constants
            if (family == SQ_FAMILY_UNSPEC || family == sqFamily) {
                results.add(new AddressInfo(address, port, sqFamily, type, protocol));
            }
        }

        if (results.isEmpty()) {
            throw new UnknownHostException("No addresses found for family");
        }
        return results;
    }

    @TruffleBoundary
    long startAddressInfoLookup(final String hostName, final String serviceName, final int flags, final int family, final int type, final int protocol) {
        final long handle = handleGenerator.getAndIncrement();
        return executeAsyncLookup(handle, () -> {
            try {
                return performGetAddressInfo(hostName, serviceName, flags, family, type, protocol);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @TruffleBoundary
    AddressInfo getNextAddressInfo(final long handle) {
        return fetchAddressInfo(handle, true);
    }

    @TruffleBoundary
    AddressInfo peekAddressInfo(final long handle) {
        return fetchAddressInfo(handle, false);
    }

    private AddressInfo fetchAddressInfo(final long handle, final boolean consume) {
        final AsyncSession session = lookupSessions.get(handle);
        if (session != null) {
            if (session.getStatus() == Status.Ready) {
                try {
                    final List<AddressInfo> results = session.future().get();
                    if (!results.isEmpty()) {
                        final AddressInfo info = consume ? results.removeFirst() : results.getFirst();
                        if (consume && results.isEmpty()) {
                            lookupSessions.remove(handle);
                        }
                        return info;
                    }
                } catch (Exception e) {
                    if (consume) {
                        lookupSessions.remove(handle);
                    }
                }
            } else if (session.getStatus() == Status.Error) {
                if (consume) {
                    lookupSessions.remove(handle);
                }
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
        final AddressInfo info = peekAddressInfo(LEGACY_HOST_LOOKUP_HANDLE);
        return info != null ? info.address().getAddress() : null;
    }

    @TruffleBoundary
    void startHostNameLookUp(final String hostName) {
        lookupSessions.remove(LEGACY_HOST_LOOKUP_HANDLE);

        executeAsyncLookup(LEGACY_HOST_LOOKUP_HANDLE, () -> {
            try {
                return performGetAddressInfo(hostName, null, 0, SQ_FAMILY_INET4, 0, 0);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @TruffleBoundary
    void startAddressLookUp(final byte[] address) {
        lookupSessions.remove(LEGACY_ADDRESS_LOOKUP_HANDLE);

        executeAsyncLookup(LEGACY_ADDRESS_LOOKUP_HANDLE, () -> {
            try {
                final InetAddress inetAddress = InetAddress.getByAddress(address);

                // Force the blocking reverse-DNS lookup on the background thread.
                inetAddress.getHostName();

                final List<AddressInfo> results = new ArrayList<>();
                results.add(new AddressInfo(inetAddress, 0, SQ_FAMILY_INET4, 0, 0));
                return results;
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @TruffleBoundary
    String lastAddressLookUpResult() {
        final AddressInfo info = peekAddressInfo(LEGACY_ADDRESS_LOOKUP_HANDLE);
        return info != null ? info.address().getHostName() : null;
    }

    /**
     * Provide a status for legacy polling loops (primitiveResolverStatus) and for initialization
     * probing at start up.
     */
    @TruffleBoundary
    Status getLegacyStatus() {
        if (statusChangeCallback == null) {
            return Status.Uninitialized;
        }

        final AsyncSession hostSession = lookupSessions.get(LEGACY_HOST_LOOKUP_HANDLE);
        if (hostSession != null && hostSession.getStatus() == Status.Busy) {
            return Status.Busy;
        }
        final AsyncSession addressSession = lookupSessions.get(LEGACY_ADDRESS_LOOKUP_HANDLE);
        if (addressSession != null && addressSession.getStatus() == Status.Busy) {
            return Status.Busy;
        }
        return Status.Ready;
    }
}
