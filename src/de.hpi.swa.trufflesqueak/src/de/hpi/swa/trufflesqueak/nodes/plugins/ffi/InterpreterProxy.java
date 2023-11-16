package de.hpi.swa.trufflesqueak.nodes.plugins.ffi;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.plugins.ffi.wrappers.NativeObjectStorage;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.NFIUtils;
import de.hpi.swa.trufflesqueak.util.NFIUtils.TruffleExecutable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class InterpreterProxy {

    private final VirtualFrame frame;
    private final ArrayList<Object> objectRegistry = new ArrayList<>();
    private final ArrayList<PostPrimitiveCleanup> postPrimitiveCleanups = new ArrayList<>();

    private static Object interpreterProxyPointer = null;

    public InterpreterProxy(SqueakImageContext context, VirtualFrame frame) throws UnsupportedMessageException, UnknownIdentifierException, UnsupportedTypeException, ArityException {
        this.frame = frame;
        if (interpreterProxyPointer == null) {
            final TruffleExecutable[] truffleExecutables = getExecutables();
            final String truffleExecutablesSignatures = Arrays.stream(truffleExecutables).map(obj -> obj.nfiSignature).collect(Collectors.joining(","));
            final Object interpreterProxy = NFIUtils.loadLibrary(
                    context,
                    "InterpreterProxy.so",
                    "{ createInterpreterProxy(" + truffleExecutablesSignatures + "):POINTER; }"
            );

            final InteropLibrary interpreterProxyLibrary = NFIUtils.getInteropLibrary(interpreterProxy);
            interpreterProxyPointer = interpreterProxyLibrary.invokeMember(
                    interpreterProxy,"createInterpreterProxy", (Object[]) truffleExecutables);
        }
    }
    public Object getPointer() {
        return interpreterProxyPointer;
    }

    public TruffleExecutable[] getExecutables() {
        return new TruffleExecutable[] {
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
    }
    public void postPrimitiveCleanups() {
        postPrimitiveCleanups.forEach(PostPrimitiveCleanup::cleanup);
    }
    private NativeObject objectRegistryGet(long oop) {
        return (NativeObject) objectRegistry.get((int) oop);
    }

    private int byteSizeOf(long oop) {
        return NativeObjectStorage.from(objectRegistryGet(oop)).byteSizeOf();
    }
    private int classString() {
        return 1;// TODO
    }
    private int failed() {
        return 1;// TODO
    }
    private NativeObjectStorage firstIndexableField(long oop) {
        NativeObjectStorage storage = NativeObjectStorage.from(objectRegistryGet(oop));
        postPrimitiveCleanups.add(storage);
        return storage;
    }
    private int instantiateClassindexableSize(long classPointer, long size) {
        return 1;// TODO
    }
    private int isBytes(long oop) {
        return objectRegistryGet(oop).isByteType() ? 1 : 0;
    }
    private int majorVersion() {
        return 1;
    }
    private int methodArgumentCount() {
        return FrameAccess.getNumArguments(frame);
    }
    private int minorVersion() {
        return 17;
    }
    private int nilObject() {
        return 1;// TODO
    }
    private int pop(long nItems) {
        return 1;// TODO
    }
    private int popthenPush(long nItems, long oop) {
        return 1;// TODO
    }
    private int primitiveFail() {
        throw PrimitiveFailed.GENERIC_ERROR;
    }
    private int pushInteger(long integerValue) {
        return 1;// TODO
    }
    private int signed32BitIntegerFor(long integerValue) {
        return 1;// TODO
    }
    private int signed32BitValueOf(long oop) {
        return 1;// TODO
    }
    private int stackIntegerValue(long stackIndex) {
        return 1;// TODO
    }
    private int stackValue(long stackIndex) {
        Object objectOnStack = FrameAccess.getStackValue(frame, (int) stackIndex, FrameAccess.getNumArguments(frame));
        int objectIndex = objectRegistry.size();
        objectRegistry.add(objectOnStack);
        return objectIndex;
    }

    public interface PostPrimitiveCleanup {
        void cleanup();
    }
}
