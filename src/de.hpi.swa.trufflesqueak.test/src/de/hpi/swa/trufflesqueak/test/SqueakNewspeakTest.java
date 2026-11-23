/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject.CompiledCodeHeaderUtils;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.VariablePointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.METACLASS;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.METHOD_DICT;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.NEWSPEAK_CLASS;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.NEWSPEAK_MIXIN;
import de.hpi.swa.trufflesqueak.nodes.dispatch.NewspeakLookup;
import de.hpi.swa.trufflesqueak.util.MiscUtils;

/**
 * Tests for Newspeak support: the NewsqueakV4 bytecode set, access control, and the lookup of
 * sends with absent receivers.
 */
@SuppressWarnings("static-method")
public final class SqueakNewspeakTest extends AbstractSqueakTestCaseWithDummyImage {
    /* Class formats: instance specification in bits 16-20 and instance size in bits 0-15. */
    private static final long FORMAT_FIXED_POINTERS = 1 << 16;
    private static final long FORMAT_METHOD_DICTIONARY = 3 << 16 | 2;
    private static final int MIXIN_INST_SIZE = 4;

    private static ClassObject methodDictionaryClass;
    private static ClassObject mixinClass;

    @BeforeClass
    public static void setUpNewspeak() {
        image.setIsNewspeak();
        methodDictionaryClass = newClass("MethodDictionary", null, FORMAT_METHOD_DICTIONARY);
        mixinClass = newClass("InstanceMixin", null, FORMAT_FIXED_POINTERS | MIXIN_INST_SIZE);
    }

    /*
     * Method headers
     */

    @Test
    public void testSignExtendedHeader() {
        /* Negative header of a 32-bit image: numArgs=1, numTemps=2, numLiterals=3, private. */
        final long header31Bit = 1L << 24 | 2L << 18 | 1L << 28 | 3;
        final long header = header31Bit - (1L << 30);
        assertTrue(header < 0);
        final CompiledCodeObject method = new CompiledCodeObject(new byte[]{(byte) 216, 0}, header, new Object[]{NilObject.SINGLETON, NilObject.SINGLETON, NilObject.SINGLETON},
                        image.compiledMethodClass);
        assertEquals(1, method.getNumArgs());
        assertEquals(2, method.getNumTemps());
        assertEquals(3, method.getNumLiterals());
        assertTrue(method.isPrivate());
        assertFalse(method.getHasV3PlusClosuresBytecodes());
        assertTrue(method.getHasNewsqueakV4Bytecodes(image));
        assertEquals("Header must be restored unchanged", header, method.getHeader());
    }

    @Test
    public void testAccessModifiers() {
        assertTrue(newMethod(CompiledCodeObject.ACCESS_MODIFIER_PUBLIC, null, 216).isPublic());
        assertTrue(newMethod(CompiledCodeObject.ACCESS_MODIFIER_PRIVATE, null, 216).isPrivate());
        assertTrue(newMethod(CompiledCodeObject.ACCESS_MODIFIER_PROTECTED, null, 216).isProtected());
    }

    /*
     * NewsqueakV4 bytecodes
     */

    @Test
    public void testPushPseudoVariables() {
        final Object receiver = image.asArrayOfObjects(42L);
        /* 77: push false; 225 n 77: push true (1) or nil (2); 217: return top */
        assertSame(BooleanObject.FALSE, runV4(receiver, 77, 217));
        assertSame(BooleanObject.TRUE, runV4(receiver, 225, 1, 77, 217));
        assertSame(NilObject.SINGLETON, runV4(receiver, 225, 2, 77, 217));
    }

    @Test
    public void testPushConstantsAndReceiver() {
        final Object receiver = image.asArrayOfObjects(42L);
        assertEquals(0L, runV4(receiver, 78, 217));
        assertEquals(1L, runV4(receiver, 79, 217));
        assertSame(receiver, runV4(receiver, 76, 217));
        assertSame(receiver, runV4(receiver, 216));
        assertEquals(42L, runV4(receiver, 0, 217));
        assertEquals(42L, runV4(receiver, 226, 0, 217));
    }

