package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Instrumentable;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNodeWithCode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotWriteNode;

@Instrumentable(factory = SqueakBytecodeNodeWrapper.class)
public abstract class SqueakBytecodeNode extends SqueakNodeWithCode {
    protected final int successorIndex;
    @Child FrameSlotReadNode readNode;
    @Child FrameSlotWriteNode writeNode;
    @Child FrameSlotReadNode spNode;

    protected SqueakBytecodeNode(SqueakBytecodeNode original) {
        super(original.code);
        successorIndex = original.successorIndex;
        setSourceSection(original.getSourceSection());
    }

    public SqueakBytecodeNode(CompiledCodeObject code, int index) {
        super(code);
        this.successorIndex = index;
    }

    public boolean isReturn() {
        return false;
    }

    public int executeInt(VirtualFrame frame) {
        if (successorIndex < 0) {
            throw new RuntimeException("Inner nodes are not allowed to be executed here");
        }
        executeVoid(frame);
        return successorIndex;
    }

    public void executeVoid(VirtualFrame frame) {
        executeGeneric(frame);
    }

    private FrameSlotReadNode getStackPointerNode() {
        if (spNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            spNode = FrameSlotReadNode.create(code.stackPointerSlot);
        }
        return spNode;
    }

    private FrameSlotReadNode getReadNode(FrameSlot slot) {
        if (readNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            readNode = FrameSlotReadNode.create(slot);
        } else if (readNode.slot != slot) {
            // throw new RuntimeException("Currently, only one slot can be read");
            if (code.image.config.isVerbose()) {
                System.out.println("Tried to read to multiple slots");
            }
            return FrameSlotReadNode.create(slot);
        }

        return readNode;
    }

    private FrameSlotWriteNode getWriteNode(FrameSlot slot) {
        if (writeNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            writeNode = FrameSlotWriteNode.create(slot);
        } else if (writeNode.slot != slot) {
            // throw new RuntimeException("Currently, only one slot can be written");
            if (code.image.config.isVerbose()) {
                System.out.println("Tried to write to multiple slots");
            }
            return FrameSlotWriteNode.create(slot);
        }
        return writeNode;
    }

    protected Object pop(VirtualFrame frame) {
        int sp = stackPointer(frame);
        frame.setInt(code.stackPointerSlot, sp - 1);
        return getReadNode(code.stackSlots[sp - 1]).executeRead(frame);
    }

    protected Object[] popN(VirtualFrame frame, int n) {
        int sp = stackPointer(frame);
        frame.setInt(code.stackPointerSlot, sp - n);
        Object[] result = new Object[n];
        for (int i = 0; i < n; i++) {
            result[i] = getReadNode(code.stackSlots[sp - 1 - i]).executeRead(frame);
        }
        return result;
    }

    protected Object push(VirtualFrame frame, Object value) {
        int sp = stackPointer(frame);
        getWriteNode(code.stackSlots[sp]).executeWrite(frame, value);
        frame.setInt(code.stackPointerSlot, sp + 1);
        return code.image.nil;
    }

    protected Object top(VirtualFrame frame) {
        return peek(frame, 0);
    }

    protected Object peek(VirtualFrame frame, int idx) {
        int sp = stackPointer(frame);
        return getReadNode(code.stackSlots[sp - 1 - idx]).executeRead(frame);
    }

    protected int stackPointer(VirtualFrame frame) {
        return (int) getStackPointerNode().executeRead(frame);
    }

    protected Object receiver(VirtualFrame frame) {
        return getReadNode(code.receiverSlot).executeRead(frame);
    }
}
