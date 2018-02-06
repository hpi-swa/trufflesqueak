package de.hpi.swa.trufflesqueak.instrumentation;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.AbstractBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushBytecodes.PushClosureNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ReturnBytecodes.ReturnTopFromBlockNode;
import de.hpi.swa.trufflesqueak.util.SqueakBytecodeDecoder;

public class CompiledCodeObjectPrinter {

    public static String getString(CompiledCodeObject code) {
        StringBuilder sb = new StringBuilder();
        long index = 1;
        long indent = 0;
        byte[] bytes = code.getBytes();
        // TODO: is a new BytecodeSequenceNode needed here?
        AbstractBytecodeNode[] bytecodeNodes = new SqueakBytecodeDecoder(code).decode();
        for (AbstractBytecodeNode node : bytecodeNodes) {
            if (node == null) {
                continue;
            }
            sb.append(index + " ");
            for (int j = 0; j < indent; j++) {
                sb.append(" ");
            }
            long numBytecodes = node.getNumBytecodes();
            sb.append("<");
            for (int j = node.getIndex(); j < node.getIndex() + numBytecodes; j++) {
                if (j > node.getIndex()) {
                    sb.append(" ");
                }
                if (j < bytes.length) {
                    sb.append(String.format("%02X", bytes[j]));
                }
            }
            sb.append("> ");
            sb.append(node.toString());
            if (node.getIndex() < bytecodeNodes.length - 1) {
                sb.append("\n");
            }
            if (PushClosureNode.class.isAssignableFrom(node.getClass())) {
                indent++;
            } else if (ReturnTopFromBlockNode.class.isAssignableFrom(node.getClass())) {
                indent--;
            }
            index++;
        }
        return sb.toString();
    }
}
