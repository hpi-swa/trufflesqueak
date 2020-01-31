/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.accessing;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

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
import de.hpi.swa.graal.squeak.nodes.AbstractNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectSizeNode;
import de.hpi.swa.graal.squeak.nodes.accessing.NativeObjectNodes.NativeObjectSizeNode;

@NodeInfo(cost = NodeCost.NONE)
public abstract class SqueakObjectSizeNode extends AbstractNode {

    public static SqueakObjectSizeNode create() {
        return SqueakObjectSizeNodeGen.create();
    }

    public abstract int execute(Object obj);

    @Specialization
    protected static final int doNil(final NilObject obj) {
        return obj.size();
    }

    @Specialization
    protected static final int doArray(final ArrayObject obj, @Cached final ArrayObjectSizeNode sizeNode) {
        return sizeNode.execute(obj);
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
    protected static final int doPointers(final PointersObject obj) {
        return obj.size();
    }

    @Specialization
    protected static final int doVariablePointers(final VariablePointersObject obj) {
        return obj.size();
    }

    @Specialization
    protected static final int doWeakVariablePointers(final WeakVariablePointersObject obj) {
        return obj.size();
    }

    @Specialization
    protected static final int doClosure(final BlockClosureObject obj) {
        return obj.size();
    }

    @Specialization
    protected static final int doMethod(final CompiledMethodObject obj) {
        return obj.size();
    }

    @Specialization
    protected static final int doBlock(final CompiledBlockObject obj) {
        return obj.size();
    }

    @Specialization
    protected static final int doEmpty(final EmptyObject obj) {
        return obj.size();
    }

    @Specialization
    protected static final int doNative(final NativeObject obj, @Cached final NativeObjectSizeNode sizeNode) {
        return sizeNode.execute(obj);
    }

    @Specialization
    protected static final int doFloat(final FloatObject obj) {
        return obj.size();
    }

    @Specialization
    protected static final int doLargeInteger(final LargeIntegerObject obj) {
        return obj.size();
    }
}
