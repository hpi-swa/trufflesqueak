/*
 * Copyright (c) 2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.process;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;

@GenerateInline(false)
public abstract class GetNextActiveContextNode extends AbstractNode {

    public static GetNextActiveContextNode create() {
        return GetNextActiveContextNodeGen.create();
    }

    public abstract ContextObject execute();

    @Specialization
    protected static final ContextObject doHandle(
                    @Bind final Node node,
                    @Cached final GetActiveProcessNode getActiveProcessNode,
                    @Cached final AbstractPointersObjectReadNode readNode,
                    @Cached final AbstractPointersObjectWriteNode writeSuspendedContextNode,
                    @Cached final AbstractPointersObjectWriteNode writeListNode) {
        final PointersObject activeProcess = getActiveProcessNode.execute(node);
        final Object newActiveContextObject = readNode.execute(node, activeProcess, PROCESS.SUSPENDED_CONTEXT);
        if (!(newActiveContextObject instanceof final ContextObject newActiveContext)) {
            throw SqueakException.create("new process not runnable");
        }
        writeSuspendedContextNode.executeNil(node, activeProcess, PROCESS.SUSPENDED_CONTEXT);
        writeListNode.executeNil(node, activeProcess, PROCESS.LIST);
        assert !newActiveContext.isDead() : "Cannot switch to terminated context";
        return newActiveContext;
    }
}
