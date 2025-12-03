/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.interpreter;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;

import de.hpi.swa.trufflesqueak.exceptions.RespecializeException;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodesFactory.AbstractPointersObjectReadNodeGen;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodesFactory.AbstractPointersObjectWriteNodeGen;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodes.ArrayObjectSizeNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodesFactory.ArrayObjectSizeNodeGen;
import de.hpi.swa.trufflesqueak.nodes.accessing.FloatObjectNodes.FloatObjectNormalizeNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector0Node.Dispatch0Node;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector0NodeFactory.Dispatch0NodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector1Node.Dispatch1Node;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector1NodeFactory.Dispatch1NodeGen;
import de.hpi.swa.trufflesqueak.nodes.interpreter.BytecodePrimsFactory.BytecodePrimAddNodeFactory.BytecodePrimAddLongNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimAddNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimBitShiftNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimDivideLargeIntegersNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimDivideNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimFloorDivideNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimFloorModLargeIntegersNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimFloorModNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimMultiplyLargeIntegersNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimMultiplyNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimSmallFloatDivideNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimSmallFloatMultiplyNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimSmallFloatSubtractNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimSubtractLargeIntegersNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives.PrimSubtractNode;

public class BytecodePrims {
    public static final class BytecodePrimAddNode extends AbstractNode {
        @Child BytecodePrimAddLongNode longNode = BytecodePrimAddLongNodeGen.create();
        @Child Dispatch1Node dispatchNode;

        BytecodePrimAddNode(final SqueakImageContext image) {
            dispatchNode = Dispatch1NodeGen.create(image.getSpecialSelector(0));
        }

        @GenerateInline(false)
        public abstract static class BytecodePrimAddLongNode extends AbstractNode {
            abstract Object execute(Long lhs, Long rhs);

            @Specialization(rewriteOn = ArithmeticException.class)
            protected static final Long doLong(final Long lhs, final Long rhs) {
                return PrimAddNode.doLong(lhs, rhs);
            }

            @Specialization(replaces = "doLong")
            protected static final Object doLongWithOverflow(final Long lhs, final Long rhs,
                            @Bind final SqueakImageContext image) {
                return PrimAddNode.doLongWithOverflow(lhs, rhs, image);
            }
        }
    }

    public static final class BytecodePrimMakePointNode extends AbstractNode {
        @Child AbstractPointersObjectWriteNode writeNode = AbstractPointersObjectWriteNodeGen.create();
        @Child Dispatch1Node dispatchNode;

        BytecodePrimMakePointNode(final SqueakImageContext image) {
            dispatchNode = Dispatch1NodeGen.create(image.getSpecialSelector(11));
        }
    }

    public static final class BytecodePrimPointXNode extends AbstractNode {
        @Child AbstractPointersObjectReadNode readNode = AbstractPointersObjectReadNodeGen.create();
        @Child Dispatch0Node dispatchNode;

        BytecodePrimPointXNode(final SqueakImageContext image) {
            dispatchNode = Dispatch0NodeGen.create(image.getSpecialSelector(30));
        }
    }

    public static final class BytecodePrimPointYNode extends AbstractNode {
        @Child AbstractPointersObjectReadNode readNode = AbstractPointersObjectReadNodeGen.create();
        @Child Dispatch0Node dispatchNode;

        BytecodePrimPointYNode(final SqueakImageContext image) {
            dispatchNode = Dispatch0NodeGen.create(image.getSpecialSelector(31));
        }
    }

    public static final class BytecodePrimSizeNode extends AbstractNode {
        @Child ArrayObjectSizeNode arrayNode = ArrayObjectSizeNodeGen.create();
        @Child Dispatch0Node dispatchNode;

        BytecodePrimSizeNode(final SqueakImageContext image) {
            dispatchNode = Dispatch0NodeGen.create(image.getSpecialSelector(18));
        }
    }

    public abstract static class AbstractBytecodePrimNode extends AbstractNode {
        protected final NativeObject getSpecialSelector() {
            return getContext().getSpecialSelector(getSelectorIndex());
        }

        abstract int getSelectorIndex();
    }

    public abstract static class AbstractBytecodePrim0Node extends AbstractBytecodePrimNode {
        public abstract Object execute(VirtualFrame frame, Object receiver);
    }

    public abstract static class AbstractBytecodePrim1Node extends AbstractBytecodePrimNode {
        public abstract Object execute(VirtualFrame frame, Object receiver, Object arg);
    }

