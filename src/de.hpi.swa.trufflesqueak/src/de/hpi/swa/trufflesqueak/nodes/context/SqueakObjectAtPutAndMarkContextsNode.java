/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;

import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectAtPut0Node;

/**
 * This node should only be used for stores into associations, receivers, and remote temps as it
 * also marks {@link ContextObject}s as escaped when stored.
 */
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
    public void doWrite(final Object object, final Object value,
                    @Bind final Node node,
                    @Cached final SqueakObjectAtPut0Node atPut0Node,
                    @Cached final InlinedBranchProfile isContextObjectProfile) {
        if (value instanceof final ContextObject context) {
            isContextObjectProfile.enter(node);
            context.markEscaped();
        }
        atPut0Node.execute(node, object, index, value);
    }
}
