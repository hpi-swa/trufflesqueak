/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.accessing;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObjectWithClassAndHash;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.EmptyObject;
import de.hpi.swa.trufflesqueak.model.EphemeronObject;
import de.hpi.swa.trufflesqueak.model.FloatObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.VariablePointersObject;
import de.hpi.swa.trufflesqueak.model.WeakVariablePointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayout;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.METACLASS;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;

@GenerateInline
@GenerateUncached
@GenerateCached(false)
public abstract class SqueakObjectNewNode extends AbstractNode {
    public static final int NEW_CACHE_SIZE = 6;

    public final AbstractSqueakObjectWithClassAndHash execute(final Node node, final SqueakImageContext image, final ClassObject classObject) {
        return execute(node, image, classObject, 0);
    }

    public static final AbstractSqueakObjectWithClassAndHash executeUncached(final SqueakImageContext image, final ClassObject classObject) {
        return SqueakObjectNewNodeGen.getUncached().execute(null, image, classObject);
    }

    public final AbstractSqueakObjectWithClassAndHash execute(final Node node, final SqueakImageContext image, final ClassObject classObject, final int extraSize) {
        CompilerAsserts.partialEvaluationConstant(image);
        return image.reportAllocation(executeAllocation(node, image, classObject, extraSize));
    }

    public static final AbstractSqueakObjectWithClassAndHash executeUncached(final SqueakImageContext image, final ClassObject classObject, final int extraSize) {
        return SqueakObjectNewNodeGen.getUncached().execute(null, image, classObject, extraSize);
    }

    protected abstract AbstractSqueakObjectWithClassAndHash executeAllocation(Node node, SqueakImageContext image, ClassObject classObject, int extraSize);

    @Specialization(guards = "classObject.isZeroSized()")
    protected static final EmptyObject doEmpty(final SqueakImageContext image, final ClassObject classObject, @SuppressWarnings("unused") final int extraSize) {
        return new EmptyObject(image, classObject);
    }

    @Specialization(guards = {"classObject.isNonIndexableWithInstVars()", "image.isMetaClass(classObject)"})
    protected static final ClassObject doClass(final SqueakImageContext image, final ClassObject classObject, final int extraSize) {
        assert classObject.getBasicInstanceSize() == METACLASS.INST_SIZE && extraSize == 0;
        return new ClassObject(image, classObject, METACLASS.INST_SIZE);
    }

    @Specialization(guards = {"classObject.isNonIndexableWithInstVars()", "!image.isMetaClass(classObject)", "classObject.instancesAreClasses()"})
    protected static final ClassObject doClassOdd(final SqueakImageContext image, final ClassObject classObject, final int extraSize) {
        assert extraSize == 0;
        return new ClassObject(image, classObject, classObject.getBasicInstanceSize() + METACLASS.INST_SIZE);
    }

    @Specialization(guards = {"classObject.getLayout() == cachedLayout"}, assumptions = "cachedLayout.getValidAssumption()", limit = "NEW_CACHE_SIZE")
    protected static final PointersObject doPointersCached(final SqueakImageContext image, final ClassObject classObject, final int extraSize,
                    @Cached("getPointersLayoutOrNull(image, classObject)") final ObjectLayout cachedLayout) {
        assert extraSize == 0 && instantiatesPointersObject(image, classObject);
        return new PointersObject(image, classObject, cachedLayout);
    }

    protected static final boolean instantiatesPointersObject(final SqueakImageContext image, final ClassObject classObject) {
        return classObject.isNonIndexableWithInstVars() && !image.isMetaClass(classObject) && !classObject.instancesAreClasses();
    }

    protected static final ObjectLayout getPointersLayoutOrNull(final SqueakImageContext image, final ClassObject classObject) {
        return instantiatesPointersObject(image, classObject) ? classObject.getLayout() : null;
    }

    @Specialization(guards = {"instantiatesPointersObject(image, classObject)"}, replaces = "doPointersCached")
    protected static final PointersObject doPointersUncached(final SqueakImageContext image, final ClassObject classObject, final int extraSize) {
        assert extraSize == 0;
        return new PointersObject(image, classObject, null);
    }

    @Specialization(guards = "classObject.isIndexableWithNoInstVars()")
    protected static final ArrayObject doIndexedPointers(final SqueakImageContext image, final ClassObject classObject, final int extraSize) {
        assert classObject.getBasicInstanceSize() == 0;
        return ArrayObject.createEmptyStrategy(image, classObject, extraSize);
    }

