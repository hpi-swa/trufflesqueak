package de.hpi.swa.graal.squeak.test;

import org.junit.BeforeClass;

import de.hpi.swa.graal.squeak.image.AbstractImageChunk;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;

public abstract class AbstractSqueakTestCaseWithDummyImage extends AbstractSqueakTestCase {

    private static final class DummyFormatChunk extends AbstractImageChunk {

        private DummyFormatChunk(final int format) {
            super(null, null, 0, format, 0, 0, 0);
        }

        @Override
        public Object[] getPointers() {
            final Object[] pointers = new Object[6];
            pointers[2] = (long) format; // FORMAT_INDEX
            return pointers;
        }
    }

    private static final class DummyPointersChunk extends AbstractImageChunk {
        private Object[] dummyPointers;

        private DummyPointersChunk(final Object[] pointers) {
            super(null, null, 0, 0, 0, 0, 0);
            this.dummyPointers = pointers;
        }

        @Override
        public Object[] getPointers() {
            return dummyPointers;
        }
    }

    @BeforeClass
    public static void setUpSqueakImageContext() {
        image = new SqueakImageContext("fake.image");
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
        image.specialObjectsArray.fillin(new DummyPointersChunk(new Object[100]));
        image.compiledMethodClass.fillin(new DummyFormatChunk(100)); // sets instanceSize to 100
    }
}
