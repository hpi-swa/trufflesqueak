package de.hpi.swa.graal.squeak.nodes.accessing;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.model.FloatObject;

public final class FloatObjectNodes {
    @GenerateUncached
    @ImportStatic(Double.class)
    public abstract static class AsFloatObjectIfNessaryNode extends Node {

        public abstract Object execute(double value);

        @Specialization(guards = "isFinite(value)")
        protected static final double doFinite(final double value) {
            return value;
        }

        @Fallback
        protected static final FloatObject doNaNOrInfinite(final double value) {
            return new FloatObject(value);
        }
    }
}
