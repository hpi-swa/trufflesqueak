package de.hpi.swa.graal.squeak.nodes.plugins;

import java.util.LinkedList;
import java.util.List;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.TreeMap;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NotProvided;
import de.hpi.swa.graal.squeak.model.PointersObject;
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

    static Map<Long, SocketImpl> sockets = new TreeMap<>();

    private static final class Resolver {
        @SuppressWarnings("unused")
        public static byte[] getLocalAddress() throws UnknownHostException {
            // return InetAddress.getLocalHost().getAddress();
            return new byte[]{127, 0, 0, 1};
        }

        public static InetAddress getLocalHostInetAddress() throws IOException {
            return InetAddress.getByAddress(Resolver.getLocalAddress());
        }
    }

    private static final class SocketImpl {
        private CompiledCodeObject code;
        private int id;

        private long socketType;
        private Socket clientSocket;
        private ServerSocket serverSocket;
        private DatagramSocket datagramSocket;
        private Socket acceptedConnection;

        private Map<String, Object> options = new TreeMap<>();
        boolean listening = false;

        private LinkedList<String> readBuffer = new LinkedList<String>();

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
            code.image.getOutput().println(id + ": " + message.toString());
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
                // TODO
            } else {
                serverSocket = new ServerSocket(port, 1, Resolver.getLocalHostInetAddress());
                print(">> Actually listening on " + Resolver.getLocalHostInetAddress() + ":" + serverSocket.getLocalPort());
                final SocketImpl self = this;
                final Thread listenerThread = new Thread() {
                    @Override
                    public void run() {
                        listening = true;
                        try {
                            while (true) {
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
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                }
            }
        }

        public SocketImpl accept() {
            print(">> Accepting");
            final SocketImpl connectionImpl = new SocketImpl(code, SocketType.TCPSocketType);
            if (acceptedConnection == null) {
                error("No connection was accepted");
                throw new PrimitiveFailed();
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
                clientSocket = new Socket(host, (int) port, Resolver.getLocalHostInetAddress(), SocketImpl.getFreePort());
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
            print(">> Receive data");
            if (clientSocket != null) {
                if (isDataAvailable()) {
                    return clientSocket.getInputStream().read(data, startIndex, count);
                } else {
                    print(">> No data available");
                    return 0;
                }
            } else if (datagramSocket != null) {
                final DatagramPacket p = new DatagramPacket(data, startIndex, count);
                datagramSocket.receive(p);
                return p.getLength();
            } else {
                error(">> Socket not connected!");
                throw new PrimitiveFailed();
            }
        }

        public void sendData(final byte[] data, final int startIndex, final int count) throws IOException {
            print(">> Send Data");
            if (clientSocket != null) {
                if (!clientSocket.isConnected()) {
                    error("Client socket is not connected!");
                    throw new PrimitiveFailed();
                }
                final OutputStream outputStream = clientSocket.getOutputStream();
                outputStream.write(data, startIndex, count);
                outputStream.flush();
            } else if (datagramSocket != null) {
                if (!datagramSocket.isConnected()) {
                    error("Datagram socket is not connected!");
                    throw new PrimitiveFailed();
                }
                print("Send to " + datagramSocket.getRemoteSocketAddress());
                final DatagramPacket p = new DatagramPacket(data, startIndex, count);
                this.datagramSocket.send(p);
            } else {
                error("Not Connected!");
                throw new PrimitiveFailed();
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

        public long getStatus() {
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
                    error(">> Undefined Socket Status");
                    throw new PrimitiveFailed();
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
                        clientSocket.getInputStream().available();
                        status = SocketStatus.Connected;
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
                return 0;
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
            // print(">> Send Done: " + !sending);
            // return !sending;
            print(">> Send Done: true");
            return true;
        }

        public static int getFreePort() throws IOException {
            try (ServerSocket socket = new ServerSocket(0);) {
                return socket.getLocalPort();
            }
        }

    }

    public static String addressBytesToString(final byte[] address) {
        String hostAddressString = "";
        for (int i = 0; i < address.length - 1; i++) {
            hostAddressString += Byte.toString(address[i]) + ".";
        }
        hostAddressString += Byte.toString(address[address.length - 1]);
        return hostAddressString;
    }

// NetNameResolver
    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveResolverStatus")
    protected abstract static class PrimResolverStatusNode extends AbstractPrimitiveNode {
        protected PrimResolverStatusNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected long doWork(@SuppressWarnings("unused") final Object receiver) {
            return ResolverStatus.Ready;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveInitializeNetwork")
    protected abstract static class PrimInitializeNetworkNode extends AbstractPrimitiveNode {
        protected PrimInitializeNetworkNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected Object doWork(final Object receiver) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveResolverStartNameLookup")
    protected abstract static class PrimResolverStartNameLookupNode extends AbstractPrimitiveNode {
        protected PrimResolverStartNameLookupNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        // Look up the given host name in the Domain Name Server to find its address. This call
        // is
        // asynchronous. To get the results, wait for it to complete or time out and then use
        // primNameLookupResult.
        @Specialization
        protected Object doWork(final Object receiver, final NativeObject hostName) {
            code.image.getOutput().println(">> Starting lookup for host name " + hostName);
            InetAddress address = null;
            final String hostNameString = hostName.toString();
            try {
                if (hostNameString.equals("localhost")) {
                    lastNameLookup = Resolver.getLocalAddress();
                    return receiver;
                }
                address = InetAddress.getByName(new URL(hostNameString).getHost());
                lastNameLookup = address.getAddress();

            } catch (UnknownHostException e) {
                code.image.getError().println(e);
                lastNameLookup = null;
            } catch (MalformedURLException e) {
                code.image.getError().println(e);
                lastNameLookup = null;
            }

            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveResolverStartAddressLookup")
    protected abstract static class PrimResolverStartAddressLookupNode extends AbstractPrimitiveNode {
        protected PrimResolverStartAddressLookupNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        // Look up the given host address in the Domain Name Server to find its name. This call
        // is
        // asynchronous. To get the results, wait for it to complete or time out and then use
        // primAddressLookupResult.
        @Specialization
        protected Object doWork(final Object receiver, final Object address) {
            code.image.getOutput().println("Starting lookup for address " + address);
            final String addressString = address.toString();
            try {
                lastAddressLookup = InetAddress.getByName(addressString).getHostName();
            } catch (UnknownHostException e) {
                code.image.getError().println(e);
                lastAddressLookup = null;
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveResolverNameLookupResult")
    protected abstract static class PrimResolverNameLookupResultNode extends AbstractPrimitiveNode {
        protected PrimResolverNameLookupResultNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        // Return the host address found by the last host name lookup. Returns nil if the last
        // lookup was unsuccessful.
        @Specialization
        protected Object doWork(@SuppressWarnings("unused") final Object receiver) {
            code.image.getOutput().println(">> Name Lookup Result: " + addressBytesToString(lastNameLookup));
            if (lastNameLookup == null) {
                return code.image.nil;
            } else {
                return code.image.wrap(lastNameLookup);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveResolverAddressLookupResult")
    protected abstract static class PrimResolverAddressLookupResultNode extends AbstractPrimitiveNode {
        protected PrimResolverAddressLookupResultNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        // Return the host name found by the last host address lookup.
        // Returns nil if the last lookup was unsuccessful.
        @Specialization
        protected Object doWork(@SuppressWarnings("unused") final Object receiver) {
            code.image.getOutput().println(">> Address Lookup Result");
            if (lastAddressLookup == null) {
                return code.image.nil;
            } else {
                return code.image.wrap(lastAddressLookup);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveResolverLocalAddress")
    protected abstract static class PrimResolverLocalAddressNode extends AbstractPrimitiveNode {
        protected PrimResolverLocalAddressNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected Object doWork(@SuppressWarnings("unused") final Object receiver) {
            try {
                final byte[] address = Resolver.getLocalAddress();
                code.image.getOutput().println(">> Local Address: " + addressBytesToString(address));
                return code.image.wrap(address);
            } catch (UnknownHostException e) {
                code.image.getError().println(e);
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSocketLocalPort")
    protected abstract static class PrimSocketLocalPortNode extends AbstractPrimitiveNode {
        protected PrimSocketLocalPortNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        // Return the local port for this socket, or zero if no port has yet been assigned.
        @Specialization
        protected Long doWork(@SuppressWarnings("unused") final Object receiver, final long socketID) {
            final SocketImpl socketImpl = sockets.get(socketID);
            if (socketImpl == null) {
                code.image.getError().println("No socket for socket id");
                throw new PrimitiveFailed();
            }
            return (long) socketImpl.getLocalPort();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSocketListenWithOrWithoutBacklog")
    protected abstract static class PrimSocketListenWithOrWithoutBacklogNode extends AbstractPrimitiveNode {
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
        protected Object doWork(final Object receiver, final long socketID,
                        final long port,
                        @SuppressWarnings("unused") final NotProvided backlogSize) {
            final SocketImpl socketImpl = sockets.get(socketID);
            if (socketImpl == null) {
                code.image.getError().println("No socket for socket id");
                throw new PrimitiveFailed();
            }

            try {
                socketImpl.listenOn((int) port);
            } catch (IOException e) {
                code.image.getError().println(e);
                throw new PrimitiveFailed();
            }
            return receiver;
        }

        @Specialization
        protected Object doWork(final Object receiver, final long socketID,
                        final long port,
                        @SuppressWarnings("unused") final Object backlogSize) {
            final SocketImpl socketImpl = sockets.get(socketID);
            if (socketImpl == null) {
                code.image.getError().println("No socket for socket id");
                throw new PrimitiveFailed();
            }

            try {
                socketImpl.listenOn((int) port);
            } catch (IOException e) {
                code.image.getError().println(e);
                throw new PrimitiveFailed();
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSocketListenOnPortBacklogInterface")
    protected abstract static class PrimSocketListenOnPortBacklogInterfaceNode extends AbstractPrimitiveNode {
        protected PrimSocketListenOnPortBacklogInterfaceNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        // Primitive. Set up the socket to listen on the given port.
        // Will be used in conjunction with #accept only.
        @SuppressWarnings("unused")
        @Specialization
        protected Object doWork(final Object receiver,
                        final long socketID,
                        final long port,
                        final Object backlogSize,
                        final Object interfaceAddress) {
            final SocketImpl socketImpl = sockets.get(socketID);
            if (socketImpl == null) {
                code.image.getError().println("No socket for socket id");
                throw new PrimitiveFailed();
            }

            try {
                socketImpl.listenOn((int) port);
            } catch (IOException e) {
                code.image.getError().println(e);
                throw new PrimitiveFailed();
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSocketSetOptions")
    protected abstract static class PrimSocketSetOptionsNode extends AbstractPrimitiveNode {
        protected PrimSocketSetOptionsNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected Object doWork(final Object receiver, final long socketID, final NativeObject option, final NativeObject value) {
            final SocketImpl socketImpl = sockets.get(socketID);
            if (socketImpl == null) {
                code.image.getError().println("No socket for socket id");
                throw new PrimitiveFailed();
            }
            socketImpl.setOption(option.toString(), value.toString());
            return receiver;
        }
    }

// Socket
    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSocketConnectToPort")
    protected abstract static class PrimSocketConnectToPortNode extends AbstractPrimitiveNode {
        protected PrimSocketConnectToPortNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected long doWork(@SuppressWarnings("unused") final Object receiver, final long socketID, final NativeObject hostAddress, final long port) {
            final SocketImpl socketImpl = sockets.get(socketID);
            if (socketImpl == null) {
                code.image.getError().println("No socket for socket id");
                throw new PrimitiveFailed();
            }
            final String hostAddressString = addressBytesToString(hostAddress.asString().getBytes());
            try {
                socketImpl.connectTo(hostAddressString, (int) port);
            } catch (IOException e) {
                code.image.getError().println(e);
                throw new PrimitiveFailed();
            }
            return 0;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSocketConnectionStatus")
    protected abstract static class PrimSocketConnectionStatusNode extends AbstractPrimitiveNode {
        protected PrimSocketConnectionStatusNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected long doWork(@SuppressWarnings("unused") final PointersObject receiver, final long socketID) {
            if (!sockets.containsKey(socketID)) {
                return SocketStatus.Unconnected;
            }

            final SocketImpl socketImpl = sockets.get(socketID);
            return socketImpl.getStatus();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSocketRemoteAddress")
    protected abstract static class PrimSocketRemoteAddressNode extends AbstractPrimitiveNode {
        protected PrimSocketRemoteAddressNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected Object doWork(@SuppressWarnings("unused") final Object receiver, final long socketID) {
            if (!sockets.containsKey(socketID)) {
                return 0;
            }

            final SocketImpl socket = sockets.get(socketID);
            try {
                final Object result = socket.getRemoteAddress();
                if (result instanceof byte[]) {
                    return code.image.wrap((byte[]) result);
                } else {
                    return (long) result;
                }

            } catch (IOException e) {
                code.image.getError().println(e);
                return 0;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSocketRemotePort")
    protected abstract static class PrimSocketRemotePortNode extends AbstractPrimitiveNode {
        protected PrimSocketRemotePortNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected Object doWork(@SuppressWarnings("unused") final Object receiver, final long socketID) {
            if (!sockets.containsKey(socketID)) {
                return 0;
            }

            final SocketImpl socketImpl = sockets.get(socketID);
            return socketImpl.getRemotePort();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSocketGetOptions")
    protected abstract static class PrimSocketGetOptionsNode extends AbstractPrimitiveNode {
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
        protected Object doWork(@SuppressWarnings("unused") final Object receiver, final long socketID, final Object option) {
            final SocketImpl socketImpl = sockets.get(socketID);
            if (socketImpl == null) {
                code.image.getError().println("No socket for socket id");
                throw new PrimitiveFailed();
            }
            final Object value = socketImpl.getOption(option.toString());
            final Long errorCode = socketImpl.getError();
            final Object[] result = new Object[]{errorCode, value};
            return code.image.wrap(result);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSocketReceiveDataAvailable")
    protected abstract static class PrimSocketReceiveDataAvailableNode extends AbstractPrimitiveNode {
        protected PrimSocketReceiveDataAvailableNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected boolean doWork(@SuppressWarnings("unused") final Object receiver, final long socketID) {
            final SocketImpl socketImpl = sockets.get(socketID);
            try {
                return socketImpl.isDataAvailable();
            } catch (IOException e) {
                code.image.getError().println(e);
                return false;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSocketError")
    protected abstract static class PrimSocketErrorNode extends AbstractPrimitiveNode {
        protected PrimSocketErrorNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected long doWork(@SuppressWarnings("unused") final Object receiver, final long socketID) {
            final SocketImpl socketImpl = sockets.get(socketID);
            if (socketImpl == null) {
                code.image.getError().println("No socket for socket id");
                throw new PrimitiveFailed();
            }

            return socketImpl.getError();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSocketLocalAddress")
    protected abstract static class PrimSocketLocalAddressNode extends AbstractPrimitiveNode {
        protected PrimSocketLocalAddressNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected Object doWork(@SuppressWarnings("unused") final Object receiver, final long socketID) {
            final SocketImpl socketImpl = sockets.get(socketID);
            if (socketImpl == null) {
                code.image.getError().println("No socket for socket id");
                throw new PrimitiveFailed();
            }

            try {
                final Object result = socketImpl.getLocalAddress();
                if (result instanceof byte[]) {
                    return code.image.wrap((byte[]) result);
                } else {
                    return (long) result;
                }

            } catch (UnknownHostException e) {
                code.image.getError().println(e);
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSocketSendDataBufCount")
    protected abstract static class PrimSocketSendDataBufCountNode extends AbstractPrimitiveNode {
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
        protected long doWork(@SuppressWarnings("unused") final Object receiver,
                        final long socketID,
                        final NativeObject aStringOrByteArray,
                        final long startIndex,
                        final long count) {
            final SocketImpl socketImpl = sockets.get(socketID);
            if (socketImpl == null) {
                code.image.getError().println("No socket for socket id");
                throw new PrimitiveFailed();
            }
            try {
                final byte[] data = aStringOrByteArray.toString().getBytes();
                socketImpl.sendData(data, (int) (startIndex - 1), (int) count);
                return count;
            } catch (Exception e) {
                code.image.getError().println(e);
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSocketCloseConnection")
    protected abstract static class PrimSocketCloseConnectionNode extends AbstractPrimitiveNode {
        protected PrimSocketCloseConnectionNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected Object doWork(final Object receiver, final long socketID) {
            final SocketImpl socketImpl = sockets.get(socketID);
            if (socketImpl == null) {
                code.image.getError().println("No socket for socket id");
                throw new PrimitiveFailed();
            }
            try {
                socketImpl.close();
            } catch (IOException e) {
                code.image.getError().println(e);
                throw new PrimitiveFailed();
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSocketSendDone")
    protected abstract static class PrimSocketSendDoneNode extends AbstractPrimitiveNode {
        protected PrimSocketSendDoneNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected Object doWork(@SuppressWarnings("unused") final Object receiver, final long socketID) {
            final SocketImpl socketImpl = sockets.get(socketID);
            if (socketImpl == null) {
                code.image.getError().println("No socket for socket id");
                throw new PrimitiveFailed();
            }
            return code.image.wrap(socketImpl.isSendDone());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSocketReceiveDataBufCount")
    protected abstract static class PrimSocketReceiveDataBufCountNode extends AbstractPrimitiveNode {
        protected PrimSocketReceiveDataBufCountNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        // Receive data from the given socket into the given array starting at the given index.
        // Return the number of bytes read or zero if no data is available.
        @Specialization
        protected long doWork(@SuppressWarnings("unused") final Object receiver, final long socketID, final NativeObject receiveBuffer, final long startIndex, final long count) {
            final SocketImpl socketImpl = sockets.get(socketID);
            if (socketImpl == null) {
                code.image.getError().println("No socket for socket id");
                throw new PrimitiveFailed();
            }
            final byte[] buffer = new byte[(int) count];
            final long readBytes;
            try {
                readBytes = socketImpl.receiveData(buffer, (int) (startIndex - 1), (int) count);
                receiveBuffer.setStorageForTesting(buffer);
            } catch (IOException e) {
                code.image.getError().println(e);
                throw new PrimitiveFailed();
            }
            code.image.getOutput().println(receiveBuffer);
            return readBytes;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSocketDestroy")
    protected abstract static class PrimSocketDestroyNode extends AbstractPrimitiveNode {
        protected PrimSocketDestroyNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected long doWork(@SuppressWarnings("unused") final Object receiver, final long socketID) {
            final SocketImpl socketImpl = sockets.get(socketID);
            if (socketImpl == null) {
                code.image.getError().println("No socket for socket id");
                throw new PrimitiveFailed();
            }
            try {
                if (socketImpl != null) {
                    socketImpl.close();
                    sockets.remove(socketID);
                }
            } catch (IOException e) {
                code.image.getError().println(e);
                throw new PrimitiveFailed();
            }

            return 0;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSocketCreate3Semaphores")
    protected abstract static class PrimSocketCreate3SemaphoresNode extends AbstractPrimitiveNode {
        protected PrimSocketCreate3SemaphoresNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected long doWork(final PointersObject receiver,
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
    protected abstract static class PrimSocketAccept3SemaphoresNode extends AbstractPrimitiveNode {
        protected PrimSocketAccept3SemaphoresNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected long doWork(final PointersObject receiver,
                        final long socketID,
                        final Object receiveBufferSize,
                        final Object sendBufSize,
                        final Object semaIndex,
                        final Object readSemaIndex,
                        final Object writeSemaIndex) {
            final SocketImpl socketImpl = sockets.get(socketID);
            if (socketImpl == null) {
                code.image.getError().println("No socket for socket id");
                throw new PrimitiveFailed();
            }
            final SocketImpl s = socketImpl.accept();
            sockets.put((long) s.hashCode(), s);
            return s.hashCode();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSocketCreate")
    protected abstract static class PrimSocketCreateNode extends AbstractPrimitiveNode {
        protected PrimSocketCreateNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected long doWork(final PointersObject receiver,
                        final Object netType,
                        final Object socketType,
                        final Object rcvBufSize,
                        final Object sendBufSize,
                        final Object semaIndexa) {
            code.image.getError().println("TODO: primitiveSocketCreate");
            throw new PrimitiveFailed();
        }

    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return SocketPluginFactory.getFactories();
    }
}
