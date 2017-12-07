package de.hpi.swa.trufflesqueak.test;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.exceptions.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.roots.SqueakMethodNode;
import junit.framework.TestCase;

public abstract class TestSqueak extends TestCase {

    protected SqueakImageContext image;

    public TestSqueak() {
        super();
    }

    public TestSqueak(String name) {
        super(name);
    }

    @Override
    public void setUp() {
        image = new SqueakImageContext(null, null, null, null, null);
        image.at.setBytes("at:".getBytes());
        image.equivalent.setBytes("==".getBytes());
        image.klass.setBytes("class".getBytes());
        image.div.setBytes("/".getBytes());
        image.divide.setBytes("//".getBytes());
        image.plus.setBytes("+".getBytes());
        image.eq.setBytes("=".getBytes());
        image.modulo.setBytes("\\\\".getBytes());
        image.value.setBytes("value".getBytes());
        image.size_.setBytes("size".getBytes());
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
        VirtualFrame frame = code.createTestFrame(receiver, arguments);
        Object result = null;
        try {
            result = new SqueakMethodNode(null, code).execute(frame);
        } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
            assertTrue("broken test", false);
        }
        return result;
    }

    public Object runMethod(BaseSqueakObject receiver, int... intbytes) {
        return runMethod(receiver, new BaseSqueakObject[4], intbytes);
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
}