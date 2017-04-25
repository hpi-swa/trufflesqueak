package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.context.ReceiverNode;

@NodeChildren({@NodeChild(value = "receiverNode", type = ReceiverNode.class)})
public abstract class PushReceiverVariable extends SqueakBytecodeNode {
    private int variableIndex;

    public PushReceiverVariable(CompiledMethodObject cm, int idx, int i) {
        super(cm, idx);
        variableIndex = i & 15;
    }

    @Specialization
    public Object pushReceiver(VirtualFrame frame, BaseSqueakObject receiver) {
        BaseSqueakObject object = receiver.at0(variableIndex);
        push(frame, object);
        return object;
    }

    @Fallback
    public Object pushReceiver(@SuppressWarnings("unused") Object receiver) {
        throw new RuntimeException("tried to push variable from non-object receiver");
    }

    public static PushReceiverVariable create(CompiledMethodObject method, int index, int i) {
        return PushReceiverVariableNodeGen.create(method, index, i, new ReceiverNode(method));
    }
}
