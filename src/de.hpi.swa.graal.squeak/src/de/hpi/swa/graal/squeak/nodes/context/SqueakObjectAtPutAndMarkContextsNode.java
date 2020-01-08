/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.context;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAtPut0Node;

/**
 * This node should only be used for stores into associations, receivers, and remote temps as it
 * also marks {@link ContextObject}s as escaped when stored.
 */
@NodeInfo(cost = NodeCost.NONE)
public abstract class SqueakObjectAtPutAndMarkContextsNode extends AbstractNode {
    @Child private SqueakObjectAtPut0Node atPut0Node = SqueakObjectAtPut0Node.create();
    private final long index;

    protected SqueakObjectAtPutAndMarkContextsNode(final long variableIndex) {
        index = variableIndex;
    }

    public static SqueakObjectAtPutAndMarkContextsNode create(final long index) {
        return SqueakObjectAtPutAndMarkContextsNodeGen.create(index);
    }

    public abstract void executeWrite(Object object, Object value);

    @Specialization
    protected final void doContext(final Object object, final ContextObject value) {
        value.markEscaped();
        atPut0Node.execute(object, index, value);
    }

    @Specialization(guards = {"!isContextObject(value)"})
    protected final void doSqueakObject(final Object object, final Object value) {
        atPut0Node.execute(object, index, value);
    }
}
