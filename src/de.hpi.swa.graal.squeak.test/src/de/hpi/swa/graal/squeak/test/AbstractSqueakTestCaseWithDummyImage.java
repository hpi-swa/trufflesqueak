/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.test;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import de.hpi.swa.graal.squeak.image.reading.SqueakImageChunk;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.CLASS_DESCRIPTION;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.METACLASS;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.SPECIAL_OBJECT;

public abstract class AbstractSqueakTestCaseWithDummyImage extends AbstractSqueakTestCase {

    @BeforeClass
    public static void setUpSqueakImageContext() {
        loadImageContext("fake.image");
        final Object[] dummySpecialObjects = new Object[100];
        dummySpecialObjects[SPECIAL_OBJECT.SPECIAL_SELECTORS] = createDummySpecialSelectors();
        image.specialObjectsArray.setStorage(dummySpecialObjects);

        Object[] pointers = new Object[]{
                        null, null, 0L, null, null, null, image.asByteString("Metaclass"), null, null, null, null};
        SqueakImageChunk fakeChunk = SqueakImageChunk.createDummyChunk(image, pointers);
        image.metaClass.fillin(fakeChunk);
        final ClassObject metaClassClass = new ClassObject(image, image.metaClass, METACLASS.INST_SIZE);
        metaClassClass.setInstancesAreClasses();
        image.metaClass.setSqueakClass(metaClassClass);
        metaClassClass.setOtherPointer(CLASS_DESCRIPTION.SIZE + 0, image.metaClass);

        pointers = new Object[]{null, null, 100L, null, null,
                        null, image.asByteString("CompiledMethod"), null, null, null, null}; // sets
        // instanceSize
        // to 100
        fakeChunk = SqueakImageChunk.createDummyChunk(image, pointers);
        image.compiledMethodClass.fillin(fakeChunk);
        final ClassObject compiledMethodClassClass = new ClassObject(image, image.metaClass, METACLASS.INST_SIZE);
        compiledMethodClassClass.setInstancesAreClasses();
        image.compiledMethodClass.setSqueakClass(compiledMethodClassClass);
        compiledMethodClassClass.setOtherPointer(CLASS_DESCRIPTION.SIZE + 0, image.compiledMethodClass);

        pointers = new Object[]{
                        null, null, 0L, null, null, null, image.asByteString("UndefinedObject"), null, null, null, null};
        fakeChunk = SqueakImageChunk.createDummyChunk(image, pointers);
        image.nilClass.fillin(fakeChunk);
        final ClassObject nilClassClass = new ClassObject(image, image.metaClass, METACLASS.INST_SIZE);
        nilClassClass.setInstancesAreClasses();
        image.nilClass.setSqueakClass(nilClassClass);
        nilClassClass.setOtherPointer(CLASS_DESCRIPTION.SIZE + 0, image.nilClass);

        pointers = new Object[]{
                        null, null, 0L, null, null, null, image.asByteString("Array"), null, null, null, null};
        fakeChunk = SqueakImageChunk.createDummyChunk(image, pointers);
        image.arrayClass.fillin(fakeChunk);
        final ClassObject arrayClassClass = new ClassObject(image, image.metaClass, METACLASS.INST_SIZE);
        arrayClassClass.setInstancesAreClasses();
        image.arrayClass.setSqueakClass(arrayClassClass);
        arrayClassClass.setOtherPointer(CLASS_DESCRIPTION.SIZE + 0, image.arrayClass);

        image.specialObjectsArray.setSqueakClass(image.arrayClass);
        ((ArrayObject) dummySpecialObjects[SPECIAL_OBJECT.SPECIAL_SELECTORS]).setSqueakClass(image.arrayClass);

        final ClassObject bindingClass = new ClassObject(image);
        pointers = new Object[]{
                        null, null, 2L, null, null, null, image.asByteString("ClassBinding"), null, null, null, null};
        fakeChunk = SqueakImageChunk.createDummyChunk(image, pointers);
        bindingClass.fillin(fakeChunk);
        final ClassObject bindingClassClass = new ClassObject(image, image.metaClass, METACLASS.INST_SIZE);
        bindingClassClass.setInstancesAreClasses();
        bindingClass.setSqueakClass(bindingClassClass);
        bindingClassClass.setOtherPointer(CLASS_DESCRIPTION.SIZE + 0, bindingClass);
        nilClassBinding = new PointersObject(image, bindingClass);
        pointers = new Object[]{image.asByteString("UndefinedObject"), image.nilClass};
        fakeChunk = SqueakImageChunk.createDummyChunk(image, pointers);
        nilClassBinding.fillin(fakeChunk);

        image.initializePrimitives();
    }

