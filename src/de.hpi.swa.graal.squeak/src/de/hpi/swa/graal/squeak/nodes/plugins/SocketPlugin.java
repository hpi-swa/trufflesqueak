package de.hpi.swa.graal.squeak.nodes.plugins;

import java.util.List;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;

import org.graalvm.collections.EconomicMap;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NotProvided;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.accessing.NativeObjectNodes.NativeGetBytesNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;

public final class SocketPlugin extends AbstractPrimitiveFactoryHolder {

    @SuppressWarnings("unused")
    private static final class ResolverStatus {
        private static final long Uninitialized = 0;
        private static final long Ready = 1;
        private static final long Busy = 2;
        private static final long Error = 3;
    }

    private static byte[] lastNameLookup;
    private static String lastAddressLookup;

    private static final class SocketStatus {
        private static final long InvalidSocket = -1;
        private static final long Unconnected = 0;
        private static final long WaitingForConnection = 1;
        private static final long Connected = 2;
        private static final long OtherEndClosed = 3;
        private static final long ThisEndClosed = 4;
    }

    private static final class SocketType {
        private static final long TCPSocketType = 0;
        private static final long UDPSocketType = 1;
    }

    private static final EconomicMap<Long, SocketImpl> sockets = EconomicMap.create();
    private static final boolean debugPrints = true;

    private static final class Resolver {
        @SuppressWarnings("unused")
        public static byte[] getLocalAddress() throws UnknownHostException {
            return new byte[]{127, 0, 0, 1};
        }

        public static InetAddress getLocalHostInetAddress() throws IOException {
            return InetAddress.getByAddress(Resolver.getLocalAddress());
        }
    }

    private static final class SocketImpl {
        private static final Duration timeout = Duration.ofSeconds(30);

        private final CompiledCodeObject code;
        private final int id;

        private final long socketType;
        private Socket clientSocket;
        private ServerSocket serverSocket;
        private DatagramSocket datagramSocket;
        private Socket acceptedConnection;

        private Map<String, Object> options = new TreeMap<>();
        boolean listening = false;
        Instant noDataSince = null;

        SocketImpl(final CompiledCodeObject code, final long netType) {
            this.code = code;
            this.id = this.hashCode();
            this.socketType = netType;
            if (this.socketType == SocketType.TCPSocketType) {
                print(">> Creating TCP Socket");
            } else {
                print(">> Creating UDP Socket");
            }
        }

        public void print(final Object message) {
            if (debugPrints) {
                code.image.getOutput().println(id + ": " + message.toString());
            }
        }

        public void error(final Object message) {
            code.image.getError().println(id + ": " + message.toString());
        }

        public void listenOn(final int port) throws IOException {
            if (listening) {
                return;
            }
            print(">> Listening on " + port);

            if (socketType == SocketType.UDPSocketType) {
                listening = true;
                datagramSocket = new DatagramSocket(port);
            } else {
                serverSocket = new ServerSocket(port, 1, Resolver.getLocalHostInetAddress());
                print(">> Actually listening on " + Resolver.getLocalHostInetAddress() + ":" + serverSocket.getLocalPort());
                final SocketImpl self = this;
                final Thread listenerThread = new Thread() {
                    @Override
                    public void run() {
                        listening = true;
                        try {
                            while (serverSocket != null) {
                                acceptedConnection = serverSocket.accept();
                            }
                        } catch (SocketException se) {
                            // The socket has been closed while listening
                            // This is fine
                            listening = false;
                            print(">> Stopped listening");
                        } catch (IOException e) {
                            self.error(e);
                        }
                    }
                };
                listenerThread.start();
            }
        }

        public SocketImpl accept() throws IOException {
            print(">> Accepting");
            final SocketImpl connectionImpl = new SocketImpl(code, SocketType.TCPSocketType);
            if (acceptedConnection == null) {
                error("No connection was accepted");
                throw new IOException("No connection was accepted");
            }

            connectionImpl.clientSocket = acceptedConnection;
            acceptedConnection = null;
            return connectionImpl;
        }

