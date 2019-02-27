package de.hpi.swa.graal.squeak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;

import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.NotProvided;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.FORM;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.SeptenaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;

public final class BitBltPlugin extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return BitBltPluginFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveCopyBits")
    protected abstract static class PrimCopyBitsNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        private final ValueProfile combinationRuleProfile = ValueProfile.createIdentityProfile();
        private final ConditionProfile noSourceProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile copyLoopPixMapProfile = ConditionProfile.createBinaryProfile();

        protected PrimCopyBitsNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final Object doCopy(final PointersObject receiver, @SuppressWarnings("unused") final NotProvided notProvided) {
            // Not provided represented by `-1` here.
            return BitBlt.primitiveCopyBits(receiver, -1, combinationRuleProfile, noSourceProfile, copyLoopPixMapProfile);
        }

        @Specialization
        protected final Object doCopyTranslucent(final PointersObject receiver, final long factor) {
            return BitBlt.primitiveCopyBits(receiver, factor, combinationRuleProfile, noSourceProfile, copyLoopPixMapProfile);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDisplayString")
    protected abstract static class PrimDisplayStringNode extends AbstractPrimitiveNode implements SeptenaryPrimitive {

        protected PrimDisplayStringNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"startIndex > 0", "stopIndex >= 0", "aString.isByteType()", "aString.getByteLength() > 0",
                        "stopIndex <= aString.getByteLength()", "glyphMap.isLongType()", "glyphMap.getLongLength() == 256"})
        protected static final PointersObject doDisplay(final PointersObject receiver, final NativeObject aString, final long startIndex, final long stopIndex,
                        final ArrayObject glyphMap, final ArrayObject xTable, final long kernDelta,
                        @Cached("createBinaryProfile()") final ConditionProfile quickBltProfile,
                        @Cached("createIdentityProfile()") final ValueProfile combinationRuleProfile,
                        @Cached("createBinaryProfile()") final ConditionProfile noSourceProfile,
                        @Cached("createBinaryProfile()") final ConditionProfile copyLoopPixMapProfile) {
            return BitBlt.primitiveDisplayString(receiver, aString, startIndex, stopIndex, glyphMap, xTable, (int) kernDelta, quickBltProfile, combinationRuleProfile, noSourceProfile,
                            copyLoopPixMapProfile);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"aString.isByteType()", "aString.getByteLength() == 0"})
        protected static final Object doNothing(final PointersObject receiver, final NativeObject aString, final long startIndex, final long stopIndex, final ArrayObject glyphMap,
                        final ArrayObject xTable, final long kernDelta) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDrawLoop")
    protected abstract static class PrimDrawLoopNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        private final ValueProfile combinationRuleProfile = ValueProfile.createIdentityProfile();
        private final ConditionProfile noSourceProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile copyLoopPixMapProfile = ConditionProfile.createBinaryProfile();

        protected PrimDrawLoopNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final PointersObject doDrawLoop(final PointersObject receiver, final long xDelta, final long yDelta) {
            return BitBlt.primitiveDrawLoop(receiver, xDelta, yDelta, combinationRuleProfile, noSourceProfile, copyLoopPixMapProfile);
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
        protected static final long doValueAt(final PointersObject receiver, final long xValue, final long yValue) {
            return BitBlt.primitivePixelValueAt(receiver, xValue, yValue);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveWarpBits")
    protected abstract static class PrimWarpBitsNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        private final ValueProfile combinationRuleProfile = ValueProfile.createIdentityProfile();

        public PrimWarpBitsNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final PointersObject doValueAt(final PointersObject receiver, final long n, @SuppressWarnings("unused") final NotProvided notProvided) {
            return BitBlt.primitiveWarpBits(receiver, n, null, combinationRuleProfile);
        }

        @Specialization
        protected final PointersObject doValueAt(final PointersObject receiver, final long n, final NilObject nil) {
            return BitBlt.primitiveWarpBits(receiver, n, nil, combinationRuleProfile);
        }

        @Specialization
        protected final PointersObject doValueAt(final PointersObject receiver, final long n, final NativeObject sourceMap) {
            return BitBlt.primitiveWarpBits(receiver, n, sourceMap, combinationRuleProfile);
        }
    }
}