    @Test
    public void testExtendedPushInteger() {
        final Object receiver = NilObject.SINGLETON;
        assertEquals(42L, runV4(receiver, 229, 42, 217));
        assertEquals(256L, runV4(receiver, 225, 1, 229, 0, 217));
        /* The first extension byte is signed. */
        assertEquals(-2L, runV4(receiver, 225, 255, 229, 254, 217));
        assertEquals(-32768L, runV4(receiver, 225, 128, 229, 0, 217));
        /* Multiple extension bytes. */
        assertEquals(0x10203L, runV4(receiver, 225, 1, 225, 2, 229, 3, 217));
    }

    @Test
    public void testTemporaries() {
        final Object receiver = NilObject.SINGLETON;
        /* push 1, pop into temp 0, push temp 0, return top */
        assertEquals(1L, runV4(receiver, 79, 184, 64, 217));
        /* push 1, store into temp 1 (long), pop, push temp 1 (long), return top */
        assertEquals(1L, runV4(receiver, 79, 234, 1, 220, 230, 1, 217));
        /* push 0, pop into temp 2 (long), push temp 2, return top */
        assertEquals(0L, runV4(receiver, 78, 237, 2, 66, 217));
    }

    @Test
    public void testJumps() {
        final Object receiver = NilObject.SINGLETON;
        /* short jump over push 0 */
        assertEquals(1L, runV4(receiver, 192, 78, 79, 217));
        /* push true, jump if true over (push 0, return top), push 1, return top */
        assertEquals(1L, runV4(receiver, 225, 1, 77, 201, 78, 217, 79, 217));
        /* push false, jump if false over (push 0, return top), push 1, return top */
        assertEquals(1L, runV4(receiver, 77, 209, 78, 217, 79, 217));
        /* push true, jump if false (not taken), push 0, return top */
        assertEquals(0L, runV4(receiver, 225, 1, 77, 209, 78, 217, 79, 217));
        /* extended jumps */
        assertEquals(1L, runV4(receiver, 242, 1, 78, 79, 217));
        assertEquals(1L, runV4(receiver, 225, 1, 77, 243, 2, 78, 217, 79, 217));
        assertEquals(1L, runV4(receiver, 77, 244, 2, 78, 217, 79, 217));
    }

    @Test
    public void testNewArrayAndDuplicate() {
        final Object receiver = NilObject.SINGLETON;
        /* push 0, push 1, new array with 2 elements from stack, return top */
        final ArrayObject array = (ArrayObject) runV4(receiver, 78, 79, 231, 128 + 2, 217);
        assertEquals(2, array.getObjectLength());
        assertEquals(0L, array.getObjectStorage()[0]);
        assertEquals(1L, array.getObjectStorage()[1]);
        /* push 1, dup, pop, return top */
        assertEquals(1L, runV4(receiver, 79, 219, 220, 217));
    }

    @Test
    public void testSpecialSelectorArithmetic() {
        final Object receiver = NilObject.SINGLETON;
        /* push 1, push 1, +, return top */
        assertEquals(2L, runV4(receiver, 79, 79, 80, 217));
        /* push 1, push 0, <, return top */
        assertSame(BooleanObject.FALSE, runV4(receiver, 79, 78, 82, 217));
        /* push 1, push 1, ==, return top */
        assertSame(BooleanObject.TRUE, runV4(receiver, 79, 79, 102, 217));
    }

    @Test
    public void testPushEnclosingObject() {
        final Hierarchy h = new Hierarchy();
        /* 225 255 77: push enclosing object at depth 1, return top */
        final CompiledCodeObject method = newMethod(CompiledCodeObject.ACCESS_MODIFIER_PUBLIC, h.innerMixin, 225, 255, 77, 217);
        assertSame(h.outerInstance, runMethod(method, h.innerInstance));
    }

