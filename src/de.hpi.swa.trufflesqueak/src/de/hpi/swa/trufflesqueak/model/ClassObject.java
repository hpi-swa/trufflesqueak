/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
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
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.SPECIAL_OBJECT;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodes.ArrayObjectShallowCopyNode;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.ObjectGraphUtils.ObjectTracer;

/*
 * Represents all subclasses of ClassDescription (Class, Metaclass, TraitBehavior, ...).
 */
@SuppressWarnings("static-method")
public final class ClassObject extends AbstractSqueakObjectWithClassAndHash {
    @CompilationFinal private CyclicAssumption classHierarchyAndMethodDictStable;
    @CompilationFinal private CyclicAssumption classFormatStable;

    private final SqueakImageContext image;
    @CompilationFinal private boolean instancesAreClasses;

    private ClassObject superclass;
    @CompilationFinal private VariablePointersObject methodDict;
    @CompilationFinal private long format = -1;
    private ArrayObject instanceVariables;
    private PointersObject organization;
    private Object[] pointers;

    @CompilationFinal private ObjectLayout layout;

    public ClassObject(final SqueakImageContext image) {
        super();
        this.image = image;
    }

    public ClassObject(final SqueakImageContext image, final long header, final ClassObject squeakClass) {
        super(header, squeakClass);
        this.image = image;
    }

    private ClassObject(final ClassObject original, final ArrayObject copiedInstanceVariablesOrNull) {
        super(original);
        image = original.image;
        instancesAreClasses = original.instancesAreClasses;
        superclass = original.superclass;
        methodDict = original.methodDict.shallowCopy();
        format = original.format;
        instanceVariables = copiedInstanceVariablesOrNull;
        organization = original.organization == null ? null : original.organization.shallowCopy();
        pointers = original.pointers.clone();
        initializeLayout();
    }

