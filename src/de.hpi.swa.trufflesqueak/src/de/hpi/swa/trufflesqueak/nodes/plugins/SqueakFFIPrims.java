/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.source.Source;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.interop.WrapToSqueakNode;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.LargeIntegerObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodes.ArrayObjectToObjectArrayCopyNode;
import de.hpi.swa.trufflesqueak.nodes.plugins.SqueakFFIPrimsFactory.ArgTypeConversionNodeGen;
import de.hpi.swa.trufflesqueak.nodes.plugins.ffi.FFIConstants.FFI_ERROR;
import de.hpi.swa.trufflesqueak.nodes.plugins.ffi.FFIConstants.FFI_TYPES;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive2WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive3WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive4WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.MiscellaneousPrimitives.AbstractPrimCalloutToFFINode;
import de.hpi.swa.trufflesqueak.util.VarHandleUtils;

public final class SqueakFFIPrims extends AbstractPrimitiveFactoryHolder {

    /** "primitiveCallout" implemented as {@link AbstractPrimCalloutToFFINode}. */

    @GenerateUncached
    @GenerateInline(false)
    @GenerateCached(false)
    @ImportStatic(FFI_TYPES.class)
    public abstract static class ArgTypeConversionNode extends Node {

        public static ArgTypeConversionNode getUncached() {
            return ArgTypeConversionNodeGen.getUncached();
        }

        public abstract Object execute(int headerWord, Object value);

        @Specialization(guards = {"getAtomicType(headerWord) == 5"})
        protected static final short doShort(@SuppressWarnings("unused") final int headerWord, final boolean value) {
            return (short) (value ? 0 : 1);
        }

        @Specialization(guards = {"getAtomicType(headerWord) == 5"})
        protected static final short doShort(@SuppressWarnings("unused") final int headerWord, final char value) {
            return (short) value;
        }

        @Specialization(guards = {"getAtomicType(headerWord) == 7"})
        protected static final long doLong(@SuppressWarnings("unused") final int headerWord, final boolean value) {
            return value ? 0L : 1L;
        }

        @Specialization(guards = {"getAtomicType(headerWord) == 7"})
        protected static final long doLong(@SuppressWarnings("unused") final int headerWord, final char value) {
            return value;
        }

        @Specialization(guards = {"getAtomicType(headerWord) == 10", "!isPointerType(headerWord)"})
        protected static final char doChar(@SuppressWarnings("unused") final int headerWord, final boolean value) {
            return (char) (value ? 0 : 1);
        }

        @Specialization(guards = {"getAtomicType(headerWord) == 10", "!isPointerType(headerWord)"})
        protected static final char doChar(@SuppressWarnings("unused") final int headerWord, final char value) {
            return value;
        }

        @Specialization(guards = {"getAtomicType(headerWord) == 10", "!isPointerType(headerWord)"})
        protected static final char doChar(@SuppressWarnings("unused") final int headerWord, final long value) {
            return (char) value;
        }

        @Specialization(guards = {"getAtomicType(headerWord) == 10", "!isPointerType(headerWord)"})
        protected static final char doChar(@SuppressWarnings("unused") final int headerWord, final double value) {
            return (char) value;
        }

        @Specialization(guards = {"getAtomicType(headerWord) == 10", "isPointerType(headerWord)"}, limit = "1")
        protected static final String doString(@SuppressWarnings("unused") final int headerWord, final Object value,
                        @CachedLibrary("value") final InteropLibrary lib) {
            try {
                return lib.asString(value);
            } catch (final UnsupportedMessageException e) {
                CompilerDirectives.transferToInterpreter();
                e.printStackTrace();
                return "unknown";
            }
        }

        @Specialization(guards = "getAtomicType(headerWord) == 12")
        protected static final float doFloat(@SuppressWarnings("unused") final int headerWord, final char value) {
            return value;
        }

        @Specialization(guards = "getAtomicType(headerWord) == 12")
        protected static final float doFloat(@SuppressWarnings("unused") final int headerWord, final double value) {
            return (float) value;
        }

        @Specialization(guards = {"getAtomicType(headerWord) == 13"})
        protected static final double doDouble(@SuppressWarnings("unused") final int headerWord, final char value) {
            return value;
        }

