package de.hpi.swa.trufflesqueak.model;

import de.hpi.swa.trufflesqueak.Chunk;
import de.hpi.swa.trufflesqueak.exceptions.InvalidIndex;

public abstract class BaseSqueakObject {
    @SuppressWarnings("unused")
    public void fillin(Chunk chunk) {
    }

    @Override
    public String toString() {
        return "a " + getSqClassName();
    }

    public abstract BaseSqueakObject getSqClass();

    public String getSqClassName() {
        String name = "unknown class name";
        BaseSqueakObject cls = getSqClass();
        if (cls instanceof PointersObject) {
            try {
                BaseSqueakObject nameObj = ((PointersObject) cls).at0(6);
                if (nameObj instanceof NativeObject) {
                    return nameObj.toString();
                }
            } catch (InvalidIndex e) {
                // fall through
            }
        }
        return name;
    }
}
