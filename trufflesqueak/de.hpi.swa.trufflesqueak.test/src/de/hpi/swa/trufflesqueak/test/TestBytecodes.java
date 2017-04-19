package de.hpi.swa.trufflesqueak.test;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.exceptions.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import junit.framework.TestCase;

public class TestBytecodes extends TestCase {
    private SqueakImageContext image;

    @Override
    public void setUp() {
        image = new SqueakImageContext(null, null, null);
    }

    public CompiledMethodObject makeMethod(byte[] bytes) {
        CompiledMethodObject cm = new CompiledMethodObject(image, bytes);
        return cm;
    }

    public BaseSqueakObject run(BaseSqueakObject receiver, int... intbytes) {
        byte[] bytes = new byte[intbytes.length];
        for (int i = 0; i < intbytes.length; i++) {
            bytes[i] = (byte) intbytes[i];
        }
        CompiledMethodObject cm = makeMethod(bytes);
        VirtualFrame frame = cm.createFrame(receiver);
        BaseSqueakObject result = null;
        try {
            result = cm.getBytecodeAST().executeGeneric(frame);
        } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
            assertTrue("broken test", false);
        }
        return result;
    }

    public void testPushReceiver() {
        BaseSqueakObject rcvr = image.specialObjectsArray;
        assertSame(rcvr, run(rcvr, 112, 124));
    }

    public void testPushTrue() {
        BaseSqueakObject rcvr = image.specialObjectsArray;
        assertSame(image.sqTrue, run(rcvr, 113, 124));
    }

    public void testPushFalse() {
        BaseSqueakObject rcvr = image.specialObjectsArray;
        assertSame(image.sqFalse, run(rcvr, 114, 124));
    }

    public void testPushNil() {
        BaseSqueakObject rcvr = image.specialObjectsArray;
        assertSame(image.nil, run(rcvr, 115, 124));
    }

    public void testReturnReceiver() {
        BaseSqueakObject rcvr = image.specialObjectsArray;
        assertSame(rcvr, run(rcvr, 115, 120));
    }
}