    @Test
    public void testImplicitReceiverSendBytecode() {
        final Hierarchy h = new Hierarchy();
        /* Implicit receiver send of #outerMethod (literal 0) with no arguments, return top. */
        final long header = v4Header(CompiledCodeObject.ACCESS_MODIFIER_PUBLIC, 3);
        final CompiledCodeObject method = new CompiledCodeObject(new byte[]{(byte) 160, (byte) 217, 0}, header, new Object[]{h.outerSelector, symbol("test"), binding(h.innerMixin)},
                        image.compiledMethodClass);
        /* Outer>>outerMethod answers its receiver, which must be the enclosing object. */
        assertSame(h.outerInstance, runMethod(method, h.innerInstance));
        /* Same send through the extended bytecode. */
        final CompiledCodeObject extendedMethod = new CompiledCodeObject(new byte[]{(byte) 240, 0, (byte) 217, 0}, header, new Object[]{h.outerSelector, symbol("test"), binding(h.innerMixin)},
                        image.compiledMethodClass);
        assertSame(h.outerInstance, runMethod(extendedMethod, h.innerInstance));
    }

    /*
     * Access control for ordinary sends
     */

    @Test
    public void testOrdinaryLookup() {
        final ClassObject superclass = newClass("Super", null, FORMAT_FIXED_POINTERS);
        final ClassObject subclass = newClass("Sub", superclass, FORMAT_FIXED_POINTERS);
        final NativeObject publicSelector = symbol("publicMethod");
        final NativeObject protectedSelector = symbol("protectedMethod");
        final NativeObject privateSelector = symbol("privateMethod");
        final NativeObject shadowedSelector = symbol("shadowedMethod");
        final CompiledCodeObject publicMethod = newMethod(CompiledCodeObject.ACCESS_MODIFIER_PUBLIC, null, 216);
        final CompiledCodeObject shadowedPublicMethod = newMethod(CompiledCodeObject.ACCESS_MODIFIER_PUBLIC, null, 216);
        final CompiledCodeObject protectedMethod = newMethod(CompiledCodeObject.ACCESS_MODIFIER_PROTECTED, null, 216);
        final CompiledCodeObject privateMethod = newMethod(CompiledCodeObject.ACCESS_MODIFIER_PRIVATE, null, 216);
        final CompiledCodeObject shadowingPrivateMethod = newMethod(CompiledCodeObject.ACCESS_MODIFIER_PRIVATE, null, 216);
        superclass.setMethodDict(newMethodDictionary(new NativeObject[]{publicSelector, protectedSelector, privateSelector, shadowedSelector},
                        new CompiledCodeObject[]{publicMethod, protectedMethod, privateMethod, shadowedPublicMethod}));
        subclass.setMethodDict(newMethodDictionary(new NativeObject[]{shadowedSelector}, new CompiledCodeObject[]{shadowingPrivateMethod}));

        /* Ordinary sends only find public methods. */
        assertSame(publicMethod, subclass.lookupInMethodDictSlow(publicSelector));
        assertNull("Protected methods are not understood", subclass.lookupInMethodDictSlow(protectedSelector));
        assertNull("Private methods are not understood", subclass.lookupInMethodDictSlow(privateSelector));
        assertSame("Private methods are skipped", shadowedPublicMethod, subclass.lookupInMethodDictSlow(shadowedSelector));
        assertNull(subclass.lookupInMethodDictSlow(symbol("unknownMethod")));

        /* Lookups for self, outer, implicit receiver, and super sends skip private methods only. */
        assertSame(protectedMethod, subclass.lookupProtectedInMethodDictSlow(protectedSelector));
        assertNull(subclass.lookupProtectedInMethodDictSlow(privateSelector));
        assertSame(shadowedPublicMethod, subclass.lookupProtectedInMethodDictSlow(shadowedSelector));

        /* The lookup for doesNotUnderstand: ignores access modifiers. */
        assertSame(shadowingPrivateMethod, subclass.lookupInMethodDictIgnoringAccessSlow(shadowedSelector));
        assertSame(privateMethod, subclass.lookupInMethodDictIgnoringAccessSlow(privateSelector));
        assertSame(shadowingPrivateMethod, subclass.lookupInOwnMethodDictSlow(shadowedSelector));
        assertNull(subclass.lookupInOwnMethodDictSlow(publicSelector));
    }

    /*
     * Lookup of sends with absent receivers
     */

