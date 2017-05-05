package de.hpi.swa.trufflesqueak.test;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.exceptions.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.SmallInteger;
import de.hpi.swa.trufflesqueak.nodes.roots.SqueakMethodNode;
import junit.framework.TestCase;

public class TestBytecodes extends TestCase {
    private SqueakImageContext image;

    @Override
    public void setUp() {
        image = new SqueakImageContext(null, null, null, null);
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

    public Object runMethod(CompiledMethodObject cm, BaseSqueakObject receiver, BaseSqueakObject... arguments) {
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
        CompiledMethodObject cm = makeMethod(intbytes);
        return runMethod(cm, receiver, arguments);
    }

    public void testPushReceiverVariable() {
        BaseSqueakObject rcvr = new PointersObject(
                        image,
                        image.arrayClass,
                        new BaseSqueakObject[]{
                                        image.nil,
                                        image.sqFalse,
                                        image.sqTrue,
                                        image.characterClass,
                                        image.metaclass,
                                        image.schedulerAssociation,
                                        image.smallIntegerClass,
                                        image.smalltalk,
                                        image.specialObjectsArray});
        assertSame(image.nil, runMethod(rcvr, 0, 124));
        assertSame(image.sqFalse, runMethod(rcvr, 1, 124));
        assertSame(image.sqTrue, runMethod(rcvr, 2, 124));
        assertSame(image.characterClass, runMethod(rcvr, 3, 124));
        assertSame(image.metaclass, runMethod(rcvr, 4, 124));
        assertSame(image.schedulerAssociation, runMethod(rcvr, 5, 124));
        assertSame(image.smallIntegerClass, runMethod(rcvr, 6, 124));
        assertSame(image.smalltalk, runMethod(rcvr, 7, 124));
        assertSame(image.specialObjectsArray, runMethod(rcvr, 8, 124));
    }

    public void testPushReceiver() {
        BaseSqueakObject rcvr = image.specialObjectsArray;
        assertSame(rcvr, runMethod(rcvr, 112, 124));
    }

    public void testPushTrue() {
        BaseSqueakObject rcvr = image.specialObjectsArray;
        assertTrue((boolean) runMethod(rcvr, 113, 124));
    }

    public void testPushFalse() {
        BaseSqueakObject rcvr = image.specialObjectsArray;
        assertFalse((boolean) runMethod(rcvr, 114, 124));
    }

    public void testPushNil() {
        BaseSqueakObject rcvr = image.specialObjectsArray;
        assertSame(null, runMethod(rcvr, 115, 124));
    }

    public void testReturnReceiver() {
        BaseSqueakObject rcvr = image.specialObjectsArray;
        assertSame(rcvr, runMethod(rcvr, 115, 120));
    }

    public void testPrimEquivalent() {
        BaseSqueakObject rcvr = image.specialObjectsArray;
        assertTrue((boolean) runPrim(110, rcvr, rcvr));
        assertFalse((boolean) runPrim(110, rcvr, image.nil));
    }

    private Object runPrim(int primCode, BaseSqueakObject rcvr, BaseSqueakObject... arguments) {
        CompiledMethodObject cm = makeMethod(new int[]{139, primCode & 0xFF, (primCode & 0xFF00) >> 8});
        cm.setLiteral(0, new SmallInteger(null, 0x10000));
        return runMethod(cm, rcvr, arguments);
    }
}