        @Specialization(guards = {"getAtomicType(headerWord) == 13"})
        protected static final double doDouble(@SuppressWarnings("unused") final int headerWord, final long value) {
            return value;
        }

        @Fallback
        protected static final Object doFallback(@SuppressWarnings("unused") final int headerWord, final Object value) {
            return value;
        }
    }

    public abstract static class AbstractFFIPrimitiveNode extends AbstractPrimitiveNode {
        protected final PointersObject asExternalFunctionOrFail(final Object object) {
            if (object instanceof final PointersObject o && o.getSqueakClass().includesExternalFunctionBehavior(getContext())) {
                return o;
            } else {
                throw PrimitiveFailed.andTransferToInterpreter(FFI_ERROR.NOT_FUNCTION);
            }
        }

        @TruffleBoundary
        protected final Object doCallout(final PointersObject externalLibraryFunction, final AbstractSqueakObject receiver, final Object... arguments) {
            final SqueakImageContext image = getContext();
            final List<Integer> headerWordList = new ArrayList<>();

            final AbstractPointersObjectReadNode readNode = AbstractPointersObjectReadNode.getUncached();
            final ArgTypeConversionNode conversionNode = ArgTypeConversionNode.getUncached();

            final ArrayObject argTypes = readNode.executeArray(null, externalLibraryFunction, ObjectLayouts.EXTERNAL_LIBRARY_FUNCTION.ARG_TYPES);

            if (argTypes != null && argTypes.getObjectStorage().length == arguments.length + 1) {
                final Object[] argTypesValues = argTypes.getObjectStorage();

                for (final Object argumentType : argTypesValues) {
                    if (argumentType instanceof final PointersObject o) {
                        final NativeObject compiledSpec = readNode.executeNative(null, o, ObjectLayouts.EXTERNAL_TYPE.COMPILED_SPEC);
                        headerWordList.add(compiledSpec.getInt(0));
                    }
                }
            }

            final Object[] argumentsConverted = getConvertedArgumentsFromHeaderWords(conversionNode, headerWordList, arguments);
            final List<String> nfiArgTypeList = getArgTypeListFromHeaderWords(headerWordList);

            final String name = readNode.executeNative(null, externalLibraryFunction, ObjectLayouts.EXTERNAL_LIBRARY_FUNCTION.NAME).asStringUnsafe();
            final String moduleName = getModuleName(readNode, null, receiver, externalLibraryFunction);
            final String nfiCodeParams = generateNfiCodeParamsString(nfiArgTypeList);
            final String nfiCode = String.format("load \"%s\" {%s%s}", getPathOrFail(image, moduleName), name, nfiCodeParams);
            try {
                final Object value = calloutToLib(image, name, argumentsConverted, nfiCode);
                assert value != null;
                return WrapToSqueakNode.executeUncached(conversionNode.execute(headerWordList.get(0), value));
            } catch (UnsupportedMessageException | ArityException | UnknownIdentifierException | UnsupportedTypeException e) {
                e.printStackTrace();
                // TODO: return correct error code.
                throw PrimitiveFailed.GENERIC_ERROR;
            } catch (final Exception e) {
                e.printStackTrace();
                // TODO: handle exception
                throw PrimitiveFailed.GENERIC_ERROR;
            }
        }

        private static Object[] getConvertedArgumentsFromHeaderWords(final ArgTypeConversionNode conversionNode, final List<Integer> headerWordList,
                        final Object[] arguments) {
            final Object[] argumentsConverted = new Object[arguments.length];

            for (int j = 1; j < headerWordList.size(); j++) {
                argumentsConverted[j - 1] = conversionNode.execute(headerWordList.get(j), arguments[j - 1]);
            }
            return argumentsConverted;
        }

        private static List<String> getArgTypeListFromHeaderWords(final List<Integer> headerWordList) {
            final List<String> nfiArgTypeList = new ArrayList<>();

            for (final int headerWord : headerWordList) {
                final String atomicName = FFI_TYPES.getTruffleTypeFromInt(headerWord);
                nfiArgTypeList.add(atomicName);
            }
            return nfiArgTypeList;
        }

