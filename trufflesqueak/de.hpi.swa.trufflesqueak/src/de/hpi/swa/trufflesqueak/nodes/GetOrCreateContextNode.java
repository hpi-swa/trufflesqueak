package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.exceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackReadNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.FrameMarker;

@ImportStatic(FrameAccess.class)
public abstract class GetOrCreateContextNode extends Node {
    @Child private FrameStackReadNode frameStackReadNode = FrameStackReadNode.create();

    public static GetOrCreateContextNode create() {
        return GetOrCreateContextNodeGen.create();
    }

    public final ContextObject executeGet(Frame frame) {
        return executeGet(frame, false);
    }

    public abstract ContextObject executeGet(Frame frame, boolean forceContext);

    protected boolean isVirtualized(VirtualFrame frame) {
        try {
            return frame.getObject(FrameAccess.getMethod(frame).thisContextOrMarkerSlot) instanceof FrameMarker;
        } catch (FrameSlotTypeException e) {
            throw new SqueakException("thisContextOrMarkerSlot should never be invalid");
        }
    }

    @Specialization(guards = {"isVirtualized(frame)"})
    protected ContextObject doCreateVirtualized(VirtualFrame frame, boolean forceContext) {
        CompilerDirectives.transferToInterpreter();
        FrameMarker frameMarker = (FrameMarker) FrameAccess.getContextOrMarker(frame);
        CompiledCodeObject method = FrameAccess.getMethod(frame);
        ContextObject context = ContextObject.create(method.image, method.frameSize(), frameMarker);

        context.setSender(FrameAccess.getSender(frame));
        context.atput0(CONTEXT.INSTRUCTION_POINTER, FrameUtil.getIntSafe(frame, method.instructionPointerSlot));
        int sp = FrameUtil.getIntSafe(frame, method.stackPointerSlot);
        context.atput0(CONTEXT.STACKPOINTER, sp);
        context.atput0(CONTEXT.METHOD, method);
        BlockClosureObject closure = FrameAccess.getClosure(frame);
        context.atput0(CONTEXT.CLOSURE_OR_NIL, closure == null ? method.image.nil : closure);
        context.atput0(CONTEXT.RECEIVER, FrameAccess.getReceiver(frame));

        // Copy temps
        for (int i = 0; i < sp - 1; i++) {
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
        return (ContextObject) FrameAccess.getContextOrMarker(frame);
    }
}