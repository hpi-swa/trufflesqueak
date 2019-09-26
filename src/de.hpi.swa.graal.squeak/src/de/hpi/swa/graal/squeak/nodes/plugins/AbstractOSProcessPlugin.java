/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.plugins;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitiveWithoutFallback;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.graal.squeak.util.ArrayUtils;

public abstract class AbstractOSProcessPlugin extends AbstractPrimitiveFactoryHolder {

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveChdir")
    protected abstract static class PrimChdirNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimChdirNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "pathString.isByteType()")
        protected final NilObject doChdir(@SuppressWarnings("unused") final Object receiver, final NativeObject pathString,
                        @Cached final BranchProfile errorProfile) {
            try {
                method.image.env.setCurrentWorkingDirectory(method.image.env.getTruffleFile(pathString.asStringUnsafe()));
                return NilObject.SINGLETON; // Signals success.
            } catch (UnsupportedOperationException | IllegalArgumentException | SecurityException e) {
                errorProfile.enter();
                throw PrimitiveFailed.BAD_ARGUMENT;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetCurrentWorkingDirectory")
    protected abstract static class PrimGetCurrentWorkingDirectoryNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        protected PrimGetCurrentWorkingDirectoryNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final NativeObject doGet(@SuppressWarnings("unused") final Object receiver) {
            return method.image.asByteString(method.image.env.getCurrentWorkingDirectory().getPath());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetPid")
    protected abstract static class PrimGetPidNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        protected PrimGetPidNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected static final long doGet(@SuppressWarnings("unused") final Object receiver) {
            try {
                final String runtimeName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
                try {
                    final int index = runtimeName.indexOf('@');
                    if (index != -1) {
                        return Long.parseLong(runtimeName.substring(0, index));
                    }
                } catch (final NumberFormatException e) {
                }
            } catch (final LinkageError err) {
            }
            throw PrimitiveFailed.GENERIC_ERROR;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetSession")
    protected abstract static class PrimGetSessionNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        @CompilationFinal private NativeObject sessionByteArray;

        protected PrimGetSessionNode(final CompiledMethodObject method) {
            super(method);
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
            sessionByteArray = method.image.asByteArray(bytes);
        }
    }
}
