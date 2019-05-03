package de.hpi.swa.graal.squeak.nodes.plugins;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleException;
import com.oracle.truffle.api.TruffleLanguage.Env;
import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.LanguageInfo;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.Source.LiteralBuilder;
import com.oracle.truffle.api.source.Source.SourceBuilder;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.interop.WrapToSqueakNode;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObjectWithClassAndHash;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectToObjectArrayNode;
import de.hpi.swa.graal.squeak.nodes.plugins.PolyglotPluginFactory.PrimExecuteNodeFactory;
import de.hpi.swa.graal.squeak.nodes.plugins.PolyglotPluginFactory.PrimReadMemberNodeFactory;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitiveWithoutFallback;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.graal.squeak.shared.SqueakLanguageConfig;
import de.hpi.swa.graal.squeak.util.MiscUtils;

public final class PolyglotPlugin extends AbstractPrimitiveFactoryHolder {
    private static final TruffleLogger LOG = TruffleLogger.getLogger(SqueakLanguageConfig.ID, PolyglotPlugin.class);
    private static final String EVAL_SOURCE_NAME = "<eval>";

    /**
     * TODO: use @CachedLibrary("receiver") instead of @CachedLibrary(limit = "2") in this plugin
     * once https://github.com/oracle/graal/issues/1210 is fixed.
     */

