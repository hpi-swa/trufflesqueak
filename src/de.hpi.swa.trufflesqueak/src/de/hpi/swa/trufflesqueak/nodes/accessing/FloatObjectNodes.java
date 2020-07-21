/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.accessing;

import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.FloatObject;
import de.hpi.swa.trufflesqueak.nodes.accessing.FloatObjectNodesFactory.AsFloatObjectIfNessaryNodeGen;

@GenerateUncached
public final class FloatObjectNodes {
    @ImportStatic(Double.class)
    public abstract static class AsFloatObjectIfNessaryNode extends Node {

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
        protected static final FloatObject doNaNOrInfinite(final double value,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return new FloatObject(image, value);
        }
    }
}
