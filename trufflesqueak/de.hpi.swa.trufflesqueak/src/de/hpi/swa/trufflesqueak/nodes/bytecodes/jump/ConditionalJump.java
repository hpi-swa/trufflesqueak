package de.hpi.swa.trufflesqueak.nodes.bytecodes.jump;

import java.util.Stack;
import java.util.Vector;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.LoopNode;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeNode;

public class ConditionalJump extends AbstractJump {
    @Child private LoopNode loopNode;
    @Child private IfThenNode ifThenNode;
    protected final int offset;
    private final boolean isIfTrue;

    private ConditionalJump(CompiledCodeObject cm, int idx, int off, boolean condition) {
        super(cm, idx);
        offset = off;
        isIfTrue = condition;
        loopNode = null;
        ifThenNode = null;
    }

    public ConditionalJump(CompiledCodeObject cm, int idx, int bytecode) {
        this(cm, idx, shortJumpOffset(bytecode), false);
    }

    public ConditionalJump(CompiledCodeObject cm, int idx, int bytecode, int parameter, boolean condition) {
        this(cm, idx + 1, longJumpOffset(bytecode, parameter), condition);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        if (ifThenNode != null) {
            return ifThenNode.executeGeneric(frame);
        } else {
            assert loopNode != null;
            loopNode.executeLoop(frame);
            return null;
        }
    }

    @Override
    public void interpretOn(Stack<SqueakNode> stack, Stack<SqueakNode> statements, Vector<SqueakBytecodeNode> sequence) {
        assert offset > 0;
        SqueakNode branchCondition = makeBranchCondition(isIfTrue, stack);

        // the nodes making up our branch
        int firstBranchBC = index + 1;
        Vector<SqueakBytecodeNode> thenBranchNodes = new Vector<>(sequence.subList(firstBranchBC, firstBranchBC + offset));
        Vector<SqueakBytecodeNode> elseBranchNodes = null;

        SqueakBytecodeNode lastNode = thenBranchNodes.get(thenBranchNodes.size() - 1);
        if (lastNode instanceof UnconditionalJump) {
            thenBranchNodes.remove(lastNode);
            sequence.set(sequence.indexOf(lastNode), null);
            UnconditionalJump jumpOutNode = (UnconditionalJump) lastNode;
            if (jumpOutNode.offset < 0) {
                // we're the condition of a loop, the unconditional back jump will jump before us
                assert sequence.indexOf(jumpOutNode) + jumpOutNode.offset < index;
                loopNode = Truffle.getRuntime().createLoopNode(new LoopRepeatingNode(
                                method,
                                branchCondition,
                                blockFrom(thenBranchNodes, sequence, new Stack<>())));
                statements.push(this);
                return;
            } else {
                // else branch
                assert jumpOutNode.offset > 0;
                int firstElseBranchBC = firstBranchBC + offset;
                elseBranchNodes = new Vector<>(sequence.subList(firstElseBranchBC, firstElseBranchBC + jumpOutNode.offset));
            }
        }
        Stack<SqueakNode> subStack = new Stack<>();
        SqueakNode[] thenStatements = blockFrom(thenBranchNodes, sequence, subStack);
        SqueakNode thenResult = subStack.empty() ? null : subStack.pop();
        SqueakNode[] elseStatements = blockFrom(elseBranchNodes, sequence, subStack);
        SqueakNode elseResult = subStack.empty() ? null : subStack.pop();
        ifThenNode = new IfThenNode(
                        method,
                        branchCondition,
                        thenStatements,
                        thenResult,
                        elseStatements,
                        elseResult);
        assert subStack.empty();
        if (thenResult != null || elseResult != null) {
            stack.push(this);
        } else {
            statements.push(this);
        }
    }

    private static SqueakNode makeBranchCondition(boolean isIfTrue, Stack<SqueakNode> stack) {
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
