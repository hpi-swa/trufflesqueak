package de.hpi.swa.trufflesqueak.model;

import java.util.Arrays;

public class CompiledBlockObject extends CompiledCodeObject {
    final CompiledCodeObject outerMethod;

    public CompiledBlockObject(CompiledCodeObject method, int numArgs, int numCopied) {
        super(method.image);
        outerMethod = method;
        BaseSqueakObject[] lits = outerMethod.getLiterals();
        lits = Arrays.copyOf(lits, lits.length - 1);
        int baseHdr = ((numArgs & 0xF) << 24) | ((numCopied & 0x3F) << 18);
        lits[0] = image.wrapInt(baseHdr); // replace header
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
        return getNumTemps();
    }
}
