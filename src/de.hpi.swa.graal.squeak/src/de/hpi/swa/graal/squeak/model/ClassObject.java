package de.hpi.swa.graal.squeak.model;

import java.util.Arrays;
import java.util.function.Predicate;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.utilities.CyclicAssumption;

import de.hpi.swa.graal.squeak.image.SqueakImageChunk;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.CLASS;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.CLASS_DESCRIPTION;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.METACLASS;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.METHOD_DICT;
import de.hpi.swa.graal.squeak.util.ArrayUtils;

/*
 * Represents all subclasses of ClassDescription (Class, Metaclass, TraitBehavior, ...).
 */
public final class ClassObject extends AbstractSqueakObject {
    private final CyclicAssumption methodDictStable = new CyclicAssumption("Method dictionary stability");
    private final CyclicAssumption classFormatStable = new CyclicAssumption("Class format stability");

    private ClassObject superclass;
    @CompilationFinal private PointersObject methodDict;
    @CompilationFinal private long format = -1;
    private ArrayObject instanceVariables;
    private PointersObject organization;
    private Object[] pointers;

    public ClassObject(final SqueakImageContext image) {
        super(image);
    }

    public ClassObject(final SqueakImageContext image, final int hash) {
        super(image, hash);
    }

    private ClassObject(final ClassObject original) {
        this(original.image, original.getSqueakClass(), original.pointers.clone());
        superclass = original.superclass;
        methodDict = original.methodDict.shallowCopy();
        format = original.format;
        instanceVariables = original.instanceVariables == null ? null : original.instanceVariables.shallowCopy();
        organization = original.organization == null ? null : original.organization.shallowCopy();
        pointers = original.pointers.clone();
    }

    private ClassObject(final SqueakImageContext image, final ClassObject sqClass, final Object[] pointers) {
        super(image, sqClass);
        this.pointers = pointers;
    }

    public ClassObject(final SqueakImageContext image, final ClassObject classObject, final int size) {
        this(image, classObject, ArrayUtils.withAll(size - CLASS_DESCRIPTION.SIZE, image.nil));
    }

    @Override
    public String nameAsClass() {
        assert isClass();
        if (isAMetaclass()) {
            final Object classInstance = getThisClass();
            if (classInstance instanceof ClassObject) {
                final NativeObject name = (NativeObject) ((ClassObject) classInstance).getClassName();
                return "Metaclass (" + name.asString() + ")";
            }
        } else {
            final Object nameObj = getClassName();
            if (nameObj instanceof NativeObject) {
                return ((NativeObject) nameObj).asString();
            }
        }
        return "UnknownClass";
    }

    private Object getThisClass() {
        return pointers[METACLASS.THIS_CLASS];
    }

    private boolean isMetaclass() {
        return this == image.metaclass;
    }

    private boolean isAMetaclass() {
        return this.getSqueakClass() == image.metaclass;
    }

    public boolean instancesAreClasses() {
        return isMetaclass() || isAMetaclass();
    }

    public void fillin(final SqueakImageChunk chunk) {
        final Object[] chunkPointers = chunk.getPointers();
        superclass = chunkPointers[CLASS_DESCRIPTION.SUPERCLASS] == image.nil ? null : (ClassObject) chunkPointers[CLASS_DESCRIPTION.SUPERCLASS];
        methodDict = (PointersObject) chunkPointers[CLASS_DESCRIPTION.METHOD_DICT];
        format = (long) chunkPointers[CLASS_DESCRIPTION.FORMAT];
        instanceVariables = chunkPointers[CLASS_DESCRIPTION.INSTANCE_VARIABLES] == image.nil ? null : (ArrayObject) chunkPointers[CLASS_DESCRIPTION.INSTANCE_VARIABLES];
        organization = chunkPointers[CLASS_DESCRIPTION.ORGANIZATION] == image.nil ? null : (PointersObject) chunkPointers[CLASS_DESCRIPTION.ORGANIZATION];
        pointers = Arrays.copyOfRange(chunkPointers, CLASS_DESCRIPTION.SIZE, chunkPointers.length);
    }

    public void setFormat(final long format) {
        classFormatStable.invalidate();
        this.format = format;
    }

