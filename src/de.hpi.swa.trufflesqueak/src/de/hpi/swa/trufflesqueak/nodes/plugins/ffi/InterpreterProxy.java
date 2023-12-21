package de.hpi.swa.trufflesqueak.nodes.plugins.ffi;

import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.AbstractPointersObject;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.FloatObject;
import de.hpi.swa.trufflesqueak.model.LargeIntegerObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectNewNode;
import de.hpi.swa.trufflesqueak.nodes.plugins.ffi.wrappers.NativeObjectStorage;
import de.hpi.swa.trufflesqueak.nodes.plugins.ffi.wrappers.PostPrimitiveCleanup;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.MiscUtils;
import de.hpi.swa.trufflesqueak.util.NFIUtils;
import de.hpi.swa.trufflesqueak.util.NFIUtils.TruffleClosure;
import de.hpi.swa.trufflesqueak.util.NFIUtils.TruffleExecutable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class InterpreterProxy {
    private static InterpreterProxy INSTANCE = null;
    private final SqueakImageContext context;
    private MaterializedFrame frame;
    private int numReceiverAndArguments;
    private final ArrayList<Object> objectRegistry = new ArrayList<>();
    private final ArrayList<PostPrimitiveCleanup> postPrimitiveCleanups = new ArrayList<>();
    // should not be local, as the references are needed to keep the native closures alive
    // since this class is a singleton, a private instance variable will suffice
    @SuppressWarnings("FieldCanBeLocal") private final TruffleClosure[] closures;
    private static Object interpreterProxyPointer = null;

    ///////////////////////
    // INSTANCE CREATION //
    ///////////////////////

    private InterpreterProxy(SqueakImageContext context, MaterializedFrame frame, int numReceiverAndArguments)
                    throws UnsupportedMessageException, UnknownIdentifierException, UnsupportedTypeException, ArityException {
        this.context = context;
        this.frame = frame;
        this.numReceiverAndArguments = numReceiverAndArguments;

        TruffleExecutable[] executables = getExecutables();
        closures = new TruffleClosure[executables.length];
        for (int i = 0; i < executables.length; i++) {
            closures[i] = executables[i].createClosure(context);
        }

        if (interpreterProxyPointer == null) {
            final String truffleExecutablesSignatures = Arrays.stream(closures).map(obj -> obj.executable.nfiSignature).collect(Collectors.joining(","));
            final Object interpreterProxy = NFIUtils.loadLibrary(context, "InterpreterProxy",
                            "{ createInterpreterProxy(" + truffleExecutablesSignatures + "):POINTER; }");

            final InteropLibrary interpreterProxyLibrary = NFIUtils.getInteropLibrary(interpreterProxy);
            interpreterProxyPointer = interpreterProxyLibrary.invokeMember(
                            interpreterProxy, "createInterpreterProxy", (Object[]) closures);
        }
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

    public static InterpreterProxy instanceFor(SqueakImageContext context, MaterializedFrame frame, int numReceiverAndArguments)
                    throws UnsupportedMessageException, UnknownIdentifierException, UnsupportedTypeException, ArityException {
        if (INSTANCE == null) {
            INSTANCE = new InterpreterProxy(context, frame, numReceiverAndArguments);
            return INSTANCE;
        }
        if (INSTANCE.context != context) {
            throw new RuntimeException("InterpreterProxy does not support multiple SqueakImageContexts");
        }
        INSTANCE.frame = frame;
        INSTANCE.numReceiverAndArguments = numReceiverAndArguments;
        return INSTANCE;
    }

    ///////////////////
    // MISCELLANEOUS //
    ///////////////////

    public Object getPointer() {
        return interpreterProxyPointer;
    }

    public void postPrimitiveCleanups() {
        postPrimitiveCleanups.forEach(PostPrimitiveCleanup::cleanup);
        postPrimitiveCleanups.clear();
    }

    /////////////////////////////
    // OBJECT REGISTRY HELPERS //
    /////////////////////////////

    private Object objectRegistryGet(long oop) {
        return objectRegistry.get((int) oop);
    }

    private int addObjectToRegistry(Object object) {
        int oop = objectRegistry.size();
        objectRegistry.add(object);
        return oop;
    }

    private int oopFor(Object object) {
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

    private void setStackPointer(int stackPointer) {
        FrameAccess.setStackPointer(frame, stackPointer);
    }

    private void pushObject(Object object) {
        int stackPointer = getStackPointer();
        setStackPointer(stackPointer + 1);
        // push to the original stack pointer, as it always points to the slot where the next object
        // is pushed
        FrameAccess.setStackSlot(frame, stackPointer, object);
    }

    private Object getObjectOnStack(long reverseStackIndex) {
        if (reverseStackIndex < 0) {
            primitiveFail();
            return null;
        }
        // the stack pointer is the index of the object that is pushed onto the stack next,
        // so we subtract 1 to get the index of the object that was last pushed onto the stack
        int stackIndex = getStackPointer() - 1 - (int) reverseStackIndex;
        if (stackIndex < 0) {
            primitiveFail();
            return null;
        }
        return FrameAccess.getStackValue(frame, stackIndex, FrameAccess.getNumArguments(frame));
    }

    ////////////////////////
    // CONVERSION HELPERS //
    ////////////////////////

    private long objectToLong(Object object) {
        if (object instanceof Long longObject) {
            return longObject;
        }
        System.err.println("Object to long called with non-Long: " + object);
        primitiveFail();
        return 0;
    }

    private double objectToDouble(Object object) {
        if (object instanceof FloatObject floatObject) {
            return floatObject.getValue();
        }
        System.err.println("Object to double called with non-FloatObject: " + object);
        primitiveFail();
        return 0;
    }

    ///////////////////////
    // ACCESSING HELPERS //
    ///////////////////////

    private Object objectAt0(Object object, long index) {
        return SqueakObjectAt0Node.executeUncached(object, index);
    }

    ////////////////////////
    // TYPE CHECK HELPERS //
    ////////////////////////

    private int instanceOfCheck(long oop, Class<?> klass) {
        Object object = objectRegistryGet(oop);
        return klass.isInstance(object) ? 1 : 0;
    }

    private int nativeObjectCheck(long oop, Predicate<NativeObject> predicate) {
        Object object = objectRegistryGet(oop);
        if (object instanceof NativeObject nativeObject) {
            return predicate.test(nativeObject) ? 1 : 0;
        }
        return 0;
    }

    ///////////////////////////////
    // INTERPRETER PROXY METHODS //
    ///////////////////////////////

    private int byteSizeOf(long oop) {
        if (objectRegistryGet(oop) instanceof NativeObject nativeObject) {
            return NativeObjectStorage.from(nativeObject).byteSizeOf();
        }
        return 0;
    }

    private int classString() {
        return oopFor(context.byteStringClass);
    }

    private int failed() {
        return 0; // TODO: when changing primitiveFail to continue executing, properly implement
                  // this
    }

    private long fetchIntegerofObject(long fieldIndex, long objectPointer) {
        return objectToLong(objectAt0(objectRegistryGet(objectPointer), fieldIndex));
    }

    private long fetchLong32ofObject(long fieldIndex, long oop) {
        return fetchIntegerofObject(fieldIndex, oop);
    }

    private long fetchPointerofObject(long index, long oop) {
        return oopFor(objectAt0(objectRegistryGet(oop), index));
    }

    private NativeObjectStorage firstIndexableField(long oop) {
        if (objectRegistryGet(oop) instanceof NativeObject nativeObject) {
            NativeObjectStorage storage = NativeObjectStorage.from(nativeObject);
            postPrimitiveCleanups.add(storage);
            return storage;
        }
        return null;
    }

    private double floatValueOf(long oop) {
        return objectToDouble(objectRegistryGet(oop));
    }

    private int instantiateClassindexableSize(long classPointer, long size) {
        Object object = objectRegistryGet(classPointer);
        if (object instanceof ClassObject classObject) {
            AbstractSqueakObject newObject = SqueakObjectNewNode.executeUncached(context, classObject, (int) size);
            return oopFor(newObject);
        }
        System.err.println("instantiateClassindexableSize called with non-ClassObject: " + object);
        primitiveFail();
        return 0;
    }

    private long integerObjectOf(long value) {
        return oopFor(value);
    }

    private long integerValueOf(long oop) {
        return objectToLong(objectRegistryGet(oop));
    }

    private NativeObjectStorage ioLoadFunctionFrom(String functionName, String moduleName) {
        /* TODO */ System.out.println("Missing implementation for ioLoadFunctionFrom");
        return null;
    }

    private long isArray(long oop) {
        return instanceOfCheck(oop, ArrayObject.class);
    }

    private int isBytes(long oop) {
        return nativeObjectCheck(oop, NativeObject::isByteType);
    }

    private long isPointers(long oop) {
        return instanceOfCheck(oop, AbstractPointersObject.class);
    }

    private long isPositiveMachineIntegerObject(long oop) {
        Object object = objectRegistryGet(oop);
        if (object instanceof Long integer) {
            return integer >= 0 ? 1 : 0;
        }
        if (object instanceof LargeIntegerObject largeInteger) {
            return largeInteger.isZeroOrPositive() && largeInteger.fitsIntoLong() ? 1 : 0;
        }
        return 0;
    }

    private long isWords(long oop) {
        return nativeObjectCheck(oop, NativeObject::isLongType);
    }

    private long isWordsOrBytes(long oop) {
        /* TODO */ System.out.println("Missing implementation for isWordsOrBytes");
        return 0;
    }

    private int majorVersion() {
        return 1;
    }

    private int methodArgumentCount() {
        return numReceiverAndArguments - 1;
    }

    private long methodReturnInteger(long integer) {
        /* TODO */ System.out.println("Missing implementation for methodReturnInteger");
        return 0;
    }

    private long methodReturnReceiver() {
        /* TODO */ System.out.println("Missing implementation for methodReturnReceiver");
        return 0;
    }

    private long methodReturnValue(long oop) {
        /* TODO */ System.out.println("Missing implementation for methodReturnValue");
        return 0;
    }

    private int minorVersion() {
        return 17;
    }

    private int nilObject() {
        return oopFor(NilObject.SINGLETON);
    }

    private int pop(long nItems) {
        setStackPointer(getStackPointer() - (int) nItems);
        return 1;
    }

    private int popthenPush(long nItems, long oop) {
        pop(nItems);
        push(oop);
        return 1;
    }

    private long positive32BitIntegerFor(long integerValue) {
        /* TODO */ System.out.println("Missing implementation for positive32BitIntegerFor");
        return 0;
    }

    private long positive32BitValueOf(long oop) {
        /* TODO */ System.out.println("Missing implementation for positive32BitValueOf");
        return 0;
    }

    private long positive64BitValueOf(long oop) {
        /* TODO */ System.out.println("Missing implementation for positive64BitValueOf");
        return 0;
    }

    private int primitiveFail() {
        // TODO: continue executing C code
        // TODO: adjust failed accordingly
        throw PrimitiveFailed.GENERIC_ERROR;
    }

    private long primitiveFailFor(long reasonCode) {
        /* TODO */ System.out.println("Missing implementation for primitiveFailFor");
        return 0;
    }

    private int push(long oop) {
        pushObject(objectRegistryGet(oop));
        return 1;
    }

    private int pushInteger(long integerValue) {
        pushObject(integerValue);
        return 1;
    }

    private long showDisplayBitsLeftTopRightBottom(long aForm, long l, long t, long r, long b) {
        /* TODO */ System.out.println("Missing implementation for showDisplayBitsLeftTopRightBottom");
        return 0;
    }

    private long signed32BitIntegerFor(long integerValue) {
        return integerObjectOf(integerValue);
    }

    private long signed32BitValueOf(long oop) {
        return integerValueOf(oop);
    }

    private long slotSizeOf(long oop) {
        /* TODO */ System.out.println("Missing implementation for slotSizeOf");
        return 0;
    }

    private long stackIntegerValue(long offset) {
        return objectToLong(getObjectOnStack(offset));
    }

    private long stackObjectValue(long offset) {
        /* TODO */ System.out.println("Missing implementation for stackObjectValue");
        return 0;
    }

    private long stackValue(long offset) {
        return oopFor(getObjectOnStack(offset));
    }

    private long statNumGCs() {
        return MiscUtils.getCollectionCount();
    }

    private long storeIntegerofObjectwithValue(long index, long oop, long integer) {
        /* TODO */ System.out.println("Missing implementation for storeIntegerofObjectwithValue");
        return 0;
    }

    private long storeLong32ofObjectwithValue(long fieldIndex, long oop, long anInteger) {
        /* TODO */ System.out.println("Missing implementation for storeLong32ofObjectwithValue");
        return 0;
    }
}
