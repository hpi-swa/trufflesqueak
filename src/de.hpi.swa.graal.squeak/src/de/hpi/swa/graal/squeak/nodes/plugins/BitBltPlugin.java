package de.hpi.swa.graal.squeak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.NotProvided;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.FORM;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;

public final class BitBltPlugin extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return BitBltPluginFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveCopyBits")
    protected abstract static class PrimCopyBitsNode extends AbstractPrimitiveNode {

        protected PrimCopyBitsNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected static final Object doCopy(final PointersObject receiver, @SuppressWarnings("unused") final NotProvided notProvided) {
            BitBlt.resetSuccessFlag();
            return BitBlt.primitiveCopyBits(receiver, -1); // Not provided represented by `-1` here.
        }

        @Specialization
        protected static final Object doCopyTranslucent(final PointersObject receiver, final long factor) {
            BitBlt.resetSuccessFlag();
            return BitBlt.primitiveCopyBits(receiver, factor);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveDisplayString")
    protected abstract static class PrimDisplayStringNode extends AbstractPrimitiveNode {

        protected PrimDisplayStringNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = {"startIndex >= 1", "stopIndex >= 0", "aString.isByteType()", "aString.getByteLength() > 0",
                        "stopIndex <= aString.getByteLength()"})
        protected static final Object doDisplay(final PointersObject receiver, final NativeObject aString, final long startIndex, final long stopIndex,
                        final ArrayObject glyphMap, final ArrayObject xTable, final long kernDelta) {
            BitBlt.resetSuccessFlag();
            return BitBlt.primitiveDisplayString(receiver, aString, startIndex, stopIndex, glyphMap, xTable, (int) kernDelta);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"aString.isByteType()", "aString.getByteLength() == 0"})
        protected static final Object doNothing(final PointersObject receiver, final NativeObject aString, final long startIndex, final long stopIndex, final ArrayObject glyphMap,
                        final ArrayObject xTable, final long kernDelta) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveDrawLoop")
    protected abstract static class PrimDrawLoopNode extends AbstractPrimitiveNode {

        protected PrimDrawLoopNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected static final Object doDrawLoop(final PointersObject receiver, final long xDelta, final long yDelta) {
            BitBlt.resetSuccessFlag();
            return BitBlt.primitiveDrawLoop(receiver, xDelta, yDelta);
        }
    }

    @ImportStatic(FORM.class)
    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitivePixelValueAt")
    protected abstract static class PrimPixelValueAtNode extends AbstractPrimitiveNode {

        public PrimPixelValueAtNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"xValue < 0 || yValue < 0"})
        protected static final long doQuickReturn(final PointersObject receiver, final long xValue, final long yValue) {
            return 0L;
        }

        @Specialization(guards = {"xValue >= 0", "yValue >= 0", "receiver.size() > OFFSET"})
        protected static final long doValueAt(final PointersObject receiver, final long xValue, final long yValue) {
            BitBlt.resetSuccessFlag();
            return BitBlt.primitivePixelValueAt(receiver, xValue, yValue);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveWarpBits")
    protected abstract static class PrimWarpBitsNode extends AbstractPrimitiveNode {

        public PrimWarpBitsNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected static final Object doValueAt(final PointersObject receiver, final long n, @SuppressWarnings("unused") final NotProvided notProvided) {
            BitBlt.resetSuccessFlag();
            return BitBlt.primitiveWarpBits(receiver, n, null);
        }

        @Specialization
        protected static final Object doValueAt(final PointersObject receiver, final long n, final NilObject nil) {
            BitBlt.resetSuccessFlag();
            return BitBlt.primitiveWarpBits(receiver, n, nil);
        }

        @Specialization
        protected static final Object doValueAt(final PointersObject receiver, final long n, final NativeObject sourceMap) {
            BitBlt.resetSuccessFlag();
            return BitBlt.primitiveWarpBits(receiver, n, sourceMap);
        }
    }
}
