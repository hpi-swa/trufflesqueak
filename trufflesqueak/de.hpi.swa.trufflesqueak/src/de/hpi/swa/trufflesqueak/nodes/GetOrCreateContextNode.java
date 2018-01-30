package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

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

    @Specialization(guards = {"isVirtualized(frame)"})
    protected ContextObject doCreateVirtualized(VirtualFrame frame, boolean forceContext) {
        FrameMarker frameMarker = (FrameMarker) FrameAccess.getContextOrMarker(frame);
        CompiledCodeObject method = FrameAccess.getMethod(frame);
        ContextObject context = ContextObject.create(method.image, method.frameSize(), frameMarker);

        context.setSender(FrameAccess.getSender(frame));
        context.atput0(CONTEXT.INSTRUCTION_POINTER, FrameAccess.getInstructionPointer(frame));
        int sp = FrameAccess.getStackPointer(frame);
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
            FrameAccess.setContext(frame, context);
        }
        return context;
    }

    @Specialization(guards = {"!isVirtualized(frame)"})
    protected ContextObject doGet(VirtualFrame frame, @SuppressWarnings("unused") boolean forceContext) {
        return (ContextObject) FrameAccess.getContextOrMarker(frame);
    }
}