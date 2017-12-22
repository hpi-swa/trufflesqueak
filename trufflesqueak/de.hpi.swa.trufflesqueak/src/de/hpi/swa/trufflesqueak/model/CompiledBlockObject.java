package de.hpi.swa.trufflesqueak.model;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

public class CompiledBlockObject extends CompiledCodeObject {
    @CompilationFinal private final CompiledMethodObject outerMethod;
    @CompilationFinal private final int numCopiedValues;

    public CompiledBlockObject(CompiledCodeObject code, int numArgs, int numCopied) {
        super(code.image);
        outerMethod = code.getMethod();
        numCopiedValues = numCopied;
        Object[] outerLiterals = outerMethod.getLiterals();
        outerLiterals = Arrays.copyOf(outerLiterals, outerLiterals.length - 1);
        int baseHdr = ((numArgs & 0xF) << 24) | ((numCopied & 0x3F) << 18);
        outerLiterals[0] = baseHdr; // replace header
        outerLiterals[outerLiterals.length - 1] = outerMethod; // last literal is back pointer to method
        this.literals = outerLiterals;
        decodeHeader();
    }

    private CompiledBlockObject(CompiledBlockObject original) {
        super(original);
        outerMethod = original.outerMethod;
        numCopiedValues = original.numCopiedValues;
    }

    @Override
    public NativeObject getCompiledInSelector() {
        return outerMethod.getCompiledInSelector();
    }

    @Override
    public ClassObject getCompiledInClass() {
        return outerMethod.getCompiledInClass();
    }

    @Override
    public int getNumCopiedValues() {
        return numCopiedValues;
    }

    @Override
    public int getNumTemps() {
        return super.getNumTemps() + numCopiedValues;
    }

    @Override
    public CompiledMethodObject getMethod() {
        return outerMethod;
    }

    @Override
    public BaseSqueakObject shallowCopy() {
        return new CompiledBlockObject(this);
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }
}
