package de.hpi.swa.graal.squeak.nodes.plugins;

import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitiveWithoutFallback;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;

public final class UnixOSProcessPlugin extends AbstractOSProcessPlugin {

    @Override
    public boolean isEnabled(final SqueakImageContext image) {
        return image.os.isLinux() || image.os.isMacOS();
    }

    @TruffleBoundary
    private static String systemGetEnv(final String key) {
        return System.getenv(key);
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        final List<NodeFactory<? extends AbstractPrimitiveNode>> factories = new ArrayList<>();
        factories.addAll(UnixOSProcessPluginFactory.getFactories());
        factories.addAll(AbstractOSProcessPluginFactory.getFactories());
        return factories;
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveArgumentAt")
    protected abstract static class PrimArgumentAtNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {
        protected PrimArgumentAtNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "inBounds1(index, method.image.getImageArguments().length)")
        protected final Object doAt(@SuppressWarnings("unused") final Object receiver, final long index) {
            return method.image.asByteString(method.image.getImageArguments()[(int) index - 1]);
        }

        @SuppressWarnings("unused")
        @Fallback
        protected static final NilObject doNil(final Object receiver, final Object index) {
            return NilObject.SINGLETON;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveEnvironmentAt")
    protected abstract static class PrimEnvironmentAtNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        private static Object[] environmentKeys;

        protected PrimEnvironmentAtNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "inBounds1(index, getEnvironmentKeys().length)")
        protected final Object doAt(@SuppressWarnings("unused") final Object receiver, final long index) {
            final String key = getEnvironmentKeys()[(int) index - 1].toString();
            assert key != null : "key should not be null";
            final String value = systemGetEnv(key);
            assert value != null : "value should not be null";
            return method.image.asByteString(key + "=" + value);
        }

        protected static final Object[] getEnvironmentKeys() {
            if (environmentKeys == null) {
                environmentKeys = systemGetEnvKeyArray();
            }
            return environmentKeys;
        }

        @TruffleBoundary
        private static Object[] systemGetEnvKeyArray() {
            return System.getenv().keySet().toArray();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveEnvironmentAtSymbol")
    protected abstract static class PrimEnvironmentAtSymbolNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimEnvironmentAtSymbolNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "aSymbol.isByteType()")
        protected final Object doAt(@SuppressWarnings("unused") final Object receiver, final NativeObject aSymbol) {
            final String key = aSymbol.asStringUnsafe();
            final String value = systemGetEnv(key);
            if (value == null) {
                throw new PrimitiveFailed();
            } else {
                return method.image.asByteString(value);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetPPid")
    protected abstract static class PrimGetPPidNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimGetPPidNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object doAt(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            return NilObject.SINGLETON; // TODO: implement parent pid
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetStdErrHandle")
    protected abstract static class PrimGetStdErrHandleNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimGetStdErrHandleNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final long doGet(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            return FilePlugin.STDIO_HANDLES.ERROR;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetStdInHandle")
    protected abstract static class PrimGetStdInHandleNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimGetStdInHandleNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final long doGet(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            return FilePlugin.STDIO_HANDLES.IN;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetStdOutHandle")
    protected abstract static class PrimGetStdOutHandleNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimGetStdOutHandleNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final long doGet(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            return FilePlugin.STDIO_HANDLES.OUT;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSigChldNumber")
    protected abstract static class PrimSigChldNumberNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimSigChldNumberNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final long doNumber(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            return 20L;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveForwardSignalToSemaphore")
    protected abstract static class PrimForwardSignalToSemaphoreNode extends AbstractPrimitiveNode implements TernaryPrimitive {

        protected PrimForwardSignalToSemaphoreNode(final CompiledMethodObject method) {
            super(method);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected final Object doForward(final Object receiver, final long signalNumber, final long semaphoreIndex) {
            return method.image.sqTrue; // TODO: implement
        }
    }
}
