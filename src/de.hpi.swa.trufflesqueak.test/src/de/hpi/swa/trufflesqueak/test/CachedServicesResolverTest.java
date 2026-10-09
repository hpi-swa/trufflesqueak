/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.hpi.swa.trufflesqueak.nodes.plugins.network.CachedServicesResolver;

@SuppressWarnings("static-method")
public final class CachedServicesResolverTest {

    @Test
    public void testResolveKnownTcpServices() {
        assertEquals(80, CachedServicesResolver.resolvePort("http", "tcp"));
        assertEquals(80, CachedServicesResolver.resolvePort("www", "tcp"));
        assertEquals(443, CachedServicesResolver.resolvePort("https", "tcp"));
        assertEquals(22, CachedServicesResolver.resolvePort("ssh", "tcp"));
    }

    @Test
    public void testResolveKnownUdpServices() {
        assertEquals(53, CachedServicesResolver.resolvePort("domain", "udp"));
    }

    @Test
    public void testResolveUnknownServiceReturnsNegativeOne() {
        assertEquals(-1, CachedServicesResolver.resolvePort("foo", "tcp"));
    }
}
