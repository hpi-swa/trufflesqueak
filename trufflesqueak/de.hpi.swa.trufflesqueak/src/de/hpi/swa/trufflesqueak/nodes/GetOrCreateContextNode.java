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
        return executeGet(frame, false);
    }

    public abstract ContextObject executeGet(Frame frame, boolean forceContext);

    @Specialization(guards = {"isVirtualized(frame)"})
    protected ContextObject doCreateVirtualized(VirtualFrame frame, boolean forceContext) {
        return materialize(frame, forceContext);
    }

    public static ContextObject getOrCreate(Frame frame) {
        CompiledCodeObject method = FrameAccess.getMethod(frame);
        Object contextOrMarker = FrameAccess.getContextOrMarker(frame, method);
        if (contextOrMarker instanceof ContextObject) {
            return (ContextObject) contextOrMarker;
        } else {
            return materialize(frame, false);
        }
    }

    private static ContextObject materialize(Frame frame, boolean forceContext) {
        CompilerDirectives.transferToInterpreter();
        CompiledCodeObject method = FrameAccess.getMethod(frame);
        FrameMarker frameMarker = (FrameMarker) FrameAccess.getContextOrMarker(frame, method);
        ContextObject context = ContextObject.create(method.image, method.frameSize(), frameMarker);

        context.setSender(FrameAccess.getSender(frame));
        int framePC = FrameUtil.getIntSafe(frame, method.instructionPointerSlot);
        int frameSP = FrameUtil.getIntSafe(frame, method.stackPointerSlot);
        context.atput0(CONTEXT.METHOD, method);
        context.setInstructionPointer(framePC);
        context.setStackPointer(frameSP);
        BlockClosureObject closure = FrameAccess.getClosure(frame);
        context.atput0(CONTEXT.CLOSURE_OR_NIL, closure == null ? method.image.nil : closure);
        context.atput0(CONTEXT.RECEIVER, FrameAccess.getReceiver(frame));

        // Copy temps
        for (int i = 0; i < frameSP - 1; i++) {
            int tempIndex = i - method.getNumArgsAndCopiedValues();
            Object tempValue;
            if (tempIndex < 0) {
                int frameArgumentIndex = frame.getArguments().length + tempIndex;
                assert frameArgumentIndex >= FrameAccess.RCVR_AND_ARGS_START;
                tempValue = frame.getArguments()[frameArgumentIndex];
            } else {
                tempValue = frameStackReadNode.execute(frame, tempIndex);
            }
            assert tempValue != null;
            context.atTempPut(i, tempValue);
        }
        if (forceContext) {
            method.invalidateNoContextNeededAssumption();
            frame.setObject(method.thisContextOrMarkerSlot, context);
        }
        return context;
    }

    @Specialization(guards = {"!isVirtualized(frame)"})
    protected ContextObject doGet(VirtualFrame frame, @SuppressWarnings("unused") boolean forceContext) {
        CompiledCodeObject method = FrameAccess.getMethod(frame);
        return (ContextObject) FrameAccess.getContextOrMarker(frame, method);
    }
}