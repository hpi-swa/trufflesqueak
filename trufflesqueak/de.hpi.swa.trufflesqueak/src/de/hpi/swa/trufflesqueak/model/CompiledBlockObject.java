package de.hpi.swa.trufflesqueak.model;

import java.util.Arrays;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;

public class CompiledBlockObject extends CompiledCodeObject {
    final CompiledMethodObject outerMethod;
    final int numCopiedValues;

    public CompiledBlockObject(CompiledCodeObject method, int numArgs, int numCopied) {
        super(method.image);
        outerMethod = method.getMethod();
        numCopiedValues = numCopied;
        Object[] lits = outerMethod.getLiterals();
        lits = Arrays.copyOf(lits, lits.length - 1);
        int baseHdr = ((numArgs & 0xF) << 24) | (((outerMethod.getNumTemps() + numCopied) & 0x3F) << 18);
        lits[0] = baseHdr; // replace header
        lits[lits.length - 1] = outerMethod; // last literal is back pointer to method
        literals = lits;
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

    /**
     * We override this, because the old inline compiled blocks in Squeak 5 and 6 make it very hard
     * to determine the correct number of temps that will be used. This way, for blocks we allow for
     * more temps on demand (but at least the required frame size has to be correct)
     */
    @Override
    public FrameSlot getStackSlot(int i) {
        FrameSlot slot = stackSlots[i];
        if (slot == null) {
            slot = stackSlots[i] = getFrameDescriptor().addFrameSlot(i, FrameSlotKind.Illegal);
        }
        return slot;
    }

    @Override
    public BaseSqueakObject shallowCopy() {
        return new CompiledBlockObject(this);
    }
}
