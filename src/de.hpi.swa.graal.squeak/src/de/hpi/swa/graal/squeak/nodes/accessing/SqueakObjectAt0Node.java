/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.accessing;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;

import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledBlockObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.VariablePointersObject;
import de.hpi.swa.graal.squeak.model.WeakVariablePointersObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.VariablePointersObjectReadNode;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.WeakVariablePointersObjectReadNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectReadNode;
import de.hpi.swa.graal.squeak.nodes.accessing.BlockClosureObjectNodes.BlockClosureObjectReadNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ClassObjectNodes.ClassObjectReadNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ContextObjectNodes.ContextObjectReadNode;
import de.hpi.swa.graal.squeak.nodes.accessing.NativeObjectNodes.NativeObjectReadNode;

public abstract class SqueakObjectAt0Node extends AbstractNode {

    public static SqueakObjectAt0Node create() {
        return SqueakObjectAt0NodeGen.create();
    }

    public abstract Object execute(Object obj, long index);

    @Specialization
    protected static final Object doArray(final ArrayObject obj, final long index,
                    @Cached final ArrayObjectReadNode readNode) {
        return readNode.execute(obj, index);
    }

    @Specialization
    protected static final Object doPointers(final PointersObject obj, final long index,
                    @Cached final AbstractPointersObjectReadNode readNode) {
        return readNode.execute(obj, (int) index);
    }

    @Specialization
    protected static final Object doVariablePointers(final VariablePointersObject obj, final long index,
                    @Cached final VariablePointersObjectReadNode readNode) {
        return readNode.execute(obj, (int) index);
    }

    @Specialization
    protected static final Object doClass(final ClassObject obj, final long index,
                    @Cached final ClassObjectReadNode readNode) {
        return readNode.execute(obj, index);
    }

    @Specialization
    protected static final Object doWeakPointersVariable(final WeakVariablePointersObject obj, final long index,
                    @Cached final WeakVariablePointersObjectReadNode readNode) {
        return readNode.execute(obj, (int) index);
    }

    @Specialization
    protected static final Object doNative(final NativeObject obj, final long index,
                    @Cached final NativeObjectReadNode readNode) {
        return readNode.execute(obj, index);
    }

    @Specialization
    protected static final Object doLargeInteger(final LargeIntegerObject obj, final long index) {
        return obj.getNativeAt0(index);
    }

    @Specialization
    protected static final Object doBlock(final CompiledBlockObject obj, final long index) {
        return obj.at0(index);
    }

    @Specialization
    protected static final Object doMethod(final CompiledMethodObject obj, final long index) {
        return obj.at0(index);
    }

    @Specialization
    protected static final Object doClosure(final BlockClosureObject obj, final long index,
                    @Cached final BlockClosureObjectReadNode readNode) {
        return readNode.execute(obj, index);
    }

    @Specialization
    protected static final Object doContext(final ContextObject obj, final long index,
                    @Cached final ContextObjectReadNode readNode) {
        return readNode.execute(obj, index);
    }

    @Specialization
    protected static final long doFloat(final FloatObject obj, final long index,
                    @Cached final BranchProfile indexZeroProfile,
                    @Cached final BranchProfile indexOneProfile) {
        if (index == 0) {
            indexZeroProfile.enter();
            return obj.getHigh();
        } else {
            assert index == 1 : "Unexpected index: " + index;
            indexOneProfile.enter();
            return obj.getLow();
        }
    }
}
