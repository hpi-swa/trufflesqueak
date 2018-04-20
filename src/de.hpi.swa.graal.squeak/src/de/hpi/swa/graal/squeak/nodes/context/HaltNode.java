package de.hpi.swa.graal.squeak.nodes.context;

import com.oracle.truffle.api.debug.DebuggerTags;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.nodes.bytecodes.AbstractBytecodeNode;

public class HaltNode extends AbstractBytecodeNode {

    public HaltNode(final CompiledCodeObject code, final int index) {
        super(code, index);
    }

    @Override
    public void executeVoid(final VirtualFrame frame) {
    }

    @Override
    public String toString() {
        return "send: halt";
    }

    @Override
    public boolean hasTag(final Class<? extends Tag> tag) {
        return tag == DebuggerTags.AlwaysHalt.class;
    }
}
