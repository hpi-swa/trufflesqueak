/*
 * Copyright (c) 2017-2023 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2023 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;

public abstract class AbstractBytecodeNode extends AbstractNode {
    protected final int index;
    private final int successorIndex;

    public AbstractBytecodeNode(final CompiledCodeObject code, final int index) {
        this(code, index, 1);
    }

    public AbstractBytecodeNode(final CompiledCodeObject code, final int index, final int numBytecodes) {
        final int initialPC = code.getInitialPC();
        this.index = initialPC + index;
        successorIndex = initialPC + index + numBytecodes;
    }

    public AbstractBytecodeNode(final AbstractBytecodeNode original) {
        index = original.index;
        successorIndex = original.successorIndex;
    }

    public abstract void executeVoid(VirtualFrame frame);

    public final int getSuccessorIndex() {
        return successorIndex;
    }

    public final int getNumBytecodes() {
        return successorIndex - index;
    }

    @Override
    public final SourceSection getSourceSection() {
        CompilerAsserts.neverPartOfCompilation();
        final CompiledCodeObject code = getCode();
        final Source source = code.getSource();
        if (CompiledCodeObject.SOURCE_UNAVAILABLE_CONTENTS.contentEquals(source.getCharacters())) {
            return source.createUnavailableSection();
        } else {
            final int lineNumber = code.findLineNumber(index - code.getInitialPC());
            return source.createSection(lineNumber);
        }
    }
}
