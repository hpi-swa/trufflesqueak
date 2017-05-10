package de.hpi.swa.trufflesqueak.test;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;

public class TestPrimitives extends TestSqueak {
    public void testPrimEquivalent() {
        BaseSqueakObject rcvr = image.specialObjectsArray;
        assertTrue((boolean) runPrim(110, rcvr, rcvr));
        assertFalse((boolean) runPrim(110, rcvr, image.nil));
    }

    public void testPrimReplaceFromTo() {
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
        for (int i = 0; i < 8; i++) {
            assertNotSame(rcvr.at0(i), null);
        }
        Object result = runPrim(105, rcvr, image.wrapInt(1), image.wrapInt(6), new PointersObject(image, image.nilClass, 10), image.wrapInt(1));
        assertSame(result, rcvr);
        for (int i = 0; i < 6; i++) {
            assertSame(rcvr.at0(i), null);
        }
        for (int i = 7; i < 8; i++) {
            assertNotSame(rcvr.at0(i), null);
        }
    }
}