    @Test
    public void testImplicitReceiverSendLookup() {
        final Hierarchy h = new Hierarchy();
        final CompiledCodeObject sender = newMethod(CompiledCodeObject.ACCESS_MODIFIER_PUBLIC, h.innerMixin, 216);

        /* A method of the enclosing class is lexically visible: the receiver is the enclosing object. */
        final NewspeakLookup.Result outer = NewspeakLookup.lookup(h.innerClass, sender, h.outerSelector, NewspeakLookup.KIND_IMPLICIT);
        assertSame(h.outerMethod, outer.lookupResult);
        assertSame(h.outerInstance, outer.getReceiver(h.innerInstance));

        /* A method of the class itself: the receiver is the receiver of the sending method. */
        final NewspeakLookup.Result inner = NewspeakLookup.lookup(h.innerClass, sender, h.innerSelector, NewspeakLookup.KIND_IMPLICIT);
        assertSame(h.innerMethod, inner.lookupResult);
        assertSame(h.innerInstance, inner.getReceiver(h.innerInstance));

        /* Private methods are lexically visible. */
        final NewspeakLookup.Result privateInner = NewspeakLookup.lookup(h.innerClass, sender, h.privateSelector, NewspeakLookup.KIND_IMPLICIT);
        assertSame(h.innerPrivateMethod, privateInner.lookupResult);
        assertSame(h.innerInstance, privateInner.getReceiver(h.innerInstance));

        /* Not lexically visible: inherited (non-private) methods of the receiver are found. */
        final NewspeakLookup.Result inherited = NewspeakLookup.lookup(h.innerClass, sender, h.inheritedSelector, NewspeakLookup.KIND_IMPLICIT);
        assertSame(h.inheritedMethod, inherited.lookupResult);
        assertSame(h.innerInstance, inherited.getReceiver(h.innerInstance));

        /* Neither lexically visible nor inherited: doesNotUnderstand: is sent to the receiver. */
        final NewspeakLookup.Result unknown = NewspeakLookup.lookup(h.innerClass, sender, symbol("unknownMethod"), NewspeakLookup.KIND_IMPLICIT);
        assertNull(unknown.lookupResult);
        assertSame(h.innerClass, unknown.lookupClass);
        assertSame(h.innerInstance, unknown.getReceiver(h.innerInstance));
    }

    @Test
    public void testSelfAndOuterSendLookup() {
        final Hierarchy h = new Hierarchy();
        final CompiledCodeObject sender = newMethod(CompiledCodeObject.ACCESS_MODIFIER_PUBLIC, h.innerMixin, 216);

        final NewspeakLookup.Result self = NewspeakLookup.lookup(h.innerClass, sender, h.privateSelector, NewspeakLookup.KIND_SELF);
        assertSame(h.innerPrivateMethod, self.lookupResult);
        assertSame(h.innerInstance, self.getReceiver(h.innerInstance));

        /* Self sends do not find methods of enclosing classes. */
        assertNull(NewspeakLookup.lookup(h.innerClass, sender, h.outerSelector, NewspeakLookup.KIND_SELF).lookupResult);

        /* Outer sends with depth 1 are sent to the enclosing object and find its private methods. */
        final NewspeakLookup.Result outer = NewspeakLookup.lookup(h.innerClass, sender, h.privateSelector, 1);
        assertSame(h.outerPrivateMethod, outer.lookupResult);
        assertSame(h.outerInstance, outer.getReceiver(h.innerInstance));

        final NewspeakLookup.Result enclosing = NewspeakLookup.lookupEnclosingObject(h.innerClass, sender, 1);
        assertSame(h.outerInstance, enclosing.getReceiver(h.innerInstance));
        assertSame(h.innerInstance, NewspeakLookup.lookupEnclosingObject(h.innerClass, sender, 0).getReceiver(h.innerInstance));
    }

    @Test
    public void testSuperSendLookup() {
        final Hierarchy h = new Hierarchy();
        final CompiledCodeObject sender = newMethod(CompiledCodeObject.ACCESS_MODIFIER_PUBLIC, h.innerMixin, 216);

        /* The lookup starts in the superclass of the application of the method's mixin. */
        final NewspeakLookup.Result inherited = NewspeakLookup.lookup(h.innerClass, sender, h.inheritedSelector, NewspeakLookup.KIND_DYNAMIC_SUPER);
        assertSame(h.inheritedMethod, inherited.lookupResult);
        assertSame(h.innerInstance, inherited.getReceiver(h.innerInstance));

        final NewspeakLookup.Result notInherited = NewspeakLookup.lookup(h.innerClass, sender, h.innerSelector, NewspeakLookup.KIND_DYNAMIC_SUPER);
        assertNull(notInherited.lookupResult);
        assertSame("Lookup of doesNotUnderstand: starts in the superclass", h.baseClass, notInherited.lookupClass);
    }

