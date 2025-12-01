/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.accessing;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;

import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.EphemeronObject;
import de.hpi.swa.trufflesqueak.model.FloatObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.VariablePointersObject;
import de.hpi.swa.trufflesqueak.model.WeakVariablePointersObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.VariablePointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.WeakVariablePointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodes.ArrayObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.BlockClosureObjectNodes.BlockClosureObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.ClassObjectNodes.ClassObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.ContextObjectNodes.ContextObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.NativeObjectNodes.NativeObjectReadNode;

@GenerateInline
@GenerateUncached
@GenerateCached(true)
public abstract class SqueakObjectAt0Node extends AbstractNode {

    public abstract Object execute(Node node, Object obj, long index);

    public static final Object executeUncached(final Object obj, final long index) {
        return SqueakObjectAt0NodeGen.getUncached().execute(null, obj, index);
    }

    @Specialization
    protected static final Object doArray(final Node node, final ArrayObject obj, final long index,
                    @Cached final ArrayObjectReadNode readNode) {
        return readNode.execute(node, obj, index);
    }

    @Specialization
    protected static final Object doPointers(final Node node, final PointersObject obj, final long index,
                    @Shared("readNode") @Cached final AbstractPointersObjectReadNode readNode) {
        return readNode.execute(node, obj, index);
    }

    @Specialization
    protected static final Object doNative(final Node node, final NativeObject obj, final long index,
                    @Cached final NativeObjectReadNode readNode) {
        return readNode.execute(node, obj, index);
    }

    @Specialization
    protected static final Object doCode(final CompiledCodeObject obj, final long index) {
        return obj.at0(index);
    }

    @Specialization
    protected static final Object doClass(final Node node, final ClassObject obj, final long index,
                    @Cached final ClassObjectReadNode readNode) {
        return readNode.execute(node, obj, index);
    }

    @Specialization
    protected static final Object doVariablePointers(final Node node, final VariablePointersObject obj, final long index,
                    @Cached final VariablePointersObjectReadNode readNode) {
        return readNode.execute(node, obj, index);
    }

    @Specialization
    protected static final Object doWeakPointersVariable(final Node node, final WeakVariablePointersObject obj, final long index,
                    @Cached final WeakVariablePointersObjectReadNode readNode) {
        return readNode.execute(node, obj, index);
    }

    @Specialization
    protected static final Object doClosure(final Node node, final BlockClosureObject obj, final long index,
                    @Cached final BlockClosureObjectReadNode readNode) {
        return readNode.execute(node, obj, index);
    }

    @Specialization
    protected static final Object doContext(final Node node, final ContextObject obj, final long index,
                    @Cached final ContextObjectReadNode readNode) {
        return readNode.execute(node, obj, index);
    }

    @Specialization
    protected static final Object doEphemeron(final Node node, final EphemeronObject obj, final long index,
                    @Shared("readNode") @Cached final AbstractPointersObjectReadNode readNode) {
        return readNode.execute(node, obj, index);
    }

    @Specialization
    protected static final long doFloat(final Node node, final FloatObject obj, final long index,
                    @Cached final InlinedBranchProfile indexZeroProfile,
                    @Cached final InlinedBranchProfile indexOneProfile) {
        if (index == 0) {
            indexZeroProfile.enter(node);
            return obj.getHigh();
        } else {
            assert index == 1 : "Unexpected index: " + index;
            indexOneProfile.enter(node);
            return obj.getLow();
        }
    }
}
