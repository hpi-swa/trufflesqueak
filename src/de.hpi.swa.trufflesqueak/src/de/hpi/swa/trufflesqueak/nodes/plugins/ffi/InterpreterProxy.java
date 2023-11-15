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
import de.hpi.swa.trufflesqueak.nodes.plugins.ffi.wrappers.ByteStorage;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.NFIUtils;

import java.util.ArrayList;

public class InterpreterProxy {

    private final VirtualFrame frame;
    private final ArrayList<Object> objectRegistry = new ArrayList<>();
    private final ArrayList<de.hpi.swa.trufflesqueak.nodes.plugins.ffi.InterpreterProxy.PostPrimitiveCleanup> postPrimitiveCleanups = new ArrayList<>();

    private static Object interpreterProxyPointer = null;

    public InterpreterProxy(SqueakImageContext context, VirtualFrame frame) throws UnsupportedMessageException, UnknownIdentifierException, UnsupportedTypeException, ArityException {
        this.frame = frame;
        if (interpreterProxyPointer == null) {
            final Object interpreterProxy = NFIUtils.loadLibrary(
                    context,
                    "InterpreterProxy.so",
                    "{ createInterpreterProxy((SINT64):SINT64,(SINT64):POINTER,(SINT64):SINT64,():SINT64,():SINT64,():SINT64,():SINT64,(SINT64):SINT64):POINTER; }"
            );

            final InteropLibrary interpreterProxyLibrary = NFIUtils.getInteropLibrary(interpreterProxy);
            interpreterProxyPointer = interpreterProxyLibrary.invokeMember(
                    interpreterProxy,"createInterpreterProxy", (Object[]) getExecutables());
        }
    }
    public Object getPointer() {
        return interpreterProxyPointer;
    }

    public NFIUtils.TruffleExecutable[] getExecutables() {
        return new NFIUtils.TruffleExecutable[] {
                NFIUtils.TruffleExecutable.wrap(this::byteSizeOf),
                NFIUtils.TruffleExecutable.wrap(this::firstIndexableField),
                NFIUtils.TruffleExecutable.wrap(this::isBytes),
                NFIUtils.TruffleExecutable.wrap(this::majorVersion),
                NFIUtils.TruffleExecutable.wrap(this::methodArgumentCount),
                NFIUtils.TruffleExecutable.wrap(this::minorVersion),
                NFIUtils.TruffleExecutable.wrap(this::primitiveFail),
                NFIUtils.TruffleExecutable.wrap(this::stackValue),
        };
    }
    public void postPrimitiveCleanups() {
        postPrimitiveCleanups.forEach(de.hpi.swa.trufflesqueak.nodes.plugins.ffi.InterpreterProxy.PostPrimitiveCleanup::cleanup);
    }

    private int byteSizeOf(long oop) {
        return 16;
    }
    private ByteStorage firstIndexableField(long oop) {
        byte[] storage = ((NativeObject) objectRegistry.get((int) oop)).getByteStorage();
        ByteStorage byteStorage = new ByteStorage(storage);
        postPrimitiveCleanups.add(byteStorage);
        return byteStorage;
    }
    private int isBytes(long oop) {
        return 1;
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
    private int primitiveFail() {
        throw PrimitiveFailed.GENERIC_ERROR;
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
