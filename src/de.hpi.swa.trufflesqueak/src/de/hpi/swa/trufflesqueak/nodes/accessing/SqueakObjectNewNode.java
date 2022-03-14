/*
 * Copyright (c) 2017-2022 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2022 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.accessing;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObjectWithClassAndHash;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.EmptyObject;
import de.hpi.swa.trufflesqueak.model.FloatObject;
import de.hpi.swa.trufflesqueak.model.LargeIntegerObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.VariablePointersObject;
import de.hpi.swa.trufflesqueak.model.WeakVariablePointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayout;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.METACLASS;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;

@GenerateUncached
public abstract class SqueakObjectNewNode extends AbstractNode {
    public static final int NEW_CACHE_SIZE = 6;

    public static SqueakObjectNewNode create() {
        return SqueakObjectNewNodeGen.create();
    }

    public static SqueakObjectNewNode getUncached() {
        return SqueakObjectNewNodeGen.getUncached();
    }

    public final AbstractSqueakObjectWithClassAndHash execute(final SqueakImageContext image, final ClassObject classObject) {
        return execute(image, classObject, 0);
    }

    public final AbstractSqueakObjectWithClassAndHash execute(final SqueakImageContext image, final ClassObject classObject, final int extraSize) {
        CompilerAsserts.partialEvaluationConstant(image);
        return image.reportAllocation(executeAllocation(image, classObject, extraSize));
    }

    protected abstract AbstractSqueakObjectWithClassAndHash executeAllocation(SqueakImageContext image, ClassObject classObject, int extraSize);

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

    @Specialization(guards = {"classObject.isNonIndexableWithInstVars()", "!image.isMetaClass(classObject)", "!classObject.instancesAreClasses()",
                    "classObject.getLayout() == cachedLayout"}, assumptions = "cachedLayout.getValidAssumption()", limit = "NEW_CACHE_SIZE")
    protected static final PointersObject doPointers(final SqueakImageContext image, final ClassObject classObject, final int extraSize,
                    @Cached(value = "classObject.getLayout()", allowUncached = true) final ObjectLayout cachedLayout) {
        assert extraSize == 0;
        return new PointersObject(image, classObject, cachedLayout);
    }

    @TruffleBoundary
    @Specialization(guards = {"classObject.isNonIndexableWithInstVars()", "!image.isMetaClass(classObject)", "!classObject.instancesAreClasses()"})
    protected static final PointersObject doPointersFallback(final SqueakImageContext image, final ClassObject classObject, final int extraSize) {
        assert extraSize == 0;
        return new PointersObject(image, classObject);
    }

    @Specialization(guards = "classObject.isIndexableWithNoInstVars()")
    protected static final ArrayObject doIndexedPointers(final SqueakImageContext image, final ClassObject classObject, final int extraSize) {
        assert classObject.getBasicInstanceSize() == 0;
        if (image.options.enableStorageStrategies) {
            return ArrayObject.createEmptyStrategy(image, classObject, extraSize);
        } else {
            return ArrayObject.createObjectStrategy(image, classObject, extraSize);
        }
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

    @Specialization(guards = {"classObject.isIndexableWithInstVars()", "!image.isMethodContextClass(classObject)", "!image.isBlockClosureClass(classObject)",
                    "!image.isFullBlockClosureClass(classObject)", "classObject.getLayout() == cachedLayout"}, assumptions = "cachedLayout.getValidAssumption()", limit = "NEW_CACHE_SIZE")
    protected static final VariablePointersObject doVariablePointers(final SqueakImageContext image, final ClassObject classObject, final int extraSize,
                    @Cached(value = "classObject.getLayout()", allowUncached = true) final ObjectLayout cachedLayout) {
        return new VariablePointersObject(image, classObject, cachedLayout, extraSize);
    }

    @TruffleBoundary
    @Specialization(guards = {"classObject.isIndexableWithInstVars()", "!image.isMethodContextClass(classObject)", "!image.isBlockClosureClass(classObject)"})
    protected static final VariablePointersObject doVariablePointersFallback(final SqueakImageContext image, final ClassObject classObject, final int extraSize) {
        return new VariablePointersObject(image, classObject, extraSize);
    }

    @Specialization(guards = {"classObject.isWeak()", "classObject.getLayout() == cachedLayout"}, assumptions = "cachedLayout.getValidAssumption()", limit = "NEW_CACHE_SIZE")
    protected static final WeakVariablePointersObject doWeakPointers(final SqueakImageContext image, final ClassObject classObject, final int extraSize,
                    @Cached(value = "classObject.getLayout()", allowUncached = true) final ObjectLayout cachedLayout) {
        return new WeakVariablePointersObject(image, classObject, cachedLayout, extraSize);
    }

    @TruffleBoundary
    @Specialization(guards = "classObject.isWeak()")
    protected static final WeakVariablePointersObject doWeakPointersFallback(final SqueakImageContext image, final ClassObject classObject, final int extraSize) {
        return new WeakVariablePointersObject(image, classObject, extraSize);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "classObject.isEphemeronClassType()")
    protected static final WeakVariablePointersObject doEphemerons(final SqueakImageContext image, final ClassObject classObject, final int extraSize) {
        throw SqueakException.create("Ephemerons not (yet) supported");
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

    @Specialization(guards = {"classObject.isBytes()", "image.isLargeIntegerClass(classObject)"})
    protected static final LargeIntegerObject doLargeIntegers(final SqueakImageContext image, final ClassObject classObject, final int extraSize) {
        assert classObject.getBasicInstanceSize() == 0;
        return new LargeIntegerObject(image, classObject, extraSize);
    }

    @Specialization(guards = {"classObject.isBytes()", "!image.isLargeIntegerClass(classObject)"})
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
