/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.accessing;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObjectWithClassAndHash;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;
import de.hpi.swa.graal.squeak.nodes.accessing.NativeObjectNodes.NativeGetBytesNode;

/** This node should only be used in primitive nodes as it may throw a PrimitiveFailed exception. */
public abstract class SqueakObjectChangeClassOfToNode extends AbstractNode {

    public abstract AbstractSqueakObject execute(AbstractSqueakObjectWithClassAndHash receiver, ClassObject argument);

    @Specialization(guards = "receiver.hasSameFormat(argument)")
    protected static final NativeObject doNative(final NativeObject receiver, final ClassObject argument) {
        receiver.setSqueakClass(argument);
        return receiver;
    }

    @Specialization(guards = {"!receiver.hasSameFormat(argument)", "argument.isBytes()"})
    protected static final NativeObject doNativeConvertToBytes(final NativeObject receiver, final ClassObject argument,
                    @Shared("getBytesNode") @Cached final NativeGetBytesNode getBytesNode) {
        receiver.setSqueakClass(argument);
        receiver.convertToBytesStorage(getBytesNode.execute(receiver));
        return receiver;
    }

    @Specialization(guards = {"!receiver.hasSameFormat(argument)", "argument.isShorts()"})
    protected static final NativeObject doNativeConvertToShorts(final NativeObject receiver, final ClassObject argument,
                    @Shared("getBytesNode") @Cached final NativeGetBytesNode getBytesNode) {
        receiver.setSqueakClass(argument);
        receiver.convertToShortsStorage(getBytesNode.execute(receiver));
        return receiver;
    }

    @Specialization(guards = {"!receiver.hasSameFormat(argument)", "argument.isWords()"})
    protected static final NativeObject doNativeConvertToInts(final NativeObject receiver, final ClassObject argument,
                    @Shared("getBytesNode") @Cached final NativeGetBytesNode getBytesNode) {
        receiver.setSqueakClass(argument);
        receiver.convertToIntsStorage(getBytesNode.execute(receiver));
        return receiver;
    }

    @Specialization(guards = {"!receiver.hasSameFormat(argument)", "argument.isLongs()"})
    protected static final NativeObject doNativeConvertToLongs(final NativeObject receiver, final ClassObject argument,
                    @Shared("getBytesNode") @Cached final NativeGetBytesNode getBytesNode) {
        receiver.setSqueakClass(argument);
        receiver.convertToLongsStorage(getBytesNode.execute(receiver));
        return receiver;
    }

    @Specialization(guards = {"argument.isBytes()"})
    protected static final LargeIntegerObject doLargeInteger(final LargeIntegerObject receiver, final ClassObject argument) {
        receiver.setSqueakClass(argument);
        return receiver;
    }

    @Specialization(guards = {"!isNativeObject(receiver)", "!isLargeIntegerObject(receiver)", "!isFloatObject(receiver)",
                    "receiver.getSqueakClass().getFormat() == argument.getFormat()"})
    protected static final AbstractSqueakObject doSqueakObject(final AbstractSqueakObjectWithClassAndHash receiver, final ClassObject argument) {
        receiver.setSqueakClass(argument);
        return receiver;
    }

    @SuppressWarnings("unused")
    @Fallback
    protected static final AbstractSqueakObject doFail(final AbstractSqueakObjectWithClassAndHash receiver, final ClassObject argument) {
        throw PrimitiveFailed.GENERIC_ERROR;
    }
}