        private static Object calloutToLib(final SqueakImageContext image, final String name, final Object[] argumentsConverted, final String nfiCode)
                        throws UnsupportedMessageException, ArityException, UnknownIdentifierException, UnsupportedTypeException {
            final Source source = Source.newBuilder("nfi", nfiCode, "native").build();
            final Object ffiTest = image.env.parseInternal(source).call();
            final InteropLibrary interopLib = InteropLibrary.getFactory().getUncached(ffiTest);
            return interopLib.invokeMember(ffiTest, name, argumentsConverted);
        }

        private static String getModuleName(final AbstractPointersObjectReadNode readExternalLibNode, final Node inlineTarget, final AbstractSqueakObject receiver,
                        final PointersObject externalLibraryFunction) {
            final Object moduleObject = readExternalLibNode.execute(inlineTarget, externalLibraryFunction, ObjectLayouts.EXTERNAL_LIBRARY_FUNCTION.MODULE);
            if (moduleObject != NilObject.SINGLETON) {
                return ((NativeObject) moduleObject).asStringUnsafe();
            } else {
                return ((NativeObject) ((PointersObject) receiver).instVarAt0Slow(ObjectLayouts.CLASS.NAME)).asStringUnsafe();
            }
        }

        protected static final String getPathOrFail(final SqueakImageContext image, final String moduleName) {
            final String libName = System.mapLibraryName(moduleName);
            final TruffleFile libPath = image.getHomePath().resolve("lib" + File.separatorChar + libName);
            if (!libPath.exists()) {
                throw PrimitiveFailed.GENERIC_ERROR;
            }
            return libPath.getAbsoluteFile().getPath();
        }

