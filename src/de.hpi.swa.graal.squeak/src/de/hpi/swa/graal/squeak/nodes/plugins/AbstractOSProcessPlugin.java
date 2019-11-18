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
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.source.Source;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitiveWithoutFallback;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.graal.squeak.util.ArrayUtils;

public abstract class AbstractOSProcessPlugin extends AbstractPrimitiveFactoryHolder {

    protected abstract static class AbstractSysCallPrimitiveNode extends AbstractPrimitiveNode {
        protected final boolean supportsNFI;
        @CompilationFinal protected Object sysCallObject;

        protected AbstractSysCallPrimitiveNode(final CompiledMethodObject method) {
            super(method);
            supportsNFI = method.image.supportsNFI();
        }

        protected static final long failIfMinusOne(final long result, final BranchProfile errorProfile) {
            if (result == -1) {
                errorProfile.enter();
                throw PrimitiveFailed.GENERIC_ERROR;
            } else {
                return result;
            }
        }

        protected abstract String getFunctionName();

        protected String getFunctionSignature() {
            return "():SINT32";
        }

        protected final Object getSysCallObject() {
            if (sysCallObject == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                final Object defaultLibrary = method.image.env.parseInternal(Source.newBuilder("nfi", "default", "native").build()).call();
                final InteropLibrary lib = InteropLibrary.getFactory().getUncached();
                try {
                    final Object symbol = lib.readMember(defaultLibrary, getFunctionName());
                    sysCallObject = lib.invokeMember(symbol, "bind", getFunctionSignature());
                } catch (UnsupportedMessageException | UnknownIdentifierException | ArityException | UnsupportedTypeException e) {
                    throw SqueakException.illegalState(e);
                }
            }
            return sysCallObject;
        }

        protected final long getValue(final InteropLibrary lib) {
            try {
                return (int) lib.execute(sysCallObject);
            } catch (final UnsupportedMessageException | UnsupportedTypeException | ArityException e) {
                throw SqueakException.illegalState(e);
            }
        }

        protected final long getValue(final InteropLibrary lib, final long id) {
            try {
                return (int) lib.execute(sysCallObject, (int) id);
            } catch (final UnsupportedMessageException | UnsupportedTypeException | ArityException e) {
                throw SqueakException.illegalState(e);
            }
        }

        protected final long setValue(final InteropLibrary lib, final long id, final long value) {
            try {
                return (int) lib.execute(sysCallObject, (int) id, (int) value);
            } catch (final UnsupportedMessageException | UnsupportedTypeException | ArityException e) {
                throw SqueakException.illegalState(e);
            }
        }
    }

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
                method.image.env.setCurrentWorkingDirectory(method.image.env.getPublicTruffleFile(pathString.asStringUnsafe()));
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
    protected abstract static class PrimGetPidNode extends AbstractSysCallPrimitiveNode implements UnaryPrimitive {

        protected PrimGetPidNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "supportsNFI")
        protected final long doGetPid(@SuppressWarnings("unused") final Object receiver,
                        @CachedLibrary("getSysCallObject()") final InteropLibrary lib) {
            return getValue(lib);
        }

        @Override
        protected final String getFunctionName() {
            return "getpid"; /* shared (POSIX compatible) */
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
