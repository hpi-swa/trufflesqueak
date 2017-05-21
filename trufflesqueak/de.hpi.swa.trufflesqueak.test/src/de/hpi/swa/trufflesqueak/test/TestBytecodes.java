package de.hpi.swa.trufflesqueak.test;

import org.junit.Test;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;

public class TestBytecodes extends TestSqueak {
    @Test
    public void testPushReceiverVariable() {
        BaseSqueakObject rcvr = new PointersObject(
                        image,
                        image.arrayClass,
                        new Object[]{
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

    @Test
    public void testPushReceiver() {
        BaseSqueakObject rcvr = image.specialObjectsArray;
        assertSame(rcvr, runMethod(rcvr, 112, 124));
    }

    @Test
    public void testPushTrue() {
        BaseSqueakObject rcvr = image.specialObjectsArray;
        assertTrue((boolean) runMethod(rcvr, 113, 124));
    }

    @Test
    public void testPushFalse() {
        BaseSqueakObject rcvr = image.specialObjectsArray;
        assertFalse((boolean) runMethod(rcvr, 114, 124));
    }

    @Test
    public void testPushNil() {
        BaseSqueakObject rcvr = image.specialObjectsArray;
        assertSame(null, runMethod(rcvr, 115, 124));
    }

    @Test
    public void testReturnReceiver() {
        BaseSqueakObject rcvr = image.specialObjectsArray;
        assertSame(rcvr, runMethod(rcvr, 115, 120));
    }
}
