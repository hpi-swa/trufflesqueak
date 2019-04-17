package de.hpi.swa.graal.squeak.nodes.accessing;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ClassObject;

@GenerateUncached
public abstract class SqueakObjectClassNode extends Node {
    public abstract ClassObject executeClass(AbstractSqueakObject object);

    @Specialization(guards = "object == cachedObject", limit = "2", assumptions = "squeakClassStableAssumption")
    protected static final ClassObject doCached(@SuppressWarnings("unused") final AbstractSqueakObject object,
                    @SuppressWarnings("unused") @Cached("object") final AbstractSqueakObject cachedObject,
                    @SuppressWarnings("unused") @Cached(value = "object.getSqueakClassStableAssumption()", allowUncached = true) final Assumption squeakClassStableAssumption,
                    @Cached(value = "object.getSqueakClass()", allowUncached = true) final ClassObject classObject) {
        return classObject;
    }

    @Specialization
    protected static final ClassObject doUncached(final AbstractSqueakObject object) {
        return object.getSqueakClass();
    }
}