    @Override
    public boolean isEnabled(final SqueakImageContext image) {
        return image.supportsTruffleObject();
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return PolyglotPluginFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveEvalString")
    protected abstract static class PrimEvalStringNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        protected PrimEvalStringNode(final CompiledMethodObject method) {
            super(method);
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        @Specialization(guards = {"languageIdOrMimeTypeObj.isByteType()", "sourceObject.isByteType()"})
        protected final Object doParseAndCall(@SuppressWarnings("unused") final Object receiver, final NativeObject languageIdOrMimeTypeObj, final NativeObject sourceObject,
                        @Cached final WrapToSqueakNode wrapNode) {
            final String languageIdOrMimeType = languageIdOrMimeTypeObj.asStringUnsafe();
            final String sourceText = sourceObject.asStringUnsafe();
            final Env env = method.image.env;
            try {
                final boolean mimeType = isMimeType(languageIdOrMimeType);
                final String lang = mimeType ? findLanguageByMimeType(env, languageIdOrMimeType) : languageIdOrMimeType;
                LiteralBuilder newBuilder = Source.newBuilder(lang, sourceText, EVAL_SOURCE_NAME);
                if (mimeType) {
                    newBuilder = newBuilder.mimeType(languageIdOrMimeType);
                }
                final Source source = newBuilder.build();
                final boolean wasActive = method.image.interrupt.isActive();
                method.image.interrupt.deactivate();
                try {
                    return wrapNode.executeWrap(env.parse(source).call());
                } finally {
                    if (wasActive) {
                        method.image.interrupt.activate();
                    }
                }
            } catch (final RuntimeException e) {
                if (e instanceof TruffleException) {
                    PrimGetLastErrorNode.setLastError(e);
                    throw new PrimitiveFailed();
                } else {
                    throw e;
                }
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveEvalFile")
    protected abstract static class PrimEvalFileNode extends AbstractPrimitiveNode implements TernaryPrimitive {

        protected PrimEvalFileNode(final CompiledMethodObject method) {
            super(method);
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        @Specialization(guards = {"languageIdOrMimeTypeObj.isByteType()", "path.isByteType()"})
        protected final Object doParseAndCall(@SuppressWarnings("unused") final Object receiver, final NativeObject languageIdOrMimeTypeObj, final NativeObject path) {
            final String languageIdOrMimeType = languageIdOrMimeTypeObj.asStringUnsafe();
            final String pathString = path.asStringUnsafe();
            final Env env = method.image.env;
            try {
                final boolean mimeType = isMimeType(languageIdOrMimeType);
                final String lang = mimeType ? findLanguageByMimeType(env, languageIdOrMimeType) : languageIdOrMimeType;
                SourceBuilder newBuilder = Source.newBuilder(lang, env.getTruffleFile(pathString));
                if (mimeType) {
                    newBuilder = newBuilder.mimeType(languageIdOrMimeType);
                }
                return env.parse(newBuilder.name(pathString).build()).call();
            } catch (final IOException e) {
                PrimGetLastErrorNode.setLastError(e);
                throw new PrimitiveFailed();
            } catch (final RuntimeException e) {
                if (e instanceof TruffleException) {
                    PrimGetLastErrorNode.setLastError(e);
                    throw new PrimitiveFailed();
                } else {
                    throw e;
                }
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveEvalC")
    protected abstract static class PrimEvalCNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        private static final String C_FILENAME = "temp.c";
        private static final String LLVM_FILENAME = "temp.bc";
        @Child private PrimReadMemberNode readNode;
        @Child private PrimExecuteNode executeNode;

        protected PrimEvalCNode(final CompiledMethodObject method) {
            super(method);
            readNode = PrimReadMemberNodeFactory.create(method, null);
            executeNode = PrimExecuteNodeFactory.create(method, null);
        }

        @Specialization(guards = {"receiver.isByteType()", "memberToCall.isByteType()"})
        protected final Object doEvaluate(final VirtualFrame frame, final NativeObject receiver, final NativeObject memberToCall,
                        @Cached final WrapToSqueakNode wrapNode) {
            final String foreignCode = receiver.asStringUnsafe();
            final String cFile = method.image.imageRelativeFilePathFor(C_FILENAME);
            final String llvmFile = method.image.imageRelativeFilePathFor(LLVM_FILENAME);
            try {
                final Source source = generateSourcefromCCode(foreignCode, cFile, llvmFile);
                final CallTarget foreignCallTarget = method.image.env.parse(source);
                final Object library = foreignCallTarget.call();
                final Object cFunction = readNode.executeWithArguments(frame, library, memberToCall);
                final Object result = executeNode.executeWithArguments(frame, cFunction);
                return wrapNode.executeWrap(result);
            } catch (final Exception e) {
                PrimGetLastErrorNode.setLastError(e);
                throw new PrimitiveFailed();
            }
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private Source generateSourcefromCCode(final String foreignCode, final String cFile, final String llvmFile) throws IOException, InterruptedException {
            Files.write(Paths.get(cFile), foreignCode.getBytes());
            final Process p = Runtime.getRuntime().exec("clang -O1 -c -emit-llvm -o " + llvmFile + " " + cFile);
            p.waitFor();
            return Source.newBuilder("llvm", method.image.env.getTruffleFile(llvmFile)).build();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAsPointer")
    protected abstract static class PrimAsPointerNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimAsPointerNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"lib.isPointer(receiver)"}, limit = "2")
        protected static final long doAsPointer(final Object receiver,
                        @CachedLibrary("receiver") final InteropLibrary lib) {
            try {
                return lib.asPointer(receiver);
            } catch (final UnsupportedMessageException e) {
                throw SqueakException.illegalState(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveExecute")
    protected abstract static class PrimExecuteNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimExecuteNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"lib.isExecutable(receiver)"}, limit = "2")
        protected static final Object doExecute(final Object receiver, final ArrayObject argumentArray,
                        @Cached final ArrayObjectToObjectArrayNode getObjectArrayNode,
                        @Cached final WrapToSqueakNode wrapNode,
                        @CachedLibrary("receiver") final InteropLibrary lib) {
            try {
                return wrapNode.executeWrap(lib.execute(receiver, getObjectArrayNode.execute(argumentArray)));
            } catch (UnsupportedTypeException | ArityException e) {
                PrimGetLastErrorNode.setLastError(e);
                throw new PrimitiveFailed();
            } catch (final RuntimeException e) {
                if (e instanceof TruffleException) {
                    PrimGetLastErrorNode.setLastError(e);
                    throw new PrimitiveFailed();
                } else {
                    throw e;
                }
            } catch (final UnsupportedMessageException e) {
                throw SqueakException.illegalState(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveExport")
    protected abstract static class PrimExportNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        protected PrimExportNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "name.isByteType()")
        @TruffleBoundary(transferToInterpreterOnException = false)
        public final Object exportSymbol(@SuppressWarnings("unused") final ClassObject receiver, final NativeObject name, final Object value) {
            method.image.env.exportSymbol(name.asStringUnsafe(), value);
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetMembers")
    protected abstract static class PrimGetMembersNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        protected PrimGetMembersNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"lib.hasMembers(receiver)"}, limit = "2")
        protected static final Object doGetMembers(final Object receiver,
                        @CachedLibrary("receiver") final InteropLibrary lib) {
            try {
                return lib.getMembers(receiver, true);
            } catch (final UnsupportedMessageException e) {
                throw SqueakException.illegalState(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveHasArrayElements")
    protected abstract static class PrimHasArrayElementsNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        protected PrimHasArrayElementsNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final boolean doHasArrayElements(final Object receiver,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return method.image.asBoolean(lib.hasArrayElements(receiver));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveHasMembers")
    protected abstract static class PrimHasMembersNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        protected PrimHasMembersNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final boolean doHasArrayElements(final Object receiver,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return method.image.asBoolean(lib.hasMembers(receiver));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetArraySize")
    protected abstract static class PrimGetArraySizeNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimGetArraySizeNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "lib.hasArrayElements(receiver)", limit = "2")
        protected static final long doGetArraySize(final Object receiver,
                        @CachedLibrary("receiver") final InteropLibrary lib) {
            try {
                return lib.getArraySize(receiver);
            } catch (final UnsupportedMessageException e) {
                throw SqueakException.illegalState(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetLanguageInfo")
    protected abstract static class PrimGetLanguageInfoNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimGetLanguageInfoNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "languageID.isByteType()")
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final ArrayObject doGet(@SuppressWarnings("unused") final ClassObject receiver, final NativeObject languageID,
                        @Cached final WrapToSqueakNode wrapNode) {
            final Collection<LanguageInfo> languages = method.image.env.getLanguages().values();
            return wrapNode.executeList(languages.stream().//
                            filter(l -> !l.isInternal() && l.getId().equals(languageID.asStringUnsafe())).//
                            map(l -> new Object[]{l.getId(), l.getName(), l.getVersion(), l.getDefaultMimeType(), l.getMimeTypes().toArray()}).//
                            findFirst().orElseThrow(PrimitiveFailed::new));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetLastError")
    protected abstract static class PrimGetLastErrorNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        protected static Exception lastError = SqueakException.create("");

        protected PrimGetLastErrorNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary
        protected final NativeObject doGetLastError(@SuppressWarnings("unused") final Object receiver) {
            return method.image.asByteString(lastError.toString());
        }

        protected static final void setLastError(final Exception e) {
            LOG.fine(() -> MiscUtils.toString(e));
            lastError = e;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetPolyglotBindings")
    protected abstract static class PrimGetPolyglotBindingsNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        protected PrimGetPolyglotBindingsNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doGet(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            return method.image.env.getPolyglotBindings();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveImport")
    protected abstract static class PrimImportNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimImportNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "name.isByteType()")
        @TruffleBoundary(transferToInterpreterOnException = false)
        public final Object importSymbol(@SuppressWarnings("unused") final ClassObject receiver, final NativeObject name) {
            final Object object = method.image.env.importSymbol(name.asStringUnsafe());
            if (object == null) {
                return NilObject.SINGLETON;
            }
            return object;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsPolyglotAccessAllowed")
    protected abstract static class PrimIsPolyglotAccessAllowedNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        protected PrimIsPolyglotAccessAllowedNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final boolean doIsPolyglotAccessAllowed(@SuppressWarnings("unused") final Object receiver) {
            return method.image.asBoolean(method.image.env.isPolyglotAccessAllowed());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveListAvailableLanguageIDs")
    protected abstract static class PrimListAvailableLanguageIDsNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        protected PrimListAvailableLanguageIDsNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final ArrayObject doList(@SuppressWarnings("unused") final ClassObject receiver,
                        @Cached final WrapToSqueakNode wrapNode) {
            final Collection<LanguageInfo> languages = method.image.env.getLanguages().values();
            final Object[] result = languages.stream().filter(l -> !l.isInternal()).map(l -> l.getId()).toArray();
            return wrapNode.executeList(result);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveInvoke")
    protected abstract static class PrimInvokeNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        protected PrimInvokeNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"member.isByteType()", "lib.isMemberInvocable(receiver, member.asStringUnsafe())"}, limit = "2")
        protected static final Object doInvoke(final Object receiver, final NativeObject member, final ArrayObject argumentArray,
                        @Cached final ArrayObjectToObjectArrayNode getObjectArrayNode,
                        @Cached final WrapToSqueakNode wrapNode,
                        @CachedLibrary("receiver") final InteropLibrary lib) {
            try {
                return wrapNode.executeWrap(lib.invokeMember(receiver, member.asStringUnsafe(), getObjectArrayNode.execute(argumentArray)));
            } catch (UnsupportedTypeException | ArityException e) {
                PrimGetLastErrorNode.setLastError(e);
                throw new PrimitiveFailed();
            } catch (final RuntimeException e) {
                if (e instanceof TruffleException) {
                    PrimGetLastErrorNode.setLastError(e);
                    throw new PrimitiveFailed();
                } else {
                    throw e;
                }
            } catch (UnknownIdentifierException | UnsupportedMessageException e) {
                throw SqueakException.illegalState(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsBoolean")
    protected abstract static class PrimIsBooleanNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        protected PrimIsBooleanNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final boolean doIsBoolean(final Object receiver,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return method.image.asBoolean(lib.isBoolean(receiver));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAsBoolean")
    protected abstract static class PrimAsBooleanNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimAsBooleanNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "lib.isBoolean(receiver)", limit = "2")
        protected final boolean doAsBoolean(final Object receiver,
                        @CachedLibrary("receiver") final InteropLibrary lib) {
            try {
                return method.image.asBoolean(lib.asBoolean(receiver));
            } catch (final UnsupportedMessageException e) {
                throw SqueakException.illegalState(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsString")
    protected abstract static class PrimIsStringNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        protected PrimIsStringNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final boolean doIsString(final Object receiver,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return method.image.asBoolean(lib.isString(receiver));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAsString")
    protected abstract static class PrimAsStringNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimAsStringNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "lib.isString(receiver)", limit = "2")
        protected final NativeObject doAsString(final Object receiver,
                        @CachedLibrary("receiver") final InteropLibrary lib) {
            try {
                return method.image.asByteString(lib.asString(receiver));
            } catch (final UnsupportedMessageException e) {
                throw SqueakException.illegalState(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFitsInLong")
    protected abstract static class PrimFitsInLongNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        protected PrimFitsInLongNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final boolean doFitsInLong(final Object receiver,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return method.image.asBoolean(lib.fitsInLong(receiver));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAsLong")
    protected abstract static class PrimAsLongNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimAsLongNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"lib.fitsInLong(receiver)"}, limit = "2")
        protected static final long doAsLong(final Object receiver,
                        @CachedLibrary("receiver") final InteropLibrary lib) {
            try {
                return lib.asLong(receiver);
            } catch (final UnsupportedMessageException e) {
                throw SqueakException.illegalState(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFitsInDouble")
    protected abstract static class PrimFitsInDoubleNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        protected PrimFitsInDoubleNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final boolean doFitsInDouble(final Object receiver,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return method.image.asBoolean(lib.fitsInDouble(receiver));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAsDouble")
    protected abstract static class PrimAsDoubleNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimAsDoubleNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"lib.fitsInDouble(receiver)"}, limit = "2")
        protected static final double doAsDouble(final Object receiver,
                        @CachedLibrary("receiver") final InteropLibrary lib) {
            try {
                return lib.asDouble(receiver);
            } catch (final UnsupportedMessageException e) {
                throw SqueakException.illegalState(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIdentityHash")
    protected abstract static class PrimPolyglotIdentityHashNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        protected PrimPolyglotIdentityHashNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final long doIdentityHash(final Object receiver) {
            return receiver.hashCode() & AbstractSqueakObjectWithClassAndHash.IDENTITY_HASH_MASK;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsExecutable")
    protected abstract static class PrimIsExecutableNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        protected PrimIsExecutableNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final boolean doIsExecutable(final Object receiver,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return method.image.asBoolean(lib.isExecutable(receiver));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsInstantiable")
    protected abstract static class PrimIsInstantiableNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        protected PrimIsInstantiableNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final boolean doIsInstantiable(final Object receiver,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return method.image.asBoolean(lib.isInstantiable(receiver));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveInstantiate")
    protected abstract static class PrimInstantiateNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        protected PrimInstantiateNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object doIsInstantiable(final Object receiver, final ArrayObject argumentArray,
                        @Cached final ArrayObjectToObjectArrayNode getObjectArrayNode,
                        @Cached final WrapToSqueakNode wrapNode,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return wrapNode.executeWrap(lib.instantiate(receiver, getObjectArrayNode.execute(argumentArray)));
            } catch (UnsupportedTypeException | ArityException | UnsupportedMessageException e) {
                PrimGetLastErrorNode.setLastError(e);
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsNull")
    protected abstract static class PrimIsNullNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        protected PrimIsNullNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final boolean doIsNull(final Object receiver,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return method.image.asBoolean(lib.isNull(receiver));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsPointer")
    protected abstract static class PrimIsPointerNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        protected PrimIsPointerNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final boolean doIsPointer(final Object receiver,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return method.image.asBoolean(lib.isPointer(receiver));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveReadArrayElement")
    protected abstract static class PrimReadArrayElementNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimReadArrayElementNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"isArrayElementReadable(lib, receiver, index)"}, limit = "2")
        protected static final Object doReadArrayElement(final Object receiver, final long index,
                        @Cached final WrapToSqueakNode wrapNode,
                        @CachedLibrary("receiver") final InteropLibrary lib) {
            try {
                return wrapNode.executeWrap(lib.readArrayElement(receiver, index - 1));
            } catch (final InvalidArrayIndexException e) {
                PrimGetLastErrorNode.setLastError(e);
                throw new PrimitiveFailed();
            } catch (final UnsupportedMessageException e) {
                throw SqueakException.illegalState(e);
            }
        }

        protected static final boolean isArrayElementReadable(final InteropLibrary lib, final Object receiver, final long index) {
            return lib.isArrayElementReadable(receiver, index - 1);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveReadMember")
    protected abstract static class PrimReadMemberNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimReadMemberNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"member.isByteType()", "lib.isMemberReadable(receiver, member.asStringUnsafe())",
                        "!lib.hasMemberReadSideEffects(receiver, member.asStringUnsafe())"}, limit = "2")
        protected static final Object doReadMember(final Object receiver, final NativeObject member,
                        @Cached final WrapToSqueakNode wrapNode,
                        @CachedLibrary("receiver") final InteropLibrary lib) {
            try {
                return wrapNode.executeWrap(lib.readMember(receiver, member.asStringUnsafe()));
            } catch (UnknownIdentifierException | UnsupportedMessageException e) {
                throw SqueakException.illegalState(e);
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"member.isByteType()", "lib.isMemberReadable(receiver, member.asStringUnsafe())",
                        "lib.hasMemberReadSideEffects(receiver, member.asStringUnsafe())"}, limit = "2")
        protected final NativeObject doReadMemberWithSideEffects(final Object receiver, final NativeObject member,
                        @CachedLibrary("receiver") final InteropLibrary lib) {
            return method.image.asByteString("[side-effect]");
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveStringRepresentation")
    protected abstract static class PrimStringRepresentationNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        protected PrimStringRepresentationNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final NativeObject doRead(final Object receiver) {
            return method.image.asByteString(MiscUtils.toString(receiver));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveWriteArrayElement")
    protected abstract static class PrimWriteArrayElementNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        protected PrimWriteArrayElementNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"lib.isArrayElementWritable(receiver, index)"}, limit = "2")
        protected static final Object doWrite(final Object receiver, final long index, final Object value,
                        @CachedLibrary("receiver") final InteropLibrary lib) {
            try {
                lib.writeArrayElement(receiver, index, value);
                return value;
            } catch (final UnsupportedTypeException e) {
                PrimGetLastErrorNode.setLastError(e);
                throw new PrimitiveFailed();
            } catch (UnsupportedMessageException | InvalidArrayIndexException e) {
                throw SqueakException.illegalState(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveWriteMember")
    protected abstract static class PrimWriteMemberNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        protected PrimWriteMemberNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"member.isByteType()", "lib.isMemberWritable(receiver, member.asStringUnsafe())"}, limit = "2")
        protected static final Object doWrite(final Object receiver, final NativeObject member, final Object value,
                        @CachedLibrary("receiver") final InteropLibrary lib) {
            try {
                lib.writeMember(receiver, member.asStringUnsafe(), value);
                return value;
            } catch (final UnsupportedTypeException e) {
                PrimGetLastErrorNode.setLastError(e);
                throw new PrimitiveFailed();
            } catch (UnknownIdentifierException | UnsupportedMessageException e) {
                throw SqueakException.illegalState(e);
            }
        }
    }

    /*
     * Java interop.
     */

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAddToHostClassPath")
    protected abstract static class PrimAddToHostClassPathNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimAddToHostClassPathNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"value.isByteType()"})
        protected final boolean doAddToHostClassPath(@SuppressWarnings("unused") final Object receiver, final NativeObject value) {
            final String path = value.asStringUnsafe();
            try {
                method.image.env.addToHostClassPath(method.image.env.getTruffleFile(path));
            } catch (final SecurityException e) {
                PrimGetLastErrorNode.setLastError(e);
                throw new PrimitiveFailed();
            }
            return method.image.sqTrue;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveLookupHostSymbol")
    protected abstract static class PrimLookupHostSymbolNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimLookupHostSymbolNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"method.image.env.isHostLookupAllowed()", "value.isByteType()"})
        protected final Object doLookupHostSymbol(@SuppressWarnings("unused") final Object receiver, final NativeObject value) {
            final String symbolName = value.asStringUnsafe();
            Object hostValue;
            try {
                hostValue = method.image.env.lookupHostSymbol(symbolName);
            } catch (final RuntimeException e) {
                hostValue = null;
            }
            if (hostValue == null) {
                throw new PrimitiveFailed();
            } else {
                return hostValue;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsHostFunction")
    protected abstract static class PrimIsHostFunctionNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        protected PrimIsHostFunctionNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final boolean doIsHostFunction(@SuppressWarnings("unused") final Object receiver) {
            return method.image.asBoolean(method.image.env.isHostFunction(receiver));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsHostObject")
    protected abstract static class PrimIsHostObjectNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        protected PrimIsHostObjectNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final boolean doIsHostObject(@SuppressWarnings("unused") final Object receiver) {
            return method.image.asBoolean(method.image.env.isHostObject(receiver));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsHostSymbol")
    protected abstract static class PrimIsHostSymbolNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        protected PrimIsHostSymbolNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final boolean doIsHostSymbol(@SuppressWarnings("unused") final Object receiver) {
            return method.image.asBoolean(method.image.env.isHostSymbol(receiver));
        }
    }

    /*
     * Helper functions.
     */

    @TruffleBoundary(transferToInterpreterOnException = false)
    private static String findLanguageByMimeType(final Env env, final String mimeType) {
        final Map<String, LanguageInfo> languages = env.getLanguages();
        for (final String registeredMimeType : languages.keySet()) {
            if (mimeType.equals(registeredMimeType)) {
                return languages.get(registeredMimeType).getId();
            }
        }
        return null;
    }

    private static boolean isMimeType(final String lang) {
        return lang.contains("/");
    }
}
