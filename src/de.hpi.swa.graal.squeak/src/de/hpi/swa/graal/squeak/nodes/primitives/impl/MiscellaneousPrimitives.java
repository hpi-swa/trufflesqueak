package de.hpi.swa.graal.squeak.nodes.primitives.impl;

import java.io.File;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.SimulationPrimitiveFailed;
import de.hpi.swa.graal.squeak.image.reading.SqueakImageReaderNode;
import de.hpi.swa.graal.squeak.model.AbstractPointersObject;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObjectWithClassAndHash;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.NotProvided;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.SPECIAL_OBJECT;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.WeakPointersObject;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAtPut0Node;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectShallowCopyNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectSizeNode;
import de.hpi.swa.graal.squeak.nodes.context.ObjectGraphNode;
import de.hpi.swa.graal.squeak.nodes.plugins.SqueakFFIPrims.AbstractFFIPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.plugins.ffi.FFIConstants.FFI_ERROR;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.DuodecimaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitiveWithoutFallback;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.graal.squeak.shared.SqueakLanguageConfig;
import de.hpi.swa.graal.squeak.util.ArrayUtils;
import de.hpi.swa.graal.squeak.util.InterruptHandlerState;
import de.hpi.swa.graal.squeak.util.MiscUtils;

public final class MiscellaneousPrimitives extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return MiscellaneousPrimitivesFactory.getFactories();
    }

    private abstract static class AbstractClockPrimitiveNode extends AbstractPrimitiveNode {
        // The delta between Squeak Epoch (January 1st 1901) and POSIX Epoch (January 1st 1970)
        private static final long EPOCH_DELTA_MICROSECONDS = (long) (69 * 365 + 17) * 24 * 3600 * 1000 * 1000;
        private static final long SEC_TO_USEC = 1000 * 1000;
        private static final long USEC_TO_NANO = 1000;
        private final long timeZoneOffsetMicroseconds;

        private AbstractClockPrimitiveNode(final CompiledMethodObject method) {
            super(method);
            final Calendar rightNow = Calendar.getInstance();
            timeZoneOffsetMicroseconds = ((long) rightNow.get(Calendar.ZONE_OFFSET) + rightNow.get(Calendar.DST_OFFSET)) * 1000;
        }

        @TruffleBoundary
        protected static final long currentMicrosecondsUTC() {
            final Instant now = Instant.now();
            return now.getEpochSecond() * SEC_TO_USEC + now.getNano() / USEC_TO_NANO + EPOCH_DELTA_MICROSECONDS;
        }

        protected final long currentMicrosecondsLocal() {
            return currentMicrosecondsUTC() + timeZoneOffsetMicroseconds;
        }
    }

    private abstract static class AbstractSignalAtPrimitiveNode extends AbstractPrimitiveNode {

        protected AbstractSignalAtPrimitiveNode(final CompiledMethodObject method) {
            super(method);
        }

        protected final void signalAtMilliseconds(final PointersObject semaphore, final long msTime) {
            method.image.setSemaphore(SPECIAL_OBJECT.THE_TIMER_SEMAPHORE, semaphore);
            method.image.interrupt.setTimerSemaphore(semaphore);
            method.image.interrupt.setNextWakeupTick(msTime);
        }

        protected final void signalAtMilliseconds(final NilObject semaphore, @SuppressWarnings("unused") final long msTime) {
            method.image.setSemaphore(SPECIAL_OBJECT.THE_TIMER_SEMAPHORE, semaphore);
            method.image.interrupt.setTimerSemaphore(null);
            method.image.interrupt.setNextWakeupTick(0);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 77)
    protected abstract static class PrimSomeInstanceNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        @Child private ObjectGraphNode objectGraphNode;

        protected PrimSomeInstanceNode(final CompiledMethodObject method) {
            super(method);
            objectGraphNode = ObjectGraphNode.create(method.image);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "classObject.isImmediateClassType()")
        protected static final Object doSmallIntegerClass(final ClassObject classObject) {
            throw new PrimitiveFailed();
        }

        @Specialization(guards = "!classObject.isImmediateClassType()")
        protected final AbstractSqueakObject doSomeInstance(final ClassObject classObject) {
            try {
                return objectGraphNode.executeSomeInstanceOf(classObject);
            } catch (final IndexOutOfBoundsException e) {
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 120)
    public abstract static class PrimCalloutToFFINode extends AbstractFFIPrimitiveNode implements DuodecimaryPrimitive {

        protected PrimCalloutToFFINode(final CompiledMethodObject method) {
            super(method);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"method.getNumLiterals() > 0"})
        protected final Object doArg0(final AbstractSqueakObject receiver, final NotProvided n1, final NotProvided n2, final NotProvided n3, final NotProvided n4, final NotProvided n5,
                        final NotProvided n6, final NotProvided n7, final NotProvided n8, final NotProvided n9, final NotProvided n10, final NotProvided n11) {
            return doCallout(receiver);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"method.getNumLiterals() > 0", "!isNotProvided(arg1)"})
        protected final Object doArg1(final AbstractSqueakObject receiver, final Object arg1, final NotProvided n2, final NotProvided n3, final NotProvided n4, final NotProvided n5,
                        final NotProvided n6, final NotProvided n7, final NotProvided n8, final NotProvided n9, final NotProvided n10, final NotProvided n11) {
            return doCallout(receiver, arg1);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"method.getNumLiterals() > 0", "!isNotProvided(arg1)", "!isNotProvided(arg2)"})
        protected final Object doArg2(final AbstractSqueakObject receiver, final Object arg1, final Object arg2, final NotProvided n3, final NotProvided n4, final NotProvided n5, final NotProvided n6,
                        final NotProvided n7, final NotProvided n8, final NotProvided n9, final NotProvided n10, final NotProvided n11) {
            return doCallout(receiver, arg1, arg2);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"method.getNumLiterals() > 0", "!isNotProvided(arg1)", "!isNotProvided(arg2)", "!isNotProvided(arg3)"})
        protected final Object doArg3(final AbstractSqueakObject receiver, final Object arg1, final Object arg2, final Object arg3, final NotProvided n4, final NotProvided n5, final NotProvided n6,
                        final NotProvided n7, final NotProvided n8, final NotProvided n9, final NotProvided n10, final NotProvided n11) {
            return doCallout(receiver, arg1, arg2, arg3);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"method.getNumLiterals() > 0", "!isNotProvided(arg1)", "!isNotProvided(arg2)", "!isNotProvided(arg3)", "!isNotProvided(arg4)"})
        protected final Object doArg3(final AbstractSqueakObject receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final NotProvided n5, final NotProvided n6,
                        final NotProvided n7, final NotProvided n8, final NotProvided n9, final NotProvided n10, final NotProvided n11) {
            return doCallout(receiver, arg1, arg2, arg3, arg4);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"method.getNumLiterals() > 0", "!isNotProvided(arg1)", "!isNotProvided(arg2)", "!isNotProvided(arg3)", "!isNotProvided(arg4)", "!isNotProvided(arg5)"})
        protected final Object doArg5(final AbstractSqueakObject receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final NotProvided n6,
                        final NotProvided n7, final NotProvided n8, final NotProvided n9, final NotProvided n10, final NotProvided n11) {
            return doCallout(receiver, arg1, arg2, arg3, arg4, arg5);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"method.getNumLiterals() > 0", "!isNotProvided(arg1)", "!isNotProvided(arg2)", "!isNotProvided(arg3)", "!isNotProvided(arg4)", "!isNotProvided(arg5)",
                        "!isNotProvided(arg6)"})
        protected final Object doArg6(final AbstractSqueakObject receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6,
                        final NotProvided n7, final NotProvided n8, final NotProvided n9, final NotProvided n10, final NotProvided n11) {
            return doCallout(receiver, arg1, arg2, arg3, arg4, arg5, arg6);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"method.getNumLiterals() > 0", "!isNotProvided(arg1)", "!isNotProvided(arg2)", "!isNotProvided(arg3)", "!isNotProvided(arg4)", "!isNotProvided(arg5)",
                        "!isNotProvided(arg6)", "!isNotProvided(arg7)"})
        protected final Object doArg7(final AbstractSqueakObject receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6,
                        final Object arg7, final NotProvided n8, final NotProvided n9, final NotProvided n10, final NotProvided n11) {
            return doCallout(receiver, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"method.getNumLiterals() > 0", "!isNotProvided(arg1)", "!isNotProvided(arg2)", "!isNotProvided(arg3)", "!isNotProvided(arg4)", "!isNotProvided(arg5)",
                        "!isNotProvided(arg6)", "!isNotProvided(arg7)", "!isNotProvided(arg8)"})
        protected final Object doArg8(final AbstractSqueakObject receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6,
                        final Object arg7, final Object arg8, final NotProvided n9, final NotProvided n10, final NotProvided n11) {
            return doCallout(receiver, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"method.getNumLiterals() > 0", "!isNotProvided(arg1)", "!isNotProvided(arg2)", "!isNotProvided(arg3)", "!isNotProvided(arg4)", "!isNotProvided(arg5)",
                        "!isNotProvided(arg6)", "!isNotProvided(arg7)", "!isNotProvided(arg8)", "!isNotProvided(arg9)"})
        protected final Object doArg9(final AbstractSqueakObject receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6,
                        final Object arg7, final Object arg8, final Object arg9, final NotProvided n10, final NotProvided n11) {
            return doCallout(receiver, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"method.getNumLiterals() > 0", "!isNotProvided(arg1)", "!isNotProvided(arg2)", "!isNotProvided(arg3)", "!isNotProvided(arg4)", "!isNotProvided(arg5)",
                        "!isNotProvided(arg6)", "!isNotProvided(arg7)", "!isNotProvided(arg8)", "!isNotProvided(arg9)", "!isNotProvided(arg10)"})
        protected final Object doArg10(final AbstractSqueakObject receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6,
                        final Object arg7, final Object arg8, final Object arg9, final Object arg10, final NotProvided n11) {
            return doCallout(receiver, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"method.getNumLiterals() > 0", "!isNotProvided(arg1)", "!isNotProvided(arg2)", "!isNotProvided(arg3)", "!isNotProvided(arg4)", "!isNotProvided(arg5)",
                        "!isNotProvided(arg6)", "!isNotProvided(arg7)", "!isNotProvided(arg8)", "!isNotProvided(arg9)", "!isNotProvided(arg10)", "!isNotProvided(arg11)"})
        protected final Object doArg11(final AbstractSqueakObject receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6,
                        final Object arg7, final Object arg8, final Object arg9, final Object arg10, final Object arg11) {
            return doCallout(receiver, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11);
        }

        private Object doCallout(final AbstractSqueakObject receiver, final Object... arguments) {
            final Object literal1 = method.getLiterals()[1];
            if (!(literal1 instanceof PointersObject)) {
                throw new PrimitiveFailed(FFI_ERROR.NOT_FUNCTION);
            }
            final PointersObject externalLibraryFunction = (PointersObject) literal1;
            return doCallout(externalLibraryFunction, receiver, arguments);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 121)
    protected abstract static class PrimImageNameNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimImageNameNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final NativeObject doName(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            return method.image.asByteString(method.image.getImagePath());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 122)
    protected abstract static class PrimNoopNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimNoopNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final AbstractSqueakObject get(final AbstractSqueakObject receiver) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 124)
    protected abstract static class PrimLowSpaceSemaphoreNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimLowSpaceSemaphoreNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final AbstractSqueakObject get(final AbstractSqueakObject receiver, final AbstractSqueakObjectWithClassAndHash semaphore) {
            method.image.setSemaphore(SPECIAL_OBJECT.THE_LOW_SPACE_SEMAPHORE, semaphore);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 125)
    protected abstract static class PrimSetLowSpaceThresholdNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimSetLowSpaceThresholdNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final AbstractSqueakObject doSet(final AbstractSqueakObject receiver, @SuppressWarnings("unused") final long numBytes) {
            // TODO: do something with numBytes
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 132)
    protected abstract static class PrimObjectPointsToNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimObjectPointsToNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final boolean doClass(final ClassObject receiver, final Object thang) {
            return receiver.pointsTo(thang) ? method.image.sqTrue : method.image.sqFalse;
        }

        @Specialization
        protected final boolean doClass(final CompiledCodeObject receiver, final Object thang) {
            return ArrayUtils.contains(receiver.getLiterals(), thang) ? method.image.sqTrue : method.image.sqFalse;
        }

        @Specialization
        protected final boolean doContext(final ContextObject receiver, final Object thang) {
            return receiver.pointsTo(thang) ? method.image.sqTrue : method.image.sqFalse;
        }

        @Specialization(guards = {"receiver.isEmptyType()", "receiver.getEmptyStorage() > 0"})
        protected final boolean doEmptyArray(@SuppressWarnings("unused") final ArrayObject receiver, final Object thang) {
            return thang == NilObject.SINGLETON ? method.image.sqTrue : method.image.sqFalse;
        }

        @Specialization(guards = "receiver.isBooleanType()")
        protected final boolean doArrayOfBooleans(final ArrayObject receiver, final boolean thang) {
            return ArrayUtils.contains(receiver.getBooleanStorage(), thang ? ArrayObject.BOOLEAN_TRUE_TAG : ArrayObject.BOOLEAN_FALSE_TAG) ? method.image.sqTrue : method.image.sqFalse;
        }

        @Specialization(guards = "receiver.isBooleanType()")
        protected final boolean doArrayOfBooleans(final ArrayObject receiver, @SuppressWarnings("unused") final NilObject thang) {
            return ArrayUtils.contains(receiver.getBooleanStorage(), ArrayObject.BOOLEAN_NIL_TAG) ? method.image.sqTrue : method.image.sqFalse;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"receiver.isBooleanType()", "!isBoolean(thang)", "!isNil(thang)"})
        protected final boolean doArrayOfBooleans(final ArrayObject receiver, final Object thang) {
            return method.image.sqFalse;
        }

        @Specialization(guards = "receiver.isCharType()")
        protected final boolean doArrayOfChars(final ArrayObject receiver, final char thang) {
            return ArrayUtils.contains(receiver.getCharStorage(), thang) ? method.image.sqTrue : method.image.sqFalse;
        }

        @Specialization(guards = "receiver.isCharType()")
        protected final boolean doArrayOfChars(final ArrayObject receiver, @SuppressWarnings("unused") final NilObject thang) {
            return ArrayUtils.contains(receiver.getCharStorage(), ArrayObject.CHAR_NIL_TAG) ? method.image.sqTrue : method.image.sqFalse;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"receiver.isCharType()", "!isCharacter(thang)", "!isNil(thang)"})
        protected final boolean doArrayOfChars(final ArrayObject receiver, final Object thang) {
            return method.image.sqFalse;
        }

        @Specialization(guards = "receiver.isLongType()")
        protected final boolean doArrayOfLongs(final ArrayObject receiver, final long thang) {
            return ArrayUtils.contains(receiver.getLongStorage(), thang) ? method.image.sqTrue : method.image.sqFalse;
        }

        @Specialization(guards = "receiver.isLongType()")
        protected final boolean doArrayOfLongs(final ArrayObject receiver, @SuppressWarnings("unused") final NilObject thang) {
            return ArrayUtils.contains(receiver.getLongStorage(), ArrayObject.LONG_NIL_TAG) ? method.image.sqTrue : method.image.sqFalse;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"receiver.isLongType()", "!isLong(thang)", "!isNil(thang)"})
        protected final boolean doArrayOfLongss(final ArrayObject receiver, final Object thang) {
            return method.image.sqFalse;
        }

        @Specialization(guards = "receiver.isDoubleType()")
        protected final boolean doArrayOfDoubles(final ArrayObject receiver, final double thang) {
            return ArrayUtils.contains(receiver.getDoubleStorage(), thang) ? method.image.sqTrue : method.image.sqFalse;
        }

        @Specialization(guards = "receiver.isDoubleType()")
        protected final boolean doArrayOfDoubles(final ArrayObject receiver, @SuppressWarnings("unused") final NilObject thang) {
            return ArrayUtils.contains(receiver.getDoubleStorage(), ArrayObject.DOUBLE_NIL_TAG) ? method.image.sqTrue : method.image.sqFalse;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"receiver.isDoubleType()", "!isDouble(thang)", "!isNil(thang)"})
        protected final boolean doArrayOfDoubles(final ArrayObject receiver, final Object thang) {
            return method.image.sqFalse;
        }

        @Specialization(guards = "receiver.isNativeObjectType()")
        protected final boolean doArrayOfNatives(final ArrayObject receiver, final Object thang) {
            return ArrayUtils.contains(receiver.getNativeObjectStorage(), thang) ? method.image.sqTrue : method.image.sqFalse;
        }

        @Specialization(guards = "receiver.isObjectType()")
        protected final boolean doArrayOfObjects(final ArrayObject receiver, final Object thang) {
            return ArrayUtils.contains(receiver.getObjectStorage(), thang) ? method.image.sqTrue : method.image.sqFalse;
        }

        @Specialization
        protected final boolean doPointers(final PointersObject receiver, final Object thang) {
            return ArrayUtils.contains(receiver.getPointers(), thang) ? method.image.sqTrue : method.image.sqFalse;
        }

        @Specialization
        protected final boolean doWeakPointers(final WeakPointersObject receiver, final Object thang) {
            return ArrayUtils.contains(receiver.getPointers(), thang) ? method.image.sqTrue : method.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 134)
    protected abstract static class PrimInterruptSemaphoreNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimInterruptSemaphoreNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final AbstractSqueakObject get(final AbstractSqueakObject receiver, final PointersObject semaphore) {
            method.image.setSemaphore(SPECIAL_OBJECT.THE_INTERRUPT_SEMAPHORE, semaphore);
            method.image.interrupt.setInterruptSemaphore(semaphore);
            return receiver;
        }

        @Specialization
        protected final AbstractSqueakObject get(final AbstractSqueakObject receiver, final NilObject semaphore) {
            method.image.setSemaphore(SPECIAL_OBJECT.THE_INTERRUPT_SEMAPHORE, semaphore);
            method.image.interrupt.setInterruptSemaphore(null);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 135)
    protected abstract static class PrimMillisecondClockNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimMillisecondClockNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final long doClock(@SuppressWarnings("unused") final ClassObject receiver) {
            return System.currentTimeMillis() - method.image.startUpMillis;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 136)
    protected abstract static class PrimSignalAtMillisecondsNode extends AbstractSignalAtPrimitiveNode implements TernaryPrimitive {

        protected PrimSignalAtMillisecondsNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "semaphore.getSqueakClass().isSemaphoreClass()")
        protected final AbstractSqueakObject doSignal(final AbstractSqueakObject receiver, final PointersObject semaphore, final long msTime) {
            signalAtMilliseconds(semaphore, msTime);
            return receiver;
        }

        @Specialization
        protected final AbstractSqueakObject doSignal(final AbstractSqueakObject receiver, final NilObject semaphore, final long msTime) {
            signalAtMilliseconds(semaphore, msTime);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 137)
    protected abstract static class PrimSecondClockNode extends AbstractClockPrimitiveNode implements UnaryPrimitive {

        protected PrimSecondClockNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final long doClock(@SuppressWarnings("unused") final ClassObject receiver) {
            return currentMicrosecondsLocal() / 1000000;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 141)
    protected abstract static class PrimClipboardTextNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        private NativeObject headlessValue;

        protected PrimClipboardTextNode(final CompiledMethodObject method) {
            super(method);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "method.image.hasDisplay()")
        protected final NativeObject getClipboardText(final Object receiver, final NotProvided value) {
            return method.image.asByteString(method.image.getDisplay().getClipboardData());
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!method.image.hasDisplay()")
        protected final NativeObject getClipboardTextHeadless(final Object receiver, final NotProvided value) {
            if (headlessValue == null) {
                headlessValue = method.image.asByteString("");
            }
            return headlessValue;
        }

        @Specialization(guards = {"method.image.hasDisplay()", "value.isByteType()"})
        protected final NativeObject setClipboardText(@SuppressWarnings("unused") final Object receiver, final NativeObject value) {
            method.image.getDisplay().setClipboardData(value.asStringUnsafe());
            return value;
        }

        @Specialization(guards = {"!method.image.hasDisplay()", "value.isByteType()"})
        protected final NativeObject setClipboardTextHeadless(@SuppressWarnings("unused") final Object receiver, final NativeObject value) {
            headlessValue = value;
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 142)
    protected abstract static class PrimVMPathNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimVMPathNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary
        protected final NativeObject doVMPath(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            return method.image.asByteString(System.getProperty("java.home") + File.separatorChar);
        }
    }

    @ImportStatic(NativeObject.class)
    @GenerateNodeFactory
    @SqueakPrimitive(indices = 145)
    protected abstract static class PrimConstantFillNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimConstantFillNode(final CompiledMethodObject method) {
            super(method);
        }

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

        @Specialization(guards = {"receiver.isIntType()", "value.lessThanOrEqualTo(INTEGER_MAX)"})
        protected static final NativeObject doNativeInts(final NativeObject receiver, final LargeIntegerObject value) {
            Arrays.fill(receiver.getIntStorage(), (int) value.longValueExact());
            return receiver;
        }

        @Specialization(guards = "receiver.isLongType()")
        protected static final NativeObject doNativeLongs(final NativeObject receiver, final long value) {
            Arrays.fill(receiver.getLongStorage(), value);
            return receiver;
        }

        @Specialization(guards = {"receiver.isLongType()", "value.fitsIntoLong()"})
        protected static final NativeObject doNativeLongs(final NativeObject receiver, final LargeIntegerObject value) {
            Arrays.fill(receiver.getLongStorage(), value.longValueExact());
            return receiver;
        }
    }

    @NodeInfo(cost = NodeCost.NONE)
    @GenerateNodeFactory
    @SqueakPrimitive(indices = 148)
    public abstract static class PrimShallowCopyNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        @Child private SqueakObjectShallowCopyNode shallowCopyNode;

        protected PrimShallowCopyNode(final CompiledMethodObject method) {
            super(method);
            shallowCopyNode = SqueakObjectShallowCopyNode.create(method.image);
        }

        @Specialization
        protected final Object doShallowCopy(final Object receiver) {
            return shallowCopyNode.execute(receiver);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 149)
    protected abstract static class PrimGetAttributeNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        private static final String VM_BUILD_ID_DATE_FORMAT = "MMM dd yyyy HH:mm:ss zzz";

        protected PrimGetAttributeNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary
        protected final Object doGet(@SuppressWarnings("unused") final Object image, final long longIndex) {
            final int index = (int) longIndex;
            if (index == 0) {
                final String separator = File.separator;
                return method.image.asByteString(System.getProperty("java.home") + separator + "bin" + separator + "java");
            } else if (index == 1) {
                return method.image.asByteString(method.image.getImagePath());
            }
            if (index >= 2 && index <= 1000) {
                final String[] restArgs = method.image.getImageArguments();
                if (restArgs.length > index - 2) {
                    return method.image.asByteString(restArgs[index - 2]);
                } else {
                    return NilObject.SINGLETON;
                }
            }
            switch (index) {
                case 1001:  // this platform's operating system 'Mac OS', 'Win32', 'unix', ...
                    return method.image.asByteString(method.image.os.getSqOSName());
                case 1002:  // operating system version
                    if (method.image.os.isMacOS()) {
                        /* The image expects things like 1095, so convert 10.10.5 into 1010.5 */
                        return method.image.asByteString(System.getProperty("os.version").replaceFirst("\\.", ""));
                    }
                    return method.image.asByteString(System.getProperty("os.version"));
                case 1003:  // this platform's processor type
                    return method.image.asByteString("intel");
                case 1004:  // vm version
                    return method.image.asByteString(SqueakLanguageConfig.NAME + " " + SqueakLanguageConfig.VERSION);
                case 1005:  // window system name
                    return method.image.asByteString("Aqua");
                case 1006:  // vm build id
                    final String osName = System.getProperty("os.name");
                    final String osVersion = System.getProperty("os.version");
                    final String osArch = System.getProperty("os.arch");
                    final String date = new SimpleDateFormat(VM_BUILD_ID_DATE_FORMAT, Locale.US).format(new Date(MiscUtils.getStartTime()));
                    return method.image.asByteString(String.format("%s %s (%s) built on %s", osName, osVersion, osArch, date));
                case 1007: // Interpreter class (Cog VM only)
                    return method.image.asByteString(MiscUtils.getGraalVMInformation());
                case 1008: // Cogit class (Cog VM only)
                    return method.image.asByteString(MiscUtils.getSystemProperties());
                case 1009: // Platform source version
                    return method.image.asByteString(MiscUtils.getVMInformation());
                case 1201: // max filename length (Mac OS only)
                    if (method.image.os.isMacOS()) {
                        return method.image.asByteString("255");
                    }
                    break;
                case 1202: // file last error (Mac OS only)
                    if (method.image.os.isMacOS()) {
                        return method.image.asByteString("0");
                    }
                    break;
                // case 10001: // hardware details (Win32 only)
                // case 10002: // operating system details (Win32 only)
                // case 10003: // graphics hardware details (Win32 only)
                default:
                    return NilObject.SINGLETON;
            }
            return NilObject.SINGLETON;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 168)
    protected abstract static class PrimCopyObjectNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimCopyObjectNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"!isContextObject(receiver)", "receiver.getSqueakClass() == anotherObject.getSqueakClass()", "receiver.size() == anotherObject.size()"})
        protected static final AbstractPointersObject doCopyAbstractPointers(final AbstractPointersObject receiver, final AbstractPointersObject anotherObject) {
            final Object[] destStorage = receiver.getPointers();
            System.arraycopy(anotherObject.getPointers(), 0, destStorage, 0, destStorage.length);
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
                        "!isNativeObject(receiver)", "!isPointersObject(receiver)", "!isContextObject(receiver)",
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
    @SqueakPrimitive(indices = 176)
    protected abstract static class PrimMaxIdentityHashNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimMaxIdentityHashNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final long doMaxHash(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            return AbstractSqueakObjectWithClassAndHash.IDENTITY_HASH_MASK;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 177)
    protected abstract static class PrimAllInstancesNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        @Child private ObjectGraphNode objectGraphNode;

        protected PrimAllInstancesNode(final CompiledMethodObject method) {
            super(method);
            objectGraphNode = ObjectGraphNode.create(method.image);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "classObject.isImmediateClassType()")
        protected final ArrayObject noInstances(final ClassObject classObject) {
            return method.image.newEmptyArray();
        }

        @Specialization(guards = {"!classObject.isNilClass()", "!classObject.isImmediateClassType()"})
        protected final ArrayObject allInstances(final ClassObject classObject) {
            return method.image.asArrayOfObjects(ArrayUtils.toArray(objectGraphNode.executeAllInstancesOf(classObject)));
        }

        @Specialization(guards = "classObject.isNilClass()")
        protected final ArrayObject doNil(@SuppressWarnings("unused") final ClassObject classObject) {
            return method.image.asArrayOfObjects(NilObject.SINGLETON);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 183)
    protected abstract static class PrimIsPinnedNode extends AbstractPrimitiveNode implements UnaryPrimitive {

        protected PrimIsPinnedNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final boolean isPinned(final AbstractSqueakObjectWithClassAndHash receiver) {
            PrimPinNode.printWarningIfNotTesting(method);
            return receiver.isPinned() ? method.image.sqTrue : method.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 184)
    protected abstract static class PrimPinNode extends AbstractPrimitiveNode implements BinaryPrimitive {

        protected PrimPinNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "enable")
        protected final boolean doPinEnable(final AbstractSqueakObjectWithClassAndHash receiver, @SuppressWarnings("unused") final boolean enable) {
            printWarningIfNotTesting(method);
            final boolean wasPinned = receiver.isPinned();
            receiver.setPinned();
            return wasPinned ? method.image.sqTrue : method.image.sqFalse;
        }

        @Specialization(guards = "!enable")
        protected final boolean doPinDisable(final AbstractSqueakObjectWithClassAndHash receiver, @SuppressWarnings("unused") final boolean enable) {
            printWarningIfNotTesting(method);
            final boolean wasPinned = receiver.isPinned();
            receiver.unsetPinned();
            return wasPinned ? method.image.sqTrue : method.image.sqFalse;
        }

        protected static final void printWarningIfNotTesting(final CompiledCodeObject code) {
            if (!code.image.isTesting()) {
                printWarning(code);
            }
        }

        private static void printWarning(final CompiledCodeObject code) {
            code.image.printToStdErr("Object pinning is not supported by this vm, but requested from Squeak/Smalltalk.");
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 240)
    protected abstract static class PrimUTCClockNode extends AbstractClockPrimitiveNode implements UnaryPrimitive {

        protected PrimUTCClockNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final long doTime(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            return currentMicrosecondsUTC();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 241)
    protected abstract static class PrimLocalMicrosecondsClockNode extends AbstractClockPrimitiveNode implements UnaryPrimitive {

        protected PrimLocalMicrosecondsClockNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final long doTime(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            return currentMicrosecondsLocal();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 242)
    protected abstract static class PrimSignalAtUTCMicrosecondsNode extends AbstractSignalAtPrimitiveNode implements TernaryPrimitive {

        protected PrimSignalAtUTCMicrosecondsNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "semaphore.getSqueakClass().isSemaphoreClass()")
        protected final AbstractSqueakObject doSignal(final AbstractSqueakObject receiver, final PointersObject semaphore, final long usecsUTC) {
            final long msTime = (usecsUTC - AbstractClockPrimitiveNode.EPOCH_DELTA_MICROSECONDS) / 1000;
            signalAtMilliseconds(semaphore, msTime);
            return receiver;
        }

        @Specialization
        protected final AbstractSqueakObject doSignal(final AbstractSqueakObject receiver, final NilObject semaphore, @SuppressWarnings("unused") final long usecsUTC) {
            signalAtMilliseconds(semaphore, -1);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 254)
    protected abstract static class PrimVMParametersNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        protected static final int PARAMS_ARRAY_SIZE = 71;

        protected PrimVMParametersNode(final CompiledMethodObject method) {
            super(method);
        }

        /**
         * Behaviour depends on argument count:
         *
         * <pre>
         * 0 args: return an Array of VM parameter values;
         * 1 arg:  return the indicated VM parameter;
         * 2 args: set the VM indicated parameter.
         * </pre>
         */

        @ExplodeLoop
        @SuppressWarnings("unused")
        @Specialization
        protected final ArrayObject getVMParameters(final Object receiver, final NotProvided index, final NotProvided value) {
            final Object[] vmParameters = new Object[PARAMS_ARRAY_SIZE];
            for (int i = 0; i < PARAMS_ARRAY_SIZE; i++) {
                vmParameters[i] = vmParameterAt(i + 1);
            }
            return method.image.asArrayOfObjects(vmParameters);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"index >= 1", "index < PARAMS_ARRAY_SIZE"})
        protected static final Object getVMParameters(final Object receiver, final long index, final NotProvided value) {
            return vmParameterAt((int) index);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isNotProvided(value)")
        protected static final NilObject getVMParameters(final Object receiver, final long index, final Object value) {
            return NilObject.SINGLETON; // ignore writes
        }

        private static Object vmParameterAt(final int index) {
            //@formatter:off
            switch (index) {
                case 1: return 1L; // end (v3)/size(Spur) of old-space (0-based, read-only)
                case 2: return 1L; // end (v3)/size(Spur) of young/new-space (read-only)
                case 3: return 1L; // end (v3)/size(Spur) of heap (read-only)
                case 4: return NilObject.SINGLETON; // nil (was allocationCount (read-only))
                case 5: return NilObject.SINGLETON; // nil (was allocations between GCs (read-write)
                case 6: return 0L; // survivor count tenuring threshold (read-write)
                case 7: return MiscUtils.getCollectionCount(); // full GCs since startup (read-only)
                case 8: return MiscUtils.getCollectionTime(); // total milliseconds in full GCs since startup (read-only)
                case 9: return 1L; // incremental GCs (SqueakV3) or scavenges (Spur) since startup (read-only)
                case 10: return 1L; // total milliseconds in incremental GCs (SqueakV3) or scavenges (Spur) since startup (read-only)
                case 11: return 1L; // tenures of surving objects since startup (read-only)
                case 12: case 13: case 14: case 15: case 16: case 17: case 18: case 19: return 0L; // case 12-20 were specific to ikp's JITTER VM, now 12-19 are open for use
                case 20: return 0L; // utc microseconds at VM start-up (actually at time initialization, which precedes image load).
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
                case 41: return SqueakImageReaderNode.IMAGE_64BIT_VERSION; // imageFormatVersion for the VM
                case 42: return 50L; // number of stack pages in use (see SmalltalkImage>>isRunningCog)
                case 43: return 0L; // desired number of stack pages (stored in image file header, max 65535)
                case 44: return 0L; // size of eden, in bytes
                case 45: return 0L; // desired size of eden, in bytes (stored in image file header)
                case 46: return NilObject.SINGLETON; // machine code zone size, in bytes (Cog only; otherwise nil)
                case 47: return NilObject.SINGLETON; // desired machine code zone size (stored in image file header; Cog only; otherwise nil)
                case 48: return 0L; // various header flags.  See getCogVMFlags.
                case 49: return 256L; // max size the image promises to grow the external semaphore table to (0 sets to default, which is 256 as of writing)
                case 50: case 51: return NilObject.SINGLETON; // nil; reserved for VM parameters that persist in the image (such as eden above)
                case 52: return 65536L; // root table capacity
                case 53: return 2L; // number of segments (Spur only; otherwise nil)
                case 54: return 1L; // total size of free old space (Spur only, otherwise nil)
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
                case 67: return 0L; // the max allowed size of old space (Spur only; nil otherwise; 0 implies no limit except that of the underlying platform)
                case 68: return 12L; // the average number of live stack pages when scanned by GC (at scavenge/gc/become et al)
                case 69: return 16L; // the maximum number of live stack pages when scanned by GC (at scavenge/gc/become et al)
                case 70: return 1L; // the vmProxyMajorVersion (the interpreterProxy VM_MAJOR_VERSION)
                case 71: return 13L; // the vmProxyMinorVersion (the interpreterProxy VM_MINOR_VERSION)
                default: return NilObject.SINGLETON;
            }
            //@formatter:on
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(indices = 255)
    protected abstract static class PrimMetaFailNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected static final boolean DEBUG_META_PRIMITIVE_FAILURES = false;

        public PrimMetaFailNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "!DEBUG_META_PRIMITIVE_FAILURES")
        protected static final Object doFail(@SuppressWarnings("unused") final PointersObject proxy, final long reasonCode) {
            throw new SimulationPrimitiveFailed(reasonCode);
        }

        @Specialization(guards = "DEBUG_META_PRIMITIVE_FAILURES")
        protected final Object doFailAndLog(@SuppressWarnings("unused") final PointersObject proxy, final long reasonCode) {
            debugMetaPrimitiveFailures(reasonCode);
            throw new SimulationPrimitiveFailed(reasonCode);
        }

        @TruffleBoundary
        private void debugMetaPrimitiveFailures(final long reasonCode) {
            final String target = Truffle.getRuntime().getCallerFrame().getCallTarget().toString();
            method.image.printToStdErr("Simulation primitive failed (target:", target, "/ reasonCode:", reasonCode, ")");
        }
    }

    /*
     * List all plugins as external modules (prim 572 is for builtins but is not used).
     */
    @GenerateNodeFactory
    @SqueakPrimitive(indices = 573)
    protected abstract static class PrimListExternalModuleNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        @CompilationFinal(dimensions = 1) private String[] externalModuleNames;

        public PrimListExternalModuleNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "inBounds1(index, getExternalModuleNames().length)")
        protected final NativeObject doGet(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final long index) {
            return method.image.asByteString(getExternalModuleNames()[(int) index - 1]);
        }

        protected final String[] getExternalModuleNames() {
            if (externalModuleNames == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                externalModuleNames = method.image.primitiveNodeFactory.getPluginNames();
                Arrays.sort(externalModuleNames);
            }
            return externalModuleNames;
        }

        @Specialization(guards = "!inBounds1(index, getExternalModuleNames().length)")
        @SuppressWarnings("unused")
        protected static final NilObject doGetOutOfBounds(final AbstractSqueakObject receiver, final long index) {
            return NilObject.SINGLETON;
        }
    }
}