    @Specialization(guards = {"classObject.isIndexableWithInstVars()", "image.isMethodContextClass(classObject)"})
    protected static final ContextObject doContext(final SqueakImageContext image, final ClassObject classObject, final int extraSize) {
        assert classObject.getBasicInstanceSize() == CONTEXT.INST_SIZE;
        return ContextObject.create(image, CONTEXT.INST_SIZE + extraSize);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"classObject.isIndexableWithInstVars()", "image.isBlockClosureClass(classObject) || image.isFullBlockClosureClass(classObject)"})
    protected static final BlockClosureObject doBlockClosure(final SqueakImageContext image, final ClassObject classObject, final int extraSize) {
        return BlockClosureObject.create(image, classObject, extraSize);
    }

    @Specialization(guards = {"classObject.getLayout() == cachedLayout"}, assumptions = "cachedLayout.getValidAssumption()", limit = "NEW_CACHE_SIZE")
    protected static final VariablePointersObject doVariablePointersCached(final SqueakImageContext image, final ClassObject classObject, final int extraSize,
                    @Cached(value = "getVariablePointersLayoutOrNull(image, classObject)") final ObjectLayout cachedLayout) {
        assert instantiatesVariablePointersObject(image, classObject);
        return new VariablePointersObject(image, classObject, cachedLayout, extraSize);
    }

    protected static final boolean instantiatesVariablePointersObject(final SqueakImageContext image, final ClassObject classObject) {
        return classObject.isIndexableWithInstVars() && !image.isMethodContextClass(classObject) && !image.isBlockClosureClass(classObject) && !image.isFullBlockClosureClass(classObject);
    }

    protected static final ObjectLayout getVariablePointersLayoutOrNull(final SqueakImageContext image, final ClassObject classObject) {
        return instantiatesVariablePointersObject(image, classObject) ? classObject.getLayout() : null;
    }

    @Specialization(guards = {"instantiatesVariablePointersObject(image, classObject)"}, replaces = "doVariablePointersCached")
    protected static final VariablePointersObject doVariablePointersUncached(final SqueakImageContext image, final ClassObject classObject, final int extraSize) {
        return new VariablePointersObject(image, classObject, null, extraSize);
    }

    @Specialization(guards = {"classObject.getLayout() == cachedLayout"}, assumptions = "cachedLayout.getValidAssumption()", limit = "NEW_CACHE_SIZE")
    protected static final WeakVariablePointersObject doWeakPointersCached(final SqueakImageContext image, final ClassObject classObject, final int extraSize,
                    @Cached(value = "getWeakPointersLayoutOrNull(classObject)") final ObjectLayout cachedLayout) {
        assert instantiatesWeakVariablePointersObject(classObject);
        return new WeakVariablePointersObject(image, classObject, cachedLayout, extraSize);
    }

    protected static final boolean instantiatesWeakVariablePointersObject(final ClassObject classObject) {
        return classObject.isWeak();
    }

    protected static final ObjectLayout getWeakPointersLayoutOrNull(final ClassObject classObject) {
        return instantiatesWeakVariablePointersObject(classObject) ? classObject.getLayout() : null;
    }

    @Specialization(guards = "instantiatesWeakVariablePointersObject(classObject)", replaces = "doWeakPointersCached")
    protected static final WeakVariablePointersObject doWeakPointersUncached(final SqueakImageContext image, final ClassObject classObject, final int extraSize) {
        return new WeakVariablePointersObject(image, classObject, null, extraSize);
    }

    @Specialization(guards = {"classObject.getLayout() == cachedLayout"}, assumptions = "cachedLayout.getValidAssumption()", limit = "NEW_CACHE_SIZE")
    protected static final EphemeronObject doEphemeronCached(final SqueakImageContext image, final ClassObject classObject, final int extraSize,
                    @Cached("getEphemeronLayoutOrNull(classObject)") final ObjectLayout cachedLayout) {
        assert extraSize == 0 && classObject.isEphemeronClassType();
        return new EphemeronObject(image, classObject, cachedLayout);
    }

    protected static final ObjectLayout getEphemeronLayoutOrNull(final ClassObject classObject) {
        return classObject.isEphemeronClassType() ? classObject.getLayout() : null;
    }

    @Specialization(guards = {"classObject.isEphemeronClassType()"}, replaces = "doEphemeronCached")
    protected static final EphemeronObject doEphemeronUncached(final SqueakImageContext image, final ClassObject classObject, final int extraSize) {
        assert extraSize == 0;
        return new EphemeronObject(image, classObject, null);
    }

    @Specialization(guards = "classObject.isLongs()")
    protected static final NativeObject doNativeLongs(final SqueakImageContext image, final ClassObject classObject, final int extraSize) {
        assert classObject.getBasicInstanceSize() == 0;
        return NativeObject.newNativeLongs(image, classObject, extraSize);
    }

    @Specialization(guards = {"classObject.isWords()", "image.isFloatClass(classObject)"})
    protected static final FloatObject doFloat(final SqueakImageContext image, final ClassObject classObject, final int extraSize) {
        assert classObject.getBasicInstanceSize() + extraSize == 2;
        return new FloatObject(image);
    }

    @Specialization(guards = {"classObject.isWords()", "!image.isFloatClass(classObject)"})
    protected static final NativeObject doNativeInts(final SqueakImageContext image, final ClassObject classObject, final int extraSize) {
        assert classObject.getBasicInstanceSize() == 0;
        return NativeObject.newNativeInts(image, classObject, extraSize);
    }

    @Specialization(guards = "classObject.isShorts()")
    protected static final NativeObject doNativeShorts(final SqueakImageContext image, final ClassObject classObject, final int extraSize) {
        assert classObject.getBasicInstanceSize() == 0;
        return NativeObject.newNativeShorts(image, classObject, extraSize);
    }

    @Specialization(guards = {"classObject.isBytes()"})
    protected static final NativeObject doNativeBytes(final SqueakImageContext image, final ClassObject classObject, final int extraSize) {
        assert classObject.getBasicInstanceSize() == 0;
        return NativeObject.newNativeBytes(image, classObject, extraSize);
    }

    @Specialization(guards = {"classObject.isCompiledMethodClassType()"})
    protected static final CompiledCodeObject doCompiledMethod(final SqueakImageContext image, final ClassObject classObject, final int extraSize) {
        assert classObject.getBasicInstanceSize() == 0;
        return CompiledCodeObject.newOfSize(image, extraSize, classObject);
    }
}
