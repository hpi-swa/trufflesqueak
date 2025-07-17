/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import com.oracle.truffle.api.strings.MutableTruffleString;
import com.oracle.truffle.api.strings.TruffleString;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive0WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.MiscUtils;

public final class UUIDPlugin extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return UUIDPluginFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveMakeUUID")
    protected abstract static class PrimMakeUUIDNode extends AbstractPrimitiveNode implements Primitive0WithFallback {
        @Specialization(guards = {"receiver.isTruffleStringType()", "receiver.getTruffleStringByteLength() == 16"})
        protected static final Object doUUID(final NativeObject receiver, @Cached final MutableTruffleString.FromByteArrayNode fromByteArrayNode) {
            // TODO prevent allocation of a new byte array, use truffle string storage directly
            byte[] uuidBytes = new byte[16];
            TruffleString.Encoding encoding = receiver.getTruffleStringEncoding();
            ArrayUtils.fillRandomly(uuidBytes);
            // Version 4
            uuidBytes[6] = (byte) (uuidBytes[6] & 0x0F | 0x40); // Set version to 4
            // Fixed 8..b value
            uuidBytes[8] = (byte) (uuidBytes[8] & 0x3F | 0x80); // Set variant to 10xx
            // Write the bytes into the receiver's storage
            receiver.setTruffleStringStorage(fromByteArrayNode.execute(uuidBytes, 0, 16, encoding, false));
            return receiver;
        }
    }
}
