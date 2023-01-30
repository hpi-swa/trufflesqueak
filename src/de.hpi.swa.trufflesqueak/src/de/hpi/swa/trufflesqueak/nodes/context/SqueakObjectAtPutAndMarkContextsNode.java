/*
 * Copyright (c) 2017-2023 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2023 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.BranchProfile;

import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectAtPut0Node;

/**
 * This node should only be used for stores into associations, receivers, and remote temps as it
 * also marks {@link ContextObject}s as escaped when stored.
 */
@NodeInfo(cost = NodeCost.NONE)
public final class SqueakObjectAtPutAndMarkContextsNode extends AbstractNode {
    private final long index;
    private final BranchProfile isContextObjectProfile = BranchProfile.create();
    @Child private SqueakObjectAtPut0Node atPut0Node = SqueakObjectAtPut0Node.create();

    protected SqueakObjectAtPutAndMarkContextsNode(final long variableIndex) {
        index = variableIndex;
    }

    public static SqueakObjectAtPutAndMarkContextsNode create(final long index) {
        return new SqueakObjectAtPutAndMarkContextsNode(index);
    }

    public void executeWrite(final Object object, final Object value) {
        if (value instanceof final ContextObject context) {
            isContextObjectProfile.enter();
            context.markEscaped();
        }
        atPut0Node.execute(object, index, value);
    }
}
