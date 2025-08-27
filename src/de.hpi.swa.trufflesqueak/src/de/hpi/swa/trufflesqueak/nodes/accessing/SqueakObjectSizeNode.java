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

import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CharacterObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.EmptyObject;
import de.hpi.swa.trufflesqueak.model.EphemeronObject;
import de.hpi.swa.trufflesqueak.model.FloatObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.VariablePointersObject;
import de.hpi.swa.trufflesqueak.model.WeakVariablePointersObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectInstSizeNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodes.ArrayObjectSizeNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.NativeObjectNodes.NativeObjectSizeNode;

@GenerateInline
@GenerateUncached
@GenerateCached(false)
public abstract class SqueakObjectSizeNode extends AbstractNode {

    public abstract int execute(Node node, Object obj);

    public static final int executeUncached(final Object obj) {
        return SqueakObjectSizeNodeGen.getUncached().execute(null, obj);
    }

    @Specialization
    protected static final int doNil(final NilObject obj) {
        return obj.size();
    }

    @Specialization
    protected static final int doEmpty(final EmptyObject obj) {
        return obj.size();
    }

    @Specialization
    protected static final int doNative(final Node node, final NativeObject obj,
                    @Cached final NativeObjectSizeNode sizeNode) {
        return sizeNode.execute(node, obj);
    }

    @Specialization
    protected static final int doArray(final Node node, final ArrayObject obj, @Cached final ArrayObjectSizeNode sizeNode) {
        return sizeNode.execute(node, obj);
    }

    @Specialization
    protected static final int doClass(final ClassObject obj) {
        return obj.size();
    }

    @Specialization
    protected static final int doContext(final ContextObject obj) {
        return obj.size();
    }

    @Specialization
    protected static final int doPointers(final Node node, final PointersObject obj,
                    @Shared("sizeNode") @Cached final AbstractPointersObjectInstSizeNode sizeNode) {
        return sizeNode.execute(node, obj);
    }

    @Specialization
    protected static final int doVariablePointers(final Node node, final VariablePointersObject obj,
                    @Shared("sizeNode") @Cached final AbstractPointersObjectInstSizeNode sizeNode) {
        return sizeNode.execute(node, obj) + obj.getVariablePartSize();
    }

    @Specialization
    protected static final int doWeakVariablePointers(final Node node, final WeakVariablePointersObject obj,
                    @Shared("sizeNode") @Cached final AbstractPointersObjectInstSizeNode sizeNode) {
        return sizeNode.execute(node, obj) + obj.getVariablePartSize();
    }

    @Specialization
    protected static final int doEphemeron(final Node node, final EphemeronObject obj,
                    @Shared("sizeNode") @Cached final AbstractPointersObjectInstSizeNode sizeNode) {
        return sizeNode.execute(node, obj);
    }

    @Specialization
    protected static final int doClosure(final BlockClosureObject obj) {
        return obj.size();
    }

    @Specialization
    protected static final int doCode(final CompiledCodeObject obj) {
        return obj.size();
    }

    @Specialization
    protected static final int doFloat(final FloatObject obj) {
        return obj.size();
    }

    @Specialization
    protected static final int doCharacterObject(final CharacterObject obj) {
        return obj.size();
    }
}
