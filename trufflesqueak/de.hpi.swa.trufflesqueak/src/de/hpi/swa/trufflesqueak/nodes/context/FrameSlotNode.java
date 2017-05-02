package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public abstract class FrameSlotNode extends ContextAccessNode {
    protected final SlotGetter getter;
    @CompilationFinal FrameSlot cachedSlot;

    protected FrameSlotNode(CompiledMethodObject cm, SlotGetter slotGetter) {
        super(cm);
        getter = slotGetter;
    }

    protected FrameSlot getSlot(int sp) {
        if (cachedSlot == null) {
            CompilerDirectives.transferToInterpreter();
            cachedSlot = getter.getSlot(sp, getMethod());
        }
        CompilerAsserts.compilationConstant(cachedSlot);
        return cachedSlot;
    }

    protected boolean isInt(FrameSlot slot) {
        return slot.getKind() == FrameSlotKind.Int;
    }

    protected boolean isLong(FrameSlot slot) {
        return slot.getKind() == FrameSlotKind.Long;
    }

    protected boolean isBoolean(FrameSlot slot) {
        return slot.getKind() == FrameSlotKind.Boolean;
    }

    protected boolean isIllegal(FrameSlot slot) {
        return slot.getKind() == FrameSlotKind.Illegal;
    }
}