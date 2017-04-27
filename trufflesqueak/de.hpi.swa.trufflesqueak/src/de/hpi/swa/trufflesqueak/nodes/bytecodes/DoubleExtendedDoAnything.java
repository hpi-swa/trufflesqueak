package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakExecutionNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.send.SendSelfSelector;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.send.SingleExtendedSuper;
import de.hpi.swa.trufflesqueak.nodes.context.ConstantNode;

public class DoubleExtendedDoAnything extends SqueakBytecodeNode {
    private final SqueakExecutionNode actualNode;

    public DoubleExtendedDoAnything(CompiledMethodObject compiledMethodObject, int idx, int i, int j) {
        super(compiledMethodObject, idx);
        int opType = i >> 5;
        switch (opType) {
            case 0:
                actualNode = new SendSelfSelector(compiledMethodObject, idx, getMethod().getLiteral(j), i & 31);
                break;
            case 1:
                actualNode = new SingleExtendedSuper(compiledMethodObject, idx, getMethod().getLiteral(j), i & 31);
                break;
            case 2:
                actualNode = new PushReceiverVariable(compiledMethodObject, j);
                break;
            case 3:
                actualNode = new PushConst(compiledMethodObject, idx, getMethod().getLiteral(j));
                break;
            case 4:
                actualNode = new PushConst(compiledMethodObject, idx, new ConstantNode(compiledMethodObject.getLiteral(j).at0(1)));
                break;
            case 5:
                // TODO: implement these three
                // store receiver var
            case 6:
                // store and pop receiver var
            case 7:
                // store into association value
            default:
                actualNode = new UnknownBytecode(compiledMethodObject, idx, i);
                // throw new RuntimeException("not implemented");
        }
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return actualNode.executeGeneric(frame);
    }
}
