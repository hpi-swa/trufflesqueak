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
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive7WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive0WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive2WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive4WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive5WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive6WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.util.MiscUtils;

public final class B2DPlugin extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return B2DPluginFactory.getFactories();
    }

    // primitiveAbortProcessing omitted because it does not seem to be used.

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAddActiveEdgeEntry")
    protected abstract static class PrimAddActiveEdgeEntryNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization
        protected final PointersObject doAdd(final PointersObject receiver, final PointersObject edgeEntry) {
            getContext().b2d.primitiveAddActiveEdgeEntry(receiver, edgeEntry);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAddBezier")
    protected abstract static class PrimAddBezierNode extends AbstractPrimitiveNode implements Primitive5WithFallback {

        @Specialization(guards = {"isPoint(start)", "isPoint(stop)", "isPoint(via)"})
        protected final PointersObject doAdd(final PointersObject receiver, final PointersObject start, final PointersObject stop, final PointersObject via, final long leftFillIndex,
                        final long rightFillIndex) {
            getContext().b2d.primitiveAddBezier(receiver, start, stop, via, MiscUtils.toIntExact(leftFillIndex), MiscUtils.toIntExact(rightFillIndex));
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAddBezierShape")
    protected abstract static class PrimAddBezierShapeNode extends AbstractPrimitiveNode implements Primitive5WithFallback {

        @Specialization
        protected final PointersObject doAdd(final PointersObject receiver, final AbstractSqueakObject points, final long nSegments, final long fillStyle, final long lineWidth,
                        final long lineFill) {
            getContext().b2d.primitiveAddBezierShape(receiver, points, MiscUtils.toIntExact(nSegments), (int) fillStyle, MiscUtils.toIntExact(lineWidth), (int) lineFill);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAddBitmapFill")
    protected abstract static class PrimAddBitmapFillNode extends AbstractPrimitiveNode implements Primitive7WithFallback {

        @Specialization(guards = {"xIndex > 0", "isPoint(origin)", "isPoint(direction)", "isPoint(normal)"})
        protected final long doAdd(final PointersObject receiver, final PointersObject form, final AbstractSqueakObject cmap, final boolean tileFlag, final PointersObject origin,
                        final PointersObject direction, final PointersObject normal, final long xIndex) {
            return getContext().b2d.primitiveAddBitmapFill(receiver, form, cmap, tileFlag, origin, direction, normal, MiscUtils.toIntExact(xIndex));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAddCompressedShape")
    protected abstract static class PrimAddCompressedShapeNode extends AbstractPrimitiveNode implements Primitive7WithFallback {

        @Specialization
        protected final PointersObject doAdd(final PointersObject receiver, final NativeObject points, final long nSegments, final NativeObject leftFills, final NativeObject rightFills,
                        final NativeObject lineWidths, final NativeObject lineFills, final NativeObject fillIndexList) {
            getContext().b2d.primitiveAddCompressedShape(receiver, points, MiscUtils.toIntExact(nSegments), leftFills, rightFills, lineWidths, lineFills, fillIndexList);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAddGradientFill")
    protected abstract static class PrimAddGradientFillNode extends AbstractPrimitiveNode implements Primitive5WithFallback {

        @Specialization(guards = {"isBitmap(colorRamp)", "isPoint(origin)", "isPoint(direction)", "isPoint(normal)"})
        protected final long doAdd(final PointersObject receiver, final NativeObject colorRamp, final PointersObject origin, final PointersObject direction,
                        final PointersObject normal,
                        final boolean isRadial) {
            return getContext().b2d.primitiveAddGradientFill(receiver, colorRamp, origin, direction, normal, isRadial);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAddLine")
    protected abstract static class PrimAddLineNode extends AbstractPrimitiveNode implements Primitive4WithFallback {

        @Specialization(guards = {"isPoint(start)", "isPoint(end)"})
        protected final PointersObject doAdd(final PointersObject receiver, final PointersObject start, final PointersObject end, final long leftFill, final long rightFill) {
            getContext().b2d.primitiveAddLine(receiver, start, end, MiscUtils.toIntExact(leftFill), MiscUtils.toIntExact(rightFill));
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAddOval")
    protected abstract static class PrimAddOvalNode extends AbstractPrimitiveNode implements Primitive5WithFallback {

        @Specialization(guards = {"isPoint(start)", "isPoint(end)"})
        protected final PointersObject doAdd(final PointersObject receiver, final PointersObject start, final PointersObject end, final long fillIndex, final long width,
                        final long pixelValue32) {
            getContext().b2d.primitiveAddOval(receiver, start, end, MiscUtils.toIntExact(fillIndex), MiscUtils.toIntExact(width), MiscUtils.toIntExact(pixelValue32));
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAddPolygon")
    protected abstract static class PrimAddPolygonNode extends AbstractPrimitiveNode implements Primitive5WithFallback {

        @Specialization
        protected final PointersObject doAdd(final PointersObject receiver, final AbstractSqueakObject points, final long nSegments, final long fillStyle, final long lineWidth, final long lineFill) {
            getContext().b2d.primitiveAddPolygon(receiver, points, MiscUtils.toIntExact(nSegments), (int) fillStyle, MiscUtils.toIntExact(lineWidth), (int) lineFill);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAddRect")
    protected abstract static class PrimAddRectNode extends AbstractPrimitiveNode implements Primitive5WithFallback {

        @Specialization(guards = {"isPoint(start)", "isPoint(end)"})
        protected final PointersObject doAdd(final PointersObject receiver, final PointersObject start, final PointersObject end, final long fillIndex, final long width,
                        final long pixelValue32) {
            getContext().b2d.primitiveAddRect(receiver, start, end, MiscUtils.toIntExact(fillIndex), MiscUtils.toIntExact(width), MiscUtils.toIntExact(pixelValue32));
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveChangedActiveEdgeEntry")
    protected abstract static class PrimChangedActiveEdgeEntryNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization
        protected final PointersObject doChange(final PointersObject receiver, final PointersObject edgeEntry) {
            getContext().b2d.primitiveChangedActiveEdgeEntry(receiver, edgeEntry);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveCopyBuffer")
    protected abstract static class PrimCopyBufferNode extends AbstractPrimitiveNode implements Primitive2WithFallback {

        @Specialization(guards = {"oldBuffer.isIntType()", "newBuffer.isIntType()"})
        protected final PointersObject doCopy(final PointersObject receiver, final NativeObject oldBuffer, final NativeObject newBuffer) {
            getContext().b2d.primitiveCopyBuffer(oldBuffer, newBuffer);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDisplaySpanBuffer")
    protected abstract static class PrimDisplaySpanBufferNode extends AbstractPrimitiveNode implements Primitive0WithFallback {

        @Specialization
        protected final PointersObject doDisplay(final PointersObject receiver) {
            getContext().b2d.primitiveDisplaySpanBuffer(receiver);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDoProfileStats")
    protected abstract static class PrimDoProfileStatsNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization
        protected final boolean doProfile(@SuppressWarnings("unused") final Object receiver, final boolean aBoolean) {
            return getContext().b2d.primitiveDoProfileStats(aBoolean);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFinishedProcessing")
    protected abstract static class PrimFinishedProcessingNode extends AbstractPrimitiveNode implements Primitive0WithFallback {

        @Specialization
        protected final boolean doCopy(final PointersObject receiver) {
            return getContext().b2d.primitiveFinishedProcessing(receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetAALevel")
    protected abstract static class PrimGetAALevelNode extends AbstractPrimitiveNode implements Primitive0WithFallback {

        @Specialization
        protected final long doGet(final PointersObject receiver) {
            return getContext().b2d.primitiveGetAALevel(receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetBezierStats")
    protected abstract static class PrimGetBezierStatsNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization(guards = {"statsArray.isIntType()", "statsArray.getIntLength() >= 4"})
        protected final PointersObject doGet(final PointersObject receiver, final NativeObject statsArray) {
            getContext().b2d.primitiveGetBezierStats(receiver, statsArray);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetClipRect")
    protected abstract static class PrimGetClipRectNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization(guards = {"rect.size() >= 2"})
        protected final PointersObject doGet(final PointersObject receiver, final PointersObject rect,
                        @Bind final Node node,
                        @Cached final AbstractPointersObjectWriteNode writeNode) {
            getContext().b2d.primitiveGetClipRect(writeNode, node, receiver, rect);
            return rect;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetCounts")
    protected abstract static class PrimGetCountsNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization(guards = {"statsArray.isIntType()", "statsArray.getIntLength() >= 9"})
        protected final PointersObject doGet(final PointersObject receiver, final NativeObject statsArray) {
            getContext().b2d.primitiveGetCounts(receiver, statsArray);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetDepth")
    protected abstract static class PrimGetDepthNode extends AbstractPrimitiveNode implements Primitive0WithFallback {

        @Specialization
        protected final long doGet(final PointersObject receiver) {
            return getContext().b2d.primitiveGetDepth(receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetFailureReason")
    protected abstract static class PrimGetFailureReasonNode extends AbstractPrimitiveNode implements Primitive0WithFallback {

        @Specialization
        protected final long doGet(final PointersObject receiver) {
            return getContext().b2d.primitiveGetFailureReason(receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetOffset")
    protected abstract static class PrimGetOffsetNode extends AbstractPrimitiveNode implements Primitive0WithFallback {

        @Specialization
        protected final PointersObject doGet(final PointersObject receiver,
                        @Bind final Node node,
                        @Cached final AbstractPointersObjectWriteNode writeNode) {
            return getContext().b2d.primitiveGetOffset(writeNode, node, receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetTimes")
    protected abstract static class PrimGetTimesNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization(guards = {"statsArray.isIntType()", "statsArray.getIntLength() >= 9"})
        protected final PointersObject doGet(final PointersObject receiver, final NativeObject statsArray) {
            getContext().b2d.primitiveGetTimes(receiver, statsArray);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveInitializeBuffer")
    protected abstract static class PrimInitializeBufferNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization(guards = {"buffer.isIntType()", "hasMinimalSize(buffer)"})
        protected final Object doInit(final Object receiver, final NativeObject buffer) {
            getContext().b2d.primitiveInitializeBuffer(buffer);
            return receiver;
        }

        protected static final boolean hasMinimalSize(final NativeObject buffer) {
            return buffer.getIntLength() >= B2D.GW_MINIMAL_SIZE;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveInitializeProcessing")
    protected abstract static class PrimInitializeProcessingNode extends AbstractPrimitiveNode implements Primitive0WithFallback {

        @Specialization
        protected final PointersObject doCopy(final PointersObject receiver) {
            getContext().b2d.primitiveInitializeProcessing(receiver);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveMergeFillFrom")
    protected abstract static class PrimMergeFillFromNode extends AbstractPrimitiveNode implements Primitive2WithFallback {

        @Specialization(guards = {"isBitmap(fillBitmap)"})
        protected final PointersObject doCopy(final PointersObject receiver, final NativeObject fillBitmap, final PointersObject fill) {
            getContext().b2d.primitiveMergeFillFrom(receiver, fillBitmap, fill);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveNeedsFlush")
    protected abstract static class PrimNeedsFlushNode extends AbstractPrimitiveNode implements Primitive0WithFallback {

        @Specialization
        protected final boolean doNeed(final PointersObject receiver) {
            return getContext().b2d.primitiveNeedsFlush(receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveNeedsFlushPut")
    protected abstract static class PrimNeedsFlushPutNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization
        protected final PointersObject doNeed(final PointersObject receiver, final boolean aBoolean) {
            getContext().b2d.primitiveNeedsFlushPut(receiver, aBoolean);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveNextActiveEdgeEntry")
    protected abstract static class PrimNextActiveEdgeEntryNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization
        protected final boolean doNext(final PointersObject receiver, final PointersObject edgeEntry) {
            return getContext().b2d.primitiveNextActiveEdgeEntry(receiver, edgeEntry);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveNextFillEntry")
    protected abstract static class PrimNextFillEntryNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization
        protected final boolean doNext(final PointersObject receiver, final PointersObject fillEntry) {
            return getContext().b2d.primitiveNextFillEntry(receiver, fillEntry);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveNextGlobalEdgeEntry")
    protected abstract static class PrimNextGlobalEdgeEntryNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization
        protected final boolean doNext(final PointersObject receiver, final PointersObject edgeEntry) {
            return getContext().b2d.primitiveNextGlobalEdgeEntry(receiver, edgeEntry);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveRegisterExternalEdge")
    protected abstract static class PrimRegisterExternalEdgeNode extends AbstractPrimitiveNode implements Primitive6WithFallback {

        @Specialization
        protected final PointersObject doRegister(final PointersObject receiver, final long index, final long initialX, final long initialY, final long initialZ, final long leftFillIndex,
                        final long rightFillIndex) {
            getContext().b2d.primitiveRegisterExternalEdge(receiver, MiscUtils.toIntExact(index), MiscUtils.toIntExact(initialX), MiscUtils.toIntExact(initialY), MiscUtils.toIntExact(initialZ),
                            MiscUtils.toIntExact(leftFillIndex), MiscUtils.toIntExact(rightFillIndex));
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveRegisterExternalFill")
    protected abstract static class PrimRegisterExternalFillNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization
        protected final long doRegister(final PointersObject receiver, final long index) {
            return getContext().b2d.primitiveRegisterExternalFill(receiver, MiscUtils.toIntExact(index));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveRenderImage")
    protected abstract static class PrimRenderImageNode extends AbstractPrimitiveNode implements Primitive2WithFallback {

        @Specialization
        protected final long doRender(final PointersObject receiver, final PointersObject edge, final PointersObject fill) {
            return getContext().b2d.primitiveRenderImage(receiver, edge, fill);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveRenderScanline")
    protected abstract static class PrimRenderScanlineNode extends AbstractPrimitiveNode implements Primitive2WithFallback {

        @Specialization
        protected final long doRender(final PointersObject receiver, final PointersObject edge, final PointersObject fill) {
            return getContext().b2d.primitiveRenderScanline(receiver, edge, fill);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSetAALevel")
    protected abstract static class PrimSetAALevelNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization
        protected final PointersObject doSet(final PointersObject receiver, final long level) {
            getContext().b2d.primitiveSetAALevel(receiver, MiscUtils.toIntExact(level));
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSetBitBltPlugin")
    protected abstract static class PrimSetBitBltPluginNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization(guards = {"pluginName.isByteType()"})
        protected static final ClassObject doSet(final ClassObject receiver, final NativeObject pluginName) {
            return B2D.primitiveSetBitBltPlugin(receiver, pluginName);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSetClipRect")
    protected abstract static class PrimSetClipRectNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization(guards = {"rect.size() >= 2"})
        protected final PointersObject doSet(final PointersObject receiver, final PointersObject rect) {
            getContext().b2d.primitiveSetClipRect(receiver, rect);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSetColorTransform")
    protected abstract static class PrimSetColorTransformNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization
        protected final PointersObject doSet(final PointersObject receiver, final AbstractSqueakObject transform) {
            getContext().b2d.primitiveSetColorTransform(receiver, transform);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSetDepth")
    protected abstract static class PrimSetDepthNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization
        protected final PointersObject doSet(final PointersObject receiver, final long depth) {
            getContext().b2d.primitiveSetDepth(receiver, MiscUtils.toIntExact(depth));
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSetEdgeTransform")
    protected abstract static class PrimSetEdgeTransformNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization
        protected final PointersObject doSet(final PointersObject receiver, final AbstractSqueakObject transform) {
            getContext().b2d.primitiveSetEdgeTransform(receiver, transform);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSetOffset")
    protected abstract static class PrimSetOffsetNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization(guards = {"isPoint(point)"})
        protected final PointersObject doSet(final PointersObject receiver, final PointersObject point) {
            getContext().b2d.primitiveSetOffset(receiver, point);
            return receiver;
        }
    }
}
