/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Map;

import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive2WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive3WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive4WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive5WithFallback;
import org.graalvm.polyglot.Engine;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleContext;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.api.interop.ExceptionType;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.InvalidBufferOffsetException;
import com.oracle.truffle.api.interop.StopIterationException;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnknownKeyException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.LanguageInfo;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.Source.LiteralBuilder;
import com.oracle.truffle.api.source.Source.SourceBuilder;
import com.oracle.truffle.api.utilities.TriState;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakExceptionWrapper;
import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakSyntaxError;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.interop.ConvertToSqueakNode;
import de.hpi.swa.trufflesqueak.interop.JavaObjectWrapper;
import de.hpi.swa.trufflesqueak.interop.WrapToSqueakNode;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.LargeIntegerObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodes.ArrayObjectSizeNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodes.ArrayObjectToObjectArrayCopyNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive0;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive0WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive2;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.LogUtils;
import de.hpi.swa.trufflesqueak.util.MiscUtils;
import de.hpi.swa.trufflesqueak.util.ReflectionUtils;
import de.hpi.swa.trufflesqueak.util.UnsafeUtils;

public final class PolyglotPlugin extends AbstractPrimitiveFactoryHolder {
    private static final String EVAL_SOURCE_NAME = "<eval>";
    private static final String INSTRUMENT_ID = "trufflesqueak-polyglot-instrument";
    private static final String INSTRUMENT_NAME = "TruffleSqueak Polyglot Instrument";
    @CompilationFinal private static TruffleInstrument.Env instrumentEnv;

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
    protected abstract static class PrimRegisterForeignObjectClassNode extends AbstractPrimitiveNode implements Primitive0WithFallback {
        @Specialization
        protected final boolean doRegisterForeignObjectClass(final ClassObject foreignObjectClass) {
            return BooleanObject.wrap(getContext().setForeignObjectClass(foreignObjectClass));
        }
    }

