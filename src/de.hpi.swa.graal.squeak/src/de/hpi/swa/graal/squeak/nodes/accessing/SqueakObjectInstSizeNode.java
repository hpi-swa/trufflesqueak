/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.accessing;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.graal.squeak.model.AbstractPointersObject;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.EmptyObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;

@NodeInfo(cost = NodeCost.NONE)
public abstract class SqueakObjectInstSizeNode extends AbstractNode {

    public static SqueakObjectInstSizeNode create() {
        return SqueakObjectInstSizeNodeGen.create();
    }

    public abstract int execute(Object obj);

    @Specialization
    protected static final int doNil(final NilObject obj) {
        return obj.instsize();
    }

    @Specialization
    protected static final int doArray(final ArrayObject obj) {
        return obj.instsize();
    }

    @Specialization
    protected static final int doPointers(final AbstractPointersObject obj) {
        return obj.instsize();
    }

    @Specialization
    protected static final int doClass(final ClassObject obj) {
        return obj.instsize();
    }

    @Specialization
    protected static final int doContext(final ContextObject obj) {
        return obj.instsize();
    }

    @Specialization
    protected static final int doClosure(final BlockClosureObject obj) {
        return obj.instsize();
    }

    @Specialization
    protected static final int doCode(final CompiledCodeObject obj) {
        return obj.instsize();
    }

    @Specialization
    protected static final int doEmpty(final EmptyObject obj) {
        return obj.instsize();
    }

    @Specialization
    protected static final int doNative(final NativeObject obj) {
        return obj.instsize();
    }

    @Specialization
    protected static final int doFloat(final FloatObject obj) {
        return obj.instsize();
    }

    @Specialization
    protected static final int doLarge(final LargeIntegerObject obj) {
        return obj.instsize();
    }
}
