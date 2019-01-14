package de.hpi.swa.graal.squeak.nodes.context;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithCode;
import de.hpi.swa.graal.squeak.nodes.accessing.CompiledCodeNodes.CalculcatePCOffsetNode;

public abstract class UpdateInstructionPointerNode extends AbstractNodeWithCode {
    @Child private CalculcatePCOffsetNode calculcatePCOffsetNode = CalculcatePCOffsetNode.create();

    protected UpdateInstructionPointerNode(final CompiledCodeObject code) {
        super(code);
    }

    public static UpdateInstructionPointerNode create(final CompiledCodeObject code) {
        return UpdateInstructionPointerNodeGen.create(code);
    }

    public abstract void executeUpdate(VirtualFrame frame, int value);

    @Specialization(guards = {"isVirtualized(frame)"})
    protected final void doUpdateVirtualized(final VirtualFrame frame, final int value) {
        frame.setInt(code.instructionPointerSlot, value);
    }

    @Fallback
    protected final void doUpdate(final VirtualFrame frame, final int value) {
        final ContextObject context = getContext(frame);
        context.setInstructionPointer(value + calculcatePCOffsetNode.execute(context.getClosureOrMethod()));
    }
}
