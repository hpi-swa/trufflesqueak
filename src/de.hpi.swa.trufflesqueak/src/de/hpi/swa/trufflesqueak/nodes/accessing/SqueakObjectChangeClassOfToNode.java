/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.accessing;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.AbstractPointersObject;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObjectWithClassAndHash;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.EmptyObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.NativeObjectNodes.NativeGetBytesNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.NativeObjectNodes.NativeGetIntsNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.NativeObjectNodes.NativeGetLongsNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.NativeObjectNodes.NativeGetShortsNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.NativeObjectNodes.NativeObjectByteSizeNode;

/** This node should only be used in primitive nodes as it may throw a PrimitiveFailed exception. */
@GenerateInline
@GenerateCached(false)
public abstract class SqueakObjectChangeClassOfToNode extends AbstractNode {

    public abstract void execute(Node node, AbstractSqueakObjectWithClassAndHash receiver, ClassObject argument);

    @Specialization(guards = "receiver.hasFormatOf(argument)")
    protected static final void doNative(final NativeObject receiver, final ClassObject argument) {
        receiver.setSqueakClass(argument);
    }

    @Specialization(guards = {"!receiver.hasFormatOf(argument)", "argument.isBytes()"})
    protected static final void doNativeConvertToBytes(final Node node, final NativeObject receiver, final ClassObject argument,
                    @Cached final NativeGetBytesNode getBytesNode) {
        receiver.setSqueakClass(argument);
        receiver.convertToBytesStorage(getBytesNode.execute(node, receiver));
    }

    @Specialization(guards = {"!receiver.hasFormatOf(argument)", "argument.isShorts()", "isIntegralWhenDividedBy(byteSize.execute(node, receiver), 2)"}, limit = "1")
    protected static final void doNativeConvertToShorts(final Node node, final NativeObject receiver, final ClassObject argument,
                    @SuppressWarnings("unused") @Exclusive @Cached final NativeObjectByteSizeNode byteSize,
                    @Cached final NativeGetShortsNode getShortsNode) {
        receiver.setSqueakClass(argument);
        receiver.convertToShortsStorage(getShortsNode.execute(node, receiver));
    }

    @Specialization(guards = {"!receiver.hasFormatOf(argument)", "argument.isWords()", "isIntegralWhenDividedBy(byteSize.execute(node, receiver), 4)"}, limit = "1")
    protected static final void doNativeConvertToInts(final Node node, final NativeObject receiver, final ClassObject argument,
                    @SuppressWarnings("unused") @Exclusive @Cached final NativeObjectByteSizeNode byteSize,
                    @Cached final NativeGetIntsNode getIntsNode) {
        receiver.setSqueakClass(argument);
        receiver.convertToIntsStorage(getIntsNode.execute(node, receiver));
    }

    @Specialization(guards = {"!receiver.hasFormatOf(argument)", "argument.isLongs()", "isIntegralWhenDividedBy(byteSize.execute(node, receiver), 8)"}, limit = "1")
    protected static final void doNativeConvertToLongs(final Node node, final NativeObject receiver, final ClassObject argument,
                    @SuppressWarnings("unused") @Exclusive @Cached final NativeObjectByteSizeNode byteSize,
                    @Cached final NativeGetLongsNode getLongsNode) {
        receiver.setSqueakClass(argument);
        receiver.convertToLongsStorage(getLongsNode.execute(node, receiver));
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