    @Test
    public void testLookupCache() {
        final Hierarchy h = new Hierarchy();
        final CompiledCodeObject sender = newMethod(CompiledCodeObject.ACCESS_MODIFIER_PUBLIC, h.innerMixin, 216);
        final NewspeakLookup.Result first = image.newspeakLookupCache.lookup(h.innerClass, sender, h.outerSelector, NewspeakLookup.KIND_IMPLICIT);
        assertSame(first, image.newspeakLookupCache.lookup(h.innerClass, sender, h.outerSelector, NewspeakLookup.KIND_IMPLICIT));
        /* Changing a class invalidates the cached result. */
        h.outerClass.setMethodDict(newMethodDictionary(new NativeObject[0], new CompiledCodeObject[0]));
        assertFalse(first.isValid());
        final NewspeakLookup.Result second = image.newspeakLookupCache.lookup(h.innerClass, sender, h.outerSelector, NewspeakLookup.KIND_IMPLICIT);
        assertNull("Method is gone", second.lookupResult);
    }

    /*
     * Helpers
     */

    /**
     * A base class, an outer class, and an inner class that is nested in the outer class and
     * inherits from the base class, with one instance of the outer class and one of the inner
     * class.
     */
    private static final class Hierarchy {
        private final NativeObject outerSelector = symbol("outerMethod");
        private final NativeObject innerSelector = symbol("innerMethod");
        private final NativeObject privateSelector = symbol("privateMethod");
        private final NativeObject inheritedSelector = symbol("inheritedMethod");

        private final PointersObject outerMixin = newMixin(NilObject.SINGLETON);
        private final PointersObject innerMixin = newMixin(outerMixin);

        /* Public methods answer their receiver. */
        private final CompiledCodeObject outerMethod = newMethod(CompiledCodeObject.ACCESS_MODIFIER_PUBLIC, outerMixin, 216);
        private final CompiledCodeObject outerPrivateMethod = newMethod(CompiledCodeObject.ACCESS_MODIFIER_PRIVATE, outerMixin, 216);
        private final CompiledCodeObject innerMethod = newMethod(CompiledCodeObject.ACCESS_MODIFIER_PUBLIC, innerMixin, 216);
        private final CompiledCodeObject innerPrivateMethod = newMethod(CompiledCodeObject.ACCESS_MODIFIER_PRIVATE, innerMixin, 216);
        private final CompiledCodeObject inheritedMethod = newMethod(CompiledCodeObject.ACCESS_MODIFIER_PROTECTED, null, 216);

        private final ClassObject baseClass = newClass("Base", null, FORMAT_FIXED_POINTERS | 1);
        private final ClassObject outerClass = newClass("Outer", null, FORMAT_FIXED_POINTERS | 1);
        private final ClassObject innerClass = newClass("Inner", baseClass, FORMAT_FIXED_POINTERS | 1);

        private final PointersObject outerInstance;
        private final PointersObject innerInstance;

        private Hierarchy() {
            baseClass.setMethodDict(newMethodDictionary(new NativeObject[]{inheritedSelector}, new CompiledCodeObject[]{inheritedMethod}));
            outerClass.setMethodDict(newMethodDictionary(new NativeObject[]{outerSelector, privateSelector}, new CompiledCodeObject[]{outerMethod, outerPrivateMethod}));
            innerClass.setMethodDict(newMethodDictionary(new NativeObject[]{innerSelector, privateSelector}, new CompiledCodeObject[]{innerMethod, innerPrivateMethod}));
            outerInstance = new PointersObject(outerClass);
            innerInstance = new PointersObject(innerClass);
            outerClass.setOtherPointer(NEWSPEAK_CLASS.MIXIN, outerMixin);
            outerClass.setOtherPointer(NEWSPEAK_CLASS.ENCLOSING_OBJECT, NilObject.SINGLETON);
            innerClass.setOtherPointer(NEWSPEAK_CLASS.MIXIN, innerMixin);
            innerClass.setOtherPointer(NEWSPEAK_CLASS.ENCLOSING_OBJECT, outerInstance);
        }
    }