        public void connectTo(final String host, final long port) throws IOException {
            print(">> Connecting to " + host + ":" + port);
            if (socketType == SocketType.TCPSocketType) {
                if (clientSocket != null) {
                    clientSocket.close();
                }
                // clientSocket = new Socket(host, (int) port,
                // Resolver.getLocalHostInetAddress(), 0);
                clientSocket = new Socket(host, (int) port);
            } else /* if (netType == SocketType.UDPSocketType) */ {
                if (datagramSocket != null) {
                    datagramSocket.close();
                }
                this.datagramSocket = new DatagramSocket();
                this.datagramSocket.connect(InetAddress.getByName(host), (int) port);
            }
        }

        public boolean isDataAvailable() throws IOException {
            boolean available = false;
            if (clientSocket != null) {
                if (!clientSocket.isClosed() || clientSocket.isConnected()) {
                    final InputStream inputStream = clientSocket.getInputStream();
                    if (inputStream != null) {
                        available = inputStream.available() > 0;
                    }
                }
            } else if (datagramSocket != null) {
                return true;
                // TODO
            }
            print(">> Data available: " + available);
            return available;
        }

        public int receiveData(final byte[] data, final int startIndex, final int count) throws IOException {
            print(">> Receive data, buffer length: " + data.length + ", start: " + startIndex + ", count: " + count);
            final int actualCount = count;
            if (clientSocket != null) {
                if (isDataAvailable()) {
                    final int bytesRead = clientSocket.getInputStream().read(data, startIndex, actualCount);
                    print(">> Bytes read: " + bytesRead);
                    return bytesRead >= 0 ? bytesRead : 0;
                } else {
                    print(">> No data available");
                    return 0;
                }
            } else if (datagramSocket != null) {
                final DatagramPacket p = new DatagramPacket(data, startIndex, actualCount);
                datagramSocket.receive(p);
                return p.getLength();
            } else {
                throw new IOException("Socket not connected!");
            }
        }

        public void sendData(final byte[] data, final int startIndex, final int count) throws IOException {
            print(">> Send Data");
            if (clientSocket != null) {
                if (!clientSocket.isConnected()) {
                    throw new IOException("Client socket is not connected!");
                }
                final OutputStream outputStream = clientSocket.getOutputStream();
                outputStream.write(data, startIndex, count);
                outputStream.flush();
            } else if (datagramSocket != null) {
                if (!datagramSocket.isConnected()) {
                    throw new IOException("Datagram socket is not connected!");
                }
                print("Send to " + datagramSocket.getRemoteSocketAddress());
                final DatagramPacket p = new DatagramPacket(data, startIndex, count);
                this.datagramSocket.send(p);
            } else {
                throw new IOException("Not Connected!");
            }
        }

        public void close() throws IOException {
            print(">> Closing");
            if (clientSocket != null) {
                clientSocket.close();
                clientSocket = null;
            }
            if (serverSocket != null) {
                serverSocket.close();
                serverSocket = null;
            }
            if (datagramSocket != null) {
                datagramSocket.close();
                datagramSocket = null;
            }
        }

