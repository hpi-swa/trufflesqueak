/*
 * Copyright (c) 2017-2021 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.FORM;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodes.ArrayObjectToObjectArrayCopyNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveFallbacks.BinaryPrimitiveFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveFallbacks.SeptenaryPrimitiveFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveFallbacks.TernaryPrimitiveFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveFallbacks.UnaryPrimitiveFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;

public final class BitBltPlugin extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return BitBltPluginFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveCopyBits")
    protected abstract static class PrimCopyBits1Node extends AbstractPrimitiveNode implements UnaryPrimitiveFallback {
        @Specialization
        protected final Object doCopy(final PointersObject receiver,
                        @Cached final ConditionProfile resultProfile) {
            final long result = getContext().bitblt.primitiveCopyBits(receiver, -1);
            return resultProfile.profile(result == -1) ? receiver : result;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveCopyBits")
    protected abstract static class PrimCopyBits2Node extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization
        protected final Object doCopyTranslucent(final PointersObject receiver, final long factor,
                        @Cached final ConditionProfile resultProfile) {
            final long result = getContext().bitblt.primitiveCopyBits(receiver, factor);
            return resultProfile.profile(result == -1) ? receiver : result;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDisplayString")
    protected abstract static class PrimDisplayStringNode extends AbstractPrimitiveNode implements SeptenaryPrimitiveFallback {

        @Specialization(guards = {"startIndex >= 1", "stopIndex > 0", "aString.isByteType()", "aString.getByteLength() > 0", "stopIndex <= aString.getByteLength()"})
        protected final PointersObject doDisplayString(final PointersObject receiver, final NativeObject aString, final long startIndex, final long stopIndex,
                        final ArrayObject glyphMap, final ArrayObject xTable, final long kernDelta) {
            if (!glyphMap.isLongType()) {
                CompilerDirectives.transferToInterpreter();
                respecializeArrayToLongOrPrimFail(glyphMap);
            }
            if (glyphMap.getLongLength() != 256) {
                throw PrimitiveFailed.andTransferToInterpreter();
            }
            if (!xTable.isLongType()) {
                CompilerDirectives.transferToInterpreter();
                respecializeArrayToLongOrPrimFail(xTable);
            }
            getContext().bitblt.primitiveDisplayString(receiver, aString, startIndex, stopIndex, glyphMap.getLongStorage(), xTable.getLongStorage(), (int) kernDelta);
            return receiver;
        }

        private static void respecializeArrayToLongOrPrimFail(final ArrayObject array) {
            CompilerAsserts.neverPartOfCompilation();
            final Object[] values = ArrayObjectToObjectArrayCopyNode.getUncached().execute(array);
            final long[] longs = new long[values.length];
            try {
                for (int i = 0; i < values.length; i++) {
                    longs[i] = ((Number) values[i]).longValue();
                }
            } catch (final ClassCastException e) {
                throw PrimitiveFailed.GENERIC_ERROR;
            }
            array.setStorage(longs);
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
    protected abstract static class PrimDrawLoopNode extends AbstractPrimitiveNode implements TernaryPrimitiveFallback {

        @Specialization
        protected final Object doDrawLoop(final PointersObject receiver, final long xDelta, final long yDelta) {
            getContext().bitblt.primitiveDrawLoop(receiver, xDelta, yDelta);
            return receiver;
        }
    }

    @ImportStatic(FORM.class)
    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitivePixelValueAt")
    protected abstract static class PrimPixelValueAtNode extends AbstractPrimitiveNode implements TernaryPrimitiveFallback {

        @SuppressWarnings("unused")
        @Specialization(guards = {"xValue < 0 || yValue < 0"})
        protected static final long doQuickReturn(final PointersObject receiver, final long xValue, final long yValue) {
            return 0L;
        }

        @Specialization(guards = {"xValue >= 0", "yValue >= 0", "receiver.size() > OFFSET"})
        protected final long doValueAt(final PointersObject receiver, final long xValue, final long yValue) {
            return getContext().bitblt.primitivePixelValueAt(receiver, xValue, yValue);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveWarpBits")
    protected abstract static class PrimWarpBits1Node extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization
        protected final PointersObject doWarpBits(final PointersObject receiver, final long n) {
            getContext().bitblt.primitiveWarpBits(receiver, n, null);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveWarpBits")
    protected abstract static class PrimWarpBits2Node extends AbstractPrimitiveNode implements TernaryPrimitiveFallback {
        @Specialization
        protected final PointersObject doWarpBits(final PointersObject receiver, final long n, final NilObject nil) {
            return warpBits(receiver, n, nil);
        }

        @Specialization
        protected final PointersObject doWarpBits(final PointersObject receiver, final long n, final NativeObject sourceMap) {
            return warpBits(receiver, n, sourceMap);
        }

        private PointersObject warpBits(final PointersObject receiver, final long n, final AbstractSqueakObject sourceMap) {
            getContext().bitblt.primitiveWarpBits(receiver, n, sourceMap);
            return receiver;
        }
    }
}
