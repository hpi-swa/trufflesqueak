/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.plugins;

import java.time.ZonedDateTime;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NotProvided;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectSizeNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectWriteNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;

public final class NullPlugin extends AbstractPrimitiveFactoryHolder {
    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveUtcWithOffset")
    protected abstract static class PrimUtcWithOffsetNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimUtcWithOffsetNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @SuppressWarnings("unused")
        protected final ArrayObject doUTC(final Object receiver, final NotProvided np) {
            return method.image.asArrayOfLongs(getUTCMicroseconds(), getOffsetFromGTMInSeconds());
        }

        @Specialization(guards = "objectWithTwoSlots.size() == 2")
        protected static final PointersObject doUTC(@SuppressWarnings("unused") final Object receiver, final PointersObject objectWithTwoSlots) {
            objectWithTwoSlots.atput0(0, getUTCMicroseconds());
            objectWithTwoSlots.atput0(1, getOffsetFromGTMInSeconds());
            return objectWithTwoSlots;
        }

        @Specialization(guards = "sizeNode.execute(arrayWithTwoSlots) == 2", limit = "1")
        protected static final ArrayObject doUTC(@SuppressWarnings("unused") final Object receiver, final ArrayObject arrayWithTwoSlots,
                        @SuppressWarnings("unused") @Cached final ArrayObjectSizeNode sizeNode,
                        @Cached final ArrayObjectWriteNode writeNode) {
            writeNode.execute(arrayWithTwoSlots, 0, getUTCMicroseconds());
            writeNode.execute(arrayWithTwoSlots, 1, getOffsetFromGTMInSeconds());
            return arrayWithTwoSlots;
        }

        @TruffleBoundary
        private static long getUTCMicroseconds() {
            return System.currentTimeMillis() * 1000L;
        }

        @TruffleBoundary
        private static long getOffsetFromGTMInSeconds() {
            return ZonedDateTime.now().getOffset().getTotalSeconds();
        }
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return NullPluginFactory.getFactories();
    }
}
