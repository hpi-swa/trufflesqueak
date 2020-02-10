/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.test;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import de.hpi.swa.graal.squeak.image.SqueakImageChunk;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.CLASS_DESCRIPTION;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.METACLASS;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.SPECIAL_OBJECT;
import de.hpi.swa.graal.squeak.util.MiscUtils;

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

        image.setByteSymbolClass(new ClassObject(image));
        setupMeta(image.metaClass, new Object[]{
                        null, null, 0L, null, null, null, asByteSymbol("Metaclass"), null, null, null, null});
        setupMeta(image.getByteSymbolClass(), new Object[]{
                        null, null, 0L, null, null, null, asByteSymbol("ByteSymbol"), null, null, null, null});
        setupMeta(image.compiledMethodClass, new Object[]{null, null, 100L,  // sets instanceSize to
                                                                             // 100
                        null, null, null, asByteSymbol("CompiledMethod"), null, null, null, null});
        setupMeta(image.nilClass, new Object[]{
                        null, null, 0L, null, null, null, asByteSymbol("UndefinedObject"), null, null, null, null});
        setupMeta(image.arrayClass, new Object[]{
                        null, null, 0L, null, null, null, asByteSymbol("Array"), null, null, null, null});

        final ClassObject bindingClass = setupMeta(new ClassObject(image), new Object[]{
                        null, null, 1L, null, null, null, asByteSymbol("Binding"), null, null, null, null});
        final ClassObject classBindingClass = setupMeta(new ClassObject(image), new Object[]{
                        bindingClass, null, 2L, null, null, null, asByteSymbol("ClassBinding"), null, null, null, null});
        nilClassBinding = new PointersObject(image, classBindingClass);
        nilClassBinding.fillin(SqueakImageChunk.createDummyChunk(image, new Object[]{
                        asByteSymbol("UndefinedObject"), image.nilClass}));

        image.initializeAfterLoadingImage(ArrayObject.createEmptyStrategy(image, image.arrayClass, 0));
        context.enter();
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
        dummyStorage[0] = asByteSymbol("+");
        dummyStorage[1] = 1L;
        dummyStorage[2] = asByteSymbol("-");
        dummyStorage[3] = 1L;
        dummyStorage[4] = asByteSymbol("<");
        dummyStorage[5] = 1L;
        dummyStorage[6] = asByteSymbol(">");
        dummyStorage[7] = 1L;
        dummyStorage[8] = asByteSymbol("<=");
        dummyStorage[9] = 1L;
        dummyStorage[10] = asByteSymbol(">=");
        dummyStorage[11] = 1L;
        dummyStorage[12] = asByteSymbol("=");
        dummyStorage[13] = 1L;
        dummyStorage[14] = asByteSymbol("~=");
        dummyStorage[15] = 1L;
        dummyStorage[16] = asByteSymbol("*");
        dummyStorage[17] = 1L;
        dummyStorage[18] = asByteSymbol("/");
        dummyStorage[19] = 1L;
        dummyStorage[20] = asByteSymbol("\\");
        dummyStorage[21] = 1L;
        dummyStorage[22] = asByteSymbol("@");
        dummyStorage[23] = 1L;
        dummyStorage[24] = asByteSymbol("bitShift:");
        dummyStorage[25] = 1L;
        dummyStorage[26] = asByteSymbol("//");
        dummyStorage[27] = 1L;
        dummyStorage[28] = asByteSymbol("bitAnd:");
        dummyStorage[29] = 1L;
        dummyStorage[30] = asByteSymbol("bitOr:");
        dummyStorage[31] = 1L;
        dummyStorage[32] = asByteSymbol("at:");
        dummyStorage[33] = 1L;
        dummyStorage[34] = asByteSymbol("at:put:");
        dummyStorage[35] = 2L;
        dummyStorage[36] = asByteSymbol("size");
        dummyStorage[37] = 0L;
        dummyStorage[38] = asByteSymbol("next");
        dummyStorage[39] = 0L;
        dummyStorage[40] = asByteSymbol("nextPut:");
        dummyStorage[41] = 1L;
        dummyStorage[42] = asByteSymbol("atEnd");
        dummyStorage[43] = 0L;
        dummyStorage[44] = asByteSymbol("==");
        dummyStorage[45] = 1L;
        dummyStorage[46] = asByteSymbol("class");
        dummyStorage[47] = 0L;
        dummyStorage[48] = asByteSymbol("~~");
        dummyStorage[49] = 1L;
        dummyStorage[50] = asByteSymbol("value");
        dummyStorage[51] = 0L;
        dummyStorage[52] = asByteSymbol("value:");
        dummyStorage[53] = 1L;
        dummyStorage[54] = asByteSymbol("do:");
        dummyStorage[55] = 1L;
        dummyStorage[56] = asByteSymbol("new");
        dummyStorage[57] = 0L;
        dummyStorage[58] = asByteSymbol("new:");
        dummyStorage[59] = 1L;
        dummyStorage[60] = asByteSymbol("x");
        dummyStorage[61] = 0L;
        dummyStorage[62] = asByteSymbol("y");
        dummyStorage[63] = 0L;
        dummySpecialSelectors.setStorage(dummyStorage);
        return dummySpecialSelectors;
    }

    private static NativeObject asByteSymbol(final String value) {
        return NativeObject.newNativeBytes(image, image.getByteSymbolClass(), MiscUtils.stringToBytes(value));
    }

    @AfterClass
    public static void tearDown() {
        destroyImageContext();
    }
}
