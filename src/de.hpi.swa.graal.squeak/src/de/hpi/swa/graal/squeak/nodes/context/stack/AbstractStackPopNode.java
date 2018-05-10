package de.hpi.swa.graal.squeak.nodes.context.stack;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.nodes.CompiledCodeNodes.GetCompiledMethodNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackWriteNode;

public abstract class AbstractStackPopNode extends AbstractStackNode {
    @Child protected FrameStackWriteNode writeNode = FrameStackWriteNode.create();
    @Child private GetCompiledMethodNode compiledMethodNode = GetCompiledMethodNode.create();

    public AbstractStackPopNode(final CompiledCodeObject code) {
        super(code);
    }

    protected final Object atStackAndClear(final VirtualFrame frame, final int index) {
        final Object value = readNode.execute(frame, index);
        final CompiledMethodObject method = compiledMethodNode.execute(code);
        if (index >= 1 + method.getNumArgs() + method.getNumTemps()) {
            // only nil out stack values, not receiver, arguments, or temporary variables
            writeNode.execute(frame, index, code.image.nil);
        }
        return value;
    }

    public final Object atStackAndClear(final ContextObject context, final long argumentIndex) {
        final Object value = context.atStack(argumentIndex);
        final CompiledMethodObject method = compiledMethodNode.execute(code);
        if (argumentIndex >= 1 + method.getNumArgs() + method.getNumTemps()) {
            // only nil out stack values, not receiver, arguments, or temporary variables
            context.atStackPut(argumentIndex, code.image.nil);
        }
        return value;
    }
}
