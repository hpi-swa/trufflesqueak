/*
 * Copyright (c) 2017-2024 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2024 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.GenerateUncached;
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
@GenerateInline
@GenerateUncached
@GenerateCached(false)
public abstract class SqueakObjectAtPutAndMarkContextsWithIndexNode extends AbstractNode {

    public abstract void executeWrite(Node node, Object object, int index, Object value);

    @Specialization
    protected static final void doWrite(final Node node, final Object object, final int index, final Object value,
                    @Cached final SqueakObjectAtPut0Node atPut0Node,
                    @Cached final InlinedBranchProfile isContextObjectProfile) {
        if (value instanceof final ContextObject context) {
            isContextObjectProfile.enter(node);
            context.markEscaped();
        }
        atPut0Node.execute(node, object, index, value);
    }
}
