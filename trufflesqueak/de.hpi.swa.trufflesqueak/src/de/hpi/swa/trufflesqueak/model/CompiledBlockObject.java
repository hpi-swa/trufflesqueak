package de.hpi.swa.trufflesqueak.model;

import java.util.Arrays;

public class CompiledBlockObject extends CompiledCodeObject {
    final CompiledMethodObject outerMethod;
    final int numCopiedValues;

    public CompiledBlockObject(CompiledCodeObject method, int numArgs, int numCopied) {
        super(method.image);
        outerMethod = method.getMethod();
        numCopiedValues = numCopied;
        BaseSqueakObject[] lits = outerMethod.getLiterals();
        lits = Arrays.copyOf(lits, lits.length - 1);
        int baseHdr = ((numArgs & 0xF) << 24) | (((outerMethod.getNumTemps() + numCopied) & 0x3F) << 18);
        lits[0] = image.wrap(baseHdr); // replace header
        lits[lits.length - 1] = outerMethod; // last literal is back pointer to method
        literals = lits;
    }

    @Override
    public NativeObject getCompiledInSelector() {
        return outerMethod.getCompiledInSelector();
    }

    @Override
    public ClassObject getCompiledInClass() {
        return outerMethod.getCompiledInClass();
    }

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
}
