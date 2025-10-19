/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
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
import de.hpi.swa.trufflesqueak.nodes.ExecuteBytecodeNode;

public abstract class AbstractBytecodeNode extends AbstractNode {
    private final int successorIndex;
    private final byte successorStackPointer;

    public AbstractBytecodeNode(final int successorIndex, final int successorStackPointer) {
        this.successorIndex = successorIndex;
        this.successorStackPointer = (byte) successorStackPointer;
        assert this.successorStackPointer == successorStackPointer;
    }

    public AbstractBytecodeNode(final AbstractBytecodeNode original) {
        successorIndex = original.successorIndex;
        successorStackPointer = original.successorStackPointer;
    }

    public abstract void executeVoid(VirtualFrame frame);

    public final int getSuccessorIndex() {
        return successorIndex;
    }

    public final byte getSuccessorStackPointer() {
        return successorStackPointer;
    }

    @Override
    public final SourceSection getSourceSection() {
        CompilerAsserts.neverPartOfCompilation();
        final CompiledCodeObject code = getCode();
        final Source source = code.getSource();
        if (successorIndex == ExecuteBytecodeNode.LOCAL_RETURN_PC || // FIXME
                        CompiledCodeObject.SOURCE_UNAVAILABLE_CONTENTS.contentEquals(source.getCharacters())) {
            return source.createUnavailableSection();
        } else {
            final int lineNumber = code.findLineNumber(successorIndex);
            return source.createSection(lineNumber);
        }
    }
}
