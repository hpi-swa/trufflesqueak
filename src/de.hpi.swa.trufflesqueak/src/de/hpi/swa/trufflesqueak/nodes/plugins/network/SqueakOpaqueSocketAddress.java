/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins.network;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import de.hpi.swa.trufflesqueak.util.OS;

final class SqueakOpaqueSocketAddress {

    private static final int SQUEAK_HEADER_SIZE = 8;
    private static final int IPV4_STRUCT_SIZE = 16;
    private static final int IPV6_STRUCT_SIZE = 28;
    private static final int MIN_PORT_CHECK_SIZE = 12; // Header + Family + Port

    // OS Native IPv6 Family (Used exclusively for struct packing)
    private static final int OS_FAMILY_INET = 2;
    private static final int OS_FAMILY_INET6 = OS.isLinux() ? 10 : (OS.isWindows() ? 23 : 30);

    record AddressInfo(InetAddress address, int port, int family, int type, int protocol) {
        int getStructSize() {
            return address instanceof Inet4Address ? IPV4_STRUCT_SIZE : IPV6_STRUCT_SIZE;
        }

        @TruffleBoundary
        byte[] toSockaddrBytes(final int sessionID) {
            return SqueakOpaqueSocketAddress.pack(address, port, sessionID);
        }
    }

    @TruffleBoundary
    static long getOpaqueAddressSize(final byte[] rawIpAddress) {
        // 8 bytes for the addressHeader (sessionID + size)
        // 16 bytes for IPv4 sockaddr_in, 28 bytes for IPv6 sockaddr_in6
        return SQUEAK_HEADER_SIZE + (rawIpAddress.length == 4 ? IPV4_STRUCT_SIZE : IPV6_STRUCT_SIZE);
    }

    private SqueakOpaqueSocketAddress() {
    }

