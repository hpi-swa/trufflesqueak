package de.hpi.swa.trufflesqueak.model;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.utilities.CyclicAssumption;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.util.Chunk;

public class ClassObject extends PointersObject {
    private static final int METHODDICT_NAMES_INDEX = 2;
    private static final int METHODDICT_VALUES_INDEX = 1;
    private static final int NAME_INDEX = 6;
    private static final int FORMAT_INDEX = 2;
    private static final int METHODDICT_INDEX = 1;
    private static final int SUPERCLASS_INDEX = 0;
    private final CyclicAssumption methodLookupStable = new CyclicAssumption("unnamed");
    private final Set<ClassObject> subclasses = new HashSet<>();

    public ClassObject(SqueakImageContext img) {
        super(img);
    }

    public ClassObject(SqueakImageContext image, ClassObject classObject, int size) {
        super(image, classObject, size);
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
            BaseSqueakObject classInstance = at0(getPointers().length - 1);
            if (classInstance instanceof ClassObject) {
                return "Metaclass (" + ((ClassObject) classInstance).getName() + ")";
            }
        } else {
            BaseSqueakObject nameObj = getName();
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
        setSuperclass(getSuperclass());
    }

    public void setSuperclass(BaseSqueakObject superclass) {
        BaseSqueakObject oldSuperclass = getSuperclass();
        if (oldSuperclass instanceof ClassObject) {
            ((ClassObject) oldSuperclass).detachSubclass(this);
        }
        atput0(SUPERCLASS_INDEX, superclass);
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

    public BaseSqueakObject getSuperclass() {
        return at0(SUPERCLASS_INDEX);
    }

    public BaseSqueakObject getMethodDict() {
        return at0(METHODDICT_INDEX);
    }

    public BaseSqueakObject getFormat() {
        return at0(FORMAT_INDEX);
    }

    public BaseSqueakObject getName() {
        return at0(NAME_INDEX);
    }

    public Assumption getMethodLookupStable() {
        return methodLookupStable.getAssumption();
    }

    // TODO: cache the methoddict in a better structure than what Squeak provides
    // ... or use the Squeak hash to decide where to put stuff
    public BaseSqueakObject lookup(Predicate<BaseSqueakObject> test) {
        BaseSqueakObject lookupClass = this;
        while (lookupClass instanceof ClassObject) {
            BaseSqueakObject methodDict = ((ClassObject) lookupClass).getMethodDict();
            if (methodDict instanceof ListObject) {
                BaseSqueakObject values = methodDict.at0(METHODDICT_VALUES_INDEX);
                for (int i = METHODDICT_NAMES_INDEX; i < methodDict.size(); i++) {
                    BaseSqueakObject methodSelector = methodDict.at0(i);
                    if (test.test(methodSelector)) {
                        return values.at0(i - METHODDICT_NAMES_INDEX);
                    }
                }
            }
            lookupClass = ((ClassObject) lookupClass).getSuperclass();
        }
        return null;
    }

    public BaseSqueakObject lookup(BaseSqueakObject selector) {
        BaseSqueakObject result = lookup(methodSelector -> methodSelector == selector);
        if (result == null) {
            return doesNotUnderstand();
        }
        return result;
    }

    public BaseSqueakObject lookup(String selector) {
        return lookup(methodSelector -> methodSelector.toString().equals(selector));
    }

    public BaseSqueakObject doesNotUnderstand() {
        BaseSqueakObject result = lookup(image.doesNotUnderstand);
        if (result == null) {
            throw new RuntimeException("doesNotUnderstand missing!");
        }
        return result;
    }

    public boolean isVariable() {
        int instSpec = getInstSpec();
        return instSpec >= 2 && (instSpec <= 4 || instSpec >= 9);
    }

    public BaseSqueakObject newInstance() {
        return newInstance(getInstanceSize());
    }

    public BaseSqueakObject newInstance(int size) {
        int instSpec = getInstSpec();
        //@formatter:off
        switch (instSpec) {
            case 1: case 2: case 3: // pointers
                if (instancesAreClasses()) {
                    assert size == getInstanceSize();
                    return new ClassObject(image, this, size);
                } else {
                    return new PointersObject(image, this, size);
                }
            case 4: case 5: // TODO: weak pointers
                return new PointersObject(image, this, size);
            case 7: case 8:
                throw new RuntimeException("tried to instantiate invalid class");
            case 9: case 10: case 11: case 12: case 13: case 14: case 15:
                // TODO: Float
                return new NativeObject(image, this, size, 4);
            case 16: case 17: case 18: case 19: case 20: case 21: case 22: case 23:
                // TODO: LPI & LNI
                return new NativeObject(image, this, size, 1);
            default:
                // FIXME: ignore the size?
                return new CompiledMethodObject(image, this);
        }
        //@formatter:on
    }

    private int getInstSpec() {
        return (getIntFormat() >> 16) & 0x1f;
    }

    private int getInstanceSize() {
        return getIntFormat() & 0xffff;
    }

    private int getIntFormat() {
        return getFormat().unsafeUnwrapInt();
    }
}
