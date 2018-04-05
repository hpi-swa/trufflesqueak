package de.hpi.swa.trufflesqueak.model;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.utilities.CyclicAssumption;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.exceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.CLASS;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.METHOD_DICT;
import de.hpi.swa.trufflesqueak.util.SqueakImageChunk;

public class ClassObject extends AbstractPointersObject {
    @CompilationFinal private final Set<ClassObject> subclasses = new HashSet<>();
    @CompilationFinal private int instSpec = -1;
    @CompilationFinal private int instanceSize = -1;
    @CompilationFinal private final CyclicAssumption methodLookupStable = new CyclicAssumption("Class lookup stability");
    @CompilationFinal private final CyclicAssumption classFormatStable = new CyclicAssumption("Class format stability");
    @CompilationFinal private CompiledMethodObject doesNotUnderstandMethod;

    public ClassObject(SqueakImageContext img) {
        super(img);
    }

    private ClassObject(ClassObject original) {
        this(original.image, original.getSqClass(), original.pointers.clone());
        instSpec = original.instSpec;
        instanceSize = original.instanceSize;
    }

    private ClassObject(SqueakImageContext img, ClassObject sqClass, Object[] ptrs) {
        super(img, sqClass);
        pointers = ptrs;
    }

    private ClassObject(SqueakImageContext image, ClassObject classObject, int size) {
        super(image, classObject, size);
    }

    @Override
    public final boolean isClass() {
        assert image.metaclass == getSqClass() || image.metaclass == getSqClass().getSqClass();
        return true;
    }

    @Override
    public final String nameAsClass() {
        assert isClass();
        if (isAMetaclass()) {
            // metaclasses store their singleton instance in the last field
            Object classInstance = at0(getPointers().length - 1);
            if (classInstance instanceof ClassObject) {
                return "Metaclass (" + ((ClassObject) classInstance).getName() + ")";
            }
        } else {
            Object nameObj = getName();
            if (nameObj instanceof NativeObject) {
                return nameObj.toString();
            }
        }
        return "UnknownClass";
    }

    private final boolean isMetaclass() {
        return this == image.metaclass;
    }

    private final boolean isAMetaclass() {
        return this.getSqClass() == image.metaclass;
    }

    private final boolean instancesAreClasses() {
        return isMetaclass() || isAMetaclass();
    }

    @Override
    public final void fillin(SqueakImageChunk chunk) {
        super.fillin(chunk);
        // initialize the subclasses set
        setFormat((long) at0(CLASS.FORMAT));
        Object superclass = getSuperclass();
        setSuperclass(superclass != null ? superclass : image.nil);
    }

