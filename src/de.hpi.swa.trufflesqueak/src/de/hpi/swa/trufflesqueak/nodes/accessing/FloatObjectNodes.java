/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.accessing;

import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.FloatObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.FloatObjectNodesFactory.FloatObjectNormalizeNodeGen;

public final class FloatObjectNodes {

    @GenerateInline
    @GenerateUncached
    @GenerateCached(false)
    @ImportStatic(Double.class)
    public abstract static class FloatObjectNormalizeNode extends AbstractNode {

        public static FloatObjectNormalizeNode getUncached() {
            return FloatObjectNormalizeNodeGen.getUncached();
        }

        public abstract Object execute(Node node, double value);

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
