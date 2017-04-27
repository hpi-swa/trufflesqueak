package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.TruffleLanguage.ContextReference;
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

    public SqueakExecutionNode(CompiledMethodObject cm) {
        method = cm;
    }

    public ContextReference<SqueakImageContext> getContext() {
        return this.getRootNode().getLanguage(SqueakLanguage.class).getContextReference();
    }

    public CompiledMethodObject getMethod() {
        return method;
    }

    public int getSP(VirtualFrame frame) {
        return FrameUtil.getIntSafe(frame, method.stackPointerSlot);
    }

    public int getPC(VirtualFrame frame) {
        return FrameUtil.getIntSafe(frame, method.pcSlot);
    }

    public void decSP(VirtualFrame frame) {
        frame.setInt(method.stackPointerSlot, Math.max(getSP(frame) - 1, 0));
    }

    public void incSP(VirtualFrame frame) {
        frame.setInt(method.stackPointerSlot, getSP(frame) + 1);
    }

    public Object getClosure(VirtualFrame frame) {
        try {
            return frame.getObject(method.closureSlot);
        } catch (FrameSlotTypeException e) {
            throw new RuntimeException(e);
        }
    }

    public SqueakImageContext getImage() {
        return getMethod().getImage();
    }
}