        public long getStatus() throws IOException {
            long status = SocketStatus.Unconnected;

            if (clientSocket == null && serverSocket == null && datagramSocket == null) {
                status = SocketStatus.Unconnected;
            }

            if (serverSocket != null) {
                if (listening && acceptedConnection == null) {
                    status = SocketStatus.WaitingForConnection;
                } else if (acceptedConnection != null) {
                    status = SocketStatus.Connected;
                } else if (!listening) {
                    status = SocketStatus.Unconnected;
                } else {
                    throw new IOException("Undefined Socket Status");
                }
            }

            if (clientSocket != null) {
                if (clientSocket.isInputShutdown()) {
                    status = SocketStatus.ThisEndClosed;
                } else if (clientSocket.isOutputShutdown()) {
                    status = SocketStatus.OtherEndClosed;
                } else if (!clientSocket.isConnected()) {
                    status = SocketStatus.Unconnected;
                } else if (clientSocket.isClosed()) {
                    status = SocketStatus.ThisEndClosed;
                } else {
                    try {
                        if (clientSocket.getInputStream().available() > 0) {
                            status = SocketStatus.Connected;
                        } else {
                            if (noDataSince == null) {
                                noDataSince = Instant.now();
                                status = SocketStatus.Connected;
                            } else {
                                final Duration elapsedTime = Duration.between(noDataSince, Instant.now());
                                if (elapsedTime.compareTo(timeout) > 0) {
                                    status = SocketStatus.OtherEndClosed;
                                } else {
                                    status = SocketStatus.Connected;
                                }
                            }
                        }
                    } catch (IOException e) {
                        error(e);
                        status = SocketStatus.Unconnected;
                    }
                }
            }

            if (datagramSocket != null) {
                if (listening) {
                    status = SocketStatus.WaitingForConnection;
                } else if (datagramSocket.isConnected()) {
                    status = SocketStatus.Connected;
                } else {
                    status = SocketStatus.Unconnected;
                }
            }

            String statusString = "Undefined";

            if (status == SocketStatus.Connected) {
                statusString = "Connected";
            } else if (status == SocketStatus.InvalidSocket) {
                statusString = "InvalidSocket";
            } else if (status == SocketStatus.Unconnected) {
                statusString = "Unconnected";
            } else if (status == SocketStatus.WaitingForConnection) {
                statusString = "WaitingForConnection";
            } else if (status == SocketStatus.OtherEndClosed) {
                statusString = "OtherEndClosed";
            } else if (status == SocketStatus.ThisEndClosed) {
                statusString = "ThisEndClosed";
            }

            print(">> SocketStatus: " + statusString);
            return status;
        }

        @SuppressWarnings("static-method")
        public long getError() {
            return 0L;
        }

        public Object getRemoteAddress() throws IOException {
            if (clientSocket != null) {
                final SocketAddress socketAddress = clientSocket.getRemoteSocketAddress();
                if (socketAddress instanceof InetSocketAddress) {
                    final InetSocketAddress inetAddress = (InetSocketAddress) socketAddress;
                    return inetAddress.getAddress().getAddress();
                } else {
                    throw new IOException("Could not retrieve remote address");
                }
            } else if (serverSocket != null) {
                return new byte[]{0, 0, 0, 0};
            } else if (datagramSocket != null) {
                final SocketAddress socketAddress = datagramSocket.getRemoteSocketAddress();
                if (socketAddress instanceof InetSocketAddress) {
                    final InetSocketAddress inetAddress = (InetSocketAddress) socketAddress;
                    return inetAddress.getAddress().getAddress();
                } else {
                    throw new IOException("Could not retrieve remote address");
                }
            } else {
                return 0L;
            }

        }

        public long getRemotePort() {
            if (clientSocket != null) {
                final InetSocketAddress address = (InetSocketAddress) clientSocket.getRemoteSocketAddress();
                if (address != null) {
                    return address.getPort();
                } else {
                    return 0L;
                }
            } else if (datagramSocket != null) {
                final InetSocketAddress address = (InetSocketAddress) datagramSocket.getRemoteSocketAddress();
                if (address != null) {
                    return address.getPort();
                } else {
                    return 0L;
                }
            } else {
                return 0L;
            }
        }

        public Object getLocalAddress() throws UnknownHostException {
            byte[] address;
            if (clientSocket != null) {
                address = clientSocket.getLocalAddress().getAddress();
            } else if (serverSocket != null) {
                final SocketAddress socketAddress = serverSocket.getLocalSocketAddress();
                if (socketAddress instanceof InetSocketAddress) {
                    address = ((InetSocketAddress) socketAddress).getAddress().getAddress();
                } else {
                    print(">> Socket local address: 0");
                    return 0;
                }
            } else if (datagramSocket != null) {
                address = datagramSocket.getLocalAddress().getAddress();
            } else {
                print(">> Socket local address: 0");
                return 0;
            }
            if (address[0] == 0 && address[1] == 0 && address[2] == 0 && address[3] == 0) {
                address = Resolver.getLocalAddress();
            }
            print(">> Socket local address: " + addressBytesToString(address));
            return address;

        }

