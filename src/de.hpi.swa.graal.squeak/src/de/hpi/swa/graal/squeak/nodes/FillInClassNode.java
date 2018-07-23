package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.image.SqueakImageChunk;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;

public abstract class FillInClassNode extends Node {

    public static FillInClassNode create() {
        return FillInClassNodeGen.create();
    }

    public abstract void execute(Object obj, SqueakImageChunk chunk);

    @Specialization(guards = {"obj.getSqClass() == null", "obj.squeakHash() >= 0"})
    protected static final void doSqueakObject(final AbstractSqueakObject obj, final SqueakImageChunk chunk) {
        obj.setSqClass(chunk.getSqClass());
    }

    @Specialization(guards = {"obj.getSqClass() == null", "obj.squeakHash() < 0"})
    protected static final void doSqueakObjectClassAndHash(final AbstractSqueakObject obj, final SqueakImageChunk chunk) {
        obj.setSqClass(chunk.getSqClass());
        obj.setSqueakHash(chunk.getHash());
    }

    @SuppressWarnings("unused")
    @Fallback
    protected static final void doNothing(final Object obj, final SqueakImageChunk chunk) {
        // do nothing
    }
}