    private static ArrayObject createDummySpecialSelectors() {
        final ArrayObject dummySpecialSelectors = image.newEmptyArray();
        final Object[] dummyStorage = new Object[64];
        dummyStorage[0] = image.asByteString("+");
        dummyStorage[1] = 1L;
        dummyStorage[2] = image.asByteString("-");
        dummyStorage[3] = 1L;
        dummyStorage[4] = image.asByteString("<");
        dummyStorage[5] = 1L;
        dummyStorage[6] = image.asByteString(">");
        dummyStorage[7] = 1L;
        dummyStorage[8] = image.asByteString("<=");
        dummyStorage[9] = 1L;
        dummyStorage[10] = image.asByteString(">=");
        dummyStorage[11] = 1L;
        dummyStorage[12] = image.asByteString("=");
        dummyStorage[13] = 1L;
        dummyStorage[14] = image.asByteString("~=");
        dummyStorage[15] = 1L;
        dummyStorage[16] = image.asByteString("*");
        dummyStorage[17] = 1L;
        dummyStorage[18] = image.asByteString("/");
        dummyStorage[19] = 1L;
        dummyStorage[20] = image.asByteString("\\");
        dummyStorage[21] = 1L;
        dummyStorage[22] = image.asByteString("@");
        dummyStorage[23] = 1L;
        dummyStorage[24] = image.asByteString("bitShift:");
        dummyStorage[25] = 1L;
        dummyStorage[26] = image.asByteString("//");
        dummyStorage[27] = 1L;
        dummyStorage[28] = image.asByteString("bitAnd:");
        dummyStorage[29] = 1L;
        dummyStorage[30] = image.asByteString("bitOr:");
        dummyStorage[31] = 1L;
        dummyStorage[32] = image.asByteString("at:");
        dummyStorage[33] = 1L;
        dummyStorage[34] = image.asByteString("at:put:");
        dummyStorage[35] = 2L;
        dummyStorage[36] = image.asByteString("size");
        dummyStorage[37] = 0L;
        dummyStorage[38] = image.asByteString("next");
        dummyStorage[39] = 0L;
        dummyStorage[40] = image.asByteString("nextPut:");
        dummyStorage[41] = 1L;
        dummyStorage[42] = image.asByteString("atEnd");
        dummyStorage[43] = 0L;
        dummyStorage[44] = image.asByteString("==");
        dummyStorage[45] = 1L;
        dummyStorage[46] = image.asByteString("class");
        dummyStorage[47] = 0L;
        dummyStorage[48] = image.asByteString("~~");
        dummyStorage[49] = 1L;
        dummyStorage[50] = image.asByteString("value");
        dummyStorage[51] = 0L;
        dummyStorage[52] = image.asByteString("value:");
        dummyStorage[53] = 1L;
        dummyStorage[54] = image.asByteString("do:");
        dummyStorage[55] = 1L;
        dummyStorage[56] = image.asByteString("new");
        dummyStorage[57] = 0L;
        dummyStorage[58] = image.asByteString("new:");
        dummyStorage[59] = 1L;
        dummyStorage[60] = image.asByteString("x");
        dummyStorage[61] = 0L;
        dummyStorage[62] = image.asByteString("y");
        dummyStorage[63] = 0L;
        dummySpecialSelectors.setStorage(dummyStorage);
        return dummySpecialSelectors;
    }

    @AfterClass
    public static void tearDown() {
        destroyImageContext();
    }
}
