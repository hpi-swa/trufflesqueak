package de.hpi.swa.graal.squeak.test;

import org.junit.BeforeClass;

import de.hpi.swa.graal.squeak.image.SqueakImageChunk;

public abstract class AbstractSqueakTestCaseWithDummyImage extends AbstractSqueakTestCase {

    @BeforeClass
    public static void setUpSqueakImageContext() {
        ensureImageContext("fake.image");
        image.plus.setStorageForTesting("plus".getBytes());
        image.minus.setStorageForTesting("minus".getBytes());
        image.lt.setStorageForTesting("lt".getBytes());
        image.gt.setStorageForTesting("gt".getBytes());
        image.le.setStorageForTesting("le".getBytes());
        image.ge.setStorageForTesting("ge".getBytes());
        image.eq.setStorageForTesting("eq".getBytes());
        image.ne.setStorageForTesting("ne".getBytes());
        image.times.setStorageForTesting("times".getBytes());
        image.divide.setStorageForTesting("divide".getBytes());
        image.modulo.setStorageForTesting("modulo".getBytes());
        image.pointAt.setStorageForTesting("pointAt".getBytes());
        image.bitShift.setStorageForTesting("bitShift".getBytes());
        image.floorDivide.setStorageForTesting("floorDivide".getBytes());
        image.bitAnd.setStorageForTesting("bitAnd".getBytes());
        image.bitOr.setStorageForTesting("bitOr".getBytes());
        image.at.setStorageForTesting("at".getBytes());
        image.atput.setStorageForTesting("atput".getBytes());
        image.sqSize.setStorageForTesting("size".getBytes());
        image.next.setStorageForTesting("next".getBytes());
        image.nextPut.setStorageForTesting("nextPut".getBytes());
        image.atEnd.setStorageForTesting("atEnd".getBytes());
        image.equivalent.setStorageForTesting("equivalent".getBytes());
        image.klass.setStorageForTesting("klass".getBytes());
        image.blockCopy.setStorageForTesting("blockCopy".getBytes());
        image.sqValue.setStorageForTesting("value".getBytes());
        image.valueWithArg.setStorageForTesting("valueWithArg".getBytes());
        image.sqDo.setStorageForTesting("do".getBytes());
        image.sqNew.setStorageForTesting("new".getBytes());
        image.newWithArg.setStorageForTesting("newWithArg".getBytes());
        image.x.setStorageForTesting("x".getBytes());
        image.y.setStorageForTesting("y".getBytes());
        image.specialObjectsArray.fillin(SqueakImageChunk.createDummyChunk(new Object[100]));
        final Object[] pointers = new Object[6];
        pointers[2] = 100; // sets instanceSize to 100
        image.compiledMethodClass.fillin(SqueakImageChunk.createDummyChunk(pointers));
    }
}
