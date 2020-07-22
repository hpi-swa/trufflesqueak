/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleContext;
import com.oracle.truffle.api.TruffleLanguage.Env;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
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

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakSyntaxError;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.interop.ConvertToSqueakNode;
import de.hpi.swa.trufflesqueak.interop.JavaObjectWrapper;
import de.hpi.swa.trufflesqueak.interop.WrapToSqueakNode;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObjectWithHash;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodes.ArrayObjectToObjectArrayCopyNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitiveWithoutFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveInterfaces.QuaternaryPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitiveWithoutFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitiveWithoutFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.util.LogUtils;
import de.hpi.swa.trufflesqueak.util.MiscUtils;

public final class PolyglotPlugin extends AbstractPrimitiveFactoryHolder {
    private static final String EVAL_SOURCE_NAME = "<eval>";

    /**
     * TODO: use @CachedLibrary("receiver") instead of @CachedLibrary(limit = "2") in this plugin
     * once https://github.com/oracle/graal/issues/1210 is fixed.
     */

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return PolyglotPluginFactory.getFactories();
    }

    /*
     * Management
     */

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveRegisterForeignObjectClass")
    protected abstract static class PrimRegisterForeignObjectClassNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        @Specialization
        protected static final boolean doRegisterForeignObjectClass(final ClassObject foreignObjectClass,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return BooleanObject.wrap(image.setForeignObjectClass(foreignObjectClass));
        }
    }

    /*
     * Code evaluation
     */

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsPolyglotEvalAllowed")
    protected abstract static class PrimIsPolyglotEvalAllowedNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        @Specialization
        protected static final boolean doIsPolyglotEvalAllowed(@SuppressWarnings("unused") final Object receiver,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return BooleanObject.wrap(image.env.isPolyglotEvalAllowed());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveEvalString")
    protected abstract static class PrimEvalStringNode extends AbstractPrimitiveNode implements QuaternaryPrimitive {
        @TruffleBoundary(transferToInterpreterOnException = false)
        @Specialization(guards = {"!inInnerContext", "languageIdOrMimeTypeObj.isByteType()", "sourceObject.isByteType()"})
        protected static final Object doEval(@SuppressWarnings("unused") final Object receiver, final NativeObject languageIdOrMimeTypeObj, final NativeObject sourceObject,
                        @SuppressWarnings("unused") final boolean inInnerContext,
                        @Cached final WrapToSqueakNode wrapNode,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return wrapNode.executeWrap(evalString(image, languageIdOrMimeTypeObj, sourceObject));
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        @Specialization(guards = {"inInnerContext", "languageIdOrMimeTypeObj.isByteType()", "sourceObject.isByteType()"})
        protected static final Object doEvalInInnerContext(@SuppressWarnings("unused") final Object receiver, final NativeObject languageIdOrMimeTypeObj, final NativeObject sourceObject,
                        @SuppressWarnings("unused") final boolean inInnerContext,
                        @Cached final ConvertToSqueakNode convertNode,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            final TruffleContext innerContext = image.env.newContextBuilder().build();
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
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveEvalFile")
    protected abstract static class PrimEvalFileNode extends AbstractPrimitiveNode implements QuaternaryPrimitive {

        @TruffleBoundary(transferToInterpreterOnException = false)
        @Specialization(guards = {"!inInnerContext", "languageIdOrMimeTypeObj.isByteType()", "path.isByteType()"})
        protected static final Object doEval(@SuppressWarnings("unused") final Object receiver, final NativeObject languageIdOrMimeTypeObj, final NativeObject path,
                        @SuppressWarnings("unused") final boolean inInnerContext,
                        @Cached final WrapToSqueakNode wrapNode,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return wrapNode.executeWrap(evalFile(image, languageIdOrMimeTypeObj, path));
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        @Specialization(guards = {"inInnerContext", "languageIdOrMimeTypeObj.isByteType()", "path.isByteType()"})
        protected static final Object doEvalInInnerContext(@SuppressWarnings("unused") final Object receiver, final NativeObject languageIdOrMimeTypeObj, final NativeObject path,
                        @SuppressWarnings("unused") final boolean inInnerContext,
                        @Cached final ConvertToSqueakNode convertNode,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            final TruffleContext innerContext = image.env.newContextBuilder().build();
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
                SourceBuilder newBuilder = Source.newBuilder(lang, image.env.getPublicTruffleFile(pathString));
                if (mimeType) {
                    newBuilder = newBuilder.mimeType(languageIdOrMimeType);
                }
                return image.env.parsePublic(newBuilder.name(pathString).build()).call();
            } catch (IOException | RuntimeException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    /*
     * Language information
     */

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveListAvailableLanguageIDs")
    protected abstract static class PrimListAvailableLanguageIDsNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        @Specialization
        @TruffleBoundary
        protected static final ArrayObject doList(@SuppressWarnings("unused") final Object receiver,
                        @Cached final WrapToSqueakNode wrapNode,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            final Collection<LanguageInfo> languages = image.env.getPublicLanguages().values();
            final Object[] result = languages.stream().map(l -> l.getId()).toArray();
            return wrapNode.executeList(result);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetLanguageInfo")
    protected abstract static class PrimGetLanguageInfoNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        @Specialization(guards = "languageID.isByteType()")
        @TruffleBoundary
        protected static final ArrayObject doGet(@SuppressWarnings("unused") final Object receiver, final NativeObject languageID,
                        @Cached final WrapToSqueakNode wrapNode,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            final Collection<LanguageInfo> languages = image.env.getPublicLanguages().values();
            return wrapNode.executeList(languages.stream().//
                            filter(l -> l.getId().equals(languageID.asStringUnsafe())).//
                            map(l -> new Object[]{l.getId(), l.getName(), l.getVersion(), l.getDefaultMimeType(), l.getMimeTypes().toArray()}).//
                            findFirst().orElseThrow(() -> PrimitiveFailed.GENERIC_ERROR));
        }
    }

    /*
     * Interaction with polyglot bindings object
     */

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsPolyglotBindingsAccessAllowed")
    protected abstract static class PrimIsPolyglotBindingsAccessAllowedNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        @Specialization
        protected static final boolean doIsPolyglotBindingsAccessAllowed(@SuppressWarnings("unused") final Object receiver,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return BooleanObject.wrap(image.env.isPolyglotBindingsAccessAllowed());
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(names = "primitiveGetPolyglotBindings")
    protected abstract static class PrimGetPolyglotBindingsNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        @Specialization(guards = "image.env.isPolyglotBindingsAccessAllowed()")
        protected static final Object doGet(@SuppressWarnings("unused") final Object receiver,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return image.env.getPolyglotBindings();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveImport")
    protected abstract static class PrimImportNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        @Specialization(guards = "name.isByteType()")
        @TruffleBoundary
        public static final Object importSymbol(@SuppressWarnings("unused") final Object receiver, final NativeObject name,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            try {
                return NilObject.nullToNil(image.env.importSymbol(name.asStringUnsafe()));
            } catch (final SecurityException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveExport")
    protected abstract static class PrimExportNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        @Specialization(guards = "name.isByteType()")
        @TruffleBoundary
        public static final Object exportSymbol(@SuppressWarnings("unused") final Object receiver, final NativeObject name, final Object value,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            try {
                image.env.exportSymbol(name.asStringUnsafe(), value);
                return value;
            } catch (final SecurityException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    /*
     * Generic interop messages
     */

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveExecute")
    protected abstract static class PrimExecuteNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        @Specialization(guards = {"lib.isExecutable(object)"}, limit = "2")
        protected static final Object doExecute(@SuppressWarnings("unused") final Object receiver, final Object object, final ArrayObject argumentArray,
                        @Cached final ArrayObjectToObjectArrayCopyNode getObjectArrayNode,
                        @Cached final WrapToSqueakNode wrapNode,
                        @CachedLibrary("object") final InteropLibrary lib) {
            try {
                return wrapNode.executeWrap(lib.execute(object, getObjectArrayNode.execute(argumentArray)));
            } catch (final Exception e) {
                /*
                 * Workaround: catch all exceptions raised by other languages to avoid crashes (see
                 * https://github.com/oracle/truffleruby/issues/1864).
                 */
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsInstantiable")
    protected abstract static class PrimIsInstantiableNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {

        @Specialization
        protected static final boolean doIsInstantiable(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isInstantiable(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveInstantiate")
    protected abstract static class PrimInstantiateNode extends AbstractPrimitiveNode implements TernaryPrimitive {

        @Specialization
        protected static final Object doIsInstantiable(@SuppressWarnings("unused") final Object receiver, final Object object, final ArrayObject argumentArray,
                        @Cached final ArrayObjectToObjectArrayCopyNode getObjectArrayNode,
                        @Cached final WrapToSqueakNode wrapNode,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return wrapNode.executeWrap(lib.instantiate(object, getObjectArrayNode.execute(argumentArray)));
            } catch (final Exception e) {
                /*
                 * Workaround: catch all exceptions raised by other languages to avoid crashes (see
                 * https://github.com/oracle/truffleruby/issues/1864).
                 */
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    /*
     * Value objects
     */

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsBoolean")
    protected abstract static class PrimIsBooleanNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {

        @Specialization
        protected static final boolean doIsBoolean(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isBoolean(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAsBoolean")
    protected abstract static class PrimAsBooleanNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        @Specialization(guards = "lib.isBoolean(object)", limit = "2")
        protected static final boolean doAsBoolean(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary("object") final InteropLibrary lib) {
            try {
                return BooleanObject.wrap(lib.asBoolean(object));
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsString")
    protected abstract static class PrimIsStringNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {

        @Specialization
        protected static final boolean doIsString(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isString(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAsString")
    protected abstract static class PrimAsStringNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        @Specialization(guards = "lib.isString(object)", limit = "2")
        protected static final NativeObject doAsString(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary("object") final InteropLibrary lib,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            try {
                return image.asByteString(lib.asString(object));
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFitsInLong")
    protected abstract static class PrimFitsInLongNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {

        @Specialization
        protected static final boolean doFitsInLong(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.fitsInLong(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAsLong")
    protected abstract static class PrimAsLongNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        @Specialization(guards = {"lib.fitsInLong(object)"}, limit = "2")
        protected static final long doAsLong(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary("object") final InteropLibrary lib) {
            try {
                return lib.asLong(object);
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFitsInDouble")
    protected abstract static class PrimFitsInDoubleNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {

        @Specialization
        protected static final boolean doFitsInDouble(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.fitsInDouble(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAsDouble")
    protected abstract static class PrimAsDoubleNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        @Specialization(guards = {"lib.fitsInDouble(object)"}, limit = "2")
        protected static final double doAsDouble(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary("object") final InteropLibrary lib) {
            try {
                return lib.asDouble(object);
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsExecutable")
    protected abstract static class PrimIsExecutableNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {

        @Specialization
        protected static final boolean doIsExecutable(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isExecutable(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsNull")
    protected abstract static class PrimIsNullNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {

        @Specialization
        protected static final boolean doIsNull(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isNull(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsPointer")
    protected abstract static class PrimIsPointerNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {

        @Specialization
        protected static final boolean doIsPointer(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isPointer(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAsPointer")
    protected abstract static class PrimAsPointerNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        @Specialization(guards = {"lib.isPointer(object)"}, limit = "2")
        protected static final long doAsPointer(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary("object") final InteropLibrary lib) {
            try {
                return lib.asPointer(object);
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    /*
     * Array-like objects
     */

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetArraySize")
    protected abstract static class PrimGetArraySizeNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        @Specialization(guards = "lib.hasArrayElements(object)", limit = "2")
        protected static final long doGetArraySize(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary("object") final InteropLibrary lib) {
            try {
                return lib.getArraySize(object);
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveHasArrayElements")
    protected abstract static class PrimHasArrayElementsNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {

        @Specialization
        protected static final boolean doHasArrayElements(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.hasArrayElements(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsArrayElementExisting")
    protected abstract static class PrimIsArrayElementExistingNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        @Specialization
        protected static final boolean doIsArrayElementExisting(@SuppressWarnings("unused") final Object receiver, final Object object, final long index,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isArrayElementExisting(object, index - 1));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsArrayElementInsertable")
    protected abstract static class PrimIsArrayElementInsertableNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        @Specialization
        protected static final boolean doIsArrayElementInsertable(@SuppressWarnings("unused") final Object receiver, final Object object, final long index,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isArrayElementInsertable(object, index - 1));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsArrayElementModifiable")
    protected abstract static class PrimIsArrayElementModifiableNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        @Specialization
        protected static final boolean doIsArrayElementModifiable(@SuppressWarnings("unused") final Object receiver, final Object object, final long index,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isArrayElementModifiable(object, index - 1));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsArrayElementReadable")
    protected abstract static class PrimIsArrayElementReadableNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        @Specialization
        protected static final boolean doIsArrayElementReadable(@SuppressWarnings("unused") final Object receiver, final Object object, final long index,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isArrayElementReadable(object, index - 1));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsArrayElementRemovable")
    protected abstract static class PrimIsArrayElementRemovableNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        @Specialization
        protected static final boolean doIsArrayElementRemovable(@SuppressWarnings("unused") final Object receiver, final Object object, final long index,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isArrayElementRemovable(object, index - 1));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsArrayElementWritable")
    protected abstract static class PrimIsArrayElementWritableNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        @Specialization
        protected static final boolean doIsArrayElementWritable(@SuppressWarnings("unused") final Object receiver, final Object object, final long index,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isArrayElementWritable(object, index - 1));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveReadArrayElement")
    protected abstract static class PrimReadArrayElementNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        @Specialization(guards = {"lib.isArrayElementReadable(object, to0(index))"}, limit = "2")
        protected static final Object doReadArrayElement(@SuppressWarnings("unused") final Object receiver, final Object object, final long index,
                        @Cached final WrapToSqueakNode wrapNode,
                        @CachedLibrary("object") final InteropLibrary lib) {
            try {
                return wrapNode.executeWrap(lib.readArrayElement(object, index - 1));
            } catch (final UnsupportedMessageException | InvalidArrayIndexException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveRemoveArrayElement")
    protected abstract static class PrimRemoveArrayElementNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        @Specialization(guards = {"lib.isArrayElementRemovable(object, to0(index))"}, limit = "2")
        protected static final Object doRemoveArrayElement(@SuppressWarnings("unused") final Object receiver, final Object object, final long index,
                        @CachedLibrary("object") final InteropLibrary lib) {
            try {
                lib.removeArrayElement(object, index - 1);
                return object;
            } catch (final UnsupportedMessageException | InvalidArrayIndexException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveWriteArrayElement")
    protected abstract static class PrimWriteArrayElementNode extends AbstractPrimitiveNode implements QuaternaryPrimitive {
        @Specialization(guards = {"lib.isArrayElementWritable(object, index)"}, limit = "2")
        protected static final Object doWrite(@SuppressWarnings("unused") final Object receiver, final Object object, final long index, final Object value,
                        @CachedLibrary("object") final InteropLibrary lib) {
            try {
                lib.writeArrayElement(object, index - 1, value);
                return value;
            } catch (InvalidArrayIndexException | UnsupportedMessageException | UnsupportedTypeException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    /*
     * Dictionary-like objects
     */

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetMembers")
    protected abstract static class PrimGetMembersNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        @Specialization(guards = {"lib.hasMembers(object)"}, limit = "2")
        protected static final ArrayObject doGetMembers(@SuppressWarnings("unused") final Object receiver, final Object object, final boolean includeInternal,
                        @CachedLibrary("object") final InteropLibrary lib,
                        @CachedLibrary(limit = "2") final InteropLibrary membersLib,
                        @CachedLibrary(limit = "2") final InteropLibrary memberNameLib,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            // TODO: is unpacking really necessary?
            try {
                final Object members = lib.getMembers(object, includeInternal);
                final int size = (int) membersLib.getArraySize(members);
                final Object[] byteStrings = new Object[size];
                for (int i = 0; i < size; i++) {
                    final Object memberName = membersLib.readArrayElement(members, i);
                    byteStrings[i] = image.asByteString(memberNameLib.asString(memberName));
                }
                return image.asArrayOfObjects(byteStrings);
            } catch (final UnsupportedMessageException | InvalidArrayIndexException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetMemberSize")
    protected abstract static class PrimGetMemberSizeNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        @Specialization(guards = {"lib.hasMembers(object)"}, limit = "2")
        protected static final Object doGetMembers(@SuppressWarnings("unused") final Object receiver, final Object object, final boolean includeInternal,
                        @CachedLibrary("object") final InteropLibrary lib,
                        @CachedLibrary(limit = "2") final InteropLibrary sizeLib) {
            try {
                return sizeLib.getArraySize(lib.getMembers(object, includeInternal));
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveHasMembers")
    protected abstract static class PrimHasMembersNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {

        @Specialization
        protected static final boolean doHasArrayElements(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.hasMembers(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveHasMemberReadSideEffects")
    protected abstract static class PrimHasMemberReadSideEffectsNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        @Specialization(guards = {"member.isByteType()"})
        protected static final boolean doHasMemberReadSideEffects(@SuppressWarnings("unused") final Object receiver, final Object object, final NativeObject member,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.hasMemberReadSideEffects(object, member.asStringUnsafe()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveHasMemberWriteSideEffects")
    protected abstract static class PrimHasMemberWriteSideEffectsNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        @Specialization(guards = {"member.isByteType()"})
        protected static final boolean doHasMemberWriteSideEffects(@SuppressWarnings("unused") final Object receiver, final Object object, final NativeObject member,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.hasMemberWriteSideEffects(object, member.asStringUnsafe()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsMemberExisting")
    protected abstract static class PrimIsMemberExistingNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        @Specialization(guards = {"member.isByteType()"})
        protected static final boolean doIsMemberExisting(@SuppressWarnings("unused") final Object receiver, final Object object, final NativeObject member,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isMemberExisting(object, member.asStringUnsafe()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsMemberInsertable")
    protected abstract static class PrimIsMemberInsertableNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        @Specialization(guards = {"member.isByteType()"})
        protected static final boolean doIsMemberInsertable(@SuppressWarnings("unused") final Object receiver, final Object object, final NativeObject member,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isMemberInsertable(object, member.asStringUnsafe()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsMemberInternal")
    protected abstract static class PrimIsMemberInternalNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        @Specialization(guards = {"member.isByteType()"})
        protected static final boolean doIsMemberInternal(@SuppressWarnings("unused") final Object receiver, final Object object, final NativeObject member,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isMemberInternal(object, member.asStringUnsafe()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsMemberInvocable")
    protected abstract static class PrimIsMemberInvocableNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        @Specialization(guards = {"member.isByteType()"})
        protected static final boolean doIsMemberInvocable(@SuppressWarnings("unused") final Object receiver, final Object object, final NativeObject member,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isMemberInvocable(object, member.asStringUnsafe()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsMemberModifiable")
    protected abstract static class PrimIsMemberModifiableNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        @Specialization(guards = {"member.isByteType()"})
        protected static final boolean doIsMemberModifiable(@SuppressWarnings("unused") final Object receiver, final Object object, final NativeObject member,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isMemberModifiable(object, member.asStringUnsafe()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsMemberReadable")
    protected abstract static class PrimIsMemberReadableNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        @Specialization(guards = {"member.isByteType()"})
        protected static final boolean doIsMemberReadable(@SuppressWarnings("unused") final Object receiver, final Object object, final NativeObject member,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isMemberReadable(object, member.asStringUnsafe()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsMemberRemovable")
    protected abstract static class PrimIsMemberRemovableNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        @Specialization(guards = {"member.isByteType()"})
        protected static final boolean doIsMemberRemovable(@SuppressWarnings("unused") final Object receiver, final Object object, final NativeObject member,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isMemberRemovable(object, member.asStringUnsafe()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsMemberWritable")
    protected abstract static class PrimIsMemberWritableNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        @Specialization(guards = {"member.isByteType()"})
        protected static final boolean doIsMemberWritable(@SuppressWarnings("unused") final Object receiver, final Object object, final NativeObject member,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isMemberWritable(object, member.asStringUnsafe()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveInvokeMember")
    protected abstract static class PrimInvokeMemberNode extends AbstractPrimitiveNode implements QuaternaryPrimitive {
        @Specialization(guards = {"member.isByteType()", "lib.isMemberInvocable(object, member.asStringUnsafe())"}, limit = "2")
        protected static final Object doInvokeMember(@SuppressWarnings("unused") final Object receiver, final Object object, final NativeObject member, final ArrayObject argumentArray,
                        @Cached final ArrayObjectToObjectArrayCopyNode getObjectArrayNode,
                        @Cached final WrapToSqueakNode wrapNode,
                        @CachedLibrary("object") final InteropLibrary lib) {
            try {
                return wrapNode.executeWrap(lib.invokeMember(object, member.asStringUnsafe(), getObjectArrayNode.execute(argumentArray)));
            } catch (final Exception e) {
                /*
                 * Workaround: catch all exceptions raised by other languages to avoid crashes (see
                 * https://github.com/oracle/truffleruby/issues/1864).
                 */
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveReadMember")
    protected abstract static class PrimReadMemberNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        @Specialization(guards = {"member.isByteType()", "lib.isMemberReadable(object, member.asStringUnsafe())",
                        "!lib.hasMemberReadSideEffects(object, member.asStringUnsafe())"}, limit = "2")
        protected static final Object doReadMember(@SuppressWarnings("unused") final Object receiver, final Object object, final NativeObject member,
                        @Cached final WrapToSqueakNode wrapNode,
                        @CachedLibrary("object") final InteropLibrary lib) {
            try {
                return wrapNode.executeWrap(lib.readMember(object, member.asStringUnsafe()));
            } catch (UnknownIdentifierException | UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"member.isByteType()", "lib.isMemberReadable(object, member.asStringUnsafe())",
                        "lib.hasMemberReadSideEffects(object, member.asStringUnsafe())"}, limit = "2")
        protected static final NativeObject doReadMemberWithSideEffects(@SuppressWarnings("unused") final Object receiver, final Object object, final NativeObject member,
                        @CachedLibrary("object") final InteropLibrary lib,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return image.asByteString("[side-effect]");
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveRemoveMember")
    protected abstract static class PrimRemoveMemberNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        @Specialization(guards = {"member.isByteType()", "lib.isMemberRemovable(object, member.asStringUnsafe())"}, limit = "2")
        protected static final Object doRemove(@SuppressWarnings("unused") final Object receiver, final Object object, final NativeObject member,
                        @CachedLibrary("object") final InteropLibrary lib) {
            final String identifier = member.asStringUnsafe();
            try {
                lib.removeMember(object, identifier);
                return object;
            } catch (UnknownIdentifierException | UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveWriteMember")
    protected abstract static class PrimWriteMemberNode extends AbstractPrimitiveNode implements QuaternaryPrimitive {
        @Specialization(guards = {"member.isByteType()", "lib.isMemberWritable(object, member.asStringUnsafe())"}, limit = "2")
        protected static final Object doWrite(@SuppressWarnings("unused") final Object receiver, final Object object, final NativeObject member, final Object value,
                        @CachedLibrary("object") final InteropLibrary lib) {
            try {
                lib.writeMember(object, member.asStringUnsafe(), value);
                return value;
            } catch (UnknownIdentifierException | UnsupportedMessageException | UnsupportedTypeException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    /*
     * Time/Date-related objects
     */

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsDate")
    protected abstract static class PrimIsDateNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {

        @Specialization
        protected static final boolean doIsDate(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isDate(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAsDate")
    protected abstract static class PrimAsDateNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {

        @Specialization(guards = "lib.isDate(object)")
        protected static final Object doAsDate(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return image.env.asGuestValue(lib.asDate(object));
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsDuration")
    protected abstract static class PrimIsDurationNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {

        @Specialization
        protected static final boolean doIsDuration(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isDuration(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAsDuration")
    protected abstract static class PrimAsDurationNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {

        @Specialization(guards = "lib.isDuration(object)")
        protected static final Object doAsDuration(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return image.env.asGuestValue(lib.asDuration(object));
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsInstant")
    protected abstract static class PrimIsInstantNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {

        @Specialization
        protected static final boolean doIsInstant(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isInstant(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAsInstant")
    protected abstract static class PrimAsInstantNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {

        @Specialization(guards = "lib.isInstant(object)")
        protected static final Object doAsInstant(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return image.env.asGuestValue(lib.asInstant(object));
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsTime")
    protected abstract static class PrimIsTimeNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {

        @Specialization
        protected static final boolean doIsTime(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isTime(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAsTime")
    protected abstract static class PrimAsTimeNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {

        @Specialization(guards = "lib.isTime(object)")
        protected static final Object doAsTime(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return image.env.asGuestValue(lib.asTime(object));
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsTimeZone")
    protected abstract static class PrimIsTimeZoneNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {

        @Specialization
        protected static final boolean doIsTimeZone(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isTimeZone(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAsTimeZone")
    protected abstract static class PrimAsTimeZoneNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {

        @Specialization(guards = "lib.isTimeZone(object)")
        protected static final Object doAsTimeZone(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return image.env.asGuestValue(lib.asTimeZone(object));
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    /* Meta-data APIs */

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveHasLanguage")
    protected abstract static class PrimHasLanguageNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {
        @Specialization
        protected static final boolean hasLanguage(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return lib.hasLanguage(object);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetLanguage")
    protected abstract static class PrimGetLanguageNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {
        @Specialization(guards = "lib.hasLanguage(object)")
        protected static final Object getLanguage(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return image.env.asGuestValue(lib.getLanguage(object));
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveToDisplayString")
    protected abstract static class PrimToDisplayStringNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {
        @Specialization
        protected static final Object toDisplayString(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return lib.toDisplayString(object);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveHasMetaObject")
    protected abstract static class PrimHasMetaObjectNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {
        @Specialization
        protected static final boolean hasMetaObject(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return lib.hasMetaObject(object);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsMetaObject")
    protected abstract static class PrimIsMetaObjectNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {
        @Specialization
        protected static final boolean isMetaObject(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return lib.isMetaObject(object);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsMetaInstance")
    protected abstract static class PrimIsMetaInstanceNode extends AbstractPrimitiveNode implements TernaryPrimitiveWithoutFallback {
        @Specialization(guards = "lib.isMetaObject(object)")
        protected static final boolean isMetaInstance(@SuppressWarnings("unused") final Object receiver, final Object object, final Object instance,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return lib.isMetaInstance(object, instance);
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetMetaObject")
    protected abstract static class PrimGetMetaObjectNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {
        @Specialization(guards = "lib.hasMetaObject(object)")
        protected static final Object getMetaObject(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return lib.getMetaObject(object);
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetMetaQualifiedName")
    protected abstract static class PrimGetMetaQualifiedNameNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {
        @Specialization(guards = "lib.isMetaObject(object)")
        protected static final Object getMetaQualifiedName(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return lib.getMetaQualifiedName(object);
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetMetaSimpleName")
    protected abstract static class PrimGetMetaSimpleNameNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {
        @Specialization(guards = "lib.isMetaObject(object)")
        protected static final Object getMetaSimpleName(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return lib.getMetaSimpleName(object);
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveHasSourceLocation")
    protected abstract static class PrimHasSourceLocationNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {
        @Specialization
        protected static final boolean hasSourceLocation(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return lib.hasSourceLocation(object);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetSourceLocation")
    protected abstract static class PrimGetSourceLocationNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {
        @Specialization(guards = "lib.hasSourceLocation(object)")
        protected static final Object getSourceLocation(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return JavaObjectWrapper.wrap(lib.getSourceLocation(object));
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    /*
     * Exception objects
     */

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsException")
    protected abstract static class PrimIsExceptionNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {

        @Specialization
        protected static final boolean doIsException(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isException(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveThrowException")
    protected abstract static class PrimThrowExceptionNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {

        @Specialization(guards = "lib.isException(object)")
        protected static final Object doThrowException(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                throw lib.throwException(object);
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveThrowSyntaxError")
    protected abstract static class PrimThrowSyntaxErrorNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {

        @Specialization(guards = {"messageObject.isByteType()", "sourceObject.isByteType()"})
        protected static final Object doThrowSyntaxError(@SuppressWarnings("unused") final Object receiver, final NativeObject messageObject, final long position, final NativeObject sourceObject) {
            final String message = messageObject.asStringUnsafe();
            final String source = sourceObject.asStringUnsafe();
            throw new SqueakSyntaxError(message, (int) position, source);
        }
    }

    /*
     * Java interop
     */

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAddToHostClassPath")
    protected abstract static class PrimAddToHostClassPathNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        @Specialization(guards = {"value.isByteType()"})
        protected static final Object doAddToHostClassPath(final Object receiver, final NativeObject value,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            final String path = value.asStringUnsafe();
            try {
                image.env.addToHostClassPath(image.env.getPublicTruffleFile(path));
                return receiver;
            } catch (final SecurityException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsHostLookupAllowed")
    protected abstract static class PrimIsHostLookupAllowedNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        @Specialization
        protected static final Object doLookupHostSymbol(@SuppressWarnings("unused") final Object receiver,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return BooleanObject.wrap(image.env.isHostLookupAllowed());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveLookupHostSymbol")
    protected abstract static class PrimLookupHostSymbolNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        @Specialization(guards = {"isHostLookupAllowed(image)", "value.isByteType()"})
        protected static final Object doLookupHostSymbol(@SuppressWarnings("unused") final Object receiver, final NativeObject value,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            final String symbolName = value.asStringUnsafe();
            try {
                return NilObject.nullToNil(image.env.lookupHostSymbol(symbolName));
            } catch (final RuntimeException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }

        @SuppressWarnings("static-method") // Work around code generation problems.
        protected final boolean isHostLookupAllowed(final SqueakImageContext image) {
            return image.env.isHostLookupAllowed();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsHostFunction")
    protected abstract static class PrimIsHostFunctionNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {
        @Specialization
        protected static final boolean doIsHostFunction(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return BooleanObject.wrap(image.env.isHostFunction(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsHostObject")
    protected abstract static class PrimIsHostObjectNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {
        @Specialization
        protected static final boolean doIsHostObject(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return BooleanObject.wrap(image.env.isHostObject(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsHostSymbol")
    protected abstract static class PrimIsHostSymbolNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {
        @Specialization
        protected static final boolean doIsHostSymbol(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return BooleanObject.wrap(image.env.isHostSymbol(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIdentityHash")
    protected abstract static class PrimPolyglotIdentityHashNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {

        @Specialization
        protected static final long doIdentityHash(@SuppressWarnings("unused") final Object receiver, final Object object) {
            return object.hashCode() & AbstractSqueakObjectWithHash.IDENTITY_HASH_MASK;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveStringRepresentation")
    protected abstract static class PrimStringRepresentationNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {

        @Specialization
        protected static final NativeObject doString(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return image.asByteString(MiscUtils.toString(object));
        }
    }

    /*
     * Error handling
     */

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetLastError")
    protected abstract static class PrimGetLastErrorNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        protected static Exception lastError = new Exception("No exception");

        @Specialization
        @TruffleBoundary
        protected static final NativeObject doGetLastError(@SuppressWarnings("unused") final Object receiver,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return image.asByteString(lastError.toString());
        }

        protected static final void setLastError(final Exception e) {
            LogUtils.INTEROP.fine(() -> MiscUtils.toString(e));
            lastError = e;
        }
    }

    /*
     * Helper functions.
     */

    private static PrimitiveFailed primitiveFailedInInterpreterCapturing(final Exception e) {
        CompilerDirectives.transferToInterpreter();
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
