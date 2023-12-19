/*
 * Copyright (c) 2017-2023 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2023 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.process;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetOrCreateContextNode;

@GenerateInline
@GenerateCached(false)
public abstract class ResumeProcessNode extends AbstractNode {

    public static void executeUncached(final VirtualFrame frame, final SqueakImageContext image, final PointersObject newProcess) {
        final AbstractPointersObjectReadNode readNode = AbstractPointersObjectReadNode.getUncached();
        final PointersObject activeProcess = image.getActiveProcessSlow();
        if (hasHigherPriority(newProcess, activeProcess, readNode, null)) {
            PutToSleepNode.executeUncached(image, activeProcess);
            final ContextObject newActiveContext = (ContextObject) readNode.execute(null, newProcess, PROCESS.SUSPENDED_CONTEXT);
            final AbstractPointersObjectWriteNode writeNode = AbstractPointersObjectWriteNode.getUncached();
            GetOrCreateContextNode.getOrCreateUncached(frame).transferTo(image, newProcess, activeProcess, newActiveContext, writeNode, null);
        } else {
            PutToSleepNode.executeUncached(image, newProcess);
        }
    }

    public abstract void executeResume(VirtualFrame frame, Node node, PointersObject newProcess);

    @Specialization(guards = "hasHigherPriority(newProcess, activeProcess, pointersReadNode, node)")
    protected static final void doTransferTo(final VirtualFrame frame, final Node node, final PointersObject newProcess,
                    @Shared("putToSleepNode") @Cached final PutToSleepNode putToSleepNode,
                    @Shared("pointersReadNode") @Cached final AbstractPointersObjectReadNode pointersReadNode,
                    @SuppressWarnings("unused") @Shared("getActiveProcessNode") @Cached final GetActiveProcessNode getActiveProcessNode,
                    @Bind("getActiveProcessNode.execute(node)") final PointersObject activeProcess,
                    @Cached final AbstractPointersObjectWriteNode pointersWriteNode,
                    @Cached final GetOrCreateContextNode contextNode) {
        putToSleepNode.executePutToSleep(node, activeProcess);
        final ContextObject newActiveContext = (ContextObject) pointersReadNode.execute(node, newProcess, PROCESS.SUSPENDED_CONTEXT);
        contextNode.executeGet(frame, node).transferTo(getContext(node), newProcess, activeProcess, newActiveContext, pointersWriteNode, node);
    }

    @Specialization(guards = "!hasHigherPriority(newProcess, getActiveProcessNode.execute(node), pointersReadNode, node)")
    protected static final void doSleep(final Node node, final PointersObject newProcess,
                    @SuppressWarnings("unused") @Shared("pointersReadNode") @Cached final AbstractPointersObjectReadNode pointersReadNode,
                    @SuppressWarnings("unused") @Shared("getActiveProcessNode") @Cached final GetActiveProcessNode getActiveProcessNode,
                    @Shared("putToSleepNode") @Cached final PutToSleepNode putToSleepNode) {
        putToSleepNode.executePutToSleep(node, newProcess);
    }

    protected static final boolean hasHigherPriority(final PointersObject newProcess, final PointersObject activeProcess, final AbstractPointersObjectReadNode readNode, final Node inlineTarget) {
        return readNode.executeLong(inlineTarget, newProcess, PROCESS.PRIORITY) > readNode.executeLong(inlineTarget, activeProcess, PROCESS.PRIORITY);
    }
}
