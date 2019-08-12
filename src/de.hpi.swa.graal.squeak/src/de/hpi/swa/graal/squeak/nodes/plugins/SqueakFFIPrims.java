package de.hpi.swa.graal.squeak.nodes.plugins;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
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
import com.oracle.truffle.api.source.Source;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.interop.WrapToSqueakNode;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectToObjectArrayCopyNode;
import de.hpi.swa.graal.squeak.nodes.plugins.SqueakFFIPrimsFactory.ArgTypeConversionNodeGen;
import de.hpi.swa.graal.squeak.nodes.plugins.ffi.FFIConstants.FFI_ERROR;
import de.hpi.swa.graal.squeak.nodes.plugins.ffi.FFIConstants.FFI_TYPES;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.QuaternaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.QuinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.MiscellaneousPrimitives.PrimCalloutToFFINode;

public final class SqueakFFIPrims extends AbstractPrimitiveFactoryHolder {

    /** "primitiveCallout" implemented as {@link PrimCalloutToFFINode}. */

    @ImportStatic(FFI_TYPES.class)
    protected abstract static class ArgTypeConversionNode extends Node {

        protected static ArgTypeConversionNode create() {
            return ArgTypeConversionNodeGen.create();
        }

        public abstract Object execute(int headerWord, Object value);

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
                throw SqueakException.illegalState(e);
            }
        }

        @Specialization(guards = "getAtomicType(headerWord) == 12")
        protected static final float doFloat(@SuppressWarnings("unused") final int headerWord, final double value) {
            return (float) value;
        }

        @Fallback
        protected static final Object doFallback(@SuppressWarnings("unused") final int headerWord, final Object value) {
            return value;
        }
    }

    public abstract static class AbstractFFIPrimitiveNode extends AbstractPrimitiveNode {

        @Child private ArgTypeConversionNode conversionNode = ArgTypeConversionNode.create();
        @Child private WrapToSqueakNode wrapNode = WrapToSqueakNode.create();

        public AbstractFFIPrimitiveNode(final CompiledMethodObject method) {
            super(method);
        }

        protected final Object doCallout(final PointersObject externalLibraryFunction, final AbstractSqueakObject receiver, final Object... arguments) {
            if (!externalLibraryFunction.getSqueakClass().includesExternalFunctionBehavior()) {
                throw new PrimitiveFailed(FFI_ERROR.NOT_FUNCTION);
            }
            final String name = ((NativeObject) externalLibraryFunction.at0(ObjectLayouts.EXTERNAL_LIBRARY_FUNCTION.NAME)).asStringUnsafe();
            final Object moduleObject = externalLibraryFunction.at0(ObjectLayouts.EXTERNAL_LIBRARY_FUNCTION.MODULE);
            final String module;
            if (moduleObject != NilObject.SINGLETON) {
                module = ((NativeObject) moduleObject).asStringUnsafe();
            } else {
                module = ((NativeObject) ((PointersObject) receiver).at0(1)).asStringUnsafe();
            }
            final Object[] argumentsConverted = new Object[arguments.length];
            final ArrayObject argTypes = (ArrayObject) externalLibraryFunction.at0(ObjectLayouts.EXTERNAL_LIBRARY_FUNCTION.ARG_TYPES);
            int returnArgHeader = 0;
            final List<String> argumentList = new ArrayList<>();
            if (argTypes != null) {
                final Object[] argTypesValues = argTypes.getObjectStorage();
                assert argTypesValues.length == 1 + arguments.length;
                for (int i = 0; i < argTypesValues.length; i++) {
                    final Object argType = argTypesValues[i];
                    if (argType instanceof PointersObject) {
                        final NativeObject compiledSpec = (NativeObject) ((PointersObject) argType).at0(ObjectLayouts.EXTERNAL_TYPE.COMPILED_SPEC);
                        final int headerWord = compiledSpec.getIntStorage()[0];
                        if (i == 0) {
                            returnArgHeader = headerWord;
                        } else if (i > 0) {
                            argumentsConverted[i - 1] = conversionNode.execute(headerWord, arguments[i - 1]);
                        }
                        final String atomicName;
                        atomicName = FFI_TYPES.getTruffleTypeFromInt(headerWord);
                        argumentList.add(atomicName);
                    }
                }
            }
            final String nfiCodeParams = creatNfiCodeParamsString(argumentList);

            final String ffiExtension = method.image.os.getFFIExtension();
            final String libPath = System.getProperty("user.dir") + File.separatorChar + "lib" + File.separatorChar + module + ffiExtension;
            final String nfiCode = String.format("load \"%s\" {%s%s}", libPath, name, nfiCodeParams);

            // method.image.env = com.oracle.truffle.api.TruffleLanguage$Env@1a1d76bd
            final Source source = Source.newBuilder("nfi", nfiCode, "native").build();
            final Object ffiTest = method.image.env.parse(source).call();
            // method.image.env.addToHostClassPath(entry);
            final InteropLibrary interopLib = InteropLibrary.getFactory().getUncached(ffiTest);
            try {
                final Object value = interopLib.invokeMember(ffiTest, name, argumentsConverted);
                assert value != null;
                return wrapNode.executeWrap(conversionNode.execute(returnArgHeader, value));
            } catch (UnsupportedMessageException | ArityException | UnknownIdentifierException | UnsupportedTypeException e) {
                e.printStackTrace();
                // TODO: return correct error code.
                throw new PrimitiveFailed();
            } catch (final Exception e) {
                e.printStackTrace();
                // TODO: handle exception
                throw new PrimitiveFailed();
            }
        }

        private static String creatNfiCodeParamsString(final List<String> argumentList) {
            String nfiCodeParams = "";
            if (!argumentList.isEmpty()) {
                final String returnType = argumentList.get(0);
                argumentList.remove(0);
                if (!argumentList.isEmpty()) {
                    nfiCodeParams = "(" + String.join(",", argumentList) + ")";
                } else {
                    nfiCodeParams = "()";
                }
                nfiCodeParams += ":" + returnType + ";";
            }
            return nfiCodeParams;
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveCalloutWithArgs")
    protected abstract static class PrimCalloutWithArgsNode extends AbstractFFIPrimitiveNode implements BinaryPrimitive {

        @Child private ArrayObjectToObjectArrayCopyNode getObjectArrayNode = ArrayObjectToObjectArrayCopyNode.create();

        protected PrimCalloutWithArgsNode(final CompiledMethodObject method) {
            super(method);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected final Object doCalloutWithArgs(final PointersObject receiver, final ArrayObject argArray) {
            return doCallout(receiver, receiver, getObjectArrayNode.execute(argArray));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveLoadSymbolFromModule")
    protected abstract static class PrimLoadSymbolFromModuleNode extends AbstractFFIPrimitiveNode implements TernaryPrimitive {

        protected PrimLoadSymbolFromModuleNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"moduleSymbol.isByteType()", "module.isByteType()"})
        protected final Object doLoadSymbol(final ClassObject receiver, final NativeObject moduleSymbol, final NativeObject module) {
            final String moduleSymbolName = moduleSymbol.asStringUnsafe();
            final String moduleName = module.asStringUnsafe();
            final String ffiExtension = method.image.os.getFFIExtension();
            final String libPath = System.getProperty("user.dir") + File.separatorChar + "lib" + File.separatorChar + moduleName + ffiExtension;
            final String nfiCode = String.format("load \"%s\"", libPath);
            final Source source = Source.newBuilder("nfi", nfiCode, "native").build();
            final CallTarget target = method.image.env.parse(source);
            final Object library;
            try {
                library = target.call();
            } catch (final Throwable e) {
                throw PrimitiveFailed.andTransferToInterpreter();
            }
            final InteropLibrary lib = InteropLibrary.getFactory().getUncached();
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
                throw SqueakException.illegalState(e);
            }
            return newExternalAddress(receiver, pointer);
        }

        private static NativeObject newExternalAddress(final ClassObject externalAddressClass, final long pointer) {
            final byte[] bytes = new byte[8];
            bytes[0] = (byte) pointer;
            bytes[1] = (byte) (pointer >> 8);
            bytes[2] = (byte) (pointer >> 16);
            bytes[3] = (byte) (pointer >> 24);
            bytes[4] = (byte) (pointer >> 32);
            bytes[5] = (byte) (pointer >> 40);
            bytes[6] = (byte) (pointer >> 48);
            bytes[7] = (byte) (pointer >> 56);
            return NativeObject.newNativeBytes(externalAddressClass.image, externalAddressClass, bytes);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFFIIntegerAt")
    protected abstract static class PrimFFIIntegerAtNode extends AbstractPrimitiveNode implements QuaternaryPrimitive {
        protected PrimFFIIntegerAtNode(final CompiledMethodObject method) {
            super(method);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 2", "isSigned"})
        protected static final long doAt2Signed(final NativeObject byteArray, final long byteOffsetLong, final long byteSize, final boolean isSigned) {
            return (int) doAt2Unsigned(byteArray, byteOffsetLong, byteSize, false);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 2", "!isSigned"})
        protected static final long doAt2Unsigned(final NativeObject byteArray, final long byteOffsetLong, final long byteSize, final boolean isSigned) {
            final int byteOffset = (int) byteOffsetLong - 1;
            final byte[] bytes = byteArray.getByteStorage();
            return bytes[byteOffset] & 0xffL | (bytes[byteOffset + 1] & 0xffL) << 8;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 4", "isSigned"})
        protected static final long doAt4Signed(final NativeObject byteArray, final long byteOffsetLong, final long byteSize, final boolean isSigned) {
            return (int) doAt4Unsigned(byteArray, byteOffsetLong, byteSize, false);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 4", "!isSigned"})
        protected static final long doAt4Unsigned(final NativeObject byteArray, final long byteOffsetLong, final long byteSize, final boolean isSigned) {
            final int byteOffset = (int) byteOffsetLong - 1;
            final byte[] bytes = byteArray.getByteStorage();
            return bytes[byteOffset] & 0xffL | (bytes[byteOffset + 1] & 0xffL) << 8 |
                            (bytes[byteOffset + 2] & 0xffL) << 16 | (bytes[byteOffset + 3] & 0xffL) << 24;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 8", "isSigned"})
        protected static final Object doAt8Signed(final NativeObject byteArray, final long byteOffsetLong, final long byteSize, final boolean isSigned) {
            final int byteOffset = (int) byteOffsetLong - 1;
            final byte[] bytes = Arrays.copyOfRange(byteArray.getByteStorage(), byteOffset, byteOffset + 8);
            return new LargeIntegerObject(byteArray.image, byteArray.image.largePositiveIntegerClass, bytes).toSigned().reduceIfPossible();
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 8", "!isSigned"})
        protected static final Object doAt8Unsigned(final NativeObject byteArray, final long byteOffsetLong, final long byteSize, final boolean isSigned) {
            final int byteOffset = (int) byteOffsetLong - 1;
            final byte[] bytes = Arrays.copyOfRange(byteArray.getByteStorage(), byteOffset, byteOffset + 8);
            return new LargeIntegerObject(byteArray.image, byteArray.image.largePositiveIntegerClass, bytes).reduceIfPossible();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFFIIntegerAtPut")
    protected abstract static class PrimFFIIntegerAtPutNode extends AbstractPrimitiveNode implements QuinaryPrimitive {
        protected static final long MAX_VALUE_SIGNED_1 = 1L << 8 * 1 - 1;
        protected static final long MAX_VALUE_SIGNED_2 = 1L << 8 * 2 - 1;
        protected static final long MAX_VALUE_SIGNED_4 = 1L << 8 * 4 - 1;
        protected static final BigInteger MAX_VALUE_SIGNED_8 = BigInteger.ONE.shiftLeft(8 * 8 - 1);
        protected static final long MAX_VALUE_UNSIGNED_1 = 1L << 8 * 1;
        protected static final long MAX_VALUE_UNSIGNED_2 = 1L << 8 * 2;
        protected static final long MAX_VALUE_UNSIGNED_4 = 1L << 8 * 4;

        protected PrimFFIIntegerAtPutNode(final CompiledMethodObject method) {
            super(method);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 1", "isSigned", "inSignedBounds(value, MAX_VALUE_SIGNED_1)"})
        protected static final Object doAtPut1Signed(final NativeObject byteArray, final long byteOffsetLong, final long value, final long byteSize, final boolean isSigned) {
            return doAtPut1Unsigned(byteArray, byteOffsetLong, value, byteSize, isSigned);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 1", "!isSigned", "inUnsignedBounds(value, MAX_VALUE_UNSIGNED_1)"})
        protected static final Object doAtPut1Unsigned(final NativeObject byteArray, final long byteOffsetLong, final long value, final long byteSize, final boolean isSigned) {
            byteArray.getByteStorage()[(int) byteOffsetLong - 1] = (byte) value;
            return value;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 2", "isSigned", "inSignedBounds(value, MAX_VALUE_SIGNED_2)"})
        protected static final Object doAtPut2Signed(final NativeObject byteArray, final long byteOffsetLong, final long value, final long byteSize, final boolean isSigned) {
            return doAtPut2Unsigned(byteArray, byteOffsetLong, value, byteSize, isSigned);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 2", "!isSigned", "inUnsignedBounds(value, MAX_VALUE_UNSIGNED_2)"})
        protected static final Object doAtPut2Unsigned(final NativeObject byteArray, final long byteOffsetLong, final long value, final long byteSize, final boolean isSigned) {
            final int byteOffset = (int) byteOffsetLong - 1;
            final byte[] bytes = byteArray.getByteStorage();
            bytes[byteOffset] = (byte) value;
            bytes[byteOffset + 1] = (byte) (value >> 8);
            return value;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 4", "isSigned", "inSignedBounds(value, MAX_VALUE_SIGNED_4)"})
        protected static final Object doAtPut4Signed(final NativeObject byteArray, final long byteOffsetLong, final long value, final long byteSize, final boolean isSigned) {
            return doAtPut4Unsigned(byteArray, byteOffsetLong, value, byteSize, isSigned);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 4", "!isSigned", "inUnsignedBounds(value, MAX_VALUE_UNSIGNED_4)"})
        protected static final Object doAtPut4Unsigned(final NativeObject byteArray, final long byteOffsetLong, final long value, final long byteSize, final boolean isSigned) {
            final int byteOffset = (int) byteOffsetLong - 1;
            final byte[] bytes = byteArray.getByteStorage();
            bytes[byteOffset] = (byte) value;
            bytes[byteOffset + 1] = (byte) (value >> 8);
            bytes[byteOffset + 2] = (byte) (value >> 16);
            bytes[byteOffset + 3] = (byte) (value >> 24);
            return value;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 4", "isSigned", "value.fitsIntoLong()", "inSignedBounds(value.longValueExact(), MAX_VALUE_SIGNED_4)"})
        protected static final Object doAtPut4SignedLarge(final NativeObject byteArray, final long byteOffsetLong, final LargeIntegerObject value, final long byteSize, final boolean isSigned) {
            return doAtPut4UnsignedLarge(byteArray, byteOffsetLong, value, byteSize, isSigned);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 4", "!isSigned", "value.fitsIntoLong()",
                        "inUnsignedBounds(value.longValueExact(), MAX_VALUE_UNSIGNED_4)"})
        @ExplodeLoop
        protected static final Object doAtPut4UnsignedLarge(final NativeObject byteArray, final long byteOffsetLong, final LargeIntegerObject value, final long byteSize, final boolean isSigned) {
            final int byteOffset = (int) byteOffsetLong - 1;
            final byte[] targetBytes = byteArray.getByteStorage();
            final byte[] sourceBytes = value.getBytes();
            final int numSourceBytes = sourceBytes.length;
            for (int i = 0; i < 4; i++) {
                targetBytes[byteOffset + i] = i < numSourceBytes ? sourceBytes[i] : 0;
            }
            return value;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 8", "isSigned"})
        protected static final Object doAtPut8Signed(final NativeObject byteArray, final long byteOffsetLong, final long value, final long byteSize, final boolean isSigned) {
            return doAtPut8Unsigned(byteArray, byteOffsetLong, value, byteSize, isSigned);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"byteArray.isByteType()", "byteOffsetLong > 0", "byteSize == 8", "!isSigned", "inUnsignedBounds(asLargeInteger(value))"})
        protected static final Object doAtPut8Unsigned(final NativeObject byteArray, final long byteOffsetLong, final long value, final long byteSize, final boolean isSigned) {
            final int byteOffset = (int) byteOffsetLong - 1;
            final byte[] bytes = byteArray.getByteStorage();
            bytes[byteOffset] = (byte) value;
            bytes[byteOffset + 1] = (byte) (value >> 8);
            bytes[byteOffset + 2] = (byte) (value >> 16);
            bytes[byteOffset + 3] = (byte) (value >> 24);
            bytes[byteOffset + 4] = (byte) (value >> 32);
            bytes[byteOffset + 5] = (byte) (value >> 40);
            bytes[byteOffset + 6] = (byte) (value >> 48);
            bytes[byteOffset + 7] = (byte) (value >> 56);
            return value;
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
            final int byteOffset = (int) byteOffsetLong - 1;
            final byte[] targetBytes = byteArray.getByteStorage();
            final byte[] sourceBytes = value.getBytes();
            final int numSourceBytes = sourceBytes.length;
            for (int i = 0; i < 8; i++) {
                targetBytes[byteOffset + i] = i < numSourceBytes ? sourceBytes[i] : 0;
            }
            return value;
        }

        protected static final boolean inSignedBounds(final long value, final long max) {
            return value >= 0 - max && value < max;
        }

        protected static final boolean inUnsignedBounds(final long value, final long max) {
            return 0 <= value && value < max;
        }

        @TruffleBoundary
        protected static final boolean inSignedBounds(final LargeIntegerObject value, final BigInteger max) {
            return value.getBigInteger().compareTo(BigInteger.ZERO.subtract(max)) >= 0 && value.getBigInteger().compareTo(max) < 0;
        }

        @TruffleBoundary
        protected static final boolean inUnsignedBounds(final LargeIntegerObject value) {
            return value.isZeroOrPositive() && value.lessThanOneShiftedBy64();
        }
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return SqueakFFIPrimsFactory.getFactories();
    }
}
