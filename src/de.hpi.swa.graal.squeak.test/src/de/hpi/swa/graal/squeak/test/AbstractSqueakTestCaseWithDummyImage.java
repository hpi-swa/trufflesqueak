package de.hpi.swa.graal.squeak.test;

import org.junit.BeforeClass;

import de.hpi.swa.graal.squeak.image.AbstractImageChunk;

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
        image.specialObjectsArray.fillin(new DummyPointersChunk(new Object[100]));
        image.compiledMethodClass.fillin(new DummyFormatChunk(100)); // sets instanceSize to 100
    }
}
