package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Instrumentable;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.source.SourceSection;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNodeWithCode;

@Instrumentable(factory = SqueakBytecodeNodeWrapper.class)
public abstract class SqueakBytecodeNode extends SqueakNodeWithCode {
    @CompilationFinal protected final int numBytecodes;
    @CompilationFinal protected final int successorIndex;
    @CompilationFinal private SourceSection sourceSection;
    public int lineNumber = 1;

    protected SqueakBytecodeNode(SqueakBytecodeNode original) {
        super(original.code);
        numBytecodes = original.numBytecodes;
        successorIndex = original.successorIndex;
        setSourceSection(original.getSourceSection());
    }

    public SqueakBytecodeNode(CompiledCodeObject code, int index, int numBytecodes) {
        super(code);
        this.numBytecodes = numBytecodes;
        this.successorIndex = index + numBytecodes;
    }

    public SqueakBytecodeNode(CompiledCodeObject code, int index) {
        this(code, index, 1);
    }

    public int executeInt(VirtualFrame frame) {
        if (successorIndex < 0) {
            throw new RuntimeException("Inner nodes are not allowed to be executed here");
        }
        executeVoid(frame);
        return successorIndex;
    }

    public void executeVoid(VirtualFrame frame) {
        executeGeneric(frame);
    }

    public int getSuccessorIndex() {
        return successorIndex;
    }

    public int getNumBytecodes() {
        return numBytecodes;
    }

    public int getIndex() {
        return successorIndex - numBytecodes;
    }

    @Override
    protected boolean isTaggedWith(Class<?> tag) {
        return tag == StandardTags.StatementTag.class;
    }
}
