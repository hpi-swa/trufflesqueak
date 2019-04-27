package de.hpi.swa.graal.squeak.nodes.process;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithCode;

public final class YieldProcessNode extends AbstractNodeWithCode {
    @Child private LinkProcessToListNode linkProcessToListNode;
    @Child private WakeHighestPriorityNode wakeHighestPriorityNode;

    private YieldProcessNode(final CompiledCodeObject code) {
        super(code);
    }

    public static YieldProcessNode create(final CompiledCodeObject image) {
        return new YieldProcessNode(image);
    }

    public void executeYield(final VirtualFrame frame, final PointersObject scheduler) {
        final PointersObject activeProcess = code.image.getActiveProcess();
        final long priority = (long) activeProcess.at0(PROCESS.PRIORITY);
        final ArrayObject processLists = (ArrayObject) scheduler.at0(PROCESS_SCHEDULER.PROCESS_LISTS);
        final PointersObject processList = (PointersObject) processLists.at0Object(priority - 1);
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
