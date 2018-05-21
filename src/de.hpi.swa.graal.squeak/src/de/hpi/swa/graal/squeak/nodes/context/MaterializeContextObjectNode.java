package de.hpi.swa.graal.squeak.nodes.context;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.MaterializedFrame;

import de.hpi.swa.graal.squeak.exceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.FrameMarker;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithImage;
import de.hpi.swa.graal.squeak.nodes.CompiledCodeNodes.CalculcatePCOffsetNode;
import de.hpi.swa.graal.squeak.nodes.GetOrCreateContextNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameArgumentNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameSlotReadNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public abstract class MaterializeContextObjectNode extends AbstractNodeWithImage {
    @Child private FrameArgumentNode senderNode = FrameArgumentNode.create(FrameAccess.SENDER_OR_SENDER_MARKER);
    @Child private FrameArgumentNode methodNode = FrameArgumentNode.create(FrameAccess.METHOD);
    @Child private FrameArgumentNode closureNode = FrameArgumentNode.create(FrameAccess.CLOSURE_OR_NULL);
    @Child private FrameArgumentNode receiverNode = FrameArgumentNode.create(FrameAccess.RECEIVER);
    @Child private CalculcatePCOffsetNode pcOffsetNode = CalculcatePCOffsetNode.create();
    @Child private GetOrCreateContextNode contextNode;
    @Child private FrameSlotReadNode pcNode;
    @Child private FrameSlotReadNode spNode;

    public static MaterializeContextObjectNode create(final CompiledCodeObject code) {
        return MaterializeContextObjectNodeGen.create(code);
    }

    protected MaterializeContextObjectNode(final CompiledCodeObject code) {
        super(code.image);
        contextNode = GetOrCreateContextNode.create(code);
        pcNode = FrameSlotReadNode.create(code.instructionPointerSlot);
        spNode = FrameSlotReadNode.create(code.stackPointerSlot);
    }

    public abstract void execute(ContextObject context);

    @Specialization(guards = {"context.hasTruffleFrame()", "context.isDirty()", "!context.hasMaterializedSender()"})
    protected final void doSenderOnly(final ContextObject context) {
        fillInSender(context);
    }

    private void fillInSender(final ContextObject context) {
        final Object senderOrMarker = senderNode.executeRead(context.getTruffleFrame());
        if (senderOrMarker instanceof FrameMarker) {
            final Frame senderFrame = FrameAccess.findFrameForMarker((FrameMarker) senderOrMarker);
            if (senderFrame == null) {
                throw new SqueakException("Unable to find senderFrame for FrameMaker");
            }
            context.setSender(contextNode.executeGet(senderFrame, false));
        }
    }

    @Specialization(guards = {"context.hasTruffleFrame()", "!context.isDirty()"})
    protected final void doMaterialize(final ContextObject context) {
        if (!context.hasMaterializedSender()) {
            fillInSender(context);
        }
        final MaterializedFrame frame = context.getTruffleFrame();
        final Object blockOrMethod = methodNode.executeRead(frame);
        context.atput0(CONTEXT.METHOD, blockOrMethod);
        final long framePC = (int) pcNode.executeRead(frame);
        if (framePC >= 0) {
            context.setInstructionPointer(framePC + pcOffsetNode.execute(blockOrMethod));
        } else { // context has been terminated
            context.atput0(CONTEXT.INSTRUCTION_POINTER, image.nil);
        }
        context.setStackPointer((int) spNode.executeRead(frame) + 1); // frame sp is zero-based
        final Object closure = closureNode.executeRead(frame);
        context.atput0(CONTEXT.CLOSURE_OR_NIL, closure == null ? image.nil : closure);
        context.atput0(CONTEXT.RECEIVER, receiverNode.executeRead(frame));
    }

    @SuppressWarnings("unused")
    @Fallback
    protected final void doNothing(final ContextObject context) {
        // nothing to do
    }
}
