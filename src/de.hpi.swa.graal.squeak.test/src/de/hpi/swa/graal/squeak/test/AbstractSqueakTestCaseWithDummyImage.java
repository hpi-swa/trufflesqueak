package de.hpi.swa.graal.squeak.test;

import org.junit.BeforeClass;

import de.hpi.swa.graal.squeak.image.SqueakImageChunk;

public abstract class AbstractSqueakTestCaseWithDummyImage extends AbstractSqueakTestCase {

    @BeforeClass
    public static void setUpSqueakImageContext() {
        ensureImageContext("fake.image");
        image.plus.setStorage("plus".getBytes());
        image.minus.setStorage("minus".getBytes());
        image.lt.setStorage("lt".getBytes());
        image.gt.setStorage("gt".getBytes());
        image.le.setStorage("le".getBytes());
        image.ge.setStorage("ge".getBytes());
        image.eq.setStorage("eq".getBytes());
        image.ne.setStorage("ne".getBytes());
        image.times.setStorage("times".getBytes());
        image.divide.setStorage("divide".getBytes());
        image.modulo.setStorage("modulo".getBytes());
        image.pointAt.setStorage("pointAt".getBytes());
        image.bitShift.setStorage("bitShift".getBytes());
        image.floorDivide.setStorage("floorDivide".getBytes());
        image.bitAnd.setStorage("bitAnd".getBytes());
        image.bitOr.setStorage("bitOr".getBytes());
        image.at.setStorage("at".getBytes());
        image.atput.setStorage("atput".getBytes());
        image.sqSize.setStorage("size".getBytes());
        image.next.setStorage("next".getBytes());
        image.nextPut.setStorage("nextPut".getBytes());
        image.atEnd.setStorage("atEnd".getBytes());
        image.equivalent.setStorage("equivalent".getBytes());
        image.klass.setStorage("klass".getBytes());
        image.blockCopy.setStorage("blockCopy".getBytes());
        image.sqValue.setStorage("value".getBytes());
        image.valueWithArg.setStorage("valueWithArg".getBytes());
        image.sqDo.setStorage("do".getBytes());
        image.sqNew.setStorage("new".getBytes());
        image.newWithArg.setStorage("newWithArg".getBytes());
        image.x.setStorage("x".getBytes());
        image.y.setStorage("y".getBytes());
        image.specialObjectsArray.setStorage(new Object[100]);
        final Object[] pointers = new Object[]{
                        null, null, 100L, null, null, null}; // sets instanceSize to 100
        final SqueakImageChunk fakeChunk = SqueakImageChunk.createDummyChunk(image, pointers);
        image.compiledMethodClass.fillin(fakeChunk);
        image.nilClass.setFormat(0);
        image.arrayClass.setFormat(0);
    }
}
