/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public abstract class AbstractBytecodeNode extends AbstractNode {
    protected final CompiledCodeObject code;
    protected final int index;
    private final int successorIndex;

    private SourceSection sourceSection;

    protected AbstractBytecodeNode(final AbstractBytecodeNode original) {
        code = original.code;
        index = original.index;
        successorIndex = original.successorIndex;
        sourceSection = original.sourceSection;
    }

    public AbstractBytecodeNode(final CompiledCodeObject code, final int index) {
        this(code, index, 1);
    }

    public AbstractBytecodeNode(final CompiledCodeObject code, final int index, final int numBytecodes) {
        this.code = code;
        final int initialPC = code.getInitialPC();
        this.index = initialPC + index;
        successorIndex = initialPC + index + numBytecodes;
    }

    public abstract void executeVoid(VirtualFrame frame);

    public final int getSuccessorIndex() {
        return successorIndex;
    }

    public final int getNumBytecodes() {
        return successorIndex - index;
    }

    protected final boolean hasContext(final VirtualFrame frame) {
        return FrameAccess.hasContext(frame, code);
    }

    protected final ContextObject getContext(final VirtualFrame frame) {
        return FrameAccess.getContext(frame, code);
    }

    protected final Object getContextOrNil(final VirtualFrame frame) {
        return FrameAccess.getContextOrNil(frame, code.getThisContextSlot());
    }

    protected final Object getMarkerOrNil(final VirtualFrame frame) {
        return FrameAccess.getMarkerOrNil(frame, code.getThisMarkerSlot());
    }

    @Override
    public final SourceSection getSourceSection() {
        CompilerAsserts.neverPartOfCompilation();
        if (sourceSection == null) {
            final Source source = code.getSource();
            if (CompiledCodeObject.SOURCE_UNAVAILABLE_CONTENTS.contentEquals(source.getCharacters())) {
                sourceSection = source.createUnavailableSection();
            } else {
                final int lineNumber = code.findLineNumber(index - code.getInitialPC());
                sourceSection = source.createSection(lineNumber);
            }
        }
        return sourceSection;
    }
}
