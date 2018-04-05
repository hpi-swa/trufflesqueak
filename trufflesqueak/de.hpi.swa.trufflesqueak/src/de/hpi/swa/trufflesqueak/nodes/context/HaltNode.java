package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.debug.DebuggerTags;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.AbstractBytecodeNode;

public class HaltNode extends AbstractBytecodeNode {

    public HaltNode(CompiledCodeObject code, int index) {
        super(code, index);
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
    }

    @Override
    public String toString() {
        return "send: halt";
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        return tag == DebuggerTags.AlwaysHalt.class;
    }
}
