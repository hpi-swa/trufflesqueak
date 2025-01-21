/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.accessing;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
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

@GenerateInline
@GenerateCached(false)
@ImportStatic(NativeObject.class)
public abstract class SqueakObjectAtPut0Node extends AbstractNode {

    public abstract void execute(Node node, Object obj, long index, Object value);

    @Specialization
    protected static final void doNative(final Node node, final NativeObject obj, final long index, final Object value,
                    @Cached final NativeObjectWriteNode writeNode) {
        writeNode.execute(node, obj, index, value);
    }

    @Specialization
    protected static final void doArray(final Node node, final ArrayObject obj, final long index, final Object value,
                    @Cached final ArrayObjectWriteNode writeNode) {
        writeNode.execute(node, obj, index, value);
    }

    @Specialization
    protected static final void doPointers(final Node node, final PointersObject obj, final long index, final Object value,
                    @Cached final AbstractPointersObjectWriteNode writeNode) {
        writeNode.execute(node, obj, (int) index, value);
    }

    @Specialization
    protected static final void doVariablePointers(final Node node, final VariablePointersObject obj, final long index, final Object value,
                    @Cached final VariablePointersObjectWriteNode writeNode) {
        writeNode.execute(node, obj, (int) index, value);
    }

    @Specialization
    protected static final void doLargeInteger(final LargeIntegerObject receiver, final long index, final long value,
                    @Bind final SqueakImageContext image) {
        receiver.setNativeAt0(image, index, value);
    }

    @Specialization
    protected static final void doWeakPointers(final Node node, final WeakVariablePointersObject obj, final long index, final Object value,
                    @Cached final WeakVariablePointersObjectWriteNode writeNode) {
        writeNode.execute(node, obj, (int) index, value);
    }

    @Specialization
    protected static final void doClass(final Node node, final ClassObject obj, final long index, final Object value,
                    @Cached final ClassObjectWriteNode writeNode) {
        writeNode.execute(node, obj, index, value);
    }

    @Specialization
    protected static final void doCode(final CompiledCodeObject obj, final long index, final Object value) {
        obj.atput0(index, value);
    }

    @Specialization
    protected static final void doClosure(final Node node, final BlockClosureObject obj, final long index, final Object value,
                    @Cached final BlockClosureObjectWriteNode writeNode) {
        writeNode.execute(node, obj, index, value);
    }

    @Specialization
    protected static final void doContext(final Node node, final ContextObject obj, final long index, final Object value,
                    @Cached final ContextObjectWriteNode writeNode) {
        writeNode.execute(node, obj, index, value);
    }

    @Specialization
    protected static final void doFloat(final Node node, final FloatObject obj, final long index, final long value,
                    @Cached final InlinedBranchProfile indexZeroProfile,
                    @Cached final InlinedBranchProfile indexOneProfile) {
        assert 0 <= value && value <= NativeObject.INTEGER_MAX : "`value` out of range";
        if (index == 0) {
            indexZeroProfile.enter(node);
            obj.setHigh(value);
        } else {
            assert index == 1 : "Unexpected index: " + index;
            indexOneProfile.enter(node);
            obj.setLow(value);
        }
    }
}
