/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.shared;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ImageDownloadSupport {
    private static final ProxySelector NO_PROXY_SELECTOR = new ProxySelector() {
        @Override
        public List<Proxy> select(final URI uri) {
            return List.of(Proxy.NO_PROXY);
        }

        @Override
        public void connectFailed(final URI uri, final java.net.SocketAddress sa, final IOException ioe) {
            // Intentionally ignored because this selector always forces direct connections.
        }
    };

    public enum ProxyMode {
        DEFAULT,
        DIRECT,
        PROXY
    }

    public record ProxyConfiguration(ProxyMode mode, String host, int port, String username, String password) {
    }

    private ImageDownloadSupport() {
    }

    public static BufferedInputStream openStream(final URI uri) throws IOException {
        return openStream(uri, System.getenv());
    }

    static BufferedInputStream openStream(final URI uri, final Map<String, String> environment) throws IOException {
        final HttpClient.Builder builder = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL);
        final ProxyConfiguration proxyConfiguration = proxyConfigurationFor(uri, environment);
        switch (proxyConfiguration.mode()) {
            case DIRECT -> builder.proxy(NO_PROXY_SELECTOR);
            case PROXY -> {
                builder.proxy(ProxySelector.of(new InetSocketAddress(proxyConfiguration.host(), proxyConfiguration.port())));
                if (proxyConfiguration.username() != null) {
                    builder.authenticator(new Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            if (getRequestorType() == RequestorType.PROXY &&
                                            proxyConfiguration.host().equalsIgnoreCase(getRequestingHost()) &&
                                            proxyConfiguration.port() == getRequestingPort()) {
                                return new PasswordAuthentication(proxyConfiguration.username(), proxyConfiguration.password().toCharArray());
                            }
                            return null;
                        }
                    });
                }
            }
            case DEFAULT -> {
            }
            default -> throw new IllegalStateException("Unhandled proxy mode: " + proxyConfiguration.mode());
        }
        final HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
        try {
            final HttpResponse<InputStream> response = builder.build().send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                response.body().close();
                throw new IOException("Failed to download " + uri + " (HTTP " + response.statusCode() + ")");
            }
            return new BufferedInputStream(response.body());
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while downloading " + uri, e);
        } catch (final IllegalArgumentException e) {
            throw new IOException("Invalid proxy configuration for " + uri + ": " + e.getMessage(), e);
        }
    }

    public static ProxyConfiguration proxyConfigurationFor(final URI uri, final Map<String, String> environment) {
        final String scheme = uri.getScheme();
        if (scheme == null) {
            return new ProxyConfiguration(ProxyMode.DEFAULT, null, -1, null, null);
        }
        final String host = uri.getHost();
        final int port = effectivePort(uri);
        final String noProxy = getEnvironmentValue(environment, "no_proxy", "NO_PROXY");
        if (host != null && noProxy != null && matchesNoProxy(host, port, noProxy)) {
            return new ProxyConfiguration(ProxyMode.DIRECT, null, -1, null, null);
        }
        final String proxyValue;
        if ("https".equalsIgnoreCase(scheme)) {
            proxyValue = getEnvironmentValue(environment, "https_proxy", "HTTPS_PROXY", "http_proxy", "HTTP_PROXY");
        } else if ("http".equalsIgnoreCase(scheme)) {
            proxyValue = getEnvironmentValue(environment, "http_proxy", "HTTP_PROXY");
        } else {
            proxyValue = null;
        }
        if (proxyValue == null || proxyValue.isBlank()) {
            return new ProxyConfiguration(ProxyMode.DEFAULT, null, -1, null, null);
        }
        return parseProxyConfiguration(proxyValue);
    }

    public static boolean matchesNoProxy(final String host, final int port, final String noProxyValue) {
        for (final String rawEntry : noProxyValue.split(",")) {
            final String entry = rawEntry.trim();
            if (!entry.isEmpty() && matchesNoProxyEntry(host, port, entry)) {
                return true;
            }
        }
        return false;
    }

    private static ProxyConfiguration parseProxyConfiguration(final String proxyValue) {
        final String normalizedProxyValue = proxyValue.contains("://") ? proxyValue : "http://" + proxyValue;
        final URI proxyUri = URI.create(normalizedProxyValue);
        final String host = proxyUri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("missing proxy host");
        }
        final int port = proxyUri.getPort() != -1 ? proxyUri.getPort() : defaultPort(proxyUri.getScheme());
        final String rawUserInfo = proxyUri.getRawUserInfo();
        if (rawUserInfo == null) {
            return new ProxyConfiguration(ProxyMode.PROXY, host, port, null, null);
        }
        final String[] parts = rawUserInfo.split(":", 2);
        final String username = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
        final String password = parts.length > 1 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
        return new ProxyConfiguration(ProxyMode.PROXY, host, port, username, password);
    }

    private static boolean matchesNoProxyEntry(final String host, final int port, final String entry) {
        if ("*".equals(entry)) {
            return true;
        }
        final HostAndPort hostAndPort = parseHostAndPort(entry);
        if (hostAndPort.port() != -1 && hostAndPort.port() != port) {
            return false;
        }
        final String normalizedHost = host.toLowerCase(Locale.ROOT);
        final String normalizedEntryHost = hostAndPort.host().toLowerCase(Locale.ROOT);
        if (normalizedEntryHost.startsWith(".")) {
            final String suffix = normalizedEntryHost.substring(1);
            return normalizedHost.equals(suffix) || normalizedHost.endsWith(normalizedEntryHost);
        }
        return normalizedHost.equals(normalizedEntryHost) || normalizedHost.endsWith("." + normalizedEntryHost);
    }

    private static HostAndPort parseHostAndPort(final String value) {
        if (value.startsWith("[") && value.contains("]")) {
            final int endBracket = value.indexOf(']');
            final String host = value.substring(1, endBracket);
            if (endBracket + 1 < value.length() && value.charAt(endBracket + 1) == ':') {
                return new HostAndPort(host, Integer.parseInt(value.substring(endBracket + 2)));
            }
            return new HostAndPort(host, -1);
        }
        final int firstColon = value.indexOf(':');
        final int lastColon = value.lastIndexOf(':');
        if (firstColon != -1 && firstColon == lastColon) {
            final String portPart = value.substring(lastColon + 1);
            if (portPart.chars().allMatch(Character::isDigit)) {
                return new HostAndPort(value.substring(0, lastColon), Integer.parseInt(portPart));
            }
        }
        return new HostAndPort(value, -1);
    }

    private static int effectivePort(final URI uri) {
        return uri.getPort() != -1 ? uri.getPort() : defaultPort(uri.getScheme());
    }

    private static int defaultPort(final String scheme) {
        return "https".equalsIgnoreCase(scheme) ? 443 : 80;
    }

    private static String getEnvironmentValue(final Map<String, String> environment, final String... names) {
        for (final String name : names) {
            final String value = environment.get(name);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private record HostAndPort(String host, int port) {
    }
}
