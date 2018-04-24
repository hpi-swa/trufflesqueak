package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackReadNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;
import de.hpi.swa.graal.squeak.util.FrameMarker;

@ImportStatic(FrameAccess.class)
public abstract class GetOrCreateContextNode extends AbstractNodeWithCode {
    @Child private static FrameStackReadNode frameStackReadNode = FrameStackReadNode.create();

    public static GetOrCreateContextNode create(final CompiledCodeObject code) {
        return GetOrCreateContextNodeGen.create(code);
    }

    protected GetOrCreateContextNode(final CompiledCodeObject code) {
        super(code);
    }

    public final ContextObject executeGet(final Frame frame) {
        return executeGet(frame, true);  // TODO: only force context when necessary
    }

    public abstract ContextObject executeGet(Frame frame, boolean forceContext);

    @Specialization(guards = {"isVirtualized(frame)"})
    protected ContextObject doCreateVirtualized(final VirtualFrame frame, final boolean forceContext) {
        return materialize(frame, forceContext);
    }

    public static ContextObject getOrCreate(final Frame frame) {
        final Object contextOrMarker = FrameAccess.getContextOrMarker(frame);
        if (contextOrMarker instanceof ContextObject) {
            return (ContextObject) contextOrMarker;
        } else {
            return materialize(frame, true); // TODO: only force context when necessary
        }
    }

    private static ContextObject materialize(final Frame frame, final boolean forceContext) {
        CompilerDirectives.transferToInterpreter();
        final CompiledCodeObject method = FrameAccess.getMethod(frame);
        final FrameMarker frameMarker = (FrameMarker) FrameAccess.getContextOrMarker(frame);
        final ContextObject context = ContextObject.create(method.image, method.frameSize(), frameMarker);

        context.setSender(FrameAccess.getSender(frame));
        final long framePC = FrameUtil.getLongSafe(frame, method.instructionPointerSlot);
        final long frameSP = FrameUtil.getLongSafe(frame, method.stackPointerSlot);
        context.atput0(CONTEXT.METHOD, method);
        if (framePC >= 0) {
            context.setInstructionPointer(framePC);
        } else { // context has been terminated
            context.atput0(CONTEXT.INSTRUCTION_POINTER, method.image.nil);
        }
        context.setStackPointer(frameSP + 1); // frame sp is zero-based
        final BlockClosureObject closure = FrameAccess.getClosure(frame);
        context.atput0(CONTEXT.CLOSURE_OR_NIL, closure == null ? method.image.nil : closure);
        context.atput0(CONTEXT.RECEIVER, FrameAccess.getReceiver(frame));

        // Copy args and temps
        for (int i = 0; i <= frameSP; i++) {
            final Object tempValue = frameStackReadNode.execute(frame, i);
            assert tempValue != null;
            context.atTempPut(i, tempValue);
        }
        if (forceContext) {
            method.invalidateCanBeVirtualizedAssumption();
            frame.setObject(method.thisContextOrMarkerSlot, context);
        }
        context.getSender(); // forces sender chain to be materialized as well
        return context;
    }

    @Specialization(guards = {"!isVirtualized(frame)"})
    protected ContextObject doGet(final VirtualFrame frame, @SuppressWarnings("unused") final boolean forceContext) {
        return (ContextObject) FrameAccess.getContextOrMarker(frame, code.thisContextOrMarkerSlot);
    }
}
