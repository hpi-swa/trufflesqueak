package de.hpi.swa.graal.squeak.nodes.plugins;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleContext;
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
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.Source.LiteralBuilder;
import com.oracle.truffle.api.source.Source.SourceBuilder;

import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.interop.ConvertToSqueakNode;
import de.hpi.swa.graal.squeak.interop.WrapToSqueakNode;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObjectWithHash;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BooleanObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectToObjectArrayCopyNode;
import de.hpi.swa.graal.squeak.nodes.plugins.PolyglotPluginFactory.PrimExecuteNodeFactory;
import de.hpi.swa.graal.squeak.nodes.plugins.PolyglotPluginFactory.PrimReadMemberNodeFactory;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitiveWithoutFallback;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.QuaternaryPrimitive;
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
    protected abstract static class PrimEvalStringNode extends AbstractPrimitiveNode implements QuaternaryPrimitive {
        protected PrimEvalStringNode(final CompiledMethodObject method) {
            super(method);
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        @Specialization(guards = {"!inInnerContext", "languageIdOrMimeTypeObj.isByteType()", "sourceObject.isByteType()"})
        protected final Object doEval(@SuppressWarnings("unused") final Object receiver, final NativeObject languageIdOrMimeTypeObj, final NativeObject sourceObject,
                        @SuppressWarnings("unused") final boolean inInnerContext,
                        @Cached final WrapToSqueakNode wrapNode) {
            return wrapNode.executeWrap(evalString(method.image, languageIdOrMimeTypeObj, sourceObject));
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        @Specialization(guards = {"inInnerContext", "languageIdOrMimeTypeObj.isByteType()", "sourceObject.isByteType()"})
        protected final Object doEvalInInnerContext(@SuppressWarnings("unused") final Object receiver, final NativeObject languageIdOrMimeTypeObj, final NativeObject sourceObject,
                        @SuppressWarnings("unused") final boolean inInnerContext,
                        @Cached final ConvertToSqueakNode convertNode) {
            final TruffleContext innerContext = method.image.env.newContextBuilder().build();
            final Object p = innerContext.enter();
            try {
                return convertNode.executeConvert(evalString(SqueakLanguage.getContext(), languageIdOrMimeTypeObj, sourceObject));
            } finally {
                innerContext.leave(p);
                innerContext.close();
            }
        }

        private static Object evalString(final SqueakImageContext image, final NativeObject languageIdOrMimeTypeObj, final NativeObject sourceObject) {
            final String languageIdOrMimeType = languageIdOrMimeTypeObj.asStringUnsafe();
            final String sourceText = sourceObject.asStringUnsafe();
            try {
                final boolean mimeType = isMimeType(languageIdOrMimeType);
                final String lang = mimeType ? findLanguageByMimeType(image.env, languageIdOrMimeType) : languageIdOrMimeType;
                LiteralBuilder newBuilder = Source.newBuilder(lang, sourceText, EVAL_SOURCE_NAME);
                if (mimeType) {
                    newBuilder = newBuilder.mimeType(languageIdOrMimeType);
                }
                final Source source = newBuilder.build();
                return image.env.parsePublic(source).call();
            } catch (final RuntimeException e) {
                throw primitiveFailedCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveEvalFile")
    protected abstract static class PrimEvalFileNode extends AbstractPrimitiveNode implements QuaternaryPrimitive {

        protected PrimEvalFileNode(final CompiledMethodObject method) {
            super(method);
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        @Specialization(guards = {"!inInnerContext", "languageIdOrMimeTypeObj.isByteType()", "path.isByteType()"})
        protected final Object doEval(@SuppressWarnings("unused") final Object receiver, final NativeObject languageIdOrMimeTypeObj, final NativeObject path,
                        @SuppressWarnings("unused") final boolean inInnerContext,
                        @Cached final WrapToSqueakNode wrapNode) {
            return wrapNode.executeWrap(evalFile(method.image, languageIdOrMimeTypeObj, path));
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        @Specialization(guards = {"inInnerContext", "languageIdOrMimeTypeObj.isByteType()", "path.isByteType()"})
        protected final Object doEvalInInnerContext(@SuppressWarnings("unused") final Object receiver, final NativeObject languageIdOrMimeTypeObj, final NativeObject path,
                        @SuppressWarnings("unused") final boolean inInnerContext,
                        @Cached final ConvertToSqueakNode convertNode) {
            final TruffleContext innerContext = method.image.env.newContextBuilder().build();
            final Object p = innerContext.enter();
            try {
                return convertNode.executeConvert(evalFile(SqueakLanguage.getContext(), languageIdOrMimeTypeObj, path));
            } finally {
                innerContext.leave(p);
                innerContext.close();
            }
        }

        private static Object evalFile(final SqueakImageContext image, final NativeObject languageIdOrMimeTypeObj, final NativeObject path) {
            final String languageIdOrMimeType = languageIdOrMimeTypeObj.asStringUnsafe();
            final String pathString = path.asStringUnsafe();
            try {
                final boolean mimeType = isMimeType(languageIdOrMimeType);
                final String lang = mimeType ? findLanguageByMimeType(image.env, languageIdOrMimeType) : languageIdOrMimeType;
                SourceBuilder newBuilder = Source.newBuilder(lang, image.env.getTruffleFile(pathString));
                if (mimeType) {
                    newBuilder = newBuilder.mimeType(languageIdOrMimeType);
                }
                return image.env.parsePublic(newBuilder.name(pathString).build()).call();
            } catch (IOException | RuntimeException e) {
                throw primitiveFailedCapturing(e);
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
                final CallTarget foreignCallTarget = method.image.env.parsePublic(source);
                final Object library = foreignCallTarget.call();
                final Object cFunction = readNode.executeWithArguments(frame, library, memberToCall);
                final Object result = executeNode.executeWithArguments(frame, cFunction);
                return wrapNode.executeWrap(result);
            } catch (final Exception e) {
                throw primitiveFailedCapturing(e);
            }
        }

        @TruffleBoundary
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
                throw primitiveFailedCapturing(e);
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
                        @Cached final ArrayObjectToObjectArrayCopyNode getObjectArrayNode,
                        @Cached final WrapToSqueakNode wrapNode,
                        @CachedLibrary("receiver") final InteropLibrary lib) {
            try {
                return wrapNode.executeWrap(lib.execute(receiver, getObjectArrayNode.execute(argumentArray)));
            } catch (UnsupportedTypeException | ArityException | RuntimeException | UnsupportedMessageException e) {
                throw primitiveFailedCapturing(e);
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
        @TruffleBoundary
        public final Object exportSymbol(@SuppressWarnings("unused") final Object receiver, final NativeObject name, final Object value) {
            method.image.env.exportSymbol(name.asStringUnsafe(), value);
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetLanguageInfo")
    protected abstract static class PrimGetLanguageInfoNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimGetLanguageInfoNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "languageID.isByteType()")
        @TruffleBoundary
        protected final ArrayObject doGet(@SuppressWarnings("unused") final Object receiver, final NativeObject languageID,
                        @Cached final WrapToSqueakNode wrapNode) {
            final Collection<LanguageInfo> languages = method.image.env.getPublicLanguages().values();
            return wrapNode.executeList(languages.stream().//
                            filter(l -> l.getId().equals(languageID.asStringUnsafe())).//
                            map(l -> new Object[]{l.getId(), l.getName(), l.getVersion(), l.getDefaultMimeType(), l.getMimeTypes().toArray()}).//
                            findFirst().orElseThrow(() -> PrimitiveFailed.GENERIC_ERROR));
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
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(names = "primitiveGetPolyglotBindings")
    protected abstract static class PrimGetPolyglotBindingsNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        protected PrimGetPolyglotBindingsNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary
        protected final Object doGet(@SuppressWarnings("unused") final Object receiver) {
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
        @TruffleBoundary
        public final Object importSymbol(@SuppressWarnings("unused") final Object receiver, final NativeObject name) {
            return NilObject.nullToNil(method.image.env.importSymbol(name.asStringUnsafe()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsPolyglotBindingsAccessAllowed")
    protected abstract static class PrimIsPolyglotBindingsAccessAllowedNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        protected PrimIsPolyglotBindingsAccessAllowedNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final boolean doIsPolyglotBindingsAccessAllowed(@SuppressWarnings("unused") final Object receiver) {
            return BooleanObject.wrap(method.image.env.isPolyglotBindingsAccessAllowed());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsPolyglotEvalAllowed")
    protected abstract static class PrimIsPolyglotEvalAllowedNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        protected PrimIsPolyglotEvalAllowedNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final boolean doIsPolyglotEvalAllowed(@SuppressWarnings("unused") final Object receiver) {
            return BooleanObject.wrap(method.image.env.isPolyglotEvalAllowed());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveListAvailableLanguageIDs")
    protected abstract static class PrimListAvailableLanguageIDsNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        protected PrimListAvailableLanguageIDsNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary
        protected final ArrayObject doList(@SuppressWarnings("unused") final Object receiver,
                        @Cached final WrapToSqueakNode wrapNode) {
            final Collection<LanguageInfo> languages = method.image.env.getPublicLanguages().values();
            final Object[] result = languages.stream().map(l -> l.getId()).toArray();
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
                        @Cached final ArrayObjectToObjectArrayCopyNode getObjectArrayNode,
                        @Cached final WrapToSqueakNode wrapNode,
                        @CachedLibrary("receiver") final InteropLibrary lib) {
            try {
                return wrapNode.executeWrap(lib.invokeMember(receiver, member.asStringUnsafe(), getObjectArrayNode.execute(argumentArray)));
            } catch (UnsupportedTypeException | ArityException | RuntimeException | UnknownIdentifierException | UnsupportedMessageException e) {
                throw primitiveFailedCapturing(e);
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
        protected static final boolean doIsBoolean(final Object receiver,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isBoolean(receiver));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAsBoolean")
    protected abstract static class PrimAsBooleanNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimAsBooleanNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "lib.isBoolean(receiver)", limit = "2")
        protected static final boolean doAsBoolean(final Object receiver,
                        @CachedLibrary("receiver") final InteropLibrary lib) {
            try {
                return BooleanObject.wrap(lib.asBoolean(receiver));
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedCapturing(e);
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
        protected static final boolean doIsString(final Object receiver,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isString(receiver));
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
                throw primitiveFailedCapturing(e);
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
        protected static final boolean doFitsInLong(final Object receiver,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.fitsInLong(receiver));
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
                throw primitiveFailedCapturing(e);
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
        protected static final boolean doFitsInDouble(final Object receiver,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.fitsInDouble(receiver));
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
                throw primitiveFailedCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsExecutable")
    protected abstract static class PrimIsExecutableNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        protected PrimIsExecutableNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final boolean doIsExecutable(final Object receiver,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isExecutable(receiver));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsInstantiable")
    protected abstract static class PrimIsInstantiableNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        protected PrimIsInstantiableNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final boolean doIsInstantiable(final Object receiver,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isInstantiable(receiver));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveInstantiate")
    protected abstract static class PrimInstantiateNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {

        protected PrimInstantiateNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object doIsInstantiable(final Object receiver, final ArrayObject argumentArray,
                        @Cached final ArrayObjectToObjectArrayCopyNode getObjectArrayNode,
                        @Cached final WrapToSqueakNode wrapNode,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return wrapNode.executeWrap(lib.instantiate(receiver, getObjectArrayNode.execute(argumentArray)));
            } catch (UnsupportedTypeException | ArityException | UnsupportedMessageException e) {
                throw primitiveFailedCapturing(e);
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
        protected static final boolean doIsNull(final Object receiver,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isNull(receiver));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsPointer")
    protected abstract static class PrimIsPointerNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        protected PrimIsPointerNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final boolean doIsPointer(final Object receiver,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isPointer(receiver));
        }
    }

    /*
     * Array-like objects
     */

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
                throw primitiveFailedCapturing(e);
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
        protected static final boolean doHasArrayElements(final Object receiver,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.hasArrayElements(receiver));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsArrayElementExisting")
    protected abstract static class PrimIsArrayElementExistingNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimIsArrayElementExistingNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final boolean doIsArrayElementExisting(final Object receiver, final long index,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isArrayElementExisting(receiver, index - 1));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsArrayElementInsertable")
    protected abstract static class PrimIsArrayElementInsertableNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimIsArrayElementInsertableNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final boolean doIsArrayElementInsertable(final Object receiver, final long index,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isArrayElementInsertable(receiver, index - 1));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsArrayElementModifiable")
    protected abstract static class PrimIsArrayElementModifiableNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimIsArrayElementModifiableNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final boolean doIsArrayElementModifiable(final Object receiver, final long index,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isArrayElementModifiable(receiver, index - 1));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsArrayElementReadable")
    protected abstract static class PrimIsArrayElementReadableNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimIsArrayElementReadableNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final boolean doIsArrayElementReadable(final Object receiver, final long index,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isArrayElementReadable(receiver, index - 1));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsArrayElementRemovable")
    protected abstract static class PrimIsArrayElementRemovableNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimIsArrayElementRemovableNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final boolean doIsArrayElementRemovable(final Object receiver, final long index,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isArrayElementRemovable(receiver, index - 1));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsArrayElementWritable")
    protected abstract static class PrimIsArrayElementWritableNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimIsArrayElementWritableNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final boolean doIsArrayElementWritable(final Object receiver, final long index,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isArrayElementWritable(receiver, index - 1));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveReadArrayElement")
    protected abstract static class PrimReadArrayElementNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimReadArrayElementNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"lib.isArrayElementReadable(receiver, to0(index))"}, limit = "2")
        protected static final Object doReadArrayElement(final Object receiver, final long index,
                        @Cached final WrapToSqueakNode wrapNode,
                        @CachedLibrary("receiver") final InteropLibrary lib) {
            try {
                return wrapNode.executeWrap(lib.readArrayElement(receiver, index - 1));
            } catch (InvalidArrayIndexException | UnsupportedMessageException e) {
                throw primitiveFailedCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveRemoveArrayElement")
    protected abstract static class PrimRemoveArrayElementNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimRemoveArrayElementNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"lib.isArrayElementRemovable(receiver, to0(index))"}, limit = "2")
        protected static final Object doRemoveArrayElement(final Object receiver, final long index,
                        @CachedLibrary("receiver") final InteropLibrary lib) {
            try {
                lib.removeArrayElement(receiver, index - 1);
                return receiver;
            } catch (UnsupportedMessageException | InvalidArrayIndexException e) {
                throw primitiveFailedCapturing(e);
            }
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
            } catch (UnsupportedTypeException | UnsupportedMessageException | InvalidArrayIndexException e) {
                throw primitiveFailedCapturing(e);
            }
        }
    }

    /*
     * Dictionary-like objects
     */

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetMembers")
    protected abstract static class PrimGetMembersNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        protected PrimGetMembersNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"lib.hasMembers(receiver)"}, limit = "2")
        protected final ArrayObject doGetMembers(final Object receiver,
                        @CachedLibrary("receiver") final InteropLibrary lib,
                        @CachedLibrary(limit = "2") final InteropLibrary membersLib,
                        @CachedLibrary(limit = "2") final InteropLibrary memberNameLib) {
            try {
                final Object members = lib.getMembers(receiver, true);
                final int size = (int) membersLib.getArraySize(members);
                final NativeObject[] byteStrings = new NativeObject[size];
                for (int i = 0; i < size; i++) {
                    final Object memberName = membersLib.readArrayElement(members, i);
                    byteStrings[i] = method.image.asByteString(memberNameLib.asString(memberName));
                }
                return method.image.asArrayOfNativeObjects(byteStrings);
            } catch (final UnsupportedMessageException | InvalidArrayIndexException e) {
                throw primitiveFailedCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetMemberSize")
    protected abstract static class PrimGetMemberSizeNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        protected PrimGetMemberSizeNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"lib.hasMembers(receiver)"}, limit = "2")
        protected static final Object doGetMembers(final Object receiver,
                        @CachedLibrary("receiver") final InteropLibrary lib,
                        @CachedLibrary(limit = "2") final InteropLibrary sizeLib) {
            try {
                return sizeLib.getArraySize(lib.getMembers(receiver, true));
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveHasMembers")
    protected abstract static class PrimHasMembersNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        protected PrimHasMembersNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final boolean doHasArrayElements(final Object receiver,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.hasMembers(receiver));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveHasMemberReadSideEffects")
    protected abstract static class PrimHasMemberReadSideEffectsNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimHasMemberReadSideEffectsNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"member.isByteType()"})
        protected static final boolean doHasMemberReadSideEffects(final Object receiver, final NativeObject member,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.hasMemberReadSideEffects(receiver, member.asStringUnsafe()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveHasMemberWriteSideEffects")
    protected abstract static class PrimHasMemberWriteSideEffectsNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimHasMemberWriteSideEffectsNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"member.isByteType()"})
        protected static final boolean doHasMemberWriteSideEffects(final Object receiver, final NativeObject member,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.hasMemberWriteSideEffects(receiver, member.asStringUnsafe()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsMemberExisting")
    protected abstract static class PrimIsMemberExistingNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimIsMemberExistingNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"member.isByteType()"})
        protected static final boolean doIsMemberExisting(final Object receiver, final NativeObject member,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isMemberExisting(receiver, member.asStringUnsafe()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsMemberInsertable")
    protected abstract static class PrimIsMemberInsertableNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimIsMemberInsertableNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"member.isByteType()"})
        protected static final boolean doIsMemberInsertable(final Object receiver, final NativeObject member,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isMemberInsertable(receiver, member.asStringUnsafe()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsMemberInternal")
    protected abstract static class PrimIsMemberInternalNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimIsMemberInternalNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"member.isByteType()"})
        protected static final boolean doIsMemberInternal(final Object receiver, final NativeObject member,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isMemberInternal(receiver, member.asStringUnsafe()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsMemberInvocable")
    protected abstract static class PrimIsMemberInvocableNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimIsMemberInvocableNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"member.isByteType()"})
        protected static final boolean doIsMemberInvocable(final Object receiver, final NativeObject member,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isMemberInvocable(receiver, member.asStringUnsafe()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsMemberModifiable")
    protected abstract static class PrimIsMemberModifiableNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimIsMemberModifiableNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"member.isByteType()"})
        protected static final boolean doIsMemberModifiable(final Object receiver, final NativeObject member,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isMemberModifiable(receiver, member.asStringUnsafe()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsMemberReadable")
    protected abstract static class PrimIsMemberReadableNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimIsMemberReadableNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"member.isByteType()"})
        protected static final boolean doIsMemberReadable(final Object receiver, final NativeObject member,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isMemberReadable(receiver, member.asStringUnsafe()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsMemberRemovable")
    protected abstract static class PrimIsMemberRemovableNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimIsMemberRemovableNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"member.isByteType()"})
        protected static final boolean doIsMemberRemovable(final Object receiver, final NativeObject member,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isMemberRemovable(receiver, member.asStringUnsafe()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsMemberWritable")
    protected abstract static class PrimIsMemberWritableNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimIsMemberWritableNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"member.isByteType()"})
        protected static final boolean doIsMemberWritable(final Object receiver, final NativeObject member,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isMemberWritable(receiver, member.asStringUnsafe()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveInvokeMember")
    protected abstract static class PrimInvokeMemberNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        protected PrimInvokeMemberNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"member.isByteType()", "lib.isMemberInvocable(receiver, member.asStringUnsafe())"}, limit = "2")
        protected static final Object doRemove(final Object receiver, final NativeObject member, final ArrayObject argumentArray,
                        @Cached final ArrayObjectToObjectArrayCopyNode getObjectArrayNode,
                        @Cached final WrapToSqueakNode wrapNode,
                        @CachedLibrary("receiver") final InteropLibrary lib) {
            try {
                return wrapNode.executeWrap(lib.invokeMember(receiver, member.asStringUnsafe(), getObjectArrayNode.execute(argumentArray)));
            } catch (UnknownIdentifierException | UnsupportedMessageException | ArityException | UnsupportedTypeException e) {
                throw primitiveFailedCapturing(e);
            }
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
                throw primitiveFailedCapturing(e);
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
    @SqueakPrimitive(names = "primitiveRemoveMember")
    protected abstract static class PrimRemoveMemberNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimRemoveMemberNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"member.isByteType()", "lib.isMemberRemovable(receiver, member.asStringUnsafe())"}, limit = "2")
        protected static final Object doRemove(final Object receiver, final NativeObject member,
                        @CachedLibrary("receiver") final InteropLibrary lib) {
            final String identifier = member.asStringUnsafe();
            try {
                lib.removeMember(receiver, identifier);
                return receiver;
            } catch (UnknownIdentifierException | UnsupportedMessageException e) {
                throw primitiveFailedCapturing(e);
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
            } catch (UnsupportedTypeException | UnknownIdentifierException | UnsupportedMessageException e) {
                throw primitiveFailedCapturing(e);
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
                throw primitiveFailedCapturing(e);
            }
            return BooleanObject.TRUE;
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
                throw PrimitiveFailed.GENERIC_ERROR;
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
            return BooleanObject.wrap(method.image.env.isHostFunction(receiver));
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
            return BooleanObject.wrap(method.image.env.isHostObject(receiver));
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
            return BooleanObject.wrap(method.image.env.isHostSymbol(receiver));
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
            return receiver.hashCode() & AbstractSqueakObjectWithHash.IDENTITY_HASH_MASK;
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

    /*
     * Helper functions.
     */

    private static PrimitiveFailed primitiveFailedCapturing(final Exception e) {
        PrimGetLastErrorNode.setLastError(e);
        return PrimitiveFailed.GENERIC_ERROR;
    }

    @TruffleBoundary
    private static String findLanguageByMimeType(final Env env, final String mimeType) {
        final Map<String, LanguageInfo> languages = env.getPublicLanguages();
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
