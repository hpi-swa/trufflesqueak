/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.Map;

import org.junit.Test;

import de.hpi.swa.trufflesqueak.shared.ImageDownloadSupport;

@SuppressWarnings("static-method")
public final class ImageDownloadSupportTest {
    @Test
    public void testHttpsProxyIsSelectedFromEnvironment() {
        final ImageDownloadSupport.ProxyConfiguration configuration = ImageDownloadSupport.proxyConfigurationFor(
                        URI.create("https://github.com/hpi-swa/trufflesqueak/releases/download/25.0.1/TruffleSqueakImage-25.0.1.zip"),
                        Map.of("https_proxy", "http://proxy.example.com:8443"));

        assertEquals(ImageDownloadSupport.ProxyMode.PROXY, configuration.mode());
        assertEquals("proxy.example.com", configuration.host());
        assertEquals(8443, configuration.port());
    }

    @Test
    public void testHttpProxyFallsBackForHttps() {
        final ImageDownloadSupport.ProxyConfiguration configuration = ImageDownloadSupport.proxyConfigurationFor(
                        URI.create("https://files.squeak.org/6.0/Squeak6.0-22148-64bit/Squeak6.0-22148-64bit.zip"),
                        Map.of("http_proxy", "proxy.example.com:3128"));

        assertEquals(ImageDownloadSupport.ProxyMode.PROXY, configuration.mode());
        assertEquals("proxy.example.com", configuration.host());
        assertEquals(3128, configuration.port());
    }

    @Test
    public void testNoProxyBypassesProxyForDomainSuffix() {
        final ImageDownloadSupport.ProxyConfiguration configuration = ImageDownloadSupport.proxyConfigurationFor(
                        URI.create("https://github.com/hpi-swa/trufflesqueak/releases/download/25.0.1/TruffleSqueakImage-25.0.1.zip"),
                        Map.of(
                                        "https_proxy", "http://proxy.example.com:8443",
                                        "no_proxy", ".github.com"));

        assertEquals(ImageDownloadSupport.ProxyMode.DIRECT, configuration.mode());
    }

    @Test
    public void testNoProxyCanMatchSpecificPort() {
        assertTrue(ImageDownloadSupport.matchesNoProxy("files.squeak.org", 443, "files.squeak.org:443"));
    }

    @Test
    public void testProxyCredentialsAreDecoded() {
        final ImageDownloadSupport.ProxyConfiguration configuration = ImageDownloadSupport.proxyConfigurationFor(
                        URI.create("https://example.com/archive.zip"),
                        Map.of("HTTPS_PROXY", "http://user%40example:pa%3Ass@proxy.example.com:8080"));

        assertEquals(ImageDownloadSupport.ProxyMode.PROXY, configuration.mode());
        assertEquals("user@example", configuration.username());
        assertEquals("pa:ss", configuration.password());
    }
}
