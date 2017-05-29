package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import java.util.Stack;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import de.hpi.swa.trufflesqueak.instrumentation.PrettyPrintVisitor;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

public class PushNewArrayNode extends SqueakBytecodeNode {
    @Children public final SqueakNode[] popIntoArrayNodes;
    @Child SqueakNode pushArrayNode;
    public final int arraySize;

    public PushNewArrayNode(CompiledCodeObject method, int idx, int param) {
        super(method, idx);
        arraySize = param & 0b0111111;
        if ((param >> 7) == 1) {
            popIntoArrayNodes = new SqueakNode[arraySize];
        } else {
            popIntoArrayNodes = null;
        }
    }

    @Override
    @ExplodeLoop
    public Object executeGeneric(VirtualFrame frame) {
        Object[] ptrs = new Object[arraySize];
        if (popIntoArrayNodes != null) {
            for (int i = 0; i < arraySize; i++) {
                ptrs[i] = popIntoArrayNodes[i].executeGeneric(frame);
            }
        }
        return method.image.wrap(ptrs);
    }

    @Override
    public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> sequence) {
        if (popIntoArrayNodes != null) {
            for (int i = 0; i < arraySize; i++) {
                popIntoArrayNodes[i] = stack.pop();
            }
        }
        stack.push(this);
    }

    @Override
    public void accept(PrettyPrintVisitor b) {
        b.visit(this);
    }

    @Override
    protected boolean isTaggedWith(Class<?> tag) {
        if (tag == StandardTags.StatementTag.class) {
            return getSourceSection().isAvailable();
        }
        return false;
    }
}
