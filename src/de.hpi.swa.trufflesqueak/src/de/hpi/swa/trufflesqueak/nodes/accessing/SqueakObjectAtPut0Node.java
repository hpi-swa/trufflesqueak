/*
 * Copyright (c) 2017-2023 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2023 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.accessing;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;

import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.FloatObject;
import de.hpi.swa.trufflesqueak.model.LargeIntegerObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.VariablePointersObject;
import de.hpi.swa.trufflesqueak.model.WeakVariablePointersObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.VariablePointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.WeakVariablePointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodes.ArrayObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.BlockClosureObjectNodes.BlockClosureObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.ClassObjectNodes.ClassObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.ContextObjectNodes.ContextObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.NativeObjectNodes.NativeObjectWriteNode;

@ImportStatic(NativeObject.class)
public abstract class SqueakObjectAtPut0Node extends AbstractNode {

    @NeverDefault
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
    protected final void doLargeInteger(final LargeIntegerObject receiver, final long index, final long value) {
        receiver.setNativeAt0(getContext(), index, value);
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
