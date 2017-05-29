package de.hpi.swa.trufflesqueak.instrumentation;

import java.util.Arrays;
import java.util.List;

import com.oracle.truffle.api.instrumentation.InstrumentableFactory.WrapperNode;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.nodes.NodeVisitor;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ConstantNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.LiteralVariableNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushActiveContextNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushClosureNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushNewArrayNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ReceiverNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ReturnNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ReturnTopFromBlockNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.jump.IfNilCheck;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.jump.IfThenNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.jump.IfTrue;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.jump.LoopRepeatingNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.send.AbstractSend;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.send.SingleExtendedSuperNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotWriteNode;
import de.hpi.swa.trufflesqueak.nodes.context.MethodLiteralNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectAtPutNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNode;

public class PrettyPrintVisitor implements NodeVisitor {
    private static final String INDENT = "  ";
    private final StringBuilder sb = new StringBuilder();
    private int padding = 0;

    public boolean visit(Node node) {
        if (node instanceof WrapperNode) {
            return false;
        } else if (node instanceof SqueakNode) {
            visitSqueakNode((SqueakNode) node);
            return false;
        } else if (node instanceof LoopNode) {
            return true;
        } else {
            append(node);
            return false;
        }
    }

    void visitSqueakNode(SqueakNode node) {
        node.accept(this);
    }

    public void visit(PrimitiveNode node) {
        append("<prim: ").append(node).append('>');
    }

    public void visit(ConstantNode node) {
        append(node.constant);
    }

    @SuppressWarnings("unused")
    public void visit(PushActiveContextNode node) {
        append("thisContext");
    }

    public void visit(PushClosureNode node) {
        append('[').newline().indent();
        Arrays.stream(node.compiledBlock.getBytecodeAST()).forEach(n -> visitStatement(n));
        dedent().append(']');
    }

    public void visit(PushNewArrayNode node) {
        if (node.popIntoArrayNodes == null) {
            append("Array new: ").append(node.arraySize);
        } else {
            append('{').newline().indent();
            Arrays.stream(node.popIntoArrayNodes).forEach(n -> visitStatement(n));
            dedent().append('}');
        }
    }

    @SuppressWarnings("unused")
    public void visit(ReceiverNode node) {
        append("self");
    }

    public void visit(ReturnNode node) {
        if (!(node instanceof ReturnTopFromBlockNode)) {
            append("^ ");
        }
        NodeUtil.forEachChild(node, this);
    }

    public void visit(IfNilCheck node) {
        visitWithParens(node.checkNode);
        newline().indent().append(node.runIfNil ? " ifNil: [" : " ifNotNil: [");
        Arrays.stream(node.statements).forEach(n -> visitStatement(n));
        visitSqueakNode(node.result);
        dedent().append("]");
    }

    public void visit(IfThenNode node) {
        visitWithParens(node.conditionNode);
        newline().indent();
        if (node.conditionNode instanceof IfTrue) {
            prettyPrintBranch("ifFalse:", node.thenNodes, node.thenResult);
            prettyPrintBranch("ifTrue:", node.elseNodes, node.elseResult);
        } else {
            prettyPrintBranch("ifTrue:", node.thenNodes, node.thenResult);
            prettyPrintBranch("ifFalse:", node.elseNodes, node.elseResult);
        }
        dedent();
    }

    private void prettyPrintBranch(String selector, SqueakNode[] branch, SqueakNode result) {
        if (branch == null && result == null)
            return;
        append(selector).append(" [").newline().indent();
        if (branch != null)
            Arrays.stream(branch).forEach(n -> visitStatement(n));
        if (result != null)
            visitStatement(result);
        dedent().append(']');
    }

