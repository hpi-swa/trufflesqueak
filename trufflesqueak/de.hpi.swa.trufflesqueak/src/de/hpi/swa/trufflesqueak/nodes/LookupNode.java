package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;

@TypeSystemReference(SqueakTypes.class)
public abstract class LookupNode extends Node {
    protected static final int LOOKUP_CACHE_SIZE = 3;

    public abstract Object executeLookup(Object sqClass, Object selector);

    @SuppressWarnings("unused")
    @Specialization(limit = "LOOKUP_CACHE_SIZE", guards = {"sqClass == cachedSqClass",
                    "selector == cachedSelector"}, assumptions = {"methodLookupStable"})
    protected static BaseSqueakObject doDirect(ClassObject sqClass, BaseSqueakObject selector,
                    @Cached("sqClass") ClassObject cachedSqClass,
                    @Cached("selector") BaseSqueakObject cachedSelector,
                    @Cached("cachedSqClass.getMethodLookupStable()") Assumption methodLookupStable,
                    @Cached("cachedSqClass.lookup(cachedSelector)") BaseSqueakObject cachedMethod) {
        return cachedMethod;
    }

    @Specialization(replaces = "doDirect")
    protected static Object doIndirect(ClassObject sqClass, BaseSqueakObject selector) {
        return sqClass.lookup(selector);
    }

    @SuppressWarnings("unused")
    @Fallback
    protected static Object fail(Object sqClass, Object selector) {
        throw new RuntimeException("failed to lookup generic selector object on generic class");
    }
}
