package de.hpi.swa.graal.squeak.nodes.accessing;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;

public abstract class SqueakObjectHashNode extends Node {
    public abstract long executeHash(AbstractSqueakObject object);

    @Specialization(guards = "object == cachedObject", limit = "2", assumptions = "squeakHashStableAssumption")
    protected static final long doCached(@SuppressWarnings("unused") final AbstractSqueakObject object,
                    @SuppressWarnings("unused") @Cached("object") final AbstractSqueakObject cachedObject,
                    @SuppressWarnings("unused") @Cached("object.getSqueakHashStableAssumption()") final Assumption squeakHashStableAssumption,
                    @Cached("object.getSqueakHash()") final long cachedHash) {
        return cachedHash;
    }

    @Specialization
    protected static final long doUncached(final AbstractSqueakObject object) {
        return object.getSqueakHash();
    }
}
