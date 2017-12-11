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
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushReceiverNode;
import de.hpi.swa.trufflesqueak.nodes.context.PushArgumentNode;
import de.hpi.swa.trufflesqueak.nodes.context.PushNilNode;
import de.hpi.swa.trufflesqueak.nodes.context.StoreReceiverFromArgumentsNode;

public class SqueakMethodNode extends RootNode {
    protected final CompiledCodeObject code;
    @Children final SqueakNode[] initStackNodes;
    @Child StoreReceiverFromArgumentsNode storeReceiverNode;

    public SqueakMethodNode(SqueakLanguage language, CompiledCodeObject code) {
        this(language, code, true);
    }

    public SqueakMethodNode(SqueakLanguage language, CompiledCodeObject code, boolean hasReceiver) {
        super(language, code.getFrameDescriptor());
        this.code = code;
        int numRcvr = hasReceiver ? 1 : 0;
        int numTemps = code.getNumTemps();
        int numArgs = code.getNumArgs();
        int numCopiedValues = 0;
        if (code instanceof CompiledBlockObject) {
            numCopiedValues = ((CompiledBlockObject) code).getNumCopiedValues();
        }
        initStackNodes = new SqueakNode[numTemps + numRcvr + numArgs + numCopiedValues];
        for (int i = 0; i < numTemps; i++) {
            initStackNodes[i] = new PushNilNode(code);
        }
        if (hasReceiver) {
            storeReceiverNode = new StoreReceiverFromArgumentsNode(code);
            initStackNodes[numTemps] = new PushReceiverNode(code, -1);
        }
        int offset = numTemps + numRcvr;
        for (int i = 0; i < numArgs; i++) {
            initStackNodes[offset + i] = new PushArgumentNode(code, i + numRcvr);
        }
        offset = offset + numArgs;
        for (int i = 0; i < numCopiedValues; i++) {
            initStackNodes[offset + i] = new PushArgumentNode(code, offset + i);
        }
    }

    @ExplodeLoop
    public void enterFrame(VirtualFrame frame) {
        CompilerDirectives.ensureVirtualized(frame);
        initializeSlots(frame);
        if (storeReceiverNode != null) {
            storeReceiverNode.executeGeneric(frame);
        }
        CompilerAsserts.compilationConstant(initStackNodes.length);
        for (SqueakNode node : initStackNodes) {
            node.executeGeneric(frame);
        }
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
        frame.setInt(code.stackPointerSlot, -1);
        frame.setObject(code.markerSlot, new FrameMarker());
        frame.setObject(code.methodSlot, code);
    }
}
