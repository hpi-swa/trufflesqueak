package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.exceptions.LocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public abstract class SqueakBytecodeNode extends Node {
    private final CompiledMethodObject method;
    private final int index;

    public SqueakBytecodeNode(CompiledMethodObject cm, int idx) {
        method = cm;
        index = idx;
    }

    public abstract void executeGeneric(VirtualFrame frame) throws NonLocalReturn, NonVirtualReturn, LocalReturn, ProcessSwitch;

    public CompiledMethodObject getMethod() {
        return method;
    }

    public int getIndex() {
        return index;
    }

    public void push(VirtualFrame frame, BaseSqueakObject obj) {
        int sp = getSP(frame);
        frame.setObject(method.stackSlots[sp], obj);
        setSP(frame, sp + 1);

    }

    public BaseSqueakObject top(VirtualFrame frame) {
        int sp = getSP(frame);
        if (sp > 0) {
            sp = sp - 1;
        }
        try {
            return (BaseSqueakObject) frame.getObject(method.stackSlots[sp]);
        } catch (FrameSlotTypeException e) {
            throw new RuntimeException(e);
        }
    }

    public BaseSqueakObject pop(VirtualFrame frame) {
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

    public BaseSqueakObject getReceiver(VirtualFrame frame) {
        try {
            return (BaseSqueakObject) frame.getObject(method.receiverSlot);
        } catch (FrameSlotTypeException e) {
            throw new RuntimeException(e);
        }
    }
}
