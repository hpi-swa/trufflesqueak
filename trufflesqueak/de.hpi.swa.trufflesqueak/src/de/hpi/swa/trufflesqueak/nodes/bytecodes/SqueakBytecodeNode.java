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
    @CompilationFinal protected final int index;
    @CompilationFinal private SourceSection sourceSection;
    public int lineNumber = 1;

    protected SqueakBytecodeNode(SqueakBytecodeNode original) {
        super(original.code);
        index = original.index;
        numBytecodes = original.numBytecodes;
        setSourceSection(original.getSourceSection());
    }

    public SqueakBytecodeNode(CompiledCodeObject code, int index, int numBytecodes) {
        super(code);
        this.index = index;
        this.numBytecodes = numBytecodes;
    }

    public SqueakBytecodeNode(CompiledCodeObject code, int index) {
        this(code, index, 1);
    }

    public int executeInt(VirtualFrame frame) {
        if (index < 0) {
            throw new RuntimeException("Inner nodes are not allowed to be executed here");
        }
        executeVoid(frame);
        return index + numBytecodes;
    }

    public abstract void executeVoid(VirtualFrame frame);

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        throw new RuntimeException("Should call executeVoid instead");
    }

    public int getNumBytecodes() {
        return numBytecodes;
    }

    public int getIndex() {
        return index;
    }

    @Override
    protected boolean isTaggedWith(Class<?> tag) {
        return tag == StandardTags.StatementTag.class;
    }

    @Override
    public void setSourceSection(SourceSection section) {
        sourceSection = section;
    }

    @Override
    public SourceSection getSourceSection() {
        if (sourceSection == null) {
            sourceSection = code.getSource().createSection(lineNumber);
        }
        return sourceSection;
    }
}
