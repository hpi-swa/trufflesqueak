/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.test;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
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
        SqueakImageContext.initializeBeforeLoadingImage();
        loadImageContext("fake.image");
        final Object[] dummySpecialObjects = new Object[100];
        final ArrayObject dummySpecialSelectors = createDummySpecialSelectors();
        dummySpecialObjects[SPECIAL_OBJECT.SPECIAL_SELECTORS] = dummySpecialSelectors;
        image.specialObjectsArray.setStorage(dummySpecialObjects);
        image.specialObjectsArray.setSqueakClass(image.arrayClass);
        dummySpecialSelectors.setSqueakClass(image.arrayClass);

        setupMeta(image.metaClass, new Object[]{
                        null, null, 0L, null, null, null, image.asByteString("Metaclass"), null, null, null, null});
        setupMeta(image.compiledMethodClass, new Object[]{null, null, 100L,  // sets instanceSize to
                                                                             // 100
                        null, null, null, image.asByteString("CompiledMethod"), null, null, null, null});
        setupMeta(image.nilClass, new Object[]{
                        null, null, 0L, null, null, null, image.asByteString("UndefinedObject"), null, null, null, null});
        setupMeta(image.arrayClass, new Object[]{
                        null, null, 0L, null, null, null, image.asByteString("Array"), null, null, null, null});

        final ClassObject bindingClass = setupMeta(new ClassObject(image), new Object[]{
                        null, null, 2L, null, null, null, image.asByteString("ClassBinding"), null, null, null, null});
        nilClassBinding = new PointersObject(image, bindingClass);
        nilClassBinding.fillin(SqueakImageChunk.createDummyChunk(image, new Object[]{
                        image.asByteString("UndefinedObject"), image.nilClass}));

        image.initializeAfterLoadingImage();
    }

    private static ClassObject setupMeta(final ClassObject aClass, final Object[] pointers) {
        final SqueakImageChunk fakeChunk = SqueakImageChunk.createDummyChunk(image, pointers);
        aClass.fillin(fakeChunk);
        final ClassObject aClassClass = new ClassObject(image, image.metaClass, METACLASS.INST_SIZE);
        aClassClass.setInstancesAreClasses();
        aClass.setSqueakClass(aClassClass);
        aClassClass.setOtherPointer(CLASS_DESCRIPTION.SIZE + 0, aClass);
        return aClass;
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