    public ClassObject(final SqueakImageContext image, final ClassObject classObject, final int size) {
        super(image, classObject);
        this.image = image;
        pointers = ArrayUtils.withAll(Math.max(size - CLASS_DESCRIPTION.SIZE, 0), NilObject.SINGLETON);
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
        if (image.isMetaClass(getSqueakClass())) {
            final Object classInstance = pointers[METACLASS.THIS_CLASS - CLASS_DESCRIPTION.SIZE];
            if (classInstance != NilObject.SINGLETON && ((ClassObject) classInstance).pointers[CLASS.NAME] instanceof final NativeObject metaClassName) {
                return metaClassName.asStringUnsafe() + " class";
            } else {
                return "Unknown metaclass";
            }
        } else if (isAClassTrait()) {
            final ClassObject traitInstance = (ClassObject) pointers[CLASS_TRAIT.BASE_TRAIT - CLASS_DESCRIPTION.SIZE];
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

    public ArrayObject getSubclasses() {
        return (ArrayObject) pointers[CLASS.SUBCLASSES];
    }

    private boolean isAClassTrait() {
        if (pointers.length <= CLASS_TRAIT.BASE_TRAIT - CLASS_DESCRIPTION.SIZE) {
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

    public void setInstancesAreClasses() {
        if (!instancesAreClasses) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            instancesAreClasses = true;
        }
    }

    public boolean instancesAreClasses() {
        return instancesAreClasses;
    }

    public boolean isCompiledMethodClass() {
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

    public boolean includesExternalFunctionBehavior(final SqueakImageContext i) {
        final Object externalFunctionClass = i.getSpecialObject(SPECIAL_OBJECT.CLASS_EXTERNAL_FUNCTION);
        if (externalFunctionClass instanceof final ClassObject efc) {
            return includesBehavior(efc);
        } else {
            return false;
        }
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
            methodDict = (VariablePointersObject) chunk.getPointer(CLASS_DESCRIPTION.METHOD_DICT);
            format = (long) chunk.getPointer(CLASS_DESCRIPTION.FORMAT);
            instanceVariables = (ArrayObject) NilObject.nilToNull(chunk.getPointer(CLASS_DESCRIPTION.INSTANCE_VARIABLES));
            organization = (PointersObject) NilObject.nilToNull(chunk.getPointer(CLASS_DESCRIPTION.ORGANIZATION));
            pointers = chunk.getPointers(CLASS_DESCRIPTION.SIZE);
            initializeLayout();
        }
    }

    /* see SpurMemoryManager>>#ensureBehaviorHash: */
    public void ensureBehaviorHash() {
        if (getSqueakHash() == SqueakImageConstants.FREE_OBJECT_CLASS_INDEX_PUN) {
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
        classFormatStable().invalidate();
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
        return NilObject.nullToNil(superclass);
    }

    public ClassObject getSuperclassOrNull() {
        return superclass;
    }

    public VariablePointersObject getMethodDict() {
        return methodDict;
    }

    public boolean hasInstanceVariables() {
        return instanceVariables != null;
    }

    public AbstractSqueakObject getInstanceVariables() {
        return hasInstanceVariables() ? instanceVariables : NilObject.SINGLETON;
    }

    public void setInstanceVariables(final ArrayObject instanceVariables) {
        this.instanceVariables = instanceVariables;
    }

    public AbstractSqueakObject getOrganization() {
        return NilObject.nullToNil(organization);
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

    public void setOtherPointers(final Object[] pointers) {
        this.pointers = pointers;
    }

    public long getFormat() {
        return format;
    }

    public void setSuperclass(final ClassObject superclass) {
        invalidateClassHierarchyAndMethodDictStableAssumption();
        this.superclass = superclass;
    }

    public void setMethodDict(final VariablePointersObject methodDict) {
        invalidateClassHierarchyAndMethodDictStableAssumption();
        this.methodDict = methodDict;
    }

    @TruffleBoundary
    public Object lookupInMethodDictSlow(final NativeObject selector) {
        ClassObject lookupClass = this;
        while (lookupClass != null) {
            final VariablePointersObject methodDictionary = lookupClass.getMethodDict();
            final Object[] methodDictVariablePart = methodDictionary.getVariablePart();
            for (int i = 0; i < methodDictVariablePart.length; i++) {
                if (selector == methodDictVariablePart[i]) {
                    return AbstractPointersObjectReadNode.getUncached().executeArray(null, methodDictionary, METHOD_DICT.VALUES).getObjectStorage()[i];
                }
            }
            lookupClass = lookupClass.getSuperclassOrNull();
        }
        assert !selector.isDoesNotUnderstand(image) : "Could not find does not understand method";
        return null; /* Signals a doesNotUnderstand. */
    }

    public CompiledCodeObject lookupMethodInMethodDictSlow(final NativeObject selector) {
        return (CompiledCodeObject) lookupInMethodDictSlow(selector);
    }

    public int getBasicInstanceSize() {
        return (int) (format & 0xffff);
    }

    public int getInstanceSpecification() {
        return (int) (format >> 16 & 0x1f);
    }

    public ClassObject shallowCopy(final Node inlineTarget, final ArrayObjectShallowCopyNode arrayCopyNode) {
        assert hasInstanceVariables();
        return shallowCopy(arrayCopyNode.execute(inlineTarget, instanceVariables));
    }

    public ClassObject shallowCopy(final ArrayObject copiedInstanceVariablesOrNull) {
        return new ClassObject(this, copiedInstanceVariablesOrNull);
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

        final boolean otherInstancesAreClasses = image.isMetaClass(other.getSqueakClass());
        if (instancesAreClasses != otherInstancesAreClasses) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            instancesAreClasses = otherInstancesAreClasses;
        }

        final ClassObject otherSuperclass = other.superclass;
        final VariablePointersObject otherMethodDict = other.methodDict;
        final long otherFormat = other.format;
        final ArrayObject otherInstanceVariables = other.instanceVariables;
        final PointersObject otherOrganization = other.organization;
        final Object[] otherPointers = other.pointers;

        other.setSuperclass(superclass);
        other.setMethodDict(methodDict);
        other.setFormat(format);
        other.setInstanceVariables(instanceVariables);
        other.setOrganization(organization);
        other.setOtherPointers(pointers);

        setSuperclass(otherSuperclass);
        setMethodDict(otherMethodDict);
        setFormat(otherFormat);
        setInstanceVariables(otherInstanceVariables);
        setOrganization(otherOrganization);
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

    public void invalidateClassHierarchyAndMethodDictStableAssumption() {
        classHierarchyAndMethodDictStable().invalidate();
    }

    private CyclicAssumption classFormatStable() {
        if (classFormatStable == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            classFormatStable = new CyclicAssumption("Class format stability");
        }
        return classFormatStable;
    }

    public Assumption getClassFormatStable() {
        return classFormatStable().getAssumption();
    }

    public String getClassComment() {
        return CLASS_DESCRIPTION.getClassComment(this);
    }

    @Override
    public void pointersBecomeOneWay(final Object[] from, final Object[] to) {
        for (int i = 0; i < from.length; i++) {
            final Object fromPointer = from[i];
            final Object toPointer = to[i];
            if (fromPointer == superclass && toPointer instanceof final ClassObject o) {
                setSuperclass(o);
            }
            if (fromPointer == methodDict && fromPointer != toPointer && toPointer instanceof final VariablePointersObject o) {
                // Only update methodDict if changed to avoid redundant invalidation.
                setMethodDict(o);
            }
            if (fromPointer == instanceVariables && toPointer instanceof final ArrayObject o) {
                setInstanceVariables(o);
            }
            if (fromPointer == organization && toPointer instanceof final PointersObject o) {
                setOrganization(o);
            }
            for (int j = 0; j < pointers.length; j++) {
                if (pointers[j] == fromPointer) {
                    pointers[j] = toPointer;
                }
            }
        }
    }

    @Override
    public void tracePointers(final ObjectTracer tracer) {
        tracer.addIfUnmarked(superclass);
        tracer.addIfUnmarked(methodDict);
        tracer.addIfUnmarked(instanceVariables);
        tracer.addIfUnmarked(organization);
        tracer.addAllIfUnmarked(pointers);
    }

    @Override
    public void trace(final SqueakImageWriter writer) {
        super.trace(writer);
        writer.traceIfNecessary(superclass);
        writer.traceIfNecessary(methodDict);
        writer.traceIfNecessary(instanceVariables);
        writer.traceIfNecessary(organization);
        writer.traceAllIfNecessary(pointers);
    }

    @Override
    public void write(final SqueakImageWriter writer) {
        if (!writeHeader(writer)) {
            throw SqueakException.create("ClassObject must have slots:", this);
        }
        writer.writeObject(getSuperclass());
        writer.writeObject(getMethodDict());
        writer.writeSmallInteger(format);
        writer.writeObject(getInstanceVariables());
        writer.writeObject(getOrganization());
        writer.writeObjects(getOtherPointers());
    }
}
