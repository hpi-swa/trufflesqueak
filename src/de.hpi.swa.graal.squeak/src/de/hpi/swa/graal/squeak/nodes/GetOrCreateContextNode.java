package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.FrameMarker;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameArgumentNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameSlotWriteNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public abstract class GetOrCreateContextNode extends AbstractNode {
    @Child private FrameArgumentNode methodNode = FrameArgumentNode.create(FrameAccess.METHOD);
    @Child private FrameSlotWriteNode contextWriteNode = FrameSlotWriteNode.createForContextOrMarker();

    public static GetOrCreateContextNode create() {
        return GetOrCreateContextNodeGen.create();
    }

    public abstract ContextObject executeGet(Frame frame, boolean fullSenderChain);

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isFullyVirtualized(frame)"})
    protected final ContextObject doGet(final VirtualFrame frame, final boolean fullSenderChain) {
        return getContext(frame);
    }

    @Specialization(guards = {"isFullyVirtualized(frame)"})
    protected final ContextObject doCreateLight(final VirtualFrame frame, final boolean fullSenderChain) {
        final CompiledCodeObject method = (CompiledCodeObject) methodNode.executeRead(frame);
        final ContextObject context = ContextObject.create(method.image, method.frameSize(), frame.materialize(), getFrameMarker(frame));
        contextWriteNode.executeWrite(frame, context);
        if (fullSenderChain) {
            forceSenderChain(method, context);
        }

        return context;
    }

    public static final ContextObject getOrCreateFull(final MaterializedFrame frame, final boolean fullSenderChain) {
        final Object contextOrMarker = FrameAccess.getContextOrMarker(frame);
        final ContextObject context;
        final CompiledCodeObject method;
        if (contextOrMarker instanceof ContextObject) {
            context = (ContextObject) contextOrMarker;
            method = context.getMethod();
        } else {
            method = (CompiledCodeObject) frame.getArguments()[FrameAccess.METHOD];
            context = ContextObject.create(method.image, method.frameSize(), frame, (FrameMarker) contextOrMarker);
            frame.setObject(CompiledCodeObject.thisContextOrMarkerSlot, context);
        }
        if (fullSenderChain) {
            forceSenderChain(method, context);
        }
        return context;
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