        public Object getOption(final String option) {
            return options.get(option);
        }

        public void setOption(final String option, final Object value) {
            options.put(option, value);
        }

        public int getLocalPort() {
            int localPort = 0;
            if (clientSocket != null) {
                localPort = clientSocket.getLocalPort();
            } else if (serverSocket != null) {
                localPort = serverSocket.getLocalPort();
            } else if (datagramSocket != null) {
                localPort = datagramSocket.getLocalPort();
            }

            print(">> Local port: " + localPort);
            return localPort;
        }

        public boolean isSendDone() {
            print(">> Send Done: true");
            return true;
        }
    }

    public static String addressBytesToString(final byte[] address) throws UnknownHostException {
        return InetAddress.getByAddress(address).getHostAddress();
    }

    protected abstract static class AbstractSocketPluginPrimitiveNode extends AbstractPrimitiveNode {
        @Child protected NativeGetBytesNode getBytesNode = NativeGetBytesNode.create();

        protected AbstractSocketPluginPrimitiveNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        protected final void error(final Object o) {
            code.image.getError().println(o);
        }

        protected final void print(final Object o) {
            if (debugPrints) {
                code.image.getOutput().println(o);
            }
        }

        protected final String toString(final NativeObject object) {
            return getBytesNode.executeAsString(object);
        }

