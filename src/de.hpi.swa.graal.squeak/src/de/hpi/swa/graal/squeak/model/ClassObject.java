package de.hpi.swa.graal.squeak.model;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.utilities.CyclicAssumption;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions;
import de.hpi.swa.graal.squeak.exceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.AbstractImageChunk;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.CLASS;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.METHOD_DICT;
import de.hpi.swa.graal.squeak.util.ArrayUtils;

public final class ClassObject extends AbstractSqueakObject {
    @CompilationFinal private final Set<ClassObject> subclasses = new HashSet<>();
    @CompilationFinal private int instSpec = -1;
    @CompilationFinal private int instanceSize = -1;
    @CompilationFinal private final CyclicAssumption methodLookupStable = new CyclicAssumption("Class lookup stability");
    @CompilationFinal private final CyclicAssumption classFormatStable = new CyclicAssumption("Class format stability");
    @CompilationFinal private CompiledMethodObject doesNotUnderstandMethod;
    protected Object[] pointers;

    public ClassObject(final SqueakImageContext img) {
        super(img);
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

    private ClassObject(final SqueakImageContext image, final ClassObject classObject, final int size) {
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

    private boolean instancesAreClasses() {
        return isMetaclass() || isAMetaclass();
    }

    public void fillin(final AbstractImageChunk chunk) {
        super.fillinHashAndClass(chunk);
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

    public int size() {
        return pointers.length;
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

    // TODO: cache the methoddict in a better structure than what Squeak provides
    // ... or use the Squeak hash to decide where to put stuff
    private Object lookup(final Predicate<Object> predicate) {
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
        return getDoesNotUnderstandMethod();
    }

    private CompiledMethodObject getDoesNotUnderstandMethod() {
        if (doesNotUnderstandMethod == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            doesNotUnderstandMethod = (CompiledMethodObject) lookup(image.doesNotUnderstand);
        }
        return doesNotUnderstandMethod;
    }

    public Object lookup(final NativeObject selector) {
        return lookup(methodSelector -> methodSelector == selector);
    }

    @TruffleBoundary
    public Object lookup(final String selector) {
        return lookup(methodSelector -> methodSelector != null && methodSelector.toString().equals(selector));
    }

    public boolean isVariable() {
        return instSpec >= 2 && (instSpec <= 4 || instSpec >= 9);
    }

    public Object newInstance() {
        return newInstance(0);
    }

    public Object newInstance(final long extraSize) {
        final int size = instanceSize + ((int) extraSize);
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
                return new PointersObject(image, this, size);
            case 3: // mixed indexable and named pointers
                if (this == image.methodContextClass) {
                    return ContextObject.create(image, size);
                } else if (this == image.blockClosureClass) {
                    return new BlockClosureObject(image); // TODO: verify this is actually used
                } else {
                    return new PointersObject(image, this, size);
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
                    return NativeObject.newNativeInts(image, this, size);
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

    public int getBasicInstanceSize() {
        return instanceSize;
    }

    public Object[] getPointers() {
        return pointers;
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

    @Override
    public boolean become(final AbstractSqueakObject other) {
        if (!(other instanceof ClassObject)) {
            throw new PrimitiveExceptions.PrimitiveFailed();
        }
        if (!super.become(other)) {
            throw new SqueakException("Should not fail");
        }
        final Object[] pointers2 = ((ClassObject) other).pointers;
        ((ClassObject) other).pointers = this.pointers;
        pointers = pointers2;
        return true;
    }

    @Override
    public void pointersBecomeOneWay(final Object[] from, final Object[] to, final boolean copyHash) {
        // TODO: super.pointersBecomeOneWay(from, to); ?
        for (int i = 0; i < from.length; i++) {
            final Object fromPointer = from[i];
            for (int j = 0; j < size(); j++) {
                final Object newPointer = at0(j);
                if (newPointer == fromPointer) {
                    final Object toPointer = to[i];
                    atput0(j, toPointer);
                    if (copyHash && fromPointer instanceof AbstractSqueakObject && toPointer instanceof AbstractSqueakObject) {
                        ((AbstractSqueakObject) toPointer).setSqueakHash(((AbstractSqueakObject) fromPointer).squeakHash());
                    }
                }
            }
        }
    }

}
