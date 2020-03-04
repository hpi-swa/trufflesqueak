/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.model;

import java.util.Arrays;

import de.hpi.swa.graal.squeak.image.SqueakImageConstants;
import de.hpi.swa.graal.squeak.image.SqueakImageWriter;

public final class CompiledBlockObject extends CompiledCodeObject {
    private final int offset;

    private CompiledBlockObject(final CompiledCodeObject code, final CompiledMethodObject outerMethod, final int numArguments, final int numCopied, final int bytecodeOffset, final int blockSize) {
        super(code.image, -1, numCopied);
        final int additionalOffset = code instanceof CompiledBlockObject ? ((CompiledBlockObject) code).getOffset() : 0;
        offset = additionalOffset + bytecodeOffset;
        final Object[] outerLiterals = outerMethod.getLiterals();
        final int outerLiteralsLength = outerLiterals.length;
        literals = new Object[outerLiteralsLength + 1];
        literals[0] = makeHeader(numArguments, numCopied, code.numLiterals, false, outerMethod.needsLargeFrame);
        System.arraycopy(outerLiterals, 1, literals, 1, outerLiteralsLength - 1);
        literals[outerLiteralsLength] = outerMethod; // Last literal is back pointer to method.
        bytes = Arrays.copyOfRange(code.getBytes(), bytecodeOffset, bytecodeOffset + blockSize);
        /* Instead of calling decodeHeader(), set fields directly. */
        numLiterals = code.numLiterals;
        hasPrimitive = false;
        needsLargeFrame = outerMethod.needsLargeFrame;
        numTemps = numArguments + numCopied;
        numArgs = numArguments;
        ensureCorrectNumberOfStackSlots();
        initializeCallTargetUnsafe();
    }

    private CompiledBlockObject(final CompiledBlockObject original) {
        super(original);
        offset = original.offset;
    }

    public static CompiledBlockObject create(final CompiledCodeObject code, final CompiledMethodObject outerMethod, final int numArgs, final int numCopied, final int bytecodeOffset,
                    final int blockSize) {
        return new CompiledBlockObject(code, outerMethod, numArgs, numCopied, bytecodeOffset, blockSize);
    }

    public Object at0(final long longIndex) {
        final int index = (int) longIndex;
        if (index < getBytecodeOffset() - getOffset()) {
            assert index % SqueakImageConstants.WORD_SIZE == 0;
            return literals[index / SqueakImageConstants.WORD_SIZE];
        } else {
            return getMethod().at0(longIndex);
        }
    }

    @Override
    public String toString() {
        return "[] in " + getMethod().toString() + " (offset: " + offset + ")";
    }

    @Override
    public CompiledMethodObject getMethod() {
        return (CompiledMethodObject) literals[literals.length - 1];
    }

    public int getInitialPC() {
        return getMethod().getInitialPC() + getOffset();
    }

    public int getOffset() {
        return offset;
    }

    public CompiledBlockObject shallowCopy() {
        return new CompiledBlockObject(this);
    }

    @Override
    public int size() {
        return getMethod().size();
    }

    @Override
    public void write(final SqueakImageWriter writerNode) {
        /*
         * This should not be reached, unless GraalSqueak supports FullBlockClosures. Print an error
         * instead of crashing for now.
         */
        image.printToStdErr("Unexpected CompiledBlockObject: " + this);
    }
}
