package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public class ArgumentNode extends ContextAccessNode {
    private final int idx;

    public ArgumentNode(CompiledMethodObject cm, int index) {
        super(cm);
        idx = index;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        Object[] args = frame.getArguments();
        if (idx < args.length) {
            return args[idx];
        } else {
            return getImage().nil;
        }
    }
}
