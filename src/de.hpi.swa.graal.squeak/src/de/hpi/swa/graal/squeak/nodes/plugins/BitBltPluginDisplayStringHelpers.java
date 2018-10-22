package de.hpi.swa.graal.squeak.nodes.plugins;

import java.util.function.LongBinaryOperator;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithCode;
import de.hpi.swa.graal.squeak.nodes.plugins.BitBltPluginDisplayStringHelpersFactory.DisplayStringHelperNodeGen;
import de.hpi.swa.graal.squeak.nodes.plugins.BitBltPluginDisplayStringHelpersFactory.ExtractFormsAndContinueNodeGen;
import de.hpi.swa.graal.squeak.nodes.plugins.BitBltPluginHelpers.BitBltWrapper;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.SimulationPrimitiveNode;

public final class BitBltPluginDisplayStringHelpers {
    protected abstract static class ExtractFormsAndContinueNode extends AbstractNodeWithCode {
        @Child private SimulationPrimitiveNode simulateNode;

        public static ExtractFormsAndContinueNode create(final CompiledCodeObject code) {
            return ExtractFormsAndContinueNodeGen.create(code);
        }

        protected ExtractFormsAndContinueNode(final CompiledCodeObject code) {
            super(code);
        }

        protected abstract Object execute(VirtualFrame frame, BitBltWrapper receiver, NativeObject aString, long startIndex, long stopIndex,
                        ArrayObject glyphMap, ArrayObject xTable, long kernDelta, Object combinationRule, Object sourceForm, Object destForm);

        @Specialization(guards = {"isSupported(combinationRule)"})
        protected static final Object doOptimized(final BitBltWrapper receiver, final NativeObject aString, final long startIndex, final long stopIndex, final ArrayObject glyphMap,
                        final ArrayObject xTable, final long kernDelta, final long combinationRule, final PointersObject sourceForm, final PointersObject destForm,
                        @Cached("create(code)") final DisplayStringHelperNode node) {
            node.execute(receiver, aString, startIndex, stopIndex, glyphMap, xTable, kernDelta, combinationRule, sourceForm, destForm);
            return receiver;
        }

        @SuppressWarnings("unused")
        @Fallback
        protected final Object doSimulation(final VirtualFrame frame, final BitBltWrapper receiver, final NativeObject aString, final long startIndex, final long stopIndex,
                        final ArrayObject glyphMap, final ArrayObject xTable, final long kernDelta, final Object combinationRule, final Object sourceForm, final Object destForm) {
            return getSimulationPrimitiveNode().executeWithArguments(frame, receiver.getBitBlt(), aString, startIndex, stopIndex, glyphMap, xTable, kernDelta);
        }

        private SimulationPrimitiveNode getSimulationPrimitiveNode() {
            if (simulateNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                simulateNode = insert(SimulationPrimitiveNode.create((CompiledMethodObject) code, "BitBltPlugin", "primitiveDisplayString"));
            }
            return simulateNode;
        }

        protected static final boolean isSupported(final long combinationRule) {
            // TODO: finish implementation for combiRule 20 and 37
            return combinationRule == -20 || combinationRule == -37;
        }
    }

    protected abstract static class DisplayStringHelperNode extends AbstractNodeWithCode {

        public static DisplayStringHelperNode create(final CompiledCodeObject code) {
            return DisplayStringHelperNodeGen.create(code);
        }

        protected DisplayStringHelperNode(final CompiledCodeObject code) {
            super(code);
        }

        protected abstract void execute(BitBltWrapper receiver, NativeObject aString, long startIndex, long stopIndex,
                        ArrayObject glyphMap, ArrayObject xTable, long kernDelta, Object combinationRule, Object sourceForm, Object destForm);

        @Specialization(guards = {"combinationRule == 20"})
        protected final void doOptimized(final BitBltWrapper receiver, final NativeObject aString, final long startIndex, final long stopIndex, final ArrayObject glyphMap,
                        final ArrayObject xTable, final long kernDelta, @SuppressWarnings("unused") final long combinationRule, final PointersObject sourceForm, final PointersObject destForm) {
            doLoop(BitBltPluginHelpers::rgbAdd20, receiver, aString, startIndex, stopIndex, glyphMap, xTable, kernDelta, sourceForm, destForm);
        }

        @Specialization(guards = {"combinationRule == 37"})
        protected final void doCombiRule37(final BitBltWrapper receiver, final NativeObject aString, final long startIndex, final long stopIndex, final ArrayObject glyphMap,
                        final ArrayObject xTable, final long kernDelta, @SuppressWarnings("unused") final long combinationRule, final PointersObject sourceForm, final PointersObject destForm) {
            doLoop(BitBltPluginHelpers::rgbMul37, receiver, aString, startIndex, stopIndex, glyphMap, xTable, kernDelta, sourceForm, destForm);
        }

        @SuppressWarnings("unused")
        protected final void doLoop(final LongBinaryOperator op, final BitBltWrapper receiver, final NativeObject aString, final long startIndex, final long stopIndex, final ArrayObject glyphMap,
                        final ArrayObject xTable, final long kernDelta, final PointersObject sourceForm, final PointersObject destForm) {
            // TODO: do something here
        }
    }

    @Fallback
    protected static void doFail(final PointersObject receiver, final NativeObject aString, final long startIndex, final long stopIndex,
                    final ArrayObject glyphMap, final ArrayObject xTable, final Object combinationRule, final Object sourceForm, final Object destForm) {
        throw new SqueakException("Unsupported operation reached:", receiver, aString, startIndex, stopIndex, glyphMap, xTable, combinationRule, sourceForm, destForm);
    }
}
