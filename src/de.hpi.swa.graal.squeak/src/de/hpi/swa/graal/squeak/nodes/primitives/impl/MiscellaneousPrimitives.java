package de.hpi.swa.graal.squeak.nodes.primitives.impl;

import java.io.File;
import java.time.Instant;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.SimulationPrimitiveFailed;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledBlockObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.EmptyObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.NotProvided;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.WeakPointersObject;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAtPut0Node;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectSizeNode;
import de.hpi.swa.graal.squeak.nodes.context.ObjectGraphNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveNodeFactory;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.graal.squeak.shared.SqueakLanguageConfig;
import de.hpi.swa.graal.squeak.util.ArrayUtils;
import de.hpi.swa.graal.squeak.util.InterruptHandlerState;

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

        private AbstractClockPrimitiveNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
            final Calendar rightNow = Calendar.getInstance();
            timeZoneOffsetMicroseconds = (((long) rightNow.get(Calendar.ZONE_OFFSET)) + rightNow.get(Calendar.DST_OFFSET)) * 1000;
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

        protected AbstractSignalAtPrimitiveNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        protected final void signalAtMilliseconds(final PointersObject semaphore, final long msTime) {
            code.image.setSemaphore(SPECIAL_OBJECT_INDEX.TheTimerSemaphore, semaphore);
            code.image.interrupt.setTimerSemaphore(semaphore);
            code.image.interrupt.setNextWakeupTick(msTime);
        }

        protected final void signalAtMilliseconds(final NilObject semaphore, @SuppressWarnings("unused") final long msTime) {
            code.image.setSemaphore(SPECIAL_OBJECT_INDEX.TheTimerSemaphore, semaphore);
            code.image.interrupt.setTimerSemaphore(null);
            code.image.interrupt.setNextWakeupTick(0);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 77)
    protected abstract static class PrimSomeInstanceNode extends AbstractPrimitiveNode {
        @Child private ObjectGraphNode objectGraphNode;

        protected PrimSomeInstanceNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
            objectGraphNode = ObjectGraphNode.create(method.image);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "classObject == code.image.smallIntegerClass")
        protected static final Object doSmallIntegerClass(final ClassObject classObject) {
            throw new PrimitiveFailed();
        }

        @Specialization(guards = "classObject != code.image.smallIntegerClass")
        protected final AbstractSqueakObject doSomeInstance(final ClassObject classObject) {
            try {
                return objectGraphNode.someInstanceOf(classObject);
            } catch (IndexOutOfBoundsException e) {
                throw new PrimitiveFailed();
            }
        }

        @SuppressWarnings("unused")
        @Fallback
        protected static final Object allInstances(final Object object) {
            throw new PrimitiveFailed();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 121)
    protected abstract static class PrimImageNameNode extends AbstractPrimitiveNode {

        protected PrimImageNameNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final AbstractSqueakObject get(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            return code.image.wrap(code.image.getImagePath());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 122)
    protected abstract static class PrimNoopNode extends AbstractPrimitiveNode {

        protected PrimNoopNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected static final AbstractSqueakObject get(final AbstractSqueakObject receiver) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 124)
    protected abstract static class PrimLowSpaceSemaphoreNode extends AbstractPrimitiveNode {

        protected PrimLowSpaceSemaphoreNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final AbstractSqueakObject get(final AbstractSqueakObject receiver, final AbstractSqueakObject semaphore) {
            code.image.setSemaphore(SPECIAL_OBJECT_INDEX.TheLowSpaceSemaphore, semaphore);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 125)
    protected abstract static class PrimSetLowSpaceThresholdNode extends AbstractPrimitiveNode {

        protected PrimSetLowSpaceThresholdNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected static final AbstractSqueakObject doSet(final AbstractSqueakObject receiver, @SuppressWarnings("unused") final long numBytes) {
            // TODO: do something with numBytes
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 132)
    protected abstract static class PrimObjectPointsToNode extends AbstractPrimitiveNode {

        protected PrimObjectPointsToNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final boolean doClass(final ClassObject receiver, final Object thang) {
            return receiver.pointsTo(thang) ? code.image.sqTrue : code.image.sqFalse;
        }

        @Specialization
        protected final boolean doClass(final CompiledCodeObject receiver, final Object thang) {
            return ArrayUtils.contains(receiver.getLiterals(), thang) ? code.image.sqTrue : code.image.sqFalse;
        }

        @Specialization
        protected final boolean doContext(final ContextObject receiver, final Object thang) {
            return ArrayUtils.contains(receiver.getPointers(), thang) ? code.image.sqTrue : code.image.sqFalse;
        }

        @Specialization(guards = {"receiver.isEmptyType()", "receiver.getEmptyStorage() > 0"})
        protected final boolean doEmptyArray(@SuppressWarnings("unused") final ArrayObject receiver, final Object thang) {
            return thang == code.image.nil ? code.image.sqTrue : code.image.sqFalse;
        }

        @Specialization(guards = "receiver.isAbstractSqueakObjectType()")
        protected final boolean doArrayOfSqueakObjects(final ArrayObject receiver, final Object thang) {
            return ArrayUtils.contains(receiver.getAbstractSqueakObjectStorage(), thang) ? code.image.sqTrue : code.image.sqFalse;
        }

        @Specialization(guards = "receiver.isBooleanType()")
        protected final boolean doArrayOfBooleans(final ArrayObject receiver, final boolean thang) {
            return ArrayUtils.contains(receiver.getBooleanStorage(), thang ? ArrayObject.BOOLEAN_TRUE_TAG : ArrayObject.BOOLEAN_FALSE_TAG) ? code.image.sqTrue : code.image.sqFalse;
        }

        @Specialization(guards = "receiver.isBooleanType()")
        protected final boolean doArrayOfBooleans(final ArrayObject receiver, @SuppressWarnings("unused") final NilObject thang) {
            return ArrayUtils.contains(receiver.getBooleanStorage(), ArrayObject.BOOLEAN_NIL_TAG) ? code.image.sqTrue : code.image.sqFalse;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"receiver.isBooleanType()", "!isBoolean(thang)", "!isNilObject(thang)"})
        protected final boolean doArrayOfBooleans(final ArrayObject receiver, final Object thang) {
            return code.image.sqFalse;
        }

        @Specialization(guards = "receiver.isCharType()")
        protected final boolean doArrayOfChars(final ArrayObject receiver, final char thang) {
            return ArrayUtils.contains(receiver.getCharStorage(), thang) ? code.image.sqTrue : code.image.sqFalse;
        }

        @Specialization(guards = "receiver.isCharType()")
        protected final boolean doArrayOfChars(final ArrayObject receiver, @SuppressWarnings("unused") final NilObject thang) {
            return ArrayUtils.contains(receiver.getCharStorage(), ArrayObject.CHAR_NIL_TAG) ? code.image.sqTrue : code.image.sqFalse;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"receiver.isCharType()", "!isCharacter(thang)", "!isNilObject(thang)"})
        protected final boolean doArrayOfChars(final ArrayObject receiver, final Object thang) {
            return code.image.sqFalse;
        }

        @Specialization(guards = "receiver.isLongType()")
        protected final boolean doArrayOfLongs(final ArrayObject receiver, final long thang) {
            return ArrayUtils.contains(receiver.getLongStorage(), thang) ? code.image.sqTrue : code.image.sqFalse;
        }

        @Specialization(guards = "receiver.isLongType()")
        protected final boolean doArrayOfLongs(final ArrayObject receiver, @SuppressWarnings("unused") final NilObject thang) {
            return ArrayUtils.contains(receiver.getLongStorage(), ArrayObject.LONG_NIL_TAG) ? code.image.sqTrue : code.image.sqFalse;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"receiver.isLongType()", "!isLong(thang)", "!isNilObject(thang)"})
        protected final boolean doArrayOfLongss(final ArrayObject receiver, final Object thang) {
            return code.image.sqFalse;
        }

        @Specialization(guards = "receiver.isDoubleType()")
        protected final boolean doArrayOfDoubles(final ArrayObject receiver, final double thang) {
            return ArrayUtils.contains(receiver.getDoubleStorage(), thang) ? code.image.sqTrue : code.image.sqFalse;
        }

        @Specialization(guards = "receiver.isDoubleType()")
        protected final boolean doArrayOfDoubles(final ArrayObject receiver, @SuppressWarnings("unused") final NilObject thang) {
            return ArrayUtils.contains(receiver.getDoubleStorage(), ArrayObject.DOUBLE_NIL_TAG) ? code.image.sqTrue : code.image.sqFalse;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"receiver.isDoubleType()", "!isDouble(thang)", "!isNilObject(thang)"})
        protected final boolean doArrayOfDoubles(final ArrayObject receiver, final Object thang) {
            return code.image.sqFalse;
        }

        @Specialization(guards = "receiver.isObjectType()")
        protected final boolean doArrayOfObjects(final ArrayObject receiver, final Object thang) {
            return ArrayUtils.contains(receiver.getObjectStorage(), thang) ? code.image.sqTrue : code.image.sqFalse;
        }

        @Specialization
        protected final boolean doPointers(final PointersObject receiver, final Object thang) {
            return ArrayUtils.contains(receiver.getPointers(), thang) ? code.image.sqTrue : code.image.sqFalse;
        }

        @Specialization
        protected final boolean doWeakPointers(final WeakPointersObject receiver, final Object thang) {
            return ArrayUtils.contains(receiver.getPointers(), thang) ? code.image.sqTrue : code.image.sqFalse;
        }

        @SuppressWarnings("unused")
        @Fallback
        protected final boolean doFallback(final Object receiver, final Object thang) {
            return code.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 134)
    protected abstract static class PrimInterruptSemaphoreNode extends AbstractPrimitiveNode {

        protected PrimInterruptSemaphoreNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final AbstractSqueakObject get(final AbstractSqueakObject receiver, final PointersObject semaphore) {
            code.image.setSemaphore(SPECIAL_OBJECT_INDEX.TheInterruptSemaphore, semaphore);
            code.image.interrupt.setInterruptSemaphore(semaphore);
            return receiver;
        }

        @Specialization
        protected final AbstractSqueakObject get(final AbstractSqueakObject receiver, final NilObject semaphore) {
            code.image.setSemaphore(SPECIAL_OBJECT_INDEX.TheInterruptSemaphore, semaphore);
            code.image.interrupt.setInterruptSemaphore(null);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 135)
    protected abstract static class PrimMillisecondClockNode extends AbstractPrimitiveNode {

        protected PrimMillisecondClockNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final long doClock(@SuppressWarnings("unused") final ClassObject receiver) {
            return code.image.wrap(System.currentTimeMillis() - code.image.startUpMillis);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 136)
    protected abstract static class PrimSignalAtMillisecondsNode extends AbstractSignalAtPrimitiveNode {

        protected PrimSignalAtMillisecondsNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = "semaphore.isSemaphore()")
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
    @SqueakPrimitive(index = 137)
    protected abstract static class PrimSecondClockNode extends AbstractClockPrimitiveNode {

        protected PrimSecondClockNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final long doClock(@SuppressWarnings("unused") final ClassObject receiver) {
            return code.image.wrap(currentMicrosecondsLocal() / 1000000);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 141)
    protected abstract static class PrimClipboardTextNode extends AbstractPrimitiveNode {
        private NativeObject headlessValue;

        protected PrimClipboardTextNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "code.image.hasDisplay()")
        protected final Object getClipboardText(final Object receiver, final NotProvided value) {
            return code.image.wrap(code.image.getDisplay().getClipboardData());
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!code.image.hasDisplay()")
        protected final Object getClipboardTextHeadless(final Object receiver, final NotProvided value) {
            if (headlessValue == null) {
                headlessValue = code.image.wrap("");
            }
            return headlessValue;
        }

        @Specialization(guards = {"code.image.hasDisplay()", "value.isByteType()"})
        protected final Object setClipboardText(@SuppressWarnings("unused") final Object receiver, final NativeObject value) {
            code.image.getDisplay().setClipboardData(value.asString());
            return value;
        }

        @Specialization(guards = {"!code.image.hasDisplay()", "value.isByteType()"})
        protected final Object setClipboardTextHeadless(@SuppressWarnings("unused") final Object receiver, final NativeObject value) {
            headlessValue = value;
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 142)
    protected abstract static class PrimVMPathNode extends AbstractPrimitiveNode {

        protected PrimVMPathNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        @TruffleBoundary
        protected final AbstractSqueakObject doVMPath(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            return code.image.wrap(System.getProperty("java.home") + File.separatorChar);
        }
    }

    @ImportStatic(NativeObject.class)
    @GenerateNodeFactory
    @SqueakPrimitive(index = 145)
    protected abstract static class PrimConstantFillNode extends AbstractPrimitiveNode {

        protected PrimConstantFillNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
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

        @Specialization(guards = "receiver.isLongType()")
        protected static final NativeObject doNativeLongs(final NativeObject receiver, final LargeIntegerObject value) {
            try {
                Arrays.fill(receiver.getLongStorage(), value.longValueExact());
            } catch (ArithmeticException e) {
                throw new PrimitiveFailed();
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 148)
    public abstract static class PrimShallowCopyNode extends AbstractPrimitiveNode {

        protected PrimShallowCopyNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final Object doDouble(final double value) {
            return new FloatObject(code.image, value);
        }

        @Specialization
        protected static final Object doClosure(final BlockClosureObject receiver) {
            return receiver.shallowCopy();
        }

        @Specialization
        protected static final Object doClass(final ClassObject receiver) {
            return receiver.shallowCopy();
        }

        @Specialization
        protected static final Object doBlock(final CompiledBlockObject receiver) {
            return receiver.shallowCopy();
        }

        @Specialization
        protected static final Object doMethod(final CompiledMethodObject receiver) {
            return receiver.shallowCopy();
        }

        @Specialization
        protected static final Object doContext(final ContextObject receiver) {
            return receiver.shallowCopy();
        }

        @Specialization
        protected static final Object doEmpty(final EmptyObject receiver) {
            return receiver.shallowCopy();
        }

        @Specialization(guards = "receiver.isByteType()")
        protected final Object doNativeBytes(final NativeObject receiver) {
            return NativeObject.newNativeBytes(code.image, receiver.getSqueakClass(), receiver.getByteStorage().clone());
        }

        @Specialization(guards = "receiver.isShortType()")
        protected final Object doNativeShorts(final NativeObject receiver) {
            return NativeObject.newNativeShorts(code.image, receiver.getSqueakClass(), receiver.getShortStorage().clone());
        }

        @Specialization(guards = "receiver.isIntType()")
        protected final Object doNativeInts(final NativeObject receiver) {
            return NativeObject.newNativeInts(code.image, receiver.getSqueakClass(), receiver.getIntStorage().clone());
        }

        @Specialization(guards = "receiver.isLongType()")
        protected final Object doNativeLongs(final NativeObject receiver) {
            return NativeObject.newNativeLongs(code.image, receiver.getSqueakClass(), receiver.getLongStorage().clone());
        }

        @Specialization
        protected static final Object doLargeInteger(final LargeIntegerObject receiver) {
            return receiver.shallowCopy();
        }

        @Specialization
        protected static final Object doFloat(final FloatObject receiver) {
            return receiver.shallowCopy();
        }

        @Specialization
        protected static final Object doNil(final NilObject receiver) {
            return receiver.shallowCopy();
        }

        @Specialization(guards = "receiver.isEmptyType()")
        protected final Object doEmptyArray(final ArrayObject receiver) {
            return ArrayObject.createWithStorage(code.image, receiver.getSqueakClass(), receiver.getEmptyStorage());
        }

        @Specialization(guards = "receiver.isAbstractSqueakObjectType()")
        protected final Object doArrayOfSqueakObjects(final ArrayObject receiver) {
            return ArrayObject.createWithStorage(code.image, receiver.getSqueakClass(), receiver.getAbstractSqueakObjectStorage().clone());
        }

        @Specialization(guards = "receiver.isBooleanType()")
        protected final Object doArrayOfBooleans(final ArrayObject receiver) {
            return ArrayObject.createWithStorage(code.image, receiver.getSqueakClass(), receiver.getBooleanStorage().clone());
        }

        @Specialization(guards = "receiver.isCharType()")
        protected final Object doArrayOfChars(final ArrayObject receiver) {
            return ArrayObject.createWithStorage(code.image, receiver.getSqueakClass(), receiver.getCharStorage().clone());
        }

        @Specialization(guards = "receiver.isLongType()")
        protected final Object doArrayOfLongs(final ArrayObject receiver) {
            return ArrayObject.createWithStorage(code.image, receiver.getSqueakClass(), receiver.getLongStorage().clone());
        }

        @Specialization(guards = "receiver.isDoubleType()")
        protected final Object doArrayOfDoubles(final ArrayObject receiver) {
            return ArrayObject.createWithStorage(code.image, receiver.getSqueakClass(), receiver.getDoubleStorage().clone());
        }

        @Specialization(guards = "receiver.isObjectType()")
        protected final Object doArrayOfObjects(final ArrayObject receiver) {
            return ArrayObject.createWithStorage(code.image, receiver.getSqueakClass(), receiver.getObjectStorage().clone());
        }

        @Specialization
        protected static final Object doPointers(final PointersObject receiver) {
            return receiver.shallowCopy();
        }

        @Specialization
        protected static final Object doWeakPointers(final WeakPointersObject receiver) {
            return receiver.shallowCopy();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 149)
    protected abstract static class PrimGetAttributeNode extends AbstractPrimitiveNode {
        protected PrimGetAttributeNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        @TruffleBoundary
        protected final Object doGet(@SuppressWarnings("unused") final Object image, final long longIndex) {
            final int index = (int) longIndex;
            if (index == 0) {
                final String separator = System.getProperty("file.separator");
                return code.image.wrap(System.getProperty("java.home") + separator + "bin" + separator + "java");
            } else if (index == 1) {
                return code.image.wrap(code.image.getImagePath());
            }
            if (index >= 2 && index <= 1000) {
                final String[] restArgs = code.image.getRestArguments();
                if (restArgs.length > index - 2) {
                    return code.image.wrap(restArgs[index - 2]);
                } else {
                    return code.image.nil;
                }
            }
            switch (index) {
                case 1001:  // this platform's operating system 'Mac OS', 'Win32', 'unix', ...
                    return code.image.wrap(code.image.os.getSqOSName());
                case 1002:  // operating system version
                    if (code.image.os.isMacOS()) {
                        /* The image expects things like 1095, so convert 10.10.5 into 1010.5 */
                        return code.image.wrap(System.getProperty("os.version").replaceFirst("\\.", ""));
                    }
                    return code.image.wrap(System.getProperty("os.version"));
                case 1003:  // this platform's processor type
                    return code.image.wrap("intel");
                case 1004:  // vm version
                    return code.image.wrap(System.getProperty("java.version"));
                case 1005:  // window system name
                    return code.image.wrap("Aqua");
                case 1006:  // vm build id
                    return code.image.wrap(SqueakLanguageConfig.NAME + " on " + Truffle.getRuntime().getName());
                // case 1007: // Interpreter class (Cog VM only)
                // case 1008: // Cogit class (Cog VM only)
                // case 1009: // Platform source version (Cog VM only?)
                case 1201: // max filename length (Mac OS only)
                    if (code.image.os.isMacOS()) {
                        return code.image.wrap("255");
                    }
                    break;
                // case 1202: // file last error (Mac OS only)
                // case 10001: // hardware details (Win32 only)
                // case 10002: // operating system details (Win32 only)
                // case 10003: // graphics hardware details (Win32 only)
                default:
                    return code.image.nil;
            }
            return code.image.nil;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 168)
    protected abstract static class PrimCopyObjectNode extends AbstractPrimitiveNode {
        @Child protected SqueakObjectSizeNode sizeNode = SqueakObjectSizeNode.create();
        @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();
        @Child private SqueakObjectAtPut0Node atput0Node = SqueakObjectAtPut0Node.create();

        protected PrimCopyObjectNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = {"!isNativeObject(receiver)", "!isContextObject(receiver)", "receiver.getSqueakClass() == anotherObject.getSqueakClass()",
                        "sizeNode.execute(receiver) == sizeNode.execute(anotherObject)"})
        protected final Object doCopy(final AbstractSqueakObject receiver, final AbstractSqueakObject anotherObject) {
            for (int i = 0; i < sizeNode.execute(receiver); i++) {
                atput0Node.execute(receiver, i, at0Node.execute(anotherObject, i));
            }
            return receiver;
        }

        // TODO: could have more specializations for NativeObject, ArrayObject, ...
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 176)
    protected abstract static class PrimMaxIdentityHashNode extends AbstractPrimitiveNode {
        protected PrimMaxIdentityHashNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final Object doMaxHash(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            return asFloatObject(Math.pow(2, 22) - 1);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 177)
    protected abstract static class PrimAllInstancesNode extends AbstractPrimitiveNode {
        @Child private ObjectGraphNode objectGraphNode;

        protected PrimAllInstancesNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
            objectGraphNode = ObjectGraphNode.create(method.image);
        }

        protected final boolean hasNoInstances(final ClassObject classObject) {
            return objectGraphNode.getClassesWithNoInstances().contains(classObject);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "hasNoInstances(classObject)")
        protected final ArrayObject noInstances(final ClassObject classObject) {
            return code.image.newList(new Object[0]);
        }

        @Specialization
        protected final ArrayObject allInstances(final ClassObject classObject) {
            return code.image.newList(ArrayUtils.toArray(objectGraphNode.allInstancesOf(classObject)));
        }

        @SuppressWarnings("unused")
        @Fallback
        protected static final ArrayObject allInstances(final Object object) {
            throw new PrimitiveFailed();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 183)
    protected abstract static class PrimIsPinnedNode extends AbstractPrimitiveNode {

        protected PrimIsPinnedNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final boolean isPinned(final AbstractSqueakObject receiver) {
            PrimPinNode.printWarningIfNotTesting(code);
            return receiver.isPinned() ? code.image.sqTrue : code.image.sqFalse;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 184)
    protected abstract static class PrimPinNode extends AbstractPrimitiveNode {

        protected PrimPinNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = "enable")
        protected final boolean doPinEnable(final AbstractSqueakObject receiver, @SuppressWarnings("unused") final boolean enable) {
            printWarningIfNotTesting(code);
            final boolean wasPinned = receiver.isPinned();
            receiver.setPinned();
            return wasPinned ? code.image.sqTrue : code.image.sqFalse;
        }

        @Specialization(guards = "!enable")
        protected final boolean doPinDisable(final AbstractSqueakObject receiver, @SuppressWarnings("unused") final boolean enable) {
            printWarningIfNotTesting(code);
            final boolean wasPinned = receiver.isPinned();
            receiver.unsetPinned();
            return wasPinned ? code.image.sqTrue : code.image.sqFalse;
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
    @SqueakPrimitive(index = 240)
    protected abstract static class PrimUTCClockNode extends AbstractClockPrimitiveNode {

        protected PrimUTCClockNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected static final long time(@SuppressWarnings("unused") final Object receiver) {
            return currentMicrosecondsUTC();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 241)
    protected abstract static class PrimLocalMicrosecondsClockNode extends AbstractClockPrimitiveNode {

        protected PrimLocalMicrosecondsClockNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final long time(@SuppressWarnings("unused") final Object receiver) {
            return currentMicrosecondsLocal();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 242)
    protected abstract static class PrimSignalAtUTCMicrosecondsNode extends AbstractSignalAtPrimitiveNode {

        protected PrimSignalAtUTCMicrosecondsNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = "semaphore.isSemaphore()")
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
    @SqueakPrimitive(index = 254)
    protected abstract static class PrimVMParametersNode extends AbstractPrimitiveNode {
        protected static final int PARAMS_ARRAY_SIZE = 71;

        protected PrimVMParametersNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
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

        @SuppressWarnings("unused")
        @Specialization
        protected final Object getVMParameters(final Object receiver, final NotProvided index, final NotProvided value) {
            final Object[] vmParameters = new Object[PARAMS_ARRAY_SIZE];
            for (int i = 0; i < PARAMS_ARRAY_SIZE; i++) {
                vmParameters[i] = vmParameterAt(i + 1);
            }
            return code.image.newList(vmParameters);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"index >= 1", "index < PARAMS_ARRAY_SIZE"})
        protected final Object getVMParameters(final Object receiver, final long index, final NotProvided value) {
            return vmParameterAt((int) index);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isNotProvided(value)")
        protected final Object getVMParameters(final Object receiver, final long index, final Object value) {
            return code.image.nil; // ignore writes
        }

        private Object vmParameterAt(final int index) {
            //@formatter:off
            switch (index) {
                case 1: return 1L; // end (v3)/size(Spur) of old-space (0-based, read-only)
                case 2: return 1L; // end (v3)/size(Spur) of young/new-space (read-only)
                case 3: return 1L; // end (v3)/size(Spur) of heap (read-only)
                case 4: return code.image.nil; // nil (was allocationCount (read-only))
                case 5: return code.image.nil; // nil (was allocations between GCs (read-write)
                case 6: return 0L; // survivor count tenuring threshold (read-write)
                case 7: return 1L; // full GCs since startup (read-only) -> used in InterpreterProxy>>#statNumGCs
                case 8: return 1L; // total milliseconds in full GCs since startup (read-only)
                case 9: return 1L; // incremental GCs (SqueakV3) or scavenges (Spur) since startup (read-only) -> used in InterpreterProxy>>#statNumGCs
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
                case 39: return 0L; // Number of finalization signals for Weak Objects pending when current IGC/FGC completed (read-only)
                case 40: return 4L; // BytesPerOop for this image
                case 41: return 6521L; // imageFormatVersion for the VM
                case 42: return 1L; // number of stack pages in use
                case 43: return 0L; // desired number of stack pages (stored in image file header, max 65535)
                case 44: return 0L; // size of eden, in bytes
                case 45: return 0L; // desired size of eden, in bytes (stored in image file header)
                case 46: return code.image.nil; // machine code zone size, in bytes (Cog only; otherwise nil)
                case 47: return code.image.nil; // desired machine code zone size (stored in image file header; Cog only; otherwise nil)
                case 48: return 0L; // various header flags.  See getCogVMFlags.
                case 49: return 256L; // max size the image promises to grow the external semaphore table to (0 sets to default, which is 256 as of writing)
                case 50: case 51: return code.image.nil; // nil; reserved for VM parameters that persist in the image (such as eden above)
                case 52: return 65536L; // root table capacity
                case 53: return 2L; // number of segments (Spur only; otherwise nil)
                case 54: return 1L; // total size of free old space (Spur only, otherwise nil)
                case 55: return 0L; // ratio of growth and image size at or above which a GC will be performed post scavenge
                case 56: return code.image.nil; // number of process switches since startup (read-only)
                case 57: return 0L; // number of ioProcessEvents calls since startup (read-only)
                case 58: return 0L; // number of ForceInterruptCheck calls since startup (read-only)
                case 59: return 0L; // number of check event calls since startup (read-only)
                case 60: return 0L; // number of stack page overflows since startup (read-only)
                case 61: return 0L; // number of stack page divorces since startup (read-only)
                case 62: return code.image.nil; // compiled code compactions since startup (read-only; Cog only; otherwise nil)
                case 63: return code.image.nil; // total milliseconds in compiled code compactions since startup (read-only; Cog only; otherwise nil)
                case 64: return 0L; // the number of methods that currently have jitted machine-code
                case 65: return 0L; // whether the VM supports a certain feature, MULTIPLE_BYTECODE_SETS is bit 0, IMMTABILITY is bit 1
                case 66: return 4096L; // the byte size of a stack page
                case 67: return 0L; // the max allowed size of old space (Spur only; nil otherwise; 0 implies no limit except that of the underlying platform)
                case 68: return 12L; // the average number of live stack pages when scanned by GC (at scavenge/gc/become et al)
                case 69: return 16L; // the maximum number of live stack pages when scanned by GC (at scavenge/gc/become et al)
                case 70: return 1L; // the vmProxyMajorVersion (the interpreterProxy VM_MAJOR_VERSION)
                case 71: return 13L; // the vmProxyMinorVersion (the interpreterProxy VM_MINOR_VERSION)
                default: return code.image.nil;
            }
            //@formatter:on
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 255)
    protected abstract static class PrimMetaFailNode extends AbstractPrimitiveNode {
        private static final boolean DEBUG_META_PRIMITIVE_FAILURES = false;

        public PrimMetaFailNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final Object doFail(@SuppressWarnings("unused") final PointersObject proxy, final long reasonCode) {
            if (DEBUG_META_PRIMITIVE_FAILURES) {
                debugMetaPrimitiveFailures(reasonCode);
            }
            throw new SimulationPrimitiveFailed(reasonCode);
        }

        @TruffleBoundary
        private void debugMetaPrimitiveFailures(final long reasonCode) {
            final String target = Truffle.getRuntime().getCallerFrame().getCallTarget().toString();
            code.image.printToStdErr("Simulation primitive failed (target:", target, "/ reasonCode:", reasonCode, ")");
        }
    }

    /*
     * List all plugins as external modules (prim 572 is for builtins but is not used).
     */
    @GenerateNodeFactory
    @SqueakPrimitive(index = 573)
    protected abstract static class PrimListExternalModuleNode extends AbstractPrimitiveNode {
        @CompilationFinal(dimensions = 1) protected static final String[] externalModuleNames;

        static {
            final Set<String> pluginNames = PrimitiveNodeFactory.getPluginNames();
            externalModuleNames = pluginNames.toArray(new String[pluginNames.size()]);
            Arrays.sort(externalModuleNames);
        }

        public PrimListExternalModuleNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = "inBounds1(index, externalModuleNames.length)")
        @TruffleBoundary
        protected final Object doGet(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final long index) {
            return code.image.wrap(externalModuleNames[(int) index - 1]);
        }

        @Specialization(guards = "!inBounds1(index, externalModuleNames.length)")
        @SuppressWarnings("unused")
        @TruffleBoundary
        protected final NilObject doGetOutOfBounds(final AbstractSqueakObject receiver, final long index) {
            return code.image.nil;
        }
    }
}
