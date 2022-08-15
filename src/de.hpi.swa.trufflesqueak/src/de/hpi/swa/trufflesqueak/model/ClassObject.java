/*
 * Copyright (c) 2017-2022 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2022 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import java.util.Arrays;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.utilities.CyclicAssumption;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.image.SqueakImageChunk;
import de.hpi.swa.trufflesqueak.image.SqueakImageConstants;
import de.hpi.swa.trufflesqueak.image.SqueakImageConstants.ObjectHeader;
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
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.ObjectGraphUtils.ObjectTracer;

/*
 * Represents all subclasses of ClassDescription (Class, Metaclass, TraitBehavior, ...).
 */
@SuppressWarnings("static-method")
public final class ClassObject extends AbstractSqueakObjectWithClassAndHash {
    @CompilationFinal private CyclicAssumption classHierarchyStable;
    @CompilationFinal private CyclicAssumption methodDictStable;
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
        super(image);
        this.image = image;
    }

    public ClassObject(final SqueakImageContext image, final long objectHeader) {
        super(image, objectHeader);
        this.image = image;
    }

    private ClassObject(final ClassObject original, final ArrayObject copiedInstanceVariablesOrNull) {
        super(original);
        image = original.image;
        setSqueakHash(image.getNextClassHash()); // FIXME: do in one go?
        insertIntoClassTable();
        instancesAreClasses = original.instancesAreClasses;
        superclass = original.superclass;
        methodDict = original.methodDict.shallowCopy();
        format = original.format;
        instanceVariables = copiedInstanceVariablesOrNull;
        organization = original.organization == null ? null : original.organization.shallowCopy();
        pointers = original.pointers.clone();
    }

    public ClassObject(final SqueakImageContext image, final ClassObject classObject, final int size) {
        super(image, classObject);
        this.image = image;
        setSqueakHash(image.getNextClassHash()); // FIXME: do in one go?
        insertIntoClassTable();
        pointers = ArrayUtils.withAll(Math.max(size - CLASS_DESCRIPTION.SIZE, 0), NilObject.SINGLETON);
        instancesAreClasses = image.isMetaClass(classObject);
        // `size - CLASS_DESCRIPTION.SIZE` is negative when instantiating "Behavior".
    }

    public SqueakImageContext getImage() {
        return image;
    }

    public final int asClassIndex() {
        return ObjectHeader.getHash(squeakObjectHeader);
    }

    public long rehashForClassTable(final SqueakImageContext i) {
        final int newHash = i.getNextClassHash();
        setSqueakHash(newHash);
        return newHash;
    }

    /* Used by TruffleSqueakTest. */
    public boolean hasLayout() {
        return layout != null;
    }

    public ObjectLayout getLayout() {
        if (layout == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            layout = new ObjectLayout(this, getBasicInstanceSize());
        }
        return layout;
    }

    public void updateLayout(final ObjectLayout newLayout) {
        assert layout == null || !layout.isValid() : "Old layout not invalidated";
        CompilerDirectives.transferToInterpreterAndInvalidate();
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
            if (classInstance != NilObject.SINGLETON && ((ClassObject) classInstance).pointers[CLASS.NAME] != NilObject.SINGLETON) {
                return ((ClassObject) classInstance).getClassNameUnsafe() + " class";
            } else {
                return "Unknown metaclass";
            }
        } else if (isAClassTrait()) {
            final ClassObject traitInstance = (ClassObject) pointers[CLASS_TRAIT.BASE_TRAIT - CLASS_DESCRIPTION.SIZE];
            if (traitInstance.pointers[CLASS.NAME] != NilObject.SINGLETON) {
                return traitInstance.getClassNameUnsafe() + " classTrait";
            } else {
                return "Unknown classTrait";
            }
        } else if (size() >= 10 && pointers[CLASS.NAME] != NilObject.SINGLETON) {
            // this also works for traits, since TRAIT.NAME == CLASS.NAME
            return getClassNameUnsafe();
        } else {
            return "Unknown behavior";
        }
    }

    public String getClassNameUnsafe() {
        return ((NativeObject) pointers[CLASS.NAME]).asStringUnsafe();
    }

    public ArrayObject getSubclasses() {
        return (ArrayObject) pointers[CLASS.SUBCLASSES];
    }

    private boolean isAClassTrait() {
        if (pointers.length <= CLASS_TRAIT.BASE_TRAIT - CLASS_DESCRIPTION.SIZE) {
            return false;
        }
        final Object traitInstance = pointers[CLASS_TRAIT.BASE_TRAIT - CLASS_DESCRIPTION.SIZE];
        return traitInstance instanceof ClassObject && this != ((ClassObject) traitInstance).getSqueakClass() && ((ClassObject) traitInstance).getSqueakClass().getClassName().equals("Trait");
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
        if (externalFunctionClass instanceof ClassObject) {
            return includesBehavior((ClassObject) i.getSpecialObject(SPECIAL_OBJECT.CLASS_EXTERNAL_FUNCTION));
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
            if (needsSqueakHash()) {
                final int hash = chunk.getHash();
                /* Generate class hashes if unknown. */
                setSqueakHash(hash != 0 ? hash : chunk.getImage().getNextClassHash());
            }
            final Object[] chunkPointers = chunk.getPointers();
            superclass = chunkPointers[CLASS_DESCRIPTION.SUPERCLASS] == NilObject.SINGLETON ? null : (ClassObject) chunkPointers[CLASS_DESCRIPTION.SUPERCLASS];
            methodDict = (VariablePointersObject) chunkPointers[CLASS_DESCRIPTION.METHOD_DICT];
            format = (long) chunkPointers[CLASS_DESCRIPTION.FORMAT];
            instanceVariables = chunkPointers[CLASS_DESCRIPTION.INSTANCE_VARIABLES] == NilObject.SINGLETON ? null : (ArrayObject) chunkPointers[CLASS_DESCRIPTION.INSTANCE_VARIABLES];
            organization = chunkPointers[CLASS_DESCRIPTION.ORGANIZATION] == NilObject.SINGLETON ? null : (PointersObject) chunkPointers[CLASS_DESCRIPTION.ORGANIZATION];
            pointers = Arrays.copyOfRange(chunkPointers, CLASS_DESCRIPTION.SIZE, chunkPointers.length);
        } else if (needsSqueakHash()) {
            setSqueakHash(chunk.getImage().getNextClassHash());
        }
    }

    public void setFormat(final long format) {
        classFormatStable().invalidate();
        this.format = format;
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

    public ArrayObject getInstanceVariablesOrNull() {
        return instanceVariables;
    }

    public void setInstanceVariables(final ArrayObject instanceVariables) {
        this.instanceVariables = instanceVariables;
    }

    public AbstractSqueakObject getOrganization() {
        return NilObject.nullToNil(organization);
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
        classHierarchyStable().invalidate();
        this.superclass = superclass;
    }

    public void setMethodDict(final VariablePointersObject methodDict) {
        methodDictStable().invalidate();
        this.methodDict = methodDict;
    }

    @TruffleBoundary
    public Object lookupInMethodDictSlow(final NativeObject selector) {
        final AbstractPointersObjectReadNode readValuesNode = AbstractPointersObjectReadNode.getUncached();
        ClassObject lookupClass = this;
        while (lookupClass != null) {
            final VariablePointersObject methodDictionary = lookupClass.getMethodDict();
            final Object[] methodDictVariablePart = methodDictionary.getVariablePart();
            for (int i = 0; i < methodDictVariablePart.length; i++) {
                if (selector == methodDictVariablePart[i]) {
                    return readValuesNode.executeArray(methodDictionary, METHOD_DICT.VALUES).getObjectStorage()[i];
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
        if (thang instanceof Long && format == (long) thang) {
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

    private CyclicAssumption classHierarchyStable() {
        if (classHierarchyStable == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            classHierarchyStable = new CyclicAssumption("Class hierarchy stability");
        }
        return classHierarchyStable;
    }

    public Assumption getClassHierarchyStable() {
        return classHierarchyStable().getAssumption();
    }

    private CyclicAssumption methodDictStable() {
        if (methodDictStable == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            methodDictStable = new CyclicAssumption("Method dictionary stability");
        }
        return methodDictStable;
    }

    public Assumption getMethodDictStable() {
        return methodDictStable().getAssumption();
    }

    public void invalidateMethodDictStableAssumption() {
        methodDictStable().invalidate();
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

    private void insertIntoClassTable() {
        final int classIndex = asClassIndex();
        final long majorIndex = SqueakImageConstants.majorClassIndexOf(classIndex);
        final long minorIndex = SqueakImageConstants.minorClassIndexOf(classIndex);
        if (image.getHiddenRoots().getObject(majorIndex) == NilObject.SINGLETON) {
            ensureConsecutiveClassPagesUpTo(majorIndex);
        }
        final ArrayObject classTablePage = (ArrayObject) image.getHiddenRoots().getObject(majorIndex);
        final Object pageEntry = classTablePage.getObject(minorIndex);
        if (pageEntry == this) {
            return; /* Found myself in page (possible because we are re-using hiddenRoots). */
        } else if (pageEntry == NilObject.SINGLETON) {
            /* Free slot found in classTable. */
            classTablePage.setObject(minorIndex, this);
        } else {
            /* classIndex clashed, re-hash class until there's no longer a clash. */
            long newMajorIndex = majorIndex;
            long newMinorIndex = minorIndex;
            while (image.getHiddenRoots().getObject(newMajorIndex) != NilObject.SINGLETON || classTablePage.getObject(newMinorIndex) != NilObject.SINGLETON) {
                final int newHash = (int) rehashForClassTable(image);
                newMajorIndex = SqueakImageConstants.majorClassIndexOf(newHash);
                newMinorIndex = SqueakImageConstants.minorClassIndexOf(newHash);
            }
            insertIntoClassTable();
        }
    }

    /* Are all entries up to numClassTablePages must not be nil (see validClassTableRootPages). */
    private void ensureConsecutiveClassPagesUpTo(final long majorIndex) {
        for (int i = 0; i < majorIndex; i++) {
            if (image.getHiddenRoots().getObject(majorIndex) == NilObject.SINGLETON) {
                image.getHiddenRoots().setObject(majorIndex, newClassPage());
            }
        }
    }

    private ArrayObject newClassPage() {
        return image.asArrayOfObjects(ArrayUtils.withAll(SqueakImageConstants.CLASS_TABLE_PAGE_SIZE, NilObject.SINGLETON));
    }

    @Override
    public void pointersBecomeOneWay(final Object[] from, final Object[] to) {
        for (int i = 0; i < from.length; i++) {
            final Object fromPointer = from[i];
            if (fromPointer == getSuperclassOrNull() && to[i] instanceof ClassObject) {
                setSuperclass((ClassObject) to[i]);
            }
            if (fromPointer == getMethodDict() && fromPointer != to[i] && to[i] instanceof VariablePointersObject) {
                // Only update methodDict if changed to avoid redundant invalidation.
                setMethodDict((VariablePointersObject) to[i]);
            }
            if (fromPointer == getInstanceVariablesOrNull() && to[i] instanceof ArrayObject) {
                setInstanceVariables((ArrayObject) to[i]);
            }
            if (fromPointer == getOrganizationOrNull() && to[i] instanceof PointersObject) {
                setOrganization((PointersObject) to[i]);
            }
        }
        pointersBecomeOneWay(getOtherPointers(), from, to);
    }

    @Override
    public void tracePointers(final ObjectTracer tracer) {
        tracer.addIfUnmarked(getSuperclass());
        tracer.addIfUnmarked(getMethodDict());
        tracer.addIfUnmarked(getInstanceVariables());
        tracer.addIfUnmarked(getOrganization());
        for (final Object value : getOtherPointers()) {
            tracer.addIfUnmarked(value);
        }
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
