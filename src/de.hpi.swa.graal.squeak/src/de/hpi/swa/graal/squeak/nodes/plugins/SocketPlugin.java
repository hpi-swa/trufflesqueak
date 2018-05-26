package de.hpi.swa.graal.squeak.nodes.plugins;

import java.util.List;
import java.io.IOException;
import java.io.OutputStream;
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
import java.util.HashMap;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NotProvided;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;

public final class SocketPlugin extends AbstractPrimitiveFactoryHolder {

    @SuppressWarnings("unused")
    private static final class Resolver {
        private static final long Uninitialized = 0;
        private static final long Ready = 1;
        private static final long Busy = 2;
        private static final long Error = 3;
    }

    private static String lastNameLookup;
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

    private final static class SocketImpl {

        private Socket clientSocket;
        private ServerSocket serverSocket;
        private Socket acceptedConnection;
        private Map<String, Object> options = new TreeMap<>();
        boolean listening = false;
        Thread listenerThread;

        public void listenOn(final int port) {
            if (listening) {
                return;
            }
            System.out.println(">> Listening on " + port);
            listenerThread = new Thread() {
                @Override
                public void run() {
                    try {
                        listening = true;
                        serverSocket = new ServerSocket(port);
                        acceptedConnection = serverSocket.accept();
                        serverSocket.close();
                        serverSocket = null;
                        listening = false;
                    } catch (SocketException se) {
                        // The socket has been closed while listening
                        // This is fine
                        listening = false;
                    } catch (IOException e) {
                        System.err.println(e);
                        e.printStackTrace();
                    }
                }
            };
            listenerThread.start();
        }

        public SocketImpl accept() {
            System.out.println(">> Accepting");
            try {
                if (serverSocket == null) {
                    System.err.println("Not a ServerSocket");
                    throw new PrimitiveFailed();
                }

                listenerThread.join();
                SocketImpl connectionImpl = new SocketImpl();
                if (acceptedConnection == null) {
                    System.err.println("No connection was accepted");
                    throw new PrimitiveFailed();
                }

                connectionImpl.clientSocket = acceptedConnection;
                return connectionImpl;
            } catch (InterruptedException e) {
                System.err.print(e);
                throw new PrimitiveFailed();
            }

        }

        public void connectTo(final String host, final long port) throws IOException {
            System.out.println(">> Connecting to " + host + ": " + port);
            if (clientSocket != null) {
                clientSocket.close();
            }
            clientSocket = new Socket();
            final SocketAddress endpoint = new InetSocketAddress(host, (int) port);
            clientSocket.connect(endpoint);
        }

        public boolean isDataAvailable() throws IOException {
            return clientSocket.getInputStream().available() > 0;
        }

        public void sendData(final byte[] data, final int startIndex, final int count) throws IOException {
            final OutputStream outputStream = clientSocket.getOutputStream();
            outputStream.write(data, startIndex, count);
        }

        public void close() throws IOException {
            System.out.println(">> Closing");
            if (clientSocket != null) {
                clientSocket.close();
                clientSocket = null;
            }
            if (serverSocket != null) {
                serverSocket.close();
                serverSocket = null;
            }
        }

        public long getStatus() {
            if (clientSocket == null) {
                return SocketStatus.Unconnected;
            }

            if (clientSocket.isInputShutdown()) {
                return SocketStatus.ThisEndClosed;
            } else if (clientSocket.isOutputShutdown()) {
                return SocketStatus.OtherEndClosed;
            } else if (!clientSocket.isConnected()) {
                return SocketStatus.Unconnected;
            } else {
                return SocketStatus.Connected;
            }
        }

        public long getError() {
            return 0L;
        }

        public Object getRemoteAddress() throws IOException {
            if (clientSocket == null) {
                return 0L;
            }

            final SocketAddress socketAddress = clientSocket.getRemoteSocketAddress();
            if (socketAddress instanceof InetSocketAddress) {
                final InetSocketAddress inetAddress = (InetSocketAddress) socketAddress;
                return inetAddress.getAddress().getHostAddress();
            } else {
                throw new IOException("Could not retrieve remote address");
            }
        }

        public Object getOption(final String option) {
            return options.get(option);
        }

