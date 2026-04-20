/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import org.graalvm.collections.UnmodifiableEconomicMap;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.utilities.CyclicAssumption;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.image.SqueakImageChunk;
import de.hpi.swa.trufflesqueak.image.SqueakImageConstants;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.image.SqueakImageReader;
import de.hpi.swa.trufflesqueak.image.SqueakImageWriter;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayout;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.CLASS;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.CLASS_DESCRIPTION;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.CLASS_TRAIT;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.METACLASS;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.METHOD_DICT;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.ObjectGraphUtils.ObjectTracer;

/*
 * Represents all subclasses of ClassDescription (Class, Metaclass, TraitBehavior, ...).
 */
@SuppressWarnings("static-method")
public final class ClassObject extends AbstractSqueakObjectWithClassAndHash {
    public enum FallbackConvention {
        CANNOT_INTERPRET, // Use #cannotInterpret: and build nested Message objects
        STANDARD_DNU,     // Use #doesNotUnderstand: and build a Message object
        SHORTCUT_DNU      // Use #dnu...: and pass arguments and selector directly (no Message
                          // object)
    }

    public record DispatchFailureResult(
                    CompiledCodeObject fallbackMethod,
                    int fallbackDepth,
                    FallbackConvention convention,
                    NativeObject fallbackSelector) {
    }

    @CompilationFinal private CyclicAssumption classHierarchyAndMethodDictStable;
    @CompilationFinal private CyclicAssumption classFormatStable;

    private final SqueakImageContext image;
    @CompilationFinal private boolean instancesAreClasses;

    private ClassObject superclass;
    @CompilationFinal private VariablePointersObject methodDict;
    @CompilationFinal private long format = -1;
    private Object[] pointers;

    @CompilationFinal private ObjectLayout layout;

    public ClassObject(final SqueakImageContext image) {
        super();
        this.image = image;
    }

    public ClassObject(final SqueakImageChunk chunk) {
        super(chunk);
        this.image = chunk.getImage();
    }

    public ClassObject(final ClassObject original) {
        super((AbstractSqueakObjectWithClassAndHash) original);
        image = original.image;
        instancesAreClasses = original.instancesAreClasses;
        superclass = original.superclass;
        assert superclass == null || superclass.assertNotForwarded();
        methodDict = original.methodDict != null ? new VariablePointersObject(original.methodDict) : null;
        format = original.format;
        // FIXME: should clone the pointers themselves, too
        pointers = original.pointers.clone();
        initializeLayout();
    }

    public ClassObject(final SqueakImageContext image, final ClassObject classObject, final int size) {
        super(classObject);
        this.image = image;
        pointers = ArrayUtils.withAll(Math.max(size - CLASS_DESCRIPTION.INLINE_POINTERS, 0), NilObject.SINGLETON);
        instancesAreClasses = image.isMetaClass(classObject);
        // `size - CLASS_DESCRIPTION.SIZE` is negative when instantiating "Behavior".
    }

    public SqueakImageContext getImage() {
        return image;
    }

    /* Used by TruffleSqueakTest. */
    public boolean hasLayout() {
        return layout != null;
    }

    public ObjectLayout getLayout() {
        assert layout != null : this + " has layout of null";
        return layout;
    }

    private void initializeLayout() {
        layout = new ObjectLayout(getBasicInstanceSize());
    }

    public void updateLayout(final ObjectLayout newLayout) {
        assert layout == null || !layout.isValid() : "Old layout not invalidated";
        layout = newLayout;
    }

    @Override
    public String toString() {
        return getClassName();
    }

