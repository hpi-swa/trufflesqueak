/*
 * Copyright (c) 2017-2023 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2023 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.accessing;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObjectLibrary;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.EmptyObject;
import de.hpi.swa.trufflesqueak.model.FloatObject;
import de.hpi.swa.trufflesqueak.model.LargeIntegerObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.VariablePointersObject;
import de.hpi.swa.trufflesqueak.model.WeakVariablePointersObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodes.ArrayObjectShallowCopyNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.NativeObjectNodes.NativeObjectShallowCopyNode;

public abstract class SqueakObjectShallowCopyNode extends AbstractNode {

    public final Object execute(final SqueakImageContext image, final Object object) {
        CompilerAsserts.partialEvaluationConstant(image);
        return image.reportAllocation(executeAllocation(object));
    }

    protected abstract Object executeAllocation(Object obj);

    @Specialization
    protected static final NilObject doNil(final NilObject receiver) {
        return receiver;
    }

    @Specialization
    protected static final EmptyObject doEmpty(final EmptyObject receiver) {
        return receiver.shallowCopy();
    }

    @Specialization
    protected static final PointersObject doPointers(final PointersObject receiver,
                    @CachedLibrary(limit = "3") final DynamicObjectLibrary lib) {
        return receiver.shallowCopy(lib);
    }

    @Specialization
    protected static final VariablePointersObject doVariablePointers(final VariablePointersObject receiver,
                    @CachedLibrary(limit = "3") final DynamicObjectLibrary lib) {
        return receiver.shallowCopy(lib);
    }

    @Specialization
    protected static final WeakVariablePointersObject doWeakPointers(final WeakVariablePointersObject receiver,
                    @CachedLibrary(limit = "3") final DynamicObjectLibrary lib) {
        return receiver.shallowCopy(lib);
    }

    @Specialization
    protected static final ArrayObject doArray(final ArrayObject receiver,
                    @Cached final ArrayObjectShallowCopyNode copyNode) {
        return copyNode.execute(receiver);
    }

    @Specialization
    protected static final LargeIntegerObject doLargeInteger(final LargeIntegerObject receiver) {
        return receiver.shallowCopy();
    }

    @Specialization
    protected static final FloatObject doFloat(final FloatObject receiver) {
        return receiver.shallowCopy();
    }

    @Specialization
    protected static final BlockClosureObject doClosure(final BlockClosureObject receiver) {
        return receiver.shallowCopy();
    }

    @Specialization
    protected static final CompiledCodeObject doCode(final CompiledCodeObject receiver) {
        return receiver.shallowCopy();
    }

    @Specialization
    protected static final ContextObject doContext(final ContextObject receiver) {
        return receiver.shallowCopy();
    }

    @Specialization
    protected static final NativeObject doNative(final NativeObject receiver,
                    @Cached final NativeObjectShallowCopyNode copyNode) {
        return copyNode.execute(receiver);
    }

    @Specialization(guards = "!receiver.hasInstanceVariables()")
    protected static final ClassObject doClassNoInstanceVariables(final ClassObject receiver,
                    @CachedLibrary(limit = "3") final DynamicObjectLibrary lib) {
        return receiver.shallowCopy(lib, null);
    }

    @Specialization(guards = "receiver.hasInstanceVariables()")
    protected static final ClassObject doClass(final ClassObject receiver,
                    @CachedLibrary(limit = "3") final DynamicObjectLibrary lib,
                    @Cached final ArrayObjectShallowCopyNode arrayCopyNode) {
        return receiver.shallowCopy(lib, arrayCopyNode.execute(receiver.getInstanceVariablesOrNull()));
    }
}
