package de.hpi.swa.trufflesqueak.nodes.roots;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
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
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.context.PushArgumentNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.AdjustStackNode;
import de.hpi.swa.trufflesqueak.util.SqueakBytecodeDecoder;

public class SqueakMethodNode extends RootNode {
    private final CompiledCodeObject code;
    @Children private final SqueakBytecodeNode[] rcvrAndArgsNodes;
    @Children private final SqueakBytecodeNode[] copiedValuesNodes;
    @Children private final SqueakBytecodeNode[] bytecodeNodes;
    @Child private AdjustStackNode adjustForTempNode;

    public SqueakMethodNode(SqueakLanguage language, CompiledCodeObject code) {
        this(language, code, true);
    }

    public SqueakMethodNode(SqueakLanguage language, CompiledCodeObject code, boolean hasReceiver) {
        super(language, code.getFrameDescriptor());
        this.code = code;
        int numRcvr = hasReceiver ? 1 : 0;
        int numArgs = code.getNumArgs();
        rcvrAndArgsNodes = new SqueakBytecodeNode[numRcvr + numArgs];
        for (int i = 0; i < numRcvr + numArgs; i++) {
            rcvrAndArgsNodes[i] = new PushArgumentNode(code, i);
        }
        if (code instanceof CompiledBlockObject) {
            int numCopiedValues = code.getNumCopiedValues();
            copiedValuesNodes = new SqueakBytecodeNode[numCopiedValues];
            for (int i = 0; i < numCopiedValues; i++) {
                copiedValuesNodes[i] = new PushArgumentNode(code, numRcvr + numArgs + i);
            }
        } else {
            copiedValuesNodes = null;
        }
        int numTemps = Math.max(code.getNumTemps() - numArgs, 0);
        adjustForTempNode = numTemps > 0 ? new AdjustStackNode(code, numTemps) : null;
        bytecodeNodes = new SqueakBytecodeDecoder(code).decode();
    }

    @ExplodeLoop
    private void enterFrame(VirtualFrame frame) {
        CompilerDirectives.ensureVirtualized(frame);
        initializeSlots(frame);
        CompilerAsserts.compilationConstant(rcvrAndArgsNodes.length);
        for (SqueakBytecodeNode node : rcvrAndArgsNodes) {
            node.executeVoid(frame);
        }
        if (copiedValuesNodes != null) {
            CompilerAsserts.compilationConstant(copiedValuesNodes.length);
            for (SqueakBytecodeNode node : copiedValuesNodes) {
                node.executeVoid(frame);
            }
            frame.setInt(code.closureSlot, 1 + code.getNumArgs() + code.getNumCopiedValues());
        }
        if (adjustForTempNode != null) {
            adjustForTempNode.executeVoid(frame);
        }
    }

    @Override
    public Object execute(VirtualFrame frame) {
        enterFrame(frame);
        try {
            executeLoop(frame);
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

    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.MERGE_EXPLODE)
    private void executeLoop(VirtualFrame frame) {
        CompilerAsserts.compilationConstant(bytecodeNodes.length);
        int pc = 0;
        outer: while (true) {
            CompilerAsserts.partialEvaluationConstant(pc);
            CompilerAsserts.partialEvaluationConstant(bytecodeNodes[pc]);
            SqueakBytecodeNode node = bytecodeNodes[pc];
            int successor = node.executeInt(frame);
            int[] successors = node.getSuccessors();
            for (int i = 0; i < successors.length; i++) {
                if (i == successor) {
                    pc = successors[i];
                    continue outer;
                }
            }
            CompilerDirectives.transferToInterpreter();
            throw new RuntimeException("Method did not return");
        }
    }

    @Override
    public String toString() {
        return code.toString();
    }

    @Override
    public String getName() {
        return code.toString();
    }

    private void initializeSlots(VirtualFrame frame) {
        frame.setInt(code.stackPointerSlot, -1);
        frame.setObject(code.markerSlot, new FrameMarker());
        frame.setObject(code.methodSlot, code);
    }

    @Override
    protected boolean isTaggedWith(Class<?> tag) {
        return tag == StandardTags.RootTag.class;
    }
}
