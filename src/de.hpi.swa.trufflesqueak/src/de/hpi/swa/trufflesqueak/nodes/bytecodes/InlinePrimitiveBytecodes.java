/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import java.util.Arrays;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObjectWithClassAndHash;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectAtPut0Node;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.InlinePrimitiveBytecodesFactory.PrimClassNodeGen;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.InlinePrimitiveBytecodesFactory.PrimFillFromToWithNodeGen;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.InlinePrimitiveBytecodesFactory.PrimIdentityHashNodeGen;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackWriteNode;

public final class InlinePrimitiveBytecodes {
    protected abstract static class AbstractPushNode extends AbstractInstrumentableBytecodeNode {
        @Child protected FrameStackWriteNode pushNode;

        protected AbstractPushNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(successorIndex, sp);
            pushNode = FrameStackWriteNode.create(frame, sp - 1);
        }
    }

    protected abstract static class AbstractNullaryInlinePrimitiveNode extends AbstractPushNode {
        @Child protected FrameStackReadNode popNode;

        protected AbstractNullaryInlinePrimitiveNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(frame, successorIndex, sp);
            popNode = FrameStackReadNode.create(frame, sp, true);
        }
    }

    protected abstract static class AbstractUnaryInlinePrimitiveNode extends AbstractPushNode {
        @Child protected FrameStackReadNode pop1Node;
        @Child protected FrameStackReadNode pop2Node;

        protected AbstractUnaryInlinePrimitiveNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(frame, successorIndex, sp);
            pop1Node = FrameStackReadNode.create(frame, sp - 1, true);
            pop2Node = FrameStackReadNode.create(frame, sp, true);
        }
    }

    protected abstract static class AbstractTrinaryInlinePrimitiveNode extends AbstractPushNode {
        @Child protected FrameStackReadNode pop1Node;
        @Child protected FrameStackReadNode pop2Node;
        @Child protected FrameStackReadNode pop3Node;

        protected AbstractTrinaryInlinePrimitiveNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(frame, successorIndex, sp);
            pop1Node = FrameStackReadNode.create(frame, sp - 2, true);
            pop2Node = FrameStackReadNode.create(frame, sp - 1, true);
            pop3Node = FrameStackReadNode.create(frame, sp, true);
        }
    }

    protected abstract static class AbstractQuaternaryInlinePrimitiveNode extends AbstractPushNode {
        @Child protected FrameStackReadNode pop1Node;
        @Child protected FrameStackReadNode pop2Node;
        @Child protected FrameStackReadNode pop3Node;
        @Child protected FrameStackReadNode pop4Node;

        protected AbstractQuaternaryInlinePrimitiveNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(frame, successorIndex, sp);
            pop1Node = FrameStackReadNode.create(frame, sp - 3, true);
            pop2Node = FrameStackReadNode.create(frame, sp - 2, true);
            pop3Node = FrameStackReadNode.create(frame, sp - 1, true);
            pop4Node = FrameStackReadNode.create(frame, sp, true);
        }
    }

    protected abstract static class PrimClassNode extends AbstractNullaryInlinePrimitiveNode {

        protected PrimClassNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(frame, successorIndex, sp);
        }

        public static PrimClassNode create(final VirtualFrame frame, final int successorIndex, final int sp) {
            return PrimClassNodeGen.create(frame, successorIndex, sp);
        }

        @Specialization
        protected final void doClass(final VirtualFrame frame,
                        @Bind final Node node,
                        @Cached final SqueakObjectClassNode classNode) {
            pushNode.executeWrite(frame, classNode.executeLookup(node, popNode.executeRead(frame)));
        }
    }

    protected static final class PrimNumSlotsNode extends AbstractNullaryInlinePrimitiveNode {
        protected PrimNumSlotsNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(frame, successorIndex, sp);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            throw SqueakException.create("Not yet implemented"); // TODO
        }
    }

    protected static final class PrimBasicSizeNode extends AbstractNullaryInlinePrimitiveNode {
        protected PrimBasicSizeNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(frame, successorIndex, sp);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            throw SqueakException.create("Not yet implemented"); // TODO
        }
    }

    protected static final class PrimNumBytesNode extends AbstractNullaryInlinePrimitiveNode {
        protected PrimNumBytesNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(frame, successorIndex, sp);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            final Object receiver = popNode.executeRead(frame);
            final long numBytes;
            if (receiver instanceof final CompiledCodeObject o) {
                numBytes = o.getBytes().length;
            } else {
                numBytes = ((NativeObject) receiver).getByteLength();
            }
            pushNode.executeWrite(frame, numBytes);
        }
    }

    protected static final class PrimNumShortsNode extends AbstractNullaryInlinePrimitiveNode {
        protected PrimNumShortsNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(frame, successorIndex, sp);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.executeWrite(frame, (long) ((NativeObject) popNode.executeRead(frame)).getShortLength());
        }
    }

    protected static final class PrimNumWordsNode extends AbstractNullaryInlinePrimitiveNode {
        protected PrimNumWordsNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(frame, successorIndex, sp);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.executeWrite(frame, (long) ((NativeObject) popNode.executeRead(frame)).getIntLength());
        }
    }

    protected static final class PrimNumDoubleWordsNode extends AbstractNullaryInlinePrimitiveNode {
        protected PrimNumDoubleWordsNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(frame, successorIndex, sp);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.executeWrite(frame, (long) ((NativeObject) popNode.executeRead(frame)).getLongLength());
        }
    }

    protected abstract static class PrimIdentityHashNode extends AbstractNullaryInlinePrimitiveNode {

        protected PrimIdentityHashNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(frame, successorIndex, sp);
        }

        public static PrimIdentityHashNode create(final VirtualFrame frame, final int successorIndex, final int sp) {
            return PrimIdentityHashNodeGen.create(frame, successorIndex, sp);
        }

        @Specialization
        protected final void doIdentityHash(final VirtualFrame frame,
                        @Cached final InlinedBranchProfile needsHashProfile) {
            pushNode.executeWrite(frame, ((AbstractSqueakObjectWithClassAndHash) popNode.executeRead(frame)).getOrCreateSqueakHash(needsHashProfile, this));
        }
    }

    protected static final class PrimIdentityHashSmallIntegerNode extends AbstractNullaryInlinePrimitiveNode {
        protected PrimIdentityHashSmallIntegerNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(frame, successorIndex, sp);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.executeWrite(frame, popNode.executeRead(frame));
        }
    }

    protected static final class PrimIdentityHashCharacterNode extends AbstractNullaryInlinePrimitiveNode {
        protected PrimIdentityHashCharacterNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(frame, successorIndex, sp);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.executeWrite(frame, (long) (char) popNode.executeRead(frame));
        }
    }

    protected static final class PrimIdentityHashSmallFloatNode extends AbstractNullaryInlinePrimitiveNode {
        protected PrimIdentityHashSmallFloatNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(frame, successorIndex, sp);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.executeWrite(frame, Double.doubleToRawLongBits((double) popNode.executeRead(frame)));
        }
    }

    protected static final class PrimIdentityHashBehaviorNode extends AbstractNullaryInlinePrimitiveNode {
        protected PrimIdentityHashBehaviorNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(frame, successorIndex, sp);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            final ClassObject classObject = (ClassObject) popNode.executeRead(frame);
            classObject.ensureBehaviorHash();
            pushNode.executeWrite(frame, classObject.getSqueakHash());
        }
    }

    protected static final class PrimImmediateAsIntegerCharacterNode extends AbstractNullaryInlinePrimitiveNode {
        protected PrimImmediateAsIntegerCharacterNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(frame, successorIndex, sp);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.executeWrite(frame, (long) (char) popNode.executeRead(frame));
        }
    }

    protected static final class PrimImmediateAsIntegerSmallFloatNode extends AbstractNullaryInlinePrimitiveNode {
        protected PrimImmediateAsIntegerSmallFloatNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(frame, successorIndex, sp);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.executeWrite(frame, Double.doubleToRawLongBits((double) popNode.executeRead(frame)));
        }
    }

    protected static final class PrimImmediateAsFloatNode extends AbstractNullaryInlinePrimitiveNode {
        protected PrimImmediateAsFloatNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(frame, successorIndex, sp);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.executeWrite(frame, (double) (long) popNode.executeRead(frame));
        }
    }

    protected static final class PrimSmallIntegerAddNode extends AbstractUnaryInlinePrimitiveNode {
        protected PrimSmallIntegerAddNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(frame, successorIndex, sp);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.executeWrite(frame, (long) pop1Node.executeRead(frame) + (long) pop2Node.executeRead(frame));
        }
    }

    protected static final class PrimSmallIntegerSubtractNode extends AbstractUnaryInlinePrimitiveNode {
        protected PrimSmallIntegerSubtractNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(frame, successorIndex, sp);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.executeWrite(frame, (long) pop1Node.executeRead(frame) - (long) pop2Node.executeRead(frame));
        }
    }

    protected static final class PrimSmallIntegerMultiplyNode extends AbstractUnaryInlinePrimitiveNode {
        protected PrimSmallIntegerMultiplyNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(frame, successorIndex, sp);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.executeWrite(frame, (long) pop1Node.executeRead(frame) * (long) pop2Node.executeRead(frame));
        }
    }

    protected static final class PrimSmallIntegerDivideNode extends AbstractUnaryInlinePrimitiveNode {
        protected PrimSmallIntegerDivideNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(frame, successorIndex, sp);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.executeWrite(frame, (long) pop1Node.executeRead(frame) / (long) pop2Node.executeRead(frame));
        }
    }

    protected static final class PrimSmallIntegerFloorDivideNode extends AbstractUnaryInlinePrimitiveNode {
        protected PrimSmallIntegerFloorDivideNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(frame, successorIndex, sp);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.executeWrite(frame, Math.floorDiv((long) pop1Node.executeRead(frame), (long) pop2Node.executeRead(frame)));
        }
    }

    protected static final class PrimSmallIntegerFloorModNode extends AbstractUnaryInlinePrimitiveNode {
        protected PrimSmallIntegerFloorModNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(frame, successorIndex, sp);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.executeWrite(frame, Math.floorMod((long) pop1Node.executeRead(frame), (long) pop2Node.executeRead(frame)));
        }
    }

    protected static final class PrimSmallIntegerQuoNode extends AbstractUnaryInlinePrimitiveNode {
        protected PrimSmallIntegerQuoNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(frame, successorIndex, sp);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.executeWrite(frame, (long) pop1Node.executeRead(frame) / (long) pop2Node.executeRead(frame));
        }
    }

    protected static final class PrimSmallIntegerBitAndNode extends AbstractUnaryInlinePrimitiveNode {
        protected PrimSmallIntegerBitAndNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(frame, successorIndex, sp);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.executeWrite(frame, (long) pop1Node.executeRead(frame) & (long) pop2Node.executeRead(frame));
        }
    }

    protected static final class PrimSmallIntegerBitOrNode extends AbstractUnaryInlinePrimitiveNode {
        protected PrimSmallIntegerBitOrNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(frame, successorIndex, sp);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.executeWrite(frame, (long) pop1Node.executeRead(frame) | (long) pop2Node.executeRead(frame));
        }
    }

    protected static final class PrimSmallIntegerBitXorNode extends AbstractUnaryInlinePrimitiveNode {
        protected PrimSmallIntegerBitXorNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(frame, successorIndex, sp);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.executeWrite(frame, (long) pop1Node.executeRead(frame) ^ (long) pop2Node.executeRead(frame));
        }
    }

    protected static final class PrimSmallIntegerBitShiftLeftNode extends AbstractUnaryInlinePrimitiveNode {
        protected PrimSmallIntegerBitShiftLeftNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(frame, successorIndex, sp);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.executeWrite(frame, (long) pop1Node.executeRead(frame) << (long) pop2Node.executeRead(frame));
        }
    }

    protected static final class PrimSmallIntegerBitShiftRightNode extends AbstractUnaryInlinePrimitiveNode {
        protected PrimSmallIntegerBitShiftRightNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(frame, successorIndex, sp);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.executeWrite(frame, (long) pop1Node.executeRead(frame) >> (long) pop2Node.executeRead(frame));
        }
    }

    protected static final class PrimSmallIntegerGreaterThanNode extends AbstractUnaryInlinePrimitiveNode {
        protected PrimSmallIntegerGreaterThanNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(frame, successorIndex, sp);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.executeWrite(frame, BooleanObject.wrap((long) pop1Node.executeRead(frame) > (long) pop2Node.executeRead(frame)));
        }
    }

    protected static final class PrimSmallIntegerLessThanNode extends AbstractUnaryInlinePrimitiveNode {
        protected PrimSmallIntegerLessThanNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(frame, successorIndex, sp);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.executeWrite(frame, BooleanObject.wrap((long) pop1Node.executeRead(frame) < (long) pop2Node.executeRead(frame)));
        }
    }

    protected static final class PrimSmallIntegerGreaterOrEqualNode extends AbstractUnaryInlinePrimitiveNode {
        protected PrimSmallIntegerGreaterOrEqualNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(frame, successorIndex, sp);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.executeWrite(frame, BooleanObject.wrap((long) pop1Node.executeRead(frame) >= (long) pop2Node.executeRead(frame)));
        }
    }

    protected static final class PrimSmallIntegerLessOrEqualNode extends AbstractUnaryInlinePrimitiveNode {
        protected PrimSmallIntegerLessOrEqualNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(frame, successorIndex, sp);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.executeWrite(frame, BooleanObject.wrap((long) pop1Node.executeRead(frame) <= (long) pop2Node.executeRead(frame)));
        }
    }

    protected static final class PrimSmallIntegerEqualNode extends AbstractUnaryInlinePrimitiveNode {
        protected PrimSmallIntegerEqualNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(frame, successorIndex, sp);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.executeWrite(frame, BooleanObject.wrap((long) pop1Node.executeRead(frame) == (long) pop2Node.executeRead(frame)));
        }
    }

    protected static final class PrimSmallIntegerNotEqualNode extends AbstractUnaryInlinePrimitiveNode {
        protected PrimSmallIntegerNotEqualNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(frame, successorIndex, sp);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            pushNode.executeWrite(frame, BooleanObject.wrap((long) pop1Node.executeRead(frame) != (long) pop2Node.executeRead(frame)));
        }
    }

    protected static final class PrimByteAtNode extends AbstractUnaryInlinePrimitiveNode {
        protected PrimByteAtNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(frame, successorIndex, sp);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            final long atIndex = (long) pop2Node.executeRead(frame);
            final NativeObject receiver = (NativeObject) pop1Node.executeRead(frame);
            pushNode.executeWrite(frame, (long) receiver.getByte(atIndex));
        }
    }

    protected static final class PrimShortAtNode extends AbstractUnaryInlinePrimitiveNode {
        protected PrimShortAtNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(frame, successorIndex, sp);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            final long atIndex = (long) pop2Node.executeRead(frame);
            final NativeObject receiver = (NativeObject) pop1Node.executeRead(frame);
            pushNode.executeWrite(frame, (long) receiver.getShort(atIndex));
        }
    }

    protected static final class PrimWordAtNode extends AbstractUnaryInlinePrimitiveNode {
        protected PrimWordAtNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(frame, successorIndex, sp);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            final long atIndex = (long) pop2Node.executeRead(frame);
            final NativeObject receiver = (NativeObject) pop1Node.executeRead(frame);
            pushNode.executeWrite(frame, (long) receiver.getInt(atIndex));
        }
    }

    protected static final class PrimDoubleWordAtNode extends AbstractUnaryInlinePrimitiveNode {
        protected PrimDoubleWordAtNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(frame, successorIndex, sp);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            final long atIndex = (long) pop2Node.executeRead(frame);
            final NativeObject receiver = (NativeObject) pop1Node.executeRead(frame);
            pushNode.executeWrite(frame, receiver.getLong(atIndex));
        }
    }

    protected static final class PrimByteAtPutNode extends AbstractTrinaryInlinePrimitiveNode {
        protected PrimByteAtPutNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(frame, successorIndex, sp);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            final long value = (long) pop3Node.executeRead(frame);
            final long atIndex = (long) pop2Node.executeRead(frame);
            final NativeObject receiver = (NativeObject) pop1Node.executeRead(frame);
            receiver.setByte(atIndex, (byte) value);
        }
    }

    protected static final class PrimShortAtPutNode extends AbstractTrinaryInlinePrimitiveNode {
        protected PrimShortAtPutNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(frame, successorIndex, sp);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            final long value = (long) pop3Node.executeRead(frame);
            final long atIndex = (long) pop2Node.executeRead(frame);
            final NativeObject receiver = (NativeObject) pop1Node.executeRead(frame);
            receiver.setShort(atIndex, (short) value);
        }
    }

    protected static final class PrimWordAtPutNode extends AbstractTrinaryInlinePrimitiveNode {
        protected PrimWordAtPutNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(frame, successorIndex, sp);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            final long value = (long) pop3Node.executeRead(frame);
            final long atIndex = (long) pop2Node.executeRead(frame);
            final NativeObject receiver = (NativeObject) pop1Node.executeRead(frame);
            receiver.setInt(atIndex, (int) value);
        }
    }

    protected static final class PrimDoubleWordAtPutNode extends AbstractTrinaryInlinePrimitiveNode {
        protected PrimDoubleWordAtPutNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(frame, successorIndex, sp);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            final long value = (long) pop3Node.executeRead(frame);
            final long atIndex = (long) pop2Node.executeRead(frame);
            final NativeObject receiver = (NativeObject) pop1Node.executeRead(frame);
            receiver.setLong(atIndex, value);
        }
    }

    protected static final class PrimByteEqualsNode extends AbstractTrinaryInlinePrimitiveNode {
        protected PrimByteEqualsNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(frame, successorIndex, sp);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            // TODO: Make use of `final long length = (long) pop3Node.execute(frame);`
            final NativeObject argument = (NativeObject) pop2Node.executeRead(frame);
            final NativeObject receiver = (NativeObject) pop1Node.executeRead(frame);
            pushNode.executeWrite(frame, BooleanObject.wrap(Arrays.equals(receiver.getByteStorage(), argument.getByteStorage())));
        }
    }

    protected abstract static class PrimFillFromToWithNode extends AbstractQuaternaryInlinePrimitiveNode {

        protected PrimFillFromToWithNode(final VirtualFrame frame, final int successorIndex, final int sp) {
            super(frame, successorIndex, sp);
        }

        public static AbstractBytecodeNode create(final VirtualFrame frame, final int successorIndex, final int sp) {
            return PrimFillFromToWithNodeGen.create(frame, successorIndex, sp);
        }

        @Specialization
        protected final void doFillFromToWith(final VirtualFrame frame,
                        @Bind final Node node,
                        @Cached final SqueakObjectAtPut0Node atPutNode) {
            final Object value = pop4Node.executeRead(frame);
            final long to = (long) pop3Node.executeRead(frame);
            final long from = (long) pop2Node.executeRead(frame);
            final Object receiver = pop1Node.executeRead(frame);
            // TODO: maybe there's a more efficient way to fill pointers object?
            for (long i = from; i < to; i++) {
                atPutNode.execute(node, receiver, i, value);
            }
            pushNode.executeWrite(frame, receiver);
        }
    }
}
