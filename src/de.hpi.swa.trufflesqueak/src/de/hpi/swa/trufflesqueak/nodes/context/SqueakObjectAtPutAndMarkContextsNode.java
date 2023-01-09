/*
 * Copyright (c) 2017-2023 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2023 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectAtPut0Node;

/**
 * This node should only be used for stores into associations, receivers, and remote temps as it
 * also marks {@link ContextObject}s as escaped when stored.
 */
@NodeInfo(cost = NodeCost.NONE)
public abstract class SqueakObjectAtPutAndMarkContextsNode extends AbstractNode {
    private final long index;

    protected SqueakObjectAtPutAndMarkContextsNode(final long variableIndex) {
        index = variableIndex;
    }

    public static SqueakObjectAtPutAndMarkContextsNode create(final long index) {
        return SqueakObjectAtPutAndMarkContextsNodeGen.create(index);
    }

    public abstract void executeWrite(Object object, Object value);

    @Specialization
    protected final void doContext(final Object object, final ContextObject value,
                    @Shared("atPut0Node") @Cached final SqueakObjectAtPut0Node atPut0Node) {
        value.markEscaped();
        atPut0Node.execute(this, object, index, value);
    }

    @Specialization(guards = "!isContextObject(value)")
    protected final void doOther(final Object object, final Object value,
                    @Shared("atPut0Node") @Cached final SqueakObjectAtPut0Node atPut0Node) {
        atPut0Node.execute(this, object, index, value);
    }
}