    public int instsize() {
        return getSqueakClass().getBasicInstanceSize();
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

    public static boolean isInstanceVariablesIndex(final long index) {
        return index == CLASS_DESCRIPTION.INSTANCE_VARIABLES;
    }

    public static boolean isOrganizationIndex(final long index) {
        return index == CLASS_DESCRIPTION.ORGANIZATION;
    }

    public static boolean isOtherIndex(final long index) {
        return index >= CLASS_DESCRIPTION.SIZE;
    }

    public AbstractSqueakObject getSuperclass() {
        return superclass == null ? image.nil : superclass;
    }

    public ClassObject getSuperclassOrNull() {
        return superclass;
    }

    public PointersObject getMethodDict() {
        return methodDict;
    }

    public Object getClassName() {
        return pointers[CLASS.NAME];
    }

    public AbstractSqueakObject getInstanceVariables() {
        return instanceVariables == null ? image.nil : instanceVariables;
    }

    public ArrayObject getInstanceVariablesOrNull() {
        return instanceVariables;
    }

    public void setInstanceVariables(final ArrayObject instanceVariables) {
        this.instanceVariables = instanceVariables;
    }

    public AbstractSqueakObject getOrganization() {
        return organization == null ? image.nil : organization;
    }

    public PointersObject getOrganizationOrNull() {
        return organization;
    }

    public void setOrganization(final PointersObject organization) {
        this.organization = organization;
    }

    public Object getOtherPointer(final int index) {
        return pointers[index - CLASS_DESCRIPTION.SIZE];
    }

    public void setOtherPointer(final int index, final Object value) {
        pointers[index - CLASS_DESCRIPTION.SIZE] = value;
    }

    public Object[] getOtherPointers() {
        return pointers;
    }

    private void setOtherPointers(final Object[] pointers) {
        this.pointers = pointers;
    }

    public long getFormat() {
        return format;
    }

    public void setSuperclass(final ClassObject superclass) {
        this.superclass = superclass;
    }

    public void setMethodDict(final PointersObject methodDict) {
        methodDictStable.invalidate();
        this.methodDict = methodDict;
    }

    @TruffleBoundary
    public Object lookup(final String selector) {
        return lookup(methodSelector -> methodSelector != null && methodSelector.toString().equals(selector));
    }

    // TODO: cache the methoddict in a better structure than what Squeak provides
    // ... or use the Squeak hash to decide where to put stuff
    private Object lookup(final Predicate<Object> predicate) {
        CompilerAsserts.neverPartOfCompilation("This is only for finding the active context on startup, use LookupNode instead.");
        ClassObject lookupClass = this;
        while (lookupClass != null) {
            final PointersObject methodDictObject = lookupClass.getMethodDict();
            for (int i = METHOD_DICT.NAMES; i < methodDictObject.size(); i++) {
                final Object methodSelector = methodDictObject.at0(i);
                if (predicate.test(methodSelector)) {
                    final ArrayObject values = (ArrayObject) methodDictObject.at0(METHOD_DICT.VALUES);
                    return values.at0Object(i - METHOD_DICT.NAMES);
                }
            }
            lookupClass = lookupClass.getSuperclassOrNull();
        }
        return lookup(methodSelector -> methodSelector == image.doesNotUnderstand);
    }

    public boolean isVariable() {
        final int instSpec = getInstanceSpecification();
        return instSpec >= 2 && (instSpec <= 4 || instSpec >= 9);
    }

    public int getBasicInstanceSize() {
        return (int) (format & 0xffff);
    }

    public int getInstanceSpecification() {
        return (int) ((format >> 16) & 0x1f);
    }

    public AbstractSqueakObject shallowCopy() {
        return new ClassObject(this);
    }

    public boolean pointsTo(final Object thang) {
        if (superclass == thang) {
            return true;
        }
        if (methodDict == thang) {
            return true;
        }
        if (thang instanceof Number && format == (long) thang) {
            return true; // TODO: check whether format needs to be checked
        }
        if (instanceVariables == thang) {
            return true;
        }
        if (organization == thang) {
            return true;
        }
        return ArrayUtils.contains(pointers, thang);
    }

    public void become(final ClassObject other) {
        becomeOtherClass(other);

        final ClassObject otherSuperclass = other.superclass;
        final PointersObject otherMethodDict = other.methodDict;
        final long otherFormat = other.format;
        final ArrayObject otherInstanceVariables = other.instanceVariables;
        final PointersObject otherOrganization = other.organization;
        final Object[] otherPointers = other.pointers;

        other.setSuperclass(this.superclass);
        other.setMethodDict(this.methodDict);
        other.setFormat(this.format);
        other.setInstanceVariables(this.instanceVariables);
        other.setOrganization(this.organization);
        other.setOtherPointers(this.pointers);

        this.setSuperclass(otherSuperclass);
        this.setMethodDict(otherMethodDict);
        this.setFormat(otherFormat);
        this.setInstanceVariables(otherInstanceVariables);
        this.setOrganization(otherOrganization);
        this.setOtherPointers(otherPointers);
    }

    public Object[] getTraceableObjects() {
        final Object[] result = new Object[CLASS_DESCRIPTION.SIZE + pointers.length];
        result[CLASS_DESCRIPTION.SUPERCLASS] = superclass;
        result[CLASS_DESCRIPTION.METHOD_DICT] = methodDict;
        result[CLASS_DESCRIPTION.FORMAT] = format;
        result[CLASS_DESCRIPTION.INSTANCE_VARIABLES] = instanceVariables;
        result[CLASS_DESCRIPTION.ORGANIZATION] = organization;
        for (int i = 0; i < pointers.length; i++) {
            result[CLASS_DESCRIPTION.SIZE + i] = pointers[i];
        }
        return result;
    }

    public Assumption getMethodDictStable() {
        return methodDictStable.getAssumption();
    }

    public Assumption getClassFormatStable() {
        return classFormatStable.getAssumption();
    }

    public int size() {
        return CLASS_DESCRIPTION.SIZE + pointers.length;
    }
}