        private static String generateNfiCodeParamsString(final List<String> argumentList) {
            final StringBuilder nfiCodeParams = new StringBuilder(32);
            if (!argumentList.isEmpty()) {
                final String returnType = argumentList.get(0);
                argumentList.remove(0);
                if (!argumentList.isEmpty()) {
                    nfiCodeParams.append('(').append(String.join(",", argumentList)).append(')');
                } else {
                    nfiCodeParams.append("()");
                }
                nfiCodeParams.append(':').append(returnType).append(';');
            }
            return nfiCodeParams.toString();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveCalloutWithArgs")
    protected abstract static class PrimCalloutWithArgsNode extends AbstractFFIPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected final Object doCalloutWithArgs(final PointersObject receiver, final ArrayObject argArray,
                        @Bind final Node node,
                        @Cached final ArrayObjectToObjectArrayCopyNode getObjectArrayNode) {
            return doCallout(asExternalFunctionOrFail(receiver), receiver, getObjectArrayNode.execute(node, argArray));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveLoadSymbolFromModule")
    protected abstract static class PrimLoadSymbolFromModuleNode extends AbstractFFIPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = {"moduleSymbol.isByteType()", "module.isByteType()"})
        protected final NativeObject doLoadSymbol(final ClassObject receiver, final NativeObject moduleSymbol, final NativeObject module,
                        @CachedLibrary(limit = "2") final InteropLibrary lib) {
            final SqueakImageContext image = getContext();
            final String moduleSymbolName = moduleSymbol.asStringUnsafe();
            final String moduleName = module.asStringUnsafe();
            final CallTarget target = image.env.parseInternal(generateNFILoadSource(image, moduleName));
            final Object library;
            try {
                library = target.call();
            } catch (final Throwable e) {
                throw PrimitiveFailed.andTransferToInterpreter();
            }
            final Object symbol;
            try {
                symbol = lib.readMember(library, moduleSymbolName);
            } catch (UnsupportedMessageException | UnknownIdentifierException e) {
                throw PrimitiveFailed.andTransferToInterpreter();
            }
            final long pointer;
            try {
                pointer = lib.asPointer(symbol);
            } catch (final UnsupportedMessageException e) {
                CompilerDirectives.transferToInterpreter();
                e.printStackTrace();
                return newExternalAddress(image, receiver, 0L);
            }
            return newExternalAddress(image, receiver, pointer);
        }

        @TruffleBoundary
        private static Source generateNFILoadSource(final SqueakImageContext image, final String moduleName) {
            return Source.newBuilder("nfi", String.format("load \"%s\"", getPathOrFail(image, moduleName)), "native").build();
        }

        private static NativeObject newExternalAddress(final SqueakImageContext image, final ClassObject externalAddressClass, final long pointer) {
            return NativeObject.newNativeBytes(image, externalAddressClass,
                            new byte[]{(byte) pointer, (byte) (pointer >> 8), (byte) (pointer >> 16), (byte) (pointer >> 24), (byte) (pointer >> 32), (byte) (pointer >> 40),
                                            (byte) (pointer >> 48), (byte) (pointer >> 56)});
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFFIDoubleAt")
    protected abstract static class PrimFFIDoubleAtNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0"})
        protected static final double doFloatAtPut(final NativeObject byteArray, final long byteOffsetLong) {
            return VarHandleUtils.getDoubleFromBytes(byteArray.getByteStorage(), (int) byteOffsetLong - 1);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFFIDoubleAtPut")
    protected abstract static class PrimFFIDoubleAtPutNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0"})
        protected static final double doFloatAtPut(final NativeObject byteArray, final long byteOffsetLong, final double value) {
            VarHandleUtils.putDoubleIntoBytes(byteArray.getByteStorage(), (int) byteOffsetLong - 1, value);
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFFIFloatAt")
    protected abstract static class PrimFFIFloatAtNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0"})
        protected static final double doFloatAtPut(final NativeObject byteArray, final long byteOffsetLong) {
            return VarHandleUtils.getFloatFromBytes(byteArray.getByteStorage(), (int) byteOffsetLong - 1);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFFIFloatAtPut")
    protected abstract static class PrimFFIFloatAtPutNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0"})
        protected static final double doFloatAtPut(final NativeObject byteArray, final long byteOffsetLong, final double value) {
            VarHandleUtils.putFloatIntoBytes(byteArray.getByteStorage(), (int) byteOffsetLong - 1, (float) value);
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFFIIntegerAt")
    protected abstract static class PrimFFIIntegerAtNode extends AbstractPrimitiveNode implements Primitive3WithFallback {
        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 1", "isSigned"})
        protected static final long doAt1Signed(final NativeObject byteArray, final long byteOffsetLong, final long byteSize, final boolean isSigned) {
            return byteArray.getByte(byteOffsetLong - 1);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 1", "!isSigned"})
        protected static final long doAt1Unsigned(final NativeObject byteArray, final long byteOffsetLong, final long byteSize, final boolean isSigned) {
            return byteArray.getByteUnsigned(byteOffsetLong - 1);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 2", "isSigned"})
        protected static final long doAt2Signed(final NativeObject byteArray, final long byteOffsetLong, final long byteSize, final boolean isSigned) {
            return PrimSignedInt16AtNode.signedInt16At(byteArray, byteOffsetLong);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 2", "!isSigned"})
        protected static final long doAt2Unsigned(final NativeObject byteArray, final long byteOffsetLong, final long byteSize, final boolean isSigned) {
            return PrimUnsignedInt16AtNode.doAt(byteArray, byteOffsetLong);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 4", "isSigned"})
        protected static final long doAt4Signed(final NativeObject byteArray, final long byteOffsetLong, final long byteSize, final boolean isSigned) {
            return PrimSignedInt16AtNode.signedInt16At(byteArray, byteOffsetLong);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 4", "!isSigned"})
        protected static final long doAt4Unsigned(final NativeObject byteArray, final long byteOffsetLong, final long byteSize, final boolean isSigned) {
            return PrimUnsignedInt32AtNode.doAt(byteArray, byteOffsetLong);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 8", "isSigned"})
        protected static final long doAt8Signed(final NativeObject byteArray, final long byteOffsetLong, final long byteSize, final boolean isSigned) {
            return PrimSignedInt64AtNode.signedInt64At(byteArray, byteOffsetLong);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 8", "!isSigned"})
        protected final Object doAt8Unsigned(final NativeObject byteArray, final long byteOffsetLong, final long byteSize, final boolean isSigned,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile positiveProfile) {
            return PrimUnsignedInt64AtNode.unsignedInt64At(getContext(), byteArray, byteOffsetLong, positiveProfile, node);
        }
    }

    protected static final class FFIGuards {
        protected static final long MAX_VALUE_SIGNED_1 = 1L << 8 * 1 - 1;
        protected static final long MAX_VALUE_SIGNED_2 = 1L << 8 * 2 - 1;
        protected static final long MAX_VALUE_SIGNED_4 = 1L << 8 * 4 - 1;
        protected static final BigInteger MAX_VALUE_SIGNED_8 = BigInteger.ONE.shiftLeft(8 * 8 - 1);
        protected static final long MAX_VALUE_UNSIGNED_1 = 1L << 8 * 1;
        protected static final long MAX_VALUE_UNSIGNED_2 = 1L << 8 * 2;
        protected static final long MAX_VALUE_UNSIGNED_4 = 1L << 8 * 4;

        protected static boolean inSignedBounds(final long value, final long max) {
            return value >= -max && value < max;
        }

        protected static boolean inUnsignedBounds(final long value, final long max) {
            return 0 <= value && value < max;
        }

        @TruffleBoundary
        protected static boolean inSignedBounds(final LargeIntegerObject value, final BigInteger max) {
            return value.getBigInteger().compareTo(BigInteger.ZERO.subtract(max)) >= 0 && value.getBigInteger().compareTo(max) < 0;
        }

        @TruffleBoundary
        protected static boolean inUnsignedBounds(final LargeIntegerObject value) {
            return value.isZeroOrPositive() && value.lessThanOneShiftedBy64();
        }
    }

    @GenerateNodeFactory
    @ImportStatic(FFIGuards.class)
    @SqueakPrimitive(names = "primitiveFFIIntegerAtPut")
    protected abstract static class PrimFFIIntegerAtPutNode extends AbstractPrimitiveNode implements Primitive4WithFallback {
        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 1", "isSigned", "inSignedBounds(value, MAX_VALUE_SIGNED_1)"})
        protected static final long doAtPut1Signed(final NativeObject byteArray, final long byteOffsetLong, final long value, final long byteSize, final boolean isSigned) {
            return doAtPut1Unsigned(byteArray, byteOffsetLong, value, byteSize, isSigned);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 1", "!isSigned", "inUnsignedBounds(value, MAX_VALUE_UNSIGNED_1)"})
        protected static final long doAtPut1Unsigned(final NativeObject byteArray, final long byteOffsetLong, final long value, final long byteSize, final boolean isSigned) {
            return PrimUnsignedInt8AtPutNode.doAtPut(byteArray, byteOffsetLong, value);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 2", "isSigned", "inSignedBounds(value, MAX_VALUE_SIGNED_2)"})
        protected static final long doAtPut2Signed(final NativeObject byteArray, final long byteOffsetLong, final long value, final long byteSize, final boolean isSigned) {
            return doAtPut2Unsigned(byteArray, byteOffsetLong, value, byteSize, isSigned);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 2", "!isSigned", "inUnsignedBounds(value, MAX_VALUE_UNSIGNED_2)"})
        protected static final long doAtPut2Unsigned(final NativeObject byteArray, final long byteOffsetLong, final long value, final long byteSize, final boolean isSigned) {
            return PrimUnsignedInt16AtPutNode.doAtPut(byteArray, byteOffsetLong, value);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 4", "isSigned", "inSignedBounds(value, MAX_VALUE_SIGNED_4)"})
        protected static final long doAtPut4Signed(final NativeObject byteArray, final long byteOffsetLong, final long value, final long byteSize, final boolean isSigned) {
            return doAtPut4Unsigned(byteArray, byteOffsetLong, value, byteSize, isSigned);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 4", "!isSigned", "inUnsignedBounds(value, MAX_VALUE_UNSIGNED_4)"})
        protected static final long doAtPut4Unsigned(final NativeObject byteArray, final long byteOffsetLong, final long value, final long byteSize, final boolean isSigned) {
            return PrimUnsignedInt32AtPutNode.doAtPut(byteArray, byteOffsetLong, value);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 4", "isSigned", "value.fitsIntoLong()", "inSignedBounds(value.longValueExact(), MAX_VALUE_SIGNED_4)"})
        protected static final LargeIntegerObject doAtPut4SignedLarge(final NativeObject byteArray, final long byteOffsetLong, final LargeIntegerObject value, final long byteSize,
                        final boolean isSigned) {
            return doAtPut4UnsignedLarge(byteArray, byteOffsetLong, value, byteSize, isSigned);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 4", "!isSigned", "value.fitsIntoLong()",
                        "inUnsignedBounds(value.longValueExact(), MAX_VALUE_UNSIGNED_4)"})
        protected static final LargeIntegerObject doAtPut4UnsignedLarge(final NativeObject byteArray, final long byteOffsetLong, final LargeIntegerObject value, final long byteSize,
                        final boolean isSigned) {
            return PrimUnsignedInt32AtPutNode.doAtPut(byteArray, byteOffsetLong, value);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 8", "isSigned"})
        protected static final Object doAtPut8Signed(final NativeObject byteArray, final long byteOffsetLong, final long value, final long byteSize, final boolean isSigned) {
            return doAtPut8Unsigned(byteArray, byteOffsetLong, value, byteSize, isSigned);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 8", "!isSigned", "value >= 0"})
        protected static final Object doAtPut8Unsigned(final NativeObject byteArray, final long byteOffsetLong, final long value, final long byteSize, final boolean isSigned) {
            return PrimUnsignedInt64AtPutNode.doAtPut(byteArray, byteOffsetLong, value);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 8", "isSigned", "inSignedBounds(value, MAX_VALUE_SIGNED_8)"})
        protected static final Object doAtPut8SignedLarge(final NativeObject byteArray, final long byteOffsetLong, final LargeIntegerObject value, final long byteSize, final boolean isSigned) {
            return doAtPut8UnsignedLarge(byteArray, byteOffsetLong, value, byteSize, isSigned);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 8", "!isSigned", "inUnsignedBounds(value)"})
        @ExplodeLoop
        protected static final Object doAtPut8UnsignedLarge(final NativeObject byteArray, final long byteOffsetLong, final LargeIntegerObject value, final long byteSize, final boolean isSigned) {
            return PrimUnsignedInt64AtPutNode.doAtPut(byteArray, byteOffsetLong, value);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSignedInt8At")
    protected abstract static class PrimSignedInt8AtNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"byteArray.isByteType()", "byteOffset > 0"})
        protected static final long doAt(final NativeObject byteArray, final long byteOffset) {
            return byteArray.getByte(byteOffset - 1);
        }
    }

    @GenerateNodeFactory
    @ImportStatic(FFIGuards.class)
    @SqueakPrimitive(names = "primitiveSignedInt8AtPut")
    protected abstract static class PrimSignedInt8AtPutNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = {"byteArray.isByteType()", "byteOffset > 0", "inSignedBounds(value, MAX_VALUE_SIGNED_1)"})
        protected static final long doAtPut(final NativeObject byteArray, final long byteOffset, final long value) {
            return PrimUnsignedInt8AtPutNode.doAtPut(byteArray, byteOffset, value);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSignedInt16At")
    protected abstract static class PrimSignedInt16AtNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"byteArray.isByteType()", "byteOffset > 0"})
        protected static final long doAt(final NativeObject byteArray, final long byteOffset) {
            return signedInt16At(byteArray, byteOffset);
        }

        private static short signedInt16At(final NativeObject byteArray, final long byteOffset) {
            return VarHandleUtils.getShortFromBytes(byteArray.getByteStorage(), (int) byteOffset - 1);
        }
    }

    @GenerateNodeFactory
    @ImportStatic(FFIGuards.class)
    @SqueakPrimitive(names = "primitiveSignedInt16AtPut")
    protected abstract static class PrimSignedInt16AtPutNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = {"byteArray.isByteType()", "byteOffset > 0", "inSignedBounds(value, MAX_VALUE_SIGNED_2)"})
        protected static final long doAtPut(final NativeObject byteArray, final long byteOffset, final long value) {
            return PrimUnsignedInt16AtPutNode.doAtPut(byteArray, byteOffset, value);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSignedInt32At")
    protected abstract static class PrimSignedInt32AtNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"byteArray.isByteType()", "byteOffset > 0"})
        protected static final long doAt(final NativeObject byteArray, final long byteOffset) {
            return signedInt32At(byteArray, byteOffset);
        }

        private static int signedInt32At(final NativeObject byteArray, final long byteOffset) {
            return VarHandleUtils.getIntFromBytes(byteArray.getByteStorage(), (int) byteOffset - 1);
        }
    }

    @GenerateNodeFactory
    @ImportStatic(FFIGuards.class)
    @SqueakPrimitive(names = "primitiveSignedInt32AtPut")
    protected abstract static class PrimSignedInt32AtPutNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = {"byteArray.isByteType()", "byteOffset > 0", "inSignedBounds(value, MAX_VALUE_SIGNED_4)"})
        protected static final long doAtPut(final NativeObject byteArray, final long byteOffset, final long value) {
            return PrimUnsignedInt32AtPutNode.doAtPut(byteArray, byteOffset, value);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "value.fitsIntoLong()", "inSignedBounds(value.longValueExact(), MAX_VALUE_SIGNED_4)"})
        protected static final LargeIntegerObject doAtPut(final NativeObject byteArray, final long byteOffsetLong, final LargeIntegerObject value) {
            return PrimUnsignedInt32AtPutNode.doAtPut(byteArray, byteOffsetLong, value);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSignedInt64At")
    protected abstract static class PrimSignedInt64AtNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"byteArray.isByteType()", "byteOffset > 0"})
        protected static final long doAt(final NativeObject byteArray, final long byteOffset) {
            return signedInt64At(byteArray, byteOffset);
        }

        private static long signedInt64At(final NativeObject byteArray, final long byteOffset) {
            return VarHandleUtils.getLongFromBytes(byteArray.getByteStorage(), (int) byteOffset - 1);
        }
    }

    @GenerateNodeFactory
    @ImportStatic(FFIGuards.class)
    @SqueakPrimitive(names = "primitiveSignedInt64AtPut")
    protected abstract static class PrimSignedInt64AtPutNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0"})
        protected static final long doAtPut(final NativeObject byteArray, final long byteOffsetLong, final long value) {
            VarHandleUtils.putLongIntoBytes(byteArray.getByteStorage(), (int) byteOffsetLong - 1, value);
            return value;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "inSignedBounds(value, MAX_VALUE_SIGNED_8)"})
        protected static final LargeIntegerObject doAtPut(final NativeObject byteArray, final long byteOffsetLong, final LargeIntegerObject value) {
            atPutNativeLarge(byteArray, byteOffsetLong, value);
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveUnsignedInt8At")
    protected abstract static class PrimUnsignedInt8AtNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"byteArray.isByteType()", "byteOffset > 0"})
        protected static final long doAt(final NativeObject byteArray, final long byteOffset) {
            return byteArray.getByteUnsigned(byteOffset - 1);
        }
    }

    @GenerateNodeFactory
    @ImportStatic(FFIGuards.class)
    @SqueakPrimitive(names = "primitiveUnsignedInt8AtPut")
    protected abstract static class PrimUnsignedInt8AtPutNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = {"byteArray.isByteType()", "byteOffset > 0", "inUnsignedBounds(value, MAX_VALUE_UNSIGNED_1)"})
        protected static final long doAtPut(final NativeObject byteArray, final long byteOffset, final long value) {
            byteArray.setByte(byteOffset - 1, (byte) value);
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveUnsignedInt16At")
    protected abstract static class PrimUnsignedInt16AtNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"byteArray.isByteType()", "byteOffset > 0"})
        protected static final long doAt(final NativeObject byteArray, final long byteOffset) {
            return Short.toUnsignedLong(PrimSignedInt16AtNode.signedInt16At(byteArray, byteOffset));
        }
    }

    @GenerateNodeFactory
    @ImportStatic(FFIGuards.class)
    @SqueakPrimitive(names = "primitiveUnsignedInt16AtPut")
    protected abstract static class PrimUnsignedInt16AtPutNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = {"byteArray.isByteType()", "byteOffset > 0", "inUnsignedBounds(value, MAX_VALUE_UNSIGNED_2)"})
        protected static final long doAtPut(final NativeObject byteArray, final long byteOffset, final long value) {
            VarHandleUtils.putShortIntoBytes(byteArray.getByteStorage(), (int) byteOffset - 1, (short) value);
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveUnsignedInt32At")
    protected abstract static class PrimUnsignedInt32AtNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"byteArray.isByteType()", "byteOffset > 0"})
        protected static final long doAt(final NativeObject byteArray, final long byteOffset) {
            return Integer.toUnsignedLong(PrimSignedInt32AtNode.signedInt32At(byteArray, byteOffset));
        }
    }

    @GenerateNodeFactory
    @ImportStatic(FFIGuards.class)
    @SqueakPrimitive(names = "primitiveUnsignedInt32AtPut")
    protected abstract static class PrimUnsignedInt32AtPutNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = {"byteArray.isByteType()", "byteOffset > 0", "inUnsignedBounds(value, MAX_VALUE_UNSIGNED_4)"})
        protected static final long doAtPut(final NativeObject byteArray, final long byteOffset, final long value) {
            VarHandleUtils.putIntIntoBytes(byteArray.getByteStorage(), (int) byteOffset - 1, (int) value);
            return value;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "value.fitsIntoLong()", "inUnsignedBounds(value.longValueExact(), MAX_VALUE_UNSIGNED_4)"})
        @ExplodeLoop
        protected static final LargeIntegerObject doAtPut(final NativeObject byteArray, final long byteOffsetLong, final LargeIntegerObject value) {
            final int byteOffset = (int) byteOffsetLong - 1;
            final byte[] targetBytes = byteArray.getByteStorage();
            final byte[] sourceBytes = value.getBytes();
            final int numSourceBytes = sourceBytes.length;
            for (int i = 0; i < 4; i++) {
                targetBytes[byteOffset + i] = i < numSourceBytes ? sourceBytes[i] : 0;
            }
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveUnsignedInt64At")
    protected abstract static class PrimUnsignedInt64AtNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"byteArray.isByteType()", "byteOffset > 0"})
        protected final Object doAt(final NativeObject byteArray, final long byteOffset,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile positiveProfile) {
            return unsignedInt64At(getContext(), byteArray, byteOffset, positiveProfile, node);
        }