    @TruffleBoundary
    public String getClassName() {
        if (!isNotForwarded()) {
            return "forward to " + getForwardingPointer().toString();
        }
        if (image.isMetaClass(getSqueakClass())) {
            final Object classInstance = pointers[METACLASS.THIS_CLASS - CLASS_DESCRIPTION.INLINE_POINTERS];
            if (classInstance != NilObject.SINGLETON && ((ClassObject) classInstance).pointers[CLASS.NAME] instanceof final NativeObject metaClassName) {
                return metaClassName.asStringUnsafe() + " class";
            } else {
                return "Unknown metaclass";
            }
        } else if (isAClassTrait()) {
            final ClassObject traitInstance = (ClassObject) pointers[CLASS_TRAIT.BASE_TRAIT - CLASS_DESCRIPTION.INLINE_POINTERS];
            if (traitInstance.pointers[CLASS.NAME] instanceof final NativeObject traitClassName) {
                return traitClassName.asStringUnsafe() + " classTrait";
            } else {
                return "Unknown classTrait";
            }
        } else if (size() >= CLASS.NAME && pointers[CLASS.NAME] instanceof final NativeObject className) {
            // this also works for traits, since TRAIT.NAME == CLASS.NAME
            return className.asStringUnsafe();
        } else {
            return "Unknown behavior";
        }
    }

    private boolean isAClassTrait() {
        if (pointers == null || pointers.length <= CLASS_TRAIT.BASE_TRAIT - CLASS_DESCRIPTION.SIZE) {
            return false;
        }
        final Object traitInstance = pointers[CLASS_TRAIT.BASE_TRAIT - CLASS_DESCRIPTION.SIZE];
        return traitInstance instanceof final ClassObject t && this != t.getSqueakClass() && t.getSqueakClass().getClassName().equals("Trait");
    }

    public boolean isBits() {
        return getInstanceSpecification() >= 7;
    }

    public boolean isBytes() {
        return getInstanceSpecification() >= 16;
    }

    public boolean isCompiledMethodClassType() {
        return getInstanceSpecification() >= 24;
    }

    public boolean isEphemeronClassType() {
        return getInstanceSpecification() == 5;
    }

    public boolean isImmediateClassType() {
        return getInstanceSpecification() == 7;
    }

    public boolean isIndexableWithInstVars() {
        return getInstanceSpecification() == 3;
    }

    public boolean isIndexableWithNoInstVars() {
        return getInstanceSpecification() == 2;
    }

    public boolean isNonIndexableWithInstVars() {
        return getInstanceSpecification() == 1;
    }

    public boolean isLongs() {
        return getInstanceSpecification() == 9;
    }

    public boolean isShorts() {
        return getInstanceSpecification() == 12;
    }

    public boolean isVariable() {
        final int instSpec = getInstanceSpecification();
        return instSpec >= 2 && (instSpec <= 4 || instSpec >= 9);
    }

    public boolean isWeak() {
        return getInstanceSpecification() == 4;
    }

    public boolean isWords() {
        return getInstanceSpecification() == 10;
    }

    public boolean isZeroSized() {
        return getInstanceSpecification() == 0;
    }

