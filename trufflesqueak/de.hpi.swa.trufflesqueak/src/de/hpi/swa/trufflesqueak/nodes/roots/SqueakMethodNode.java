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
import de.hpi.swa.trufflesqueak.model.CompiledBlockObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.FrameMarker;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotWriteNode;

public class SqueakMethodNode extends RootNode {
    private final CompiledCodeObject code;
    @Child SqueakNode receiverNode;
    @Children final SqueakNode[] argumentNodes;
    @Children final SqueakNode[] copiedValuesNodes;
    @Children final SqueakNode[] ast;
    private final FrameSlot pcSlot;
    private final FrameSlot stackPointerSlot;
    private final FrameSlot markerSlot;

    public SqueakMethodNode(SqueakLanguage language, CompiledCodeObject cc) {
        super(language, cc.getFrameDescriptor());
        code = cc;
        ast = cc.getBytecodeAST();
        receiverNode = FrameSlotWriteNode.argument(cc, cc.receiverSlot, 0);
        int numArgs = cc.getNumArgs();
        argumentNodes = new SqueakNode[numArgs];
        for (int i = 0; i < numArgs; i++) {
            argumentNodes[i] = FrameSlotWriteNode.argument(cc, cc.stackSlots[i], 1 + i);
        }
        if (cc instanceof CompiledBlockObject) {
            int numCopiedValues = ((CompiledBlockObject) cc).getNumCopiedValues();
            copiedValuesNodes = new SqueakNode[numCopiedValues + 1];
            for (int i = 0; i < numCopiedValues; i++) {
                copiedValuesNodes[i] = FrameSlotWriteNode.argument(
                                cc,
                                cc.stackSlots[i + numArgs],
                                1 + numArgs + i);
            }
            copiedValuesNodes[copiedValuesNodes.length - 1] = FrameSlotWriteNode.argument(
                            cc,
                            cc.closureSlot,
                            1 + numArgs + numCopiedValues);

        } else {
            copiedValuesNodes = null;
        }
        stackPointerSlot = cc.stackPointerSlot;
        pcSlot = cc.pcSlot;
        markerSlot = cc.markerSlot;
    }

    @ExplodeLoop
    public void enterFrame(VirtualFrame frame) {
        CompilerDirectives.ensureVirtualized(frame);
        frame.setInt(stackPointerSlot, code.getNumTemps());
        frame.setInt(pcSlot, 0);
        frame.setObject(markerSlot, new FrameMarker());
        receiverNode.executeGeneric(frame);
        CompilerAsserts.compilationConstant(argumentNodes.length);
        for (SqueakNode node : argumentNodes) {
            node.executeGeneric(frame);
        }
        if (copiedValuesNodes != null) {
            for (SqueakNode node : copiedValuesNodes) {
                node.executeGeneric(frame);
            }
        }
    }

    public String prettyPrint() {
        StringBuilder str = new StringBuilder();
        str.append(code.toString()).append('\n');
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
        throw new RuntimeException("unimplemented exit from activation");
    }
}
