/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.process;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithCode;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectReadNode;

public final class YieldProcessNode extends AbstractNodeWithCode {
    @Child private LinkProcessToListNode linkProcessToListNode;
    @Child private WakeHighestPriorityNode wakeHighestPriorityNode;
    @Child private ArrayObjectReadNode arrayReadNode = ArrayObjectReadNode.create();

    private YieldProcessNode(final CompiledCodeObject code) {
        super(code);
    }

    public static YieldProcessNode create(final CompiledCodeObject image) {
        return new YieldProcessNode(image);
    }

    public void executeYield(final VirtualFrame frame, final PointersObject scheduler) {
        assert scheduler == code.image.getScheduler();
        final PointersObject activeProcess = code.image.getActiveProcess();
        final long priority = activeProcess.getPriority();
        final ArrayObject processLists = code.image.getProcessLists();
        final PointersObject processList = (PointersObject) arrayReadNode.execute(processLists, priority - 1);
        if (!processList.isEmptyList()) {
            getLinkProcessToListNode().executeLink(activeProcess, processList);
            getWakeHighestPriorityNode().executeWake(frame);
        }
    }

    private LinkProcessToListNode getLinkProcessToListNode() {
        if (linkProcessToListNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            linkProcessToListNode = insert(LinkProcessToListNode.create());
        }
        return linkProcessToListNode;
    }

    private WakeHighestPriorityNode getWakeHighestPriorityNode() {
        if (wakeHighestPriorityNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            wakeHighestPriorityNode = insert(WakeHighestPriorityNode.create(code));
        }
        return wakeHighestPriorityNode;
    }
}
