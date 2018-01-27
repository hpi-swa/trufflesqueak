package de.hpi.swa.trufflesqueak.test;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.nodes.TopLevelContextNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.SqueakImageChunk;
import junit.framework.TestCase;

@RunWith(Parameterized.class)
public abstract class AbstractSqueakTestCase extends TestCase {
    protected static SqueakImageContext image;

    @Parameters(name = "{index}: noContextNeeded={0}")
    public static Boolean[] data() {
        return new Boolean[]{true, false};
    }

    @Parameter public boolean invalidateNoContextNeededAssumption;

    public AbstractSqueakTestCase() {
        super();
    }

    public AbstractSqueakTestCase(String name) {
        super(name);
    }

    private static class DummyFormatChunk extends SqueakImageChunk {

        public DummyFormatChunk(int format) {
            super(null, null, 0, format, 0, 0, 0);
        }

        @Override
        public Object[] getPointers() {
            Object[] pointers = new Object[6];
            pointers[2] = format; // FORMAT_INDEX
            return pointers;
        }
    }

    private static class DummyPointersChunk extends SqueakImageChunk {
        private Object[] dummyPointers;

        public DummyPointersChunk(Object[] pointers) {
            super(null, null, 0, 0, 0, 0, 0);
            this.dummyPointers = pointers;
        }

        @Override
        public Object[] getPointers() {
            return dummyPointers;
        }
    }

    @BeforeClass
    public static void setUpSqueakImageContext() {
        image = new SqueakImageContext(null, null, null, null);
        image.plus.setBytes("plus".getBytes());
        image.minus.setBytes("minus".getBytes());
        image.lt.setBytes("lt".getBytes());
        image.gt.setBytes("gt".getBytes());
        image.le.setBytes("le".getBytes());
        image.ge.setBytes("ge".getBytes());
        image.eq.setBytes("eq".getBytes());
        image.ne.setBytes("ne".getBytes());
        image.times.setBytes("times".getBytes());
        image.divide.setBytes("divide".getBytes());
        image.modulo.setBytes("modulo".getBytes());
        image.pointAt.setBytes("pointAt".getBytes());
        image.bitShift.setBytes("bitShift".getBytes());
        image.floorDivide.setBytes("floorDivide".getBytes());
        image.bitAnd.setBytes("bitAnd".getBytes());
        image.bitOr.setBytes("bitOr".getBytes());
        image.at.setBytes("at".getBytes());
        image.atput.setBytes("atput".getBytes());
        image.size_.setBytes("size".getBytes());
        image.next.setBytes("next".getBytes());
        image.nextPut.setBytes("nextPut".getBytes());
        image.atEnd.setBytes("atEnd".getBytes());
        image.equivalent.setBytes("equivalent".getBytes());
        image.klass.setBytes("klass".getBytes());
        image.blockCopy.setBytes("blockCopy".getBytes());
        image.value_.setBytes("value".getBytes());
        image.valueWithArg.setBytes("valueWithArg".getBytes());
        image.do_.setBytes("do".getBytes());
        image.new_.setBytes("new".getBytes());
        image.newWithArg.setBytes("newWithArg".getBytes());
        image.x.setBytes("x".getBytes());
        image.y.setBytes("y".getBytes());
        image.specialObjectsArray.fillin(new DummyPointersChunk(new Object[100]));
        image.compiledMethodClass.fillin(new DummyFormatChunk(100)); // sets instanceSize to 100
    }

    public CompiledCodeObject makeMethod(byte[] bytes) {
        // Always add three literals...
        return makeMethod(bytes, new Object[]{68419598, null, null});
    }

    public CompiledCodeObject makeMethod(byte[] bytes, Object[] literals) {
        CompiledMethodObject code = new CompiledMethodObject(image, bytes, literals);
        if (invalidateNoContextNeededAssumption) {
            code.invalidateNoContextNeededAssumption();
        }
        return code;
    }

    public CompiledCodeObject makeMethod(Object[] literals, int... intbytes) {
        byte[] bytes = new byte[intbytes.length];
        for (int i = 0; i < intbytes.length; i++) {
            bytes[i] = (byte) intbytes[i];
        }
        return makeMethod(bytes, literals);
    }

    public CompiledCodeObject makeMethod(int... intbytes) {
        return makeMethod(new Object[]{68419598}, intbytes);
    }

    public Object runMethod(CompiledCodeObject code, Object receiver, Object... arguments) {
        VirtualFrame frame = createTestFrame(code);
        Object result = null;
        try {
            result = createContext(code, receiver, arguments).execute(frame);
        } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
            assertTrue("broken test", false);
        }
        return result;
    }

    protected TopLevelContextNode createContext(CompiledCodeObject code, Object receiver) {
        return createContext(code, receiver, new Object[0]);
    }

    protected TopLevelContextNode createContext(CompiledCodeObject code, Object receiver, Object[] arguments) {
        // always use large instance size and large frame size for testing
        ContextObject testContext = ContextObject.create(code.image, 50 + CONTEXT.LARGE_FRAMESIZE);
        testContext.atput0(CONTEXT.METHOD, code);
        testContext.atput0(CONTEXT.INSTRUCTION_POINTER, testContext.getCodeObject().getInitialPC());
        testContext.atput0(CONTEXT.RECEIVER, receiver);
        testContext.atput0(CONTEXT.STACKPOINTER, 0);
        testContext.atput0(CONTEXT.CLOSURE_OR_NIL, code.image.nil);
        testContext.setSender(code.image.nil);
        for (int i = 0; i < arguments.length; i++) {
            testContext.atput0(CONTEXT.TEMP_FRAME_START + i, arguments[i]);
        }
        return TopLevelContextNode.create(null, testContext);
    }

    public Object runMethod(BaseSqueakObject receiver, int... intbytes) {
        return runMethod(receiver, new BaseSqueakObject[0], intbytes);
    }

    public Object runMethod(BaseSqueakObject receiver, Object[] arguments, int... intbytes) {
        CompiledCodeObject cm = makeMethod(intbytes);
        return runMethod(cm, receiver, arguments);
    }

    protected Object runBinaryPrimitive(int primCode, Object rcvr, Object... arguments) {
        return runPrim(new Object[]{17104899}, primCode, rcvr, arguments);
    }

    protected Object runQuinaryPrimitive(int primCode, Object rcvr, Object... arguments) {
        return runPrim(new Object[]{68222979}, primCode, rcvr, arguments);
    }

    protected Object runPrim(Object[] literals, int primCode, Object rcvr, Object... arguments) {
        CompiledCodeObject cm = makeMethod(literals, new int[]{139, primCode & 0xFF, (primCode & 0xFF00) >> 8});
        return runMethod(cm, rcvr, arguments);
    }

    public VirtualFrame createTestFrame(CompiledCodeObject code) {
        Object[] arguments = FrameAccess.newWith(code, null, null, new Object[0]);
        return Truffle.getRuntime().createVirtualFrame(arguments, code.getFrameDescriptor());
    }
}