        public void setOption(final String option, final Object value) {
            options.put(option, value);
        }
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
            return Resolver.Ready;
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
                    lastNameLookup = InetAddress.getLocalHost().getHostAddress();
                    return receiver;
                }
                address = InetAddress.getByName(new URL(hostNameString).getHost());
                lastNameLookup = address.getHostAddress();
            } catch (UnknownHostException e) {
                System.err.println(e);
                lastNameLookup = null;
            } catch (MalformedURLException e) {
                System.err.println(e);
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
            code.image.getOutput().println(">> Name Lookup Result: " + lastNameLookup);
            if (lastNameLookup == null) {
                return code.image.nil;
            } else {
                String result = "";
                byte[] address = new byte[4];
                int i = 0;
                for (String byteString : lastNameLookup.split("\\.")) {
                    byte b = Byte.parseByte(byteString);
                    address[i] = b;
                    i++;
                    result += (char) b;
                }
                return code.image.wrap(result);
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
                return code.image.wrap(InetAddress.getLocalHost().getHostAddress());
            } catch (UnknownHostException e) {
                code.image.getError().println(e);
                throw new PrimitiveFailed();
            }
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
        protected Object doWork(final Object receiver, final long socketID, final long port, final NotProvided backlogSize) {
            final SocketImpl socketImpl = sockets.get(socketID);
            if (socketImpl == null) {
                code.image.getError().println("No socket for socket id");
                throw new PrimitiveFailed();
            }

            socketImpl.listenOn((int) port);
            return receiver;
        }

        @Specialization
        protected Object doWork(final Object receiver, final long socketID, final long port, final Object backlogSize) {
            final SocketImpl socketImpl = sockets.get(socketID);
            if (socketImpl == null) {
                code.image.getError().println("No socket for socket id");
                throw new PrimitiveFailed();
            }

            socketImpl.listenOn((int) port);
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

            socketImpl.listenOn((int) port);
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
            SocketImpl socketImpl = sockets.get(socketID);
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
            SocketImpl socketImpl = sockets.get(socketID);
            if (socketImpl == null) {
                code.image.getError().println("No socket for socket id");
                throw new PrimitiveFailed();
            }
            final String hostAddressString = hostAddress.toString();
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
                return code.image.wrap(socket.getRemoteAddress());
            } catch (IOException e) {
                code.image.getError().println(e);
                return 0;
            }
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
            SocketImpl socketImpl = sockets.get(socketID);
            if (socketImpl == null) {
                code.image.getError().println("No socket for socket id");
                throw new PrimitiveFailed();
            }
            Object value = socketImpl.getOption(option.toString());
            Long errorCode = socketImpl.getError();
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
            try {
                return code.image.wrap(InetAddress.getLocalHost().getHostAddress());
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
                        final Object aStringOrByteArray,
                        final long startIndex,
                        final long count) {
            final SocketImpl socketImpl = sockets.get(socketID);
            if (socketImpl == null) {
                code.image.getError().println("No socket for socket id");
                throw new PrimitiveFailed();
            }
            try {
                byte[] data = null;
                if (aStringOrByteArray instanceof String) {
                    data = ((String) aStringOrByteArray).getBytes();
                } else if (aStringOrByteArray instanceof byte[]) {
                    data = (byte[]) aStringOrByteArray;
                } else {
                    code.image.getError().println("Unknown data type");
                    throw new PrimitiveFailed();
                }
                socketImpl.sendData(data, (int) startIndex, (int) count);
                return count;
            } catch (IOException e) {
                code.image.getError().println(e);
                return 0;
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
            final SocketImpl socket = sockets.get(socketID);
            try {
                socket.close();
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
        protected boolean doWork(@SuppressWarnings("unused") final Object receiver, final long socketID) {
            code.image.getError().println("TODO: primitiveSocketSendDone");
            throw new PrimitiveFailed();
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
        protected long doWork(@SuppressWarnings("unused") final Object receiver, final Object socketID, final Object receiveBuffer, final Object startIndex, final Object count) {
            code.image.getError().println("TODO: primitiveSocketReceiveDataBufCount");
            throw new PrimitiveFailed();
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

        @Specialization
        protected long doWork(final PointersObject receiver,
                        final long netType,
                        final long socketType,
                        final long rcvBufSize,
                        final long semaIndex,
                        final long aReadSema,
                        final long aWriteSema) {
            SocketImpl s = new SocketImpl();
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

        @Specialization
        protected long doWork(final PointersObject receiver,
                        long socketID,
                        Object receiveBufferSize,
                        Object sendBufSize,
                        Object semaIndex,
                        Object readSemaIndex,
                        Object writeSemaIndex) {
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
