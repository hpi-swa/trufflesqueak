package de.hpi.swa.trufflesqueak.test;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.exceptions.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.roots.SqueakMethodNode;
import de.hpi.swa.trufflesqueak.util.SqueakImageChunk;
import junit.framework.TestCase;

public abstract class AbstractSqueakTestCase extends TestCase {

    protected SqueakImageContext image;

    public AbstractSqueakTestCase() {
        super();
    }

    public AbstractSqueakTestCase(String name) {
        super(name);
    }

    private class DummyChunk extends SqueakImageChunk {

        public DummyChunk(int format) {
            super(null, null, 0, format, 0, 0, 0);
        }

        @Override
        public Object[] getPointers() {
            Object[] pointers = new Object[6];
            pointers[2] = format; // FORMAT_INDEX
            return pointers;
        }
    }

    @Override
    public void setUp() {
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
        image.size_.setBytes("size_".getBytes());
        image.next.setBytes("next".getBytes());
        image.nextPut.setBytes("nextPut".getBytes());
        image.atEnd.setBytes("atEnd".getBytes());
        image.equivalent.setBytes("equivalent".getBytes());
        image.klass.setBytes("klass".getBytes());
        image.blockCopy.setBytes("blockCopy".getBytes());
        image.value.setBytes("value".getBytes());
        image.valueWithArg.setBytes("valueWithArg".getBytes());
        image.do_.setBytes("do_".getBytes());
        image.new_.setBytes("new_".getBytes());
        image.newWithArg.setBytes("newWithArg".getBytes());
        image.x.setBytes("x".getBytes());
        image.y.setBytes("y".getBytes());
        image.compiledMethodClass.fillin(new DummyChunk(100)); // sets instanceSize to 100
    }

    public CompiledCodeObject makeMethod(byte[] bytes) {
        // Always add three literals...
        return makeMethod(bytes, new Object[]{68419598, null, null});
    }

    public CompiledCodeObject makeMethod(byte[] bytes, Object[] literals) {
        // Always add three literals...
        return new CompiledMethodObject(image, bytes, literals);
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
        VirtualFrame frame = createTestFrame(code, receiver, arguments);
        Object result = null;
        try {
            result = new SqueakMethodNode(null, code).execute(frame);
        } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
            assertTrue("broken test", false);
        }
        return result;
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

    public VirtualFrame createTestFrame(CompiledCodeObject code, Object receiver) {
        return createTestFrame(code, receiver, new Object[]{});
    }

    public VirtualFrame createTestFrame(CompiledCodeObject code, Object receiver, Object[] arguments) {
        Object[] args = new Object[arguments.length + 1];
        int i = 0;
        args[i++] = receiver;
        for (Object o : arguments) {
            args[i++] = o;
        }
        return Truffle.getRuntime().createVirtualFrame(args, code.getFrameDescriptor());
    }
}