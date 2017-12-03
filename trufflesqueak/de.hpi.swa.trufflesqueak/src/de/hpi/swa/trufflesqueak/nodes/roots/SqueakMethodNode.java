package de.hpi.swa.trufflesqueak.nodes.roots;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.FrameUtil;
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
import de.hpi.swa.trufflesqueak.nodes.context.ArgumentNode;
import de.hpi.swa.trufflesqueak.nodes.context.ReceiverWriteNode;

public class SqueakMethodNode extends RootNode {
    protected final CompiledCodeObject code;
    @Children final SqueakNode[] argumentNodes;
    @Children final SqueakNode[] copiedValuesNodes;

    public SqueakMethodNode(SqueakLanguage language, CompiledCodeObject code) {
        super(language, code.getFrameDescriptor());
        this.code = code;
        int numArgs = code.getNumArgs();
        argumentNodes = new SqueakNode[numArgs + 1];
        argumentNodes[0] = new ReceiverWriteNode(code);
        for (int i = 1; i <= numArgs; i++) {
            argumentNodes[i] = new ArgumentNode(code, i);
        }
        if (code instanceof CompiledBlockObject) {
            int numCopiedValues = ((CompiledBlockObject) code).getNumCopiedValues();
            copiedValuesNodes = new SqueakNode[numCopiedValues + 1];
            for (int i = 0; i < numCopiedValues; i++) {
                copiedValuesNodes[i] = new ArgumentNode(code, 1 + numArgs + i);
            }
            copiedValuesNodes[copiedValuesNodes.length - 1] = new ArgumentNode(code, 1 + numArgs + numCopiedValues);

        } else {
            copiedValuesNodes = null;
        }
    }

    @ExplodeLoop
    public void enterFrame(VirtualFrame frame) {
        CompilerDirectives.ensureVirtualized(frame);
        initializeSlots(frame);
        CompilerAsserts.compilationConstant(argumentNodes.length);
        for (SqueakNode node : argumentNodes) {
            node.executeGeneric(frame);
        }
        if (isClosure()) {
            CompilerAsserts.compilationConstant(copiedValuesNodes.length);
            for (SqueakNode node : copiedValuesNodes) {
                node.executeGeneric(frame);
            }
        }
        frame.setInt(code.stackPointerSlot, code.getNumTemps());
    }

    private boolean isClosure() {
        return copiedValuesNodes != null;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        enterFrame(frame);
        try {
            code.getBytecodeNode().executeGeneric(frame);
        } catch (LocalReturn e) {
            return e.returnValue;
        } catch (NonLocalReturn e) {
            Object targetMarker = e.getTarget();
            Object frameMarker = FrameUtil.getObjectSafe(frame, code.markerSlot);
            if (targetMarker == frameMarker) {
                return e.returnValue;
            } else {
                throw e;
            }
        } catch (NonVirtualReturn e) {
            // TODO: unwind context chain towards e.targetContext
        } catch (ProcessSwitch e) {
            // TODO: switch
        }
        throw new RuntimeException("unimplemented exit from activation");
    }

    @Override
    public String toString() {
        return code.toString();
    }

    protected void initializeSlots(VirtualFrame frame) {
        frame.setInt(code.stackPointerSlot, 0);
        frame.setObject(code.markerSlot, new FrameMarker());
        frame.setObject(code.methodSlot, code);
    }
}
