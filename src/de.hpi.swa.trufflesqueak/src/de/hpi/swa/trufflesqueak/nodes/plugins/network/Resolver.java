/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins.network;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

final class Resolver {

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

    private static InetAddress anyLocalAddress;
    private static InetAddress loopbackAddress;

    private static byte[] lastNameLookup;
    private static String lastAddressLookup;

    private Resolver() {
    }

    protected static byte[] getAnyLocalAddress() {
        if (anyLocalAddress == null) {
            anyLocalAddress = new InetSocketAddress(0).getAddress();
        }
        return anyLocalAddress.getAddress();
    }

    @TruffleBoundary
    protected static byte[] getLoopbackAddress() {
        if (loopbackAddress == null) {
            loopbackAddress = InetAddress.getLoopbackAddress();
        }
        return loopbackAddress.getAddress();
    }

    @TruffleBoundary
    protected static void startHostNameLookUp(final String hostName) throws UnknownHostException {
        try {
            if ("localhost".equals(hostName)) {
                lastNameLookup = Resolver.getLoopbackAddress();
                return;
            }

            final InetAddress address = InetAddress.getByName(hostName);
            lastNameLookup = address.getAddress();
        } catch (final UnknownHostException e) {
            lastNameLookup = null;
            throw e;
        }
    }

    protected static byte[] lastHostNameLookupResult() {
        return lastNameLookup;
    }

    @TruffleBoundary
    protected static void startAddressLookUp(final byte[] address) throws UnknownHostException {
        try {
            lastAddressLookup = InetAddress.getByAddress(address).getHostName();
        } catch (final UnknownHostException e) {
            lastAddressLookup = null;
            throw e;
        }
    }

    protected static String lastAddressLookUpResult() {
        return lastAddressLookup;
    }

    @TruffleBoundary
    protected static String addressBytesToString(final byte[] address) {
        try {
            return InetAddress.getByAddress(address).getHostAddress();
        } catch (final UnknownHostException e) {
            return null;
        }
    }
}
