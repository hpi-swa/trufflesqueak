/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.process;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithCode;
import de.hpi.swa.graal.squeak.nodes.GetOrCreateContextNode;

public abstract class ResumeProcessNode extends AbstractNodeWithCode {
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
                    @Cached("create(code)") final GetOrCreateContextNode contextNode) {
        putToSleepNode.executePutToSleep(code.image.getActiveProcess());
        contextNode.executeGet(frame).transferTo(newProcess);
    }

    @Fallback
    protected final void doSleep(final PointersObject newProcess) {
        putToSleepNode.executePutToSleep(newProcess);
    }

    protected final boolean hasHigherPriority(final PointersObject newProcess) {
        return (long) newProcess.at0(PROCESS.PRIORITY) > (long) code.image.getActiveProcess().at0(PROCESS.PRIORITY);
    }
}
