/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins.network;

import de.hpi.swa.trufflesqueak.util.LogUtils;
import de.hpi.swa.trufflesqueak.util.OS;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class CachedServicesResolver {

    // Composite key: service-name (or alias) + protocol (e.g., "http#tcp")
    private record ServiceKey(String name, String protocol) {
        ServiceKey {
            name = name.toLowerCase(Locale.ROOT);
            protocol = protocol.toLowerCase(Locale.ROOT);
        }
    }

    private static final Path DEFAULT_SERVICES_PATH = detectServicesPath();
    private static final Map<ServiceKey, Integer> CACHE = new ConcurrentHashMap<>();
    private static volatile boolean initialized;

    private CachedServicesResolver() {
    }

    private static Path detectServicesPath() {
        if (!OS.isWindows()) {
            return Path.of("/etc/services");
        }

        final String sysDrive = System.getenv("SystemDrive");
        final String[] candidateBases = {
                        System.getenv("SystemRoot"),
                        System.getenv("windir"),
                        (sysDrive != null && !sysDrive.isBlank()) ? sysDrive + "\\Windows" : null
        };

        for (final String base : candidateBases) {
            if (base != null && !base.isBlank()) {
                final Path path = Path.of(base, "system32", "drivers", "etc", "services");
                if (Files.exists(path)) {
                    return path;
                }
            }
        }

        // Last-ditch: Scan available filesystem roots (C:\, D:\, etc.)
        for (final Path root : FileSystems.getDefault().getRootDirectories()) {
            final Path candidate = root.resolve(Path.of("Windows", "system32", "drivers", "etc", "services"));
            if (Files.exists(candidate)) {
                return candidate;
            }
        }

        // Final default matching typical layout
        return Path.of("C:\\Windows", "system32", "drivers", "etc", "services");
    }

    /**
     * Preloads and populates the in-memory cache.
     * Safe to call multiple times or invoke concurrently.
     */
    public static synchronized void initializeCache(final Path customPath) {
        final Path targetPath = (customPath != null) ? customPath : DEFAULT_SERVICES_PATH;
        final Map<ServiceKey, Integer> freshMap = new HashMap<>();

        if (Files.exists(targetPath)) {
            try (BufferedReader reader = Files.newBufferedReader(targetPath)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    parseServiceLine(line, freshMap);
                }
            } catch (IOException e) {
                // Ignore exception and fall through to the empty-check below
            }
        }

        // If the file was missing, unreadable, or empty, load standard defaults
        if (freshMap.isEmpty()) {
            LogUtils.SOCKET.fine("Services file unreadable or empty; loading fallback standard ports.");
            populateFallbackServices(freshMap);
        }

        CACHE.clear();
        CACHE.putAll(freshMap);
        initialized = true;
    }

    private static void parseServiceLine(final String line, final Map<ServiceKey, Integer> freshMap) {
        String processedLine = line.trim();
        if (processedLine.isEmpty() || processedLine.startsWith("#")) {
            return;
        }

        // Strip inline comments
        final int commentIndex = processedLine.indexOf('#');
        if (commentIndex != -1) {
            processedLine = processedLine.substring(0, commentIndex).trim();
        }

        final String[] tokens = processedLine.split("\\s+");
        if (tokens.length < 2) {
            return;
        }

        final String canonicalName = tokens[0];
        final String[] portAndProto = tokens[1].split("/");
        if (portAndProto.length != 2) {
            return;
        }

        final int port;
        try {
            port = Integer.parseInt(portAndProto[0]);
        } catch (NumberFormatException ignored) {
            return;
        }

        final String protocol = portAndProto[1];

        // Map primary service name
        freshMap.putIfAbsent(new ServiceKey(canonicalName, protocol), port);

        // Map all aliases listed in remaining columns
        for (int i = 2; i < tokens.length; i++) {
            freshMap.putIfAbsent(new ServiceKey(tokens[i], protocol), port);
        }
    }

    private static void populateFallbackServices(final Map<ServiceKey, Integer> map) {
        map.put(new ServiceKey("http", "tcp"), 80);
        map.put(new ServiceKey("www", "tcp"), 80); // Alias
        map.put(new ServiceKey("https", "tcp"), 443);
        map.put(new ServiceKey("ftp", "tcp"), 21);
        map.put(new ServiceKey("ssh", "tcp"), 22);
        map.put(new ServiceKey("smtp", "tcp"), 25);
        map.put(new ServiceKey("domain", "udp"), 53);
        map.put(new ServiceKey("domain", "tcp"), 53);
        map.put(new ServiceKey("pop3", "tcp"), 110);
        map.put(new ServiceKey("imap", "tcp"), 143);
    }

    /**
     * Resolves a service name and protocol to a port number in O(1) time.
     * Automatically performs lazy initialization on first lookup.
     *
     * @return the port number, or -1 if not found
     */
    public static int resolvePort(final String serviceName, final String protocol) {
        if (!initialized) {
            initializeCache(null);
        }

        if (serviceName == null || protocol == null) {
            return -1;
        }

        return CACHE.getOrDefault(new ServiceKey(serviceName, protocol), -1);
    }
}
