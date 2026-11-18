/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.junit.Test;

import de.hpi.swa.trufflesqueak.nodes.plugins.network.SqueakOpaqueSocketAddress;

@SuppressWarnings("static-method")
public final class SqueakOpaqueSocketAddressTest {

    private static final int SESSION_ID = 0x12345678;

    @Test
    public void testIPv4RoundTripAndPortMutation() throws UnknownHostException {
        final InetAddress addr = InetAddress.getByName("127.0.0.1");
        final byte[] packed = SqueakOpaqueSocketAddress.pack(addr, 8080, SESSION_ID);
        assertEquals(24L, SqueakOpaqueSocketAddress.getOpaqueAddressSize(addr.getAddress()));
        assertEquals(8080, SqueakOpaqueSocketAddress.getPort(packed, SESSION_ID));

        SqueakOpaqueSocketAddress.setPort(packed, 65535, SESSION_ID);
        assertEquals(65535, SqueakOpaqueSocketAddress.getPort(packed, SESSION_ID));

        final InetSocketAddress unpacked = SqueakOpaqueSocketAddress.unpack(packed, SESSION_ID);
        assertEquals(addr, unpacked.getAddress());
        assertEquals(65535, unpacked.getPort());
    }

    @Test
    public void testIPv6WithScopeIdRoundTrip() throws UnknownHostException {
        final byte[] raw = new byte[]{(byte) 0xfe, (byte) 0x80, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1};
        final Inet6Address addr = Inet6Address.getByAddress(null, raw, 3);
        final byte[] packed = SqueakOpaqueSocketAddress.pack(addr, 443, SESSION_ID);
        assertEquals(36L, SqueakOpaqueSocketAddress.getOpaqueAddressSize(raw));

        final InetSocketAddress unpacked = SqueakOpaqueSocketAddress.unpack(packed, SESSION_ID);
        assertEquals(addr, unpacked.getAddress());
        assertEquals(3, ((Inet6Address) unpacked.getAddress()).getScopeId());
        assertEquals(443, unpacked.getPort());
    }

    @Test
    public void testStaleSessionIdRejected() throws UnknownHostException {
        final byte[] packed = SqueakOpaqueSocketAddress.pack(InetAddress.getByName("127.0.0.1"), 80, SESSION_ID);
        assertThrows(IllegalArgumentException.class, () -> SqueakOpaqueSocketAddress.unpack(packed, SESSION_ID + 1));
        assertThrows(IllegalArgumentException.class, () -> SqueakOpaqueSocketAddress.getPort(packed, SESSION_ID + 1));
        assertThrows(IllegalArgumentException.class, () -> SqueakOpaqueSocketAddress.setPort(packed, 81, SESSION_ID + 1));
    }

    @Test
    public void testInvalidPortAndTruncatedBuffersRejected() throws UnknownHostException {
        final InetAddress addr = InetAddress.getByName("127.0.0.1");
        assertThrows(IllegalArgumentException.class, () -> SqueakOpaqueSocketAddress.pack(addr, -1, SESSION_ID));
        assertThrows(IllegalArgumentException.class, () -> SqueakOpaqueSocketAddress.pack(addr, 65536, SESSION_ID));

        final byte[] packed = SqueakOpaqueSocketAddress.pack(addr, 80, SESSION_ID);
        assertThrows(IllegalArgumentException.class, () -> SqueakOpaqueSocketAddress.setPort(packed, 70000, SESSION_ID));
        assertThrows(IllegalArgumentException.class, () -> SqueakOpaqueSocketAddress.unpack(new byte[10], SESSION_ID));
    }
}
