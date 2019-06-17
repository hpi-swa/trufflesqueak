package de.hpi.swa.graal.squeak.model;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerAsserts;

public final class CompiledBlockObject extends CompiledCodeObject {
    private final int offset;

    private CompiledBlockObject(final CompiledCodeObject code, final CompiledMethodObject outerMethod, final int numArguments, final int numCopied, final int bytecodeOffset, final int blockSize) {
        super(code.image, 0, numCopied);
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
        numTemps = numCopied;
        numArgs = numArguments;
        ensureCorrectNumberOfStackSlots();
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
            assert index % image.flags.wordSize() == 0;
            return literals[index / image.flags.wordSize()];
        } else {
            return getMethod().at0(longIndex);
        }
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        String className = "UnknownClass";
        String selector = "unknownSelector";
        final ClassObject methodClass = getMethod().getMethodClass();
        if (methodClass != null) {
            className = methodClass.getClassName();
        }
        final NativeObject selectorObj = getMethod().getCompiledInSelector();
        if (selectorObj != null) {
            selector = selectorObj.asStringUnsafe();
        }
        return className + ">>" + selector;
    }

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
}
