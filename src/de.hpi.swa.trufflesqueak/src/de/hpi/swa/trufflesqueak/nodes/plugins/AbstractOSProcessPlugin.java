/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.TruffleLanguage.Env;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.nfi.api.SignatureLibrary;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive0;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;

public abstract class AbstractOSProcessPlugin extends AbstractPrimitiveFactoryHolder {

    protected abstract static class AbstractSysCallPrimitiveNode extends AbstractPrimitiveNode {
        protected final boolean supportsNFI;
        @CompilationFinal protected Object sysCallObject;

        public AbstractSysCallPrimitiveNode() {
            supportsNFI = SqueakImageContext.getSlow().supportsNFI();
        }

        protected static final long failIfMinusOne(final long result, final InlinedBranchProfile errorProfile, final Node node) {
            if (result == -1) {
                errorProfile.enter(node);
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
            assert supportsNFI;
            if (sysCallObject == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                final Env env = SqueakImageContext.getSlow().env;
                final Object defaultLibrary = env.parseInternal(Source.newBuilder("nfi", "default", "native").build()).call();
                try {
                    final Object symbol = InteropLibrary.getUncached().readMember(defaultLibrary, getFunctionName());
                    sysCallObject = SignatureLibrary.getUncached().bind(createNFISignature(env, getFunctionSignature()), symbol);
                } catch (UnsupportedMessageException | UnknownIdentifierException e) {
                    throw PrimitiveFailed.andTransferToInterpreterWithError(e);
                }
            }
            return sysCallObject;
        }

        private static Object createNFISignature(final Env env, final String functionSignature) {
            final Source source = Source.newBuilder("nfi", functionSignature, "signature").build();
            return env.parseInternal(source).call();
        }

        protected final long getValue(final InteropLibrary lib) {
            try {
                return (int) lib.execute(sysCallObject);
            } catch (final UnsupportedMessageException | UnsupportedTypeException | ArityException e) {
                throw PrimitiveFailed.andTransferToInterpreterWithError(e);
            }
        }

        protected final long getValue(final InteropLibrary lib, final long id) {
            try {
                return (int) lib.execute(sysCallObject, (int) id);
            } catch (final UnsupportedMessageException | UnsupportedTypeException | ArityException e) {
                throw PrimitiveFailed.andTransferToInterpreterWithError(e);
            }
        }

        protected final long setValue(final InteropLibrary lib, final long id, final long value) {
            try {
                return (int) lib.execute(sysCallObject, (int) id, (int) value);
            } catch (final UnsupportedMessageException | UnsupportedTypeException | ArityException e) {
                throw PrimitiveFailed.andTransferToInterpreterWithError(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveChdir")
    protected abstract static class PrimChdirNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization(guards = "pathString.isByteType()")
        protected final NilObject doChdir(@SuppressWarnings("unused") final Object receiver, final NativeObject pathString,
                        @Bind final Node node,
                        @Cached final InlinedBranchProfile errorProfile) {
            final SqueakImageContext image = getContext();
            try {
                image.env.setCurrentWorkingDirectory(image.env.getPublicTruffleFile(pathString.asStringUnsafe()));
                return NilObject.SINGLETON; // Signals success.
            } catch (UnsupportedOperationException | IllegalArgumentException | SecurityException e) {
                errorProfile.enter(node);
                throw PrimitiveFailed.BAD_ARGUMENT;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetCurrentWorkingDirectory")
    protected abstract static class PrimGetCurrentWorkingDirectoryNode extends AbstractPrimitiveNode implements Primitive0 {

        @Specialization
        protected final NativeObject doGet(@SuppressWarnings("unused") final Object receiver) {
            final SqueakImageContext image = getContext();
            return image.asByteString(image.env.getCurrentWorkingDirectory().getPath());
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
}
