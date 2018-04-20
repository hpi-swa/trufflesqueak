package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.exceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;

public abstract class LookupNode extends Node {
    protected static final int LOOKUP_CACHE_SIZE = 3;

    public static LookupNode create() {
        return LookupNodeGen.create();
    }

    public abstract Object executeLookup(Object sqClass, Object selector);

    @SuppressWarnings("unused")
    @Specialization(limit = "LOOKUP_CACHE_SIZE", guards = {"sqClass == cachedSqClass",
                    "selector == cachedSelector"}, assumptions = {"methodLookupStable"})
    protected static Object doDirect(final ClassObject sqClass, final NativeObject selector,
                    @Cached("sqClass") final ClassObject cachedSqClass,
                    @Cached("selector") final NativeObject cachedSelector,
                    @Cached("cachedSqClass.getMethodLookupStable()") final Assumption methodLookupStable,
                    @Cached("cachedSqClass.lookup(cachedSelector)") final Object cachedMethod) {
        return cachedMethod;
    }

    @Specialization(replaces = "doDirect")
    protected static Object doIndirect(final ClassObject sqClass, final NativeObject selector) {
        return sqClass.lookup(selector);
    }

    @SuppressWarnings("unused")
    @Fallback
    protected static Object fail(final Object sqClass, final Object selector) {
        throw new SqueakException("failed to lookup generic selector object on generic class");
    }
}
