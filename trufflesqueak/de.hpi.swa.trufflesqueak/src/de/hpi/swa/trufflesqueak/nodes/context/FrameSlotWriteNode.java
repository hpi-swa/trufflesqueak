package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.instrumentation.PrettyPrintVisitor;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

@NodeChildren({@NodeChild(value = "value", type = SqueakNode.class)})
public abstract class FrameSlotWriteNode extends FrameSlotNode {
    public FrameSlotWriteNode(FrameSlotWriteNode original) {
        super(original.method, original.slot);
    }

    protected FrameSlotWriteNode(CompiledCodeObject cm, FrameSlot slot) {
        super(cm, slot);
    }

    public static FrameSlotWriteNode argument(CompiledCodeObject cm, FrameSlot slot, int argumentIndex) {
        return FrameSlotWriteNodeGen.create(cm, slot, new ArgumentNode(argumentIndex));
    }

    public static FrameSlotWriteNode create(CompiledCodeObject cm, FrameSlot slot, SqueakNode node) {
        return FrameSlotWriteNodeGen.create(cm, slot, node);
    }

    public static FrameSlotWriteNode temp(CompiledCodeObject method, int index, SqueakNode node) {
        return create(method, method.stackSlots[index], node);
    }

    protected boolean isNullWrite(VirtualFrame frame, Object value) {
        return isIllegal(frame) && value == null;
    }

    @Specialization(guards = "isNullWrite(frame, value)")
    public Object skipNullWrite(@SuppressWarnings("unused") VirtualFrame frame, Object value) {
        return value;
    }

    @Specialization(guards = "isInt(frame) || isIllegal(frame)")
    public long writeInt(VirtualFrame frame, int value) {
        slot.setKind(FrameSlotKind.Int);
        frame.setInt(slot, value);
        return value;
    }

    @Specialization(guards = "isLong(frame) || isIllegal(frame)")
    public long writeLong(VirtualFrame frame, long value) {
        slot.setKind(FrameSlotKind.Long);
        frame.setLong(slot, value);
        return value;
    }

    @Specialization(guards = "isBoolean(frame) || isIllegal(frame)")
    public boolean writeBool(VirtualFrame frame, boolean value) {
        slot.setKind(FrameSlotKind.Boolean);
        frame.setBoolean(slot, value);
        return value;
    }

    @Specialization(replaces = {"skipNullWrite", "writeLong", "writeBool"})
    public Object writeObject(VirtualFrame frame, Object value) {
        slot.setKind(FrameSlotKind.Object);
        frame.setObject(slot, value);
        return value;
    }

    @Override
    public void prettyPrintOn(PrettyPrintVisitor b) {
        b.append("t").append(slot.getIdentifier()).append(" := ");
        super.prettyPrintOn(b);
    }
}
