/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.process;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.PROCESS;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithCode;
import de.hpi.swa.graal.squeak.nodes.GetOrCreateContextNode;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.graal.squeak.util.DebugUtils;
import de.hpi.swa.graal.squeak.util.LogUtils;

public abstract class ResumeProcessNode extends AbstractNodeWithCode {
    @Child private AbstractPointersObjectReadNode pointersReadNode = AbstractPointersObjectReadNode.create();

    @Child private PutToSleepNode putToSleepNode;

    protected ResumeProcessNode(final CompiledCodeObject code) {
        super(code);
        putToSleepNode = PutToSleepNode.create(code.image);
    }

    public static ResumeProcessNode create(final CompiledCodeObject code) {
        return ResumeProcessNodeGen.create(code);
    }

    public abstract void executeResume(VirtualFrame frame, PointersObject newProcess);

    @Specialization(guards = "hasHigherPriority(newProcess)")
    protected final void doTransferTo(final VirtualFrame frame, final PointersObject newProcess,
                    @Cached final AbstractPointersObjectWriteNode pointersWriteNode,
                    @Cached("create(code)") final GetOrCreateContextNode contextNode) {
        final PointersObject currentProcess = code.image.getActiveProcess(pointersReadNode);
        putToSleepNode.executePutToSleep(currentProcess);
        final ContextObject thisContext = contextNode.executeGet(frame, currentProcess);
        LogUtils.SCHEDULING.fine(() -> DebugUtils.logSwitch(newProcess, (int) pointersReadNode.executeLong(newProcess, PROCESS.PRIORITY), currentProcess, thisContext,
                        (ContextObject) pointersReadNode.execute(newProcess, PROCESS.SUSPENDED_CONTEXT)));
        thisContext.transferTo(pointersReadNode, pointersWriteNode, newProcess);
    }

    @Specialization(guards = "!hasHigherPriority(newProcess)")
    protected final void doSleep(final PointersObject newProcess) {
        putToSleepNode.executePutToSleep(newProcess);
        LogUtils.SCHEDULING.fine(() -> DebugUtils.logNoSwitch(newProcess));
    }

    protected final boolean hasHigherPriority(final PointersObject newProcess) {
        return pointersReadNode.executeLong(newProcess, PROCESS.PRIORITY) > pointersReadNode.executeLong(code.image.getActiveProcess(pointersReadNode), PROCESS.PRIORITY);
    }
}
