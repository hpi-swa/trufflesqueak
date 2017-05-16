package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.debug.DebuggerTags;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNode;

public class PrimDebugger extends PrimitiveNode {
    public PrimDebugger(CompiledMethodObject cm) {
        super(cm);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return null;
    }

    @Override
    protected boolean isTaggedWith(Class<?> tag) {
        return tag == DebuggerTags.AlwaysHalt.class;
    }

    @Override
    public SourceSection getSourceSection() {
        return super.getSourceSection();
    }
}
