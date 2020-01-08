/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.FrameMarker;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;
import de.hpi.swa.graal.squeak.util.SqueakBytecodeDecoder;

public abstract class AbstractBytecodeNode extends AbstractNode {
    protected final CompiledCodeObject code;
    protected final int numBytecodes;
    protected final int index;

    private SourceSection sourceSection;

    protected AbstractBytecodeNode(final AbstractBytecodeNode original) {
        code = original.code;
        index = original.index;
        numBytecodes = original.numBytecodes;
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
    public final SourceSection getSourceSection() {
        if (sourceSection == null) {
            final Source source = code.getSource();
            if (source.getCharacters().equals(CompiledCodeObject.SOURCE_UNAVAILABLE)) {
                sourceSection = source.createUnavailableSection();
            } else {
                final int lineNumber = SqueakBytecodeDecoder.findLineNumber(code, index);
                sourceSection = source.createSection(lineNumber);
            }
        }
        return sourceSection;
    }
}
