package de.hpi.swa.trufflesqueak.nodes.bytecodes.jump;

import java.util.List;
import java.util.Stack;
import java.util.Vector;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.LoopNode;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ExtendedStoreNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.jump.LoopRepeatingNode.WhileNode;
import de.hpi.swa.trufflesqueak.util.Decompiler;

public class ConditionalJump extends AbstractJump {
    protected final int offset;
    public final boolean isIfTrue;

    private ConditionalJump(CompiledCodeObject cm, int idx, int off, boolean condition) {
        super(cm, idx);
        offset = off;
        isIfTrue = condition;
    }

    public ConditionalJump(CompiledCodeObject cm, int idx, int bytecode) {
        this(cm, idx, shortJumpOffset(bytecode), false);
    }

    public ConditionalJump(CompiledCodeObject cm, int idx, int bytecode, int parameter, boolean condition) {
        this(cm, idx + 1, longJumpOffset(bytecode, parameter), condition);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        throw new RuntimeException("should not execute");
    }

    private int firstBranchBC(List<SqueakBytecodeNode> sequence) {
        return sequence.indexOf(this) + 1;
    }

    private int lastBranchBC(List<SqueakBytecodeNode> sequence) {
        return firstBranchBC(sequence) + offset - 1;
    }

    private boolean isLoop(List<SqueakBytecodeNode> sequence) {
        int lastBranchBC = lastBranchBC(sequence);
        SqueakBytecodeNode lastNode = sequence.get(lastBranchBC);
        if (lastNode instanceof UnconditionalJump) {
            int myIndex = sequence.indexOf(this);
            // don't be fooled if an inner loop's return jump is simply the last
            // code in our branch
            return (lastBranchBC + ((UnconditionalJump) lastNode).offset + 1 < myIndex);
        } else {
            return false;
        }
    }

    private int interpretAsLoop(Stack<SqueakNode> stack,
                    Stack<SqueakNode> statements,
                    List<SqueakBytecodeNode> sequence) {
        // remove jump back node
        int lastBranchBC = lastBranchBC(sequence);
        UnconditionalJump jumpOutNode = (UnconditionalJump) sequence.get(lastBranchBC);
        // we're the condition of a loop, the unconditional back jump will jump
        // before us
        int sequenceIndex = sequence.indexOf(this);
        int firstCondBC = lastBranchBC + jumpOutNode.offset + 1;
        assert firstCondBC < sequenceIndex;
        // before the condition there may be other statements that are part of
        // the condition body
        Stack<SqueakNode> subStack = new Stack<>();
        List<SqueakBytecodeNode> conditionBody = sequence.subList(firstCondBC, sequenceIndex);
        SqueakNode[] conditionBlock = Decompiler.blockFrom(conditionBody, subStack);
        // the statements we got in the condition block must be removed from the
        // outer
        // statements
        for (int i = 0; i < conditionBlock.length; i++) {
            statements.pop();
        }
        // the condition left on the re-interpreted stack should be the same as
        // the one on the outer stack that was interpreted before we were
        assert !subStack.empty() && subStack.peek().getClass() == stack.peek().getClass();
        SqueakNode condition = branchCondition(stack);

        // Now we can interpret our loop body
        List<SqueakBytecodeNode> body = sequence.subList(firstBranchBC(sequence), lastBranchBC);
        subStack.clear();
        SqueakNode[] bodyBlock = Decompiler.blockFrom(body, subStack);

        LoopNode node = Truffle.getRuntime().createLoopNode(new LoopRepeatingNode(method, conditionBlock, condition, bodyBlock));
        if (!stack.empty() && stack.peek() instanceof ExtendedStoreNode) {
            statements.push(stack.pop());
        }
        statements.push(new WhileNode(method, index, node));
        return lastBranchBC + 1;
    }

    @SuppressWarnings("static-method")
    private boolean isIfNil(Stack<SqueakNode> stack) {
        return stack.peek() instanceof IfNilCheck;
    }

    @Override
    public int interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> statements, List<SqueakBytecodeNode> sequence) {
        if (isLoop(sequence)) {
            return interpretAsLoop(stack, statements, sequence);
        } else if (isIfNil(stack)) {
            return interpretAsIfNil(stack, sequence);
        } else {
            return interpretAsIfTrueIfFalse(stack, statements, sequence);
        }
    }

    private int interpretAsIfNil(Stack<SqueakNode> stack, List<SqueakBytecodeNode> sequence) {
        Vector<SqueakBytecodeNode> thenBranchNodes = new Vector<>(sequence.subList(firstBranchBC(sequence),
                        lastBranchBC(sequence) + 1));
        Stack<SqueakNode> subStack = new Stack<>();
        SqueakNode[] thenStatements = Decompiler.blockFrom(thenBranchNodes, subStack);
        SqueakNode thenResult = subStack.empty() ? null : subStack.pop();
        stack.push(new IfNilCheck((IfNilCheck) stack.pop(), thenStatements, thenResult));
        return lastBranchBC(sequence) + 1;
    }

    private int interpretAsIfTrueIfFalse(Stack<SqueakNode> stack,
                    Stack<SqueakNode> statements,
                    List<SqueakBytecodeNode> sequence) {
        int skip = lastBranchBC(sequence) + 1;
        assert offset > 0;
        SqueakNode branchCondition = branchCondition(stack);

        // the nodes making up our branch
        Vector<SqueakBytecodeNode> thenBranchNodes = new Vector<>(sequence.subList(firstBranchBC(sequence),
                        lastBranchBC(sequence) + 1));
        Vector<SqueakBytecodeNode> elseBranchNodes = null;

        SqueakBytecodeNode lastNode = thenBranchNodes.get(thenBranchNodes.size() - 1);
        if (lastNode instanceof UnconditionalJump && ((UnconditionalJump) lastNode).offset > 0) {
            // else branch
            thenBranchNodes.remove(lastNode);
            int firstElseBranchBC = firstBranchBC(sequence) + offset;
            skip = firstElseBranchBC + ((UnconditionalJump) lastNode).offset;
            elseBranchNodes = new Vector<>(sequence.subList(firstElseBranchBC, skip));
        }
        Stack<SqueakNode> subStack = new Stack<>();
        SqueakNode[] thenStatements = Decompiler.blockFrom(thenBranchNodes, subStack);
        SqueakNode thenResult = subStack.empty() ? null : subStack.pop();
        SqueakNode[] elseStatements = Decompiler.blockFrom(elseBranchNodes, subStack);
        SqueakNode elseResult = subStack.empty() ? null : subStack.pop();
        IfThenNode ifThenNode = new IfThenNode(method,
                        branchCondition,
                        thenStatements,
                        thenResult,
                        elseStatements,
                        elseResult);
        assert subStack.empty();
        if (thenResult != null || elseResult != null) {
            stack.push(ifThenNode);
        } else {
            statements.push(ifThenNode);
        }
        return skip;
    }

    private SqueakNode branchCondition(Stack<SqueakNode> stack) {
        SqueakNode branchCondition;
        if (isIfTrue) {
            branchCondition = IfTrueNodeGen.create(stack.pop());
        } else {
            branchCondition = IfFalseNodeGen.create(stack.pop());
        }
        return branchCondition;
    }

    @Override
    public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> sequence) {
        // nothing
    }
}
