/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.process;

import com.oracle.truffle.api.TruffleLogger;
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
import de.hpi.swa.graal.squeak.nodes.primitives.impl.ControlPrimitives;
import de.hpi.swa.graal.squeak.shared.SqueakLanguageConfig;

public abstract class ResumeProcessNode extends AbstractNodeWithCode {
    private static final TruffleLogger LOG = TruffleLogger.getLogger(SqueakLanguageConfig.ID, ControlPrimitives.class);

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
        final PointersObject currentProcess = code.image.getActiveProcess(pointersReadNode);
        putToSleepNode.executePutToSleep(currentProcess);
        final ContextObject thisContext = contextNode.executeGet(frame);
        LOG.fine(() -> logSwitch(newProcess, currentProcess, thisContext));
        thisContext.transferTo(pointersReadNode, pointersWriteNode, newProcess);
    }

    private String logSwitch(final PointersObject newProcess, final PointersObject currentProcess, final ContextObject thisContext) {
        final StringBuilder b = new StringBuilder();
        b.append("Switching from process @");
        b.append(currentProcess.hashCode());
        b.append(" with priority ");
        b.append(pointersReadNode.execute(currentProcess, PROCESS.PRIORITY));
        b.append(" and stack\n");
        thisContext.printSqMaterializedStackTraceOn(b);
        b.append("\n...to process @");
        b.append(newProcess.hashCode());
        b.append(" with priority ");
        b.append(pointersReadNode.execute(newProcess, PROCESS.PRIORITY));
        b.append(" and stack\n");
        final Object newContext = pointersReadNode.execute(newProcess, PROCESS.SUSPENDED_CONTEXT);
        ((ContextObject) newContext).printSqMaterializedStackTraceOn(b);
        return b.toString();
    }

    @Specialization(guards = {"!hasHigherPriority(newProcess)", "hasSuspendedContext(newProcess)"})
    protected final void doSleep(final PointersObject newProcess) {
        putToSleepNode.executePutToSleep(newProcess);
        LOG.fine(() -> logNoSwitch(newProcess));
    }

    private String logNoSwitch(final PointersObject newProcess) {
        final StringBuilder b = new StringBuilder();
        b.append("\nCannot resume process @");
        b.append(newProcess.hashCode());
        b.append(" with priority ");
        b.append(pointersReadNode.execute(newProcess, PROCESS.PRIORITY));
        b.append(" and stack\n");
        final Object newContext = pointersReadNode.execute(newProcess, PROCESS.SUSPENDED_CONTEXT);
        ((ContextObject) newContext).printSqMaterializedStackTraceOn(b);
        b.append("\n...because it hs a lower priority than the currently active process @");
        final PointersObject currentProcess = code.image.getActiveProcess(pointersReadNode);
        b.append(currentProcess.hashCode());
        b.append(" with priority ");
        b.append(pointersReadNode.execute(currentProcess, PROCESS.PRIORITY));
        return b.toString();
    }

    @Specialization(guards = "!hasSuspendedContext(newProcess)")
    protected final void doFail(@SuppressWarnings("unused") final PointersObject newProcess) {
        LOG.severe(() -> logFail(newProcess));
        throw PrimitiveFailed.GENERIC_ERROR;
    }

    private String logFail(final PointersObject newProcess) {
        final StringBuilder b = new StringBuilder();
        b.append("\nCannot resume process @");
        b.append(newProcess.hashCode());
        b.append(" with priority ");
        b.append(pointersReadNode.execute(newProcess, PROCESS.PRIORITY));
        b.append("\n...because it does not have a suspended context\nThe currently active process @");
        final PointersObject currentProcess = code.image.getActiveProcess(pointersReadNode);
        b.append(currentProcess.hashCode());
        b.append(" has priority ");
        b.append(pointersReadNode.execute(currentProcess, PROCESS.PRIORITY));
        return b.toString();
    }

    protected final boolean hasHigherPriority(final PointersObject newProcess) {
        return pointersReadNode.executeLong(newProcess, PROCESS.PRIORITY) > pointersReadNode.executeLong(code.image.getActiveProcess(pointersReadNode), PROCESS.PRIORITY);
    }

    protected final boolean hasSuspendedContext(final PointersObject newProcess) {
        return pointersReadNode.execute(newProcess, PROCESS.SUSPENDED_CONTEXT) instanceof ContextObject;
    }
}
