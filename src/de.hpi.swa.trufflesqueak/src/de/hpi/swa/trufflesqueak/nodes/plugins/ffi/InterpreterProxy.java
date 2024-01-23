package de.hpi.swa.trufflesqueak.nodes.plugins.ffi;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
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

    @TruffleBoundary
    private int oopFor(final Object object) {
        int oop = objectRegistry.indexOf(object);
        if (oop < 0) {
            oop = addObjectToRegistry(object);
        }
        return oop;
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

    private long objectToLong(final Object object) {
        if (object instanceof Long longObject) {
            return longObject;
        }
        LogUtils.PRIMITIVES.severe(() -> "Object to long called with non-Long: " + object);
        primitiveFail();
        return returnNull();
    }

    private double objectToDouble(final Object object) {
        if (object instanceof FloatObject floatObject) {
            return floatObject.getValue();
        }
        LogUtils.PRIMITIVES.severe(() -> "Object to long called with non-FloatObject: " + object);
        primitiveFail();
        return returnNull();
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
        return klass.isInstance(object) ? 1 : 0;
    }

    private long nativeObjectCheck(final long oop, final Predicate<NativeObject> predicate) {
        final Object object = objectRegistryGet(oop);
        if (object instanceof NativeObject nativeObject) {
            return predicate.test(nativeObject) ? 1 : 0;
        }
        return returnVoid();
    }

    ///////////////////////////////
    // INTERPRETER PROXY METHODS //
    ///////////////////////////////

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
    private long classString() {
        return oopFor(context.byteStringClass);
    }

    @SuppressWarnings("unused")
    private long failed() {
        return primFailCode;
    }

    @SuppressWarnings("unused")
    private long fetchIntegerofObject(final long fieldIndex, final long objectPointer) {
        return objectToLong(objectAt0(objectRegistryGet(objectPointer), fieldIndex));
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
        return objectToDouble(objectRegistryGet(oop));
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
        return oopFor(value);
    }

    @SuppressWarnings("unused")
    private long integerValueOf(final long oop) {
        return objectToLong(objectRegistryGet(oop));
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
    private long isPointers(final long oop) {
        return instanceOfCheck(oop, AbstractPointersObject.class);
    }

    @SuppressWarnings("unused")
    private long isPositiveMachineIntegerObject(final long oop) {
        final Object object = objectRegistryGet(oop);
        if (object instanceof Long integer) {
            return integer >= 0L ? 1L : 0L;
        }
        if (object instanceof LargeIntegerObject largeInteger) {
            return largeInteger.isZeroOrPositive() && largeInteger.fitsIntoLong() ? 1L : 0L;
        }
        return returnVoid();
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
    private long methodReturnInteger(final long integer) {
        /* TODO */
        LogUtils.PRIMITIVES.warning("Missing implementation for methodReturnInteger");
        return returnVoid();
    }

    @SuppressWarnings("unused")
    private long methodReturnReceiver() {
        /* TODO */
        LogUtils.PRIMITIVES.warning("Missing implementation for methodReturnReceiver");
        return returnVoid();
    }

    @SuppressWarnings("unused")
    private long methodReturnValue(final long oop) {
        /* TODO */
        LogUtils.PRIMITIVES.warning("Missing implementation for methodReturnValue");
        return returnVoid();
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
    private long pushInteger(final long integerValue) {
        pushObject(integerValue);
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
        return objectToLong(getObjectOnStack(offset));
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
