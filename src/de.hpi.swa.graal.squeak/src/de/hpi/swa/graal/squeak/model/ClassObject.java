package de.hpi.swa.graal.squeak.model;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.utilities.CyclicAssumption;

import de.hpi.swa.graal.squeak.image.SqueakImageChunk;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.CLASS;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.METHOD_DICT;
import de.hpi.swa.graal.squeak.util.ArrayUtils;

public final class ClassObject extends AbstractPointersObject {
    private final Set<ClassObject> subclasses = new HashSet<>();
    private final CyclicAssumption methodLookupStable = new CyclicAssumption("Class lookup stability");
    private final CyclicAssumption classFormatStable = new CyclicAssumption("Class format stability");

    @CompilationFinal private int instSpec = -1;
    @CompilationFinal private int instanceSize = -1;

    public ClassObject(final SqueakImageContext img) {
        super(img, -1L, null);
    }

    public ClassObject(final SqueakImageContext img, final long hash) {
        super(img, hash, null);
    }

    private ClassObject(final ClassObject original) {
        this(original.image, original.getSqClass(), original.pointers);
        instSpec = original.instSpec;
        instanceSize = original.instanceSize;
    }

    private ClassObject(final SqueakImageContext img, final ClassObject sqClass, final Object[] ptrs) {
        super(img, sqClass);
        pointers = ptrs;
    }

    public ClassObject(final SqueakImageContext image, final ClassObject classObject, final int size) {
        this(image, classObject, ArrayUtils.withAll(size, image.nil));
    }

    @Override
    public String nameAsClass() {
        assert isClass();
        if (isAMetaclass()) {
            // metaclasses store their singleton instance in the last field
            final Object classInstance = at0(size() - 1);
            if (classInstance instanceof ClassObject) {
                final NativeObject name = (NativeObject) ((ClassObject) classInstance).getName();
                return "Metaclass (" + name.asString() + ")";
            }
        } else {
            final Object nameObj = getName();
            if (nameObj instanceof NativeObject) {
                return ((NativeObject) nameObj).asString();
            }
        }
        return "UnknownClass";
    }

    private boolean isMetaclass() {
        return this == image.metaclass;
    }

    private boolean isAMetaclass() {
        return this.getSqClass() == image.metaclass;
    }

    public boolean instancesAreClasses() {
        return isMetaclass() || isAMetaclass();
    }

    public void fillin(final SqueakImageChunk chunk) {
        pointers = chunk.getPointers();
        // initialize the subclasses set
        setFormat((long) at0(CLASS.FORMAT));
        final Object superclass = getSuperclass();
        setSuperclass(superclass != null ? superclass : image.nil);
    }

    public void setFormat(final long format) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        pointers[CLASS.FORMAT] = format;
        if (instSpec >= 0) { // only invalidate if not initialized
            classFormatStable.invalidate();
        }
        instSpec = (int) ((format >> 16) & 0x1f);
        instanceSize = (int) (format & 0xffff);
    }

    @TruffleBoundary
    public void setSuperclass(final Object superclass) {
        final Object oldSuperclass = getSuperclass();
        pointers[CLASS.SUPERCLASS] = superclass;
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

    public int instsize() {
        return getSqClass().getBasicInstanceSize();
    }

    private void invalidateMethodLookup() {
        methodLookupStable.invalidate();
    }

    @TruffleBoundary
    private void attachSubclass(final ClassObject classObject) {
        subclasses.add(classObject);
    }

    @TruffleBoundary
    private void detachSubclass(final ClassObject classObject) {
        subclasses.remove(classObject);
    }

    public Object getSuperclass() {
        return at0(CLASS.SUPERCLASS);
    }

    public Object getMethodDict() {
        return at0(CLASS.METHOD_DICT);
    }

    public Object getName() {
        return at0(CLASS.NAME);
    }

    public Object at0(final long index) {
        return pointers[(int) index];
    }

    public void atput0(final long index, final Object obj) {
        if (index == CLASS.FORMAT) {
            setFormat((long) obj);
        } else if (index == CLASS.SUPERCLASS) {
            setSuperclass(obj);
        } else {
            pointers[(int) index] = obj;
        }
    }

    public Assumption getMethodLookupStable() {
        return methodLookupStable.getAssumption();
    }

    public Assumption getClassFormatStable() {
        return classFormatStable.getAssumption();
    }

    @TruffleBoundary
    public Object lookup(final String selector) {
        return lookup(methodSelector -> methodSelector != null && methodSelector.toString().equals(selector));
    }

    // TODO: cache the methoddict in a better structure than what Squeak provides
    // ... or use the Squeak hash to decide where to put stuff
    private Object lookup(final Predicate<Object> predicate) {
        CompilerAsserts.neverPartOfCompilation("This is only for finding the active context on startup, use LookupNode instead.");
        Object lookupClass = this;
        while (lookupClass instanceof ClassObject) {
            final Object methodDict = ((ClassObject) lookupClass).getMethodDict();
            if (methodDict instanceof PointersObject) {
                final Object values = ((PointersObject) methodDict).at0(METHOD_DICT.VALUES);
                if (values instanceof PointersObject) {
                    for (int i = METHOD_DICT.NAMES; i < ((PointersObject) methodDict).size(); i++) {
                        final Object methodSelector = ((PointersObject) methodDict).at0(i);
                        if (predicate.test(methodSelector)) {
                            return ((PointersObject) values).at0(i - METHOD_DICT.NAMES);
                        }
                    }
                }
            }
            lookupClass = ((ClassObject) lookupClass).getSuperclass();
        }
        return lookup(methodSelector -> methodSelector == image.doesNotUnderstand);
    }

    public boolean isVariable() {
        return instSpec >= 2 && (instSpec <= 4 || instSpec >= 9);
    }

    public int getBasicInstanceSize() {
        return instanceSize;
    }

    public int getInstanceSpecification() {
        return instSpec;
    }

    public AbstractSqueakObject shallowCopy() {
        return new ClassObject(this);
    }

    public long classByteSizeOfInstance(final long numElements) {
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

    public void become(final ClassObject other) {
        becomeOtherClass(other);
        final Object[] otherPointers = other.pointers;
        other.pointers = this.pointers;
        pointers = otherPointers;
    }
}
