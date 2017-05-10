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

    private int firstBranchBC() {
        return index + 1;
    }

    private int lastBranchBC() {
        return firstBranchBC() + offset - 1;
    }

    private boolean isLoop(Vector<SqueakBytecodeNode> sequence) {
        SqueakBytecodeNode lastNode = sequence.get(lastBranchBC());
        return (lastNode instanceof UnconditionalJump &&
                        ((UnconditionalJump) lastNode).offset < 0);
    }

    private void interpretAsLoop(Stack<SqueakNode> stack, Stack<SqueakNode> statements, Vector<SqueakBytecodeNode> sequence) {
        List<SqueakBytecodeNode> body = sequence.subList(firstBranchBC(), lastBranchBC());
        // remove jump back node
        UnconditionalJump jumpOutNode = (UnconditionalJump) sequence.get(lastBranchBC());
        sequence.set(lastBranchBC(), null);
        // we're the condition of a loop, the unconditional back jump will jump before us
        assert sequence.indexOf(jumpOutNode) + jumpOutNode.offset < index;

        Stack<SqueakNode> subStack = new Stack<>();
        LoopNode node = Truffle.getRuntime().createLoopNode(new LoopRepeatingNode(
                        method,
                        branchCondition(stack),
                        blockFrom(body, sequence, subStack)));
        if (!stack.empty() && stack.peek() instanceof ExtendedStoreNode) {
            statements.push(stack.pop());
        }
        statements.push(new WhileNode(method, index, node));
    }

    @SuppressWarnings("static-method")
    private boolean isIfNil(Stack<SqueakNode> stack) {
        return stack.peek() instanceof IfNilCheck;
    }

    @Override
    public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> statements, Vector<SqueakBytecodeNode> sequence) {
        if (isLoop(sequence)) {
            interpretAsLoop(stack, statements, sequence);
        } else if (isIfNil(stack)) {
            interpretAsIfNil(stack, sequence);
        } else {
            interpretAsIfTrueIfFalse(stack, statements, sequence);
        }
    }

    private void interpretAsIfNil(Stack<SqueakNode> stack, Vector<SqueakBytecodeNode> sequence) {
        Vector<SqueakBytecodeNode> thenBranchNodes = new Vector<>(sequence.subList(firstBranchBC(), lastBranchBC() + 1));
        Stack<SqueakNode> subStack = new Stack<>();
        SqueakNode[] thenStatements = blockFrom(thenBranchNodes, sequence, subStack);
        SqueakNode thenResult = subStack.empty() ? null : subStack.pop();
        stack.push(new IfNilCheck((IfNilCheck) stack.pop(), thenStatements, thenResult));
    }

    private void interpretAsIfTrueIfFalse(Stack<SqueakNode> stack, Stack<SqueakNode> statements, Vector<SqueakBytecodeNode> sequence) {
        assert offset > 0;
        SqueakNode branchCondition = branchCondition(stack);

        // the nodes making up our branch
        Vector<SqueakBytecodeNode> thenBranchNodes = new Vector<>(sequence.subList(firstBranchBC(), lastBranchBC() + 1));
        Vector<SqueakBytecodeNode> elseBranchNodes = null;

        SqueakBytecodeNode lastNode = thenBranchNodes.get(thenBranchNodes.size() - 1);
        if (lastNode instanceof UnconditionalJump) {
            // else branch
            thenBranchNodes.remove(lastNode);
            sequence.set(sequence.indexOf(lastNode), null);
            assert ((UnconditionalJump) lastNode).offset > 0;
            int firstElseBranchBC = firstBranchBC() + offset;
            elseBranchNodes = new Vector<>(sequence.subList(firstElseBranchBC, firstElseBranchBC + ((UnconditionalJump) lastNode).offset));
        }
        Stack<SqueakNode> subStack = new Stack<>();
        SqueakNode[] thenStatements = blockFrom(thenBranchNodes, sequence, subStack);
        SqueakNode thenResult = subStack.empty() ? null : subStack.pop();
        SqueakNode[] elseStatements = blockFrom(elseBranchNodes, sequence, subStack);
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
    }

    private static SqueakNode[] blockFrom(List<SqueakBytecodeNode> nodes, Vector<SqueakBytecodeNode> sequence, Stack<SqueakNode> subStack) {
        if (nodes == null)
            return null;
        Stack<SqueakNode> subStatements = new Stack<>();
        for (int i = 0; i < nodes.size(); i++) {
            SqueakBytecodeNode node = nodes.get(i);
            if (node != null) {
                if (sequence.contains(node)) {
                    int size = subStatements.size();
                    node.interpretOn(subStack, subStatements, sequence);
                    if (!subStack.empty() && subStatements.size() > size) {
                        // some statement was discovered, but something remains on the stack.
                        // this means the statement didn't consume everything from under itself,
                        // and we need to insert the stack item before the last statement.
                        // FIXME: can these asserts be wrong?
                        assert subStatements.size() == size + 1;
                        assert subStack.size() == 1;
                        subStatements.insertElementAt(subStack.pop(), size);
                    }
                    sequence.set(sequence.indexOf(node), null);
                }
            }
        }
        return subStatements.toArray(new SqueakNode[0]);
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