    public final void setFormat(long format) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        super.atput0(CLASS.FORMAT, format);
        if (instSpec >= 0) { // only invalidate if not initialized
            classFormatStable.invalidate();
        }
        instSpec = (int) ((format >> 16) & 0x1f);
        instanceSize = (int) (format & 0xffff);
    }

    public final void setSuperclass(Object superclass) {
        Object oldSuperclass = getSuperclass();
        super.atput0(CLASS.SUPERCLASS, superclass);
        if (oldSuperclass instanceof ClassObject) {
            ((ClassObject) oldSuperclass).detachSubclass(this);
        }
        if (superclass instanceof ClassObject) {
            ((ClassObject) superclass).attachSubclass(this);
        }
        for (ClassObject subclass : subclasses) {
            subclass.invalidateMethodLookup();
        }
    }

    private final void invalidateMethodLookup() {
        methodLookupStable.invalidate();
    }

    private final void attachSubclass(ClassObject classObject) {
        subclasses.add(classObject);
    }

    private final void detachSubclass(ClassObject classObject) {
        subclasses.remove(classObject);
    }

    public final Object getSuperclass() {
        return at0(CLASS.SUPERCLASS);
    }

    public final Object getMethodDict() {
        return at0(CLASS.METHOD_DICT);
    }

    public final Object getName() {
        return at0(CLASS.NAME);
    }

    @Override
    public final void atput0(long idx, Object obj) {
        if (idx == CLASS.FORMAT) {
            setFormat((long) obj);
        } else if (idx == CLASS.SUPERCLASS) {
            setSuperclass(obj);
        } else {
            super.atput0(idx, obj);
        }
    }

    public final Assumption getMethodLookupStable() {
        return methodLookupStable.getAssumption();
    }

    public final Assumption getClassFormatStable() {
        return classFormatStable.getAssumption();
    }

    // TODO: cache the methoddict in a better structure than what Squeak provides
    // ... or use the Squeak hash to decide where to put stuff
    private final Object lookup(Predicate<Object> predicate) {
        Object lookupClass = this;
        while (lookupClass instanceof ClassObject) {
            Object methodDict = ((ClassObject) lookupClass).getMethodDict();
            if (methodDict instanceof ListObject) {
                Object values = ((ListObject) methodDict).at0(METHOD_DICT.VALUES);
                if (values instanceof BaseSqueakObject) {
                    for (int i = METHOD_DICT.NAMES; i < ((BaseSqueakObject) methodDict).size(); i++) {
                        Object methodSelector = ((BaseSqueakObject) methodDict).at0(i);
                        if (predicate.test(methodSelector)) {
                            return ((BaseSqueakObject) values).at0(i - METHOD_DICT.NAMES);
                        }
                    }
                }
            }
            lookupClass = ((ClassObject) lookupClass).getSuperclass();
        }
        return getDoesNotUnderstandMethod();
    }

    private final CompiledMethodObject getDoesNotUnderstandMethod() {
        if (doesNotUnderstandMethod == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            doesNotUnderstandMethod = (CompiledMethodObject) lookup(image.doesNotUnderstand);
        }
        return doesNotUnderstandMethod;
    }

    public final Object lookup(NativeObject selector) {
        return lookup(methodSelector -> methodSelector == selector);
    }

    @TruffleBoundary
    public final Object lookup(String selector) {
        return lookup(methodSelector -> methodSelector != null && methodSelector.toString().equals(selector));
    }

    public final boolean isVariable() {
        return instSpec >= 2 && (instSpec <= 4 || instSpec >= 9);
    }

    public final Object newInstance() {
        return newInstance(0);
    }

    public final Object newInstance(long extraSize) {
        int size = instanceSize + ((int) extraSize);
        //@formatter:off
        switch (instSpec) {
            case 0: // empty objects
                return new EmptyObject(image, this);
            case 1:
                assert size == instanceSize;
                if (instancesAreClasses()) {
                    return new ClassObject(image, this, size);
                } else {
                    return new PointersObject(image, this, size);
                }
            case 2: // indexed pointers
                return new ListObject(image, this, size);
            case 3: // mixed indexable and named pointers
                if (this == image.methodContextClass) {
                    return ContextObject.create(image, size);
                } else if (this == image.blockClosureClass) {
                    return new BlockClosureObject(image); // TODO: verify this is actually used
                } else {
                    return new ListObject(image, this, size);
                }
            case 4:
                return new WeakPointersObject(image, this, size);
            case 5: // TODO: weak pointers
                return new PointersObject(image, this, size);
            case 7: case 8:
                throw new SqueakException("Tried to instantiate an immediate");
            case 9:
                return NativeObject.newNativeLongs(image, this, size);
            case 10: case 11:
                if (this == image.floatClass) {
                    assert size == 2;
                    return new FloatObject(image);
                } else {
                    return NativeObject.newNativeWords(image, this, size);
                }
            case 12: case 13: case 14: case 15:
                return NativeObject.newNativeShorts(image, this, size);
            case 16: case 17: case 18: case 19: case 20: case 21: case 22: case 23:
                if (this == image.largePositiveIntegerClass || this == image.largeNegativeIntegerClass) {
                    return new LargeIntegerObject(image, this, size);
                } else {
                    return NativeObject.newNativeBytes(image, this, size);
                }
            case 24: case 25: case 26: case 27: case 28: case 29: case 30: case 31:
                return new CompiledMethodObject(image, this, (int) extraSize);
            default:
                throw new SqueakException("Tried to instantiate with bogus instSpec: " + instSpec);
        }
        //@formatter:on
    }

    public final int getBasicInstanceSize() {
        return instanceSize;
    }

    @Override
    public final BaseSqueakObject shallowCopy() {
        return new ClassObject(this);
    }

    public final long classByteSizeOfInstance(long numElements) {
        int numWords = instanceSize;
        if (instSpec < 9) {                   // 32 bit
            numWords += numElements;
        } else if (instSpec >= 16) {          // 8 bit
            numWords += (numElements + 3) / 4 | 0;
        } else if (instSpec >= 12) {          // 16 bit
            numWords += (numElements + 1) / 2 | 0;
        } else if (instSpec >= 10) {          // 32 bit
            numWords += numElements;
        } else {                              // 64 bit
            numWords += numElements * 2;
        }
        numWords += numWords & 1;             // align to 64 bits
        numWords += numWords >= 255 ? 4 : 2;  // header words
        if (numWords < 4) {
            numWords = 4;                     // minimum object size
        }
        return numWords * 4;
    }
}
