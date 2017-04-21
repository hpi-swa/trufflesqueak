package de.hpi.swa.trufflesqueak.test;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.exceptions.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
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

    public CompiledMethodObject makeMethod(int... intbytes) {
        byte[] bytes = new byte[intbytes.length];
        for (int i = 0; i < intbytes.length; i++) {
            bytes[i] = (byte) intbytes[i];
        }
        return makeMethod(bytes);
    }

    public Object run(BaseSqueakObject receiver, CompiledMethodObject cm) {
        VirtualFrame frame = cm.createFrame(receiver);
        Object result = null;
        try {
            result = cm.getBytecodeAST().executeGeneric(frame);
        } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
            assertTrue("broken test", false);
        }
        return result;
    }

    public Object run(BaseSqueakObject receiver, int... intbytes) {
        CompiledMethodObject cm = makeMethod(intbytes);
        return run(receiver, cm);
    }

    public void testPushReceiverVariable() {
        BaseSqueakObject rcvr = new PointersObject(new BaseSqueakObject[]{
                        image.nil,
                        image.sqFalse,
                        image.sqTrue,
                        image.characterClass,
                        image.metaclass,
                        image.schedulerAssociation,
                        image.smallIntegerClass,
                        image.smalltalk,
                        image.specialObjectsArray,
        });
        assertSame(image.nil, run(rcvr, 0, 124));
        assertSame(image.sqFalse, run(rcvr, 1, 124));
        assertSame(image.sqTrue, run(rcvr, 2, 124));
        assertSame(image.characterClass, run(rcvr, 3, 124));
        assertSame(image.metaclass, run(rcvr, 4, 124));
        assertSame(image.schedulerAssociation, run(rcvr, 5, 124));
        assertSame(image.smallIntegerClass, run(rcvr, 6, 124));
        assertSame(image.smalltalk, run(rcvr, 7, 124));
        assertSame(image.specialObjectsArray, run(rcvr, 8, 124));
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

    public void testPrimEquivalentBC() {
        BaseSqueakObject rcvr = image.specialObjectsArray;
        assertTrue((boolean) run(rcvr, 112, 112, 198, 124));
        assertFalse((boolean) run(rcvr, 112, 113, 198, 124));
    }
}