    @TruffleBoundary
    static byte[] pack(final InetAddress address, final int port, final int sessionID) {
        final boolean isIPv4 = address instanceof Inet4Address;
        final int structSize = isIPv4 ? IPV4_STRUCT_SIZE : IPV6_STRUCT_SIZE;

        // Allocate 8 bytes for addressHeader (sessionID + size) + sockaddr struct
        final ByteBuffer buffer = ByteBuffer.allocate(SQUEAK_HEADER_SIZE + structSize);

        // Write the 8-byte addressHeader using the context's unique sessionID
        buffer.order(ByteOrder.nativeOrder());
        buffer.putInt(sessionID);
        buffer.putInt(structSize);

        // Write the sockaddr struct
        final int osFamily = isIPv4 ? OS_FAMILY_INET : OS_FAMILY_INET6;

        if (OS.isMacOS()) {
            buffer.put((byte) structSize);
            buffer.put((byte) osFamily);
        } else {
            buffer.putShort((short) osFamily);
        }

        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) port);

        if (isIPv4) {
            buffer.put(address.getAddress());
            buffer.put(new byte[8]); // 8 bytes of padding for sockaddr_in
        } else {
            buffer.putInt(0); // flow info
            buffer.put(address.getAddress());
            int scopeId = 0;
            if (address instanceof Inet6Address inet6Address) {
                scopeId = inet6Address.getScopeId();
            }
            buffer.order(ByteOrder.nativeOrder());
            buffer.putInt(scopeId);
        }

        return buffer.array();
    }

    @TruffleBoundary
    static InetSocketAddress unpack(final byte[] opaqueArray, final int expectedSessionId) throws UnknownHostException {
        if (opaqueArray.length < SQUEAK_HEADER_SIZE + IPV4_STRUCT_SIZE) {
            throw new IllegalArgumentException("Address array too small");
        }
        final ByteBuffer buffer = ByteBuffer.wrap(opaqueArray);
        buffer.order(ByteOrder.nativeOrder());

        checkSessionId(buffer, expectedSessionId);
        buffer.getInt(); // Skip size

        int family;
        if (OS.isMacOS()) {
            buffer.get(); // Skip length byte
            family = buffer.get();
        } else {
            family = buffer.getShort();
        }

        buffer.order(ByteOrder.BIG_ENDIAN);
        final int port = Short.toUnsignedInt(buffer.getShort());

        final byte[] ip;
        int scopeId = 0;

        if (family == OS_FAMILY_INET) {
            ip = new byte[4];
            buffer.get(ip);
        } else if (family == OS_FAMILY_INET6) {
            if (opaqueArray.length < SQUEAK_HEADER_SIZE + IPV6_STRUCT_SIZE) {
                throw new IllegalArgumentException("IPv6 Address array too small");
            }
            buffer.getInt(); // Skip flow info
            ip = new byte[16];
            buffer.get(ip);
            buffer.order(ByteOrder.nativeOrder()); // Scope ID is in native byte order
            scopeId = buffer.getInt();
        } else {
            throw new IllegalArgumentException("Unsupported OS address family: " + family);
        }

        final InetAddress address;
        if (family == OS_FAMILY_INET6 && scopeId != 0) {
            // Only apply scope if one was explicitly set
            address = Inet6Address.getByAddress(null, ip, scopeId);
        } else {
            address = InetAddress.getByAddress(ip);
        }

        return new InetSocketAddress(address, port);
    }

    @TruffleBoundary
    static int getPort(final byte[] opaqueArray, final int expectedSessionId) {
        if (opaqueArray.length < MIN_PORT_CHECK_SIZE) {
            throw new IllegalArgumentException("Address array too small");
        }
        final ByteBuffer buffer = ByteBuffer.wrap(opaqueArray);
        buffer.order(ByteOrder.nativeOrder());

        checkSessionId(buffer, expectedSessionId);
        buffer.getInt(); // skip size

        if (OS.isMacOS()) {
            buffer.get(); // skip len
            buffer.get(); // skip family
        } else {
            buffer.getShort(); // skip family
        }

        buffer.order(ByteOrder.BIG_ENDIAN);
        return Short.toUnsignedInt(buffer.getShort());
    }

    @TruffleBoundary
    static void setPort(final byte[] opaqueArray, final int port, final int expectedSessionId) {
        if (opaqueArray.length < MIN_PORT_CHECK_SIZE) {
            throw new IllegalArgumentException("Address array too small");
        }
        final ByteBuffer buffer = ByteBuffer.wrap(opaqueArray);
        buffer.order(ByteOrder.nativeOrder());

        checkSessionId(buffer, expectedSessionId);
        buffer.getInt(); // skip size

        if (OS.isMacOS()) {
            buffer.get(); // skip len
            buffer.get(); // skip family
        } else {
            buffer.getShort(); // skip family
        }

        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) port);
    }

    @TruffleBoundary
    static String getIpAddressString(final byte[] bytes, final int expectedSessionId) {
        if (bytes == null) {
            return null;
        }
        try {
            if (bytes.length >= SQUEAK_HEADER_SIZE) {
                try {
                    final InetAddress addr = unpack(bytes, expectedSessionId).getAddress();
                    if (addr.isAnyLocalAddress() && addr instanceof Inet6Address) {
                        return "::";
                    }
                    return addr.getHostAddress();
                } catch (final Exception e) {
                    // Fall through to legacy fallback parsing if parsing struct fails
                }
            }
            // Legacy primitive support (direct IP byte arrays without 8-byte headers)
            final InetAddress addr = InetAddress.getByAddress(bytes);
            if (addr.isAnyLocalAddress() && addr instanceof Inet6Address) {
                return "::";
            }
            return addr.getHostAddress();
        } catch (final UnknownHostException e) {
            return null;
        }
    }

    private static void checkSessionId(final ByteBuffer buffer, final int expectedSessionId) {
        final int sid = buffer.getInt();
        if (sid != expectedSessionId) {
            throw new IllegalArgumentException("Stale network session ID");
        }
    }
}
