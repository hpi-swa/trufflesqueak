package de.hpi.swa.graal.squeak.util;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.nodes.bytecodes.AbstractBytecodeNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.PushBytecodes.PushClosureNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.ReturnBytecodes.ReturnTopFromBlockNode;

public class CompiledCodeObjectPrinter {

    public static String getString(final CompiledCodeObject code) {
        final StringBuilder sb = new StringBuilder();
        long index = 1;
        long indent = 0;
        final byte[] bytes = code.getBytes();
        // TODO: is a new BytecodeSequenceNode needed here?
        final AbstractBytecodeNode[] bytecodeNodes = SqueakBytecodeDecoder.decode(code);
        for (AbstractBytecodeNode node : bytecodeNodes) {
            if (node == null) {
                continue;
            }
            sb.append(index + " ");
            for (int j = 0; j < indent; j++) {
                sb.append(" ");
            }
            final long numBytecodes = node.getNumBytecodes();
            sb.append("<");
            for (int j = node.getIndex(); j < node.getIndex() + numBytecodes; j++) {
                if (j > node.getIndex()) {
                    sb.append(" ");
                }
                if (j < bytes.length) {
                    sb.append(MiscUtils.format("%02X", bytes[j]));
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
