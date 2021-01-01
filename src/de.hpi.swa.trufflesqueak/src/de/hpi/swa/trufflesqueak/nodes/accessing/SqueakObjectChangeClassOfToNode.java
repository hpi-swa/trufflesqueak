/*
 * Copyright (c) 2017-2021 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.accessing;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.AbstractPointersObject;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObjectWithClassAndHash;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.EmptyObject;
import de.hpi.swa.trufflesqueak.model.LargeIntegerObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.NativeObjectNodes.NativeGetBytesNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.NativeObjectNodes.NativeGetIntsNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.NativeObjectNodes.NativeGetLongsNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.NativeObjectNodes.NativeGetShortsNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.NativeObjectNodes.NativeObjectByteSizeNode;

/** This node should only be used in primitive nodes as it may throw a PrimitiveFailed exception. */
public abstract class SqueakObjectChangeClassOfToNode extends AbstractNode {

    public abstract void execute(AbstractSqueakObjectWithClassAndHash receiver, ClassObject argument);

    @Specialization(guards = "receiver.hasFormatOf(argument)")
    protected static final void doNative(final NativeObject receiver, final ClassObject argument) {
        receiver.setSqueakClass(argument);
    }

    @Specialization(guards = {"!receiver.hasFormatOf(argument)", "argument.isBytes()"})
    protected static final void doNativeConvertToBytes(final NativeObject receiver, final ClassObject argument,
                    @Cached final NativeGetBytesNode getBytesNode) {
        receiver.setSqueakClass(argument);
        receiver.convertToBytesStorage(getBytesNode.execute(receiver));
    }

    @Specialization(guards = {"!receiver.hasFormatOf(argument)", "argument.isShorts()", "isIntegralWhenDividedBy(byteSize.execute(receiver), 2)"}, limit = "1")
    protected static final void doNativeConvertToShorts(final NativeObject receiver, final ClassObject argument,
                    @SuppressWarnings("unused") @Cached final NativeObjectByteSizeNode byteSize,
                    @Cached final NativeGetShortsNode getShortsNode) {
        receiver.setSqueakClass(argument);
        receiver.convertToShortsStorage(getShortsNode.execute(receiver));
    }

    @Specialization(guards = {"!receiver.hasFormatOf(argument)", "argument.isWords()", "isIntegralWhenDividedBy(byteSize.execute(receiver), 4)"}, limit = "1")
    protected static final void doNativeConvertToInts(final NativeObject receiver, final ClassObject argument,
                    @SuppressWarnings("unused") @Cached final NativeObjectByteSizeNode byteSize,
                    @Cached final NativeGetIntsNode getIntsNode) {
        receiver.setSqueakClass(argument);
        receiver.convertToIntsStorage(getIntsNode.execute(receiver));
    }

    @Specialization(guards = {"!receiver.hasFormatOf(argument)", "argument.isLongs()", "isIntegralWhenDividedBy(byteSize.execute(receiver), 8)"}, limit = "1")
    protected static final void doNativeConvertToLongs(final NativeObject receiver, final ClassObject argument,
                    @SuppressWarnings("unused") @Cached final NativeObjectByteSizeNode byteSize,
                    @Cached final NativeGetLongsNode getLongsNode) {
        receiver.setSqueakClass(argument);
        receiver.convertToLongsStorage(getLongsNode.execute(receiver));
    }

    @Specialization(guards = {"argument.isBytes()"})
    protected static final void doLargeInteger(final LargeIntegerObject receiver, final ClassObject argument) {
        receiver.setSqueakClass(argument);
    }

    @Specialization(guards = {"receiver.hasFormatOf(argument)"})
    protected static final void doPointers(final AbstractPointersObject receiver, final ClassObject argument) {
        receiver.changeClassTo(argument);
    }

    @Specialization(guards = {"receiver.hasFormatOf(argument)"})
    protected static final void doArray(final ArrayObject receiver, final ClassObject argument) {
        receiver.setSqueakClass(argument);
    }

    @Specialization(guards = {"receiver.hasFormatOf(argument)"})
    protected static final void doClass(final ClassObject receiver, final ClassObject argument) {
        receiver.setSqueakClass(argument);
    }

    @Specialization(guards = {"receiver.hasFormatOf(argument)"})
    protected static final void doCode(final CompiledCodeObject receiver, final ClassObject argument) {
        receiver.setSqueakClass(argument);
    }

    @Specialization(guards = {"receiver.hasFormatOf(argument)"})
    protected static final void doEmpty(final EmptyObject receiver, final ClassObject argument) {
        receiver.setSqueakClass(argument);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "!receiver.hasFormatOf(argument)")
    protected static final void doFail(final AbstractSqueakObjectWithClassAndHash receiver, final ClassObject argument) {
        throw PrimitiveFailed.GENERIC_ERROR;
    }
}
