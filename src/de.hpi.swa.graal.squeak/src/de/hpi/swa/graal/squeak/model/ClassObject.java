package de.hpi.swa.graal.squeak.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.utilities.CyclicAssumption;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.image.reading.SqueakImageChunk;
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

    @CompilationFinal private boolean instancesAreClasses = false;

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

    private ClassObject(final ClassObject original, final ArrayObject copiedInstanceVariablesOrNull) {
        this(original.image, original.getSqueakClass(), original.pointers.clone());
        instancesAreClasses = original.instancesAreClasses;
        superclass = original.superclass;
        methodDict = original.methodDict.shallowCopy();
        format = original.format;
        instanceVariables = copiedInstanceVariablesOrNull;
        organization = original.organization == null ? null : original.organization.shallowCopy();
        pointers = original.pointers.clone();
    }

    private ClassObject(final SqueakImageContext image, final ClassObject sqClass, final Object[] pointers) {
        super(image, sqClass);
        this.pointers = pointers;
        instancesAreClasses = sqClass.isMetaClass();
    }

    public ClassObject(final SqueakImageContext image, final ClassObject classObject, final int size) {
        this(image, classObject, ArrayUtils.withAll(Math.max(size - CLASS_DESCRIPTION.SIZE, 0), image.nil));
        // `size - CLASS_DESCRIPTION.SIZE` is negative when instantiating "Behavior".
    }

    @Override
    public String nameAsClass() {
        assert isClass();
        if (isAMetaClass()) {
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

    private boolean isAMetaClass() {
        return this.getSqueakClass().isMetaClass();
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

    public boolean isFixed() {
        return !isVariable();
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

    public boolean isPointers() {
        return !isBits();
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

    public void setInstancesAreClasses(final String className) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        // TODO: think about better check for the below.
        instancesAreClasses = isMetaClass() || isAMetaClass() || "Behavior".equals(className) || "ClassDescription".equals(className) || "Class".equals(className);
    }

    public boolean instancesAreClasses() {
        return instancesAreClasses;
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

    @Override
    public int instsize() {
        return getSqueakClass().getBasicInstanceSize();
    }

    @Override
    public int size() {
        return CLASS_DESCRIPTION.SIZE + pointers.length;
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

    public boolean hasInstanceVariables() {
        return instanceVariables != null;
    }

    public AbstractSqueakObject getInstanceVariables() {
        return hasInstanceVariables() ? instanceVariables : image.nil;
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
                    return values.at0(i - METHOD_DICT.NAMES);
                }
            }
            lookupClass = lookupClass.getSuperclassOrNull();
        }
        return lookup(methodSelector -> methodSelector == image.doesNotUnderstand);
    }

    public Object[] listMethods() {
        final List<String> methodNames = new ArrayList<>();
        ClassObject lookupClass = this;
        while (lookupClass != null) {
            final PointersObject methodDictObject = lookupClass.getMethodDict();
            for (int i = METHOD_DICT.NAMES; i < methodDictObject.size(); i++) {
                final Object methodSelector = methodDictObject.at0(i);
                if (methodSelector != image.nil) {
                    methodNames.add(methodSelector.toString());
                }
            }
            lookupClass = lookupClass.getSuperclassOrNull();
        }
        return methodNames.toArray(new String[methodNames.size()]);
    }

    public int getBasicInstanceSize() {
        return (int) (format & 0xffff);
    }

    public int getInstanceSpecification() {
        return (int) ((format >> 16) & 0x1f);
    }

    public AbstractSqueakObject shallowCopy(final ArrayObject copiedInstanceVariablesOrNull) {
        return new ClassObject(this, copiedInstanceVariablesOrNull);
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

        if (instancesAreClasses != other.getSqueakClass().isMetaClass()) {
            CompilerDirectives.transferToInterpreter();
            instancesAreClasses = other.getSqueakClass().isMetaClass();
        }

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
        final Object[] result = new Object[CLASS_DESCRIPTION.SIZE + pointers.length - 1];
        result[CLASS_DESCRIPTION.SUPERCLASS] = superclass;
        result[CLASS_DESCRIPTION.METHOD_DICT] = methodDict;
        // result[CLASS_DESCRIPTION.FORMAT] = format; <- Ignore immediate value during tracing.
        result[CLASS_DESCRIPTION.INSTANCE_VARIABLES - 1] = instanceVariables;
        result[CLASS_DESCRIPTION.ORGANIZATION - 1] = organization;
        for (int i = 0; i < pointers.length; i++) {
            result[CLASS_DESCRIPTION.SIZE + i - 1] = pointers[i];
        }
        return result;
    }

    public Assumption getMethodDictStable() {
        return methodDictStable.getAssumption();
    }

    public Assumption getClassFormatStable() {
        return classFormatStable.getAssumption();
    }

    public String getClassComment() {
        return CLASS_DESCRIPTION.getClassComment(this);
    }

    public boolean isCompiledMethodClass() {
        return this == image.compiledMethodClass;
    }

    public boolean isMethodContextClass() {
        return this == image.methodContextClass;
    }

    public boolean isBlockClosureClass() {
        return this == image.blockClosureClass;
    }

    public boolean isFloatClass() {
        return this == image.floatClass;
    }

    public boolean isLargePositiveIntegerClass() {
        return this == image.largePositiveIntegerClass;
    }

    public boolean isLargeNegativeIntegerClass() {
        return this == image.largeNegativeIntegerClass;
    }

    public boolean isSmallIntegerClass() {
        return this == image.smallIntegerClass;
    }
}
