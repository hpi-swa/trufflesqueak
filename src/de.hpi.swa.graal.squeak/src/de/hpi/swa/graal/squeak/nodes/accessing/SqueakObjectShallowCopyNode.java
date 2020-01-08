/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.accessing;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledBlockObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.EmptyObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.VariablePointersObject;
import de.hpi.swa.graal.squeak.model.WeakVariablePointersObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithImage;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectShallowCopyNode;
import de.hpi.swa.graal.squeak.nodes.accessing.NativeObjectNodes.NativeObjectShallowCopyNode;

public abstract class SqueakObjectShallowCopyNode extends AbstractNodeWithImage {

    protected SqueakObjectShallowCopyNode(final SqueakImageContext image) {
        super(image);
    }

    public static SqueakObjectShallowCopyNode create(final SqueakImageContext image) {
        return SqueakObjectShallowCopyNodeGen.create(image);
    }

    public final Object execute(final Object object) {
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
    protected static final CompiledMethodObject doMethod(final CompiledMethodObject receiver) {
        return receiver.shallowCopy();
    }

    @Specialization
    protected static final ContextObject doContext(final ContextObject receiver) {
        return receiver.shallowCopy();
    }

    @Specialization
    protected static final CompiledBlockObject doBlock(final CompiledBlockObject receiver) {
        return receiver.shallowCopy();
    }

    @Specialization
    protected static final NativeObject doNative(final NativeObject receiver,
                    @Cached final NativeObjectShallowCopyNode copyNode) {
        return copyNode.execute(receiver);
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
