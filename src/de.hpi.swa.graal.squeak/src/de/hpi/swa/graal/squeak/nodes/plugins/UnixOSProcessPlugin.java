package de.hpi.swa.graal.squeak.nodes.plugins;

import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;

public final class UnixOSProcessPlugin extends AbstractOSProcessPlugin {

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        final List<NodeFactory<? extends AbstractPrimitiveNode>> factories = new ArrayList<>();
        factories.addAll(UnixOSProcessPluginFactory.getFactories());
        factories.addAll(AbstractOSProcessPluginFactory.getFactories());
        return factories;
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveEnvironmentAt")
    protected abstract static class PrimEnvironmentAtNode extends AbstractPrimitiveNode {
        protected PrimEnvironmentAtNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        @TruffleBoundary
        protected final Object doAt(@SuppressWarnings("unused") final Object receiver, final long index) {
            try {
                final String key = System.getenv().keySet().toArray()[(int) index].toString();
                final String value = System.getenv(key);
                assert value != null : "value should never be null, ArrayIndexOutOfBoundsException should have been thrown when retrieving key";
                return code.image.wrap(key + "=" + value);
            } catch (ArrayIndexOutOfBoundsException | NullPointerException | SecurityException e) {
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveEnvironmentAtSymbol")
    protected abstract static class PrimEnvironmentAtSymbolNode extends AbstractPrimitiveNode {

        protected PrimEnvironmentAtSymbolNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = "aSymbol.isByteType()")
        @TruffleBoundary
        protected final Object doAt(@SuppressWarnings("unused") final Object receiver, final NativeObject aSymbol) {
            final String key = aSymbol.asString();
            try {
                final String value = System.getenv(key);
                if (value == null) {
                    throw new PrimitiveFailed();
                }
                return code.image.wrap(value);
            } catch (NullPointerException | SecurityException e) {
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveGetPPid")
    protected abstract static class PrimGetPPidNode extends AbstractPrimitiveNode {

        protected PrimGetPPidNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final Object doAt(@SuppressWarnings("unused") final Object receiver) {
            return code.image.nil; // TODO: implement parent pid
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveSigChldNumber")
    protected abstract static class PrimSigChldNumberNode extends AbstractPrimitiveNode {

        protected PrimSigChldNumberNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final long doNumber(@SuppressWarnings("unused") final Object receiver) {
            return code.image.wrap(20);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveForwardSignalToSemaphore")
    protected abstract static class PrimForwardSignalToSemaphoreNode extends AbstractPrimitiveNode {

        protected PrimForwardSignalToSemaphoreNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected final Object doForward(final Object receiver, final long signalNumber, final long semaphoreIndex) {
            return code.image.nil; // TODO: implement
        }
    }

}
