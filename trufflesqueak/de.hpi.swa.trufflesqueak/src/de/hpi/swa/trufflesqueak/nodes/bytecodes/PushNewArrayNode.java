package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import java.util.Arrays;
import java.util.Stack;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import de.hpi.swa.trufflesqueak.instrumentation.SourceStringBuilder;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

public class PushNewArrayNode extends SqueakBytecodeNode {
    @Children final SqueakNode[] popIntoArrayNodes;
    @Child SqueakNode pushArrayNode;
    private final int arraySize;

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
        BaseSqueakObject[] ptrs = new BaseSqueakObject[arraySize];
        if (popIntoArrayNodes != null) {
            for (int i = 0; i < arraySize; i++) {
                // TODO: FIXME: popping primitive values into an array
                ptrs[i] = (BaseSqueakObject) popIntoArrayNodes[i].executeGeneric(frame);
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
    public void prettyPrintOn(SourceStringBuilder b) {
        if (popIntoArrayNodes == null) {
            b.append("Array new: ").append(arraySize);
        } else {
            b.append('{');
            Arrays.stream(popIntoArrayNodes).forEach(n -> {
                n.prettyPrintOn(b);
                b.append(". ");
            });
            b.append('}');
        }
    }

    @Override
    protected boolean isTaggedWith(Class<?> tag) {
        if (tag == StandardTags.StatementTag.class) {
            return getSourceSection().isAvailable();
        }
        return false;
    }
}
