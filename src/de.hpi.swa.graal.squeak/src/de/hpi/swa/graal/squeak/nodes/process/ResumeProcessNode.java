/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.process;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.PROCESS;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithCode;
import de.hpi.swa.graal.squeak.nodes.GetOrCreateContextNode;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;

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

    @Specialization(guards = {"hasHigherPriority(newProcess)", "hasSuspendedContext(newProcess)"})
    protected final void doTransferTo(final VirtualFrame frame, final PointersObject newProcess,
                    @Cached final AbstractPointersObjectWriteNode pointersWriteNode,
                    @Cached("create(code)") final GetOrCreateContextNode contextNode) {
        putToSleepNode.executePutToSleep(code.image.getActiveProcess(pointersReadNode));
        contextNode.executeGet(frame).transferTo(pointersReadNode, pointersWriteNode, newProcess);
    }

    @Specialization(guards = {"!hasHigherPriority(newProcess)", "hasSuspendedContext(newProcess)"})
    protected final void doSleep(final PointersObject newProcess) {
        putToSleepNode.executePutToSleep(newProcess);
    }

    @Specialization(guards = "!hasSuspendedContext(newProcess)")
    protected final static void doFail(@SuppressWarnings("unused") final PointersObject newProcess) {
        throw PrimitiveFailed.GENERIC_ERROR;
    }

    protected final boolean hasHigherPriority(final PointersObject newProcess) {
        return pointersReadNode.executeLong(newProcess, PROCESS.PRIORITY) > pointersReadNode.executeLong(code.image.getActiveProcess(pointersReadNode), PROCESS.PRIORITY);
    }

    protected final boolean hasSuspendedContext(final PointersObject newProcess) {
        return pointersReadNode.execute(newProcess, PROCESS.SUSPENDED_CONTEXT) instanceof ContextObject;
    }
}
