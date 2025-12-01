/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.util.Arrays;
import java.util.List;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DenyReplace;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.image.SqueakImageConstants;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.io.SqueakDisplay;
import de.hpi.swa.trufflesqueak.model.AbstractPointersObject;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObjectWithClassAndHash;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.EphemeronObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.VariablePointersObject;
import de.hpi.swa.trufflesqueak.model.WeakVariablePointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.SPECIAL_OBJECT;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectAtPut0Node;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectIdentityNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectShallowCopyNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectSizeNode;
import de.hpi.swa.trufflesqueak.nodes.plugins.LargeIntegers;
import de.hpi.swa.trufflesqueak.nodes.plugins.MiscPrimitivePlugin.AbstractPrimCompareStringNode;
import de.hpi.swa.trufflesqueak.nodes.plugins.SqueakFFIPrims.AbstractFFIPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractSingletonPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive0;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive0WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive10WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive11WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive2WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive3WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive4WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive5WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive6WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive7WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive8WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive9WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeFactory;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.MiscUtils;
import de.hpi.swa.trufflesqueak.util.UnsafeUtils;
import sun.misc.Unsafe;

public final class MiscellaneousPrimitives extends AbstractPrimitiveFactoryHolder {

    private abstract static class AbstractSignalAtPrimitiveNode extends AbstractPrimitiveNode {

        protected final void signalAtMilliseconds(final PointersObject semaphore, final long msTime) {
            final SqueakImageContext image = getContext();
            image.setSemaphore(SPECIAL_OBJECT.THE_TIMER_SEMAPHORE, semaphore);
            image.interrupt.setNextWakeupTick(msTime);
        }

        protected final void resetTimerSemaphore() {
            final SqueakImageContext image = getContext();
            image.setSemaphore(SPECIAL_OBJECT.THE_TIMER_SEMAPHORE, NilObject.SINGLETON);
            image.interrupt.setNextWakeupTick(0);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 77)
    protected abstract static class PrimSomeInstanceNode extends AbstractPrimitiveNode implements Primitive0WithFallback {

        @SuppressWarnings("unused")
        @Specialization(guards = "classObject.isImmediateClassType()")
        protected static final Object doImmediate(final ClassObject classObject) {
            throw PrimitiveFailed.GENERIC_ERROR;
        }

        @Specialization(guards = "!classObject.isImmediateClassType()")
        protected final AbstractSqueakObject doSomeInstance(final ClassObject classObject) {
            if (classObject.getSqueakHashInt() == SqueakImageConstants.FREE_OBJECT_CLASS_INDEX_PUN) {
                return NilObject.SINGLETON; // Class has not been instantiated yet
            }
            return getContext().objectGraphUtils.someInstanceOf(classObject);
        }
    }

    public abstract static class AbstractPrimCalloutToFFINode extends AbstractFFIPrimitiveNode {
        @CompilationFinal private PointersObject externalFunction;

        @Override
        public final boolean acceptsMethod(final CompiledCodeObject method) {
            CompilerAsserts.neverPartOfCompilation();
            if (method.getNumLiterals() > 0) {
                final Object firstLiteral = method.getLiteral(0);
                if (firstLiteral instanceof final PointersObject l1 && l1.getSqueakClass().includesExternalFunctionBehavior(getContext())) {
                    externalFunction = l1;
                    return true;
                }
            }
            return false;
        }

