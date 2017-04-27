package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.frame.FrameSlot;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public class SlotGetter {
    private final FrameSlot slot;
    private final int offset;

    public SlotGetter(FrameSlot slot) {
        this.slot = slot;
        this.offset = 0;
    }

    public SlotGetter(int offset) {
        this.slot = null;
        this.offset = offset;
    }

    public FrameSlot getSlot(int currentSP, CompiledMethodObject cm) {
        if (slot != null) {
            return slot;
        } else {
            int sp = currentSP - offset;
            if (sp < 0) {
                return cm.receiverSlot;
            } else {
                return cm.stackSlots[sp];
            }
        }
    }
}
