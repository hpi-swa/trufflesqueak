package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackReadNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.FrameMarker;

@ImportStatic(FrameAccess.class)
public abstract class GetOrCreateContextNode extends AbstractNodeWithCode {
    public static GetOrCreateContextNode create(CompiledCodeObject code) {
        return GetOrCreateContextNodeGen.create(code);
    }

    @Child private static FrameStackReadNode frameStackReadNode = FrameStackReadNode.create();

    protected GetOrCreateContextNode(CompiledCodeObject code) {
        super(code);
    }

    public final ContextObject executeGet(Frame frame) {
        return executeGet(frame, true);  // TODO: only force context when necessary
    }

    public abstract ContextObject executeGet(Frame frame, boolean forceContext);

    @Specialization(guards = {"isVirtualized(frame)"})
    protected ContextObject doCreateVirtualized(VirtualFrame frame, boolean forceContext) {
        return materialize(frame, forceContext);
    }

    public static ContextObject getOrCreate(Frame frame) {
        Object contextOrMarker = FrameAccess.getContextOrMarker(frame);
        if (contextOrMarker instanceof ContextObject) {
            return (ContextObject) contextOrMarker;
        } else {
            return materialize(frame, true); // TODO: only force context when necessary
        }
    }

    private static ContextObject materialize(Frame frame, boolean forceContext) {
        CompilerDirectives.transferToInterpreter();
        CompiledCodeObject method = FrameAccess.getMethod(frame);
        FrameMarker frameMarker = (FrameMarker) FrameAccess.getContextOrMarker(frame);
        ContextObject context = ContextObject.create(method.image, method.frameSize(), frameMarker);

        context.setSender(FrameAccess.getSender(frame));
        long framePC = FrameUtil.getLongSafe(frame, method.instructionPointerSlot);
        long frameSP = FrameUtil.getLongSafe(frame, method.stackPointerSlot);
        context.atput0(CONTEXT.METHOD, method);
        if (framePC >= 0) {
            context.setInstructionPointer(framePC);
        } else { // context has been terminated
            context.atput0(CONTEXT.INSTRUCTION_POINTER, method.image.nil);
        }
        context.setStackPointer(frameSP + 1); // frame sp is zero-based
        BlockClosureObject closure = FrameAccess.getClosure(frame);
        context.atput0(CONTEXT.CLOSURE_OR_NIL, closure == null ? method.image.nil : closure);
        context.atput0(CONTEXT.RECEIVER, FrameAccess.getReceiver(frame));

        // Copy args and temps
        for (int i = 0; i <= frameSP; i++) {
            Object tempValue = frameStackReadNode.execute(frame, i);
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
    protected ContextObject doGet(VirtualFrame frame, @SuppressWarnings("unused") boolean forceContext) {
        return (ContextObject) FrameAccess.getContextOrMarker(frame);
    }
}