        protected final byte[] toByteArray(final NativeObject object) {
            return getBytesNode.execute(object);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveResolverStatus")
    protected abstract static class PrimResolverStatusNode extends AbstractSocketPluginPrimitiveNode {
        protected PrimResolverStatusNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected static final long doWork(@SuppressWarnings("unused") final Object receiver) {
            return ResolverStatus.Ready;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveInitializeNetwork")
    protected abstract static class PrimInitializeNetworkNode extends AbstractSocketPluginPrimitiveNode {
        protected PrimInitializeNetworkNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected static final Object doWork(final Object receiver) {
            return receiver;
        }
    }

    @TruffleBoundary
    private static SocketImpl getSocketImplOrPrimFail(final long socketHandle) {
        final SocketImpl socketImpl = sockets.get(socketHandle);
        if (socketImpl == null) {
            throw new PrimitiveFailed();
        }
        return socketImpl;
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveResolverStartNameLookup")
    protected abstract static class PrimResolverStartNameLookupNode extends AbstractSocketPluginPrimitiveNode {
        protected PrimResolverStartNameLookupNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        // Look up the given host name in the Domain Name Server to find its address. This call
        // is
        // asynchronous. To get the results, wait for it to complete or time out and then use
        // primNameLookupResult.
        @Specialization
        protected final Object doWork(final Object receiver, final NativeObject hostName) {
            print(">> Starting lookup for host name " + hostName);
            InetAddress address = null;
            final String hostNameString = toString(hostName);

            try {
                if (hostNameString.equals("localhost")) {
                    lastNameLookup = Resolver.getLocalAddress();
                    return receiver;
                }
                address = InetAddress.getByName(hostNameString);
                lastNameLookup = address.getAddress();

            } catch (UnknownHostException e) {
                error(e);
                lastNameLookup = null;
            }

            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveResolverStartAddressLookup")
    protected abstract static class PrimResolverStartAddressLookupNode extends AbstractSocketPluginPrimitiveNode {
        protected PrimResolverStartAddressLookupNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        // Look up the given host address in the Domain Name Server to find its name. This call
        // is
        // asynchronous. To get the results, wait for it to complete or time out and then use
        // primAddressLookupResult.
        @Specialization
        protected final Object doWork(final Object receiver, final NativeObject address) {
            print("Starting lookup for address " + address);
            final String addressString = toString(address);
            try {
                lastAddressLookup = InetAddress.getByName(addressString).getHostName();
            } catch (UnknownHostException e) {
                error(e);
                lastAddressLookup = null;
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveResolverNameLookupResult")
    protected abstract static class PrimResolverNameLookupResultNode extends AbstractSocketPluginPrimitiveNode {
        protected PrimResolverNameLookupResultNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        // Return the host address found by the last host name lookup. Returns nil if the last
        // lookup was unsuccessful.
        @Specialization
        protected final Object doWork(@SuppressWarnings("unused") final Object receiver) {
            if (lastNameLookup == null) {
                print(">> Name Lookup Result: " + null);
                return code.image.nil;
            } else {
                try {
                    print(">> Name Lookup Result: " + addressBytesToString(lastNameLookup));
                    return code.image.wrap(lastNameLookup);
                } catch (UnknownHostException e) {
                    error(e);
                }
                print(">> Name Lookup Result: " + null);
                return null;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveResolverAddressLookupResult")
    protected abstract static class PrimResolverAddressLookupResultNode extends AbstractSocketPluginPrimitiveNode {
        protected PrimResolverAddressLookupResultNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        // Return the host name found by the last host address lookup.
        // Returns nil if the last lookup was unsuccessful.
        @Specialization
        protected final Object doWork(@SuppressWarnings("unused") final Object receiver) {
            print(">> Address Lookup Result");
            if (lastAddressLookup == null) {
                return code.image.nil;
            } else {
                return code.image.wrap(lastAddressLookup);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveResolverLocalAddress")
    protected abstract static class PrimResolverLocalAddressNode extends AbstractSocketPluginPrimitiveNode {
        protected PrimResolverLocalAddressNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final Object doWork(@SuppressWarnings("unused") final Object receiver) {
            try {
                final byte[] address = Resolver.getLocalAddress();
                print(">> Local Address: " + addressBytesToString(address));
                return code.image.wrap(address);
            } catch (UnknownHostException e) {
                error(e);
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSocketLocalPort")
    protected abstract static class PrimSocketLocalPortNode extends AbstractSocketPluginPrimitiveNode {
        protected PrimSocketLocalPortNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        // Return the local port for this socket, or zero if no port has yet been assigned.
        @Specialization
        protected static final Long doWork(@SuppressWarnings("unused") final Object receiver, final long socketID) {
            return (long) getSocketImplOrPrimFail(socketID).getLocalPort();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSocketListenWithOrWithoutBacklog")
    protected abstract static class PrimSocketListenWithOrWithoutBacklogNode extends AbstractSocketPluginPrimitiveNode {
        protected PrimSocketListenWithOrWithoutBacklogNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        // Method 1:
        // Set the local port associated with a UDP socket.
        // Note: this primitive is overloaded. The primitive will not fail on a TCP socket, but
        // the effects will not be what was desired. Best solution would be to split Socket into
        // two subclasses, TCPSocket and UDPSocket.
        // Method 2:
        // Listen for a connection on the given port. This is an asynchronous call; query the
        // socket status to discover if and when the connection is actually completed.
        // Method 3:
        // Primitive. Set up the socket to listen on the given port.
        // Will be used in conjunction with #accept only.
        @Specialization
        protected final Object doWork(final Object receiver, final long socketID,
                        final long port,
                        @SuppressWarnings("unused") final NotProvided backlogSize) {
            try {
                getSocketImplOrPrimFail(socketID).listenOn((int) port);
            } catch (IOException e) {
                error(e);
                throw new PrimitiveFailed();
            }
            return receiver;
        }

        @Specialization
        protected final Object doWork(final Object receiver, final long socketID,
                        final long port,
                        @SuppressWarnings("unused") final Object backlogSize) {
            try {
                getSocketImplOrPrimFail(socketID).listenOn((int) port);
            } catch (IOException e) {
                error(e);
                throw new PrimitiveFailed();
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSocketListenOnPortBacklogInterface")
    protected abstract static class PrimSocketListenOnPortBacklogInterfaceNode extends AbstractSocketPluginPrimitiveNode {
        protected PrimSocketListenOnPortBacklogInterfaceNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        // Primitive. Set up the socket to listen on the given port.
        // Will be used in conjunction with #accept only.
        @SuppressWarnings("unused")
        @Specialization
        protected final Object doWork(final Object receiver,
                        final long socketID,
                        final long port,
                        final Object backlogSize,
                        final Object interfaceAddress) {
            try {
                getSocketImplOrPrimFail(socketID).listenOn((int) port);
            } catch (IOException e) {
                error(e);
                throw new PrimitiveFailed();
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSocketSetOptions")
    protected abstract static class PrimSocketSetOptionsNode extends AbstractSocketPluginPrimitiveNode {
        protected PrimSocketSetOptionsNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final Object doWork(final Object receiver, final long socketID, final NativeObject option, final NativeObject value) {
            final SocketImpl socketImpl = getSocketImplOrPrimFail(socketID);
            socketImpl.setOption(toString(option), value);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSocketConnectToPort")
    protected abstract static class PrimSocketConnectToPortNode extends AbstractSocketPluginPrimitiveNode {
        protected PrimSocketConnectToPortNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = "hostAddress.isByteType()")
        protected final long doWork(@SuppressWarnings("unused") final Object receiver, final long socketID, final NativeObject hostAddress, final long port) {
            final SocketImpl socketImpl = getSocketImplOrPrimFail(socketID);

            try {
                final byte[] bytes = toByteArray(hostAddress);
                final String hostAddressString = addressBytesToString(bytes);
                socketImpl.connectTo(hostAddressString, (int) port);
            } catch (IOException e) {
                error(e);
                throw new PrimitiveFailed();
            }
            return 0;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSocketConnectionStatus")
    protected abstract static class PrimSocketConnectionStatusNode extends AbstractSocketPluginPrimitiveNode {
        protected PrimSocketConnectionStatusNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        @TruffleBoundary
        protected final long doWork(@SuppressWarnings("unused") final PointersObject receiver, final long socketID) {
            if (!sockets.containsKey(socketID)) {
                return SocketStatus.Unconnected;
            }
            final SocketImpl socketImpl = getSocketImplOrPrimFail(socketID);
            try {
                return socketImpl.getStatus();
            } catch (IOException e) {
                error(e);
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSocketRemoteAddress")
    protected abstract static class PrimSocketRemoteAddressNode extends AbstractSocketPluginPrimitiveNode {
        protected PrimSocketRemoteAddressNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final Object doWork(@SuppressWarnings("unused") final Object receiver, final long socketID) {
            try {
                return code.image.wrap(getSocketImplOrPrimFail(socketID).getRemoteAddress());
            } catch (IOException e) {
                error(e);
                return 0;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSocketRemotePort")
    protected abstract static class PrimSocketRemotePortNode extends AbstractSocketPluginPrimitiveNode {
        protected PrimSocketRemotePortNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected static final Object doWork(@SuppressWarnings("unused") final Object receiver, final long socketID) {
            final SocketImpl socketImpl = getSocketImplOrPrimFail(socketID);
            return socketImpl.getRemotePort();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSocketGetOptions")
    protected abstract static class PrimSocketGetOptionsNode extends AbstractSocketPluginPrimitiveNode {
        protected PrimSocketGetOptionsNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        // Get some option information on this socket. Refer to the UNIX
        // man pages for valid SO, TCP, IP, UDP options. In case of doubt
        // refer to the source code.
        // TCP_NODELAY, SO_KEEPALIVE are valid options for example
        // returns an array containing the error code and the option value

        // options := {
        // 'SO_DEBUG'. 'SO_REUSEADDR'. 'SO_REUSEPORT'. 'SO_DONTROUTE'.
        // 'SO_BROADCAST'. 'SO_SNDBUF'. 'SO_RCVBUF'. 'SO_KEEPALIVE'.
        // 'SO_OOBINLINE'. 'SO_PRIORITY'. 'SO_LINGER'. 'SO_RCVLOWAT'.
        // 'SO_SNDLOWAT'. 'IP_TTL'. 'IP_HDRINCL'. 'IP_RCVOPTS'.
        // 'IP_RCVDSTADDR'. 'IP_MULTICAST_IF'. 'IP_MULTICAST_TTL'.
        // 'IP_MULTICAST_LOOP'. 'UDP_CHECKSUM'. 'TCP_MAXSEG'.
        // 'TCP_NODELAY'. 'TCP_ABORT_THRESHOLD'. 'TCP_CONN_NOTIFY_THRESHOLD'.
        // 'TCP_CONN_ABORT_THRESHOLD'. 'TCP_NOTIFY_THRESHOLD'.
        // 'TCP_URGENT_PTR_TYPE'}.
        @Specialization
        protected final Object doWork(@SuppressWarnings("unused") final Object receiver, final long socketID, final NativeObject option) {
            final SocketImpl socketImpl = getSocketImplOrPrimFail(socketID);
            final Object value = socketImpl.getOption(toString(option));
            final Long errorCode = socketImpl.getError();
            final Object[] result = new Object[]{errorCode, value};
            return code.image.wrap(result);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSocketReceiveDataAvailable")
    protected abstract static class PrimSocketReceiveDataAvailableNode extends AbstractSocketPluginPrimitiveNode {
        protected PrimSocketReceiveDataAvailableNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final boolean doWork(@SuppressWarnings("unused") final Object receiver, final long socketID) {
            final SocketImpl socketImpl = getSocketImplOrPrimFail(socketID);
            try {
                return socketImpl.isDataAvailable();
            } catch (IOException e) {
                error(e);
                return code.image.sqFalse;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSocketError")
    protected abstract static class PrimSocketErrorNode extends AbstractSocketPluginPrimitiveNode {
        protected PrimSocketErrorNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected static final long doWork(@SuppressWarnings("unused") final Object receiver, final long socketID) {
            final SocketImpl socketImpl = getSocketImplOrPrimFail(socketID);
            return socketImpl.getError();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSocketLocalAddress")
    protected abstract static class PrimSocketLocalAddressNode extends AbstractSocketPluginPrimitiveNode {
        protected PrimSocketLocalAddressNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final Object doWork(@SuppressWarnings("unused") final Object receiver, final long socketID) {
            final SocketImpl socketImpl = getSocketImplOrPrimFail(socketID);
            try {
                final Object result = socketImpl.getLocalAddress();
                if (result instanceof byte[]) {
                    return code.image.wrap((byte[]) result);
                } else {
                    return (long) result;
                }

            } catch (UnknownHostException e) {
                error(e);
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSocketSendDataBufCount")
    protected abstract static class PrimSocketSendDataBufCountNode extends AbstractSocketPluginPrimitiveNode {
        protected PrimSocketSendDataBufCountNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        // Send data to the remote host through the given socket starting with the given byte
        // index
        // of the given byte array. The data sent is 'pushed' immediately. Return the number of
        // bytes of data actually sent; any remaining data should be re-submitted for sending
        // after
        // the current send operation has completed.
        // Note: In general, it many take several sendData calls to transmit a large data array
        // since the data is sent in send-buffer-sized chunks. The size of the send buffer is
        // determined when the socket is created.
        @Specialization
        protected final long doWork(@SuppressWarnings("unused") final Object receiver,
                        final long socketID,
                        final NativeObject aStringOrByteArray,
                        final long startIndex,
                        final long count) {
            final SocketImpl socketImpl = getSocketImplOrPrimFail(socketID);
            try {
                final byte[] data = toByteArray(aStringOrByteArray);
                socketImpl.sendData(data, (int) (startIndex - 1), (int) count);
                return count;
            } catch (Exception e) {
                error(e);
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSocketCloseConnection")
    protected abstract static class PrimSocketCloseConnectionNode extends AbstractSocketPluginPrimitiveNode {
        protected PrimSocketCloseConnectionNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final Object doWork(final Object receiver, final long socketID) {
            try {
                getSocketImplOrPrimFail(socketID).close();
            } catch (IOException e) {
                error(e);
                throw new PrimitiveFailed();
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSocketAbortConnection")
    protected abstract static class PrimSocketAbortConnectionNode extends AbstractSocketPluginPrimitiveNode {
        protected PrimSocketAbortConnectionNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final Object doWork(final Object receiver, final long socketID) {
            try {
                getSocketImplOrPrimFail(socketID).close();
            } catch (IOException e) {
                error(e);
                throw new PrimitiveFailed();
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSocketSendDone")
    protected abstract static class PrimSocketSendDoneNode extends AbstractSocketPluginPrimitiveNode {
        protected PrimSocketSendDoneNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final Object doWork(@SuppressWarnings("unused") final Object receiver, final long socketID) {
            return code.image.wrap(getSocketImplOrPrimFail(socketID).isSendDone());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSocketReceiveDataBufCount")
    protected abstract static class PrimSocketReceiveDataBufCountNode extends AbstractSocketPluginPrimitiveNode {
        protected PrimSocketReceiveDataBufCountNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        // Receive data from the given socket into the given array starting at the given index.
        // Return the number of bytes read or zero if no data is available.
        @Specialization
        protected final long doWork(@SuppressWarnings("unused") final Object receiver, final long socketID, final NativeObject receiveBuffer, final long startIndex, final long count) {
            final SocketImpl socketImpl = getSocketImplOrPrimFail(socketID);
            final byte[] buffer = toByteArray(receiveBuffer);
            final long readBytes;
            try {
                readBytes = socketImpl.receiveData(buffer, (int) (startIndex - 1), (int) count);
            } catch (IOException e) {
                error(e);
                throw new PrimitiveFailed();
            }
            return readBytes;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSocketDestroy")
    protected abstract static class PrimSocketDestroyNode extends AbstractSocketPluginPrimitiveNode {
        protected PrimSocketDestroyNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        @TruffleBoundary
        protected final long doWork(@SuppressWarnings("unused") final Object receiver, final long socketID) {
            final SocketImpl socketImpl = getSocketImplOrPrimFail(socketID);
            try {
                if (socketImpl != null) {
                    socketImpl.close();
                    sockets.removeKey(socketID);
                }
            } catch (IOException e) {
                error(e);
                throw new PrimitiveFailed();
            }

            return 0;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSocketCreate3Semaphores")
    protected abstract static class PrimSocketCreate3SemaphoresNode extends AbstractSocketPluginPrimitiveNode {
        protected PrimSocketCreate3SemaphoresNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @SuppressWarnings("unused")
        @TruffleBoundary
        @Specialization
        protected final long doWork(final PointersObject receiver,
                        final long netType,
                        final long socketType,
                        final long rcvBufSize,
                        final long semaIndex,
                        final long aReadSema,
                        final long aWriteSema) {
            final SocketImpl s = new SocketImpl(code, socketType);
            sockets.put((long) s.hashCode(), s);
            return s.hashCode();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSocketAccept3Semaphores")
    protected abstract static class PrimSocketAccept3SemaphoresNode extends AbstractSocketPluginPrimitiveNode {
        protected PrimSocketAccept3SemaphoresNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @SuppressWarnings("unused")
        @TruffleBoundary
        @Specialization
        protected final long doWork(final PointersObject receiver,
                        final long socketID,
                        final Object receiveBufferSize,
                        final Object sendBufSize,
                        final Object semaIndex,
                        final Object readSemaIndex,
                        final Object writeSemaIndex) {
            try {
                final SocketImpl socketImpl = getSocketImplOrPrimFail(socketID);
                final SocketImpl s = socketImpl.accept();
                sockets.put((long) s.hashCode(), s);
                return s.hashCode();
            } catch (IOException e) {
                error(e);
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSocketCreate")
    protected abstract static class PrimSocketCreateNode extends AbstractSocketPluginPrimitiveNode {
        protected PrimSocketCreateNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected final long doWork(final PointersObject receiver,
                        final Object netType,
                        final Object socketType,
                        final Object rcvBufSize,
                        final Object sendBufSize,
                        final Object semaIndexa) {
            error("TODO: primitiveSocketCreate");
            throw new PrimitiveFailed();
        }

    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return SocketPluginFactory.getFactories();
    }
}
