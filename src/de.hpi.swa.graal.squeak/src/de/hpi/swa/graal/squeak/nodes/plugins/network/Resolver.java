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

    private static InetAddress anyLocalAddress = new InetSocketAddress(0).getAddress();
    private static InetAddress loopbackAddress = InetAddress.getLoopbackAddress();

    private static byte[] lastNameLookup;
    private static String lastAddressLookup;

    private Resolver() {
    }

    static byte[] getAnyLocalAddress() {
        return anyLocalAddress.getAddress();
    }

    static byte[] getLoopbackAddress() {
        return loopbackAddress.getAddress();
    }

    static void startHostNameLookUp(final String hostName) throws UnknownHostException {
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

    static byte[] lastHostNameLookupResult() {
        return lastNameLookup;
    }

    static void startAddressLookUp(final byte[] address) throws UnknownHostException {
        try {
            lastAddressLookup = InetAddress.getByAddress(address).getHostName();
        } catch (final UnknownHostException e) {
            lastAddressLookup = null;
            throw e;
        }
    }

    static String lastAddressLookUpResult() {
        return lastAddressLookup;
    }

    static String addressBytesToString(final byte[] address) {
        try {
            return InetAddress.getByAddress(address).getHostAddress();
        } catch (final UnknownHostException e) {
            return null;
        }
    }
}
