/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.accessing;

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

/** This node should only be used in primitive nodes as it may throw a PrimitiveFailed exception. */
public abstract class SqueakObjectChangeClassOfToNode extends AbstractNode {

    public abstract void execute(AbstractSqueakObjectWithClassAndHash receiver, ClassObject argument);

    @Specialization(guards = "receiver.hasFormatOf(argument)")
    protected static final void doNative(final NativeObject receiver, final ClassObject argument) {
        receiver.setSqueakClass(argument);
    }

    @Specialization(guards = {"!receiver.hasFormatOf(argument)", "argument.isBytes()"})
    protected static final void doNativeConvertToBytes(final NativeObject receiver, final ClassObject argument) {
        receiver.setSqueakClass(argument);
    }

    @Specialization(guards = {"!receiver.hasFormatOf(argument)", "argument.isShorts()", "isIntegralWhenDividedBy(receiver.getByteSize(), 2)"})
    protected static final void doNativeConvertToShorts(final NativeObject receiver, final ClassObject argument) {
        receiver.setSqueakClass(argument);
    }

    @Specialization(guards = {"!receiver.hasFormatOf(argument)", "argument.isWords()", "isIntegralWhenDividedBy(receiver.getByteSize(), 4)"})
    protected static final void doNativeConvertToInts(final NativeObject receiver, final ClassObject argument) {
        receiver.setSqueakClass(argument);
    }

    @Specialization(guards = {"!receiver.hasFormatOf(argument)", "argument.isLongs()", "isIntegralWhenDividedBy(receiver.getByteSize(), 8)"})
    protected static final void doNativeConvertToLongs(final NativeObject receiver, final ClassObject argument) {
        receiver.setSqueakClass(argument);
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
