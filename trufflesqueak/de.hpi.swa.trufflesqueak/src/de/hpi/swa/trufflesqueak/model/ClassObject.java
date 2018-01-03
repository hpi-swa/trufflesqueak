package de.hpi.swa.trufflesqueak.model;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.utilities.CyclicAssumption;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.util.KnownClasses.CLASS;
import de.hpi.swa.trufflesqueak.util.KnownClasses.METHOD_DICT;
import de.hpi.swa.trufflesqueak.util.SqueakImageChunk;

public class ClassObject extends AbstractPointersObject {
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

    private ClassObject(SqueakImageContext img, ClassObject sqClass, Object[] ptrs) {
        super(img, sqClass);
        pointers = ptrs;
    }

    private ClassObject(SqueakImageContext image, ClassObject classObject, int size) {
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
    public void fillin(SqueakImageChunk chunk) {
        super.fillin(chunk);
        // initialize the subclasses set
        setFormat((int) at0(CLASS.FORMAT));
        setSuperclass(getSuperclass());
    }

    public void setFormat(int format) {
        super.atput0(CLASS.FORMAT, format);
        instSpec = (format >> 16) & 0x1f;
        instanceSize = format & 0xffff;
        classFormatStable.invalidate();
    }

    public void setSuperclass(Object superclass) {
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
        return at0(CLASS.SUPERCLASS);
    }

    public Object getMethodDict() {
        return at0(CLASS.METHOD_DICT);
    }

    public Object getName() {
        return at0(CLASS.NAME);
    }

    @Override
    public void atput0(int idx, Object obj) {
        if (idx == CLASS.FORMAT) {
            setFormat((int) obj);
        } else if (idx == CLASS.SUPERCLASS) {
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
    private Object lookup(Predicate<Object> test) {
        Object lookupClass = this;
        while (lookupClass instanceof ClassObject) {
            Object methodDict = ((ClassObject) lookupClass).getMethodDict();
            if (methodDict instanceof ListObject) {
                Object values = ((ListObject) methodDict).at0(METHOD_DICT.VALUES);
                if (values instanceof BaseSqueakObject) {
                    for (int i = METHOD_DICT.NAMES; i < ((BaseSqueakObject) methodDict).size(); i++) {
                        Object methodSelector = ((BaseSqueakObject) methodDict).at0(i);
                        if (test.test(methodSelector)) {
                            return ((BaseSqueakObject) values).at0(i - METHOD_DICT.NAMES);
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

    private Object doesNotUnderstand() {
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
                if (this == image.methodContextClass) {
                    return ContextObject.createWriteableContextObject(image, size);
                } else if (this.getSqClass() == image.blockClosureClass) {
                    return new BlockClosure(image); // TODO: verify this is actually used
                } else {
                return new PointersObject(image, this, size);
                }
            case 4: // TODO: weak pointers
                return new ListObject(image, this, size);
            case 5: // TODO: weak pointers
                return new PointersObject(image, this, size);
            case 7: case 8:
                throw new RuntimeException("tried to instantiate an immediate");
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
