package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakBytecodeNode;

public abstract class ExtendedAccess extends SqueakBytecodeNode {
    protected final byte type;
    protected final byte storeIdx;

    public ExtendedAccess(CompiledMethodObject cm, int index, int i) {
        super(cm, index);
        type = extractType(i);
        storeIdx = extractIndex(i);
    }

    protected static byte extractIndex(int i) {
        return (byte) (i & 63);
    }

    protected static byte extractType(int i) {
        return (byte) ((i >> 6) & 3);
    }
}