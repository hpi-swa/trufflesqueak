package de.hpi.swa.trufflesqueak.nodes.roots;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.RootNode;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.BytecodeSequence;
import de.hpi.swa.trufflesqueak.nodes.context.ContextAccessNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotWriteNode;

public class SqueakMethodNode extends RootNode {
    private final CompiledMethodObject method;
    @Child BytecodeSequence bytecode;
    @Child ContextAccessNode receiverNode;
    @Children final ContextAccessNode[] argumentNodes;

    public SqueakMethodNode(SqueakLanguage language, CompiledMethodObject cm) {
        super(language, cm.getFrameDescriptor());
        method = cm;
        bytecode = method.getBytecodeAST();
        receiverNode = FrameSlotWriteNode.argument(cm, cm.receiverSlot, 0);
        int numArgs = method.getNumArgs();
        argumentNodes = new ContextAccessNode[numArgs];
        for (int i = 0; i < numArgs; i++) {
            argumentNodes[i] = FrameSlotWriteNode.argument(cm, cm.stackSlots[i], i + 1);
        }
    }

    @ExplodeLoop
    public void enterFrame(VirtualFrame frame) {
        frame.setInt(method.stackPointerSlot, method.getNumTemps());
        frame.setInt(method.pcSlot, 0);
        receiverNode.executeGeneric(frame);
        CompilerAsserts.compilationConstant(argumentNodes.length);
        for (ContextAccessNode node : argumentNodes) {
            node.executeGeneric(frame);
        }
    }

    @Override
    public Object execute(VirtualFrame frame) {
        enterFrame(frame);
        try {
            return bytecode.executeGeneric(frame);
        } catch (NonLocalReturn e) {
            // TODO: unwind context chain towards target
        } catch (NonVirtualReturn e) {
            // TODO: unwind context chain towards e.targetContext
        } catch (ProcessSwitch e) {
            // TODO: switch
        }
        throw new RuntimeException("unimplemented exit from method");
    }
}