    private static NativeObject symbol(final String value) {
        final NativeObject symbol = NativeObject.newNativeBytes(image.getByteSymbolClass(), MiscUtils.stringToBytes(value));
        symbol.getOrCreateSqueakHash();
        return symbol;
    }

    private static ClassObject newClass(final String name, final ClassObject superclass, final long format) {
        final ClassObject metaclass = new ClassObject(image, image.metaClass, METACLASS.INST_SIZE);
        metaclass.setInstancesAreClasses();
        final ClassObject classObject = new ClassObject(image, metaclass, 7);
        classObject.setSuperclass(superclass);
        classObject.setMethodDict(null);
        classObject.setFormat(format);
        /* Slots 3 to 6: mixin, enclosing object, unused, and name. */
        classObject.setOtherPointers(new Object[]{NilObject.SINGLETON, NilObject.SINGLETON, NilObject.SINGLETON, symbol(name)});
        metaclass.setOtherPointer(METACLASS.THIS_CLASS, classObject);
        return classObject;
    }

    private static PointersObject newMixin(final Object enclosingMixin) {
        final PointersObject mixin = new PointersObject(mixinClass);
        mixin.instVarAtPut0Slow(NEWSPEAK_MIXIN.ENCLOSING_MIXIN, enclosingMixin);
        return mixin;
    }

    private static PointersObject binding(final Object value) {
        final PointersObject binding = new PointersObject(nilClassBinding.getSqueakClass());
        binding.instVarAtPut0Slow(0, symbol("methodClass"));
        binding.instVarAtPut0Slow(1, value);
        return binding;
    }

    private static VariablePointersObject newMethodDictionary(final NativeObject[] selectors, final CompiledCodeObject[] methods) {
        final int size = 8;
        assert selectors.length < size : "Method dictionaries must contain at least one nil";
        final VariablePointersObject methodDictionary = new VariablePointersObject(methodDictionaryClass, methodDictionaryClass.getLayout(), size);
        final Object[] values = new Object[size];
        java.util.Arrays.fill(values, NilObject.SINGLETON);
        for (int i = 0; i < size; i++) {
            methodDictionary.putIntoVariablePart(i, NilObject.SINGLETON);
        }
        for (int i = 0; i < selectors.length; i++) {
            int index = selectors[i].getSqueakHashInt() & size - 1;
            while (methodDictionary.getFromVariablePart(index) != NilObject.SINGLETON) {
                index = index + 1 & size - 1;
            }
            methodDictionary.putIntoVariablePart(index, selectors[i]);
            values[index] = methods[i];
        }
        methodDictionary.instVarAtPut0Slow(0, (long) selectors.length);
        methodDictionary.instVarAtPut0Slow(METHOD_DICT.VALUES, image.asArrayOfObjects(values));
        return methodDictionary;
    }

    private static long v4Header(final int accessModifier, final int numLiterals) {
        return CompiledCodeHeaderUtils.makeHeaderWord(false, 0, 4, numLiterals, false, false) | (long) accessModifier << 28;
    }

    /** Creates a NewsqueakV4 method without arguments whose method class association refers to mixin. */
    private static CompiledCodeObject newMethod(final int accessModifier, final Object mixinOrNull, final int... intbytes) {
        final byte[] bytes = new byte[intbytes.length + 1]; // flagByte = 0 for no method trailer.
        for (int i = 0; i < intbytes.length; i++) {
            bytes[i] = (byte) intbytes[i];
        }
        final Object methodClassAssociation = mixinOrNull == null ? NilObject.SINGLETON : binding(mixinOrNull);
        return new CompiledCodeObject(bytes, v4Header(accessModifier, 2), new Object[]{symbol("test"), methodClassAssociation}, image.compiledMethodClass);
    }

    private static Object runV4(final Object receiver, final int... intbytes) {
        return runMethod(newMethod(CompiledCodeObject.ACCESS_MODIFIER_PUBLIC, null, intbytes), receiver);
    }
}
