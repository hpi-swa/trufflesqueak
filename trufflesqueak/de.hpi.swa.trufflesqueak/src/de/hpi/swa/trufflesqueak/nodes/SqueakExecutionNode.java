package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

/**
 * This is the base class for Squeak bytecode evaluation. Since Squeak operates on a stack, until we
 * have a decompiler to re-create a useful AST, this also implements the access methods required to
 * work on the frame. The hope is that Truffle/Graal can virtualize these frames most of the time...
 */
public abstract class SqueakExecutionNode extends SqueakNode {
    private final CompiledMethodObject method;
    private final FrameSlot stackPointerSlot;
    private final FrameSlot pcSlot;
    private final FrameSlot closureSlot;

    public SqueakExecutionNode(CompiledMethodObject cm) {
        method = cm;
        if (cm != null) {
            stackPointerSlot = cm.stackPointerSlot;
            pcSlot = cm.pcSlot;
            closureSlot = cm.closureSlot;
        } else {
            stackPointerSlot = null;
            pcSlot = null;
            closureSlot = null;
        }
    }

    public ContextReference<SqueakImageContext> getContext() {
        return this.getRootNode().getLanguage(SqueakLanguage.class).getContextReference();
    }

    public CompiledMethodObject getMethod() {
        return method;
    }

    public final int getSP(VirtualFrame frame) {
        return FrameUtil.getIntSafe(frame, stackPointerSlot);
    }

    public final int getPC(VirtualFrame frame) {
        return FrameUtil.getIntSafe(frame, pcSlot);
    }

    public final void decSP(VirtualFrame frame, int count) {
        changeSP(frame, -count);
    }

    public final void incSP(VirtualFrame frame, int count) {
        changeSP(frame, count);
    }

    public final void changeSP(VirtualFrame frame, int count) {
        frame.setInt(stackPointerSlot, Math.max(getSP(frame) + count, 0));
    }

    public Object getClosure(VirtualFrame frame) {
        try {
            return frame.getObject(closureSlot);
        } catch (FrameSlotTypeException e) {
            throw new RuntimeException(e);
        }
    }

    public SqueakImageContext getImage() {
        return getMethod().getImage();
    }
}
