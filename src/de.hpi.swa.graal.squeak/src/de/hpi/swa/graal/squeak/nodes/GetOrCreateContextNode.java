package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.util.FrameAccess;

@ImportStatic(FrameAccess.class)
public abstract class GetOrCreateContextNode extends AbstractNodeWithCode {

    protected GetOrCreateContextNode(final CompiledCodeObject code) {
        super(code);
    }

    public static GetOrCreateContextNode create(final CompiledCodeObject code) {
        return GetOrCreateContextNodeGen.create(code);
    }

    public abstract ContextObject executeGet(Frame frame);

    @Specialization(guards = {"isFullyVirtualized(frame)"})
    protected final ContextObject doCreateLight(final VirtualFrame frame) {
        final CompiledCodeObject method = FrameAccess.getMethod(frame);
        final ContextObject context = ContextObject.create(method.image, method.sqContextSize(), frame.materialize(), getFrameMarker(frame));
        frame.setObject(code.thisContextOrMarkerSlot, context);
        return context;
    }

    @Fallback
    protected final ContextObject doGet(final VirtualFrame frame) {
        return getContext(frame);
    }
}
