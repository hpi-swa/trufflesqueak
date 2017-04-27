package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

@NodeChildren({@NodeChild(value = "value", type = SqueakNode.class)})
public abstract class FrameSlotWriteNode extends FrameSlotNode {
    protected FrameSlotWriteNode(CompiledMethodObject cm, SlotGetter slotGetter) {
        super(cm, slotGetter);
    }

    public static FrameSlotWriteNode argument(CompiledMethodObject cm, FrameSlot slot, int argumentIndex) {
        return FrameSlotWriteNodeGen.create(cm, new SlotGetter(slot), new ArgumentNode(cm, argumentIndex));
    }

    public static FrameSlotWriteNode create(CompiledMethodObject cm, FrameSlot slot, ContextAccessNode node) {
        return FrameSlotWriteNodeGen.create(cm, new SlotGetter(slot), node);
    }

    public static FrameSlotWriteNode push(CompiledMethodObject cm, SqueakNode node) {
        return FrameSlotWriteNodeGen.create(cm, new SlotGetter(0), node);
    }

    public static FrameSlotWriteNode temp(CompiledMethodObject cm, int index, ContextAccessNode node) {
        return create(cm, cm.stackSlots[index], node);
    }

    @Specialization(guards = "isInt(frame) || isIllegal(frame)")
    public int writeInt(VirtualFrame frame, int value) {
        FrameSlot slot = getSlot(frame);
        slot.setKind(FrameSlotKind.Int);
        frame.setInt(slot, value);
        return value;
    }

    @Specialization(guards = "isLong(frame) || isIllegal(frame)")
    public long writeLong(VirtualFrame frame, long value) {
        FrameSlot slot = getSlot(frame);
        slot.setKind(FrameSlotKind.Long);
        frame.setLong(slot, value);
        return value;
    }

    @Specialization(guards = "isBoolean(frame) || isIllegal(frame)")
    public boolean writeBool(VirtualFrame frame, boolean value) {
        FrameSlot slot = getSlot(frame);
        slot.setKind(FrameSlotKind.Boolean);
        frame.setBoolean(slot, value);
        return value;
    }

    @Specialization(replaces = {"writeInt", "writeLong", "writeBool"})
    public Object writeObject(VirtualFrame frame, Object value) {
        FrameSlot slot = getSlot(frame);
        slot.setKind(FrameSlotKind.Object);
        frame.setObject(slot, value);
        return value;
    }
}