    @GenerateInline(false)
    public abstract static class BytecodePrimSubtractNode extends AbstractBytecodePrim1Node {
        @Override
        final int getSelectorIndex() {
            return 1;
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        protected static final long doLong(final long lhs, final long rhs) {
            return PrimSubtractNode.doLong(lhs, rhs);
        }

        @Specialization(replaces = "doLong")
        protected static final Object doLongWithOverflow(final long lhs, final long rhs,
                        @Bind final SqueakImageContext image) {
            return PrimSubtractNode.doLongWithOverflow(lhs, rhs, image);
        }

        @Specialization
        protected static final double doDouble(final double lhs, final double rhs) {
            return PrimSmallFloatSubtractNode.doDouble(lhs, rhs);
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
        protected static final double doLongDouble(final long lhs, final double rhs) {
            return PrimSubtractNode.doLongDouble(lhs, rhs);
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()")
        protected static final double doDoubleLong(final double lhs, final long rhs) {
            return PrimSmallFloatSubtractNode.doLong(lhs, rhs);
        }

        @Specialization(guards = {"image.isLargeInteger(lhs)"})
        protected static final Object doLargeIntegerLong(final NativeObject lhs, final long rhs,
                        @Bind final SqueakImageContext image) {
            return PrimSubtractLargeIntegersNode.doLargeIntegerLong(lhs, rhs, image);
        }

        @Specialization(guards = {"image.isLargeInteger(lhs)", "image.isLargeInteger(rhs)"})
        protected static final Object doLargeInteger(final NativeObject lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image) {
            return PrimSubtractLargeIntegersNode.doLargeInteger(lhs, rhs, image);
        }

        @Specialization(guards = {"image.isLargeInteger(rhs)"})
        protected static final Object doLongLargeInteger(final long lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image) {
            return PrimSubtractNode.doLongLargeInteger(lhs, rhs, image);
        }
    }

    @GenerateInline(false)
    public abstract static class BytecodePrimMultiplyNode extends AbstractBytecodePrim1Node {
        @Override
        final int getSelectorIndex() {
            return 8;
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        protected static final long doLong(final long lhs, final long rhs) {
            return PrimMultiplyNode.doLong(lhs, rhs);
        }

        @Specialization(replaces = "doLong")
        protected static final Object doLongWithOverflow(final long lhs, final long rhs,
                        @Bind final SqueakImageContext image) {
            return PrimMultiplyNode.doLongWithOverflow(lhs, rhs, image);
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()", rewriteOn = RespecializeException.class)
        protected static final double doLongDoubleFinite(final long lhs, final double rhs) throws RespecializeException {
            return PrimMultiplyNode.doLongDoubleFinite(lhs, rhs);
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()", replaces = "doLongDoubleFinite")
        protected static final Object doLongDouble(final long lhs, final double rhs,
                        @Bind final Node node,
                        @Shared("normalizeNode") @Cached final FloatObjectNormalizeNode normalizeNode) {
            return PrimMultiplyNode.doLongDouble(lhs, rhs, node, normalizeNode);
        }

        @Specialization(rewriteOn = RespecializeException.class)
        protected static final double doDoubleFinite(final double lhs, final double rhs) throws RespecializeException {
            return PrimSmallFloatMultiplyNode.doDoubleFinite(lhs, rhs);
        }

        @Specialization(replaces = "doDoubleFinite")
        protected static final Object doDouble(final double lhs, final double rhs,
                        @Bind final Node node,
                        @Shared("normalizeNode") @Cached final FloatObjectNormalizeNode normalizeNode) {
            return PrimSmallFloatMultiplyNode.doDouble(lhs, rhs, node, normalizeNode);
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()", rewriteOn = RespecializeException.class)
        protected static final double doDoubleLongFinite(final double lhs, final long rhs) throws RespecializeException {
            return PrimSmallFloatMultiplyNode.doLongFinite(lhs, rhs);
        }

        @Specialization(guards = "isPrimitiveDoMixedArithmetic()", replaces = "doDoubleLongFinite")
        protected static final Object doDoubleLong(final double lhs, final long rhs,
                        @Bind final Node node,
                        @Shared("normalizeNode") @Cached final FloatObjectNormalizeNode normalizeNode) {
            return PrimSmallFloatMultiplyNode.doLong(lhs, rhs, node, normalizeNode);
        }

        @Specialization(guards = {"image.isLargeInteger(lhs)"})
        protected static final Object doLargeIntegerLong(final NativeObject lhs, final long rhs,
                        @Bind final SqueakImageContext image) {
            return PrimMultiplyLargeIntegersNode.doLargeIntegerLong(lhs, rhs, image);
        }

        @Specialization(guards = {"image.isLargeInteger(lhs)", "image.isLargeInteger(rhs)"})
        protected static final Object doLargeInteger(final NativeObject lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image) {
            return PrimMultiplyLargeIntegersNode.doLargeInteger(lhs, rhs, image);
        }

        @Specialization(guards = {"image.isLargeInteger(rhs)"})
        protected static final Object doLongLargeInteger(final long lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image) {
            return PrimMultiplyNode.doLongLargeInteger(lhs, rhs, image);
        }
    }

    @GenerateInline(false)
    public abstract static class BytecodePrimDivideNode extends AbstractBytecodePrim1Node {
        @Override
        final int getSelectorIndex() {
            return 9;
        }

        @Specialization(guards = {"rhs != 0", "!isOverflowDivision(lhs, rhs)", "isIntegralWhenDividedBy(lhs, rhs)"})
        protected static final long doLong(final long lhs, final long rhs) {
            return PrimDivideNode.doLong(lhs, rhs);
        }

        @Specialization(replaces = "doLong")
        protected static final Object doLongFraction(final long lhs, final long rhs,
                        @Bind final SqueakImageContext image,
                        @Bind final Node node,
                        @Exclusive @Cached final InlinedConditionProfile isZeroProfile,
                        @Exclusive @Cached final InlinedConditionProfile isOverflowProfile,
                        @Exclusive @Cached final InlinedConditionProfile isIntegralProfile,
                        @Cached(inline = true) final AbstractPointersObjectWriteNode writeNode) {
            return PrimDivideNode.doLongFraction(lhs, rhs, image, node, isZeroProfile, isOverflowProfile, isIntegralProfile, writeNode);
        }

        @Specialization(guards = {"isPrimitiveDoMixedArithmetic()"}, rewriteOn = RespecializeException.class)
        protected static final double doLongDoubleFinite(final long lhs, final double rhs,
                        @Bind final Node node,
                        @Shared("isZeroProfile") @Cached final InlinedConditionProfile isZeroProfile) throws RespecializeException {
            return PrimDivideNode.doLongDoubleFinite(lhs, rhs, node, isZeroProfile);
        }

        @Specialization(guards = {"isPrimitiveDoMixedArithmetic()"}, replaces = "doLongDoubleFinite")
        protected static final Object doLongDouble(final long lhs, final double rhs,
                        @Bind final Node node,
                        @Shared("isZeroProfile") @Cached final InlinedConditionProfile isZeroProfile,
                        @Shared("normalizeNode") @Cached final FloatObjectNormalizeNode normalizeNode) {
            return PrimDivideNode.doLongDouble(lhs, rhs, node, isZeroProfile, normalizeNode);
        }

        @Specialization(rewriteOn = RespecializeException.class)
        protected static final double doDoubleFinite(final double lhs, final double rhs,
                        @Bind final Node node,
                        @Shared("isZeroProfile") @Cached final InlinedConditionProfile isZeroProfile) throws RespecializeException {
            return PrimSmallFloatDivideNode.doDoubleFinite(lhs, rhs, node, isZeroProfile);
        }

        @Specialization(replaces = "doDoubleFinite")
        protected static final Object doDouble(final double lhs, final double rhs,
                        @Bind final Node node,
                        @Shared("isZeroProfile") @Cached final InlinedConditionProfile isZeroProfile,
                        @Shared("normalizeNode") @Cached final FloatObjectNormalizeNode normalizeNode) {
            return PrimSmallFloatDivideNode.doDouble(lhs, rhs, node, isZeroProfile, normalizeNode);
        }

        @Specialization(guards = {"isPrimitiveDoMixedArithmetic()"}, rewriteOn = RespecializeException.class)
        protected static final double doDoubleLongFinite(final double lhs, final long rhs,
                        @Bind final Node node,
                        @Shared("isZeroProfile") @Cached final InlinedConditionProfile isZeroProfile) throws RespecializeException {
            return PrimSmallFloatDivideNode.doDoubleLongFinite(lhs, rhs, node, isZeroProfile);
        }

        @Specialization(guards = {"isPrimitiveDoMixedArithmetic()"}, replaces = "doDoubleLongFinite")
        protected static final Object doDoubleLong(final double lhs, final long rhs,
                        @Bind final Node node,
                        @Shared("isZeroProfile") @Cached final InlinedConditionProfile isZeroProfile,
                        @Shared("normalizeNode") @Cached final FloatObjectNormalizeNode normalizeNode) {
            return PrimSmallFloatDivideNode.doDoubleLong(lhs, rhs, node, isZeroProfile, normalizeNode);
        }

        @Specialization(guards = {"image.isLargeInteger(lhs)"})
        protected static final Object doLargeIntegerLong(final NativeObject lhs, final long rhs,
                        @Bind final SqueakImageContext image,
                        @Bind final Node node,
                        @Exclusive @Cached final InlinedConditionProfile isZeroProfile,
                        @Exclusive @Cached final InlinedConditionProfile successProfile) {
            return PrimDivideLargeIntegersNode.doLargeIntegerLong(lhs, rhs, image, node, isZeroProfile, successProfile);
        }

        @Specialization(guards = {"image.isLargeInteger(lhs)", "image.isLargeInteger(rhs)"})
        protected static final Object doLargeInteger(final NativeObject lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image,
                        @Bind final Node node,
                        @Exclusive @Cached final InlinedConditionProfile isZeroProfile,
                        @Exclusive @Cached final InlinedConditionProfile successProfile) {
            return PrimDivideLargeIntegersNode.doLargeInteger(lhs, rhs, image, node, isZeroProfile, successProfile);
        }
    }

    @GenerateInline(false)
    public abstract static class BytecodePrimModNode extends AbstractBytecodePrim1Node {
        @Override
        final int getSelectorIndex() {
            return 10;
        }

        @Specialization
        protected static final long doLong(final long lhs, final long rhs,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isZeroProfile,
                        @Cached final InlinedConditionProfile sameSignProfile) {
            return PrimFloorModNode.doLong(lhs, rhs, node, isZeroProfile, sameSignProfile);
        }

        @Specialization(guards = "image.isLargeInteger(lhs)")
        protected static final Object doLargeIntegerLong(final NativeObject lhs, final long rhs,
                        @Bind final SqueakImageContext image) {
            return PrimFloorModLargeIntegersNode.doLargeIntegerLong(lhs, rhs, image);
        }

        @Specialization(guards = {"image.isLargeInteger(lhs)", "image.isLargeInteger(rhs)"})
        protected static final Object doLargeInteger(final NativeObject lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image) {
            return PrimFloorModLargeIntegersNode.doLargeInteger(lhs, rhs, image);
        }
    }

    @GenerateInline(false)
    public abstract static class BytecodePrimBitShiftNode extends AbstractBytecodePrim1Node {
        @Override
        final int getSelectorIndex() {
            return 12;
        }

        @Specialization(guards = {"arg >= 0"})
        protected static final Object doLongPositive(final long receiver, final long arg,
                        @Bind final Node node,
                        @Exclusive @Cached final InlinedConditionProfile isOverflowProfile) {
            return PrimBitShiftNode.doLongPositive(receiver, arg, node, isOverflowProfile);
        }

        @Specialization(guards = {"arg < 0"})
        protected static final long doLongNegativeInLongSizeRange(final long receiver, final long arg,
                        @Bind final Node node,
                        @Exclusive @Cached final InlinedConditionProfile inLongSizeRangeProfile) {
            return PrimBitShiftNode.doLongNegativeInLongSizeRange(receiver, arg, node, inLongSizeRangeProfile);
        }
    }

    @GenerateInline(false)
    public abstract static class BytecodePrimDivNode extends AbstractBytecodePrim1Node {
        @Override
        final int getSelectorIndex() {
            return 13;
        }

        @Specialization(guards = {"rhs != 0", "!isOverflowDivision(lhs, rhs)"})
        protected static final Object doLong(final long lhs, final long rhs,
                        @Bind final Node node,
                        @Shared("isZeroProfile") @Cached final InlinedConditionProfile isZeroProfile,
                        @Exclusive @Cached final InlinedConditionProfile isOverflowDivisionProfile,
                        @Exclusive @Cached final InlinedConditionProfile sameSignProfile) {
            return PrimFloorDivideNode.doLong(lhs, rhs, node, isZeroProfile, isOverflowDivisionProfile, sameSignProfile);
        }

        @Specialization(guards = {"!isZero(rhs)", "image.isLargeInteger(rhs)"})
        protected static final long doLongLargeInteger(final long lhs, final NativeObject rhs,
                        @Bind final SqueakImageContext image,
                        @Bind final Node node,
                        @Shared("isZeroProfile") @Cached final InlinedConditionProfile isZeroProfile) {
            return PrimFloorDivideNode.doLongLargeInteger(lhs, rhs, image, node, isZeroProfile);
        }
    }
}
