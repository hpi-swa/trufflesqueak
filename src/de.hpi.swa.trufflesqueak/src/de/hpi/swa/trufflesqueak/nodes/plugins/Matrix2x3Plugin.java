/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.POINT;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive0WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive2WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;

public class Matrix2x3Plugin extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return Matrix2x3PluginFactory.getFactories();
    }

    protected abstract static class AbstractMatrix2x3PrimitiveNode extends AbstractPrimitiveNode {
        protected static final int MATRIX_SIZE = 6;
        protected static final int FLOAT_ONE = Float.floatToIntBits(1.0F);
        private final BranchProfile invalidSizeProfile = BranchProfile.create();

        protected final int[] loadMatrix(final NativeObject object) {
            final int[] ints = object.getIntStorage();
            if (ints.length != MATRIX_SIZE) {
                invalidSizeProfile.enter();
                throw PrimitiveFailed.GENERIC_ERROR;
            }
            return ints;
        }

        @ExplodeLoop
        protected final float[] loadMatrixAsFloat(final NativeObject object) {
            final int[] ints = loadMatrix(object);
            final float[] floats = new float[MATRIX_SIZE];
            for (int i = 0; i < MATRIX_SIZE; i++) {
                floats[i] = Float.intBitsToFloat(ints[i]);
            }
            return floats;
        }

        protected final double loadArgumentPointX(final PointersObject point, final AbstractPointersObjectReadNode readNode, final InlinedBranchProfile errorProfile, final Node node) {
            return loadArgumentPointAt(point, POINT.X, readNode, errorProfile, node);
        }

        protected final double loadArgumentPointY(final PointersObject point, final AbstractPointersObjectReadNode readNode, final InlinedBranchProfile errorProfile, final Node node) {
            return loadArgumentPointAt(point, POINT.Y, readNode, errorProfile, node);
        }

        private double loadArgumentPointAt(final PointersObject point, final long index, final AbstractPointersObjectReadNode readNode, final InlinedBranchProfile errorProfile, final Node node) {
            if (isPoint(point)) {
                final Object value = readNode.execute(node, point, index);
                if (value instanceof final Long longValue) {
                    return longValue;
                } else if (value instanceof final Double doubleValue) {
                    return doubleValue;
                }
            }
            errorProfile.enter(node);
            throw PrimitiveFailed.GENERIC_ERROR;
        }

        protected static final double[] matrix2x3InvertPoint(final float[] m, final double m23ArgX, final double m23ArgY, final InlinedBranchProfile errorProfile, final Node node) {
            final double x = m23ArgX - m[2];
            final double y = m23ArgY - m[5];
            double det = m[0] * m[4] - m[1] * m[3];
            if (det == 0.0) {
                /* "Matrix is singular." */
                errorProfile.enter(node);
                throw PrimitiveFailed.GENERIC_ERROR;
            }
            det = 1.0 / det;
            final double detX = x * m[4] - m[1] * y;
            final double detY = m[0] * y - x * m[3];
            return new double[]{detX * det, detY * det};
        }

        protected static final double matrix2x3TransformPointX(final float[] m, final double m23ArgX, final double m23ArgY) {
            return m23ArgX * m[0] + m23ArgY * m[1] + m[2];
        }

        protected static final double matrix2x3TransformPointY(final float[] m, final double m23ArgX, final double m23ArgY) {
            return m23ArgX * m[3] + m23ArgY * m[4] + m[5];
        }

        protected final PointersObject roundAndStoreResultPoint(final double m23ResultXValue, final double m23ResultYValue,
                        final AbstractPointersObjectWriteNode writeNode, final InlinedBranchProfile errorProfile, final Node node) {
            final double m23ResultX = m23ResultXValue + 0.5;
            final double m23ResultY = m23ResultYValue + 0.5;
            if (!(okayIntValue(m23ResultX) && okayIntValue(m23ResultY))) {
                errorProfile.enter(node);
                throw PrimitiveFailed.GENERIC_ERROR;
            }
            return getContext().asPoint(writeNode, node, (long) m23ResultX, (long) m23ResultY);
        }

        protected final PointersObject roundAndStoreResultRect(final PointersObject dstRect, final double x0, final double y0, final double x1, final double y1,
                        final AbstractPointersObjectWriteNode writeNode, final InlinedBranchProfile errorProfile, final Node node) {
            final double minX = x0 + 0.5;
            final double maxX = x1 + 0.5;
            final double minY = y0 + 0.5;
            final double maxY = y1 + 0.5;
            if (!(okayIntValue(minX) && okayIntValue(maxX) && okayIntValue(minY) && okayIntValue(maxY))) {
                errorProfile.enter(node);
                throw PrimitiveFailed.GENERIC_ERROR;
            }
            final SqueakImageContext image = getContext();
            final PointersObject origin = image.asPoint(writeNode, node, (long) minX, (long) minY);
            final PointersObject corner = image.asPoint(writeNode, node, (long) maxX, (long) maxY);
            writeNode.execute(node, dstRect, 0, origin);
            writeNode.execute(node, dstRect, 1, corner);
            return dstRect;
        }

        private static boolean okayIntValue(final double value) {
            return Long.MIN_VALUE <= value && value <= Long.MAX_VALUE;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveComposeMatrix")
    protected abstract static class PrimComposeMatrixNode extends AbstractMatrix2x3PrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = {"receiver.isIntType()", "aTransformation.isIntType()", "result.isIntType()"})
        protected final Object doCompose(final NativeObject receiver, final NativeObject aTransformation, final NativeObject result) {
            final float[] m1 = loadMatrixAsFloat(receiver);
            final float[] m2 = loadMatrixAsFloat(aTransformation);
            final int[] m3 = loadMatrix(result);
            m3[0] = Float.floatToRawIntBits(m1[0] * m2[0] + m1[1] * m2[3]);
            m3[1] = Float.floatToRawIntBits(m1[0] * m2[1] + m1[1] * m2[4]);
            m3[2] = Float.floatToRawIntBits(m1[0] * m2[2] + m1[1] * m2[5] + m1[2]);
            m3[3] = Float.floatToRawIntBits(m1[3] * m2[0] + m1[4] * m2[3]);
            m3[4] = Float.floatToRawIntBits(m1[3] * m2[1] + m1[4] * m2[4]);
            m3[5] = Float.floatToRawIntBits(m1[3] * m2[2] + m1[4] * m2[5] + m1[5]);
            return result;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveInvertPoint")
    protected abstract static class PrimInvertPointNode extends AbstractMatrix2x3PrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"receiver.isIntType()", "receiver.getIntLength() == 6"})
        protected final PointersObject doInvert(final NativeObject receiver, final PointersObject point,
                        @Bind final Node node,
                        @Cached final AbstractPointersObjectReadNode readNode,
                        @Cached final AbstractPointersObjectWriteNode writeNode,
                        @Cached final InlinedBranchProfile errorProfile) {
            final double m23ArgX = loadArgumentPointX(point, readNode, errorProfile, node);
            final double m23ArgY = loadArgumentPointY(point, readNode, errorProfile, node);
            final float[] m = loadMatrixAsFloat(receiver);
            final double[] m23Result = matrix2x3InvertPoint(m, m23ArgX, m23ArgY, errorProfile, node);
            return roundAndStoreResultPoint(m23Result[0], m23Result[1], writeNode, errorProfile, node);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveInvertRectInto")
    protected abstract static class PrimInvertRectIntoNode extends AbstractMatrix2x3PrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = {"receiver.isIntType()", "receiver.getIntLength() == 6", "srcRect.getSqueakClass() == dstRect.getSqueakClass()", "srcRect.size() == 2"})
        protected final PointersObject doInvert(final NativeObject receiver, final PointersObject srcRect, final PointersObject dstRect,
                        @Bind final Node node,
                        @Cached final AbstractPointersObjectReadNode readPointNode,
                        @Cached final AbstractPointersObjectReadNode readNode,
                        @Cached final AbstractPointersObjectWriteNode writeNode,
                        @Cached final InlinedBranchProfile errorProfile) {
            final float[] m = loadMatrixAsFloat(receiver);

            /* Load top-left point */
            final PointersObject originPoint = readPointNode.executePointers(node, srcRect, 0);
            final double originX = loadArgumentPointX(originPoint, readNode, errorProfile, node);
            final double originY = loadArgumentPointY(originPoint, readNode, errorProfile, node);
            final double[] result1 = matrix2x3InvertPoint(m, originX, originY, errorProfile, node);
            double minX = result1[0];
            double maxX = minX;
            double minY = result1[1];
            double maxY = minY;

            /* Load bottom-right point */
            final PointersObject cornerPoint = readPointNode.executePointers(node, srcRect, 1);
            final double cornerX = loadArgumentPointX(cornerPoint, readNode, errorProfile, node);
            final double cornerY = loadArgumentPointY(cornerPoint, readNode, errorProfile, node);
            final double[] result2 = matrix2x3InvertPoint(m, originX, originY, errorProfile, node);
            minX = Math.min(minX, result2[0]);
            maxX = Math.max(maxX, result2[0]);
            minY = Math.min(minY, result2[1]);
            maxY = Math.max(maxY, result2[1]);

            /* Load top-right point */
            final double[] result3 = matrix2x3InvertPoint(m, cornerX, originY, errorProfile, node);
            minX = Math.min(minX, result3[0]);
            maxX = Math.max(maxX, result3[0]);
            minY = Math.min(minY, result3[1]);
            maxY = Math.max(maxY, result3[1]);

            /* Load bottom-left point */
            final double[] result4 = matrix2x3InvertPoint(m, originX, cornerY, errorProfile, node);
            minX = Math.min(minX, result4[0]);
            maxX = Math.max(maxX, result4[0]);
            minY = Math.min(minY, result4[1]);
            maxY = Math.max(maxY, result4[1]);

            return roundAndStoreResultRect(dstRect, minX, minY, maxX, maxY, writeNode, errorProfile, node);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsIdentity")
    protected abstract static class PrimIsIdentityNode extends AbstractMatrix2x3PrimitiveNode implements Primitive0WithFallback {
        @Specialization(guards = "receiver.isIntType()")
        protected final Object doIdentity(final NativeObject receiver) {
            final int[] ints = loadMatrix(receiver);
            return BooleanObject.wrap(ints[0] == FLOAT_ONE && ints[1] == 0 && ints[2] == 0 && ints[3] == 0 && ints[4] == FLOAT_ONE && ints[5] == 0);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveIsPureTranslation")
    protected abstract static class PrimIsPureTranslationNode extends AbstractMatrix2x3PrimitiveNode implements Primitive0WithFallback {
        @Specialization(guards = "receiver.isIntType()")
        protected final Object doPure(final NativeObject receiver) {
            final int[] ints = loadMatrix(receiver);
            return BooleanObject.wrap(ints[0] == FLOAT_ONE && ints[1] == 0 && ints[3] == 0 && ints[4] == FLOAT_ONE);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveTransformPoint")
    protected abstract static class PrimTransformPointNode extends AbstractMatrix2x3PrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"receiver.isIntType()", "receiver.getIntLength() == 6"})
        protected final PointersObject doTransform(final NativeObject receiver, final PointersObject point,
                        @Bind final Node node,
                        @Cached final AbstractPointersObjectReadNode readNode,
                        @Cached final AbstractPointersObjectWriteNode writeNode,
                        @Cached final InlinedBranchProfile errorProfile) {
            final double m23ArgX = loadArgumentPointX(point, readNode, errorProfile, node);
            final double m23ArgY = loadArgumentPointY(point, readNode, errorProfile, node);
            final float[] m = loadMatrixAsFloat(receiver);
            return roundAndStoreResultPoint(matrix2x3TransformPointX(m, m23ArgX, m23ArgY), matrix2x3TransformPointY(m, m23ArgX, m23ArgY), writeNode, errorProfile, node);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveTransformRectInto")
    protected abstract static class PrimTransformRectIntoNode extends AbstractMatrix2x3PrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = {"receiver.isIntType()", "receiver.getIntLength() == 6", "srcRect.getSqueakClass() == dstRect.getSqueakClass()", "srcRect.size() == 2"})
        protected final PointersObject doTransform(final NativeObject receiver, final PointersObject srcRect, final PointersObject dstRect,
                        @Bind final Node node,
                        @Cached final AbstractPointersObjectReadNode readPointNode,
                        @Cached final AbstractPointersObjectReadNode readNode,
                        @Cached final AbstractPointersObjectWriteNode writeNode,
                        @Cached final InlinedBranchProfile errorProfile) {
            final float[] m = loadMatrixAsFloat(receiver);

            /* Load top-left point */
            final PointersObject point0 = readPointNode.executePointers(node, srcRect, 0);
            final double originX = loadArgumentPointX(point0, readNode, errorProfile, node);
            final double originY = loadArgumentPointY(point0, readNode, errorProfile, node);
            double minX = matrix2x3TransformPointX(m, originX, originY);
            double maxX = minX;
            double minY = matrix2x3TransformPointY(m, originX, originY);
            double maxY = minY;

            /* Load bottom-right point */
            final PointersObject point1 = readPointNode.executePointers(node, srcRect, 1);
            final double cornerX = loadArgumentPointX(point1, readNode, errorProfile, node);
            final double cornerY = loadArgumentPointY(point1, readNode, errorProfile, node);
            final double m23ResultX1 = matrix2x3TransformPointX(m, cornerX, cornerY);
            final double m23ResultY1 = matrix2x3TransformPointY(m, cornerX, cornerY);
            minX = Math.min(minX, m23ResultX1);
            maxX = Math.max(maxX, m23ResultX1);
            minY = Math.min(minY, m23ResultY1);
            maxY = Math.max(maxY, m23ResultY1);

            /* Load top-right point */
            final double m23ResultX2 = matrix2x3TransformPointX(m, cornerX, originY);
            final double m23ResultY2 = matrix2x3TransformPointY(m, cornerX, originY);
            minX = Math.min(minX, m23ResultX2);
            maxX = Math.max(maxX, m23ResultX2);
            minY = Math.min(minY, m23ResultY2);
            maxY = Math.max(maxY, m23ResultY2);

            /* Load bottom-left point */
            final double m23ResultX3 = matrix2x3TransformPointX(m, originX, cornerY);
            final double m23ResultY3 = matrix2x3TransformPointY(m, originX, cornerY);
            minX = Math.min(minX, m23ResultX3);
            maxX = Math.max(maxX, m23ResultX3);
            minY = Math.min(minY, m23ResultY3);
            maxY = Math.max(maxY, m23ResultY3);

            return roundAndStoreResultRect(dstRect, minX, minY, maxX, maxY, writeNode, errorProfile, node);
        }
    }
}
