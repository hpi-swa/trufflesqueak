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
    protected FrameSlotWriteNode(CompiledMethodObject cm, FrameSlot slot) {
        super(cm, slot);
    }

    public static FrameSlotWriteNode argument(CompiledMethodObject cm, FrameSlot slot, int argumentIndex) {
        return FrameSlotWriteNodeGen.create(cm, slot, new ArgumentNode(argumentIndex));
    }

    public static FrameSlotWriteNode create(CompiledMethodObject cm, FrameSlot slot, SqueakNode node) {
        return FrameSlotWriteNodeGen.create(cm, slot, node);
    }

    public static FrameSlotWriteNode temp(CompiledMethodObject cm, int index, SqueakNode node) {
        return create(cm, cm.stackSlots[index], node);
    }

    protected boolean isNullWrite(Object value) {
        return isIllegal() && (value == null || value == getImage().nil);
    }

    @Specialization(guards = "isNullWrite(value)")
    public Object skipNullWrite(Object value) {
        return value;
    }

    @Specialization(guards = "isInt() || isIllegal()")
    public int writeInt(VirtualFrame frame, int value) {
        slot.setKind(FrameSlotKind.Int);
        frame.setInt(slot, value);
        return value;
    }

    @Specialization(guards = "isLong() || isIllegal()")
    public long writeLong(VirtualFrame frame, long value) {
        slot.setKind(FrameSlotKind.Long);
        frame.setLong(slot, value);
        return value;
    }

    @Specialization(guards = "isBoolean() || isIllegal()")
    public boolean writeBool(VirtualFrame frame, boolean value) {
        slot.setKind(FrameSlotKind.Boolean);
        frame.setBoolean(slot, value);
        return value;
    }

    @Specialization(replaces = {"skipNullWrite", "writeInt", "writeLong", "writeBool"})
    public Object writeObject(VirtualFrame frame, Object value) {
        slot.setKind(FrameSlotKind.Object);
        frame.setObject(slot, value);
        return value;
    }
}
