package de.hpi.swa.trufflesqueak.nodes.roots;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.RootNode;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.LocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.model.BlockClosure;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.FrameMarker;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ConstantNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotWriteNode;

public class SqueakBlockNode extends RootNode {
    private final BlockClosure block;
    @Children final SqueakNode[] ast;
    private final FrameSlot pcSlot;
    private final FrameSlot stackPointerSlot;
    private final FrameSlot markerSlot;
    private final FrameSlot closureSlot;
    @Children final SqueakNode[] argumentNodes;
    @Child SqueakNode receiverNode;
    @Children final SqueakNode[] copiedValuesNodes;

    public SqueakBlockNode(SqueakLanguage language, BlockClosure blk, FrameDescriptor fd) {
        // FIXME: refactor location of fd
        super(language, fd);
        block = blk;
        ast = blk.getAST();
        receiverNode = FrameSlotWriteNode.create(null, fd.findFrameSlot(CompiledMethodObject.RECEIVER), new ConstantNode(null, -1, block.getReceiver()));
        int numArgs = block.getNumArgs();
        argumentNodes = new SqueakNode[numArgs];
        for (int i = 0; i < numArgs; i++) {
            argumentNodes[i] = FrameSlotWriteNode.argument(null, fd.findFrameSlot(i), i + 1);
        }
        Object[] stack = block.getStack();
        copiedValuesNodes = new SqueakNode[stack.length];
        for (int i = 0; i < stack.length; i++) {
            copiedValuesNodes[i] = FrameSlotWriteNode.create(null, fd.findFrameSlot(i + numArgs), new ConstantNode(null, -1, stack[i]));
        }
        stackPointerSlot = fd.findFrameSlot(CompiledMethodObject.STACK_POINTER);
        pcSlot = fd.findFrameSlot(CompiledMethodObject.PC);
        markerSlot = fd.findFrameSlot(CompiledMethodObject.MARKER);
        closureSlot = fd.findFrameSlot(CompiledMethodObject.CLOSURE);
    }

    @ExplodeLoop
    public void enterFrame(VirtualFrame frame) {
        CompilerDirectives.ensureVirtualized(frame);
        frame.setInt(stackPointerSlot, block.getNumArgs() + block.varsize());
        frame.setInt(pcSlot, 0);
        frame.setObject(markerSlot, new FrameMarker());
        frame.setObject(closureSlot, block);
        receiverNode.executeGeneric(frame);
        CompilerAsserts.compilationConstant(argumentNodes.length);
        for (SqueakNode node : argumentNodes) {
            node.executeGeneric(frame);
        }
        for (SqueakNode node : copiedValuesNodes) {
            node.executeGeneric(frame);
        }
    }

    public String prettyPrint() {
        StringBuilder str = new StringBuilder();
        str.append(block.toString()).append('\n');
        for (SqueakNode node : ast) {
            node.prettyPrintOn(str);
            str.append('\n');
        }
        return str.toString();
    }

    @Override
    @ExplodeLoop
    public Object execute(VirtualFrame frame) {
        enterFrame(frame);
        for (SqueakNode node : ast) {
            try {
                node.executeGeneric(frame);
            } catch (LocalReturn e) {
                return e.returnValue;
            } catch (NonLocalReturn e) {
                // TODO: unwind context chain towards target
            } catch (NonVirtualReturn e) {
                // TODO: unwind context chain towards e.targetContext
            } catch (ProcessSwitch e) {
                // TODO: switch
            }
        }
        throw new RuntimeException("unimplemented exit from block");
    }
}
