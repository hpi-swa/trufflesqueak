package de.hpi.swa.graal.squeak.nodes.plugins;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage.Env;
import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.LanguageInfo;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.Source.LiteralBuilder;
import com.oracle.truffle.api.source.Source.SourceBuilder;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.nodes.WrapToSqueakNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectToObjectArrayNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.graal.squeak.shared.SqueakLanguageConfig;
import de.hpi.swa.graal.squeak.util.MiscUtils;

public final class PolyglotPlugin extends AbstractPrimitiveFactoryHolder {
    private static final TruffleLogger LOG = TruffleLogger.getLogger(SqueakLanguageConfig.ID, PolyglotPlugin.class);
    private static final String EVAL_SOURCE_NAME = "<eval>";

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
        @Child private WrapToSqueakNode wrapNode;

        protected PrimEvalStringNode(final CompiledMethodObject method) {
            super(method);
            wrapNode = WrapToSqueakNode.create(method.image);
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        @Specialization(guards = {"languageIdOrMimeTypeObj.isByteType()", "sourceObject.isByteType()"})
        protected final Object doParseAndCall(@SuppressWarnings("unused") final Object receiver, final NativeObject languageIdOrMimeTypeObj, final NativeObject sourceObject) {
            PrimGetLastErrorNode.unsetLastError();
            final String languageIdOrMimeType = languageIdOrMimeTypeObj.asString();
            final String sourceText = sourceObject.asString();
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
                PrimGetLastErrorNode.setLastError(e);
                throw new PrimitiveFailed();
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
            PrimGetLastErrorNode.unsetLastError();
            final String languageIdOrMimeType = languageIdOrMimeTypeObj.asString();
            final String pathString = path.asString();
            final Env env = method.image.env;
            try {
                final boolean mimeType = isMimeType(languageIdOrMimeType);
                final String lang = mimeType ? findLanguageByMimeType(env, languageIdOrMimeType) : languageIdOrMimeType;
                SourceBuilder newBuilder = Source.newBuilder(lang, env.getTruffleFile(pathString));
                if (mimeType) {
                    newBuilder = newBuilder.mimeType(languageIdOrMimeType);
                }
                return env.parse(newBuilder.name(pathString).build()).call();
            } catch (IOException | RuntimeException e) {
                CompilerDirectives.transferToInterpreter();
                PrimGetLastErrorNode.setLastError(e);
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveEvalC")
    protected abstract static class PrimEvalCNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        private static final String C_FILENAME = "temp.c";
        private static final String LLVM_FILENAME = "temp.bc";
        @Child private Node readNode = Message.READ.createNode();
        @Child private Node executeNode = Message.EXECUTE.createNode();
        @Child private WrapToSqueakNode wrapNode;

        protected PrimEvalCNode(final CompiledMethodObject method) {
            super(method);
            wrapNode = WrapToSqueakNode.create(method.image);
        }

        @Specialization(guards = {"receiver.isByteType()", "memberToCall.isByteType()"})
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doEvaluate(final NativeObject receiver, final NativeObject memberToCall) {
            PrimGetLastErrorNode.unsetLastError();
            final String foreignCode = receiver.asString();
            final String cFile = method.image.imageRelativeFilePathFor(C_FILENAME);
            final String llvmFile = method.image.imageRelativeFilePathFor(LLVM_FILENAME);
            try {
                Files.write(Paths.get(cFile), foreignCode.getBytes());
                final Process p = Runtime.getRuntime().exec("clang -O1 -c -emit-llvm -o " + llvmFile + " " + cFile);
                p.waitFor();
                final Source source = Source.newBuilder("llvm", method.image.env.getTruffleFile(llvmFile)).build();
                final CallTarget foreignCallTarget = method.image.env.parse(source);
                final TruffleObject library = (TruffleObject) foreignCallTarget.call();
                final TruffleObject cFunction;
                try {
                    cFunction = (TruffleObject) ForeignAccess.sendRead(readNode, library, memberToCall.asString());
                } catch (UnknownIdentifierException | UnsupportedMessageException e) {
                    PrimGetLastErrorNode.setLastError(e);
                    throw new PrimitiveFailed();
                }
                final Object result;
                try {
                    result = ForeignAccess.sendExecute(executeNode, cFunction);
                } catch (UnsupportedTypeException | ArityException | UnsupportedMessageException e) {
                    PrimGetLastErrorNode.setLastError(e);
                    throw new PrimitiveFailed();
                }
                return wrapNode.executeWrap(result);
            } catch (IOException | RuntimeException | InterruptedException e) {
                PrimGetLastErrorNode.setLastError(e);
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAsPointer")
    protected abstract static class PrimAsPointerNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        @Child private Node asPointerNode = Message.AS_POINTER.createNode();

        protected PrimAsPointerNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"!isAbstractSqueakObject(receiver)"})
        protected final long doExecute(final TruffleObject receiver) {
            PrimGetLastErrorNode.unsetLastError();
            try {
                return method.image.wrap(ForeignAccess.sendAsPointer(asPointerNode, receiver));
            } catch (final UnsupportedMessageException e) {
                PrimGetLastErrorNode.setLastError(e);
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveExecute")
    protected abstract static class PrimExecuteNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        @Child private Node executeNode = Message.EXECUTE.createNode();
        @Child private ArrayObjectToObjectArrayNode getObjectArrayNode = ArrayObjectToObjectArrayNode.create();
        @Child private WrapToSqueakNode wrapNode;

        protected PrimExecuteNode(final CompiledMethodObject method) {
            super(method);
            wrapNode = WrapToSqueakNode.create(method.image);
        }

        @Specialization(guards = {"!isAbstractSqueakObject(receiver)", "identifier.isByteType()"})
        protected final Object doExecute(final TruffleObject receiver, final NativeObject identifier, final ArrayObject argumentArray) {
            PrimGetLastErrorNode.unsetLastError();
            final Object[] arguments = getObjectArrayNode.execute(argumentArray);
            try {
                return wrapNode.executeWrap(ForeignAccess.sendExecute(executeNode, receiver, identifier.asString(), arguments));
            } catch (UnsupportedMessageException | UnsupportedTypeException | ArityException e) {
                PrimGetLastErrorNode.setLastError(e);
                throw new PrimitiveFailed();
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
            method.image.env.exportSymbol(name.asString(), value);
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetKeys")
    protected abstract static class PrimGetKeysNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        @Child private Node keysNode = Message.KEYS.createNode();
        @Child private Node getSizeNode = Message.GET_SIZE.createNode();
        @Child private Node readNode = Message.READ.createNode();
        @Child private WrapToSqueakNode wrapNode;

        protected PrimGetKeysNode(final CompiledMethodObject method) {
            super(method);
            wrapNode = WrapToSqueakNode.create(method.image);
        }

        @Specialization(guards = "!isAbstractSqueakObject(receiver)")
        protected final ArrayObject doKeys(final TruffleObject receiver) {
            PrimGetLastErrorNode.unsetLastError();
            try {
                final TruffleObject sendKeysResult = ForeignAccess.sendKeys(keysNode, receiver);
                final int size = (int) ForeignAccess.sendGetSize(getSizeNode, sendKeysResult);
                final Object[] keys = new Object[size];
                for (int index = 0; index < size; index++) {
                    final Object value = ForeignAccess.sendRead(readNode, sendKeysResult, index);
                    keys[index] = value == null ? "null" : value.toString();
                }
                return wrapNode.executeList(keys);
            } catch (UnsupportedMessageException | UnknownIdentifierException e) {
                PrimGetLastErrorNode.setLastError(e);
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetInstSize")
    protected abstract static class PrimGetInstSizeNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        @Child private Node hasKeysNode = Message.HAS_KEYS.createNode();
        @Child private Node getKeysNode = Message.KEYS.createNode();
        @Child private Node getSizeNode = Message.GET_SIZE.createNode();

        protected PrimGetInstSizeNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "!isAbstractSqueakObject(receiver)")
        protected final long doInstSize(final TruffleObject receiver) {
            PrimGetLastErrorNode.unsetLastError();
            try {
                if (ForeignAccess.sendHasKeys(hasKeysNode, receiver)) {
                    final TruffleObject keys = ForeignAccess.sendKeys(getKeysNode, receiver);
                    return (int) ForeignAccess.sendGetSize(getSizeNode, keys);
                } else {
                    return 0L;
                }
            } catch (final UnsupportedMessageException e) {
                PrimGetLastErrorNode.setLastError(e);
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetSize")
    protected abstract static class PrimGetSizeNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        @Child private Node hasSizeNode = Message.HAS_SIZE.createNode();
        @Child private Node getSizeNode = Message.GET_SIZE.createNode();

        protected PrimGetSizeNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "!isAbstractSqueakObject(receiver)")
        protected final long doSize(final TruffleObject receiver) {
            PrimGetLastErrorNode.unsetLastError();
            try {
                if (ForeignAccess.sendHasSize(hasSizeNode, receiver)) {
                    return (int) ForeignAccess.sendGetSize(getSizeNode, receiver);
                } else {
                    return 0L;
                }
            } catch (final UnsupportedMessageException e) {
                PrimGetLastErrorNode.setLastError(e);
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetLanguageInfo")
    protected abstract static class PrimGetLanguageInfoNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        @Child private WrapToSqueakNode wrapNode;

        protected PrimGetLanguageInfoNode(final CompiledMethodObject method) {
            super(method);
            wrapNode = WrapToSqueakNode.create(method.image);
        }

        @Specialization(guards = "languageID.isByteType()")
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final ArrayObject doGet(@SuppressWarnings("unused") final ClassObject receiver, final NativeObject languageID) {
            final Collection<LanguageInfo> languages = method.image.env.getLanguages().values();
            return wrapNode.executeList(languages.stream().//
                            filter(l -> !l.isInternal() && l.getId().equals(languageID.asString())).//
                            map(l -> new Object[]{l.getId(), l.getName(), l.getVersion(), l.getDefaultMimeType(), l.getMimeTypes().toArray()}).//
                            findFirst().orElseThrow(PrimitiveFailed::new));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetLastError")
    protected abstract static class PrimGetLastErrorNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        protected static Exception lastError = null;

        protected PrimGetLastErrorNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "lastError != null")
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final NativeObject doError(@SuppressWarnings("unused") final Object receiver) {
            return method.image.wrap(lastError.toString());
        }

        @Specialization(guards = "lastError == null")
        protected final NilObject doNil(@SuppressWarnings("unused") final Object receiver) {
            return method.image.nil;
        }

        protected static final void setLastError(final Exception e) {
            LOG.fine(() -> MiscUtils.toString(e));
            lastError = e;
        }

        protected static final void unsetLastError() {
            lastError = null;
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
        protected final TruffleObject doGet(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            return (TruffleObject) method.image.env.getPolyglotBindings();
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
            final Object object = method.image.env.importSymbol(name.asString());
            if (object == null) {
                return method.image.nil;
            }
            return object;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveListAvailableLanguageIDs")
    protected abstract static class PrimListAvailableLanguageIDsNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        @Child private WrapToSqueakNode wrapNode;

        protected PrimListAvailableLanguageIDsNode(final CompiledMethodObject method) {
            super(method);
            wrapNode = WrapToSqueakNode.create(method.image);
        }

        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final ArrayObject doList(@SuppressWarnings("unused") final ClassObject receiver) {
            final Collection<LanguageInfo> languages = method.image.env.getLanguages().values();
            final Object[] result = languages.stream().filter(l -> !l.isInternal()).map(l -> l.getId()).toArray();
            return wrapNode.executeList(result);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveInvoke")
    protected abstract static class PrimInvokeNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        @Child private Node invokeNode = Message.INVOKE.createNode();
        @Child private ArrayObjectToObjectArrayNode getObjectArrayNode = ArrayObjectToObjectArrayNode.create();
        @Child private WrapToSqueakNode wrapNode;

        protected PrimInvokeNode(final CompiledMethodObject method) {
            super(method);
            wrapNode = WrapToSqueakNode.create(method.image);
        }

        @Specialization(guards = {"!isAbstractSqueakObject(receiver)", "identifier.isByteType()"})
        protected final Object doInvoke(final TruffleObject receiver, final NativeObject identifier, final ArrayObject argumentArray) {
            PrimGetLastErrorNode.unsetLastError();
            final Object[] arguments = getObjectArrayNode.execute(argumentArray);
            try {
                return wrapNode.executeWrap(ForeignAccess.sendInvoke(invokeNode, receiver, identifier.asString(), arguments));
            } catch (UnsupportedTypeException | ArityException | UnknownIdentifierException | UnsupportedMessageException e) {
                PrimGetLastErrorNode.setLastError(e);
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsBoxed")
    protected abstract static class PrimIsBoxedNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        @Child private Node isExecutableNode = Message.IS_BOXED.createNode();

        protected PrimIsBoxedNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"!isAbstractSqueakObject(receiver)"})
        protected final boolean doIsBoxed(final TruffleObject receiver) {
            return method.image.wrap(ForeignAccess.sendIsBoxed(isExecutableNode, receiver));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsExecutable")
    protected abstract static class PrimIsExecutableNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        @Child private Node isExecutableNode = Message.IS_EXECUTABLE.createNode();

        protected PrimIsExecutableNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"!isAbstractSqueakObject(receiver)"})
        protected final boolean doIsExecutable(final TruffleObject receiver) {
            return method.image.wrap(ForeignAccess.sendIsExecutable(isExecutableNode, receiver));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsInstantiable")
    protected abstract static class PrimIsInstantiableNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        @Child private Node isIntantiable = Message.IS_INSTANTIABLE.createNode();

        protected PrimIsInstantiableNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"!isAbstractSqueakObject(receiver)"})
        protected final boolean doIsInstantiable(final TruffleObject receiver) {
            return method.image.wrap(ForeignAccess.sendIsInstantiable(isIntantiable, receiver));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsNull")
    protected abstract static class PrimIsNullNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        @Child private Node isNullNode = Message.IS_NULL.createNode();

        protected PrimIsNullNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"!isAbstractSqueakObject(receiver)"})
        protected final boolean doIsNull(final TruffleObject receiver) {
            return method.image.wrap(ForeignAccess.sendIsNull(isNullNode, receiver));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsPointer")
    protected abstract static class PrimIsPointerNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        @Child private Node isPointerNode = Message.IS_POINTER.createNode();

        protected PrimIsPointerNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"!isAbstractSqueakObject(receiver)"})
        protected final boolean doIsPointer(final TruffleObject receiver) {
            return method.image.wrap(ForeignAccess.sendIsPointer(isPointerNode, receiver));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveRead")
    protected abstract static class PrimReadNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        @Child private Node readNode = Message.READ.createNode();
        @Child private WrapToSqueakNode wrapNode;

        protected PrimReadNode(final CompiledMethodObject method) {
            super(method);
            wrapNode = WrapToSqueakNode.create(method.image);
        }

        @Specialization(guards = {"!isAbstractSqueakObject(receiver)"})
        protected final Object doRead(final TruffleObject receiver, final long index) {
            return readIdentifier(receiver, index);
        }

        @Specialization(guards = {"!isAbstractSqueakObject(receiver)", "selector.isByteType()"})
        protected final Object doRead(final TruffleObject receiver, final NativeObject selector) {
            return readIdentifier(receiver, selector.asString());
        }

        private Object readIdentifier(final TruffleObject receiver, final Object identifier) {
            PrimGetLastErrorNode.unsetLastError();
            try {
                return wrapNode.executeWrap(ForeignAccess.sendRead(readNode, receiver, identifier));
            } catch (UnknownIdentifierException | UnsupportedMessageException e) {
                PrimGetLastErrorNode.setLastError(e);
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveStringRepresentation")
    protected abstract static class PrimStringRepresentationNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        @Child private Node readNode = Message.READ.createNode();

        protected PrimStringRepresentationNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"!isAbstractSqueakObject(receiver)"})
        protected final NativeObject doRead(final TruffleObject receiver) {
            return method.image.wrap(MiscUtils.toString(receiver));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveUnbox")
    protected abstract static class PrimUnboxNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        @Child private Node unboxNode = Message.UNBOX.createNode();
        @Child private WrapToSqueakNode wrapNode;

        protected PrimUnboxNode(final CompiledMethodObject method) {
            super(method);
            wrapNode = WrapToSqueakNode.create(method.image);
        }

        @Specialization(guards = {"!isAbstractSqueakObject(receiver)"})
        protected final Object doRead(final TruffleObject receiver) {
            PrimGetLastErrorNode.unsetLastError();
            try {
                return wrapNode.executeWrap(ForeignAccess.sendUnbox(unboxNode, receiver));
            } catch (final UnsupportedMessageException e) {
                PrimGetLastErrorNode.setLastError(e);
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveWrite")
    protected abstract static class PrimWriteNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        @Child private Node writeNode = Message.WRITE.createNode();
        @Child private WrapToSqueakNode wrapNode;

        protected PrimWriteNode(final CompiledMethodObject method) {
            super(method);
            wrapNode = WrapToSqueakNode.create(method.image);
        }

        @Specialization(guards = {"!isAbstractSqueakObject(receiver)", "selector.isByteType()"})
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doWrite(final TruffleObject receiver, final NativeObject selector, final Object value) {
            PrimGetLastErrorNode.unsetLastError();
            try {
                return wrapNode.executeWrap(ForeignAccess.sendWrite(writeNode, receiver, selector.asString(), value));
            } catch (UnknownIdentifierException | UnsupportedMessageException | UnsupportedTypeException e) {
                PrimGetLastErrorNode.setLastError(e);
                throw new PrimitiveFailed();
            }
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
