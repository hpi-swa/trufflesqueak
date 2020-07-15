/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.accessing;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;

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

public abstract class SqueakObjectShallowCopyNode extends AbstractNode {

    public final Object execute(final SqueakImageContext image, final Object object) {
        CompilerAsserts.partialEvaluationConstant(image);
        image.reportNewAllocationRequest();
        return image.reportNewAllocationResult(executeAllocation(object));
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
    protected static final PointersObject doPointers(final PointersObject receiver) {
        return receiver.shallowCopy();
    }

    @Specialization
    protected static final VariablePointersObject doVariablePointers(final VariablePointersObject receiver) {
        return receiver.shallowCopy();
    }

    @Specialization
    protected static final WeakVariablePointersObject doWeakPointers(final WeakVariablePointersObject receiver) {
        return receiver.shallowCopy();
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
    protected static final NativeObject doNative(final NativeObject receiver) {
        return receiver.shallowCopy();
    }

    @Specialization(guards = "!receiver.hasInstanceVariables()")
    protected static final ClassObject doClassNoInstanceVariables(final ClassObject receiver) {
        return receiver.shallowCopy(null);
    }

    @Specialization(guards = "receiver.hasInstanceVariables()")
    protected static final ClassObject doClass(final ClassObject receiver,
                    @Cached final ArrayObjectShallowCopyNode arrayCopyNode) {
        return receiver.shallowCopy(arrayCopyNode.execute(receiver.getInstanceVariablesOrNull()));
    }
}
