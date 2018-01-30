package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.FrameMarker;

public abstract class GetOrCreateContextNode extends AbstractNodeWithCode {
    public static GetOrCreateContextNode create(CompiledCodeObject code) {
        return GetOrCreateContextNodeGen.create(code);
    }

    protected GetOrCreateContextNode(CompiledCodeObject code) {
        super(code);
    }

    public abstract ContextObject executeGet(VirtualFrame frame);

    @Specialization(guards = {"isVirtualized(frame)"})
    protected ContextObject doCreateVirtualized(VirtualFrame frame) {
        code.invalidateNoContextNeededAssumption();
        ContextObject context = ContextObject.materialize(frame, (FrameMarker) FrameAccess.getContextOrMarker(frame));
        frame.setObject(code.thisContextOrMarkerSlot, context);
        return context;
    }

    @Specialization(guards = {"!isVirtualized(frame)"})
    protected ContextObject doGet(VirtualFrame frame) {
        return (ContextObject) FrameAccess.getContextOrMarker(frame);
    }
}