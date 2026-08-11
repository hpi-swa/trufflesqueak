/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.nodes.plugins.FilePlugin.PrimGetWorkingDirectoryNode;
import de.hpi.swa.trufflesqueak.nodes.plugins.FilePlugin.PrimSetWorkingDirectoryNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive0;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;

public abstract class AbstractOSProcessPlugin extends AbstractPrimitiveFactoryHolder {

    protected abstract static class AbstractSysCallPrimitiveNode extends AbstractPrimitiveNode {
        private final MethodHandle handle = findHandle();

        protected static final long failIfMinusOne(final long result, final InlinedBranchProfile errorProfile, final Node node) {
            if (result == -1) {
                errorProfile.enter(node);
                throw PrimitiveFailed.GENERIC_ERROR;
            } else {
                return result;
            }
        }

        protected abstract String getFunctionName();

        protected FunctionDescriptor getFunctionSignature() {
            return FunctionDescriptor.of(ValueLayout.JAVA_INT);
        }

        protected final MethodHandle getHandle() {
            return handle;
        }

        @SuppressWarnings("restricted")
        private MethodHandle findHandle() {
            final Linker linker = Linker.nativeLinker();
            final SymbolLookup stdlib = linker.defaultLookup();
            return stdlib.find(getFunctionName()).map(memorySegment -> linker.downcallHandle(memorySegment, getFunctionSignature())).orElseThrow(
                            () -> new RuntimeException("Could not find '" + getFunctionName() + "' in the standard library. Are you on a POSIX system?"));
        }

        @TruffleBoundary
        protected final long getValue() {
            try {
                return (int) handle.invokeExact();
            } catch (final Throwable e) {
                throw PrimitiveFailed.andTransferToInterpreterWithError(e);
            }
        }

        @TruffleBoundary
        protected final long getValue(final long id) {
            try {
                return (int) handle.invokeExact((int) id);
            } catch (final Throwable e) {
                throw PrimitiveFailed.andTransferToInterpreterWithError(e);
            }
        }

        @TruffleBoundary
        protected final long setValue(final long id, final long value) {
            try {
                return (int) handle.invokeExact((int) id, (int) value);
            } catch (final Throwable e) {
                throw PrimitiveFailed.andTransferToInterpreterWithError(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveChdir")
    protected abstract static class PrimChdirNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "path.isByteType()")
        protected static final NilObject doChdir(@SuppressWarnings("unused") final Object receiver, final NativeObject path,
                        @Bind final SqueakImageContext image) {
            PrimSetWorkingDirectoryNode.setWorkingDirectoryOrFail(image, path);
            return NilObject.SINGLETON; // Signals success.
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetCurrentWorkingDirectory")
    protected abstract static class PrimGetCurrentWorkingDirectoryNode extends AbstractPrimitiveNode implements Primitive0 {
        @Specialization
        protected static final NativeObject doGet(final Object receiver,
                        @Bind final SqueakImageContext image) {
            return PrimGetWorkingDirectoryNode.getWorkingDirectory(receiver, image);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetPid")
    protected abstract static class PrimGetPidNode extends AbstractPrimitiveNode implements Primitive0 {
        @Specialization
        protected static final long doGetPid(@SuppressWarnings("unused") final Object receiver) {
            return ProcessHandle.current().pid();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetSession")
    protected abstract static class PrimGetSessionNode extends AbstractPrimitiveNode implements Primitive0 {
        @CompilationFinal private NativeObject sessionByteArray;

        @Specialization
        protected final NativeObject doSession(@SuppressWarnings("unused") final Object receiver) {
            if (sessionByteArray == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                final byte[] bytes = new byte[4];
                ArrayUtils.fillRandomly(bytes);
                sessionByteArray = getContext().asByteArray(bytes);
            }
            return sessionByteArray;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSizeOfInt")
    protected abstract static class PrimSizeOfIntNode extends AbstractPrimitiveNode implements Primitive0 {
        @Specialization
        protected static final long doSizeOf(@SuppressWarnings("unused") final Object receiver) {
            return Integer.BYTES;
        }
    }
}