        private static Object unsignedInt64At(final SqueakImageContext image, final NativeObject byteArray, final long byteOffset, final InlinedConditionProfile positiveProfile, final Node node) {
            final long signedLong = PrimSignedInt64AtNode.signedInt64At(byteArray, byteOffset);
            if (positiveProfile.profile(node, signedLong >= 0)) {
                return signedLong;
            } else {
                return LargeIntegerObject.toUnsigned(image, signedLong);
            }
        }
    }

    @GenerateNodeFactory
    @ImportStatic(FFIGuards.class)
    @SqueakPrimitive(names = "primitiveUnsignedInt64AtPut")
    protected abstract static class PrimUnsignedInt64AtPutNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "value >= 0"})
        protected static final long doAtPut(final NativeObject byteArray, final long byteOffsetLong, final long value) {
            VarHandleUtils.putLongIntoBytes(byteArray.getByteStorage(), (int) byteOffsetLong - 1, value);
            return value;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "inUnsignedBounds(value)"})
        protected static final LargeIntegerObject doAtPut(final NativeObject byteArray, final long byteOffsetLong, final LargeIntegerObject value) {
            atPutNativeLarge(byteArray, byteOffsetLong, value);
            return value;
        }
    }

    @ExplodeLoop
    private static void atPutNativeLarge(final NativeObject byteArray, final long byteOffsetLong, final LargeIntegerObject value) {
        final int byteOffset = (int) byteOffsetLong - 1;
        final byte[] targetBytes = byteArray.getByteStorage();
        final byte[] sourceBytes = value.getBytes();
        final int numSourceBytes = sourceBytes.length;
        for (int i = 0; i < 8; i++) {
            targetBytes[byteOffset + i] = i < numSourceBytes ? sourceBytes[i] : 0;
        }
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return SqueakFFIPrimsFactory.getFactories();
    }
}
