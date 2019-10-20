/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.OctonaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.QuinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.SenaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.SeptenaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;

public final class B2DPlugin extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return B2DPluginFactory.getFactories();
    }

    // primitiveAbortProcessing omitted because it does not seem to be used.

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAddActiveEdgeEntry")
    protected abstract static class PrimAddActiveEdgeEntryNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimAddActiveEdgeEntryNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doAdd(final PointersObject receiver, final PointersObject edgeEntry) {
            return method.image.b2d.primitiveAddActiveEdgeEntry(receiver, edgeEntry);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAddBezier")
    protected abstract static class PrimAddBezierNode extends AbstractPrimitiveNode implements SenaryPrimitive {

        protected PrimAddBezierNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"start.isPoint()", "stop.isPoint()", "via.isPoint()"})
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doAdd(final PointersObject receiver, final PointersObject start, final PointersObject stop, final PointersObject via, final long leftFillIndex,
                        final long rightFillIndex) {
            return method.image.b2d.primitiveAddBezier(receiver, start, stop, via, leftFillIndex, rightFillIndex);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAddBezierShape")
    protected abstract static class PrimAddBezierShapeNode extends AbstractPrimitiveNode implements SenaryPrimitive {

        protected PrimAddBezierShapeNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doAdd(final PointersObject receiver, final AbstractSqueakObject points, final long nSegments, final long fillStyle, final long lineWidth,
                        final long lineFill) {
            return method.image.b2d.primitiveAddBezierShape(receiver, points, nSegments, fillStyle, lineWidth, lineFill);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAddBitmapFill")
    protected abstract static class PrimAddBitmapFillNode extends AbstractPrimitiveNode implements OctonaryPrimitive {

        protected PrimAddBitmapFillNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"xIndex > 0", "origin.isPoint()", "direction.isPoint()", "normal.isPoint()"})
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doAdd(final PointersObject receiver, final PointersObject form, final AbstractSqueakObject cmap, final boolean tileFlag, final PointersObject origin,
                        final PointersObject direction, final PointersObject normal, final long xIndex) {
            return method.image.b2d.primitiveAddBitmapFill(receiver, form, cmap, tileFlag, origin, direction, normal, xIndex);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAddCompressedShape")
    protected abstract static class PrimAddCompressedShapeNode extends AbstractPrimitiveNode implements OctonaryPrimitive {

        protected PrimAddCompressedShapeNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doAdd(final PointersObject receiver, final NativeObject points, final long nSegments, final NativeObject leftFills, final NativeObject rightFills,
                        final NativeObject lineWidths, final NativeObject lineFills, final NativeObject fillIndexList) {
            return method.image.b2d.primitiveAddCompressedShape(receiver, points, nSegments, leftFills, rightFills, lineWidths, lineFills, fillIndexList);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAddGradientFill")
    protected abstract static class PrimAddGradientFillNode extends AbstractPrimitiveNode implements SenaryPrimitive {

        protected PrimAddGradientFillNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"colorRamp.getSqueakClass().isBitmapClass()", "origin.isPoint()", "direction.isPoint()", "normal.isPoint()"})
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doAdd(final PointersObject receiver, final NativeObject colorRamp, final PointersObject origin, final PointersObject direction, final PointersObject normal,
                        final boolean isRadial) {
            return method.image.b2d.primitiveAddGradientFill(receiver, colorRamp, origin, direction, normal, isRadial);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAddLine")
    protected abstract static class PrimAddLineNode extends AbstractPrimitiveNode implements QuinaryPrimitive {

        protected PrimAddLineNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"start.isPoint()", "end.isPoint()"})
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doAdd(final PointersObject receiver, final PointersObject start, final PointersObject end, final long leftFill, final long rightFill) {
            return method.image.b2d.primitiveAddLine(receiver, start, end, leftFill, rightFill);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAddOval")
    protected abstract static class PrimAddOvalNode extends AbstractPrimitiveNode implements SenaryPrimitive {

        protected PrimAddOvalNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"start.isPoint()", "end.isPoint()"})
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doAdd(final PointersObject receiver, final PointersObject start, final PointersObject end, final long fillIndex, final long width,
                        final long pixelValue32) {
            return method.image.b2d.primitiveAddOval(receiver, start, end, fillIndex, width, pixelValue32);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAddPolygon")
    protected abstract static class PrimAddPolygonNode extends AbstractPrimitiveNode implements SenaryPrimitive {

        protected PrimAddPolygonNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doAdd(final PointersObject receiver, final AbstractSqueakObject points, final long nSegments, final long fillStyle, final long lineWidth,
                        final long lineFill) {
            return method.image.b2d.primitiveAddPolygon(receiver, points, nSegments, fillStyle, lineWidth, lineFill);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAddRect")
    protected abstract static class PrimAddRectNode extends AbstractPrimitiveNode implements SenaryPrimitive {

        protected PrimAddRectNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"start.isPoint()", "end.isPoint()"})
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doAdd(final PointersObject receiver, final PointersObject start, final PointersObject end, final long fillIndex, final long width,
                        final long pixelValue32) {
            return method.image.b2d.primitiveAddRect(receiver, start, end, fillIndex, width, pixelValue32);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveChangedActiveEdgeEntry")
    protected abstract static class PrimChangedActiveEdgeEntryNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimChangedActiveEdgeEntryNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doChange(final PointersObject receiver, final PointersObject edgeEntry) {
            return method.image.b2d.primitiveChangedActiveEdgeEntry(receiver, edgeEntry);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveCopyBuffer")
    protected abstract static class PrimCopyBufferNode extends AbstractPrimitiveNode implements TernaryPrimitive {

        protected PrimCopyBufferNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"oldBuffer.isIntType()", "newBuffer.isIntType()"})
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doCopy(final PointersObject receiver, final NativeObject oldBuffer, final NativeObject newBuffer) {
            return method.image.b2d.primitiveCopyBuffer(receiver, oldBuffer, newBuffer);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDisplaySpanBuffer")
    protected abstract static class PrimDisplaySpanBufferNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimDisplaySpanBufferNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doDisplay(final PointersObject receiver) {
            return method.image.b2d.primitiveDisplaySpanBuffer(receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDoProfileStats")
    protected abstract static class PrimDoProfileStatsNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimDoProfileStatsNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doProfile(final Object receiver, final boolean aBoolean) {
            return method.image.b2d.primitiveDoProfileStats(receiver, aBoolean);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFinishedProcessing")
    protected abstract static class PrimFinishedProcessingNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimFinishedProcessingNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doCopy(final PointersObject receiver) {
            return method.image.b2d.primitiveFinishedProcessing(receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetAALevel")
    protected abstract static class PrimGetAALevelNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimGetAALevelNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doGet(final PointersObject receiver) {
            return method.image.b2d.primitiveGetAALevel(receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetBezierStats")
    protected abstract static class PrimGetBezierStatsNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimGetBezierStatsNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"statsArray.isIntType()", "statsArray.getIntLength() >= 4"})
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doGet(final PointersObject receiver, final NativeObject statsArray) {
            return method.image.b2d.primitiveGetBezierStats(receiver, statsArray);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetClipRect")
    protected abstract static class PrimGetClipRectNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimGetClipRectNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"rect.size() >= 2"})
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doGet(final PointersObject receiver, final PointersObject rect,
                        @Cached final AbstractPointersObjectWriteNode writeNode) {
            return method.image.b2d.primitiveGetClipRect(writeNode, receiver, rect);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetCounts")
    protected abstract static class PrimGetCountsNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimGetCountsNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"statsArray.isIntType()", "statsArray.getIntLength() >= 9"})
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doGet(final PointersObject receiver, final NativeObject statsArray) {
            return method.image.b2d.primitiveGetCounts(receiver, statsArray);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetDepth")
    protected abstract static class PrimGetDepthNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimGetDepthNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doGet(final PointersObject receiver) {
            return method.image.b2d.primitiveGetDepth(receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetFailureReason")
    protected abstract static class PrimGetFailureReasonNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimGetFailureReasonNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doGet(final PointersObject receiver) {
            return method.image.b2d.primitiveGetFailureReason(receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetOffset")
    protected abstract static class PrimGetOffsetNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimGetOffsetNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doGet(final PointersObject receiver,
                        @Cached final AbstractPointersObjectWriteNode writeNode) {
            return method.image.b2d.primitiveGetOffset(writeNode, receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetTimes")
    protected abstract static class PrimGetTimesNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimGetTimesNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"statsArray.isIntType()", "statsArray.getIntLength() >= 9"})
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doGet(final PointersObject receiver, final NativeObject statsArray) {
            return method.image.b2d.primitiveGetTimes(receiver, statsArray);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveInitializeBuffer")
    protected abstract static class PrimInitializeBufferNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimInitializeBufferNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"buffer.isIntType()", "hasMinimalSize(buffer)"})
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doInit(final Object receiver, final NativeObject buffer) {
            return method.image.b2d.primitiveInitializeBuffer(receiver, buffer);
        }

        protected static final boolean hasMinimalSize(final NativeObject buffer) {
            return buffer.getIntLength() >= B2D.GW_MINIMAL_SIZE;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveInitializeProcessing")
    protected abstract static class PrimInitializeProcessingNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimInitializeProcessingNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doCopy(final PointersObject receiver) {
            return method.image.b2d.primitiveInitializeProcessing(receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveMergeFillFrom")
    protected abstract static class PrimMergeFillFromNode extends AbstractPrimitiveNode implements TernaryPrimitive {

        protected PrimMergeFillFromNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"fillBitmap.getSqueakClass().isBitmapClass()"})
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doCopy(final PointersObject receiver, final NativeObject fillBitmap, final PointersObject fill) {
            return method.image.b2d.primitiveMergeFillFrom(receiver, fillBitmap, fill);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveNeedsFlush")
    protected abstract static class PrimNeedsFlushNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimNeedsFlushNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doNeed(final PointersObject receiver) {
            return method.image.b2d.primitiveNeedsFlush(receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveNeedsFlushPut")
    protected abstract static class PrimNeedsFlushPutNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimNeedsFlushPutNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doNeed(final PointersObject receiver, final boolean aBoolean) {
            return method.image.b2d.primitiveNeedsFlushPut(receiver, aBoolean);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveNextActiveEdgeEntry")
    protected abstract static class PrimNextActiveEdgeEntryNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimNextActiveEdgeEntryNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doNext(final PointersObject receiver, final PointersObject edgeEntry) {
            return method.image.b2d.primitiveNextActiveEdgeEntry(receiver, edgeEntry);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveNextFillEntry")
    protected abstract static class PrimNextFillEntryNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimNextFillEntryNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doNext(final PointersObject receiver, final PointersObject fillEntry) {
            return method.image.b2d.primitiveNextFillEntry(receiver, fillEntry);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveNextGlobalEdgeEntry")
    protected abstract static class PrimNextGlobalEdgeEntryNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimNextGlobalEdgeEntryNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doNext(final PointersObject receiver, final PointersObject edgeEntry) {
            return method.image.b2d.primitiveNextGlobalEdgeEntry(receiver, edgeEntry);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveRegisterExternalEdge")
    protected abstract static class PrimRegisterExternalEdgeNode extends AbstractPrimitiveNode implements SeptenaryPrimitive {

        protected PrimRegisterExternalEdgeNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doRegister(final PointersObject receiver, final long index, final long initialX, final long initialY, final long initialZ, final long leftFillIndex,
                        final long rightFillIndex) {
            return method.image.b2d.primitiveRegisterExternalEdge(receiver, index, initialX, initialY, initialZ, leftFillIndex, rightFillIndex);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveRegisterExternalFill")
    protected abstract static class PrimRegisterExternalFillNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimRegisterExternalFillNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doRegister(final PointersObject receiver, final long index) {
            return method.image.b2d.primitiveRegisterExternalFill(receiver, index);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveRenderImage")
    protected abstract static class PrimRenderImageNode extends AbstractPrimitiveNode implements TernaryPrimitive {

        protected PrimRenderImageNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doRender(final PointersObject receiver, final PointersObject edge, final PointersObject fill) {
            return method.image.b2d.primitiveRenderImage(receiver, edge, fill);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveRenderScanline")
    protected abstract static class PrimRenderScanlineNode extends AbstractPrimitiveNode implements TernaryPrimitive {

        protected PrimRenderScanlineNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doRender(final PointersObject receiver, final PointersObject edge, final PointersObject fill) {
            return method.image.b2d.primitiveRenderScanline(receiver, edge, fill);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSetAALevel")
    protected abstract static class PrimSetAALevelNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimSetAALevelNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doSet(final PointersObject receiver, final long level) {
            return method.image.b2d.primitiveSetAALevel(receiver, level);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSetBitBltPlugin")
    protected abstract static class PrimSetBitBltPluginNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimSetBitBltPluginNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"pluginName.isByteType()"})
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected static final Object doSet(final ClassObject receiver, final NativeObject pluginName) {
            return B2D.primitiveSetBitBltPlugin(receiver, pluginName);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSetClipRect")
    protected abstract static class PrimSetClipRectNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimSetClipRectNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"rect.size() >= 2"})
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doSet(final PointersObject receiver, final PointersObject rect) {
            return method.image.b2d.primitiveSetClipRect(receiver, rect);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSetColorTransform")
    protected abstract static class PrimSetColorTransformNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimSetColorTransformNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doSet(final PointersObject receiver, final AbstractSqueakObject transform) {
            return method.image.b2d.primitiveSetColorTransform(receiver, transform);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSetDepth")
    protected abstract static class PrimSetDepthNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimSetDepthNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doSet(final PointersObject receiver, final long depth) {
            return method.image.b2d.primitiveSetDepth(receiver, depth);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSetEdgeTransform")
    protected abstract static class PrimSetEdgeTransformNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimSetEdgeTransformNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doSet(final PointersObject receiver, final AbstractSqueakObject transform) {
            return method.image.b2d.primitiveSetEdgeTransform(receiver, transform);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSetOffset")
    protected abstract static class PrimSetOffsetNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimSetOffsetNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"point.isPoint()"})
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doSet(final PointersObject receiver, final PointersObject point) {
            return method.image.b2d.primitiveSetOffset(receiver, point);
        }
    }
}
