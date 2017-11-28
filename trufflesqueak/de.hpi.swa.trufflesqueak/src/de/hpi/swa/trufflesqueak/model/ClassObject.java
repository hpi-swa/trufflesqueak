package de.hpi.swa.trufflesqueak.model;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.utilities.CyclicAssumption;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.util.Chunk;

public class ClassObject extends AbstractPointersObject {
    private static final int METHODDICT_NAMES_INDEX = 2;
    private static final int METHODDICT_VALUES_INDEX = 1;
    private static final int NAME_INDEX = 6;
    private static final int FORMAT_INDEX = 2;
    private static final int METHODDICT_INDEX = 1;
    private static final int SUPERCLASS_INDEX = 0;
    private final Set<ClassObject> subclasses = new HashSet<>();

    @CompilationFinal private int instSpec;
    @CompilationFinal private int instanceSize;
    private final CyclicAssumption methodLookupStable = new CyclicAssumption("Class lookup stability");
    private final CyclicAssumption classFormatStable = new CyclicAssumption("Class format stability");

    public ClassObject(SqueakImageContext img) {
        super(img);
    }

    private ClassObject(ClassObject original) {
        this(original.image, original.getSqClass(), original.pointers);
        instSpec = original.instSpec;
        instanceSize = original.instanceSize;
    }

    public ClassObject(SqueakImageContext img, ClassObject sqClass, Object[] ptrs) {
        super(img, sqClass);
        pointers = ptrs;
    }

    public ClassObject(SqueakImageContext image, ClassObject classObject, int size) {
        this(image, classObject, new Object[size]);
    }

    @Override
    public boolean isClass() {
        assert image.metaclass == getSqClass() || image.metaclass == getSqClass().getSqClass();
        return true;
    }

    @Override
    public String nameAsClass() {
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

    private boolean isMetaclass() {
        return this == image.metaclass;
    }

    private boolean isAMetaclass() {
        return this.getSqClass() == image.metaclass;
    }

    private boolean instancesAreClasses() {
        return isMetaclass() || isAMetaclass();
    }

    @Override
    public void fillin(Chunk chunk) {
        super.fillin(chunk);
        // initialize the subclasses set
        setFormat((int) at0(FORMAT_INDEX));
        setSuperclass(getSuperclass());
    }

    public void setFormat(int format) {
        super.atput0(FORMAT_INDEX, format);
        instSpec = (format >> 16) & 0x1f;
        instanceSize = format & 0xffff;
        classFormatStable.invalidate();
    }

    public void setSuperclass(Object superclass) {
        Object oldSuperclass = getSuperclass();
        super.atput0(SUPERCLASS_INDEX, superclass);
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

    private void invalidateMethodLookup() {
        methodLookupStable.invalidate();
    }

    private void attachSubclass(ClassObject classObject) {
        subclasses.add(classObject);
    }

    private void detachSubclass(ClassObject classObject) {
        subclasses.remove(classObject);
    }

    public Object getSuperclass() {
        return at0(SUPERCLASS_INDEX);
    }

    public Object getMethodDict() {
        return at0(METHODDICT_INDEX);
    }

    public Object getName() {
        return at0(NAME_INDEX);
    }

    @Override
    public void atput0(int idx, Object obj) {
        if (idx == FORMAT_INDEX) {
            setFormat((int) obj);
        } else if (idx == SUPERCLASS_INDEX) {
            setSuperclass(obj);
        } else {
            super.atput0(idx, obj);
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
    public Object lookup(Predicate<Object> test) {
        Object lookupClass = this;
        while (lookupClass instanceof ClassObject) {
            Object methodDict = ((ClassObject) lookupClass).getMethodDict();
            if (methodDict instanceof ListObject) {
                Object values = ((ListObject) methodDict).at0(METHODDICT_VALUES_INDEX);
                if (values instanceof BaseSqueakObject) {
                    for (int i = METHODDICT_NAMES_INDEX; i < ((BaseSqueakObject) methodDict).size(); i++) {
                        Object methodSelector = ((BaseSqueakObject) methodDict).at0(i);
                        if (test.test(methodSelector)) {
                            return ((BaseSqueakObject) values).at0(i - METHODDICT_NAMES_INDEX);
                        }
                    }
                }
            }
            lookupClass = ((ClassObject) lookupClass).getSuperclass();
        }
        return null;
    }

    public Object lookup(Object selector) {
        Object result = lookup(methodSelector -> methodSelector == selector);
        if (result == null) {
            return doesNotUnderstand();
        }
        return result;
    }

    public Object lookup(String selector) {
        return lookup(methodSelector -> methodSelector != null && methodSelector.toString().equals(selector));
    }

    public Object doesNotUnderstand() {
        Object result = lookup(methodSelector -> methodSelector == image.doesNotUnderstand);
        if (result == null) {
            throw new RuntimeException("doesNotUnderstand missing!");
        }
        return result;
    }

    public boolean isVariable() {
        return instSpec >= 2 && (instSpec <= 4 || instSpec >= 9);
    }

    public BaseSqueakObject newInstance() {
        return newInstance(instanceSize);
    }

    public BaseSqueakObject newInstance(int size) {
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
                return new PointersObject(image, this, size);
            case 4: case 5: // TODO: weak pointers
                return new PointersObject(image, this, size);
            case 7: case 8:
                throw new RuntimeException("tried to instantiate invalid class");
            case 9: case 10: case 11: case 12: case 13: case 14: case 15:
                // TODO: Float
                return new NativeObject(image, this, size, 4);
            case 16: case 17: case 18: case 19: case 20: case 21: case 22: case 23:
                return new NativeObject(image, this, size, 1);
            default:
                // FIXME: ignore the size?
                return new CompiledMethodObject(image, this);
        }
        //@formatter:on
    }

    public int getBasicInstanceSize() {
        return instanceSize;
    }

    @Override
    public BaseSqueakObject shallowCopy() {
        return new ClassObject(this);
    }
}