    public void visit(LoopRepeatingNode node) {
        append('[').newline().indent();
        Arrays.stream(node.conditionBodyNodes).forEach(n -> visitStatement(n));
        visitStatement(node.conditionNode);
        dedent();
        if (node.conditionNode instanceof IfTrue) {
            append("] whileFalse: [");
        } else {
            append("] whileTrue: [");
        }
        newline().indent();
        Arrays.stream(node.bodyNodes).forEach(n -> visitStatement(n));
        dedent().append("]");
    }

    public void visit(AbstractSend node) {
        if (node instanceof SingleExtendedSuperNode) {
            append("super ");
        } else {
            visitWithParens(node.receiverNode);
        }
        String[] splitSelector = node.selector.toString().split(":");
        if (splitSelector.length == 1 && !splitSelector[0].matches("[A-Za-z]")) {
            append(' ').append(node.selector);
            if (node.argumentNodes.length == 1) {
                append(' ');
                visitWithParens(node.argumentNodes[0]);
            }
        } else {
            assert node.argumentNodes.length == splitSelector.length;
            for (int i = 0; i < node.argumentNodes.length; i++) {
                append(' ').append(splitSelector[i]).append(": ");
                visitWithParens(node.argumentNodes[i]);
            }
        }
    }

    public void visit(FrameSlotReadNode node) {
        append("t").append(node.slot.getIdentifier());
    }

    public void visit(FrameSlotWriteNode node) {
        append("t").append(node.slot.getIdentifier()).append(" := ");
        NodeUtil.forEachChild(node, this);
    }

    public void visit(MethodLiteralNode node) {
        if (node.literal instanceof PointersObject && ((PointersObject) node.literal).size() == 2) {
            append(((PointersObject) node.literal).at0(0));
        } else {
            append(node.literal);
        }
    }

    public void visit(ObjectAtNode node) {
        NodeUtil.forEachChild(node, this);
        append(" instVarAt: ").append(node.index);
    }

    public void visit(LiteralVariableNode node) {
        NodeUtil.forEachChild(node.valueNode, this);
    }

    public void visit(ObjectAtPutNode node) {
        List<Node> children = NodeUtil.findNodeChildren(node);
        visitWithParens(children.get(0));
        append(" instVarAt: ").append(node.index).append(" put: ");
        visitWithParens(children.get(1));
    }

    public void visit(Node[] nodes) {
        for (Node node : nodes) {
            visitStatement(node);
        }
    }

    public void visit(CompiledCodeObject cc) {
        append(cc.toString()).newline().indent();
        visit(cc.getBytecodeAST());
        dedent();
    }

    @SuppressWarnings("unused")
    boolean visit(WrapperNode node) {
        return false;
    }

    public void visit(SqueakNode node) {
        NodeUtil.forEachChild(node, this);
    }

    void visitWithParens(Node node) {
        append('(');
        visit(node);
        append(')');
    }

    void visitStatement(Node node) {
        visit(node);
        append('.').newline();
    }

    PrettyPrintVisitor newline() {
        sb.append('\n');
        for (int i = 0; i < padding; i++) {
            sb.append(INDENT);
        }
        return this;
    }

    PrettyPrintVisitor append(String string) {
        sb.append(string);
        return this;
    }

    PrettyPrintVisitor append(char c) {
        sb.append(c);
        return this;
    }

    PrettyPrintVisitor append(int arraySize) {
        sb.append(arraySize);
        return this;
    }

    PrettyPrintVisitor append(Object obj) {
        sb.append(obj);
        return this;
    }

    PrettyPrintVisitor indent() {
        padding += 2;
        sb.append(INDENT).append(INDENT);
        return this;
    }

    PrettyPrintVisitor dedent() {
        padding -= 2;
        int length = sb.length();
        int dedentLength = INDENT.length() * 2;
        // only delete whitespace
        if (sb.subSequence(length - dedentLength, length).toString().trim().length() == 0) {
            sb.delete(length - dedentLength, length);
        }
        return this;
    }

    public String build() {
        return sb.toString();
    }

    public int length() {
        return sb.length();
    }

}