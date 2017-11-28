package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.instrumentation.StandardTags;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class StoreRemoteTempNode extends RemoteTempBytecodeNode {
    private final int indexInArray;
    private final int indexOfArray;

    public StoreRemoteTempNode(CompiledCodeObject cm, int idx, int indexInArray, int indexOfArray) {
        super(cm, idx);
        this.indexInArray = indexInArray;
        this.indexOfArray = indexOfArray;
    }

    @Override
    protected boolean isTaggedWith(Class<?> tag) {
        if (tag == StandardTags.StatementTag.class) {
            return getSourceSection().isAvailable();
        }
        return false;
    }
}
