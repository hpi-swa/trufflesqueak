package de.hpi.swa.trufflesqueak.model;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.exceptions.UnwrappingError;
import de.hpi.swa.trufflesqueak.util.Chunk;

public abstract class ImmutableObject extends BaseSqueakObject {
    public ImmutableObject(SqueakImageContext image) {
        super(image);
    }

    @Override
    public void atput0(int idx, BaseSqueakObject obj) throws UnwrappingError {
        throw new UnwrappingError();
    }

    @Override
    public int instsize() {
        return size();
    }

    @Override
    public void fillin(Chunk chunk) {
        // nothing
    }
}