    public void setInstancesAreClasses() {
        if (!instancesAreClasses) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            instancesAreClasses = true;
        }
    }

    public boolean instancesAreClasses() {
        return instancesAreClasses;
    }

    public boolean isBlockClosureClass() {
        CompilerAsserts.neverPartOfCompilation();
        return this == image.blockClosureClass;
    }

    public boolean isCompiledMethodClass() {
        CompilerAsserts.neverPartOfCompilation();
        return this == image.compiledMethodClass;
    }

    private boolean includesBehavior(final ClassObject squeakClass) {
        ClassObject current = this;
        while (current != null) {
            if (current == squeakClass) {
                return true;
            }
            current = current.getSuperclassOrNull();
        }
        return false;
    }

    public boolean includesExternalFunctionBehavior(final SqueakImageContext theImage) {
        return includesBehavior(theImage.getExternalFunctionClassOrNull());
    }

    /**
     * {@link ClassObject}s are filled in at an earlier stage in
     * {@link SqueakImageReader#fillInClassObjects}.
     */
    @Override
    public void fillin(final SqueakImageChunk chunk) {
        if (methodDict == null) {
            if (needsSqueakHash() && chunk.getHash() != 0) {
                setSqueakHash(chunk.getHash());
            }
            superclass = (ClassObject) NilObject.nilToNull(chunk.getPointer(CLASS_DESCRIPTION.SUPERCLASS));
            methodDict = (VariablePointersObject) NilObject.nilToNull(chunk.getPointer(CLASS_DESCRIPTION.METHOD_DICT));
            format = (long) chunk.getPointer(CLASS_DESCRIPTION.FORMAT);
            pointers = chunk.getPointers(CLASS_DESCRIPTION.INLINE_POINTERS);
            initializeLayout();
        }
    }

    /* see SpurMemoryManager>>#ensureBehaviorHash: */
    public void ensureBehaviorHash() {
        if (getSqueakHashInt() == SqueakImageConstants.FREE_OBJECT_CLASS_INDEX_PUN) {
            CompilerDirectives.transferToInterpreter();
            /* This happens only once for a class and should thus happen on the slow path. */
            image.enterIntoClassTable(this);
        }
    }

    public ClassObject withEnsuredBehaviorHash() {
        CompilerAsserts.neverPartOfCompilation();
        ensureBehaviorHash();
        return this;
    }

    public void setFormat(final long format) {
        invalidateClassFormatStableAssumption("new format");
        final int oldBasicInstanceSize = getBasicInstanceSize();
        this.format = format;
        if (oldBasicInstanceSize != getBasicInstanceSize()) {
            initializeLayout();
        }
    }

    @Override
    public int instsize() {
        return METACLASS.INST_SIZE;
    }

    @Override
    public int size() {
        return CLASS_DESCRIPTION.INLINE_POINTERS + pointers.length;
    }

    public static boolean isSuperclassIndex(final long index) {
        return index == CLASS_DESCRIPTION.SUPERCLASS;
    }

    public static boolean isMethodDictIndex(final long index) {
        return index == CLASS_DESCRIPTION.METHOD_DICT;
    }

    public static boolean isFormatIndex(final long index) {
        return index == CLASS_DESCRIPTION.FORMAT;
    }

    public static boolean isOtherIndex(final long index) {
        return index >= CLASS_DESCRIPTION.INLINE_POINTERS;
    }

    public AbstractSqueakObject getSuperclass() {
        return NilObject.nullToNil(superclass);
    }

    public ClassObject getSuperclassOrNull() {
        assert assertNotForwarded() && (superclass == null || superclass.assertNotForwarded());
        return superclass;
    }

    public ClassObject getResolvedSuperclass() {
        assert assertNotForwarded();
        if (!superclass.isNotForwarded()) {
            CompilerDirectives.transferToInterpreter();
            setSuperclass((ClassObject) superclass.getForwardingPointer());
        }
        return getSuperclassOrNull();
    }

    public AbstractSqueakObject getMethodDictOrNil() {
        assert assertNotForwarded();
        return NilObject.nullToNil(methodDict);
    }

    public VariablePointersObject getMethodDict() {
        assert assertNotForwarded() && (methodDict == null || methodDict.assertNotForwarded());
        return methodDict;
    }

    private VariablePointersObject getResolvedMethodDict() {
        assert assertNotForwarded();
        if (methodDict == null) {
            return methodDict;
        }
        if (!methodDict.isNotForwarded()) {
            CompilerDirectives.transferToInterpreter();
            setMethodDict((VariablePointersObject) methodDict.getForwardingPointer());
        }
        return getMethodDict();
    }

    public Object getOtherPointer(final int index) {
        return pointers[index - CLASS_DESCRIPTION.INLINE_POINTERS];
    }

    public void setOtherPointer(final int index, final Object value) {
        pointers[index - CLASS_DESCRIPTION.INLINE_POINTERS] = value;
    }

    public Object[] getOtherPointers() {
        return pointers;
    }

    public void setOtherPointers(final Object[] pointers) {
        this.pointers = pointers;
    }

    public long getFormat() {
        return format;
    }

    public void setSuperclass(final ClassObject superclass) {
        assert superclass == null || superclass.assertNotForwarded();
        invalidateClassHierarchyAndMethodDictStableAssumption("new superclass");
        this.superclass = superclass;
        /*
         * TODO: Instead of a full global flush, this should be refined to only flush entries in
         * image.methodCache where the entry's class is `this` class or a subclass of `this` class.
         */
        image.flushMethodCache();
    }

    public void setMethodDict(final VariablePointersObject methodDict) {
        assert methodDict == null || methodDict.assertNotForwarded();
        invalidateClassHierarchyAndMethodDictStableAssumption("new method dict");
        this.methodDict = methodDict;
        /*
         * TODO: Instead of a full global flush, this should be refined to only flush entries in
         * image.methodCache where the entry's class is `this` class or a subclass of `this` class.
         */
        image.flushMethodCache();
    }

    /**
     * Performs a full method lookup up the class hierarchy. See Interpreter#lookupMethodInClass:.
     * <p>
     * This method handles the core Smalltalk lookup semantics. It can return:
     * <ul>
     * <li>A {@link CompiledCodeObject} for a standard method.</li>
     * <li>An arbitrary {@link Object} for Object-As-Method (run:with:in:) semantics.</li>
     * <li>{@code null} if a structural dispatch failure occurs (either the selector is not found,
     * or a nil method dictionary is encountered).</li>
     * </ul>
     * <p>
     * Callers encountering a {@code null} result should use {@link #resolveDispatchFailure} to
     * determine the appropriate fallback method (e.g., #doesNotUnderstand: or #cannotInterpret:).
     *
     * @param selector The message selector to look up.
     * @return The result of the lookup (method or object), or {@code null} if dispatch fails.
     */

    @TruffleBoundary
    public Object lookupInMethodDictSlow(final NativeObject selector) {
        final int selectorHash = selector.getSqueakHashInt();
        ClassObject lookupClass = this;
        while (lookupClass != null) {
            assert lookupClass.assertNotForwarded();
            if (lookupClass.methodDict == null) {
                /*
                 * "MethodDict pointer is nil (hopefully due a swapped out stub) -- raise exception
                 * #cannotInterpret:."
                 */
                return null;
            }
            final Object result = lookupMethodInDictionary(lookupClass.getResolvedMethodDict(), selector, selectorHash);
            if (result != null) {
                return result;
            }
            if (lookupClass.getSuperclassOrNull() == null) {
                break;
            }
            lookupClass = lookupClass.getResolvedSuperclass();
        }
        assert !selector.isDoesNotUnderstand(image) : "Could not find does not understand method";
        return null; /* Signals a doesNotUnderstand. */
    }

    /** See Interpreter#lookupMethodInDictionary:. */
    private static Object lookupMethodInDictionary(final VariablePointersObject methodDictionary, final NativeObject messageSelector, final int messageSelectorHash) {
        final Object[] methodDictSelectors = methodDictionary.getVariablePart();
        /* MethodDictionary always has a power-of-two size */
        assert methodDictSelectors.length > 0 && (methodDictSelectors.length & (methodDictSelectors.length - 1)) == 0;
        final int sizeMask = methodDictSelectors.length - 1;
        int index = messageSelectorHash & sizeMask;
        /*
         * "It is assumed that there are some nils in this dictionary, and search will stop when one
         * is encountered."
         */
        for (int j = 0; j <= sizeMask; j++) {
            final Object nextSelector = methodDictSelectors[index];
            if (nextSelector == NilObject.SINGLETON) {
                return null;
            } else if (nextSelector == messageSelector) {
                final Object[] methodDictValues = AbstractPointersObjectReadNode.getUncached().executeArray(methodDictionary, METHOD_DICT.VALUES).getObjectStorage();
                final Object method = methodDictValues[index];
                if (method instanceof final AbstractSqueakObjectWithClassAndHash o && !o.isNotForwarded()) {
                    return methodDictValues[index] = o.getForwardingPointer();
                }
                return method;
            }
            index = ++index & sizeMask;
        }
        return null;
    }

    /**
     * Resolves the fallback method for a failed dispatch (either DNU or CI).
     *
     * @param originalSelector The selector that failed (used for assertions and error context).
     * @return The CompiledCodeObject for the fallback method.
     * @throws SqueakException if the fallback method cannot be found (fatal VM error).
     */
    @TruffleBoundary
    public DispatchFailureResult resolveDispatchFailure(final NativeObject originalSelector, final int arity) {
        CompilerAsserts.neverPartOfCompilation();

        // Walk superclass chain to determine if this is a #cannotInterpret: case.
        ClassObject current = this;
        while (current != null) {
            assert current.assertNotForwarded();
            if (current.methodDict == null) {
                break;
            }
            current = current.getSuperclassOrNull();
        }

        // --- DNU Path (No nil method dictionaries) ---
        if (current == null) {
            assert originalSelector != image.doesNotUnderstand : "Fatal dispatch error: Recursive doesNotUnderstand:";

            // Attempt the Shortcut DNU first
            final NativeObject shortcutSelector = image.getDNUShortcutSelector(arity);
            if (shortcutSelector != null) {
                final CompiledCodeObject shortcutMethod = lookupMethodInMethodDictSlow(shortcutSelector);
                if (shortcutMethod != null) {
                    return new DispatchFailureResult(shortcutMethod, 1, FallbackConvention.SHORTCUT_DNU, shortcutSelector);
                }
            }

            // Fall back to standard #doesNotUnderstand:
            final CompiledCodeObject dnuMethod = lookupMethodInMethodDictSlow(image.doesNotUnderstand);
            if (dnuMethod != null) {
                return new DispatchFailureResult(dnuMethod, 1, FallbackConvention.STANDARD_DNU, image.doesNotUnderstand);
            } else {
                throw SqueakException.create("Fatal dispatch error: #doesNotUnderstand: not found in", this);
            }
        }

        // --- CannotInterpret Path (Hit a nil dictionary) ---
        int depth = 1;
        ClassObject searchClass = current.getSuperclassOrNull();
        final NativeObject selector = image.cannotInterpretSelector;
        final int messageSelectorHash = selector.getSqueakHashInt();

        while (searchClass != null) {
            if (searchClass.methodDict == null) {
                depth++;
            } else {
                final Object result = lookupMethodInDictionary(searchClass.getResolvedMethodDict(), selector, messageSelectorHash);
                if (result instanceof CompiledCodeObject ciMethod) {
                    return new DispatchFailureResult(ciMethod, depth, FallbackConvention.CANNOT_INTERPRET, selector);
                }
            }
            searchClass = searchClass.getSuperclassOrNull();
        }

        throw SqueakException.create("Fatal dispatch error: #cannotInterpret: not found in superclass chain of", this);
    }

    /**
     * A strict convenience wrapper around {@link #lookupInMethodDictSlow(NativeObject)} that only
     * returns compiled methods.
     * <p>
     * This acts as a safe filter. It returns {@code null} if the underlying lookup results in a
     * structural failure (such as #doesNotUnderstand: or #cannotInterpret:) OR if the lookup
     * resolves to a non-method object (Object-As-Method).
     *
     * @param selector The message selector to look up.
     * @return The compiled method, or {@code null} if not found or not a method.
     */
    public CompiledCodeObject lookupMethodInMethodDictSlow(final NativeObject selector) {
        final Object result = lookupInMethodDictSlow(selector);
        if (result instanceof CompiledCodeObject compiledCode) {
            return compiledCode;
        }
        return null;
    }

    public void flushCachesForSelector(final NativeObject selector) {
        final VariablePointersObject methodDictionary = getResolvedMethodDict();
        if (methodDictionary == null) {
            return;
        }
        final Object[] methodDictVariablePart = methodDictionary.getVariablePart();
        for (int i = 0; i < methodDictVariablePart.length; i++) {
            if (selector == methodDictVariablePart[i]) {
                final Object lookupResult = AbstractPointersObjectReadNode.getUncached().executeArray(methodDictionary, METHOD_DICT.VALUES).getObjectStorage()[i];
                if (lookupResult instanceof final CompiledCodeObject codeObject) {
                    codeObject.flushCacheBySelector();
                }
                return;
            }
        }
    }

    public int getBasicInstanceSize() {
        return (int) (format & 0xffff);
    }

    public int getInstanceSpecification() {
        return (int) (format >> 16 & 0x1f);
    }

    public boolean pointsTo(final Object thang) {
        if (superclass == thang) {
            return true;
        }
        if (methodDict == thang) {
            return true;
        }
        if (thang instanceof final Long l && format == l) {
            return true;
        }
        return ArrayUtils.contains(pointers, thang);
    }

    public void become(final ClassObject other) {
        becomeOtherClass(other);

        final boolean otherInstancesAreClasses = image.isMetaClass(other.getSqueakClass());
        if (instancesAreClasses != otherInstancesAreClasses) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            instancesAreClasses = otherInstancesAreClasses;
        }

        final ClassObject otherSuperclass = other.superclass;
        final VariablePointersObject otherMethodDict = other.methodDict;
        final long otherFormat = other.format;
        final Object[] otherPointers = other.pointers;

        other.setSuperclass(superclass);
        other.setMethodDict(methodDict);
        other.setFormat(format);
        other.setOtherPointers(pointers);

        setSuperclass(otherSuperclass);
        setMethodDict(otherMethodDict);
        setFormat(otherFormat);
        setOtherPointers(otherPointers);
    }

    private CyclicAssumption classHierarchyAndMethodDictStable() {
        if (classHierarchyAndMethodDictStable == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            classHierarchyAndMethodDictStable = new CyclicAssumption("Class hierarchy stability");
        }
        return classHierarchyAndMethodDictStable;
    }

    public Assumption getClassHierarchyAndMethodDictStable() {
        return classHierarchyAndMethodDictStable().getAssumption();
    }

    private void invalidateClassHierarchyAndMethodDictStableAssumption(final String reason) {
        if (classHierarchyAndMethodDictStable != null) {
            classHierarchyAndMethodDictStable.invalidate(reason);
        }
    }

    private CyclicAssumption classFormatStable() {
        if (classFormatStable == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            classFormatStable = new CyclicAssumption("Class format stability");
        }
        return classFormatStable;
    }

    private void invalidateClassFormatStableAssumption(final String reason) {
        if (classFormatStable != null) {
            classFormatStable.invalidate(reason);
        }
    }

    public Assumption getClassFormatStable() {
        return classFormatStable().getAssumption();
    }

    @Override
    public void forwardTo(final AbstractSqueakObjectWithClassAndHash pointer) {
        super.forwardTo(pointer);
        invalidateClassHierarchyAndMethodDictStableAssumption("forwarded");
        invalidateClassFormatStableAssumption("forwarded");
    }

    @Override
    public void pointersBecomeOneWay(final UnmodifiableEconomicMap<Object, Object> fromToMap) {
        super.pointersBecomeOneWay(fromToMap);
        if (superclass != null) {
            final Object replacement = fromToMap.get(superclass);
            if (replacement != null) {
                setSuperclass((ClassObject) replacement);
            }
            assert superclass.assertNotForwarded();
        }
        if (methodDict != null) {
            final Object replacement = fromToMap.get(methodDict);
            if (replacement != null) {
                assert replacement != methodDict;
                setMethodDict((VariablePointersObject) replacement);
            }
            assert methodDict.assertNotForwarded();
        }
        ArrayUtils.replaceAll(pointers, fromToMap);
    }

    @Override
    public void tracePointers(final ObjectTracer tracer) {
        super.tracePointers(tracer);
        tracer.addIfUnmarked(superclass);
        tracer.addIfUnmarked(methodDict);
        tracer.addAllIfUnmarked(pointers);
    }

    @Override
    public void trace(final SqueakImageWriter writer) {
        super.trace(writer);
        writer.traceIfNecessary(superclass);
        writer.traceIfNecessary(methodDict);
        writer.traceAllIfNecessary(pointers);
    }

    @Override
    public void write(final SqueakImageWriter writer) {
        if (!writeHeader(writer)) {
            throw SqueakException.create("ClassObject must have slots:", this);
        }
        writer.writeObject(getSuperclass());
        writer.writeObject(getMethodDictOrNil());
        writer.writeSmallInteger(format);
        writer.writeObjects(getOtherPointers());
    }
}
