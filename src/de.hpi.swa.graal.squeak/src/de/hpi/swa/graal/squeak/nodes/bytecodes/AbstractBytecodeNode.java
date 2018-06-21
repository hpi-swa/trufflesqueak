package de.hpi.swa.graal.squeak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

import de.hpi.swa.graal.squeak.exceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.nodes.SqueakNodeWithCode;

public abstract class AbstractBytecodeNode extends SqueakNodeWithCode {
    @CompilationFinal protected final int numBytecodes;
    @CompilationFinal protected final int index;
    @CompilationFinal private SourceSection sourceSection;
    @CompilationFinal private int lineNumber = 1;

    protected AbstractBytecodeNode(final AbstractBytecodeNode original) {
        super(original.code);
        index = original.index;
        numBytecodes = original.numBytecodes;
        setSourceSection(original.getSourceSection());
    }

    public AbstractBytecodeNode(final CompiledCodeObject code, final int index) {
        this(code, index, 1);
    }

    public AbstractBytecodeNode(final CompiledCodeObject code, final int index, final int numBytecodes) {
        super(code);
        this.index = index;
        this.numBytecodes = numBytecodes;
    }

    @Override
    public Object executeRead(final VirtualFrame frame) {
        throw new SqueakException("Should call executeVoid instead");
    }

    public int executeInt(final VirtualFrame frame) {
        assert index >= 0; // Inner nodes are not allowed to be executed here
        executeVoid(frame);
        return getSuccessorIndex();
    }

    public abstract void executeVoid(VirtualFrame frame);

    public int getSuccessorIndex() {
        return index + numBytecodes;
    }

    public final int getIndex() {
        return index;
    }

    public final int getNumBytecodes() {
        return numBytecodes;
    }

    @Override
    @TruffleBoundary
    public final SourceSection getSourceSection() {
        if (sourceSection == null) {
            sourceSection = code.getSource().createSection(lineNumber);
        }
        return sourceSection;
    }

    public final void setLineNumber(final int lineNumber) {
        this.lineNumber = lineNumber;
    }

    private void setSourceSection(final SourceSection section) {
        sourceSection = section;
    }
}
