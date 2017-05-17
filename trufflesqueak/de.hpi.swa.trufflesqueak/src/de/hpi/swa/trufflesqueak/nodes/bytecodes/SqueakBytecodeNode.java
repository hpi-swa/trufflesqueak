package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import java.util.List;
import java.util.Stack;

import com.oracle.truffle.api.instrumentation.Instrumentable;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.SqueakNodeWithMethod;

@Instrumentable(factory = SqueakBytecodeNodeWrapper.class)
public abstract class SqueakBytecodeNode extends SqueakNodeWithMethod {
    protected final int index;

    protected SqueakBytecodeNode(SqueakBytecodeNode original) {
        super(original.method);
        index = original.index;
        setSourceSection(original.getSourceSection());
    }

    public SqueakBytecodeNode(CompiledCodeObject method, int idx) {
        super(method);
        index = idx;
    }

    @SuppressWarnings("unused")
    public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> sequence) {
        throw new RuntimeException("my subclass should implement interpretOn");
    }

    /**
     * Decompile this instruction into an AST node, given the current stack and statements already
     * decompiled. The sequence represents the sequence of nodes generated directly from each method
     * bytecode. This method should return the next index into this sequence at which to continue
     * decompilation. For most bytecode nodes this will be their own index + 1, but jumps and
     * closures interpret some of the following nodes in the sequence directly, and thus the caller
     * should skip over those nodes.
     *
     * @param stack
     * @param statements
     * @param sequence
     * @return the next index into sequence at which the caller should continue decompiling
     */
    public int interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> statements, List<SqueakBytecodeNode> sequence) {
        interpretOn(stack, statements);
        return sequence.indexOf(this) + 1;
    }

    public boolean isReturn() {
        return false;
    }
}
