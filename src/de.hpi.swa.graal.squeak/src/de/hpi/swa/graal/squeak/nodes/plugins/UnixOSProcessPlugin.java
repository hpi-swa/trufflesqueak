package de.hpi.swa.graal.squeak.nodes.plugins;

import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage.Env;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.sun.security.auth.module.UnixSystem;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.BooleanObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitiveWithoutFallback;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitiveWithoutFallback;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.graal.squeak.util.MiscUtils;

public final class UnixOSProcessPlugin extends AbstractOSProcessPlugin {
    private static final UnixSystem UNIX_SYSTEM = new UnixSystem();

    @Override
    public boolean isEnabled(final SqueakImageContext image) {
        return image.os.isLinux() || image.os.isMacOS();
    }

    @TruffleBoundary
    private static String systemGetEnv(final Env env, final String key) {
        return env.getEnvironment().get(key);
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

        @Specialization
        protected final Object doAt(@SuppressWarnings("unused") final Object receiver, final long index) {
            if (index == 1) {
                return method.image.asByteString(MiscUtils.getVMPath(method.image));
            } else if (1 < index && index < method.image.getImageArguments().length) {
                return method.image.asByteString(method.image.getImageArguments()[(int) index - 2]);
            } else {
                return NilObject.SINGLETON;
            }
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
        protected final NativeObject doAt(@SuppressWarnings("unused") final Object receiver, final long index) {
            final String key = getEnvironmentKeys()[(int) index - 1].toString();
            assert key != null : "key should not be null";
            final String value = systemGetEnv(method.image.env, key);
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
        protected final NativeObject doAt(@SuppressWarnings("unused") final Object receiver, final NativeObject aSymbol) {
            final String key = aSymbol.asStringUnsafe();
            final String value = systemGetEnv(method.image.env, key);
            if (value == null) {
                throw PrimitiveFailed.GENERIC_ERROR;
            } else {
                return method.image.asByteString(value);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetGid")
    protected abstract static class PrimGetGidNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        protected PrimGetGidNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final long doGid(@SuppressWarnings("unused") final Object receiver) {
            return UNIX_SYSTEM.getGid();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetPPid")
    protected abstract static class PrimGetPPidNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        protected PrimGetPPidNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final NilObject doAt(@SuppressWarnings("unused") final Object receiver) {
            return NilObject.SINGLETON; // TODO: implement parent pid
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetUid")
    protected abstract static class PrimGetUidNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        protected PrimGetUidNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final long doUid(@SuppressWarnings("unused") final Object receiver) {
            return UNIX_SYSTEM.getUid();
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(names = "primitiveGetStdErrHandle")
    protected abstract static class PrimGetStdErrHandleNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        protected PrimGetStdErrHandleNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final long doGet(@SuppressWarnings("unused") final Object receiver) {
            return FilePlugin.STDIO_HANDLES.ERROR;
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(names = "primitiveGetStdInHandle")
    protected abstract static class PrimGetStdInHandleNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        protected PrimGetStdInHandleNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final long doGet(@SuppressWarnings("unused") final Object receiver) {
            return FilePlugin.STDIO_HANDLES.IN;
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(names = "primitiveGetStdOutHandle")
    protected abstract static class PrimGetStdOutHandleNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        protected PrimGetStdOutHandleNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final long doGet(@SuppressWarnings("unused") final Object receiver) {
            return FilePlugin.STDIO_HANDLES.OUT;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSigChldNumber")
    protected abstract static class PrimSigChldNumberNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        protected PrimSigChldNumberNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final long doNumber(@SuppressWarnings("unused") final Object receiver) {
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
        protected static final boolean doForward(final Object receiver, final long signalNumber, final long semaphoreIndex) {
            return BooleanObject.TRUE; // TODO: implement
        }
    }
}
