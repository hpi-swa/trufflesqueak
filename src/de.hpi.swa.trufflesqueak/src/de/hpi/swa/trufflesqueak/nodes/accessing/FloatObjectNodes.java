/*
 * Copyright (c) 2017-2023 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2023 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.accessing;

import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.FloatObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.FloatObjectNodesFactory.AsFloatObjectIfNessaryNodeGen;

public final class FloatObjectNodes {
    @GenerateUncached
    @ImportStatic(Double.class)
    public abstract static class AsFloatObjectIfNessaryNode extends AbstractNode {

        @NeverDefault
        public static AsFloatObjectIfNessaryNode create() {
            return AsFloatObjectIfNessaryNodeGen.create();
        }

        public static AsFloatObjectIfNessaryNode getUncached() {
            return AsFloatObjectIfNessaryNodeGen.getUncached();
        }

        public abstract Object execute(double value);

        @Specialization(guards = "isFinite(value)")
        protected static final double doFinite(final double value) {
            return value;
        }

        @Specialization(guards = "!isFinite(value)")
        protected final FloatObject doNaNOrInfinite(final double value) {
            return new FloatObject(getContext(), value);
        }
    }
}
