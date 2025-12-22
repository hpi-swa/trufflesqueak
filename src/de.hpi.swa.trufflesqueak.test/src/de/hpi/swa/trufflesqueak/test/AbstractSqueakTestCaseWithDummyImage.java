/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.test;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.CLASS_DESCRIPTION;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.METACLASS;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.SPECIAL_OBJECT;
import de.hpi.swa.trufflesqueak.util.MiscUtils;

public abstract class AbstractSqueakTestCaseWithDummyImage extends AbstractSqueakTestCase {

    @BeforeClass
    public static void setUpSqueakImageContext() {
        SqueakImageContext.initializeBeforeLoadingImage();
        loadImageContext(new TestImageSpec("fake.image", false));
        final Object[] dummySpecialObjects = new Object[100];
        final ArrayObject dummySpecialSelectors = createDummySpecialSelectors();
        dummySpecialObjects[SPECIAL_OBJECT.SPECIAL_SELECTORS] = dummySpecialSelectors;
        image.specialObjectsArray.setStorage(dummySpecialObjects);
        image.specialObjectsArray.setSqueakClass(image.arrayClass);
        dummySpecialSelectors.setSqueakClass(image.arrayClass);

        image.setByteSymbolClass(new ClassObject(image));
        setupMeta(image.metaClass, null, 0L, "Metaclass");
        setupMeta(image.getByteSymbolClass(), null, 0L, "ByteSymbol");
        setupMeta(image.compiledMethodClass, null, 1572864L, "CompiledMethod");
        setupMeta(image.nilClass, null, 0L, "UndefinedObject");
        setupMeta(image.arrayClass, null, 0L, "Array");

        final ClassObject bindingClass = setupMeta(new ClassObject(image), null, 1L, "Binding");
        final ClassObject classBindingClass = setupMeta(new ClassObject(image), bindingClass, 2L, "ClassBinding");
        nilClassBinding = new PointersObject(classBindingClass);
        nilClassBinding.instVarAtPut0Slow(0, asByteSymbol("UndefinedObject"));
        nilClassBinding.instVarAtPut0Slow(1, image.nilClass);

        image.setHiddenRoots(ArrayObject.createEmptyStrategy(image.arrayClass, 0));
        context.enter();
    }

    private static ClassObject setupMeta(final ClassObject aClass, final ClassObject superclass, final long format, final String name) {
        aClass.setSuperclass(superclass);
        aClass.setMethodDict(null);
        aClass.setFormat(format);
        aClass.setOtherPointers(new Object[]{null, null, null, asByteSymbol(name), null, null, null, null});

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
        return NativeObject.newNativeBytes(image.getByteSymbolClass(), MiscUtils.stringToBytes(value));
    }

    @AfterClass
    public static void tearDown() {
        destroyImageContext();
    }
}
