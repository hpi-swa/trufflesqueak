/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
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
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.image.SqueakImageConstants;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.AbstractPointersObject;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObjectWithClassAndHash;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.LargeIntegerObject;
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
import de.hpi.swa.trufflesqueak.nodes.plugins.SqueakFFIPrims.AbstractFFIPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveInterfaces.DuodecimaryPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitiveWithoutFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeFactory;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.InterruptHandlerState;
import de.hpi.swa.trufflesqueak.util.MiscUtils;
import de.hpi.swa.trufflesqueak.util.NotProvided;
import de.hpi.swa.trufflesqueak.util.ObjectGraphUtils;

public final class MiscellaneousPrimitives extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return MiscellaneousPrimitivesFactory.getFactories();
    }

    private abstract static class AbstractSignalAtPrimitiveNode extends AbstractPrimitiveNode {

        protected static final void signalAtMilliseconds(final SqueakImageContext image, final PointersObject semaphore, final long msTime) {
            image.setSemaphore(SPECIAL_OBJECT.THE_TIMER_SEMAPHORE, semaphore);
            image.interrupt.setTimerSemaphore(semaphore);
            image.interrupt.setNextWakeupTick(msTime);
        }

        protected static final void resetTimerSemaphore(final SqueakImageContext image) {
            image.setSemaphore(SPECIAL_OBJECT.THE_TIMER_SEMAPHORE, NilObject.SINGLETON);
            image.interrupt.setTimerSemaphore(null);
            image.interrupt.setNextWakeupTick(0);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 77)
    protected abstract static class PrimSomeInstanceNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        @SuppressWarnings("unused")
        @Specialization(guards = "classObject.isImmediateClassType()")
        protected static final Object doImmeditate(final ClassObject classObject) {
            throw PrimitiveFailed.GENERIC_ERROR;
        }

        @Specialization(guards = "!classObject.isImmediateClassType()")
        protected static final AbstractSqueakObject doSomeInstance(final ClassObject classObject,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            try {
                return ObjectGraphUtils.someInstanceOf(image, classObject);
            } catch (final IndexOutOfBoundsException e) {
                throw PrimitiveFailed.GENERIC_ERROR;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 120)
    public abstract static class PrimCalloutToFFINode extends AbstractFFIPrimitiveNode implements DuodecimaryPrimitive {
        @CompilationFinal private PointersObject externalFunction;

        @Override
        public final boolean acceptsMethod(final CompiledCodeObject method) {
            CompilerAsserts.neverPartOfCompilation();
            if (method.getNumLiterals() > 0) {
                final Object literal1 = method.getLiterals()[1];
                if (literal1 instanceof PointersObject && ((PointersObject) literal1).getSqueakClass().includesExternalFunctionBehavior()) {
                    externalFunction = (PointersObject) literal1;
                    return true;
                }
            }
            return false;
        }

        @SuppressWarnings("unused")
        @Specialization
        protected final Object doArg0(final AbstractSqueakObject receiver, final NotProvided n1, final NotProvided n2, final NotProvided n3, final NotProvided n4, final NotProvided n5,
                        final NotProvided n6, final NotProvided n7, final NotProvided n8, final NotProvided n9, final NotProvided n10, final NotProvided n11,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return doCallout(image, receiver);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isNotProvided(arg1)"})
        protected final Object doArg1(final AbstractSqueakObject receiver, final Object arg1, final NotProvided n2, final NotProvided n3, final NotProvided n4, final NotProvided n5,
                        final NotProvided n6, final NotProvided n7, final NotProvided n8, final NotProvided n9, final NotProvided n10, final NotProvided n11,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return doCallout(image, receiver, arg1);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isNotProvided(arg1)", "!isNotProvided(arg2)"})
        protected final Object doArg2(final AbstractSqueakObject receiver, final Object arg1, final Object arg2, final NotProvided n3, final NotProvided n4, final NotProvided n5, final NotProvided n6,
                        final NotProvided n7, final NotProvided n8, final NotProvided n9, final NotProvided n10, final NotProvided n11,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return doCallout(image, receiver, arg1, arg2);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isNotProvided(arg1)", "!isNotProvided(arg2)", "!isNotProvided(arg3)"})
        protected final Object doArg3(final AbstractSqueakObject receiver, final Object arg1, final Object arg2, final Object arg3, final NotProvided n4, final NotProvided n5, final NotProvided n6,
                        final NotProvided n7, final NotProvided n8, final NotProvided n9, final NotProvided n10, final NotProvided n11,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return doCallout(image, receiver, arg1, arg2, arg3);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isNotProvided(arg1)", "!isNotProvided(arg2)", "!isNotProvided(arg3)", "!isNotProvided(arg4)"})
        protected final Object doArg3(final AbstractSqueakObject receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final NotProvided n5, final NotProvided n6,
                        final NotProvided n7, final NotProvided n8, final NotProvided n9, final NotProvided n10, final NotProvided n11,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return doCallout(image, receiver, arg1, arg2, arg3, arg4);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isNotProvided(arg1)", "!isNotProvided(arg2)", "!isNotProvided(arg3)", "!isNotProvided(arg4)", "!isNotProvided(arg5)"})
        protected final Object doArg5(final AbstractSqueakObject receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final NotProvided n6,
                        final NotProvided n7, final NotProvided n8, final NotProvided n9, final NotProvided n10, final NotProvided n11,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return doCallout(image, receiver, arg1, arg2, arg3, arg4, arg5);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isNotProvided(arg1)", "!isNotProvided(arg2)", "!isNotProvided(arg3)", "!isNotProvided(arg4)", "!isNotProvided(arg5)", "!isNotProvided(arg6)"})
        protected final Object doArg6(final AbstractSqueakObject receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6,
                        final NotProvided n7, final NotProvided n8, final NotProvided n9, final NotProvided n10, final NotProvided n11,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return doCallout(image, receiver, arg1, arg2, arg3, arg4, arg5, arg6);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isNotProvided(arg1)", "!isNotProvided(arg2)", "!isNotProvided(arg3)", "!isNotProvided(arg4)", "!isNotProvided(arg5)", "!isNotProvided(arg6)",
                        "!isNotProvided(arg7)"})
        protected final Object doArg7(final AbstractSqueakObject receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6,
                        final Object arg7, final NotProvided n8, final NotProvided n9, final NotProvided n10, final NotProvided n11,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return doCallout(image, receiver, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isNotProvided(arg1)", "!isNotProvided(arg2)", "!isNotProvided(arg3)", "!isNotProvided(arg4)", "!isNotProvided(arg5)", "!isNotProvided(arg6)",
                        "!isNotProvided(arg7)", "!isNotProvided(arg8)"})
        protected final Object doArg8(final AbstractSqueakObject receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6,
                        final Object arg7, final Object arg8, final NotProvided n9, final NotProvided n10, final NotProvided n11,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return doCallout(image, receiver, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isNotProvided(arg1)", "!isNotProvided(arg2)", "!isNotProvided(arg3)", "!isNotProvided(arg4)", "!isNotProvided(arg5)", "!isNotProvided(arg6)",
                        "!isNotProvided(arg7)", "!isNotProvided(arg8)", "!isNotProvided(arg9)"})
        protected final Object doArg9(final AbstractSqueakObject receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6,
                        final Object arg7, final Object arg8, final Object arg9, final NotProvided n10, final NotProvided n11,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return doCallout(image, receiver, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isNotProvided(arg1)", "!isNotProvided(arg2)", "!isNotProvided(arg3)", "!isNotProvided(arg4)", "!isNotProvided(arg5)", "!isNotProvided(arg6)",
                        "!isNotProvided(arg7)", "!isNotProvided(arg8)", "!isNotProvided(arg9)", "!isNotProvided(arg10)"})
        protected final Object doArg10(final AbstractSqueakObject receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6,
                        final Object arg7, final Object arg8, final Object arg9, final Object arg10, final NotProvided n11,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return doCallout(image, receiver, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isNotProvided(arg1)", "!isNotProvided(arg2)", "!isNotProvided(arg3)", "!isNotProvided(arg4)", "!isNotProvided(arg5)", "!isNotProvided(arg6)",
                        "!isNotProvided(arg7)", "!isNotProvided(arg8)", "!isNotProvided(arg9)", "!isNotProvided(arg10)", "!isNotProvided(arg11)"})
        protected final Object doArg11(final AbstractSqueakObject receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6,
                        final Object arg7, final Object arg8, final Object arg9, final Object arg10, final Object arg11,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return doCallout(image, receiver, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11);
        }

        private Object doCallout(final SqueakImageContext image, final AbstractSqueakObject receiver, final Object... arguments) {
            return doCallout(image, externalFunction, receiver, arguments);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 121)
    protected abstract static class PrimImageNameNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        @Specialization
        protected static final NativeObject doGetName(@SuppressWarnings("unused") final Object receiver, @SuppressWarnings("unused") final NotProvided value,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return image.asByteString(image.getImagePath());
        }

        @Specialization(guards = "newName.isByteType()")
        protected static final NativeObject doSetName(@SuppressWarnings("unused") final Object receiver, final NativeObject newName,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            image.setImagePath(newName.asStringUnsafe());
            return doGetName(receiver, null, image);
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 122)
    protected abstract static class PrimNoopNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        @Specialization
        protected static final Object doNothing(final Object receiver) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 124)
    protected abstract static class PrimLowSpaceSemaphoreNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        @Specialization
        protected static final Object get(final Object receiver, final AbstractSqueakObjectWithClassAndHash semaphore,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            image.setSemaphore(SPECIAL_OBJECT.THE_LOW_SPACE_SEMAPHORE, semaphore);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 125)
    protected abstract static class PrimSetLowSpaceThresholdNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        @Specialization
        protected static final Object doSet(final Object receiver, @SuppressWarnings("unused") final long numBytes) {
            // TODO: do something with numBytes
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 132)
    protected abstract static class PrimObjectPointsToNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        @Specialization
        protected static final boolean doClass(final ClassObject receiver, final Object thang) {
            return BooleanObject.wrap(receiver.pointsTo(thang));
        }

        @Specialization
        protected static final boolean doClass(final CompiledCodeObject receiver, final Object thang) {
            return BooleanObject.wrap(ArrayUtils.contains(receiver.getLiterals(), thang));
        }

        @Specialization
        protected static final boolean doContext(final ContextObject receiver, final Object thang) {
            return BooleanObject.wrap(receiver.pointsTo(thang));
        }

        @Specialization(guards = {"receiver.isEmptyType()"})
        protected static final boolean doEmptyArray(final ArrayObject receiver, final Object thang,
                        @Cached final ConditionProfile noElementsProfile) {
            if (noElementsProfile.profile(receiver.getEmptyStorage() == 0)) {
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
                        @Shared("identityNode") @Cached final SqueakObjectIdentityNode identityNode) {
            return BooleanObject.wrap(receiver.pointsTo(identityNode, thang));
        }

        @Specialization
        protected static final boolean doVariablePointers(final VariablePointersObject receiver, final Object thang,
                        @Shared("identityNode") @Cached final SqueakObjectIdentityNode identityNode) {
            return BooleanObject.wrap(receiver.pointsTo(identityNode, thang));
        }

        @Specialization
        protected static final boolean doWeakPointers(final WeakVariablePointersObject receiver, final Object thang,
                        @Shared("identityNode") @Cached final SqueakObjectIdentityNode identityNode) {
            return BooleanObject.wrap(receiver.pointsTo(identityNode, thang));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 134)
    protected abstract static class PrimInterruptSemaphoreNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        @Specialization
        protected static final Object get(final Object receiver, final PointersObject semaphore,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            image.setSemaphore(SPECIAL_OBJECT.THE_INTERRUPT_SEMAPHORE, semaphore);
            image.interrupt.setInterruptSemaphore(semaphore);
            return receiver;
        }

        @Specialization
        protected static final Object get(final Object receiver, final NilObject semaphore,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            image.setSemaphore(SPECIAL_OBJECT.THE_INTERRUPT_SEMAPHORE, semaphore);
            image.interrupt.setInterruptSemaphore(null);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 135)
    protected abstract static class PrimMillisecondClockNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        @Specialization
        protected static final long doClock(@SuppressWarnings("unused") final Object receiver,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return System.currentTimeMillis() - image.startUpMillis;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 136)
    protected abstract static class PrimSignalAtMillisecondsNode extends AbstractSignalAtPrimitiveNode implements TernaryPrimitive {

        @Specialization(guards = "semaphore.getSqueakClass().isSemaphoreClass()")
        protected static final Object doSignal(final Object receiver, final PointersObject semaphore, final long msTime,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            signalAtMilliseconds(image, semaphore, msTime);
            return receiver;
        }

        @SuppressWarnings("unused")
        @Specialization
        protected static final Object doSignal(final Object receiver, final NilObject semaphore, final long msTime,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            resetTimerSemaphore(image);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 137)
    protected abstract static class PrimSecondClockNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        @Specialization
        protected static final long doClock(@SuppressWarnings("unused") final Object receiver) {
            return MiscUtils.toSqueakSecondsLocal(System.currentTimeMillis() / 1000);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 141)
    protected abstract static class PrimClipboardTextNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        @SuppressWarnings("unused")
        @Specialization
        protected static final NativeObject getClipboardText(final Object receiver, final NotProvided value,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            if (image.hasDisplay()) {
                return image.asByteString(image.getDisplay().getClipboardData());
            } else {
                return image.clipboardTextHeadless;
            }
        }

        @Specialization(guards = "value.isByteType()")
        protected static final NativeObject setClipboardText(@SuppressWarnings("unused") final Object receiver, final NativeObject value,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            if (image.hasDisplay()) {
                image.getDisplay().setClipboardData(value.asStringUnsafe());
            } else {
                image.clipboardTextHeadless = value;
            }
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 142)
    protected abstract static class PrimVMPathNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        @Specialization
        protected static final NativeObject doVMPath(@SuppressWarnings("unused") final Object receiver,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return image.getResourcesDirectory();
        }
    }

    @ImportStatic(NativeObject.class)
    @GenerateNodeFactory
    @SqueakPrimitive(indices = 145)
    protected abstract static class PrimConstantFillNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        @Specialization(guards = "receiver.isByteType()")
        protected static final NativeObject doNativeBytes(final NativeObject receiver, final long value) {
            Arrays.fill(receiver.getByteStorage(), (byte) value);
            return receiver;
        }

        @Specialization(guards = "receiver.isShortType()")
        protected static final NativeObject doNativeShorts(final NativeObject receiver, final long value) {
            Arrays.fill(receiver.getShortStorage(), (short) value);
            return receiver;
        }

        @Specialization(guards = "receiver.isIntType()")
        protected static final NativeObject doNativeInts(final NativeObject receiver, final long value) {
            Arrays.fill(receiver.getIntStorage(), (int) value);
            return receiver;
        }

        @Specialization(guards = {"receiver.isIntType()"}, rewriteOn = ArithmeticException.class)
        protected static final NativeObject doNativeInts(final NativeObject receiver, final LargeIntegerObject value) {
            Arrays.fill(receiver.getIntStorage(), value.intValueExact());
            return receiver;
        }

        @Specialization(guards = {"receiver.isIntType()", "value.lessThanOrEqualTo(INTEGER_MAX)"}, replaces = "doNativeInts")
        protected static final NativeObject doNativeIntsFallback(final NativeObject receiver, final LargeIntegerObject value) {
            Arrays.fill(receiver.getIntStorage(), value.intValueExact());
            return receiver;
        }

        @Specialization(guards = "receiver.isLongType()")
        protected static final NativeObject doNativeLongs(final NativeObject receiver, final long value) {
            Arrays.fill(receiver.getLongStorage(), value);
            return receiver;
        }

        @Specialization(guards = {"receiver.isLongType()"}, rewriteOn = ArithmeticException.class)
        protected static final NativeObject doNativeLongs(final NativeObject receiver, final LargeIntegerObject value) {
            Arrays.fill(receiver.getLongStorage(), value.longValueExact());
            return receiver;
        }

        @Specialization(guards = {"receiver.isLongType()", "value.fitsIntoLong()"}, replaces = "doNativeLongs")
        protected static final NativeObject doNativeLongsFallback(final NativeObject receiver, final LargeIntegerObject value) {
            Arrays.fill(receiver.getLongStorage(), value.longValueExact());
            return receiver;
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    @GenerateNodeFactory
    @SqueakPrimitive(indices = 148)
    public abstract static class PrimShallowCopyNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        @Specialization
        protected static final Object doShallowCopy(final Object receiver,
                        @Cached final SqueakObjectShallowCopyNode shallowCopyNode,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return shallowCopyNode.execute(image, receiver);
        }
    }

    @ReportPolymorphism
    @GenerateNodeFactory
    @ImportStatic(MiscUtils.class)
    @SqueakPrimitive(indices = 149)
    protected abstract static class PrimGetAttributeNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        @Specialization(guards = "index == cachedIndex", limit = "1")
        protected static final AbstractSqueakObject doGetCached(@SuppressWarnings("unused") final Object receiver, @SuppressWarnings("unused") final long index,
                        @Cached("toIntExact(index)") final int cachedIndex,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return image.getSystemAttribute(cachedIndex);
        }

        @TruffleBoundary
        @Specialization(replaces = "doGetCached")
        protected static final AbstractSqueakObject doGet(@SuppressWarnings("unused") final Object receiver, final long index,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return image.getSystemAttribute((int) index);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 163)
    protected abstract static class PrimGetImmutabilityNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        @Specialization
        protected static final boolean doGet(final Object receiver,
                        @Cached final SqueakObjectClassNode classNode) {
            return BooleanObject.wrap(classNode.executeLookup(receiver).isImmediateClassType());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 164)
    protected abstract static class PrimSetImmutabilityNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        @Specialization
        protected static final boolean doGet(final Object receiver, @SuppressWarnings("unused") final boolean value,
                        @Cached final SqueakObjectClassNode classNode) {
            // FIXME: implement immutability
            return BooleanObject.wrap(classNode.executeLookup(receiver).isImmediateClassType());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 168)
    protected abstract static class PrimCopyObjectNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        @Specialization(guards = {"receiver.getSqueakClass() == anotherObject.getSqueakClass()", "receiver.size() == anotherObject.size()"})
        protected static final AbstractPointersObject doCopyAbstractPointers(final PointersObject receiver, final PointersObject anotherObject) {
            receiver.copyLayoutValuesFrom(anotherObject);
            return receiver;
        }

        @Specialization(guards = {"receiver.getSqueakClass() == anotherObject.getSqueakClass()", "receiver.size() == anotherObject.size()"})
        protected static final AbstractPointersObject doCopyVariablePointers(final VariablePointersObject receiver, final VariablePointersObject anotherObject) {
            receiver.copyLayoutValuesFrom(anotherObject);
            System.arraycopy(anotherObject.getVariablePart(), 0, receiver.getVariablePart(), 0, anotherObject.getVariablePart().length);
            return receiver;
        }

        @Specialization(guards = {"receiver.getSqueakClass() == anotherObject.getSqueakClass()", "receiver.size() == anotherObject.size()"})
        protected static final AbstractPointersObject doCopyWeakPointers(final WeakVariablePointersObject receiver, final WeakVariablePointersObject anotherObject) {
            receiver.copyLayoutValuesFrom(anotherObject);
            System.arraycopy(anotherObject.getVariablePart(), 0, receiver.getVariablePart(), 0, anotherObject.getVariablePart().length);
            return receiver;
        }

        @Specialization(guards = {"receiver.getSqueakClass() == anotherObject.getSqueakClass()",
                        "receiver.isByteType()", "anotherObject.isByteType()", "receiver.getByteLength() == anotherObject.getByteLength()"})
        protected static final NativeObject doCopyNativeByte(final NativeObject receiver, final NativeObject anotherObject) {
            final byte[] destStorage = receiver.getByteStorage();
            System.arraycopy(anotherObject.getByteStorage(), 0, destStorage, 0, destStorage.length);
            return receiver;
        }

        @Specialization(guards = {"receiver.getSqueakClass() == anotherObject.getSqueakClass()",
                        "receiver.isShortType()", "anotherObject.isShortType()", "receiver.getShortLength() == anotherObject.getShortLength()"})
        protected static final NativeObject doCopyNativeShort(final NativeObject receiver, final NativeObject anotherObject) {
            final short[] destStorage = receiver.getShortStorage();
            System.arraycopy(anotherObject.getShortStorage(), 0, destStorage, 0, destStorage.length);
            return receiver;
        }

        @Specialization(guards = {"receiver.getSqueakClass() == anotherObject.getSqueakClass()",
                        "receiver.isIntType()", "anotherObject.isIntType()", "receiver.getIntLength() == anotherObject.getIntLength()"})
        protected static final NativeObject doCopyNativeInt(final NativeObject receiver, final NativeObject anotherObject) {
            final int[] destStorage = receiver.getIntStorage();
            System.arraycopy(anotherObject.getIntStorage(), 0, destStorage, 0, destStorage.length);
            return receiver;
        }

        @Specialization(guards = {"receiver.getSqueakClass() == anotherObject.getSqueakClass()",
                        "receiver.isLongType()", "anotherObject.isLongType()", "receiver.getLongLength() == anotherObject.getLongLength()"})
        protected static final NativeObject doCopyNativeLong(final NativeObject receiver, final NativeObject anotherObject) {
            final long[] destStorage = receiver.getLongStorage();
            System.arraycopy(anotherObject.getLongStorage(), 0, destStorage, 0, destStorage.length);
            return receiver;
        }

        @Specialization(guards = {"receiver.getSqueakClass() == anotherObject.getSqueakClass()",
                        "!isNativeObject(receiver)", "!isAbstractPointersObject(receiver)", "!isContextObject(receiver)",
                        "sizeNode.execute(receiver) == sizeNode.execute(anotherObject)"}, limit = "1")
        protected static final AbstractSqueakObject doCopy(final AbstractSqueakObjectWithClassAndHash receiver, final AbstractSqueakObjectWithClassAndHash anotherObject,
                        @Cached final SqueakObjectSizeNode sizeNode,
                        @Cached final SqueakObjectAtPut0Node atput0Node,
                        @Cached final SqueakObjectAt0Node at0Node) {
            for (int i = 0; i < sizeNode.execute(receiver); i++) {
                atput0Node.execute(receiver, i, at0Node.execute(anotherObject, i));
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(indices = 176)
    protected abstract static class PrimMaxIdentityHashNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        @Specialization
        protected static final long doMaxHash(@SuppressWarnings("unused") final Object receiver) {
            return AbstractSqueakObjectWithClassAndHash.IDENTITY_HASH_MASK;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 177)
    protected abstract static class PrimAllInstancesNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        @SuppressWarnings("unused")
        @Specialization(guards = "classObject.isImmediateClassType()")
        protected static final ArrayObject noInstances(final ClassObject classObject,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return image.newEmptyArray();
        }

        @Specialization(guards = {"!classObject.isNilClass()", "!classObject.isImmediateClassType()"})
        protected static final ArrayObject allInstances(final ClassObject classObject,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return image.asArrayOfObjects(ObjectGraphUtils.allInstancesOf(image, classObject));
        }

        @Specialization(guards = "classObject.isNilClass()")
        protected static final ArrayObject doNil(@SuppressWarnings("unused") final ClassObject classObject,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return image.asArrayOfObjects(NilObject.SINGLETON);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 240)
    protected abstract static class PrimUTCClockNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        @Specialization
        protected static final long doTime(@SuppressWarnings("unused") final Object receiver) {
            return MiscUtils.toSqueakMicrosecondsUTC(System.currentTimeMillis() * 1000);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 241)
    protected abstract static class PrimLocalMicrosecondsClockNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        @Specialization
        protected static final long doTime(@SuppressWarnings("unused") final Object receiver) {
            return MiscUtils.toSqueakMicrosecondsLocal(System.currentTimeMillis() * 1000);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 242)
    protected abstract static class PrimSignalAtUTCMicrosecondsNode extends AbstractSignalAtPrimitiveNode implements TernaryPrimitive {

        @Specialization(guards = "semaphore.getSqueakClass().isSemaphoreClass()")
        protected static final Object doSignal(final Object receiver, final PointersObject semaphore, final long usecsUTC,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            final long msTime = MiscUtils.toJavaMicrosecondsUTC(usecsUTC) / 1000;
            signalAtMilliseconds(image, semaphore, msTime);
            return receiver;
        }

        @SuppressWarnings("unused")
        @Specialization
        protected static final Object doSignal(final Object receiver, final NilObject semaphore, final long usecsUTC,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            resetTimerSemaphore(image);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 254)
    protected abstract static class PrimVMParametersNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        protected static final int PARAMS_ARRAY_SIZE = 71;

        /**
         * Behaviour depends on argument count:
         *
         * <pre>
         * 0 args: return an Array of VM parameter values;
         * 1 arg:  return the indicated VM parameter;
         * 2 args: set the VM indicated parameter.
         * </pre>
         */

        @SuppressWarnings("unused")
        @Specialization
        protected static final ArrayObject getVMParameters(final Object receiver, final NotProvided index, final NotProvided value,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            final Object[] vmParameters = new Object[PARAMS_ARRAY_SIZE];
            for (int i = 0; i < PARAMS_ARRAY_SIZE; i++) {
                vmParameters[i] = vmParameterAt(image, i + 1);
            }
            return image.asArrayOfObjects(vmParameters);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"index >= 1", "index < PARAMS_ARRAY_SIZE"})
        protected static final Object getVMParameters(final Object receiver, final long index, final NotProvided value,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return vmParameterAt(image, (int) index);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isNotProvided(value)")
        protected static final NilObject getVMParameters(final Object receiver, final long index, final Object value) {
            return NilObject.SINGLETON; // ignore writes
        }

        private static Object vmParameterAt(final SqueakImageContext image, final int index) {
            //@formatter:off
            switch (index) {
                case 1: return 1L; // end (v3)/size(Spur) of old-space (0-based, read-only)
                case 2: return 1L; // end (v3)/size(Spur) of young/new-space (read-only)
                case 3: return MiscUtils.runtimeTotalMemory(); // end (v3)/size(Spur) of heap (read-only)
                case 4: return NilObject.SINGLETON; // nil (was allocationCount (read-only))
                case 5: return NilObject.SINGLETON; // nil (was allocations between GCs (read-write)
                case 6: return 0L; // survivor count tenuring threshold (read-write)
                case 7: return MiscUtils.getCollectionCount(); // full GCs since startup (read-only)
                case 8: return MiscUtils.getCollectionTime(); // total milliseconds in full GCs since startup (read-only)
                case 9: return 1L; // incremental GCs (SqueakV3) or scavenges (Spur) since startup (read-only)
                case 10: return 1L; // total milliseconds in incremental GCs (SqueakV3) or scavenges (Spur) since startup (read-only)
                case 11: return 1L; // tenures of surving objects since startup (read-only)
                case 12: case 13: case 14: case 15: case 16: case 17: case 18: case 19: return 0L; // case 12-20 were specific to ikp's JITTER VM, now 12-19 are open for use
                case 20: return MiscUtils.toSqueakMicrosecondsUTC(image.startUpMillis * 1000L); // utc microseconds at VM start-up (actually at time initialization, which precedes image load).
                case 21: return 0L; // root table size (read-only)
                case 22: return 0L; // root table overflows since startup (read-only)
                case 23: return 0L; // bytes of extra memory to reserve for VM buffers, plugins, etc (stored in image file header).
                case 24: return 1L; // memory threshold above which shrinking object memory (rw)
                case 25: return 1L; // memory headroom when growing object memory (rw)
                case 26: return (long) InterruptHandlerState.getInterruptChecksEveryNms(); // interruptChecksEveryNms - force an ioProcessEvents every N milliseconds (rw)
                case 27: return 0L; // number of times mark loop iterated for current IGC/FGC (read-only) includes ALL marking
                case 28: return 0L; // number of times sweep loop iterated for current IGC/FGC (read-only)
                case 29: return 0L; // number of times make forward loop iterated for current IGC/FGC (read-only)
                case 30: return 0L; // number of times compact move loop iterated for current IGC/FGC (read-only)
                case 31: return 0L; // number of grow memory requests (read-only)
                case 32: return 0L; // number of shrink memory requests (read-only)
                case 33: return 0L; // number of root table entries used for current IGC/FGC (read-only)
                case 34: return 0L; // number of allocations done before current IGC/FGC (read-only)
                case 35: return 0L; // number of survivor objects after current IGC/FGC (read-only)
                case 36: return 0L; // millisecond clock when current IGC/FGC completed (read-only)
                case 37: return 0L; // number of marked objects for Roots of the world, not including Root Table entries for current IGC/FGC (read-only)
                case 38: return 0L; // milliseconds taken by current IGC (read-only)
                case 39: return MiscUtils.getObjectPendingFinalizationCount(); // Number of finalization signals for Weak Objects pending when current IGC/FGC completed (read-only)
                case 40: return 8L; // BytesPerOop for this image
                case 41: return (long) SqueakImageConstants.IMAGE_FORMAT; // imageFormatVersion for the VM
                case 42: return 50L; // number of stack pages in use (see SmalltalkImage>>isRunningCog)
                case 43: return 0L; // desired number of stack pages (stored in image file header, max 65535)
                case 44: return 0L; // size of eden, in bytes
                case 45: return 0L; // desired size of eden, in bytes (stored in image file header)
                case 46: return NilObject.SINGLETON; // machine code zone size, in bytes (Cog only; otherwise nil)
                case 47: return NilObject.SINGLETON; // desired machine code zone size (stored in image file header; Cog only; otherwise nil)
                case 48: return 0L; // various header flags.  See getCogVMFlags.
                case 49: return (long) image.flags.getMaxExternalSemaphoreTableSize(); // max size the image promises to grow the external semaphore table to (0 sets to default, which is 256 as of writing)
                case 50: case 51: return NilObject.SINGLETON; // nil; reserved for VM parameters that persist in the image (such as eden above)
                case 52: return 65536L; // root table capacity
                case 53: return 2L; // number of segments (Spur only; otherwise nil)
                case 54: return MiscUtils.runtimeFreeMemory(); // total size of free old space (Spur only, otherwise nil)
                case 55: return 0L; // ratio of growth and image size at or above which a GC will be performed post scavenge
                case 56: return NilObject.SINGLETON; // number of process switches since startup (read-only)
                case 57: return 0L; // number of ioProcessEvents calls since startup (read-only)
                case 58: return 0L; // number of ForceInterruptCheck calls since startup (read-only)
                case 59: return 0L; // number of check event calls since startup (read-only)
                case 60: return 0L; // number of stack page overflows since startup (read-only)
                case 61: return 0L; // number of stack page divorces since startup (read-only)
                case 62: return NilObject.SINGLETON; // compiled code compactions since startup (read-only; Cog only; otherwise nil)
                case 63: return NilObject.SINGLETON; // total milliseconds in compiled code compactions since startup (read-only; Cog only; otherwise nil)
                case 64: return 0L; // the number of methods that currently have jitted machine-code
                case 65: return 0L; // whether the VM supports a certain feature, MULTIPLE_BYTECODE_SETS is bit 0, IMMTABILITY is bit 1
                case 66: return 4096L; // the byte size of a stack page
                case 67:
                    final long maxMemory = MiscUtils.runtimeMaxMemory();
                    return maxMemory == Long.MAX_VALUE ? 0L : maxMemory; // the max allowed size of old space (Spur only; nil otherwise; 0 implies no limit except that of the underlying platform)
                case 68: return 12L; // the average number of live stack pages when scanned by GC (at scavenge/gc/become et al)
                case 69: return 16L; // the maximum number of live stack pages when scanned by GC (at scavenge/gc/become et al)
                case 70: return 1L; // the vmProxyMajorVersion (the interpreterProxy VM_MAJOR_VERSION)
                case 71: return 13L; // the vmProxyMinorVersion (the interpreterProxy VM_MINOR_VERSION)
                default: return NilObject.SINGLETON;
            }
            //@formatter:on
        }
    }

    /* Primitive 255 is reserved for RSqueak/VM and no longer needed in TruffleSqueak. */

    /*
     * List all plugins as external modules (prim 572 is for builtins but is not used).
     */
    @GenerateNodeFactory
    @SqueakPrimitive(indices = 573)
    protected abstract static class PrimListExternalModuleNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        @CompilationFinal(dimensions = 1) private String[] externalModuleNames;

        @Specialization(guards = "inBounds1(index, getExternalModuleNames().length)")
        protected final NativeObject doGet(@SuppressWarnings("unused") final Object receiver, final long index,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return image.asByteString(getExternalModuleNames()[(int) index - 1]);
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
}
