package de.hpi.swa.trufflesqueak.nodes.roots;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.FrameSlot;
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
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotWriteNode;

public class SqueakMethodNode extends RootNode {
    private final CompiledCodeObject code;
    @Children final SqueakNode[] argumentNodes;
    @Children final SqueakNode[] copiedValuesNodes;
    @Children final SqueakNode[] ast;
    private final FrameSlot markerSlot;
    private final FrameSlot methodSlot;

    public SqueakMethodNode(SqueakLanguage language, CompiledCodeObject cc) {
        this(language, cc, true);
    }

    protected SqueakMethodNode(SqueakLanguage language, CompiledCodeObject cc, boolean hasReceiver) {
        super(language, cc.getFrameDescriptor());
        code = cc;
        ast = cc.getBytecodeAST();
        int numArgs = cc.getNumArgs();
        if (hasReceiver) {
            argumentNodes = new SqueakNode[numArgs + 1];
            argumentNodes[0] = FrameSlotWriteNode.argument(cc, cc.receiverSlot, 0);
        } else {
            argumentNodes = new SqueakNode[numArgs];
        }
        for (int i = 0; i < numArgs; i++) {
            argumentNodes[i + 1] = FrameSlotWriteNode.argument(cc, cc.getStackSlot(i), i + 1);
        }
        if (cc instanceof CompiledBlockObject) {
            int numCopiedValues = ((CompiledBlockObject) cc).getNumCopiedValues();
            copiedValuesNodes = new SqueakNode[numCopiedValues + 1];
            for (int i = 0; i < numCopiedValues; i++) {
                copiedValuesNodes[i] = FrameSlotWriteNode.argument(
                                cc,
                                cc.getStackSlot(i + numArgs),
                                1 + numArgs + i);
            }
            copiedValuesNodes[copiedValuesNodes.length - 1] = FrameSlotWriteNode.argument(
                            cc,
                            cc.closureSlot,
                            1 + numArgs + numCopiedValues);

        } else {
            copiedValuesNodes = null;
        }
        markerSlot = cc.markerSlot;
        methodSlot = cc.methodSlot;
    }

    @ExplodeLoop
    public void enterFrame(VirtualFrame frame) {
        CompilerDirectives.ensureVirtualized(frame);
        frame.setObject(markerSlot, new FrameMarker());
        frame.setObject(methodSlot, code);
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
        return code.prettyPrint();
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
                Object targetMarker = e.getTarget();
                Object frameMarker = FrameUtil.getObjectSafe(frame, markerSlot);
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
        }
        throw new RuntimeException("unimplemented exit from activation");
    }

    @Override
    public String toString() {
        return code.toString();
    }
}
