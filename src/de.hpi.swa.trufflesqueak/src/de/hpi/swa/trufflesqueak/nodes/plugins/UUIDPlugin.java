/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive0WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;

public final class UUIDPlugin extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return UUIDPluginFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveMakeUUID")
    protected abstract static class PrimMakeUUIDNode extends AbstractPrimitiveNode implements Primitive0WithFallback {
        @Specialization(guards = {"receiver.isByteType()", "receiver.getByteLength() == 16"})
        protected static final Object doUUID(final NativeObject receiver) {
            ArrayUtils.fillRandomly(receiver.getByteStorage());
            // Version 4
            receiver.setByte(6, receiver.getByte(6) & 0x0F | 0x40);
            // Fixed 8..b value
            receiver.setByte(8, receiver.getByte(8) & 0x3F | 0x80);
            return receiver;
        }
    }
}
