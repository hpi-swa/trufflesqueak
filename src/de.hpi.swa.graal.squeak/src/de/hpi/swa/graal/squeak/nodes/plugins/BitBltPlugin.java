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
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.FORM;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectSizeNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectToObjectArrayCopyNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.SeptenaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitive;
import de.hpi.swa.graal.squeak.util.NotProvided;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;

public final class BitBltPlugin extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return BitBltPluginFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveCopyBits")
    protected abstract static class PrimCopyBitsNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimCopyBitsNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doCopy(final PointersObject receiver, @SuppressWarnings("unused") final NotProvided notProvided) {
            method.image.bitblt.resetSuccessFlag();
            // Not provided represented by `-1L` factor.
            return method.image.bitblt.primitiveCopyBits(receiver, -1L);
        }

        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doCopyTranslucent(final PointersObject receiver, final long factor) {
            method.image.bitblt.resetSuccessFlag();
            return method.image.bitblt.primitiveCopyBits(receiver, factor);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDisplayString")
    protected abstract static class PrimDisplayStringNode extends AbstractPrimitiveNode implements SeptenaryPrimitive {

        protected PrimDisplayStringNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"startIndex >= 1", "stopIndex > 0", "aString.isByteType()", "aString.getByteLength() > 0",
                        "stopIndex <= aString.getByteLength()", "glyphMap.isLongType()", "glyphMap.getLongLength() == 256", "xTable.isLongType()"})
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doDisplayLongArrays(final PointersObject receiver, final NativeObject aString, final long startIndex, final long stopIndex,
                        final ArrayObject glyphMap, final ArrayObject xTable, final long kernDelta) {
            method.image.bitblt.resetSuccessFlag();
            return method.image.bitblt.primitiveDisplayString(receiver, aString, startIndex, stopIndex, glyphMap.getLongStorage(), xTable.getLongStorage(), (int) kernDelta);
        }

        @Specialization(guards = {"startIndex >= 1", "stopIndex > 0", "aString.isByteType()", "aString.getByteLength() > 0",
                        "stopIndex <= aString.getByteLength()", "!glyphMap.isLongType() || !xTable.isLongType()", "sizeNode.execute(glyphMap) == 256"}, limit = "1")
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doDisplayGeneric(final PointersObject receiver, final NativeObject aString, final long startIndex, final long stopIndex,
                        final ArrayObject glyphMap, final ArrayObject xTable, final long kernDelta,
                        @SuppressWarnings("unused") @Cached final ArrayObjectSizeNode sizeNode,
                        @Cached final ArrayObjectToObjectArrayCopyNode toObjectArrayNode) {
            method.image.bitblt.resetSuccessFlag();
            final long[] glyphMapValues = toLongArray(toObjectArrayNode.execute(glyphMap));
            glyphMap.setStorage(glyphMapValues); // re-specialize.
            final long[] xTableValues = toLongArray(toObjectArrayNode.execute(xTable));
            xTable.setStorage(xTableValues); // re-specialize.
            return method.image.bitblt.primitiveDisplayString(receiver, aString, startIndex, stopIndex, glyphMapValues, xTableValues, (int) kernDelta);
        }

        // TODO: Find good replacement for `toLongArray`.
        private static long[] toLongArray(final Object[] values) {
            final long[] longs = new long[values.length];
            try {
                for (int i = 0; i < values.length; i++) {
                    longs[i] = ((Number) values[i]).longValue();
                }
            } catch (final ClassCastException e) {
                throw PrimitiveFailed.andTransferToInterpreter();
            }
            return longs;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"aString.isByteType()", "aString.getByteLength() == 0 || stopIndex == 0"})
        protected static final Object doNothing(final PointersObject receiver, final NativeObject aString, final long startIndex, final long stopIndex, final ArrayObject glyphMap,
                        final ArrayObject xTable, final long kernDelta) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDrawLoop")
    protected abstract static class PrimDrawLoopNode extends AbstractPrimitiveNode implements TernaryPrimitive {

        protected PrimDrawLoopNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doDrawLoop(final PointersObject receiver, final long xDelta, final long yDelta) {
            method.image.bitblt.resetSuccessFlag();
            return method.image.bitblt.primitiveDrawLoop(receiver, xDelta, yDelta);
        }
    }

    @ImportStatic(FORM.class)
    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitivePixelValueAt")
    protected abstract static class PrimPixelValueAtNode extends AbstractPrimitiveNode implements TernaryPrimitive {

        public PrimPixelValueAtNode(final CompiledMethodObject method) {
            super(method);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"xValue < 0 || yValue < 0"})
        protected static final long doQuickReturn(final PointersObject receiver, final long xValue, final long yValue) {
            return 0L;
        }

        @Specialization(guards = {"xValue >= 0", "yValue >= 0", "receiver.size() > OFFSET"})
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final long doValueAt(final PointersObject receiver, final long xValue, final long yValue) {
            method.image.bitblt.resetSuccessFlag();
            return method.image.bitblt.primitivePixelValueAt(receiver, xValue, yValue);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveWarpBits")
    protected abstract static class PrimWarpBitsNode extends AbstractPrimitiveNode implements TernaryPrimitive {

        public PrimWarpBitsNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doValueAt(final PointersObject receiver, final long n, @SuppressWarnings("unused") final NotProvided notProvided) {
            method.image.bitblt.resetSuccessFlag();
            return method.image.bitblt.primitiveWarpBits(receiver, n, null);
        }

        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doValueAt(final PointersObject receiver, final long n, final NilObject nil) {
            method.image.bitblt.resetSuccessFlag();
            return method.image.bitblt.primitiveWarpBits(receiver, n, nil);
        }

        @Specialization
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doValueAt(final PointersObject receiver, final long n, final NativeObject sourceMap) {
            method.image.bitblt.resetSuccessFlag();
            return method.image.bitblt.primitiveWarpBits(receiver, n, sourceMap);
        }
    }
}
