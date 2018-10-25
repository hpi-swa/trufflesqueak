package de.hpi.swa.graal.squeak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.BIT_BLT;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.FORM;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.plugins.BitBltPluginCopyBitsHelpers.CopyBitsEnsureDepthAndExecuteHelperNode;
import de.hpi.swa.graal.squeak.nodes.plugins.BitBltPluginDisplayStringHelpers.ExtractFormsAndContinueNode;
import de.hpi.swa.graal.squeak.nodes.plugins.BitBltPluginHelpers.BitBltWrapper;
import de.hpi.swa.graal.squeak.nodes.plugins.BitBltPluginPixelValueAtHelpers.PixelValueAtExtractHelperNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;

public final class BitBltPlugin extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return BitBltPluginFactory.getFactories();
    }

    @Override
    public boolean useSimulationAsFallback() {
        return true;
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveCopyBits")
    protected abstract static class PrimCopyBitsNode extends AbstractPrimitiveNode {
        @Child private CopyBitsEnsureDepthAndExecuteHelperNode executeNode;

        protected PrimCopyBitsNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
            executeNode = CopyBitsEnsureDepthAndExecuteHelperNode.create(method);
        }

        @Specialization
        protected final Object doOptimized(final VirtualFrame frame, final PointersObject receiver) {
            return executeNode.executeExtract(frame, receiver, receiver.at0(BIT_BLT.SOURCE_FORM), receiver.at0(BIT_BLT.DEST_FORM));
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
        protected static final Object doOptimized(final VirtualFrame frame, final PointersObject receiver, final NativeObject aString, final long startIndex, final long stopIndex,
                        final ArrayObject glyphMap, final ArrayObject xTable, final long kernDelta,
                        @Cached("create(code)") final ExtractFormsAndContinueNode executeNode) {
            return executeNode.execute(frame, new BitBltWrapper(receiver), aString, startIndex, stopIndex, glyphMap, xTable, kernDelta,
                            receiver.at0(BIT_BLT.COMBINATION_RULE), receiver.at0(BIT_BLT.SOURCE_FORM), receiver.at0(BIT_BLT.DEST_FORM));
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"aString.isByteType()", "aString.getByteLength() == 0"})
        protected static final Object doNothing(final PointersObject receiver, final NativeObject aString, final long startIndex, final long stopIndex, final ArrayObject glyphMap,
                        final ArrayObject xTable, final long kernDelta) {
            return receiver;
        }
    }

    @ImportStatic(FORM.class)
    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitivePixelValueAt")
    protected abstract static class PrimPixelValueAtNode extends AbstractPrimitiveNode {
        @Child private PixelValueAtExtractHelperNode handleNode = PixelValueAtExtractHelperNode.create();

        public PrimPixelValueAtNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"xValue < 0 || yValue < 0"})
        protected static final long doQuickReturn(final PointersObject receiver, final long xValue, final long yValue) {
            return 0L;
        }

        @Specialization(guards = {"xValue >= 0", "yValue >= 0", "receiver.size() > OFFSET"})
        protected final long doValueAt(final PointersObject receiver, final long xValue, final long yValue) {
            return handleNode.executeValueAt(receiver, xValue, yValue, receiver.at0(FORM.BITS));
        }
    }
}
