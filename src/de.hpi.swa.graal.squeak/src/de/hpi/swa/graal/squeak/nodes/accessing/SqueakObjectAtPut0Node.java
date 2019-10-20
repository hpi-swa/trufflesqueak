/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.accessing;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;

import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.VariablePointersObject;
import de.hpi.swa.graal.squeak.model.WeakVariablePointersObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.VariablePointersObjectWriteNode;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.WeakVariablePointersObjectWriteNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectWriteNode;
import de.hpi.swa.graal.squeak.nodes.accessing.BlockClosureObjectNodes.BlockClosureObjectWriteNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ClassObjectNodes.ClassObjectWriteNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ContextObjectNodes.ContextObjectWriteNode;
import de.hpi.swa.graal.squeak.nodes.accessing.NativeObjectNodes.NativeObjectWriteNode;

@GenerateUncached
@ImportStatic(NativeObject.class)
public abstract class SqueakObjectAtPut0Node extends AbstractNode {

    public static SqueakObjectAtPut0Node create() {
        return SqueakObjectAtPut0NodeGen.create();
    }

    public abstract void execute(Object obj, long index, Object value);

    @Specialization
    protected static final void doNative(final NativeObject obj, final long index, final Object value,
                    @Cached final NativeObjectWriteNode writeNode) {
        writeNode.execute(obj, index, value);
    }

    @Specialization
    protected static final void doArray(final ArrayObject obj, final long index, final Object value,
                    @Cached final ArrayObjectWriteNode writeNode) {
        writeNode.execute(obj, index, value);
    }

    @Specialization
    protected static final void doPointers(final PointersObject obj, final long index, final Object value,
                    @Cached final AbstractPointersObjectWriteNode writeNode) {
        writeNode.execute(obj, (int) index, value);
    }

    @Specialization
    protected static final void doVariablePointers(final VariablePointersObject obj, final long index, final Object value,
                    @Cached final VariablePointersObjectWriteNode writeNode) {
        writeNode.execute(obj, (int) index, value);
    }

    @Specialization
    protected static final void doLargeInteger(final LargeIntegerObject receiver, final long index, final long value) {
        receiver.setNativeAt0(index, value);
    }

    @Specialization
    protected static final void doWeakPointers(final WeakVariablePointersObject obj, final long index, final Object value,
                    @Cached final WeakVariablePointersObjectWriteNode writeNode) {
        writeNode.execute(obj, (int) index, value);
    }

    @Specialization
    protected static final void doClass(final ClassObject obj, final long index, final Object value,
                    @Cached final ClassObjectWriteNode writeNode) {
        writeNode.execute(obj, index, value);
    }

    @Specialization
    protected static final void doCode(final CompiledCodeObject obj, final long index, final Object value) {
        obj.atput0(index, value);
    }

    @Specialization
    protected static final void doClosure(final BlockClosureObject obj, final long index, final Object value,
                    @Cached final BlockClosureObjectWriteNode writeNode) {
        writeNode.execute(obj, index, value);
    }

    @Specialization
    protected static final void doContext(final ContextObject obj, final long index, final Object value,
                    @Cached final ContextObjectWriteNode writeNode) {
        writeNode.execute(obj, index, value);
    }

    @Specialization
    protected static final void doFloat(final FloatObject obj, final long index, final long value,
                    @Cached final BranchProfile indexZeroProfile,
                    @Cached final BranchProfile indexOneProfile) {
        assert 0 <= value && value <= NativeObject.INTEGER_MAX : "`value` out of range";
        if (index == 0) {
            indexZeroProfile.enter();
            obj.setHigh(value);
        } else {
            assert index == 1 : "Unexpected index: " + index;
            indexOneProfile.enter();
            obj.setLow(value);
        }
    }
}
