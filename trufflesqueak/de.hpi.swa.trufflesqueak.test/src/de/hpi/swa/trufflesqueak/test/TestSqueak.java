package de.hpi.swa.trufflesqueak.test;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.exceptions.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.SmallInteger;
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
        image = new SqueakImageContext(null, null, null, null);
    }

    public CompiledCodeObject makeMethod(byte[] bytes) {
        CompiledCodeObject cm = new CompiledMethodObject(image, bytes);
        return cm;
    }

    public CompiledCodeObject makeMethod(int... intbytes) {
        byte[] bytes = new byte[intbytes.length];
        for (int i = 0; i < intbytes.length; i++) {
            bytes[i] = (byte) intbytes[i];
        }
        return makeMethod(bytes);
    }

    public Object runMethod(CompiledCodeObject cm, BaseSqueakObject receiver, BaseSqueakObject... arguments) {
        VirtualFrame frame = cm.createTestFrame(receiver, arguments);
        Object result = null;
        try {
            result = new SqueakMethodNode(null, cm).execute(frame);
        } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
            assertTrue("broken test", false);
        }
        return result;
    }

    public Object runMethod(BaseSqueakObject receiver, int... intbytes) {
        return runMethod(receiver, new BaseSqueakObject[0], intbytes);
    }

    public Object runMethod(BaseSqueakObject receiver, BaseSqueakObject[] arguments, int... intbytes) {
        CompiledCodeObject cm = makeMethod(intbytes);
        return runMethod(cm, receiver, arguments);
    }

    protected Object runPrim(int primCode, BaseSqueakObject rcvr, BaseSqueakObject... arguments) {
        CompiledCodeObject cm = makeMethod(new int[]{139, primCode & 0xFF, (primCode & 0xFF00) >> 8});
        cm.setLiteral(0, new SmallInteger(null, 0x10000));
        return runMethod(cm, rcvr, arguments);
    }

}