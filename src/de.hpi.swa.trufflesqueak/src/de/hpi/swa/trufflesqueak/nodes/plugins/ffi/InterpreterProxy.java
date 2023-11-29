package de.hpi.swa.trufflesqueak.nodes.plugins.ffi;

import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectNewNode;
import de.hpi.swa.trufflesqueak.nodes.plugins.ffi.wrappers.NativeObjectStorage;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.NFIUtils;
import de.hpi.swa.trufflesqueak.util.NFIUtils.TruffleExecutable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class InterpreterProxy {
    private static InterpreterProxy INSTANCE = null;

    private final SqueakImageContext context;
    private MaterializedFrame frame;
    private int numReceiverAndArguments;
    private final ArrayList<Object> objectRegistry = new ArrayList<>();
    private final ArrayList<PostPrimitiveCleanup> postPrimitiveCleanups = new ArrayList<>();
    private final TruffleExecutable[] executables = new TruffleExecutable[]{
            TruffleExecutable.wrap("(SINT64):SINT64", this::byteSizeOf),
            TruffleExecutable.wrap("():SINT64", this::classString),
            TruffleExecutable.wrap("():SINT64", this::failed),
            TruffleExecutable.wrap("(SINT64):POINTER", this::firstIndexableField),
            TruffleExecutable.wrap("(SINT64,SINT64):SINT64", this::instantiateClassindexableSize),
            TruffleExecutable.wrap("(SINT64):SINT64", this::isBytes),
            TruffleExecutable.wrap("():SINT64", this::majorVersion),
            TruffleExecutable.wrap("():SINT64", this::methodArgumentCount),
            TruffleExecutable.wrap("():SINT64", this::minorVersion),
            TruffleExecutable.wrap("():SINT64", this::nilObject),
            TruffleExecutable.wrap("(SINT64):SINT64", this::pop),
            TruffleExecutable.wrap("(SINT64,SINT64):SINT64", this::popthenPush),
            TruffleExecutable.wrap("():SINT64", this::primitiveFail),
            TruffleExecutable.wrap("(SINT64):SINT64", this::pushInteger),
            TruffleExecutable.wrap("(SINT64):SINT64", this::signed32BitIntegerFor),
            TruffleExecutable.wrap("(SINT64):SINT32", this::signed32BitValueOf),
            TruffleExecutable.wrap("(SINT64):SINT64", this::stackIntegerValue),
            TruffleExecutable.wrap("(SINT64):SINT64", this::stackValue),
    };

    private static Object interpreterProxyPointer = null;

    private InterpreterProxy(SqueakImageContext context, MaterializedFrame frame, int numReceiverAndArguments) throws UnsupportedMessageException, UnknownIdentifierException, UnsupportedTypeException, ArityException {
        this.context = context;
        this.frame = frame;
        this.numReceiverAndArguments = numReceiverAndArguments;
        if (interpreterProxyPointer == null) {
            final String truffleExecutablesSignatures = Arrays.stream(executables).map(obj -> obj.nfiSignature).collect(Collectors.joining(","));
            final Object interpreterProxy = NFIUtils.loadLibrary(
                    context,
                    "InterpreterProxy.so",
                    "{ createInterpreterProxy(" + truffleExecutablesSignatures + "):POINTER; }"
            );

            final InteropLibrary interpreterProxyLibrary = NFIUtils.getInteropLibrary(interpreterProxy);
            interpreterProxyPointer = interpreterProxyLibrary.invokeMember(
                    interpreterProxy, "createInterpreterProxy", (Object[]) executables);
        }
    }

    public static InterpreterProxy instanceFor(SqueakImageContext context, MaterializedFrame frame, int numReceiverAndArguments) throws UnsupportedMessageException, UnknownIdentifierException, UnsupportedTypeException, ArityException {
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

    public Object getPointer() {
        return interpreterProxyPointer;
    }

    public void postPrimitiveCleanups() {
        postPrimitiveCleanups.forEach(PostPrimitiveCleanup::cleanup);
        postPrimitiveCleanups.clear();
    }

    private Object objectRegistryGet(long oop) {
        System.out.println("Asked for oop " + oop);
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
        System.out.println("Giving out oop " + oop + " for " + object);
        return oop;
    }

    private int getStackPointer() {
        return FrameAccess.getStackPointer(frame);
    }

    private void setStackPointer(int stackPointer) {
        FrameAccess.setStackPointer(frame, stackPointer);
    }

    private void pushObject(Object object) {
        System.out.println("Pushing object " + object);
        int stackPointer = getStackPointer() + 1;
        setStackPointer(stackPointer);
        FrameAccess.setStackSlot(frame, stackPointer - 1, object);
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

    private long objectToLong(Object object) {
        if (!(object instanceof Long)) {
            System.out.println("Object to long called with non-Long: " + object);
            primitiveFail();
            return 0;
        }
        return (Long) object;
    }

    private int byteSizeOf(long oop) {
        return NativeObjectStorage.from((NativeObject) objectRegistryGet(oop)).byteSizeOf();
    }

    private int classString() {
        return oopFor(context.byteStringClass);
    }

    private int failed() {
        return 0; // TODO: when changing primitiveFail to continue executing, properly implement this
    }

    private NativeObjectStorage firstIndexableField(long oop) {
        NativeObjectStorage storage = NativeObjectStorage.from((NativeObject) objectRegistryGet(oop));
        postPrimitiveCleanups.add(storage);
        return storage;
    }

    private int instantiateClassindexableSize(long classPointer, long size) {
        Object classObject = objectRegistryGet(classPointer);
        if (!(classObject instanceof ClassObject)) {
            System.out.println("instantiateClassindexableSize called with non-ClassObject: " + classObject);
            primitiveFail();
            return -1;
        }
        SqueakObjectNewNode objectNewNode = SqueakObjectNewNode.create();
        AbstractSqueakObject newObject = objectNewNode.execute(context, (ClassObject) classObject, (int) size);
        return oopFor(newObject);
    }

    private int isBytes(long oop) {
        Object object = objectRegistryGet(oop);
        if (!(object instanceof NativeObject)) {
            return 0;
        }
        return ((NativeObject) object).isByteType() ? 1 : 0;
    }

    private int majorVersion() {
        return 1;
    }

    private int methodArgumentCount() {
        return numReceiverAndArguments - 1;
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

    private int primitiveFail() {
        // TODO: continue executing C code
        // TODO: adjust failed accordingly
        throw PrimitiveFailed.GENERIC_ERROR;
    }

    private int pushInteger(long integerValue) {
        pushObject(integerValue);
        return 1;
    }

    private int push(long oop) {
        pushObject(objectRegistryGet(oop));
        return 1;
    }

    private int signed32BitIntegerFor(long integerValue) {
        return oopFor(integerValue);
    }

    private int signed32BitValueOf(long oop) {
        return (int) objectToLong(objectRegistryGet(oop));
    }

    private long stackIntegerValue(long reverseStackIndex) {
        return objectToLong(getObjectOnStack(reverseStackIndex));
    }

    private int stackValue(long reverseStackIndex) {
        return oopFor(getObjectOnStack(reverseStackIndex));
    }

    public interface PostPrimitiveCleanup {
        void cleanup();
    }
}
