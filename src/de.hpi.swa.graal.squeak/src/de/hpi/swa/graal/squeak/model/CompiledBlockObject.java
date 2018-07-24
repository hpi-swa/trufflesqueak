package de.hpi.swa.graal.squeak.model;

import java.util.Arrays;

public final class CompiledBlockObject extends CompiledCodeObject {
    private final CompiledMethodObject outerMethod;
    private final int offset;

    public static CompiledBlockObject create(final CompiledCodeObject code, final CompiledMethodObject outerMethod, final int numArgs, final int numCopied, final int bytecodeOffset,
                    final int blockSize) {
        return new CompiledBlockObject(code, outerMethod, numArgs, numCopied, bytecodeOffset, blockSize);
    }

    private CompiledBlockObject(final CompiledCodeObject code, final CompiledMethodObject outerMethod, final int numArgs, final int numCopied, final int bytecodeOffset, final int blockSize) {
        super(code.image, numCopied);
        this.outerMethod = outerMethod;
        final int additionalOffset = code instanceof CompiledBlockObject ? ((CompiledBlockObject) code).getOffset() : 0;
        this.offset = additionalOffset + bytecodeOffset;
        final Object[] outerLiterals = outerMethod.getLiterals();
        final Object[] blockLiterals = new Object[outerLiterals.length + 1];
        blockLiterals[0] = makeHeader(numArgs, numCopied, code.numLiterals, false, outerMethod.needsLargeFrame);
        for (int i = 1; i < outerLiterals.length; i++) {
            blockLiterals[i] = outerLiterals[i];
        }
        blockLiterals[blockLiterals.length - 1] = outerMethod; // last literal is back pointer to
                                                               // method
        this.literals = blockLiterals;
        this.bytes = Arrays.copyOfRange(code.getBytes(), bytecodeOffset, (bytecodeOffset + blockSize));
        decodeHeader();
    }

    private CompiledBlockObject(final CompiledBlockObject original) {
        super(original);
        outerMethod = original.outerMethod;
        offset = original.offset;
    }

    public Object at0(final long longIndex) {
        final int index = (int) longIndex;
        if (index < getBytecodeOffset() - getOffset()) {
            assert index % BYTES_PER_WORD == 0;
            return literals[index / BYTES_PER_WORD];
        } else {
            return getMethod().at0(longIndex);
        }
    }

    @Override
    public String toString() {
        String className = "UnknownClass";
        String selector = "unknownSelector";
        final ClassObject classObject = getCompiledInClass();
        if (classObject != null) {
            className = classObject.nameAsClass();
        }
        final NativeObject selectorObj = getCompiledInSelector();
        if (selectorObj != null) {
            selector = selectorObj.asString();
        }
        return className + ">>" + selector;
    }

    public NativeObject getCompiledInSelector() {
        return outerMethod.getCompiledInSelector();
    }

    public ClassObject getCompiledInClass() {
        return outerMethod.getCompiledInClass();
    }

    public CompiledMethodObject getMethod() {
        return outerMethod;
    }

    public int getInitialPC() {
        return outerMethod.getInitialPC() + getOffset();
    }

    public int getOffset() {
        return offset;
    }

    public AbstractSqueakObject shallowCopy() {
        return new CompiledBlockObject(this);
    }

    public int size() {
        return outerMethod.size();
    }
}
