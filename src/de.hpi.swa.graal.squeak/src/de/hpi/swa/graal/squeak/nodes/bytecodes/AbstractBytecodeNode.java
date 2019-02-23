package de.hpi.swa.graal.squeak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.FrameMarker;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public abstract class AbstractBytecodeNode extends AbstractNode {
    protected final CompiledCodeObject code;
    protected final int numBytecodes;
    protected final int index;

    private SourceSection sourceSection;
    private int lineNumber = 1;

    protected AbstractBytecodeNode(final AbstractBytecodeNode original) {
        code = original.code;
        index = original.index;
        numBytecodes = original.numBytecodes;
        sourceSection = original.getSourceSection();
    }

    public AbstractBytecodeNode(final CompiledCodeObject code, final int index) {
        this(code, index, 1);
    }

    public AbstractBytecodeNode(final CompiledCodeObject code, final int index, final int numBytecodes) {
        this.code = code;
        this.index = index;
        this.numBytecodes = numBytecodes;
    }

    public abstract void executeVoid(VirtualFrame frame);

    public final int getSuccessorIndex() {
        return index + numBytecodes;
    }

    public final int getIndex() {
        return index;
    }

    public final int getNumBytecodes() {
        return numBytecodes;
    }

    protected final ContextObject getContext(final VirtualFrame frame) {
        return FrameAccess.getContext(frame, code);
    }

    protected final FrameMarker getMarker(final VirtualFrame frame) {
        return FrameAccess.getMarker(frame, code);
    }

    @Override
    @TruffleBoundary
    public final SourceSection getSourceSection() {
        if (sourceSection == null) {
            /*
             * sourceSection requested when logging transferToInterpreters. Therefore, do not
             * trigger another TTI here which otherwise would cause endless recursion in Truffle
             * debug code.
             */
            sourceSection = code.getSource().createSection(lineNumber);
        }
        return sourceSection;
    }

    public final void setLineNumber(final int lineNumber) {
        this.lineNumber = lineNumber;
    }
}
