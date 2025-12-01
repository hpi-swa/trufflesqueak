/*
 * Copyright (c) 2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;

@GenerateInline(false)
abstract class CreateDoesNotUnderstandMessageNode extends AbstractNode {
    public abstract PointersObject execute(NativeObject selector, Object receiver, Object[] arguments);

    @Specialization
    protected static final PointersObject doCreate(final NativeObject selector, final Object receiver, final Object[] arguments,
                    @Bind final Node node,
                    @Cached final AbstractPointersObjectWriteNode writeNode,
                    @Cached(inline = true) final SqueakObjectClassNode classNode) {
        final ClassObject receiverClass = classNode.executeLookup(node, receiver);
        return getContext(node).newMessage(writeNode, node, selector, receiverClass, arguments);
    }
}
