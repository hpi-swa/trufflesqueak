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
        Object[] arguments = frame.getArguments();
        if (idx == 0) {
            if (arguments.length == 0) {
                return getImage().nil;
            }
        }
        return arguments[idx];
    }
}
