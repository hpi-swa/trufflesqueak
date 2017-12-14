package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Instrumentable;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNodeWithCode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotWriteNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameStackReadNode;

@Instrumentable(factory = SqueakBytecodeNodeWrapper.class)
public abstract class SqueakBytecodeNode extends SqueakNodeWithCode {
    protected final int numBytecodes;
    protected final int successorIndex;
    @Child FrameStackReadNode readNode;
    @Child FrameSlotWriteNode writeNode;
    @Child FrameSlotReadNode spNode;

    protected SqueakBytecodeNode(SqueakBytecodeNode original) {
        super(original.code);
        numBytecodes = original.numBytecodes;
        successorIndex = original.successorIndex;
        setSourceSection(original.getSourceSection());
    }

    public SqueakBytecodeNode(CompiledCodeObject code, int index, int numBytecodes) {
        super(code);
        this.numBytecodes = numBytecodes;
        this.successorIndex = index + numBytecodes;
    }

    public SqueakBytecodeNode(CompiledCodeObject code, int index) {
        this(code, index, 1);
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

    public int getSuccessorIndex() {
        return successorIndex;
    }

    public int getNumBytecodes() {
        return numBytecodes;
    }

    public int getIndex() {
        return successorIndex - numBytecodes;
    }

    private FrameSlotReadNode getStackPointerNode() {
        if (spNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            spNode = insert(FrameSlotReadNode.create(code.stackPointerSlot));
        }
        return spNode;
    }

    protected FrameStackReadNode getReadNode() {
        if (readNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            readNode = insert(FrameStackReadNode.create());
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
        return getReadNode().execute(frame, sp);
    }

    @ExplodeLoop
    protected Object[] popN(VirtualFrame frame, int n) {
        int sp = stackPointer(frame);
        frame.setInt(code.stackPointerSlot, sp - n);
        Object[] result = new Object[n];
        for (int i = 0; i < n; i++) {
            result[i] = getReadNode().execute(frame, sp - i);
        }
        return result;
    }

    @ExplodeLoop
    protected Object[] popNReversed(VirtualFrame frame, int n) {
        int sp = stackPointer(frame);
        frame.setInt(code.stackPointerSlot, sp - n);
        Object[] result = new Object[n];
        for (int i = 0; i < n; i++) {
            result[n - 1 - i] = getReadNode().execute(frame, sp - i);
        }
        return result;
    }

    protected Object push(VirtualFrame frame, Object value) {
        int newSP = stackPointer(frame) + 1;
        getWriteNode(code.stackSlots[newSP]).executeWrite(frame, value);
        frame.setInt(code.stackPointerSlot, newSP);
        return code.image.nil;
    }

    protected Object top(VirtualFrame frame) {
        return peek(frame, 0);
    }

    @ExplodeLoop
    protected Object[] topN(VirtualFrame frame, int n) {
        int sp = stackPointer(frame);
        Object[] result = new Object[n];
        for (int i = 0; i < n; i++) {
            result[i] = getReadNode().execute(frame, sp - i);
        }
        return result;
    }

    @ExplodeLoop
    protected Object[] bottomN(VirtualFrame frame, int n) {
        Object[] result = new Object[n];
        for (int i = 0; i < n; i++) {
            result[i] = getReadNode().execute(frame, i);
        }
        return result;
    }

    protected Object peek(VirtualFrame frame, int idx) {
        int sp = stackPointer(frame);
        return getReadNode().execute(frame, sp - idx);
    }

    protected int stackPointer(VirtualFrame frame) {
        return (int) getStackPointerNode().executeRead(frame);
    }

    protected Object receiver(VirtualFrame frame) {
        return getReadNode().execute(frame, 0);
    }

    @Override
    protected boolean isTaggedWith(Class<?> tag) {
        return tag == StandardTags.StatementTag.class;
    }
}
