package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import java.util.Stack;

import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

public class PushActiveContextNode extends SqueakBytecodeNode {

    public PushActiveContextNode(CompiledMethodObject compiledMethodObject, int idx) {
        super(compiledMethodObject, idx);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        // TODO: ...
        MaterializedFrame materializedFrame = frame.materialize();
        ContextObject contextObject = new ContextObject(method.image, materializedFrame);
        // push(frame, contextObject);
        return contextObject;
    }

    @Override
    public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> sequence) {
        stack.push(this);
    }

}
