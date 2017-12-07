package de.hpi.swa.trufflesqueak.nodes.bytecodes;

public abstract class ExtendedAccess extends Object {
    protected ExtendedAccess() {
    }

    protected static byte variableIndex(int i) {
        return (byte) (i & 63);
    }

    protected static byte variableType(int i) {
        return (byte) ((i >> 6) & 3);
    }
}
