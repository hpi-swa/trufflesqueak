package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Instrumentable;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.source.SourceSection;

import de.hpi.swa.trufflesqueak.exceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNodeWithCode;

@Instrumentable(factory = AbstractBytecodeNodeWrapper.class)
public abstract class AbstractBytecodeNode extends SqueakNodeWithCode {
    @CompilationFinal protected final int numBytecodes;
    @CompilationFinal protected final int index;
    @CompilationFinal private SourceSection sourceSection;
    @CompilationFinal private int lineNumber = 1;

    protected AbstractBytecodeNode(AbstractBytecodeNode original) {
        super(original.code);
        index = original.index;
        numBytecodes = original.numBytecodes;
        setSourceSection(original.getSourceSection());
    }

    public AbstractBytecodeNode(CompiledCodeObject code, int index) {
        this(code, index, 1);
    }

    public AbstractBytecodeNode(CompiledCodeObject code, int index, int numBytecodes) {
        super(code);
        this.index = index;
        this.numBytecodes = numBytecodes;
    }

    @Override
    public Object executeRead(VirtualFrame frame) {
        throw new SqueakException("Should call executeVoid instead");
    }

    public int executeInt(VirtualFrame frame) {
        assert index >= 0; // Inner nodes are not allowed to be executed here
        executeVoid(frame);
        return index + numBytecodes;
    }

    public abstract void executeVoid(VirtualFrame frame);

    public int getIndex() {
        return index;
    }

    public int getNumBytecodes() {
        return numBytecodes;
    }

    @Override
    public SourceSection getSourceSection() {
        if (sourceSection == null) {
            sourceSection = code.getSource().createSection(lineNumber);
        }
        return sourceSection;
    }

    @Override
    protected boolean isTaggedWith(Class<?> tag) {
        return tag == StandardTags.StatementTag.class;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    @Override
    public void setSourceSection(SourceSection section) {
        sourceSection = section;
    }
}
