package de.hpi.swa.graal.squeak.nodes.plugins;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.graal.squeak.util.ArrayUtils;

public abstract class AbstractOSProcessPlugin extends AbstractPrimitiveFactoryHolder {

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveGetPid")
    protected abstract static class PrimGetPidNode extends AbstractPrimitiveNode {

        protected PrimGetPidNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        @TruffleBoundary
        protected static final long doGet(@SuppressWarnings("unused") final Object receiver) {
            try {
                final String runtimeName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
                try {
                    final int index = runtimeName.indexOf('@');
                    if (index != -1) {
                        return Long.parseLong(runtimeName.substring(0, index));
                    }
                } catch (NumberFormatException e) {
                }
                throw new PrimitiveFailed();
            } catch (LinkageError err) {
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveGetSession")
    protected abstract static class PrimGetSessionNode extends AbstractPrimitiveNode {
        @CompilationFinal private NativeObject sessionByteArray;

        protected PrimGetSessionNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final NativeObject doSession(@SuppressWarnings("unused") final Object receiver) {
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
            sessionByteArray = code.image.wrap(bytes);
        }
    }
}
