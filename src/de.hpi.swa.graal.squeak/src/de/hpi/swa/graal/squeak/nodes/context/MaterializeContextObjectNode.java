package de.hpi.swa.graal.squeak.nodes.context;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;

import de.hpi.swa.graal.squeak.exceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.FrameMarker;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithImage;
import de.hpi.swa.graal.squeak.nodes.GetOrCreateContextNode;
import de.hpi.swa.graal.squeak.nodes.accessing.CompiledCodeNodes.CalculcatePCOffsetNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameArgumentNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameSlotReadNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public abstract class MaterializeContextObjectNode extends AbstractNodeWithImage {
    @Child private FrameArgumentNode senderNode = FrameArgumentNode.create(FrameAccess.SENDER_OR_SENDER_MARKER);
    @Child private FrameArgumentNode methodNode = FrameArgumentNode.create(FrameAccess.METHOD);
    @Child private FrameArgumentNode closureNode = FrameArgumentNode.create(FrameAccess.CLOSURE_OR_NULL);
    @Child private FrameArgumentNode receiverNode = FrameArgumentNode.create(FrameAccess.RECEIVER);
    @Child private CalculcatePCOffsetNode pcOffsetNode = CalculcatePCOffsetNode.create();
    @Child private FrameSlotReadNode instructionPointerReadNode = FrameSlotReadNode.createForInstructionPointer();
    @Child private FrameSlotReadNode stackPointerReadNode = FrameSlotReadNode.createForStackPointer();
    @Child private GetOrCreateContextNode contextNode = GetOrCreateContextNode.create();

    public static MaterializeContextObjectNode create(final CompiledCodeObject code) {
        return MaterializeContextObjectNodeGen.create(code);
    }

    protected MaterializeContextObjectNode(final CompiledCodeObject code) {
        super(code.image);
    }

    public abstract void execute(ContextObject context);

    @Specialization(guards = {"context.hasTruffleFrame()", "!context.hasMaterializedSender()"})
    protected final void doSenderOnly(final ContextObject context) {
        final Object senderOrMarker = senderNode.executeRead(context.getTruffleFrame());
        final Frame senderFrame = FrameAccess.findFrameForMarker((FrameMarker) senderOrMarker);
        if (senderFrame == null) {
            throw new SqueakException("Unable to find senderFrame for FrameMaker");
        }
        context.setSender(contextNode.executeGet(senderFrame, false));
    }

    @SuppressWarnings("unused")
    @Fallback
    protected final void doNothing(final ContextObject context) {
        // nothing to do
    }
}
