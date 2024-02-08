/*
 * Copyright (c) 2017-2024 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2024 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins.ffi;

import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.AbstractPointersObject;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.FloatObject;
import de.hpi.swa.trufflesqueak.model.LargeIntegerObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.ERROR_TABLE;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectNewNode;
import de.hpi.swa.trufflesqueak.nodes.plugins.ffi.wrappers.NativeObjectStorage;
import de.hpi.swa.trufflesqueak.nodes.plugins.ffi.wrappers.PostPrimitiveCleanup;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.LogUtils;
import de.hpi.swa.trufflesqueak.util.MiscUtils;
import de.hpi.swa.trufflesqueak.util.NFIUtils;
import de.hpi.swa.trufflesqueak.util.NFIUtils.TruffleClosure;
import de.hpi.swa.trufflesqueak.util.NFIUtils.TruffleExecutable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class InterpreterProxy {
    private static InterpreterProxy INSTANCE;
    private final SqueakImageContext context;
    private MaterializedFrame frame;
    private int numReceiverAndArguments;
    private final ArrayList<PostPrimitiveCleanup> postPrimitiveCleanups = new ArrayList<>();
    // should not be local, as the references are needed to keep the native closures alive
    // since this class is a singleton, a private instance variable will suffice
    @SuppressWarnings("FieldCanBeLocal") private final TruffleClosure[] closures;
    private final Object interpreterProxyPointer;

    ///////////////////////////
    // INTERPRETER VARIABLES //
    ///////////////////////////
    private final ArrayList<Object> objectRegistry = new ArrayList<>();
    private long primFailCode;

    ///////////////////////
    // INSTANCE CREATION //
    ///////////////////////

    private InterpreterProxy(final SqueakImageContext context, final MaterializedFrame frame, final int numReceiverAndArguments)
                    throws UnsupportedMessageException, UnknownIdentifierException, UnsupportedTypeException, ArityException {
        this.context = context;
        this.frame = frame;
        this.numReceiverAndArguments = numReceiverAndArguments;

        final TruffleExecutable[] executables = getExecutables();
        closures = new TruffleClosure[executables.length];
        for (int i = 0; i < executables.length; i++) {
            closures[i] = executables[i].createClosure(context);
        }

        final String truffleExecutablesSignatures = Arrays.stream(closures).map(obj -> obj.executable.nfiSignature).collect(Collectors.joining(","));
        final Object interpreterProxy = NFIUtils.loadLibrary(context, "InterpreterProxy",
                        "{ createInterpreterProxy(" + truffleExecutablesSignatures + "):POINTER; }");
        assert interpreterProxy != null : "InterpreterProxy module not found!";

        final InteropLibrary interpreterProxyLibrary = NFIUtils.getInteropLibrary(interpreterProxy);
        interpreterProxyPointer = interpreterProxyLibrary.invokeMember(
                        interpreterProxy, "createInterpreterProxy", (Object[]) closures);
    }

    private TruffleExecutable[] getExecutables() {
        // sorted alphabetically, identical to createInterpreterProxy in
        // src/de.hpi.swa.trufflesqueak.ffi.native/src/InterpreterProxy.c
        return new TruffleExecutable[]{
                        TruffleExecutable.wrap("(SINT64):SINT64", this::byteSizeOf),
                        TruffleExecutable.wrap("():SINT64", this::classString),
                        TruffleExecutable.wrap("():SINT64", this::failed),
                        TruffleExecutable.wrap("(SINT64,SINT64):SINT64", this::fetchIntegerofObject),
                        TruffleExecutable.wrap("(SINT64,SINT64):SINT64", this::fetchLong32ofObject),
                        TruffleExecutable.wrap("(SINT64,SINT64):SINT64", this::fetchPointerofObject),
                        TruffleExecutable.wrap("(SINT64):POINTER", this::firstIndexableField),
                        TruffleExecutable.wrap("(SINT64):DOUBLE", this::floatValueOf),
                        TruffleExecutable.wrap("(SINT64,SINT64):SINT64", this::instantiateClassindexableSize),
                        TruffleExecutable.wrap("(SINT64):SINT64", this::integerObjectOf),
                        TruffleExecutable.wrap("(SINT64):SINT64", this::integerValueOf),
                        TruffleExecutable.wrap("(STRING,STRING):POINTER", this::ioLoadFunctionFrom),
                        TruffleExecutable.wrap("(SINT64):SINT64", this::isArray),
                        TruffleExecutable.wrap("(SINT64):SINT64", this::isBytes),
                        TruffleExecutable.wrap("(SINT64):SINT64", this::isPointers),
                        TruffleExecutable.wrap("(SINT64):SINT64", this::isPositiveMachineIntegerObject),
                        TruffleExecutable.wrap("(SINT64):SINT64", this::isWords),
                        TruffleExecutable.wrap("(SINT64):SINT64", this::isWordsOrBytes),
                        TruffleExecutable.wrap("():SINT64", this::majorVersion),
                        TruffleExecutable.wrap("():SINT64", this::methodArgumentCount),
                        TruffleExecutable.wrap("(SINT64):SINT64", this::methodReturnInteger),
                        TruffleExecutable.wrap("():SINT64", this::methodReturnReceiver),
                        TruffleExecutable.wrap("(SINT64):SINT64", this::methodReturnValue),
                        TruffleExecutable.wrap("():SINT64", this::minorVersion),
                        TruffleExecutable.wrap("():SINT64", this::nilObject),
                        TruffleExecutable.wrap("(SINT64):SINT64", this::pop),
                        TruffleExecutable.wrap("(SINT64,SINT64):SINT64", this::popthenPush),
                        TruffleExecutable.wrap("(UINT64):SINT64", this::positive32BitIntegerFor),
                        TruffleExecutable.wrap("(SINT64):UINT64", this::positive32BitValueOf),
                        TruffleExecutable.wrap("(SINT64):UINT64", this::positive64BitValueOf),
                        TruffleExecutable.wrap("():SINT64", this::primitiveFail),
                        TruffleExecutable.wrap("(SINT64):SINT64", this::primitiveFailFor),
                        TruffleExecutable.wrap("(SINT64):SINT64", this::pushInteger),
                        TruffleExecutable.wrap("(SINT64,SINT64,SINT64,SINT64,SINT64):SINT64", this::showDisplayBitsLeftTopRightBottom),
                        TruffleExecutable.wrap("(SINT64):SINT64", this::signed32BitIntegerFor),
                        TruffleExecutable.wrap("(SINT64):SINT64", this::signed32BitValueOf),
                        TruffleExecutable.wrap("(SINT64):SINT64", this::slotSizeOf),
                        TruffleExecutable.wrap("(SINT64):SINT64", this::stackIntegerValue),
                        TruffleExecutable.wrap("(SINT64):SINT64", this::stackObjectValue),
                        TruffleExecutable.wrap("(SINT64):SINT64", this::stackValue),
                        TruffleExecutable.wrap("():SINT64", this::statNumGCs),
                        TruffleExecutable.wrap("(SINT64,SINT64,SINT64):SINT64", this::storeIntegerofObjectwithValue),
                        TruffleExecutable.wrap("(SINT64,SINT64,UINT64):UINT64", this::storeLong32ofObjectwithValue),
        };
    }

    public static InterpreterProxy instanceFor(final SqueakImageContext context, final MaterializedFrame frame, final int numReceiverAndArguments)
                    throws UnsupportedMessageException, UnknownIdentifierException, UnsupportedTypeException, ArityException {
        if (INSTANCE == null) {
            INSTANCE = new InterpreterProxy(context, frame, numReceiverAndArguments);
            return INSTANCE;
        }
        assert INSTANCE.context == context : "received new SqueakImageContext";

        INSTANCE.frame = frame;
        INSTANCE.numReceiverAndArguments = numReceiverAndArguments;
        return INSTANCE;
    }

    ///////////////////
    // MISCELLANEOUS //
    ///////////////////

    public static Object getPointer() {
        return INSTANCE.interpreterProxyPointer;
    }

    public void postPrimitiveCleanups() {
        postPrimitiveCleanups.forEach(PostPrimitiveCleanup::cleanup);
        postPrimitiveCleanups.clear();
    }

    private boolean hasSucceeded() {
        return failed() == 0;
    }

    private Object global(final String name) {
        return context.lookup(name);
    }

    /////////////////////////////
    // OBJECT REGISTRY HELPERS //
    /////////////////////////////

    private Object objectRegistryGet(final long oop) {
        return objectRegistry.get((int) oop);
    }

    private int addObjectToRegistry(final Object object) {
        final int oop = objectRegistry.size();
        objectRegistry.add(object);
        return oop;
    }

    private int oopFor(final Object object) {
        for (int oop = 0; oop < objectRegistry.size(); oop++) {
            if (objectRegistry.get(oop) == object) {
                return oop;
            }
        }
        return addObjectToRegistry(object);
    }

    ///////////////////
    // STACK HELPERS //
    ///////////////////

    private int getStackPointer() {
        return FrameAccess.getStackPointer(frame);
    }

    private void setStackPointer(final int stackPointer) {
        FrameAccess.setStackPointer(frame, stackPointer);
    }

    private void pushObject(final Object object) {
        final int stackPointer = getStackPointer();
        setStackPointer(stackPointer + 1);
        // push to the original stack pointer, as it always points to the slot where the next object
        // is pushed
        FrameAccess.setStackSlot(frame, stackPointer, object);
    }

    private Object getObjectOnStack(final long reverseStackIndex) {
        if (reverseStackIndex < 0) {
            primitiveFail();
            return null;
        }
        // the stack pointer is the index of the object that is pushed onto the stack next,
        // so we subtract 1 to get the index of the object that was last pushed onto the stack
        final int stackIndex = getStackPointer() - 1 - (int) reverseStackIndex;
        if (stackIndex < 0) {
            primitiveFail();
            return null;
        }
        return FrameAccess.getStackValue(frame, stackIndex, FrameAccess.getNumArguments(frame));
    }

    private long methodReturnObject(Object object) {
        assert hasSucceeded();
        pop(numReceiverAndArguments);
        pushObject(object);
        return 0;
    }

    private static long returnBoolean(final boolean bool) {
        return bool ? 1L : 0L;
    }

    private static long returnVoid() {
        // For functions that do not have a defined return value
        return 0L;
    }

    private static long returnNull() {
        // For functions that should return null (=0)
        return 0L;
    }

    ////////////////////////
    // CONVERSION HELPERS //
    ////////////////////////

    private long objectToInteger(final Object object) {
        if (object instanceof Long longObject) {
            return longObject;
        }
        LogUtils.PRIMITIVES.severe(() -> "Object to long called with non-Long: " + object);
        primitiveFail();
        return returnNull();
    }

    private double objectToFloat(final Object object) {
        if (object instanceof FloatObject floatObject) {
            return floatObject.getValue();
        }
        LogUtils.PRIMITIVES.severe(() -> "Object to long called with non-FloatObject: " + object);
        primitiveFail();
        return returnNull();
    }

    private static Object integerToObject(final long integer) {
        return integer; // encoded as Long in TruffleSqueak
    }

    private static Object boolToObject(final boolean bool) {
        return BooleanObject.wrap(bool);
    }

    private static Object boolToObject(final long bool) {
        return boolToObject(bool != 0);
    }

    private Object floatToObject(final double value) {
        return new FloatObject(context, value);
    }

    private Object stringToObject(final String string) {
        return context.asByteString(string);
    }

    ///////////////////////
    // ACCESSING HELPERS //
    ///////////////////////

    private static Object objectAt0(final Object object, final long index) {
        return SqueakObjectAt0Node.executeUncached(object, index);
    }

    ////////////////////////
    // TYPE CHECK HELPERS //
    ////////////////////////

    private long instanceOfCheck(final long oop, final Class<?> klass) {
        final Object object = objectRegistryGet(oop);
        return returnBoolean(klass.isInstance(object));
    }

    private long nativeObjectCheck(final long oop, final Predicate<NativeObject> predicate) {
        final Object object = objectRegistryGet(oop);
        if (object instanceof NativeObject nativeObject) {
            return returnBoolean(predicate.test(nativeObject));
        }
        return returnVoid();
    }

    ///////////////////////////////
    // INTERPRETER PROXY METHODS //
    ///////////////////////////////

    @SuppressWarnings("unused")
    private long booleanValueOf(final long oop) {
        Object object = objectRegistryGet(oop);
        if (object instanceof Boolean bool) {
            return returnBoolean(bool);
        }
        primitiveFail();
        return returnNull();
    }

    @SuppressWarnings("unused")
    private long byteSizeOf(final long oop) {
        if (objectRegistryGet(oop) instanceof NativeObject nativeObject) {
            return NativeObjectStorage.from(nativeObject).byteSizeOf();
        }
        // type is not supported (yet)
        primitiveFail();
        return 0L;
    }

    @SuppressWarnings("unused")
    private long classAlien() {
        return oopFor(global("Alien"));
    }

    @SuppressWarnings("unused")
    private long classArray() {
        return oopFor(context.arrayClass);
    }

    @SuppressWarnings("unused")
    private long classBitmap() {
        return oopFor(context.bitmapClass);
    }

    @SuppressWarnings("unused")
    private long classByteArray() {
        return oopFor(context.byteArrayClass);
    }

    @SuppressWarnings("unused")
    private long classCharacter() {
        return oopFor(context.characterClass);
    }

    @SuppressWarnings("unused")
    private long classDoubleByteArray() {
        return oopFor(global("DoubleByteArray"));
    }

    @SuppressWarnings("unused")
    private long classDoubleWordArray() {
        return oopFor(global("DoubleWordArray"));
    }

    @SuppressWarnings("unused")
    private long classExternalAddress() {
        return oopFor(global("ExternalAddress"));
    }

    @SuppressWarnings("unused")
    private long classExternalData() {
        return oopFor(global("ExternalData"));
    }

    @SuppressWarnings("unused")
    private long classExternalFunction() {
        return oopFor(global("ExternalFunction"));
    }

    @SuppressWarnings("unused")
    private long classExternalLibrary() {
        return oopFor(global("ExternalLibrary"));
    }

    @SuppressWarnings("unused")
    private long classExternalStructure() {
        return oopFor(global("ExternalStructure"));
    }

    @SuppressWarnings("unused")
    private long classFloat() {
        return oopFor(context.floatClass);
    }

    @SuppressWarnings("unused")
    private long classFloat32Array() {
        return oopFor(global("FloatArray"));
    }

    @SuppressWarnings("unused")
    private long classFloat64Array() {
        return oopFor(global("Float64Array"));
    }

    @SuppressWarnings("unused")
    private long classLargeNegativeInteger() {
        return oopFor(context.largeNegativeIntegerClass);
    }

    @SuppressWarnings("unused")
    private long classLargePositiveIntegerClass() {
        return oopFor(context.largePositiveIntegerClass);
    }

    @SuppressWarnings("unused")
    private long classPoint() {
        return oopFor(context.pointClass);
    }

    @SuppressWarnings("unused")
    private long classSemaphore() {
        return oopFor(context.semaphoreClass);
    }

    @SuppressWarnings("unused")
    private long classSmallInteger() {
        return oopFor(context.smallIntegerClass);
    }

    @SuppressWarnings("unused")
    private long classString() {
        return oopFor(context.byteStringClass);
    }

    @SuppressWarnings("unused")
    private long classUnsafeAlien() {
        return oopFor(global("UnsafeAlien"));
    }

    @SuppressWarnings("unused")
    private long classWordArray() {
        return oopFor(global("WordArray"));
    }

    @SuppressWarnings("unused")
    public long failed() {
        return primFailCode;
    }

    @SuppressWarnings("unused")
    private long fetchIntegerofObject(final long fieldIndex, final long objectPointer) {
        return objectToInteger(objectAt0(objectRegistryGet(objectPointer), fieldIndex));
    }

    @SuppressWarnings("unused")
    private long fetchLong32ofObject(final long fieldIndex, final long oop) {
        return fetchIntegerofObject(fieldIndex, oop);
    }

    @SuppressWarnings("unused")
    private long fetchPointerofObject(final long index, final long oop) {
        return oopFor(objectAt0(objectRegistryGet(oop), index));
    }

    @SuppressWarnings("unused")
    private NativeObjectStorage firstIndexableField(final long oop) {
        if (objectRegistryGet(oop) instanceof NativeObject nativeObject) {
            final NativeObjectStorage storage = NativeObjectStorage.from(nativeObject);
            postPrimitiveCleanups.add(storage);
            return storage;
        }
        return null;
    }

    @SuppressWarnings("unused")
    private double floatValueOf(final long oop) {
        return objectToFloat(objectRegistryGet(oop));
    }

    @SuppressWarnings("unused")
    private long instantiateClassindexableSize(final long classPointer, final long size) {
        final Object object = objectRegistryGet(classPointer);
        if (object instanceof ClassObject classObject) {
            final AbstractSqueakObject newObject = SqueakObjectNewNode.executeUncached(context, classObject, (int) size);
            return oopFor(newObject);
        }
        LogUtils.PRIMITIVES.severe(() -> "instantiateClassindexableSize called with non-ClassObject: " + object);
        primitiveFail();
        return returnVoid();
    }

    @SuppressWarnings("unused")
    private long integerObjectOf(final long value) {
        return oopFor(integerToObject(value));
    }

    @SuppressWarnings("unused")
    private long integerValueOf(final long oop) {
        return objectToInteger(objectRegistryGet(oop));
    }

    @SuppressWarnings("unused")
    private NativeObjectStorage ioLoadFunctionFrom(final String functionName, final String moduleName) {
        /* TODO */
        LogUtils.PRIMITIVES.severe(() -> "Missing implementation for ioLoadFunctionFrom");
        return null;
    }

    @SuppressWarnings("unused")
    private long isArray(final long oop) {
        return instanceOfCheck(oop, ArrayObject.class);
    }

    @SuppressWarnings("unused")
    private long isBytes(final long oop) {
        return nativeObjectCheck(oop, NativeObject::isByteType);
    }

    @SuppressWarnings("unused")
    private long isIntegerObject(final long oop) {
        return returnBoolean(objectRegistryGet(oop) instanceof Long);
    }

    @SuppressWarnings("unused")
    private long isPointers(final long oop) {
        return instanceOfCheck(oop, AbstractPointersObject.class);
    }

    @SuppressWarnings("unused")
    private long isPositiveMachineIntegerObject(final long oop) {
        final Object object = objectRegistryGet(oop);
        if (object instanceof Long integer) {
            return returnBoolean(integer >= 0L);
        }
        if (object instanceof LargeIntegerObject largeInteger) {
            return returnBoolean(largeInteger.isZeroOrPositive() && largeInteger.fitsIntoLong());
        }
        return returnBoolean(false);
    }

    @SuppressWarnings("unused")
    private long isWords(final long oop) {
        return nativeObjectCheck(oop, NativeObject::isLongType);
    }

    @SuppressWarnings("unused")
    private long isWordsOrBytes(final long oop) {
        /* TODO */
        LogUtils.PRIMITIVES.warning("Missing implementation for isWordsOrBytes");
        return returnVoid();
    }

    @SuppressWarnings("unused")
    private long majorVersion() {
        return 1L;
    }

    @SuppressWarnings("unused")
    private long methodArgumentCount() {
        return numReceiverAndArguments - 1;
    }

    @SuppressWarnings("unused")
    private long methodReturnBool(final long bool) {
        return methodReturnObject(boolToObject(bool));
    }

    @SuppressWarnings("unused")
    private long methodReturnFloat(final double value) {
        return methodReturnObject(floatToObject(value));
    }

    @SuppressWarnings("unused")
    private long methodReturnInteger(final long integer) {
        return methodReturnObject(integerToObject(integer));
    }

    @SuppressWarnings("unused")
    private long methodReturnReceiver() {
        assert hasSucceeded();
        pop(numReceiverAndArguments - 1); // leave the receiver on the stack
        return 0;
    }

    @SuppressWarnings("unused")
    private long methodReturnString(final String string) {
        return methodReturnObject(stringToObject(string));
    }

    @SuppressWarnings("unused")
    private long methodReturnValue(final long oop) {
        return methodReturnObject(objectRegistryGet(oop));
    }

    @SuppressWarnings("unused")
    private long minorVersion() {
        return 17;
    }

    @SuppressWarnings("unused")
    private long nilObject() {
        return oopFor(NilObject.SINGLETON);
    }

    @SuppressWarnings("unused")
    private long pop(final long nItems) {
        setStackPointer(getStackPointer() - (int) nItems);
        return returnNull();
    }

    @SuppressWarnings("unused")
    private long popthenPush(final long nItems, final long oop) {
        pop(nItems);
        push(oop);
        return returnVoid();
    }

    @SuppressWarnings("unused")
    private long positive32BitIntegerFor(final long integerValue) {
        /* TODO */
        LogUtils.PRIMITIVES.warning("Missing implementation for positive32BitIntegerFor");
        return returnVoid();
    }

    @SuppressWarnings("unused")
    private long positive32BitValueOf(final long oop) {
        /* TODO */
        LogUtils.PRIMITIVES.warning("Missing implementation for positive32BitValueOf");
        return returnVoid();
    }

    @SuppressWarnings("unused")
    private long positive64BitValueOf(final long oop) {
        /* TODO */
        LogUtils.PRIMITIVES.warning("Missing implementation for positive64BitValueOf");
        return returnVoid();
    }

    @SuppressWarnings("unused")
    private long primitiveFail() {
        return primitiveFailFor(ERROR_TABLE.GENERIC_ERROR.ordinal());
    }

    @SuppressWarnings("unused")
    private long primitiveFailFor(final long reasonCode) {
        LogUtils.PRIMITIVES.info(() -> "Primitive failed with code: " + reasonCode);
        return primFailCode = reasonCode;
    }

    @SuppressWarnings("unused")
    private long push(final long oop) {
        pushObject(objectRegistryGet(oop));
        return returnVoid();
    }

    @SuppressWarnings("unused")
    private long pushInteger(final long integer) {
        pushObject(integerToObject(integer));
        return returnNull();
    }

    @SuppressWarnings("unused")
    private long showDisplayBitsLeftTopRightBottom(final long aForm, final long l, final long t, final long r, final long b) {
        /* TODO */
        LogUtils.PRIMITIVES.warning("Missing implementation for showDisplayBitsLeftTopRightBottom");
        return returnVoid();
    }

    @SuppressWarnings("unused")
    private long signed32BitIntegerFor(final long integerValue) {
        return integerObjectOf(integerValue);
    }

    @SuppressWarnings("unused")
    private long signed32BitValueOf(final long oop) {
        return integerValueOf(oop);
    }

    @SuppressWarnings("unused")
    private long slotSizeOf(final long oop) {
        /* TODO */
        LogUtils.PRIMITIVES.warning("Missing implementation for slotSizeOf");
        return returnVoid();
    }

    @SuppressWarnings("unused")
    private long stackIntegerValue(final long offset) {
        return objectToInteger(getObjectOnStack(offset));
    }

    @SuppressWarnings("unused")
    private long stackObjectValue(final long offset) {
        /* TODO */
        LogUtils.PRIMITIVES.warning("Missing implementation for stackObjectValue");
        return returnVoid();
    }

    @SuppressWarnings("unused")
    private long stackValue(final long offset) {
        return oopFor(getObjectOnStack(offset));
    }

    @SuppressWarnings("unused")
    private long statNumGCs() {
        return MiscUtils.getCollectionCount();
    }

    @SuppressWarnings("unused")
    private long stringForCString(final String string) {
        return oopFor(stringForCString(string));
    }

    @SuppressWarnings("unused")
    private long storeIntegerofObjectwithValue(final long index, final long oop, final long integer) {
        /* TODO */
        LogUtils.PRIMITIVES.warning("Missing implementation for storeIntegerofObjectwithValue");
        return returnVoid();
    }

    @SuppressWarnings("unused")
    private long storeLong32ofObjectwithValue(final long fieldIndex, final long oop, final long anInteger) {
        /* TODO */
        LogUtils.PRIMITIVES.warning("Missing implementation for storeLong32ofObjectwithValue");
        return returnVoid();
    }
}
