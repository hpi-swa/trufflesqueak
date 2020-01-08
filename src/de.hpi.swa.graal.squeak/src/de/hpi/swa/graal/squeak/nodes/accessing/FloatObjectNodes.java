/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.accessing;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithImage;
import de.hpi.swa.graal.squeak.nodes.accessing.FloatObjectNodesFactory.AsFloatObjectIfNessaryNodeGen;

public final class FloatObjectNodes {
    @ImportStatic(Double.class)
    public abstract static class AsFloatObjectIfNessaryNode extends AbstractNodeWithImage {

        protected AsFloatObjectIfNessaryNode(final SqueakImageContext image) {
            super(image);
        }

        public static AsFloatObjectIfNessaryNode create(final SqueakImageContext image) {
            return AsFloatObjectIfNessaryNodeGen.create(image);
        }

        public abstract Object execute(double value);

        @Specialization(guards = "isFinite(value)")
        protected static final double doFinite(final double value) {
            return value;
        }

        @Fallback
        protected final FloatObject doNaNOrInfinite(final double value) {
            return new FloatObject(image, value);
        }
    }
}
