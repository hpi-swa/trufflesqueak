/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.accessing;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObjectWithHash;
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

    public final AbstractSqueakObjectWithHash execute(final Node node, final ClassObject classObject) {
        return execute(node, classObject, 0);
    }

    public static final AbstractSqueakObjectWithHash executeUncached(final ClassObject classObject) {
        return SqueakObjectNewNodeGen.getUncached().execute(null, classObject);
    }

    public abstract AbstractSqueakObjectWithHash execute(Node node, ClassObject classObject, int extraSize);

    public static final AbstractSqueakObjectWithHash executeUncached(final ClassObject classObject, final int extraSize) {
        return SqueakObjectNewNodeGen.getUncached().execute(null, classObject, extraSize);
    }

    @Specialization(guards = "classObject.isIndexableWithNoInstVars()")
    protected static final ArrayObject doArray(final ClassObject classObject, final int extraSize) {
        assert classObject.getBasicInstanceSize() == 0;
        return ArrayObject.createEmptyStrategy(classObject, extraSize);
    }

    @Specialization(guards = {"classObject.isBytes()"})
    protected static final NativeObject doNativeBytes(final ClassObject classObject, final int extraSize) {
        assert classObject.getBasicInstanceSize() == 0;
        return NativeObject.newNativeBytes(classObject, extraSize);
    }

    @Specialization(guards = "classObject.isShorts()")
    protected static final NativeObject doNativeShorts(final ClassObject classObject, final int extraSize) {
        assert classObject.getBasicInstanceSize() == 0;
        return NativeObject.newNativeShorts(classObject, extraSize);
    }

    @Specialization(guards = {"classObject.isWords()", "!getContext().isFloatClass(classObject)"})
    protected static final NativeObject doNativeInts(final ClassObject classObject, final int extraSize) {
        assert classObject.getBasicInstanceSize() == 0;
        return NativeObject.newNativeInts(classObject, extraSize);
    }

    @Specialization(guards = "classObject.isLongs()")
    protected static final NativeObject doNativeLongs(final ClassObject classObject, final int extraSize) {
        assert classObject.getBasicInstanceSize() == 0;
        return NativeObject.newNativeLongs(classObject, extraSize);
    }

    @Specialization(guards = {"classObject.getLayout() == cachedLayout"}, assumptions = "cachedLayout.getValidAssumption()", limit = "NEW_CACHE_SIZE")
    protected static final PointersObject doPointersCached(final ClassObject classObject, final int extraSize,
                    @Cached("getPointersLayoutOrNull(classObject)") final ObjectLayout cachedLayout) {
        assert extraSize == 0;
        return new PointersObject(classObject, cachedLayout);
    }

    protected static final boolean instantiatesPointersObject(final SqueakImageContext image, final ClassObject classObject) {
        return classObject.isNonIndexableWithInstVars() && !image.isMetaClass(classObject) && !classObject.instancesAreClasses();
    }

    protected static final ObjectLayout getPointersLayoutOrNull(final ClassObject classObject) {
        CompilerAsserts.neverPartOfCompilation();
        return instantiatesPointersObject(classObject.getImage(), classObject) ? classObject.getLayout() : null;
    }

    @Specialization(guards = {"instantiatesPointersObject(getContext(), classObject)"}, replaces = "doPointersCached")
    protected static final PointersObject doPointersUncached(final ClassObject classObject, final int extraSize) {
        assert extraSize == 0;
        return new PointersObject(classObject);
    }

    @Specialization(guards = {"classObject.getLayout() == cachedLayout"}, assumptions = "cachedLayout.getValidAssumption()", limit = "NEW_CACHE_SIZE")
    protected static final VariablePointersObject doVariablePointersCached(final ClassObject classObject, final int extraSize,
                    @Cached(value = "getVariablePointersLayoutOrNull(classObject)") final ObjectLayout cachedLayout) {
        return new VariablePointersObject(classObject, cachedLayout, extraSize);
    }

    protected static final boolean instantiatesVariablePointersObject(final SqueakImageContext image, final ClassObject classObject) {
        return classObject.isIndexableWithInstVars() && !image.isMethodContextClass(classObject) && !image.isBlockClosureClass(classObject) && !image.isFullBlockClosureClass(classObject);
    }

    protected static final ObjectLayout getVariablePointersLayoutOrNull(final ClassObject classObject) {
        CompilerAsserts.neverPartOfCompilation();
        return instantiatesVariablePointersObject(classObject.getImage(), classObject) ? classObject.getLayout() : null;
    }

    @Specialization(guards = {"instantiatesVariablePointersObject(getContext(), classObject)"}, replaces = "doVariablePointersCached")
    protected static final VariablePointersObject doVariablePointersUncached(final ClassObject classObject, final int extraSize) {
        return new VariablePointersObject(classObject, classObject.getLayout(), extraSize);
    }

    @Specialization(guards = {"classObject.getLayout() == cachedLayout"}, assumptions = "cachedLayout.getValidAssumption()", limit = "NEW_CACHE_SIZE")
    protected static final WeakVariablePointersObject doWeakPointersCached(final ClassObject classObject, final int extraSize,
                    @Cached(value = "getWeakPointersLayoutOrNull(classObject)") final ObjectLayout cachedLayout) {
        assert instantiatesWeakVariablePointersObject(classObject);
        return new WeakVariablePointersObject(classObject, cachedLayout, extraSize);
    }

    protected static final boolean instantiatesWeakVariablePointersObject(final ClassObject classObject) {
        return classObject.isWeak();
    }

    protected static final ObjectLayout getWeakPointersLayoutOrNull(final ClassObject classObject) {
        return instantiatesWeakVariablePointersObject(classObject) ? classObject.getLayout() : null;
    }

    @Specialization(guards = "instantiatesWeakVariablePointersObject(classObject)", replaces = "doWeakPointersCached")
    protected static final WeakVariablePointersObject doWeakPointersUncached(final ClassObject classObject, final int extraSize) {
        return new WeakVariablePointersObject(classObject, classObject.getLayout(), extraSize);
    }

    @Specialization(guards = "classObject.isZeroSized()")
    protected static final EmptyObject doEmpty(final ClassObject classObject, @SuppressWarnings("unused") final int extraSize) {
        return new EmptyObject(classObject);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"classObject.isIndexableWithInstVars()", "image.isFullBlockClosureClass(classObject)"})
    protected static final BlockClosureObject doFullBlockClosure(final ClassObject classObject, final int extraSize,
                    @Bind final SqueakImageContext image) {
        return new BlockClosureObject(false, extraSize);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"classObject.isIndexableWithInstVars()", "image.isBlockClosureClass(classObject)"})
    protected static final BlockClosureObject doBlockClosure(final ClassObject classObject, final int extraSize,
                    @Bind final SqueakImageContext image) {
        return new BlockClosureObject(true, extraSize);
    }

    @Specialization(guards = {"classObject.isIndexableWithInstVars()", "getContext().isMethodContextClass(classObject)"})
    protected static final ContextObject doContext(final ClassObject classObject, final int extraSize) {
        assert classObject.getBasicInstanceSize() == CONTEXT.INST_SIZE;
        return new ContextObject(extraSize);
    }

    @Specialization(guards = {"classObject.isNonIndexableWithInstVars()", "image.isMetaClass(classObject)"})
    protected static final ClassObject doClass(final ClassObject classObject, final int extraSize,
                    @Bind final SqueakImageContext image) {
        assert classObject.getBasicInstanceSize() == METACLASS.INST_SIZE && extraSize == 0;
        return new ClassObject(image, classObject, METACLASS.INST_SIZE);
    }

    @Specialization(guards = {"classObject.isNonIndexableWithInstVars()", "!image.isMetaClass(classObject)", "classObject.instancesAreClasses()"})
    protected static final ClassObject doClassOdd(final ClassObject classObject, final int extraSize,
                    @Bind final SqueakImageContext image) {
        assert extraSize == 0;
        return new ClassObject(image, classObject, classObject.getBasicInstanceSize() + METACLASS.INST_SIZE);
    }

    @Specialization(guards = {"classObject.getLayout() == cachedLayout"}, assumptions = "cachedLayout.getValidAssumption()", limit = "NEW_CACHE_SIZE")
    protected static final EphemeronObject doEphemeronCached(final ClassObject classObject, final int extraSize,
                    @Bind final SqueakImageContext image,
                    @Cached("getEphemeronLayoutOrNull(classObject)") final ObjectLayout cachedLayout) {
        assert extraSize == 0 && classObject.isEphemeronClassType();
        return new EphemeronObject(image, classObject, cachedLayout);
    }

    protected static final ObjectLayout getEphemeronLayoutOrNull(final ClassObject classObject) {
        return classObject.isEphemeronClassType() ? classObject.getLayout() : null;
    }

    @Specialization(guards = {"classObject.isEphemeronClassType()"}, replaces = "doEphemeronCached")
    protected static final EphemeronObject doEphemeronUncached(final ClassObject classObject, final int extraSize,
                    @Bind final SqueakImageContext image) {
        assert extraSize == 0;
        return new EphemeronObject(image, classObject, classObject.getLayout());
    }

    @Specialization(guards = {"classObject.isWords()", "getContext().isFloatClass(classObject)"})
    protected static final FloatObject doFloat(final ClassObject classObject, final int extraSize) {
        assert classObject.getBasicInstanceSize() + extraSize == 2;
        return new FloatObject();
    }

    @Specialization(guards = {"classObject.isCompiledMethodClassType()"})
    protected static final CompiledCodeObject doCompiledMethod(final ClassObject classObject, final int extraSize) {
        assert classObject.getBasicInstanceSize() == 0;
        return CompiledCodeObject.newOfSize(extraSize, classObject);
    }
}
