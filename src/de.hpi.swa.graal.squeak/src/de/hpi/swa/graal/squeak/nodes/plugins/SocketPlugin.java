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
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Map;
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

    private static String lastLookup = "";

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

    static Map<Long, SocketImpl> sockets = new HashMap<>();

    private static final class SocketImpl {
        private Socket clientSocket;
        private ServerSocket serverSocket;

        public void listenOn(long port) {

        }

        public void connectTo(String host, long port) {
        }

        public void destroy() {

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

        // Look up the given host name in the Domain Name Server to find its address. This call is
        // asynchronous. To get the results, wait for it to complete or time out and then use
        // primNameLookupResult.
        @Specialization
        protected Object doWork(final Object receiver, final NativeObject hostName) {
            InetAddress address = null;
            final String hostNameString = hostName.toString();
            try {
                if (hostNameString.equals("localhost")) {
                    lastLookup = InetAddress.getLocalHost().getHostAddress();
                    return 1;
                }
                address = InetAddress.getByName(new URL(hostNameString).getHost());
                lastLookup = address.getHostAddress();
            } catch (UnknownHostException e) {
                System.out.println(e);
                lastLookup = null;
            } catch (MalformedURLException e) {
                System.out.println(e);
                lastLookup = null;
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
            if (lastLookup == null) {
                return code.image.nil;
            } else {
                return code.image.wrap(lastLookup);
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
                System.out.println(e);
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
        protected long doWork(@SuppressWarnings("unused") final Object receiver, final long socketID, final long port, final NotProvided backlogSize) {
            System.out.println("TODO: primitiveSocketListenWithOrWithoutBacklog");
            throw new PrimitiveFailed();
        }

        @Specialization
        protected long doWork(@SuppressWarnings("unused") final Object receiver, final long socketID, final long port, final Object backlogSize) {
            System.out.println("TODO: primitiveSocketListenWithOrWithoutBacklog");
            throw new PrimitiveFailed();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSocketListenOnPortBacklogInterface")
    protected abstract static class PrimSocketListenOnPortBacklogInterfaceNode extends AbstractPrimitiveNode {
        protected PrimSocketListenOnPortBacklogInterfaceNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected long doWork(@SuppressWarnings("unused") final Object receiver, final long socketID, final long port, final Object backlogSize, final Object interfaceAddress) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        ServerSocket ss = new ServerSocket((int) port);
                        ss.accept();
                    } catch (IOException e) {
                        System.out.println(e);
                    }
                }
            }.start();

            throw new PrimitiveFailed();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSocketSetOptions")
    protected abstract static class PrimSocketSetOptionsNode extends AbstractPrimitiveNode {
        protected PrimSocketSetOptionsNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected long doWork(@SuppressWarnings("unused") final Object receiver, final long socketID, final Object option, final Object value) {
            System.out.println("TODO: SocketSetOptions");
            throw new PrimitiveFailed();
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
            Socket socket = sockets.get(socketID);
            if (socket == null) {
                System.out.println("No socket for socket id");
                throw new PrimitiveFailed();
            }
            final String hostAddressString = hostAddress.toString();
            final SocketAddress endpoint = new InetSocketAddress(hostAddressString, (int) port);
            try {
                socket.connect(endpoint);
            } catch (IOException e) {
                System.out.println(e);
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

            final Socket socket = sockets.get(socketID);
            if (socket.isInputShutdown()) {
                return SocketStatus.ThisEndClosed;
            } else if (socket.isOutputShutdown()) {
                return SocketStatus.OtherEndClosed;
            } else if (!socket.isConnected()) {
                return SocketStatus.Unconnected;
            } else {
                return SocketStatus.Connected;
            }
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
            final Socket socket = sockets.get(socketID);
            final SocketAddress socketAddress = socket.getRemoteSocketAddress();
            if (socketAddress instanceof InetSocketAddress) {
                final InetSocketAddress inetAddress = (InetSocketAddress) socketAddress;
                return code.image.wrap(inetAddress.getAddress().getHostAddress());
            } else {
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
        @Specialization
        protected long doWork(@SuppressWarnings("unused") final Object receiver, final long socketID, final Object getOption) {
            throw new PrimitiveFailed();
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
            final Socket socket = sockets.get(socketID);
            try {
                return socket.getInputStream().available() > 0;
            } catch (IOException e) {
                System.out.println(e);
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
            return 0;
            // TODO
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
                System.out.println(e);
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

        // Send data to the remote host through the given socket starting with the given byte index
        // of the given byte array. The data sent is 'pushed' immediately. Return the number of
        // bytes of data actually sent; any remaining data should be re-submitted for sending after
        // the current send operation has completed.
        // Note: In general, it many take several sendData calls to transmit a large data array
        // since the data is sent in send-buffer-sized chunks. The size of the send buffer is
        // determined when the socket is created.
        @Specialization
        protected long doWork(@SuppressWarnings("unused") final Object receiver, final long socketID, final Object aStringOrByteArray, final long startIndex, final long count) {
            final Socket socket = sockets.get(socketID);
            try {
                final OutputStream outputStream = socket.getOutputStream();
                byte[] data = null;
                if (aStringOrByteArray instanceof String) {
                    data = ((String) aStringOrByteArray).getBytes();
                } else if (aStringOrByteArray instanceof byte[]) {
                    data = (byte[]) aStringOrByteArray;
                }

                outputStream.write(data, (int) startIndex, (int) count);
                return count;
            } catch (IOException e) {
                System.out.println(e);
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
            final Socket socket = sockets.get(socketID);
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println(e);
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
            final Socket socket = sockets.get(socketID);
            return true;
            // TODO
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
            @SuppressWarnings("unused")
            final Socket socket = sockets.get(socketID);
            return 0;
            // TODO
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
            final Socket socket = sockets.get(socketID);
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                System.out.println(e);
                throw new PrimitiveFailed();
            }
            sockets.remove(socketID);
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
            Socket s = new Socket();
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
            System.out.println("TODO: primitiveSocketCreate");
            throw new PrimitiveFailed();
        }
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return SocketPluginFactory.getFactories();
    }
}
