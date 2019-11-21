/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.plugins.network;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

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

    private static InetAddress anyLocalAddress = null;
    private static InetAddress loopbackAddress = null;

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

    protected static byte[] getLoopbackAddress() {
        if (loopbackAddress == null) {
            loopbackAddress = InetAddress.getLoopbackAddress();
        }
        return loopbackAddress.getAddress();
    }

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

    protected static String addressBytesToString(final byte[] address) {
        try {
            return InetAddress.getByAddress(address).getHostAddress();
        } catch (final UnknownHostException e) {
            return null;
        }
    }
}