    /*
     * Code evaluation
     */

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsPolyglotEvalAllowed")
    protected abstract static class PrimIsPolyglotEvalAllowedNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @TruffleBoundary
        @Specialization(guards = "languageId.isByteType()")
        protected final boolean doIsPolyglotEvalAllowed(@SuppressWarnings("unused") final Object receiver, final NativeObject languageId) {
            final TruffleLanguage.Env env = getContext().env;
            return BooleanObject.wrap(env.isPolyglotEvalAllowed(env.getPublicLanguages().get(languageId.asStringUnsafe())));
        }
    }

    protected abstract static class AbstractEvalStringPrimitiveNode extends AbstractPrimitiveNode {
        protected static final Object evalString(final Node node, final NativeObject languageIdOrMimeTypeObj, final NativeObject sourceObject) {
            return evalString(node, languageIdOrMimeTypeObj, sourceObject, ArrayUtils.EMPTY_STRINGS_ARRAY, ArrayUtils.EMPTY_ARRAY);
        }

        protected static final Object evalString(final Node node, final NativeObject languageIdOrMimeTypeObj, final NativeObject sourceObject,
                        final String[] argumentNames, final Object[] argumentValues) {
            final String languageIdOrMimeType = languageIdOrMimeTypeObj.asStringUnsafe();
            final String sourceText = sourceObject.asStringUnsafe();
            final TruffleLanguage.Env env = getContext(node).env;
            try {
                final boolean mimeType = isMimeType(languageIdOrMimeType);
                final String lang = mimeType ? findLanguageByMimeType(env, languageIdOrMimeType) : languageIdOrMimeType;
                LiteralBuilder newBuilder = Source.newBuilder(lang, sourceText, EVAL_SOURCE_NAME);
                if (mimeType) {
                    newBuilder = newBuilder.mimeType(languageIdOrMimeType);
                }
                final Source source = newBuilder.build();
                return env.parsePublic(source, argumentNames).call(argumentValues);
            } catch (final RuntimeException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveEvalString")
    protected abstract static class PrimEvalStringNode extends AbstractEvalStringPrimitiveNode implements Primitive2WithFallback {
        @TruffleBoundary(transferToInterpreterOnException = false)
        @Specialization(guards = {"languageIdOrMimeTypeObj.isByteType()", "sourceObject.isByteType()"})
        protected final Object doEval(@SuppressWarnings("unused") final Object receiver, final NativeObject languageIdOrMimeTypeObj, final NativeObject sourceObject,
                        @Bind final Node node,
                        @Cached final WrapToSqueakNode wrapNode) {
            return wrapNode.executeWrap(node, evalString(this, languageIdOrMimeTypeObj, sourceObject));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveEvalStringWithArguments")
    protected abstract static class PrimEvalStringWithArgumentsNode extends AbstractEvalStringPrimitiveNode implements Primitive4WithFallback {
        @TruffleBoundary(transferToInterpreterOnException = false)
        @Specialization(guards = {"languageIdOrMimeTypeObj.isByteType()", "sourceObject.isByteType()", "sizeNode.execute(node, argumentNames) == sizeNode.execute(node, argumentValues)"}, limit = "1")
        protected static final Object doEvalWithArguments(@SuppressWarnings("unused") final Object receiver, final NativeObject languageIdOrMimeTypeObj, final NativeObject sourceObject,
                        final ArrayObject argumentNames, final ArrayObject argumentValues,
                        @Bind final Node node,
                        @SuppressWarnings("unused") @Cached final ArrayObjectSizeNode sizeNode,
                        @Cached final ArrayObjectToObjectArrayCopyNode toObjectArrayNode,
                        @Cached final WrapToSqueakNode wrapNode) {
            final Object[] namesObjects = toObjectArrayNode.execute(node, argumentNames);
            final String[] names = new String[namesObjects.length];
            for (int i = 0; i < namesObjects.length; i++) {
                final Object name = namesObjects[i];
                if (name instanceof final NativeObject o) {
                    names[i] = o.asStringUnsafe();
                } else {
                    throw PrimitiveFailed.andTransferToInterpreter();
                }
            }
            final Object[] values = toObjectArrayNode.execute(node, argumentValues);
            return wrapNode.executeWrap(node, evalString(node, languageIdOrMimeTypeObj, sourceObject, names, values));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveEvalStringInInnerContext")
    protected abstract static class PrimEvalStringInInnerContextNode extends AbstractEvalStringPrimitiveNode implements Primitive2WithFallback {
        @TruffleBoundary(transferToInterpreterOnException = false)
        @Specialization(guards = {"languageIdOrMimeTypeObj.isByteType()", "sourceObject.isByteType()"})
        protected final Object doEvalInInnerContext(@SuppressWarnings("unused") final Object receiver, final NativeObject languageIdOrMimeTypeObj, final NativeObject sourceObject,
                        @Bind final Node node,
                        @Cached final ConvertToSqueakNode convertNode) {
            final TruffleContext innerContext = getContext().env.newInnerContextBuilder().initializeCreatorContext(true).inheritAllAccess(true).build();
            final Object p = innerContext.enter(this);
            try {
                return convertNode.executeConvert(node, evalString(null, languageIdOrMimeTypeObj, sourceObject));
            } finally {
                innerContext.leave(this, p);
                innerContext.close();
            }
        }
    }

    protected abstract static class AbstractEvalFilePrimitiveNode extends AbstractPrimitiveNode {
        protected final Object evalFile(final NativeObject languageIdOrMimeTypeObj, final NativeObject path) {
            final String languageIdOrMimeType = languageIdOrMimeTypeObj.asStringUnsafe();
            final String pathString = path.asStringUnsafe();
            final TruffleLanguage.Env env = getContext().env;
            try {
                final boolean mimeType = isMimeType(languageIdOrMimeType);
                final String lang = mimeType ? findLanguageByMimeType(env, languageIdOrMimeType) : languageIdOrMimeType;
                SourceBuilder newBuilder = Source.newBuilder(lang, env.getPublicTruffleFile(pathString));
                if (mimeType) {
                    newBuilder = newBuilder.mimeType(languageIdOrMimeType);
                }
                return env.parsePublic(newBuilder.name(pathString).build()).call();
            } catch (IOException | RuntimeException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveEvalFile")
    protected abstract static class PrimEvalFileNode extends AbstractEvalFilePrimitiveNode implements Primitive2WithFallback {
        @TruffleBoundary(transferToInterpreterOnException = false)
        @Specialization(guards = {"languageIdOrMimeTypeObj.isByteType()", "path.isByteType()"})
        protected final Object doEval(@SuppressWarnings("unused") final Object receiver, final NativeObject languageIdOrMimeTypeObj, final NativeObject path,
                        @Bind final Node node,
                        @Cached final WrapToSqueakNode wrapNode) {
            return wrapNode.executeWrap(node, evalFile(languageIdOrMimeTypeObj, path));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveEvalFileInInnerContext")
    protected abstract static class PrimEvalFileInInnerContextNode extends AbstractEvalFilePrimitiveNode implements Primitive2WithFallback {
        @TruffleBoundary(transferToInterpreterOnException = false)
        @Specialization(guards = {"languageIdOrMimeTypeObj.isByteType()", "path.isByteType()"})
        protected final Object doEvalInInnerContext(@SuppressWarnings("unused") final Object receiver, final NativeObject languageIdOrMimeTypeObj, final NativeObject path,
                        @Bind final Node node,
                        @Cached final ConvertToSqueakNode convertNode) {
            final TruffleContext innerContext = getContext().env.newInnerContextBuilder().initializeCreatorContext(true).inheritAllAccess(true).build();
            final Object p = innerContext.enter(null);
            try {
                return convertNode.executeConvert(node, evalFile(languageIdOrMimeTypeObj, path));
            } finally {
                innerContext.leave(null, p);
                innerContext.close();
            }
        }
    }

    /*
     * Language information
     */

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetPublicLanguages")
    protected abstract static class PrimGetPublicLanguagesNode extends AbstractPrimitiveNode implements Primitive0 {
        @CompilationFinal(dimensions = 1) private static Object[] cachedList;

        @Specialization
        protected final ArrayObject doGet(@SuppressWarnings("unused") final Object receiver) {
            final SqueakImageContext image = getContext();
            if (cachedList == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                cachedList = image.env.getPublicLanguages().values().stream().map(JavaObjectWrapper::wrap).toArray();
            }
            return image.asArrayOfObjects(cachedList.clone());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetPublicLanguageInfo")
    protected abstract static class PrimGetPublicLanguageInfoNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @TruffleBoundary
        @Specialization(guards = "languageId.isByteType()")
        protected final Object doGet(@SuppressWarnings("unused") final Object receiver, final NativeObject languageId) {
            return JavaObjectWrapper.wrap(getContext().env.getPublicLanguages().get(languageId.asStringUnsafe()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetInternalLanguages")
    protected abstract static class PrimGetLanguagesNode extends AbstractPrimitiveNode implements Primitive0 {
        @CompilationFinal(dimensions = 1) private static Object[] cachedList;

        @Specialization
        @TruffleBoundary
        protected final ArrayObject doGet(@SuppressWarnings("unused") final Object receiver) {
            final SqueakImageContext image = getContext();
            if (cachedList == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                cachedList = image.env.getInternalLanguages().values().stream().map(JavaObjectWrapper::wrap).toArray();
            }
            return image.asArrayOfObjects(cachedList.clone());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetInternalLanguageInfo")
    protected abstract static class PrimGetInternalLanguageInfoNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @TruffleBoundary
        @Specialization(guards = "languageId.isByteType()")
        protected final Object doGet(@SuppressWarnings("unused") final Object receiver, final NativeObject languageId) {
            return JavaObjectWrapper.wrap(getContext().env.getInternalLanguages().get(languageId.asStringUnsafe()));
        }
    }

    /*
     * Interaction with polyglot bindings object
     */

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsPolyglotBindingsAccessAllowed")
    protected abstract static class PrimIsPolyglotBindingsAccessAllowedNode extends AbstractPrimitiveNode implements Primitive0 {
        @Specialization
        protected final boolean doIsPolyglotBindingsAccessAllowed(@SuppressWarnings("unused") final Object receiver) {
            return BooleanObject.wrap(getContext().env.isPolyglotBindingsAccessAllowed());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetPolyglotBindings")
    protected abstract static class PrimGetPolyglotBindingsNode extends AbstractPrimitiveNode implements Primitive0 {
        @Specialization
        protected final Object doGet(@SuppressWarnings("unused") final Object receiver) {
            final SqueakImageContext image = getContext();
            if (image.env.isPolyglotBindingsAccessAllowed()) {
                return image.env.getPolyglotBindings();
            } else {
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveImport")
    protected abstract static class PrimImportNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "name.isByteType()")
        @TruffleBoundary
        public final Object importSymbol(@SuppressWarnings("unused") final Object receiver, final NativeObject name) {
            try {
                return NilObject.nullToNil(getContext().env.importSymbol(name.asStringUnsafe()));
            } catch (final SecurityException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveExport")
    protected abstract static class PrimExportNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = "name.isByteType()")
        @TruffleBoundary
        public final Object exportSymbol(@SuppressWarnings("unused") final Object receiver, final NativeObject name, final Object value) {
            try {
                getContext().env.exportSymbol(name.asStringUnsafe(), value);
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
    protected abstract static class PrimExecuteNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = {"lib.isExecutable(object)"}, limit = "2")
        protected static final Object doExecute(@SuppressWarnings("unused") final Object receiver, final Object object, final ArrayObject argumentArray,
                        @Bind final Node node,
                        @Cached final ArrayObjectToObjectArrayCopyNode getObjectArrayNode,
                        @Cached final WrapToSqueakNode wrapNode,
                        @CachedLibrary("object") final InteropLibrary lib) {
            try {
                return wrapNode.executeWrap(node, lib.execute(object, getObjectArrayNode.execute(node, argumentArray)));
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
    protected abstract static class PrimIsInstantiableNode extends AbstractPrimitiveNode implements Primitive1 {

        @Specialization
        protected static final boolean doIsInstantiable(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isInstantiable(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveInstantiate")
    protected abstract static class PrimInstantiateNode extends AbstractPrimitiveNode implements Primitive2WithFallback {

        @Specialization
        protected static final Object doIsInstantiable(@SuppressWarnings("unused") final Object receiver, final Object object, final ArrayObject argumentArray,
                        @Bind final Node node,
                        @Cached final ArrayObjectToObjectArrayCopyNode getObjectArrayNode,
                        @Cached final WrapToSqueakNode wrapNode,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return wrapNode.executeWrap(node, lib.instantiate(object, getObjectArrayNode.execute(node, argumentArray)));
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
    protected abstract static class PrimIsBooleanNode extends AbstractPrimitiveNode implements Primitive1 {

        @Specialization
        protected static final boolean doIsBoolean(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isBoolean(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAsBoolean")
    protected abstract static class PrimAsBooleanNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

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
    protected abstract static class PrimIsStringNode extends AbstractPrimitiveNode implements Primitive1 {

        @Specialization
        protected static final boolean doIsString(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isString(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAsString")
    protected abstract static class PrimAsStringNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization(guards = "lib.isString(object)", limit = "2")
        protected final NativeObject doAsString(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary("object") final InteropLibrary lib) {
            try {
                return getContext().asByteString(lib.asString(object));
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsNumber")
    protected abstract static class PrimIsNumberNode extends AbstractPrimitiveNode implements Primitive1 {
        @Specialization
        protected static final boolean doFitsInLong(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isNumber(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFitsInLong")
    protected abstract static class PrimFitsInLongNode extends AbstractPrimitiveNode implements Primitive1 {

        @Specialization
        protected static final boolean doFitsInLong(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.fitsInLong(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAsLong")
    protected abstract static class PrimAsLongNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

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
    protected abstract static class PrimFitsInDoubleNode extends AbstractPrimitiveNode implements Primitive1 {

        @Specialization
        protected static final boolean doFitsInDouble(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.fitsInDouble(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAsDouble")
    protected abstract static class PrimAsDoubleNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

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
    @SqueakPrimitive(names = "primitiveFitsInBigInteger")
    protected abstract static class PrimFitsInBigIntegerNode extends AbstractPrimitiveNode implements Primitive1 {

        @Specialization
        protected static final boolean doFitsInBigInteger(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.fitsInBigInteger(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAsBigInteger")
    protected abstract static class PrimAsBigIntegerNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @TruffleBoundary
        @Specialization(guards = {"lib.fitsInBigInteger(object)"}, limit = "2")
        protected final Object doAsDouble(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary("object") final InteropLibrary lib) {
            try {
                return new LargeIntegerObject(getContext(), lib.asBigInteger(object));
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsExecutable")
    protected abstract static class PrimIsExecutableNode extends AbstractPrimitiveNode implements Primitive1 {
        @Specialization
        protected static final boolean doIsExecutable(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isExecutable(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveHasExecutableName")
    protected abstract static class PrimHasExecutableNameNode extends AbstractPrimitiveNode implements Primitive1 {
        @Specialization
        protected static final boolean doHasExecutableName(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.hasExecutableName(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetExecutableName")
    protected abstract static class PrimGetExecutableNameNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "lib.hasExecutableName(object)")
        protected static final Object doGetExecutableName(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return lib.getExecutableName(object);
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsNull")
    protected abstract static class PrimIsNullNode extends AbstractPrimitiveNode implements Primitive1 {

        @Specialization
        protected static final boolean doIsNull(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isNull(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsPointer")
    protected abstract static class PrimIsPointerNode extends AbstractPrimitiveNode implements Primitive1 {

        @Specialization
        protected static final boolean doIsPointer(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isPointer(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAsPointer")
    protected abstract static class PrimAsPointerNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

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
     * Hashes
     */

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveHasHashEntries")
    protected abstract static class PrimHasHashEntriesNode extends AbstractPrimitiveNode implements Primitive1 {
        @Specialization
        protected static final boolean doHasHashEntries(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.hasHashEntries(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetHashSize")
    protected abstract static class PrimGetHashSizeNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "lib.hasHashEntries(object)")
        protected static final long doGetHashSize(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return lib.getHashSize(object);
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsHashEntryReadable")
    protected abstract static class PrimIsHashEntryReadableNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = "lib.hasHashEntries(object)")
        protected static final boolean doIsHashEntryReadable(@SuppressWarnings("unused") final Object receiver, final Object object, final Object key,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isHashEntryReadable(object, key));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveReadHashValue")
    protected abstract static class PrimReadHashValueNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = "lib.isHashEntryReadable(object, key)")
        protected static final Object doReadHashValue(@SuppressWarnings("unused") final Object receiver, final Object object, final Object key,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return lib.readHashValue(object, key);
            } catch (UnsupportedMessageException | UnknownKeyException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveReadHashValueOrDefault")
    protected abstract static class PrimReadHashValueOrDefaultNode extends AbstractPrimitiveNode implements Primitive3WithFallback {
        @Specialization(guards = "lib.hasHashEntries(object)")
        protected static final Object doReadHashValueOrDefault(@SuppressWarnings("unused") final Object receiver, final Object object, final Object key, final Object defaultValue,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return lib.readHashValueOrDefault(object, key, defaultValue);
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsHashEntryModifiable")
    protected abstract static class PrimIsHashEntryModifiableNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = "lib.hasHashEntries(object)")
        protected static final boolean doIsHashEntryModifiable(@SuppressWarnings("unused") final Object receiver, final Object object, final Object key,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isHashEntryModifiable(object, key));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsHashEntryInsertable")
    protected abstract static class PrimIsHashEntryInsertableNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = "lib.hasHashEntries(object)")
        protected static final boolean doIsHashEntryInsertable(@SuppressWarnings("unused") final Object receiver, final Object object, final Object key,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isHashEntryInsertable(object, key));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsHashEntryWritable")
    protected abstract static class PrimIsHashEntryWritableNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = "lib.hasHashEntries(object)")
        protected static final boolean doIsHashEntryWritable(@SuppressWarnings("unused") final Object receiver, final Object object, final Object key,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isHashEntryWritable(object, key));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveWriteHashEntry")
    protected abstract static class PrimWriteHashEntryNode extends AbstractPrimitiveNode implements Primitive3WithFallback {
        @Specialization(guards = "lib.hasHashEntries(object)")
        protected static final Object doWriteHashEntry(@SuppressWarnings("unused") final Object receiver, final Object object, final Object key, final Object value,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                lib.writeHashEntry(object, key, value);
                return value;
            } catch (final UnsupportedMessageException | UnknownKeyException | UnsupportedTypeException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsHashEntryRemovable")
    protected abstract static class PrimIsHashEntryRemovableNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = "lib.hasHashEntries(object)")
        protected static final boolean doIsHashEntryRemovable(@SuppressWarnings("unused") final Object receiver, final Object object, final Object key,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isHashEntryRemovable(object, key));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveRemoveHashEntry")
    protected abstract static class PrimRemoveHashEntryNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = "lib.hasHashEntries(object)")
        protected static final Object doRemoveHashEntry(@SuppressWarnings("unused") final Object receiver, final Object object, final Object key,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                lib.removeHashEntry(object, key);
            } catch (UnsupportedMessageException | UnknownKeyException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
            return object;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsHashEntryExisting")
    protected abstract static class PrimIsHashEntryExistingNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = "lib.hasHashEntries(object)")
        protected static final boolean doIsHashEntryExisting(@SuppressWarnings("unused") final Object receiver, final Object object, final Object key,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isHashEntryExisting(object, key));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetHashEntriesIterator")
    protected abstract static class PrimGetHashEntriesIteratorNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "lib.hasHashEntries(object)")
        protected static final Object doGetHashEntriesIterator(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return lib.getHashEntriesIterator(object);
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetHashKeysIterator")
    protected abstract static class PrimGetHashKeysIteratorNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "lib.hasHashEntries(object)")
        protected static final Object doGetHashKeysIterator(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return lib.getHashKeysIterator(object);
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetHashValuesIterator")
    protected abstract static class PrimGetHashValuesIteratorNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "lib.hasHashEntries(object)")
        protected static final Object doGetHashValuesIterator(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return lib.getHashValuesIterator(object);
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
    protected abstract static class PrimGetArraySizeNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

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
    protected abstract static class PrimHasArrayElementsNode extends AbstractPrimitiveNode implements Primitive1 {

        @Specialization
        protected static final boolean doHasArrayElements(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.hasArrayElements(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsArrayElementExisting")
    protected abstract static class PrimIsArrayElementExistingNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization
        protected static final boolean doIsArrayElementExisting(@SuppressWarnings("unused") final Object receiver, final Object object, final long index,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isArrayElementExisting(object, index - 1));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsArrayElementInsertable")
    protected abstract static class PrimIsArrayElementInsertableNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization
        protected static final boolean doIsArrayElementInsertable(@SuppressWarnings("unused") final Object receiver, final Object object, final long index,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isArrayElementInsertable(object, index - 1));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsArrayElementModifiable")
    protected abstract static class PrimIsArrayElementModifiableNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization
        protected static final boolean doIsArrayElementModifiable(@SuppressWarnings("unused") final Object receiver, final Object object, final long index,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isArrayElementModifiable(object, index - 1));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsArrayElementReadable")
    protected abstract static class PrimIsArrayElementReadableNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization
        protected static final boolean doIsArrayElementReadable(@SuppressWarnings("unused") final Object receiver, final Object object, final long index,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isArrayElementReadable(object, index - 1));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsArrayElementRemovable")
    protected abstract static class PrimIsArrayElementRemovableNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization
        protected static final boolean doIsArrayElementRemovable(@SuppressWarnings("unused") final Object receiver, final Object object, final long index,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isArrayElementRemovable(object, index - 1));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsArrayElementWritable")
    protected abstract static class PrimIsArrayElementWritableNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization
        protected static final boolean doIsArrayElementWritable(@SuppressWarnings("unused") final Object receiver, final Object object, final long index,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isArrayElementWritable(object, index - 1));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveReadArrayElement")
    protected abstract static class PrimReadArrayElementNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = {"lib.isArrayElementReadable(object, to0(index))"}, limit = "2")
        protected static final Object doReadArrayElement(@SuppressWarnings("unused") final Object receiver, final Object object, final long index,
                        @Bind final Node node,
                        @Cached final WrapToSqueakNode wrapNode,
                        @CachedLibrary("object") final InteropLibrary lib) {
            try {
                return wrapNode.executeWrap(node, lib.readArrayElement(object, index - 1));
            } catch (final UnsupportedMessageException | InvalidArrayIndexException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveRemoveArrayElement")
    protected abstract static class PrimRemoveArrayElementNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
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
    protected abstract static class PrimWriteArrayElementNode extends AbstractPrimitiveNode implements Primitive3WithFallback {
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
     * Members
     */

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetMembers")
    protected abstract static class PrimGetMembersNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = {"lib.hasMembers(object)"}, limit = "2")
        protected final ArrayObject doGetMembers(@SuppressWarnings("unused") final Object receiver, final Object object, final boolean includeInternal,
                        @CachedLibrary("object") final InteropLibrary lib,
                        @CachedLibrary(limit = "2") final InteropLibrary membersLib,
                        @CachedLibrary(limit = "2") final InteropLibrary memberNameLib) {
            // TODO: is unpacking really necessary?
            final SqueakImageContext image = getContext();
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
    protected abstract static class PrimGetMemberSizeNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
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
    protected abstract static class PrimHasMembersNode extends AbstractPrimitiveNode implements Primitive1 {

        @Specialization
        protected static final boolean doHasArrayElements(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.hasMembers(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveHasMemberReadSideEffects")
    protected abstract static class PrimHasMemberReadSideEffectsNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = {"member.isByteType()"})
        protected static final boolean doHasMemberReadSideEffects(@SuppressWarnings("unused") final Object receiver, final Object object, final NativeObject member,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.hasMemberReadSideEffects(object, member.asStringUnsafe()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveHasMemberWriteSideEffects")
    protected abstract static class PrimHasMemberWriteSideEffectsNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = {"member.isByteType()"})
        protected static final boolean doHasMemberWriteSideEffects(@SuppressWarnings("unused") final Object receiver, final Object object, final NativeObject member,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.hasMemberWriteSideEffects(object, member.asStringUnsafe()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsMemberExisting")
    protected abstract static class PrimIsMemberExistingNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = {"member.isByteType()"})
        protected static final boolean doIsMemberExisting(@SuppressWarnings("unused") final Object receiver, final Object object, final NativeObject member,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isMemberExisting(object, member.asStringUnsafe()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsMemberInsertable")
    protected abstract static class PrimIsMemberInsertableNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = {"member.isByteType()"})
        protected static final boolean doIsMemberInsertable(@SuppressWarnings("unused") final Object receiver, final Object object, final NativeObject member,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isMemberInsertable(object, member.asStringUnsafe()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsMemberInternal")
    protected abstract static class PrimIsMemberInternalNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = {"member.isByteType()"})
        protected static final boolean doIsMemberInternal(@SuppressWarnings("unused") final Object receiver, final Object object, final NativeObject member,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isMemberInternal(object, member.asStringUnsafe()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsMemberInvocable")
    protected abstract static class PrimIsMemberInvocableNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = {"member.isByteType()"})
        protected static final boolean doIsMemberInvocable(@SuppressWarnings("unused") final Object receiver, final Object object, final NativeObject member,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isMemberInvocable(object, member.asStringUnsafe()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsMemberModifiable")
    protected abstract static class PrimIsMemberModifiableNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = {"member.isByteType()"})
        protected static final boolean doIsMemberModifiable(@SuppressWarnings("unused") final Object receiver, final Object object, final NativeObject member,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isMemberModifiable(object, member.asStringUnsafe()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsMemberReadable")
    protected abstract static class PrimIsMemberReadableNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = {"member.isByteType()"})
        protected static final boolean doIsMemberReadable(@SuppressWarnings("unused") final Object receiver, final Object object, final NativeObject member,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isMemberReadable(object, member.asStringUnsafe()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsMemberRemovable")
    protected abstract static class PrimIsMemberRemovableNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = {"member.isByteType()"})
        protected static final boolean doIsMemberRemovable(@SuppressWarnings("unused") final Object receiver, final Object object, final NativeObject member,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isMemberRemovable(object, member.asStringUnsafe()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsMemberWritable")
    protected abstract static class PrimIsMemberWritableNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = {"member.isByteType()"})
        protected static final boolean doIsMemberWritable(@SuppressWarnings("unused") final Object receiver, final Object object, final NativeObject member,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isMemberWritable(object, member.asStringUnsafe()));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveInvokeMember")
    protected abstract static class PrimInvokeMemberNode extends AbstractPrimitiveNode implements Primitive3WithFallback {
        @Specialization(guards = {"member.isByteType()", "lib.isMemberInvocable(object, member.asStringUnsafe())"}, limit = "2")
        protected static final Object doInvokeMember(@SuppressWarnings("unused") final Object receiver, final Object object, final NativeObject member, final ArrayObject argumentArray,
                        @Bind final Node node,
                        @Cached final ArrayObjectToObjectArrayCopyNode getObjectArrayNode,
                        @Cached final WrapToSqueakNode wrapNode,
                        @CachedLibrary("object") final InteropLibrary lib) {
            try {
                return wrapNode.executeWrap(node, lib.invokeMember(object, member.asStringUnsafe(), getObjectArrayNode.execute(node, argumentArray)));
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
    protected abstract static class PrimReadMemberNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = {"member.isByteType()", "lib.isMemberReadable(object, member.asStringUnsafe())"}, limit = "2")
        protected static final Object doReadMember(@SuppressWarnings("unused") final Object receiver, final Object object, final NativeObject member,
                        @Bind final Node node,
                        @Cached final WrapToSqueakNode wrapNode,
                        @CachedLibrary("object") final InteropLibrary lib) {
            try {
                return wrapNode.executeWrap(node, lib.readMember(object, member.asStringUnsafe()));
            } catch (UnknownIdentifierException | UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveRemoveMember")
    protected abstract static class PrimRemoveMemberNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
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
    protected abstract static class PrimWriteMemberNode extends AbstractPrimitiveNode implements Primitive3WithFallback {
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
     * Buffers
     */

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveHasBufferElements")
    protected abstract static class PrimHasBufferElementsNode extends AbstractPrimitiveNode implements Primitive1 {
        @Specialization
        protected static final boolean doHasBufferElements(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.hasBufferElements(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsBufferWritable")
    protected abstract static class PrimIsBufferWritableNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "lib.hasBufferElements(object)")
        protected static final boolean doIsBufferWritable(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return BooleanObject.wrap(lib.isBufferWritable(object));
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetBufferSize")
    protected abstract static class PrimGetBufferSizeNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "lib.hasBufferElements(object)")
        protected static final long doGetBufferSize(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return lib.getBufferSize(object);
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveReadBufferByte")
    protected abstract static class PrimReadBufferByteNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = "lib.hasBufferElements(object)")
        protected static final long doReadBufferByte(@SuppressWarnings("unused") final Object receiver, final Object object, final long byteOffset,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return lib.readBufferByte(object, byteOffset);
            } catch (final UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveReadBuffer")
    protected abstract static class PrimReadBufferNode extends AbstractPrimitiveNode implements Primitive5WithFallback {
        @Specialization(guards = {"lib.hasBufferElements(object)", "destination.isByteType()"})
        protected static final Object doReadBuffer(final Object receiver, final Object object, final long byteOffset, final NativeObject destination, final long destinationOffset, final long length,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                lib.readBuffer(object, byteOffset, destination.getByteStorage(), MiscUtils.toIntExact(destinationOffset), MiscUtils.toIntExact(length));
                return receiver;
            } catch (final UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveWriteBufferByte")
    protected abstract static class PrimWriteBufferByteNode extends AbstractPrimitiveNode implements Primitive3WithFallback {
        @Specialization(guards = "lib.hasBufferElements(object)")
        protected static final long doWriteBufferByte(@SuppressWarnings("unused") final Object receiver, final Object object, final long byteOffset, final long value,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                lib.writeBufferByte(object, byteOffset, (byte) value);
                return value;
            } catch (final UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveReadBufferShort")
    protected abstract static class PrimReadBufferShortNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = "lib.hasBufferElements(object)")
        protected static final long doReadBufferShort(@SuppressWarnings("unused") final Object receiver, final Object object, final long byteOffset,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return lib.readBufferShort(object, ByteOrder.nativeOrder(), byteOffset);
            } catch (final UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveWriteBufferShort")
    protected abstract static class PrimWriteBufferShortNode extends AbstractPrimitiveNode implements Primitive3WithFallback {
        @Specialization(guards = "lib.hasBufferElements(object)")
        protected static final long doWriteBufferShort(@SuppressWarnings("unused") final Object receiver, final Object object, final long byteOffset, final long value,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                lib.writeBufferShort(object, ByteOrder.nativeOrder(), byteOffset, (short) value);
                return value;
            } catch (final UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveReadBufferInt")
    protected abstract static class PrimReadBufferIntNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = "lib.hasBufferElements(object)")
        protected static final long doReadBufferInt(@SuppressWarnings("unused") final Object receiver, final Object object, final long byteOffset,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return lib.readBufferInt(object, ByteOrder.nativeOrder(), byteOffset);
            } catch (final UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveWriteBufferInt")
    protected abstract static class PrimWriteBufferIntNode extends AbstractPrimitiveNode implements Primitive3WithFallback {
        @Specialization(guards = "lib.hasBufferElements(object)")
        protected static final long doWriteBufferInt(@SuppressWarnings("unused") final Object receiver, final Object object, final long byteOffset, final long value,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                lib.writeBufferInt(object, ByteOrder.nativeOrder(), byteOffset, (int) value);
                return value;
            } catch (final UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveReadBufferLong")
    protected abstract static class PrimReadBufferLongNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = "lib.hasBufferElements(object)")
        protected static final long doReadBufferLong(@SuppressWarnings("unused") final Object receiver, final Object object, final long byteOffset,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return lib.readBufferLong(object, ByteOrder.nativeOrder(), byteOffset);
            } catch (final UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveWriteBufferLong")
    protected abstract static class PrimWriteBufferLongNode extends AbstractPrimitiveNode implements Primitive3WithFallback {
        @Specialization(guards = "lib.hasBufferElements(object)")
        protected static final long doWriteBufferLong(@SuppressWarnings("unused") final Object receiver, final Object object, final long byteOffset, final long value,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                lib.writeBufferLong(object, ByteOrder.nativeOrder(), byteOffset, value);
                return value;
            } catch (final UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveReadBufferFloat")
    protected abstract static class PrimReadBufferFloatNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = "lib.hasBufferElements(object)")
        protected static final double doReadBufferFloat(@SuppressWarnings("unused") final Object receiver, final Object object, final long byteOffset,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return lib.readBufferFloat(object, ByteOrder.nativeOrder(), byteOffset);
            } catch (final UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveWriteBufferFloat")
    protected abstract static class PrimWriteBufferFloatNode extends AbstractPrimitiveNode implements Primitive3WithFallback {
        @Specialization(guards = "lib.hasBufferElements(object)")
        protected static final double doWriteBufferFloat(@SuppressWarnings("unused") final Object receiver, final Object object, final long byteOffset, final double value,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                lib.writeBufferFloat(object, ByteOrder.nativeOrder(), byteOffset, (float) value);
                return value;
            } catch (final UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveReadBufferDouble")
    protected abstract static class PrimReadBufferDoubleNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = "lib.hasBufferElements(object)")
        protected static final double doReadBufferDouble(@SuppressWarnings("unused") final Object receiver, final Object object, final long byteOffset,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return lib.readBufferDouble(object, ByteOrder.nativeOrder(), byteOffset);
            } catch (final UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveWriteBufferDouble")
    protected abstract static class PrimWriteBufferDoubleNode extends AbstractPrimitiveNode implements Primitive3WithFallback {
        @Specialization(guards = "lib.hasBufferElements(object)")
        protected static final double doWriteBufferDouble(@SuppressWarnings("unused") final Object receiver, final Object object, final long byteOffset, final double value,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                lib.writeBufferDouble(object, ByteOrder.nativeOrder(), byteOffset, value);
                return value;
            } catch (final UnsupportedMessageException | InvalidBufferOffsetException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    /*
     * Time/Date-related objects
     */

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsDate")
    protected abstract static class PrimIsDateNode extends AbstractPrimitiveNode implements Primitive1 {

        @Specialization
        protected static final boolean doIsDate(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isDate(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAsDate")
    protected abstract static class PrimAsDateNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization(guards = "lib.isDate(object)")
        protected final Object doAsDate(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return getContext().env.asGuestValue(lib.asDate(object));
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsDuration")
    protected abstract static class PrimIsDurationNode extends AbstractPrimitiveNode implements Primitive1 {

        @Specialization
        protected static final boolean doIsDuration(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isDuration(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAsDuration")
    protected abstract static class PrimAsDurationNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization(guards = "lib.isDuration(object)")
        protected final Object doAsDuration(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return getContext().env.asGuestValue(lib.asDuration(object));
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsInstant")
    protected abstract static class PrimIsInstantNode extends AbstractPrimitiveNode implements Primitive1 {

        @Specialization
        protected static final boolean doIsInstant(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isInstant(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAsInstant")
    protected abstract static class PrimAsInstantNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization(guards = "lib.isInstant(object)")
        protected final Object doAsInstant(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return getContext().env.asGuestValue(lib.asInstant(object));
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsTime")
    protected abstract static class PrimIsTimeNode extends AbstractPrimitiveNode implements Primitive1 {

        @Specialization
        protected static final boolean doIsTime(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isTime(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAsTime")
    protected abstract static class PrimAsTimeNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization(guards = "lib.isTime(object)")
        protected final Object doAsTime(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return getContext().env.asGuestValue(lib.asTime(object));
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsTimeZone")
    protected abstract static class PrimIsTimeZoneNode extends AbstractPrimitiveNode implements Primitive1 {

        @Specialization
        protected static final boolean doIsTimeZone(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isTimeZone(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAsTimeZone")
    protected abstract static class PrimAsTimeZoneNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization(guards = "lib.isTimeZone(object)")
        protected final Object doAsTimeZone(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return getContext().env.asGuestValue(lib.asTimeZone(object));
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    /* Meta-data APIs */

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveHasLanguage")
    protected abstract static class PrimHasLanguageNode extends AbstractPrimitiveNode implements Primitive1 {
        @Specialization
        protected static final boolean hasLanguage(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return lib.hasLanguage(object);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetLanguage")
    protected abstract static class PrimGetLanguageNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "lib.hasLanguage(object)")
        protected static final Class<? extends TruffleLanguage<?>> getLanguage(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return lib.getLanguage(object);
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetLanguageInfo")
    protected abstract static class PrimGetLanguageInfoNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "lib.hasLanguage(object)")
        protected static final Object getLanguage(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return JavaObjectWrapper.wrap(getInstrumentEnv().getLanguageInfo(lib.getLanguage(object)));
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveToDisplayString")
    protected abstract static class PrimToDisplayStringNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization
        protected static final Object toDisplayString(@SuppressWarnings("unused") final Object receiver, final Object object, final boolean allowSideEffects,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return lib.toDisplayString(object, allowSideEffects);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveHasMetaObject")
    protected abstract static class PrimHasMetaObjectNode extends AbstractPrimitiveNode implements Primitive1 {
        @Specialization
        protected static final boolean hasMetaObject(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.hasMetaObject(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveHasDeclaringMetaObject")
    protected abstract static class PrimHasDeclaringMetaObjectNode extends AbstractPrimitiveNode implements Primitive1 {
        @Specialization
        protected static final boolean hasMetaObject(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.hasDeclaringMetaObject(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsMetaObject")
    protected abstract static class PrimIsMetaObjectNode extends AbstractPrimitiveNode implements Primitive1 {
        @Specialization
        protected static final boolean isMetaObject(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return lib.isMetaObject(object);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsMetaInstance")
    protected abstract static class PrimIsMetaInstanceNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
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
    protected abstract static class PrimGetMetaObjectNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
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
    @SqueakPrimitive(names = "primitiveGetDeclaringMetaObject")
    protected abstract static class PrimGetDeclaringMetaObjectNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "lib.hasDeclaringMetaObject(object)")
        protected static final Object getDeclaringMetaObject(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return lib.getDeclaringMetaObject(object);
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetMetaQualifiedName")
    protected abstract static class PrimGetMetaQualifiedNameNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
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
    protected abstract static class PrimGetMetaSimpleNameNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
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
    @SqueakPrimitive(names = "primitiveHasMetaParents")
    protected abstract static class PrimHasMetaParentsNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "lib.isMetaObject(object)")
        protected static final boolean hasMetaParents(@SuppressWarnings("unused") final Object receiver, final Object object, @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return lib.hasMetaParents(object);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetMetaParents")
    protected abstract static class PrimGetMetaParentsNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "lib.isMetaObject(object)")
        protected static final Object getMetaParents(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return lib.getMetaParents(object);
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveHasSourceLocation")
    protected abstract static class PrimHasSourceLocationNode extends AbstractPrimitiveNode implements Primitive1 {
        @Specialization
        protected static final boolean hasSourceLocation(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return lib.hasSourceLocation(object);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetSourceLocation")
    protected abstract static class PrimGetSourceLocationNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
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

    /* Identity APIs */

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveHasIdentity")
    protected abstract static class PrimHasIdentityNode extends AbstractPrimitiveNode implements Primitive1 {
        @Specialization
        protected static final boolean hasIdentity(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.hasIdentity(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsIdentical")
    protected abstract static class PrimIsIdenticalNode extends AbstractPrimitiveNode implements Primitive2 {
        @Specialization
        protected static final boolean isIdentical(@SuppressWarnings("unused") final Object receiver, final Object object, final Object other,
                        @CachedLibrary(limit = "2") final InteropLibrary leftLib,
                        @CachedLibrary(limit = "2") final InteropLibrary rightLib) {
            return BooleanObject.wrap(leftLib.isIdentical(object, other, rightLib));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIdentityHashCode")
    protected abstract static class PrimIdentityHashCodeNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "lib.hasIdentity(object)")
        protected static final long identityHashCode(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return lib.identityHashCode(object);
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    /*
     * Scope objects
     */

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsScope")
    protected abstract static class PrimIsScopeNode extends AbstractPrimitiveNode implements Primitive1 {
        @Specialization
        protected static final boolean isScope(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isScope(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveHasScopeParent")
    protected abstract static class PrimHasScopeParentNode extends AbstractPrimitiveNode implements Primitive1 {
        @Specialization
        protected static final boolean hasScopeParent(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.hasScopeParent(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetScopeParent")
    protected abstract static class PrimGetScopeParentNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "lib.hasScopeParent(object)")
        protected static final Object getScopeParent(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return lib.getScopeParent(object);
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
    protected abstract static class PrimIsExceptionNode extends AbstractPrimitiveNode implements Primitive1 {

        @Specialization
        protected static final boolean isException(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isException(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsExceptionIncompleteSource")
    protected abstract static class PrimIsExceptionIncompleteSourceNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization(guards = "isParseErrorException(lib, object)")
        protected static final boolean isExceptionIncompleteSource(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return BooleanObject.wrap(lib.isExceptionIncompleteSource(object));
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }

        protected static final boolean isParseErrorException(final InteropLibrary lib, final Object object) {
            try {
                return lib.getExceptionType(object) == ExceptionType.PARSE_ERROR;
            } catch (final UnsupportedMessageException e) {
                return false;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveHasExceptionCause")
    protected abstract static class PrimHasExceptionCauseNode extends AbstractPrimitiveNode implements Primitive1 {

        @Specialization
        protected static final boolean hasExceptionCause(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.hasExceptionCause(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetExceptionCause")
    protected abstract static class PrimGetExceptionCauseNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization(guards = "lib.hasExceptionCause(object)")
        protected static final Object getExceptionCause(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return lib.getExceptionCause(object);
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveHasExceptionMessage")
    protected abstract static class PrimHasExceptionMessageNode extends AbstractPrimitiveNode implements Primitive1 {

        @Specialization
        protected static final boolean hasExceptionMessage(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.hasExceptionMessage(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetExceptionMessage")
    protected abstract static class PrimGetExceptionMessageNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization(guards = "lib.hasExceptionMessage(object)")
        protected static final Object getExceptionMessage(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return lib.getExceptionMessage(object);
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveHasExceptionStackTrace")
    protected abstract static class PrimHasExceptionStackTraceNode extends AbstractPrimitiveNode implements Primitive1 {

        @Specialization
        protected static final boolean hasExceptionStackTrace(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.hasExceptionStackTrace(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetExceptionStackTrace")
    protected abstract static class PrimGetExceptionStackTraceNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization(guards = "lib.hasExceptionStackTrace(object)")
        protected static final Object getExceptionStackTrace(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return lib.getExceptionStackTrace(object);
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @ImportStatic(ExceptionType.class)
    @SqueakPrimitive(names = "primitiveGetExceptionExitStatus")
    protected abstract static class PrimGetExceptionExitStatusNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization(guards = "isExitException(lib, object)")
        protected static final long getExceptionExitStatus(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return lib.getExceptionExitStatus(object);
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }

        protected static final boolean isExitException(final InteropLibrary lib, final Object object) {
            try {
                return lib.getExceptionType(object) == ExceptionType.EXIT;
            } catch (final UnsupportedMessageException e) {
                return false;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetExceptionType")
    protected abstract static class PrimGetExceptionTypeNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization(guards = "lib.isException(object)")
        protected final NativeObject getExceptionType(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return getContext().asByteString(lib.getExceptionType(object).name());
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveThrowException")
    protected abstract static class PrimThrowExceptionNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

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
    protected abstract static class PrimThrowSyntaxErrorNode extends AbstractPrimitiveNode implements Primitive3WithFallback {

        @Specialization(guards = {"messageObject.isByteType()", "sourceObject.isByteType()"})
        protected static final Object doThrowSyntaxError(@SuppressWarnings("unused") final Object receiver, final NativeObject messageObject, final long position, final NativeObject sourceObject) {
            final String message = messageObject.asStringUnsafe();
            final String source = sourceObject.asStringUnsafe();
            throw new SqueakSyntaxError(message, (int) position, source);
        }
    }

    /*
     * Iterator
     */

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveHasIterator")
    protected abstract static class PrimHasIteratorNode extends AbstractPrimitiveNode implements Primitive1 {
        @Specialization
        protected static final boolean doHasIterator(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.hasIterator(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetIterator")
    protected abstract static class PrimGetIteratorNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "lib.hasIterator(object)")
        protected static final Object doGetIterator(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return lib.getIterator(object);
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsIterator")
    protected abstract static class PrimIsIteratorNode extends AbstractPrimitiveNode implements Primitive1 {
        @Specialization
        protected static final boolean doIsIterator(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            return BooleanObject.wrap(lib.isIterator(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveHasIteratorNextElement")
    protected abstract static class PrimHasIteratorNextElementNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "lib.isIterator(object)")
        protected static final boolean doHasIteratorNextElement(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return BooleanObject.wrap(lib.hasIteratorNextElement(object));
            } catch (final UnsupportedMessageException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetIteratorNextElement")
    protected abstract static class PrimGetIteratorNextElementNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "lib.isIterator(object)")
        protected static final Object doGetIteratorNextElement(@SuppressWarnings("unused") final Object receiver, final Object object,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            try {
                return lib.getIteratorNextElement(object);
            } catch (final UnsupportedMessageException | StopIterationException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    /*
     * Instrumentation API
     */

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetLanguageView")
    protected abstract static class PrimGetLanguageViewNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @TruffleBoundary
        @Specialization(guards = "languageId.isByteType()")
        protected final Object getLanguageView(@SuppressWarnings("unused") final Object receiver, final NativeObject languageId, final Object target) {
            return NilObject.nullToNil(getInstrumentEnv().getLanguageView(getContext().env.getPublicLanguages().get(languageId.asStringUnsafe()), target));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetScope")
    protected abstract static class PrimGetScopeNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @TruffleBoundary
        @Specialization(guards = "languageId.isByteType()")
        protected final Object getScope(@SuppressWarnings("unused") final Object receiver, final NativeObject languageId) {
            return NilObject.nullToNil(getInstrumentEnv().getScope(getContext().env.getPublicLanguages().get(languageId.asStringUnsafe())));
        }
    }

    /*
     * Host/Java interop
     */

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAddToHostClassPath")
    protected abstract static class PrimAddToHostClassPathNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"value.isByteType()"})
        protected final Object doAddToHostClassPath(final Object receiver, final NativeObject value) {
            final String path = value.asStringUnsafe();
            final TruffleLanguage.Env env = getContext().env;
            try {
                env.addToHostClassPath(env.getPublicTruffleFile(path));
                return receiver;
            } catch (final SecurityException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsHostLookupAllowed")
    protected abstract static class PrimIsHostLookupAllowedNode extends AbstractPrimitiveNode implements Primitive0 {
        @Specialization
        protected final Object doLookupHostSymbol(@SuppressWarnings("unused") final Object receiver) {
            return BooleanObject.wrap(getContext().env.isHostLookupAllowed());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveLookupHostSymbol")
    protected abstract static class PrimLookupHostSymbolNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"getContext().env.isHostLookupAllowed()", "value.isByteType()"})
        protected final Object doLookupHostSymbol(@SuppressWarnings("unused") final Object receiver, final NativeObject value) {
            final String symbolName = value.asStringUnsafe();
            try {
                return NilObject.nullToNil(getContext().env.lookupHostSymbol(symbolName));
            } catch (final RuntimeException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveLoadLanguageClass")
    protected abstract static class PrimLoadLanguageClassNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        private static final Method LOAD_LANGUAGE_CLASS = ReflectionUtils.lookupMethod(Engine.class, "loadLanguageClass", String.class);

        @TruffleBoundary
        @Specialization(guards = {"value.isByteType()"})
        protected final Object doLoadLanguageClass(@SuppressWarnings("unused") final Object receiver, final NativeObject value) {
            final String symbolName = value.asStringUnsafe();
            try {
                final Class<?> languageSymbol = (Class<?>) LOAD_LANGUAGE_CLASS.invoke(null, symbolName);
                if (languageSymbol != null) {
                    return getContext().env.asHostSymbol(languageSymbol);
                } else {
                    return NilObject.SINGLETON;
                }
            } catch (final RuntimeException | IllegalAccessException | InvocationTargetException e) {
                throw primitiveFailedInInterpreterCapturing(e);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsHostFunction")
    protected abstract static class PrimIsHostFunctionNode extends AbstractPrimitiveNode implements Primitive1 {
        @Specialization
        protected final boolean doIsHostFunction(@SuppressWarnings("unused") final Object receiver, final Object object) {
            return BooleanObject.wrap(getContext().env.isHostFunction(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsHostObject")
    protected abstract static class PrimIsHostObjectNode extends AbstractPrimitiveNode implements Primitive1 {
        @Specialization
        protected final boolean doIsHostObject(@SuppressWarnings("unused") final Object receiver, final Object object) {
            return BooleanObject.wrap(getContext().env.isHostObject(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsHostSymbol")
    protected abstract static class PrimIsHostSymbolNode extends AbstractPrimitiveNode implements Primitive1 {
        @Specialization
        protected final boolean doIsHostSymbol(@SuppressWarnings("unused") final Object receiver, final Object object) {
            return BooleanObject.wrap(getContext().env.isHostSymbol(object));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveHostIdentityHashCode")
    protected abstract static class PrimHostIdentityHashCodeNode extends AbstractPrimitiveNode implements Primitive1 {
        @Specialization
        protected static final long identityHashCode(@SuppressWarnings("unused") final Object receiver, final Object object) {
            return System.identityHashCode(object);
        }
    }

    /*
     * Error handling
     */

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetLastError")
    protected abstract static class PrimGetLastErrorNode extends AbstractPrimitiveNode implements Primitive0 {
        private static Exception lastError = new Exception("No exception");

        @Specialization
        @TruffleBoundary
        protected final Object doGetLastError(@SuppressWarnings("unused") final Object receiver) {
            return getContext().env.asGuestValue(lastError);
        }

        protected static final void setLastError(final Exception e) {
            lastError = e;
            LogUtils.INTEROP.fine(() -> MiscUtils.toString(lastError));
        }
    }

    /*
     * Interop type conversion
     */

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveToHostObject")
    protected abstract static class PrimToHostObjectNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "getContext().env.isHostObject(value)")
        protected final Object toHost(@SuppressWarnings("unused") final Object receiver, final Object value) {
            return getContext().env.asHostObject(value);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveToJavaInteger")
    protected abstract static class PrimToJavaIntegerNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final int toJavaInteger(@SuppressWarnings("unused") final Object receiver, final long value) {
            return MiscUtils.toIntExact(value);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveToJavaBigInteger")
    protected abstract static class PrimToJavaBigIntegerNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final BigInteger toJavaInteger(@SuppressWarnings("unused") final Object receiver, final LargeIntegerObject value) {
            return value.getBigInteger();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveToJavaString")
    protected abstract static class PrimToJavaStringNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "target.isByteType()")
        protected static final String bytesToString(@SuppressWarnings("unused") final Object receiver, final NativeObject target) {
            return target.asStringUnsafe();
        }

        @Specialization(guards = "target.isIntType()")
        protected static final String intsToString(@SuppressWarnings("unused") final Object receiver, final NativeObject target) {
            return target.asStringFromWideString();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveToTriState")
    protected abstract static class PrimToTriStateNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final TriState toTriState(@SuppressWarnings("unused") final Object receiver, final boolean value) {
            return TriState.valueOf(value);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSqueakLanguage")
    protected abstract static class PrimSqueakLanguageNode extends AbstractPrimitiveNode implements Primitive0 {
        @Specialization
        protected static final Class<SqueakLanguage> get(@SuppressWarnings("unused") final Object receiver) {
            return SqueakLanguage.class;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetRuntimeExceptionType")
    protected abstract static class PrimGetRuntimeExceptionTypeNode extends AbstractPrimitiveNode implements Primitive0 {
        @Specialization
        protected static final ExceptionType doGet(@SuppressWarnings("unused") final Object receiver) {
            return ExceptionType.RUNTIME_ERROR;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetParseExceptionType")
    protected abstract static class PrimGetParseExceptionTypeNode extends AbstractPrimitiveNode implements Primitive0 {
        @Specialization
        protected static final ExceptionType doGet(@SuppressWarnings("unused") final Object receiver) {
            return ExceptionType.PARSE_ERROR;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveThrowAsPolyglotException")
    protected abstract static class PrimThrowAsPolyglotExceptionNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "lib.isException(exception)")
        protected static final RuntimeException throwException(@SuppressWarnings("unused") final Object receiver, final PointersObject exception,
                        @SuppressWarnings("unused") @CachedLibrary(limit = "2") final InteropLibrary lib) {
            throw new SqueakExceptionWrapper(exception);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveThrowUnsupportedMessageException")
    protected abstract static class PrimThrowUnsupportedMessageExceptionNode extends AbstractPrimitiveNode implements Primitive0 {
        private static final UnsupportedMessageException EXCEPTION = UnsupportedMessageException.create();

        @Specialization
        protected static final UnsupportedMessageException throwException(@SuppressWarnings("unused") final Object receiver) {
            throw UnsafeUtils.throwException(EXCEPTION);
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
    private static String findLanguageByMimeType(final TruffleLanguage.Env env, final String mimeType) {
        final Map<String, LanguageInfo> languages = env.getPublicLanguages();
        for (final Map.Entry<String, LanguageInfo> entry : languages.entrySet()) {
            if (mimeType.equals(entry.getKey())) {
                return entry.getValue().getId();
            }
        }
        return null;
    }

    private static boolean isMimeType(final String lang) {
        return lang.contains("/");
    }

    private static com.oracle.truffle.api.instrumentation.TruffleInstrument.Env getInstrumentEnv() {
        if (instrumentEnv == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            final TruffleLanguage.Env env = SqueakImageContext.getSlow().env;
            instrumentEnv = env.lookup(env.getInstruments().get(INSTRUMENT_ID), PolyglotInstrument.class).getInstrumentEnv();
        }
        return instrumentEnv;
    }

    @TruffleInstrument.Registration(id = INSTRUMENT_ID, name = INSTRUMENT_NAME, services = PolyglotInstrument.class)
    public static class PolyglotInstrument extends TruffleInstrument {
        private Env instrumentEnv;

        @Override
        protected void onCreate(final Env env) {
            instrumentEnv = env;
            env.registerService(this);
        }

        public Env getInstrumentEnv() {
            return instrumentEnv;
        }
    }
}
