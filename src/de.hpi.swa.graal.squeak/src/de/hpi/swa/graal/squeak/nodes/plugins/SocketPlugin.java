package de.hpi.swa.graal.squeak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
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

    private static final class SocketStatus {
        private static final long InvalidSocket = -1;
        private static final long Unconnected = 0;
        private static final long WaitingForConnection = 1;
        private static final long Connected = 2;
        private static final long OtherEndClosed = 3;
        private static final long ThisEndClosed = 4;
    }

    // NetNameResolver
    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveResolverStatus")
    protected abstract static class PrimResolverStatusNode extends AbstractPrimitiveNode {
        protected PrimResolverStatusNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected long doStatus(@SuppressWarnings("unused") final Object receiver) {
            return Resolver.Ready;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveResolverStartNameLookup")
    protected abstract static class PrimResolverStartNameLookup extends AbstractPrimitiveNode {
        protected PrimResolverStartNameLookup(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected long doStart(@SuppressWarnings("unused") final Object receiver, final Object hostName) {
            return 0;
            // TODO
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveResolverNameLookupResult")
    protected abstract static class PrimResolverNameLookupResult extends AbstractPrimitiveNode {
        protected PrimResolverNameLookupResult(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected Object doGet(@SuppressWarnings("unused") final Object receiver) {
            return null;
            // TODO
        }
    }

    // Socket
    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSocketConnectToPort")
    protected abstract static class PrimSocketConnectToPort extends AbstractPrimitiveNode {
        protected PrimSocketConnectToPort(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected long doConnect(@SuppressWarnings("unused") final Object receiver, final Object socketID, final Object hostAddress, final Object port) {
            // TODO
            return 0;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSocketConnectionStatus")
    protected abstract static class PrimSocketConnectionStatus extends AbstractPrimitiveNode {
        protected PrimSocketConnectionStatus(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected long doStatus(@SuppressWarnings("unused") final Object receiver, final Object socketID) {
            return SocketStatus.Unconnected;
            // TODO
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSocketCreate3Semaphores")
    protected abstract static class PrimSocketCreate3SemaphoresNode extends AbstractPrimitiveNode {
        protected PrimSocketCreate3SemaphoresNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected Object doCreate(final PointersObject receiver, final long netType, final long socketType, final long rcvBufSize, final long semaIndex, final long aReadSema,
                        final long aWriteSema) {
            return receiver;
        }
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return SocketPluginFactory.getFactories();
    }
}
