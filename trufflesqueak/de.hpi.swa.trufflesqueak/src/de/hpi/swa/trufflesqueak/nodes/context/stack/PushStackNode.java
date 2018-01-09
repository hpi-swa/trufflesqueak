package de.hpi.swa.trufflesqueak.nodes.context.stack;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.MethodContextObject;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameSlotReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackWriteNode;

//TODO: this node cannot inherit from SqueakNode, because it takes an additional argument. it's very similar to AbstractStackNodes
public abstract class PushStackNode extends Node {
    @CompilationFinal protected final CompiledCodeObject code;
    @Child private FrameStackWriteNode writeNode;
    @Child private FrameSlotReadNode spNode;

    public static PushStackNode create(CompiledCodeObject code) {
        return PushStackNodeGen.create(code);
    }

    public abstract Object executeWrite(VirtualFrame frame, Object value);

    protected PushStackNode(CompiledCodeObject code) {
        this.code = code;
        this.spNode = FrameSlotReadNode.create(code.stackPointerSlot);
        writeNode = FrameStackWriteNode.create();
    }

    protected boolean isVirtualized(VirtualFrame frame) {
        return FrameUtil.getObjectSafe(frame, code.thisContextSlot) == null;
    }

    protected MethodContextObject getContext(VirtualFrame frame) {
        return (MethodContextObject) FrameUtil.getObjectSafe(frame, code.thisContextSlot);
    }

    protected int frameStackPointer(VirtualFrame frame) {
        return (int) spNode.executeRead(frame);
    }

    @Specialization(guards = {"isVirtualized(frame)"})
    protected Object doWriteVirtualized(VirtualFrame frame, Object value) {
        assert value != null;
        int newSP = frameStackPointer(frame) + 1;
        writeNode.execute(frame, newSP, value);
        frame.setInt(code.stackPointerSlot, newSP);
        return null;
    }

    @Specialization(guards = {"!isVirtualized(frame)"})
    protected Object doWrite(VirtualFrame frame, Object value) {
        getContext(frame).push(value);
        return null;
    }
}
