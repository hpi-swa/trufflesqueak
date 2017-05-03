package de.hpi.swa.trufflesqueak.nodes.roots;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.RootNode;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.LocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotWriteNode;

public class SqueakMethodNode extends RootNode {
    private final CompiledMethodObject method;
    @Child SqueakNode receiverNode;
    @Children final SqueakNode[] argumentNodes;
    @Children final SqueakBytecodeNode[] ast;
    private final FrameSlot pcSlot;
    private final FrameSlot stackPointerSlot;

    public SqueakMethodNode(SqueakLanguage language, CompiledMethodObject cm) {
        super(language, cm.getFrameDescriptor());
        method = cm;
        ast = method.getBytecodeAST();
        receiverNode = FrameSlotWriteNode.argument(cm, cm.receiverSlot, 0);
        int numArgs = method.getNumArgs();
        argumentNodes = new SqueakNode[numArgs];
        for (int i = 0; i < numArgs; i++) {
            argumentNodes[i] = FrameSlotWriteNode.argument(cm, cm.stackSlots[i], i + 1);
        }
        stackPointerSlot = method.stackPointerSlot;
        pcSlot = method.pcSlot;
    }

    // FIXME: replace this
    public Object executeGeneric(VirtualFrame frame, int initialPC) {
        int offset = method.getBytecodeOffset();
        int pc = initialPC - 1 - offset;
        while (pc >= 0 && pc < ast.length) {
            SqueakBytecodeNode node = ast[pc];
            if (node == null) {
                pc += 1;
            } else {
                try {
                    node.executeGeneric(frame);
                } catch (LocalReturn e) {
                    return e.returnValue;
                }
            }
        }
        throw new RuntimeException("Method did not return");
    }

    @ExplodeLoop
    public void enterFrame(VirtualFrame frame) {
        CompilerDirectives.ensureVirtualized(frame);
        frame.setInt(stackPointerSlot, method.getNumTemps());
        frame.setInt(pcSlot, 0);
        receiverNode.executeGeneric(frame);
        CompilerAsserts.compilationConstant(argumentNodes.length);
        for (SqueakNode node : argumentNodes) {
            node.executeGeneric(frame);
        }
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
        throw new RuntimeException("unimplemented exit from method");
    }
}
