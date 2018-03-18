package de.hpi.swa.trufflesqueak.test;

import org.junit.BeforeClass;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.util.SqueakImageChunk;

public abstract class AbstractSqueakTestCaseWithDummyImage extends AbstractSqueakTestCase {

    private static class DummyFormatChunk extends SqueakImageChunk {

        public DummyFormatChunk(int format) {
            super(null, null, 0, format, 0, 0, 0);
        }

        @Override
        public Object[] getPointers() {
            Object[] pointers = new Object[6];
            pointers[2] = (long) format; // FORMAT_INDEX
            return pointers;
        }
    }

    private static class DummyPointersChunk extends SqueakImageChunk {
        private Object[] dummyPointers;

        public DummyPointersChunk(Object[] pointers) {
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
        image.plus.setBytes("plus".getBytes());
        image.minus.setBytes("minus".getBytes());
        image.lt.setBytes("lt".getBytes());
        image.gt.setBytes("gt".getBytes());
        image.le.setBytes("le".getBytes());
        image.ge.setBytes("ge".getBytes());
        image.eq.setBytes("eq".getBytes());
        image.ne.setBytes("ne".getBytes());
        image.times.setBytes("times".getBytes());
        image.divide.setBytes("divide".getBytes());
        image.modulo.setBytes("modulo".getBytes());
        image.pointAt.setBytes("pointAt".getBytes());
        image.bitShift.setBytes("bitShift".getBytes());
        image.floorDivide.setBytes("floorDivide".getBytes());
        image.bitAnd.setBytes("bitAnd".getBytes());
        image.bitOr.setBytes("bitOr".getBytes());
        image.at.setBytes("at".getBytes());
        image.atput.setBytes("atput".getBytes());
        image.size_.setBytes("size".getBytes());
        image.next.setBytes("next".getBytes());
        image.nextPut.setBytes("nextPut".getBytes());
        image.atEnd.setBytes("atEnd".getBytes());
        image.equivalent.setBytes("equivalent".getBytes());
        image.klass.setBytes("klass".getBytes());
        image.blockCopy.setBytes("blockCopy".getBytes());
        image.value_.setBytes("value".getBytes());
        image.valueWithArg.setBytes("valueWithArg".getBytes());
        image.do_.setBytes("do".getBytes());
        image.new_.setBytes("new".getBytes());
        image.newWithArg.setBytes("newWithArg".getBytes());
        image.x.setBytes("x".getBytes());
        image.y.setBytes("y".getBytes());
        image.specialObjectsArray.fillin(new DummyPointersChunk(new Object[100]));
        image.compiledMethodClass.fillin(new DummyFormatChunk(100)); // sets instanceSize to 100
    }
}