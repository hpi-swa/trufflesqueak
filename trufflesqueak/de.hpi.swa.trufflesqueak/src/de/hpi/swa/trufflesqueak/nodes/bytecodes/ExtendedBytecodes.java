package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.push.PushLiteralConstantNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.push.PushLiteralVariableNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.push.PushReceiverVariableNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.push.PushTemporaryLocationNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.store.PopIntoAssociationNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.store.PopIntoReceiverVariableNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.store.PopIntoTemporaryLocationNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.store.StoreIntoAssociationNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.store.StoreIntoReceiverVariableNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.store.StoreIntoTempNode;

public abstract class ExtendedBytecodes {

    public static AbstractBytecodeNode createPush(CompiledCodeObject code, int index, int numBytecodes, int nextByte) {
        int variableIndex = variableIndex(nextByte);
        switch (variableType(nextByte)) {
            case 0:
                return new PushReceiverVariableNode(code, index, numBytecodes, variableIndex);
            case 1:
                return new PushTemporaryLocationNode(code, index, numBytecodes, variableIndex);
            case 2:
                return new PushLiteralConstantNode(code, index, numBytecodes, variableIndex);
            case 3:
                return new PushLiteralVariableNode(code, index, numBytecodes, variableIndex);
            default:
                throw new RuntimeException("unexpected type for ExtendedPush");
        }
    }

    public static AbstractBytecodeNode createStoreInto(CompiledCodeObject code, int index, int numBytecodes, int nextByte) {
        int variableIndex = variableIndex(nextByte);
        switch (variableType(nextByte)) {
            case 0:
                return new StoreIntoReceiverVariableNode(code, index, numBytecodes, variableIndex);
            case 1:
                return new StoreIntoTempNode(code, index, numBytecodes, variableIndex);
            case 2:
                return new UnknownBytecodeNode(code, index, numBytecodes, nextByte);
            case 3:
                return new StoreIntoAssociationNode(code, index, numBytecodes, variableIndex);
            default:
                throw new RuntimeException("illegal ExtendedStore bytecode");
        }
    }

    public static AbstractBytecodeNode createPopInto(CompiledCodeObject code, int index, int numBytecodes, int nextByte) {
        int variableIndex = variableIndex(nextByte);
        switch (variableType(nextByte)) {
            case 0:
                return new PopIntoReceiverVariableNode(code, index, numBytecodes, variableIndex);
            case 1:
                return new PopIntoTemporaryLocationNode(code, index, numBytecodes, variableIndex);
            case 2:
                return new UnknownBytecodeNode(code, index, numBytecodes, nextByte);
            case 3:
                return new PopIntoAssociationNode(code, index, numBytecodes, variableIndex);
            default:
                throw new RuntimeException("illegal ExtendedStore bytecode");
        }
    }

    protected static byte variableIndex(int i) {
        return (byte) (i & 63);
    }

    protected static byte variableType(int i) {
        return (byte) ((i >> 6) & 3);
    }
}
