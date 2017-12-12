package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

public class PushNewArrayNode extends SqueakBytecodeNode {
    @Child SqueakNode pushArrayNode;
    public final boolean popValues;
    public final int arraySize;

    public PushNewArrayNode(CompiledCodeObject code, int index, int param) {
        super(code, index);
        arraySize = param & 127;
        popValues = param > 127;
    }

    @Override
    @ExplodeLoop
    public Object executeGeneric(VirtualFrame frame) {
        Object[] array;
        if (popValues) {
            array = popNReversed(frame, arraySize);
        } else {
            array = new Object[arraySize];
        }
        return push(frame, code.image.wrap(array));
    }

    @Override
    protected boolean isTaggedWith(Class<?> tag) {
        if (tag == StandardTags.StatementTag.class) {
            return getSourceSection().isAvailable();
        }
        return false;
    }
}
