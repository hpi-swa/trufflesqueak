package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
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

    public void push(VirtualFrame frame, Object result) {
        int sp = getSP(frame);
        frame.setObject(method.stackSlots[sp], result);
        setSP(frame, sp + 1);

    }

    public Object top(VirtualFrame frame, int offset) {
        int sp = Math.max(0, getSP(frame) - offset - 1);
        try {
            return frame.getObject(method.stackSlots[sp]);
        } catch (FrameSlotTypeException e) {
            throw new RuntimeException(e);
        }
    }

    public Object top(VirtualFrame frame) {
        return top(frame, 0);
    }

    public Object pop(VirtualFrame frame) {
        int sp = getSP(frame);
        try {
            return top(frame);
        } finally {
            if (sp > 0) {
                setSP(frame, sp - 1);
            }
        }
    }

    public int getSP(VirtualFrame frame) {
        try {
            return frame.getInt(method.stackPointerSlot);
        } catch (FrameSlotTypeException e) {
            throw new RuntimeException(e);
        }
    }

    public void setSP(VirtualFrame frame, int newSP) {
        frame.setInt(method.stackPointerSlot, newSP);
    }

    public int getPC(VirtualFrame frame) {
        try {
            return frame.getInt(method.pcSlot);
        } catch (FrameSlotTypeException e) {
            throw new RuntimeException(e);
        }
    }

    public Object getReceiver(VirtualFrame frame) {
        try {
            return frame.getObject(method.receiverSlot);
        } catch (FrameSlotTypeException e) {
            throw new RuntimeException(e);
        }
    }

    public Object getTemp(VirtualFrame frame, int idx) {
        try {
            return frame.getObject(method.stackSlots[idx]);
        } catch (FrameSlotTypeException e) {
            throw new RuntimeException(e);
        }
    }

    public void setTemp(VirtualFrame frame, int idx, Object obj) {
        frame.setObject(method.stackSlots[idx], obj);
    }
}
