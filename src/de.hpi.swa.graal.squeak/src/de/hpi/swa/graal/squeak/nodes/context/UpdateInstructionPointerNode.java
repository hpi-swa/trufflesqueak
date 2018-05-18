package de.hpi.swa.graal.squeak.nodes.context;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithCode;
import de.hpi.swa.graal.squeak.nodes.CompiledCodeNodes.CalculcatePCOffsetNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameSlotWriteNode;

public abstract class UpdateInstructionPointerNode extends AbstractNodeWithCode {
    @Child private FrameSlotWriteNode instructionPointerWriteNode;
    @Child private CalculcatePCOffsetNode calculcatePCOffsetNode = CalculcatePCOffsetNode.create();

    public static UpdateInstructionPointerNode create(final CompiledCodeObject code) {
        return UpdateInstructionPointerNodeGen.create(code);
    }

    protected UpdateInstructionPointerNode(final CompiledCodeObject code) {
        super(code);
        instructionPointerWriteNode = FrameSlotWriteNode.create(code.instructionPointerSlot);
    }

    public abstract void executeUpdate(VirtualFrame frame, int value);

    @Specialization(guards = {"isVirtualized(frame)"})
    protected final void doUpdateVirtualized(final VirtualFrame frame, final int value) {
        instructionPointerWriteNode.executeWrite(frame, value);
    }

    @Specialization(guards = {"!isVirtualized(frame)"})
    protected final void doUpdate(final VirtualFrame frame, final int value) {
        final ContextObject context = getContext(frame);
        context.setInstructionPointer(value + calculcatePCOffsetNode.execute(context.getClosureOrMethod()));
    }
}