        protected final Object doCallout(final AbstractSqueakObject receiver, final Object... arguments) {
            return doCallout(externalFunction, receiver, arguments);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 120)
    protected abstract static class PrimCalloutToFFI1Node extends AbstractPrimCalloutToFFINode implements Primitive0WithFallback {
        @Specialization
        protected final Object doArg0(final AbstractSqueakObject receiver) {
            return doCallout(receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 120)
    protected abstract static class PrimCalloutToFFI2Node extends AbstractPrimCalloutToFFINode implements Primitive1WithFallback {
        @Specialization
        protected final Object doArg1(final AbstractSqueakObject receiver, final Object arg1) {
            return doCallout(receiver, arg1);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 120)
    protected abstract static class PrimCalloutToFFI3Node extends AbstractPrimCalloutToFFINode implements Primitive2WithFallback {
        @Specialization
        protected final Object doArg2(final AbstractSqueakObject receiver, final Object arg1, final Object arg2) {
            return doCallout(receiver, arg1, arg2);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 120)
    protected abstract static class PrimCalloutToFFI4Node extends AbstractPrimCalloutToFFINode implements Primitive3WithFallback {
        @Specialization
        protected final Object doArg3(final AbstractSqueakObject receiver, final Object arg1, final Object arg2, final Object arg3) {
            return doCallout(receiver, arg1, arg2, arg3);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 120)
    protected abstract static class PrimCalloutToFFI5Node extends AbstractPrimCalloutToFFINode implements Primitive4WithFallback {
        @Specialization
        protected final Object doArg3(final AbstractSqueakObject receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4) {
            return doCallout(receiver, arg1, arg2, arg3, arg4);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 120)
    protected abstract static class PrimCalloutToFFI6Node extends AbstractPrimCalloutToFFINode implements Primitive5WithFallback {
        @Specialization
        protected final Object doArg5(final AbstractSqueakObject receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5) {
            return doCallout(receiver, arg1, arg2, arg3, arg4, arg5);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 120)
    protected abstract static class PrimCalloutToFFI7Node extends AbstractPrimCalloutToFFINode implements Primitive6WithFallback {
        @Specialization
        protected final Object doArg6(final AbstractSqueakObject receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6) {
            return doCallout(receiver, arg1, arg2, arg3, arg4, arg5, arg6);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 120)
    protected abstract static class PrimCalloutToFFI8Node extends AbstractPrimCalloutToFFINode implements Primitive7WithFallback {
        @Specialization
        protected final Object doArg7(final AbstractSqueakObject receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6,
                        final Object arg7) {
            return doCallout(receiver, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 120)
    protected abstract static class PrimCalloutToFFI9Node extends AbstractPrimCalloutToFFINode implements Primitive8WithFallback {
        @Specialization
        protected final Object doArg8(final AbstractSqueakObject receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6,
                        final Object arg7, final Object arg8) {
            return doCallout(receiver, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 120)
    protected abstract static class PrimCalloutToFFI10Node extends AbstractPrimCalloutToFFINode implements Primitive9WithFallback {
        @Specialization
        protected final Object doArg9(final AbstractSqueakObject receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6,
                        final Object arg7, final Object arg8, final Object arg9) {
            return doCallout(receiver, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 120)
    protected abstract static class PrimCalloutToFFI11Node extends AbstractPrimCalloutToFFINode implements Primitive10WithFallback {
        @Specialization
        protected final Object doArg10(final AbstractSqueakObject receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6,
                        final Object arg7, final Object arg8, final Object arg9, final Object arg10) {
            return doCallout(receiver, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 120)
    protected abstract static class PrimCalloutToFFI12Node extends AbstractPrimCalloutToFFINode implements Primitive11WithFallback {
        @Specialization
        protected final Object doArg11(final AbstractSqueakObject receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6,
                        final Object arg7, final Object arg8, final Object arg9, final Object arg10, final Object arg11) {
            return doCallout(receiver, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 121)
    protected abstract static class PrimImageName1Node extends AbstractPrimitiveNode implements Primitive0 {
        @Specialization
        protected final NativeObject doGetName(@SuppressWarnings("unused") final Object receiver) {
            final SqueakImageContext image = getContext();
            return image.asByteString(image.getImagePath());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 121)
    protected abstract static class PrimImageName2Node extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "newName.isByteType()")
        protected final NativeObject doSetName(@SuppressWarnings("unused") final Object receiver, final NativeObject newName) {
            getContext().setImagePath(newName.asStringUnsafe());
            return newName;
        }
    }

    @DenyReplace
    @SqueakPrimitive(indices = 122)
    public static final class PrimNoopNode extends AbstractSingletonPrimitiveNode implements Primitive0 {
        @Override
        public Object execute(final VirtualFrame frame, final Object receiver) {
            return receiver;
        }
    }

    @DenyReplace
    @SqueakPrimitive(indices = 124)
    private static final class PrimLowSpaceSemaphoreNode extends AbstractSingletonPrimitiveNode implements Primitive1 {
        @Override
        public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1) {
            final AbstractSqueakObject semaphoreOrNil;
            if (arg1 instanceof PointersObject pointersObject && isSemaphore(pointersObject)) {
                semaphoreOrNil = pointersObject;
            } else {
                semaphoreOrNil = NilObject.SINGLETON;
            }
            getContext().setSemaphore(SPECIAL_OBJECT.THE_LOW_SPACE_SEMAPHORE, semaphoreOrNil);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 125)
    protected abstract static class PrimSetLowSpaceThresholdNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization
        protected static final Object doSet(final Object receiver, @SuppressWarnings("unused") final long numBytes) {
            // TODO: do something with numBytes
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 132)
    protected abstract static class PrimObjectPointsToNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization
        protected static final boolean doClass(final ClassObject receiver, final Object thang) {
            return BooleanObject.wrap(receiver.pointsTo(thang));
        }

        @Specialization
        protected static final boolean doCode(final CompiledCodeObject receiver, final Object thang) {
            return BooleanObject.wrap(receiver.pointsTo(thang));
        }

        @Specialization
        protected static final boolean doContext(final ContextObject receiver, final Object thang) {
            return BooleanObject.wrap(receiver.pointsTo(thang));
        }

        @Specialization(guards = {"receiver.isEmptyType()"})
        protected static final boolean doEmptyArray(final ArrayObject receiver, final Object thang,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile noElementsProfile) {
            if (noElementsProfile.profile(node, receiver.getEmptyStorage() == 0)) {
                return BooleanObject.FALSE;
            } else {
                return BooleanObject.wrap(thang == NilObject.SINGLETON);
            }
        }

        @Specialization(guards = "receiver.isBooleanType()")
        protected static final boolean doArrayOfBooleans(final ArrayObject receiver, final boolean thang) {
            return BooleanObject.wrap(ArrayUtils.contains(receiver.getBooleanStorage(), thang ? ArrayObject.BOOLEAN_TRUE_TAG : ArrayObject.BOOLEAN_FALSE_TAG));
        }

        @Specialization(guards = "receiver.isBooleanType()")
        protected static final boolean doArrayOfBooleans(final ArrayObject receiver, @SuppressWarnings("unused") final NilObject thang) {
            return BooleanObject.wrap(ArrayUtils.contains(receiver.getBooleanStorage(), ArrayObject.BOOLEAN_NIL_TAG));
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"receiver.isBooleanType()", "!isBoolean(thang)", "!isNil(thang)"})
        protected static final boolean doArrayOfBooleans(final ArrayObject receiver, final Object thang) {
            return BooleanObject.FALSE;
        }

        @Specialization(guards = "receiver.isCharType()")
        protected static final boolean doArrayOfChars(final ArrayObject receiver, final char thang) {
            return BooleanObject.wrap(ArrayUtils.contains(receiver.getCharStorage(), thang));
        }

        @Specialization(guards = "receiver.isCharType()")
        protected static final boolean doArrayOfChars(final ArrayObject receiver, @SuppressWarnings("unused") final NilObject thang) {
            return BooleanObject.wrap(ArrayUtils.contains(receiver.getCharStorage(), ArrayObject.CHAR_NIL_TAG));
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"receiver.isCharType()", "!isCharacter(thang)", "!isNil(thang)"})
        protected static final boolean doArrayOfChars(final ArrayObject receiver, final Object thang) {
            return BooleanObject.FALSE;
        }

        @Specialization(guards = "receiver.isLongType()")
        protected static final boolean doArrayOfLongs(final ArrayObject receiver, final long thang) {
            return BooleanObject.wrap(ArrayUtils.contains(receiver.getLongStorage(), thang));
        }

        @Specialization(guards = "receiver.isLongType()")
        protected static final boolean doArrayOfLongs(final ArrayObject receiver, @SuppressWarnings("unused") final NilObject thang) {
            return BooleanObject.wrap(ArrayUtils.contains(receiver.getLongStorage(), ArrayObject.LONG_NIL_TAG));
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"receiver.isLongType()", "!isLong(thang)", "!isNil(thang)"})
        protected static final boolean doArrayOfLongss(final ArrayObject receiver, final Object thang) {
            return BooleanObject.FALSE;
        }

        @Specialization(guards = "receiver.isDoubleType()")
        protected static final boolean doArrayOfDoubles(final ArrayObject receiver, final double thang) {
            return BooleanObject.wrap(ArrayUtils.contains(receiver.getDoubleStorage(), thang));
        }

        @Specialization(guards = "receiver.isDoubleType()")
        protected static final boolean doArrayOfDoubles(final ArrayObject receiver, @SuppressWarnings("unused") final NilObject thang) {
            return BooleanObject.wrap(ArrayUtils.contains(receiver.getDoubleStorage(), ArrayObject.DOUBLE_NIL_TAG));
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"receiver.isDoubleType()", "!isDouble(thang)", "!isNil(thang)"})
        protected static final boolean doArrayOfDoubles(final ArrayObject receiver, final Object thang) {
            return BooleanObject.FALSE;
        }

        @Specialization(guards = "receiver.isObjectType()")
        protected static final boolean doArrayOfObjects(final ArrayObject receiver, final Object thang) {
            return BooleanObject.wrap(ArrayUtils.contains(receiver.getObjectStorage(), thang));
        }

        @Specialization
        protected static final boolean doPointers(final PointersObject receiver, final Object thang,
                        @Bind final Node node,
                        @Shared("identityNode") @Cached final SqueakObjectIdentityNode identityNode) {
            return BooleanObject.wrap(receiver.pointsTo(identityNode, node, thang));
        }

        @Specialization
        protected static final boolean doVariablePointers(final VariablePointersObject receiver, final Object thang,
                        @Bind final Node node,
                        @Shared("identityNode") @Cached final SqueakObjectIdentityNode identityNode) {
            return BooleanObject.wrap(receiver.pointsTo(identityNode, node, thang));
        }

        @Specialization
        protected static final boolean doWeakPointers(final WeakVariablePointersObject receiver, final Object thang,
                        @Bind final Node node,
                        @Shared("identityNode") @Cached final SqueakObjectIdentityNode identityNode) {
            return BooleanObject.wrap(receiver.pointsTo(identityNode, node, thang));
        }

        @Specialization
        protected static final boolean doEphemeron(final EphemeronObject receiver, final Object thang,
                        @Bind final Node node,
                        @Shared("identityNode") @Cached final SqueakObjectIdentityNode identityNode) {
            return BooleanObject.wrap(receiver.pointsTo(identityNode, node, thang));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 134)
    protected abstract static class PrimInterruptSemaphoreNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization
        protected final Object set(final Object receiver, final PointersObject semaphore) {
            final SqueakImageContext image = getContext();
            image.setSemaphore(SPECIAL_OBJECT.THE_INTERRUPT_SEMAPHORE, semaphore);
            return receiver;
        }

        @Specialization
        protected final Object set(final Object receiver, final NilObject semaphore) {
            final SqueakImageContext image = getContext();
            image.setSemaphore(SPECIAL_OBJECT.THE_INTERRUPT_SEMAPHORE, semaphore);
            return receiver;
        }
    }

    @DenyReplace
    @SqueakPrimitive(indices = 135)
    protected static final class PrimMillisecondClockNode extends AbstractSingletonPrimitiveNode implements Primitive0 {
        @Override
        public Object execute(final VirtualFrame frame, final Object receiver) {
            return MiscUtils.currentTimeMillis() - getContext().startUpMillis;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 136)
    protected abstract static class PrimSignalAtMillisecondsNode extends AbstractSignalAtPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = "isSemaphore(semaphore)")
        protected final Object doSignal(final Object receiver, final PointersObject semaphore, final long msTime) {
            signalAtMilliseconds(semaphore, msTime);
            return receiver;
        }

        @SuppressWarnings("unused")
        @Specialization
        protected final Object doSignal(final Object receiver, final NilObject semaphore, final long msTime) {
            resetTimerSemaphore();
            return receiver;
        }
    }

    @DenyReplace
    @SqueakPrimitive(indices = 137)
    public static final class PrimSecondClockNode extends AbstractSingletonPrimitiveNode implements Primitive0 {
        @Override
        public Object execute(final VirtualFrame frame, final Object receiver) {
            return MiscUtils.toSqueakSecondsLocal(MiscUtils.currentTimeMillis() / 1000);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 141)
    protected abstract static class PrimClipboardText1Node extends AbstractPrimitiveNode implements Primitive0 {
        @Specialization
        protected final NativeObject getClipboardText(@SuppressWarnings("unused") final Object receiver) {
            final SqueakImageContext image = getContext();
            if (image.hasDisplay()) {
                return image.asByteString(SqueakDisplay.getClipboardData());
            } else {
                return image.clipboardTextHeadless;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 141)
    protected abstract static class PrimClipboardText2Node extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "value.isByteType()")
        protected final NativeObject setClipboardText(@SuppressWarnings("unused") final Object receiver, final NativeObject value) {
            final SqueakImageContext image = getContext();
            if (image.hasDisplay()) {
                SqueakDisplay.setClipboardData(value.asStringUnsafe());
            } else {
                image.clipboardTextHeadless = value;
            }
            return value;
        }
    }

    @DenyReplace
    @SqueakPrimitive(indices = 142)
    public static final class PrimVMPathNode extends AbstractSingletonPrimitiveNode implements Primitive0 {
        @Override
        public Object execute(final VirtualFrame frame, final Object receiver) {
            return getContext().getResourcesPath(); // Must end with file separator
        }
    }

    @ImportStatic(NativeObject.class)
    @GenerateNodeFactory
    @SqueakPrimitive(indices = 145)
    protected abstract static class PrimConstantFillNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization(guards = "receiver.isByteType()")
        protected static final NativeObject doNativeBytes(final NativeObject receiver, final long value) {
            ArrayUtils.fill(receiver.getByteStorage(), (byte) value);
            return receiver;
        }

        @Specialization(guards = "receiver.isShortType()")
        protected static final NativeObject doNativeShorts(final NativeObject receiver, final long value) {
            ArrayUtils.fill(receiver.getShortStorage(), (short) value);
            return receiver;
        }

        @Specialization(guards = "receiver.isIntType()")
        protected static final NativeObject doNativeInts(final NativeObject receiver, final long value) {
            ArrayUtils.fill(receiver.getIntStorage(), (int) value);
            return receiver;
        }

        @Specialization(guards = {"receiver.isIntType()"}, rewriteOn = ArithmeticException.class)
        protected static final NativeObject doNativeInts(final NativeObject receiver, final NativeObject value,
                        @Bind final SqueakImageContext image) {
            ArrayUtils.fill(receiver.getIntStorage(), LargeIntegers.intValueExact(image, value));
            return receiver;
        }

        @Specialization(guards = {"receiver.isIntType()", "compareTo(image, value, INTEGER_MAX) <= 0"}, replaces = "doNativeInts")
        protected static final NativeObject doNativeIntsFallback(final NativeObject receiver, final NativeObject value,
                        @Bind final SqueakImageContext image) {
            ArrayUtils.fill(receiver.getIntStorage(), LargeIntegers.intValue(image, value));
            return receiver;
        }

        @Specialization(guards = "receiver.isLongType()")
        protected static final NativeObject doNativeLongs(final NativeObject receiver, final long value) {
            ArrayUtils.fill(receiver.getLongStorage(), value);
            return receiver;
        }

        @Specialization(guards = {"receiver.isLongType()"}, rewriteOn = ArithmeticException.class)
        protected static final NativeObject doNativeLongs(final NativeObject receiver, final NativeObject value) {
            ArrayUtils.fill(receiver.getLongStorage(), LargeIntegers.longValueExact(value));
            return receiver;
        }

        @Specialization(guards = {"receiver.isLongType()", "fitsIntoLong(value)"}, replaces = "doNativeLongs")
        protected static final NativeObject doNativeLongsFallback(final NativeObject receiver, final NativeObject value) {
            ArrayUtils.fill(receiver.getLongStorage(), LargeIntegers.longValue(value));
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 148)
    public abstract static class PrimShallowCopyNode extends AbstractPrimitiveNode implements Primitive0 {
        @Specialization
        protected static final Object doShallowCopy(final Object receiver,
                        @Bind final Node node,
                        @Cached final SqueakObjectShallowCopyNode shallowCopyNode) {
            return shallowCopyNode.execute(node, receiver);
        }
    }

    @GenerateNodeFactory
    @ImportStatic(MiscUtils.class)
    @SqueakPrimitive(indices = 149)
    protected abstract static class PrimGetAttributeNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "index == cachedIndex", limit = "1")
        protected final AbstractSqueakObject doGetCached(@SuppressWarnings("unused") final Object receiver, @SuppressWarnings("unused") final long index,
                        @Cached("toIntExact(index)") final int cachedIndex) {
            return getContext().getSystemAttribute(cachedIndex);
        }

        @TruffleBoundary
        @ReportPolymorphism.Megamorphic
        @Specialization(replaces = "doGetCached")
        protected final AbstractSqueakObject doGet(@SuppressWarnings("unused") final Object receiver, final long index) {
            return getContext().getSystemAttribute((int) index);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 158)
    public abstract static class PrimCompareString2Node extends AbstractPrimCompareStringNode implements Primitive1WithFallback {
        @Specialization(guards = {"receiver.isByteType()", "other.isByteType()"})
        protected static final long doCompareAsciiOrder(final NativeObject receiver, final NativeObject other) {
            return compareAsciiOrder(receiver, other) - 2L;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 158)
    public abstract static class PrimCompareString3Node extends AbstractPrimCompareStringNode implements Primitive2WithFallback {
        @Specialization(guards = {"receiver.isByteType()", "other.isByteType()", "orderValue == cachedAsciiOrder"}, limit = "1")
        protected static final long doCompareAsciiOrder(final NativeObject receiver, final NativeObject other, @SuppressWarnings("unused") final NativeObject orderValue,
                        @SuppressWarnings("unused") @Cached("asciiOrderOrNull(orderValue)") final NativeObject cachedAsciiOrder) {
            return compareAsciiOrder(receiver, other);
        }

        @Specialization(guards = {"receiver.isByteType()", "other.isByteType()", "orderValue == cachedOrder"}, limit = "1")
        protected static final long doCompareCached(final NativeObject receiver, final NativeObject other,
                        @SuppressWarnings("unused") final NativeObject orderValue,
                        @Cached("validOrderOrNull(orderValue)") final NativeObject cachedOrder) {
            return compare(receiver, other, cachedOrder);
        }

        @Specialization(guards = {"receiver.isByteType()", "other.isByteType()", "orderValue.isByteType()", "orderValue.getByteLength() >= 256"}, //
                        replaces = {"doCompareAsciiOrder", "doCompareCached"})
        protected static final long doCompare(final NativeObject receiver, final NativeObject other, final NativeObject orderValue) {
            return compare(receiver, other, orderValue);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 163)
    protected abstract static class PrimGetImmutabilityNode extends AbstractPrimitiveNode implements Primitive0 {
        @Specialization
        protected static final boolean doGet(final Object receiver,
                        @Bind final Node node,
                        @Cached(inline = true) final SqueakObjectClassNode classNode) {
            return BooleanObject.wrap(classNode.executeLookup(node, receiver).isImmediateClassType());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 164)
    protected abstract static class PrimSetImmutabilityNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected static final boolean doGet(final Object receiver, @SuppressWarnings("unused") final boolean value,
                        @Bind final Node node,
                        @Cached(inline = true) final SqueakObjectClassNode classNode) {
            // FIXME: implement immutability
            return BooleanObject.wrap(classNode.executeLookup(node, receiver).isImmediateClassType());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 168)
    protected abstract static class PrimCopyObjectNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization(guards = {"receiver.getSqueakClass() == anotherObject.getSqueakClass()", "receiver.size() == anotherObject.size()"})
        protected static final AbstractPointersObject doCopyAbstractPointers(final PointersObject receiver, final PointersObject anotherObject) {
            receiver.copyLayoutValuesFrom(anotherObject);
            return receiver;
        }

        @Specialization(guards = {"receiver.getSqueakClass() == anotherObject.getSqueakClass()", "receiver.size() == anotherObject.size()"})
        protected static final AbstractPointersObject doCopyVariablePointers(final VariablePointersObject receiver, final VariablePointersObject anotherObject) {
            receiver.copyLayoutValuesFrom(anotherObject);
            ArrayUtils.arraycopy(anotherObject.getVariablePart(), 0, receiver.getVariablePart(), 0, anotherObject.getVariablePart().length);
            return receiver;
        }

        @Specialization(guards = {"receiver.getSqueakClass() == anotherObject.getSqueakClass()", "receiver.size() == anotherObject.size()"})
        protected static final AbstractPointersObject doCopyWeakPointers(final WeakVariablePointersObject receiver, final WeakVariablePointersObject anotherObject) {
            receiver.copyLayoutValuesFrom(anotherObject);
            ArrayUtils.arraycopy(anotherObject.getVariablePart(), 0, receiver.getVariablePart(), 0, anotherObject.getVariablePart().length);
            return receiver;
        }

        @Specialization(guards = {"receiver.getSqueakClass() == anotherObject.getSqueakClass()",
                        "receiver.isByteType()", "anotherObject.isByteType()", "receiver.getByteLength() == anotherObject.getByteLength()"})
        protected static final NativeObject doCopyNativeByte(final NativeObject receiver, final NativeObject anotherObject) {
            final byte[] destStorage = receiver.getByteStorage();
            UnsafeUtils.copyBytes(anotherObject.getByteStorage(), 0L, destStorage, 0L, destStorage.length);
            return receiver;
        }

        @Specialization(guards = {"receiver.getSqueakClass() == anotherObject.getSqueakClass()",
                        "receiver.isShortType()", "anotherObject.isShortType()", "receiver.getShortLength() == anotherObject.getShortLength()"})
        protected static final NativeObject doCopyNativeShort(final NativeObject receiver, final NativeObject anotherObject) {
            final short[] destStorage = receiver.getShortStorage();
            UnsafeUtils.copyShorts(anotherObject.getShortStorage(), 0L, destStorage, 0L, destStorage.length);
            return receiver;
        }

        @Specialization(guards = {"receiver.getSqueakClass() == anotherObject.getSqueakClass()",
                        "receiver.isIntType()", "anotherObject.isIntType()", "receiver.getIntLength() == anotherObject.getIntLength()"})
        protected static final NativeObject doCopyNativeInt(final NativeObject receiver, final NativeObject anotherObject) {
            final int[] destStorage = receiver.getIntStorage();
            UnsafeUtils.copyInts(anotherObject.getIntStorage(), 0L, destStorage, 0L, destStorage.length);
            return receiver;
        }

        @Specialization(guards = {"receiver.getSqueakClass() == anotherObject.getSqueakClass()",
                        "receiver.isLongType()", "anotherObject.isLongType()", "receiver.getLongLength() == anotherObject.getLongLength()"})
        protected static final NativeObject doCopyNativeLong(final NativeObject receiver, final NativeObject anotherObject) {
            final long[] destStorage = receiver.getLongStorage();
            UnsafeUtils.copyLongs(anotherObject.getLongStorage(), 0L, destStorage, 0L, destStorage.length);
            return receiver;
        }

        @Specialization(guards = {"receiver.getSqueakClass() == anotherObject.getSqueakClass()",
                        "!isNativeObject(receiver)", "!isAbstractPointersObject(receiver)", "!isContextObject(receiver)",
                        "receiverSize == sizeNode.execute(node, anotherObject)"}, limit = "1")
        protected static final AbstractSqueakObject doCopy(final AbstractSqueakObjectWithClassAndHash receiver, final AbstractSqueakObjectWithClassAndHash anotherObject,
                        @SuppressWarnings("unused") @Bind final Node node,
                        @SuppressWarnings("unused") @Cached final SqueakObjectSizeNode sizeNode,
                        @Bind("sizeNode.execute(node, receiver)") final int receiverSize,
                        @Cached final SqueakObjectAtPut0Node atput0Node,
                        @Cached final SqueakObjectAt0Node at0Node) {
            for (int i = 0; i < receiverSize; i++) {
                atput0Node.execute(node, receiver, i, at0Node.execute(node, anotherObject, i));
            }
            return receiver;
        }
    }

    @DenyReplace
    @SqueakPrimitive(indices = 172)
    protected static final class PrimFetchMournerNode extends AbstractSingletonPrimitiveNode implements Primitive0 {
        @Override
        public Object execute(final VirtualFrame frame, final Object receiver) {
            return NilObject.nullToNil(getContext().ephemeronsQueue.pollFirst());
        }
    }

    @DenyReplace
    @SqueakPrimitive(indices = 176)
    public static final class PrimMaxIdentityHashNode extends AbstractSingletonPrimitiveNode implements Primitive0 {
        @Override
        public Object execute(final VirtualFrame frame, final Object receiver) {
            return (long) SqueakImageConstants.IDENTITY_HASH_HALF_WORD_MASK;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 177)
    protected abstract static class PrimAllInstancesNode extends AbstractPrimitiveNode implements Primitive0WithFallback {

        @SuppressWarnings("unused")
        @Specialization(guards = "classObject.isImmediateClassType()")
        protected final ArrayObject noInstances(final ClassObject classObject) {
            return getContext().newEmptyArray();
        }

        @Specialization(guards = {"!classObject.isImmediateClassType()"})
        protected final ArrayObject allInstances(final ClassObject classObject,
                        @Bind final Node node,
                        @Cached final InlinedConditionProfile isNilClass) {
            final SqueakImageContext image = getContext();
            if (isNilClass.profile(node, image.isNilClass(classObject))) {
                return getContext().asArrayOfObjects(NilObject.SINGLETON);
            } else {
                if (classObject.getSqueakHashInt() == SqueakImageConstants.FREE_OBJECT_CLASS_INDEX_PUN) {
                    return getContext().newEmptyArray(); // Class has not been instantiated yet
                } else {
                    return image.asArrayOfObjects(image.objectGraphUtils.allInstancesOf(classObject));
                }
            }
        }
    }

    @DenyReplace
    @SqueakPrimitive(indices = 240)
    public static final class PrimUTCClockNode extends AbstractSingletonPrimitiveNode implements Primitive0 {
        @Override
        public Object execute(final VirtualFrame frame, final Object receiver) {
            return MiscUtils.toSqueakMicrosecondsUTC(MiscUtils.currentTimeMillis() * 1000);
        }
    }

    @DenyReplace
    @SqueakPrimitive(indices = 241)
    public static final class PrimLocalMicrosecondsClockNode extends AbstractSingletonPrimitiveNode implements Primitive0 {
        @Override
        public Object execute(final VirtualFrame frame, final Object receiver) {
            return MiscUtils.toSqueakMicrosecondsLocal(MiscUtils.currentTimeMillis() * 1000);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 242)
    protected abstract static class PrimSignalAtUTCMicrosecondsNode extends AbstractSignalAtPrimitiveNode implements Primitive2WithFallback {

        @Specialization(guards = "isSemaphore(semaphore)")
        protected final Object doSignal(final Object receiver, final PointersObject semaphore, final long usecsUTC) {
            final long msTime = MiscUtils.toJavaMicrosecondsUTC(usecsUTC) / 1000;
            signalAtMilliseconds(semaphore, msTime);
            return receiver;
        }

        @SuppressWarnings("unused")
        @Specialization
        protected final Object doSignal(final Object receiver, final NilObject semaphore, final long usecsUTC) {
            resetTimerSemaphore();
            return receiver;
        }
    }

    protected abstract static class AbstractPrimVMParametersNode extends AbstractPrimitiveNode {
        protected static final int PARAMS_ARRAY_SIZE = 76;

        protected static final Object vmParameterAt(final SqueakImageContext image, final int index) {
            return switch (index) {
                // end (v3)/size(Spur) of old-space (0-based, read-only)
                case 1 -> MiscUtils.getMemoryPoolUsageCommitted(MiscUtils.GC_OLD_GEN_SUFFIX);
                // end (v3)/size(Spur) of young/new-space (read-only)
                case 2 -> MiscUtils.getMemoryPoolUsageUsed(MiscUtils.GC_EDEN_SPACE_SUFFIX) + MiscUtils.getMemoryPoolUsageUsed(MiscUtils.GC_SURVIVOR_SPACE_SUFFIX);
                // end (v3)/size(Spur) of heap (read-only)
                case 3 -> MiscUtils.runtimeTotalMemory();
                // nil (was allocationCount (read-only))
                case 4 -> NilObject.SINGLETON;
                // nil (was allocations between GCs (read-write)
                case 5 -> NilObject.SINGLETON;
                // survivor count tenuring threshold (read-write)
                case 6 -> 0L;
                // full GCs since startup (read-only)
                case 7 -> MiscUtils.getCollectionCount(MiscUtils.GC_OLD_GEN_NAMES);
                // total milliseconds in full GCs since startup (read-only)
                case 8 -> MiscUtils.getCollectionTime(MiscUtils.GC_OLD_GEN_NAMES);
                // incremental GCs (SqueakV3) or scavenges (Spur) since startup (read-only)
                case 9 -> MiscUtils.getCollectionCount(MiscUtils.GC_YOUNG_GEN_NAMES);
                // total milliseconds in incremental GCs (SqueakV3) or scavenges (Spur) since
                // startup (read-only)
                case 10 -> MiscUtils.getCollectionTime(MiscUtils.GC_YOUNG_GEN_NAMES);
                // tenures of surviving objects since startup (read-only)
                case 11 -> 1L;
                // case 12-20 were specific to ikp's JITTER VM, now 12-19 are open for use
                case 12, 13, 14, 15, 16, 17, 18, 19 -> 0L;
                // utc microseconds at VM start-up (actually at time initialization, which precedes
                // image load).
                case 20 -> MiscUtils.toSqueakMicrosecondsUTC(image.startUpMillis * 1000L);
                // root table size (read-only)
                case 21 -> 0L;
                // root table overflows since startup (read-only)
                case 22 -> 0L;
                // bytes of extra memory to reserve for VM buffers, plugins, etc (stored in image
                // file header).
                case 23 -> 0L;
                // memory threshold above which shrinking object memory (rw)
                case 24 -> 1L;
                // memory headroom when growing object memory (rw)
                case 25 -> 1L;
                // interruptChecksEveryNms - force an ioProcessEvents every N milliseconds (rw)
                case 26 -> image.interrupt.getInterruptCheckMilliseconds();
                // number of times mark loop iterated for current IGC/FGC (read-only) includes ALL
                // marking
                case 27 -> 0L;
                // number of times sweep loop iterated for current IGC/FGC (read-only)
                case 28 -> 0L;
                // number of times make forward loop iterated for current IGC/FGC (read-only)
                case 29 -> 0L;
                // number of times compact move loop iterated for current IGC/FGC (read-only)
                case 30 -> 0L;
                // number of grow memory requests (read-only)
                case 31 -> 0L;
                // number of shrink memory requests (read-only)
                case 32 -> 0L;
                // number of root table entries used for current IGC/FGC (read-only)
                case 33 -> 0L;
                // number of allocations done before current IGC/FGC (read-only)
                case 34 -> 0L;
                // number of survivor objects after current IGC/FGC (read-only)
                case 35 -> 0L;
                // millisecond clock when current IGC/FGC completed (read-only)
                case 36 -> 0L;
                // number of marked objects for Roots of the world, not including Root Table entries
                // for current IGC/FGC (read-only)
                case 37 -> 0L;
                // milliseconds taken by current IGC (read-only)
                case 38 -> 0L;
                // Number of finalization signals for Weak Objects pending when current IGC/FGC
                // completed (read-only)
                case 39 -> 0L;
                // BytesPerOop for this image
                case 40 -> (long) Unsafe.ADDRESS_SIZE;
                // imageFormatVersion for the VM
                case 41 -> (long) image.imageFormat;
                // number of stack pages in use (see SmalltalkImage>>isRunningCog)
                case 42 -> 50L;
                // desired number of stack pages (stored in image file header, max 65535)
                case 43 -> 0L;
                // size of eden, in bytes
                case 44 -> MiscUtils.getMemoryPoolUsageCommitted(MiscUtils.GC_EDEN_SPACE_SUFFIX);
                // desired size of eden, in bytes (stored in image file header)
                case 45 -> 0L;
                // machine code zone size, in bytes (Cog only; otherwise nil)
                case 46 -> NilObject.SINGLETON;
                // desired machine code zone size (stored in image file header; Cog only; otherwise
                // nil)
                case 47 -> NilObject.SINGLETON;
                // various header flags. See #getImageHeaderFlags.
                case 48 -> image.flags.getHeaderFlagsDecoded();
                // max size the image promises to grow the external semaphore table to (0 sets to
                // default, which is 256 as of writing)
                case 49 -> (long) image.flags.getMaxExternalSemaphoreTableSize();
                // nil; reserved for VM parameters that persist in the image (such as eden above)
                case 50, 51 -> NilObject.SINGLETON;
                // root table capacity
                case 52 -> 65536L;
                // number of segments (Spur only; otherwise nil)
                case 53 -> 2L;
                // total size of free old space (Spur only, otherwise nil)
                case 54 -> MiscUtils.getMemoryPoolUsageFree(MiscUtils.GC_OLD_GEN_SUFFIX);
                // ratio of growth and image size at or above which a GC will be performed post
                // scavenge
                case 55 -> 0L;
                // number of process switches since startup (read-only)
                case 56 -> NilObject.SINGLETON;
                // number of ioProcessEvents calls since startup (read-only)
                case 57 -> 0L;
                // number of ForceInterruptCheck calls since startup (read-only)
                case 58 -> 0L;
                // number of check event calls since startup (read-only)
                case 59 -> 0L;
                // number of stack page overflows since startup (read-only)
                case 60 -> 0L;
                // number of stack page divorces since startup (read-only)
                case 61 -> 0L;
                // compiled code compactions since startup (read-only; Cog only; otherwise nil)
                case 62 -> NilObject.SINGLETON;
                // total milliseconds in compiled code compactions since startup (read-only; Cog
                // only; otherwise nil)
                case 63 -> NilObject.SINGLETON;
                // the number of methods that currently have jitted machine-code
                case 64 -> 0L;
                // whether the VM supports a certain feature, MULTIPLE_BYTECODE_SETS is bit 0,
                // IMMTABILITY is bit 1
                case 65 -> 49L;
                // the byte size of a stack page
                case 66 -> (long) UnsafeUtils.getPageSize();
                // the max allowed size of old space (Spur only; nil otherwise; 0 implies no limit
                // except that of the underlying platform)
                case 67 -> 0L;
                // the average number of live stack pages when scanned by GC (at scavenge/gc/become
                // et al)
                case 68 -> 12L;
                // the maximum number of live stack pages when scanned by GC (at scavenge/gc/become
                // et al)
                case 69 -> 16L;
                // the vmProxyMajorVersion (the interpreterProxy VM_MAJOR_VERSION)
                case 70 -> 1L;
                // the vmProxyMinorVersion (the interpreterProxy VM_MINOR_VERSION)
                case 71 -> 13L;
                // do mixed arithmetic; if false binary arithmetic primitives will fail unless
                // receiver and argument are of the same type
                case 75 -> image.flags.isPrimitiveDoMixedArithmetic();
                default -> NilObject.SINGLETON;
            };
        }

        protected static void vmParameterAtPut(final SqueakImageContext image, final int index, final long parameter) {
            // interruptChecksEveryNms - force an ioProcessEvents every N milliseconds (rw)
            if (index == 26) {
                image.interrupt.setInterruptCheckMilliseconds(parameter);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 254)
    protected abstract static class PrimVMParameters1Node extends AbstractPrimVMParametersNode implements Primitive0 {
        /**
         * Behaviour depends on argument count:
         *
         * <pre>
         * 0 args: return an Array of VM parameter values;
         * 1 arg:  return the indicated VM parameter;
         * 2 args: set the VM indicated parameter.
         * </pre>
         */

        @Specialization
        protected final ArrayObject getVMParameters(@SuppressWarnings("unused") final Object receiver) {
            final SqueakImageContext image = getContext();
            final Object[] vmParameters = new Object[PARAMS_ARRAY_SIZE];
            for (int i = 0; i < PARAMS_ARRAY_SIZE; i++) {
                vmParameters[i] = vmParameterAt(image, i + 1);
            }
            return image.asArrayOfObjects(vmParameters);
        }
    }

    @GenerateNodeFactory
    @ImportStatic(MiscUtils.class)
    @SqueakPrimitive(indices = 254)
    protected abstract static class PrimVMParameters2Node extends AbstractPrimVMParametersNode implements Primitive1WithFallback {
        @SuppressWarnings("unused")
        @Specialization(guards = {"index == cachedIndex", "index >= 1", "index < PARAMS_ARRAY_SIZE"}, limit = "1")
        protected final Object getVMParametersCached(final Object receiver, final long index,
                        @Cached("toIntExact(index)") final int cachedIndex) {
            return vmParameterAt(getContext(), cachedIndex);
        }

        @TruffleBoundary
        @Specialization(guards = {"index >= 1", "index < PARAMS_ARRAY_SIZE"}, replaces = "getVMParametersCached")
        protected final Object getVMParameters(@SuppressWarnings("unused") final Object receiver, final long index) {
            return vmParameterAt(getContext(), MiscUtils.toIntExact(index));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 254)
    protected abstract static class PrimVMParameters3Node extends AbstractPrimVMParametersNode implements Primitive2WithFallback {
        @Specialization
        protected final Object setVMParameters(@SuppressWarnings("unused") final Object receiver, final long index, final long value) {
            final SqueakImageContext image = getContext();
            final int theIndex = MiscUtils.toIntExact(index);
            final Object result = vmParameterAt(image, theIndex);
            vmParameterAtPut(image, theIndex, value);
            return result;
        }

        @Specialization
        protected final Object setVMParameters(@SuppressWarnings("unused") final Object receiver, final long index, @SuppressWarnings("unused") final Object value) {
            return vmParameterAt(getContext(), MiscUtils.toIntExact(index)); // ignore writes
        }

    }

    /* Primitive 255 is reserved for RSqueak/VM and no longer needed in TruffleSqueak. */

    /*
     * List all plugins as external modules (prim 572 is for builtins but is not used).
     */
    @GenerateNodeFactory
    @SqueakPrimitive(indices = 573)
    protected abstract static class PrimListExternalModuleNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @CompilationFinal(dimensions = 1) private String[] externalModuleNames;

        @Specialization(guards = "inBounds1(index, getExternalModuleNames().length)")
        protected final NativeObject doGet(@SuppressWarnings("unused") final Object receiver, final long index) {
            return getContext().asByteString(getExternalModuleNames()[(int) index - 1]);
        }

        protected final String[] getExternalModuleNames() {
            if (externalModuleNames == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                externalModuleNames = PrimitiveNodeFactory.getPluginNames();
                Arrays.sort(externalModuleNames);
            }
            return externalModuleNames;
        }

        @Specialization(guards = "!inBounds1(index, getExternalModuleNames().length)")
        @SuppressWarnings("unused")
        protected static final NilObject doGetOutOfBounds(final Object receiver, final long index) {
            return NilObject.SINGLETON;
        }
    }

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return MiscellaneousPrimitivesFactory.getFactories();
    }

    @Override
    public List<? extends AbstractSingletonPrimitiveNode> getSingletonPrimitives() {
        return List.of(
                        new PrimNoopNode(),
                        new PrimLowSpaceSemaphoreNode(),
                        new PrimMillisecondClockNode(),
                        new PrimSecondClockNode(),
                        new PrimVMPathNode(),
                        new PrimFetchMournerNode(),
                        new PrimMaxIdentityHashNode(),
                        new PrimUTCClockNode(),
                        new PrimLocalMicrosecondsClockNode());
    }
}
