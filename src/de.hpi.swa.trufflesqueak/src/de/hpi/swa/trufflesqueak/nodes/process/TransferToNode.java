/*
 * Copyright (c) 2023-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2023-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.process;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.context.GetOrCreateContextWithFrameNode;

/**
 * Record a Process to be awakened on the next interpreter cycle. Suspends the active Context and
 * returns the new active Context.
 */
@GenerateInline
@GenerateCached(false)
public abstract class TransferToNode extends AbstractNode {
    private static final AbstractPointersObjectReadNode READ_NODE = AbstractPointersObjectReadNode.getUncached();
    private static final AbstractPointersObjectWriteNode WRITE_NODE = AbstractPointersObjectWriteNode.getUncached();

    public abstract void execute(VirtualFrame frame, Node node, PointersObject newProcess);

    public static final void executeUncached(final VirtualFrame frame, final PointersObject newProcess) {
        final PointersObject scheduler = getContext(null).getScheduler();
        final PointersObject oldProcess = READ_NODE.executePointers(null, scheduler, PROCESS_SCHEDULER.ACTIVE_PROCESS);
        WRITE_NODE.execute(null, scheduler, PROCESS_SCHEDULER.ACTIVE_PROCESS, newProcess);
        final ContextObject activeContext = GetOrCreateContextWithFrameNode.executeUncached(frame);
        WRITE_NODE.execute(null, oldProcess, PROCESS.SUSPENDED_CONTEXT, activeContext);
    }

    @Specialization
    protected static final void transferTo(final VirtualFrame frame, final Node node, final PointersObject newProcess,
                    @Cached final GetOrCreateContextWithFrameNode contextNode,
                    @Cached final AbstractPointersObjectReadNode readOldProcessNode,
                    @Cached final AbstractPointersObjectWriteNode writeActiveProcessNode,
                    @Cached final AbstractPointersObjectWriteNode writeSuspendedContextNode) {
        final PointersObject scheduler = getContext(node).getScheduler();
        final PointersObject oldProcess = readOldProcessNode.executePointers(node, scheduler, PROCESS_SCHEDULER.ACTIVE_PROCESS);
        writeActiveProcessNode.execute(node, scheduler, PROCESS_SCHEDULER.ACTIVE_PROCESS, newProcess);
        writeSuspendedContextNode.execute(node, oldProcess, PROCESS.SUSPENDED_CONTEXT, contextNode.executeGet(frame, node));
    }
}
