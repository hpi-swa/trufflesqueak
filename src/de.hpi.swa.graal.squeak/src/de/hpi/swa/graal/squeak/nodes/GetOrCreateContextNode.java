package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.nodes.CompiledCodeNodes.CalculcatePCOffsetNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameArgumentNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameSlotReadNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameSlotWriteNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackReadNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public abstract class GetOrCreateContextNode extends AbstractNodeWithCode {
    @Child private static FrameStackReadNode frameStackReadNode = FrameStackReadNode.create();
    @Child private FrameArgumentNode methodNode = FrameArgumentNode.create(FrameAccess.METHOD);
    @Child private FrameArgumentNode closureNode = FrameArgumentNode.create(FrameAccess.CLOSURE_OR_NULL);
    @Child private FrameArgumentNode receiverNode = FrameArgumentNode.create(FrameAccess.RECEIVER);
    @Child private FrameArgumentNode senderNode = FrameArgumentNode.create(FrameAccess.SENDER_OR_SENDER_MARKER);
    @Child private CalculcatePCOffsetNode calcNode = CalculcatePCOffsetNode.create();
    @Child private FrameSlotReadNode pcNode;
    @Child private FrameSlotReadNode spNode;
    @Child private FrameSlotReadNode contextReadNode;
    @Child private FrameSlotWriteNode contextWriteNode;

    public static GetOrCreateContextNode create(final CompiledCodeObject code) {
        return GetOrCreateContextNodeGen.create(code);
    }

    protected GetOrCreateContextNode(final CompiledCodeObject code) {
        super(code);
        pcNode = FrameSlotReadNode.create(code.instructionPointerSlot);
        spNode = FrameSlotReadNode.create(code.stackPointerSlot);
        contextReadNode = FrameSlotReadNode.create(code.thisContextOrMarkerSlot);
        contextWriteNode = FrameSlotWriteNode.create(code.thisContextOrMarkerSlot);
    }

    public final ContextObject executeGet(final Frame frame, final boolean invalidateCanBeVirtualizedAssumption) {
        return executeGet(frame, invalidateCanBeVirtualizedAssumption, true);
    }

    public abstract ContextObject executeGet(Frame frame, boolean invalidateCanBeVirtualizedAssumption, boolean fullSenderChain);

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isFullyVirtualized(frame)"})
    protected ContextObject doGet(final VirtualFrame frame, final boolean invalidateCanBeVirtualizedAssumption, final boolean fullSenderChain) {
        return getContext(frame);
    }

    @Specialization(guards = {"isFullyVirtualized(frame)"})
    protected ContextObject doCreateLight(final VirtualFrame frame, final boolean invalidateCanBeVirtualizedAssumption, final boolean fullSenderChain) {
        final CompiledCodeObject method = (CompiledCodeObject) methodNode.executeRead(frame);
        final ContextObject context = ContextObject.create(method.image, method.frameSize(), frame.materialize());

        if (invalidateCanBeVirtualizedAssumption) {
            method.invalidateCanBeVirtualizedAssumption();
        }
        contextWriteNode.executeWrite(frame, context);
        if (fullSenderChain) {
            forceSenderChain(method, context);
        }

        return context;
    }

    @TruffleBoundary
    public static ContextObject getOrCreateFull(final MaterializedFrame frame, final boolean invalidateCanBeVirtualizedAssumption) {
        final Object contextOrMarker = FrameAccess.getContextOrMarker(frame);
        if (contextOrMarker instanceof ContextObject) {
            final ContextObject context = (ContextObject) contextOrMarker;
            final CompiledCodeObject method = context.getMethod();
            if (invalidateCanBeVirtualizedAssumption) {
                method.invalidateCanBeVirtualizedAssumption();
            }
            forceSenderChain(method, context);
            return context;
        } else {
            CompilerDirectives.transferToInterpreter();
            final CompiledCodeObject method = FrameAccess.getMethod(frame);
            final ContextObject context = ContextObject.create(method.image, method.frameSize(), frame);
            frame.setObject(method.thisContextOrMarkerSlot, context);
            if (invalidateCanBeVirtualizedAssumption) {
                method.invalidateCanBeVirtualizedAssumption();
            }
            forceSenderChain(method, context);
            return context;
        }
    }

    private static void forceSenderChain(final CompiledCodeObject method, final ContextObject context) {
        ContextObject current = context;
        while (true) {
            current.materialize(); // full!
            final AbstractSqueakObject next = current.getSender();
            if (next == method.image.nil) {
                break;
            }
            current = (ContextObject) next;
        }
    }
}
