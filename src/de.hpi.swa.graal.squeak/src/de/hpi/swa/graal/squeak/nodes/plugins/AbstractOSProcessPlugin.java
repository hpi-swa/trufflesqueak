package de.hpi.swa.graal.squeak.nodes.plugins;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.graal.squeak.util.ArrayUtils;

public abstract class AbstractOSProcessPlugin extends AbstractPrimitiveFactoryHolder {

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetPid")
    protected abstract static class PrimGetPidNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimGetPidNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected static final long doGet(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            try {
                final String runtimeName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
                try {
                    final int index = runtimeName.indexOf('@');
                    if (index != -1) {
                        return Long.parseLong(runtimeName.substring(0, index));
                    }
                } catch (final NumberFormatException e) {
                }
                throw new PrimitiveFailed();
            } catch (final LinkageError err) {
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetSession")
    protected abstract static class PrimGetSessionNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        @CompilationFinal private NativeObject sessionByteArray;

        protected PrimGetSessionNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final NativeObject doSession(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            return getSessionByteArray();
        }

        private NativeObject getSessionByteArray() {
            if (sessionByteArray == null) {
                initializeSessionByteArray();
            }
            return sessionByteArray;
        }

        @TruffleBoundary
        private void initializeSessionByteArray() {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            final byte[] bytes = new byte[4];
            ArrayUtils.fillRandomly(bytes);
            sessionByteArray = method.image.asByteArray(bytes);
        }
    }
}
