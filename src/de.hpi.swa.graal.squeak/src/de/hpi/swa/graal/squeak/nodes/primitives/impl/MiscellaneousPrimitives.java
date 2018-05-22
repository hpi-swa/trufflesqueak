package de.hpi.swa.graal.squeak.nodes.primitives.impl;

import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ValueProfile;

import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.SimulationPrimitiveFailed;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledBlockObject;
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
import de.hpi.swa.graal.squeak.nodes.DispatchNode;
import de.hpi.swa.graal.squeak.nodes.GetOrCreateContextNode;
import de.hpi.swa.graal.squeak.nodes.LookupNode;
import de.hpi.swa.graal.squeak.nodes.accessing.CompiledCodeNodes.IsDoesNotUnderstandNode;
import de.hpi.swa.graal.squeak.nodes.context.SqueakLookupClassNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveNodeFactory;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.graal.squeak.util.ArrayUtils;
import de.hpi.swa.graal.squeak.util.InterruptHandlerNode;

public class MiscellaneousPrimitives extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return MiscellaneousPrimitivesFactory.getFactories();
    }

    private abstract static class AbstractClockPrimitiveNode extends AbstractPrimitiveNode {
        // The delta between Squeak Epoch (January 1st 1901) and POSIX Epoch (January 1st 1970)
        @CompilationFinal private static final long EPOCH_DELTA_MICROSECONDS = (long) (69 * 365 + 17) * 24 * 3600 * 1000 * 1000;
        @CompilationFinal private static final long SEC_TO_USEC = 1000 * 1000;
        @CompilationFinal private static final long USEC_TO_NANO = 1000;
        @CompilationFinal private final long timeZoneOffsetMicroseconds;

        private AbstractClockPrimitiveNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
            final Calendar rightNow = Calendar.getInstance();
            timeZoneOffsetMicroseconds = (((long) rightNow.get(Calendar.ZONE_OFFSET)) + rightNow.get(Calendar.DST_OFFSET)) * 1000;
        }

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

        protected final void signalAtMilliseconds(final AbstractSqueakObject semaphore, final long msTime) {
            if (semaphore.isSpecialKindAt(SPECIAL_OBJECT_INDEX.ClassSemaphore)) {
                code.image.registerSemaphore(semaphore, SPECIAL_OBJECT_INDEX.TheTimerSemaphore);
                code.image.interrupt.setNextWakeupTick(msTime);
            } else {
                code.image.registerSemaphore(code.image.nil, SPECIAL_OBJECT_INDEX.TheTimerSemaphore);
                code.image.interrupt.setNextWakeupTick(0);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 77)
    protected abstract static class PrimSomeInstanceNode extends AbstractPrimitiveNode {

        protected PrimSomeInstanceNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        protected boolean isSmallIntegerClass(final ClassObject classObject) {
            return classObject.equals(code.image.smallIntegerClass);
        }

        protected boolean isClassObject(final ClassObject classObject) {
            return classObject.isClass();
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "isSmallIntegerClass(classObject)")
        protected PointersObject allInstances(final ClassObject classObject) {
            throw new PrimitiveFailed();
        }

        @Specialization(guards = "isClassObject(classObject)")
        protected AbstractSqueakObject someInstance(final ClassObject classObject) {
            try {
                return code.image.objects.someInstance(classObject).get(0);
            } catch (IndexOutOfBoundsException e) {
                throw new PrimitiveFailed();
            }
        }

        @SuppressWarnings("unused")
        @Fallback
        protected PointersObject allInstances(final Object object) {
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
        protected AbstractSqueakObject get(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            return code.image.wrap(code.image.config.getImagePath());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 124)
    protected abstract static class PrimLowSpaceSemaphoreNode extends AbstractPrimitiveNode {

        protected PrimLowSpaceSemaphoreNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected AbstractSqueakObject get(final AbstractSqueakObject receiver, final AbstractSqueakObject semaphore) {
            code.image.registerSemaphore(semaphore, SPECIAL_OBJECT_INDEX.TheLowSpaceSemaphore);
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
    @SqueakPrimitive(index = 134)
    protected abstract static class PrimInterruptSemaphoreNode extends AbstractPrimitiveNode {

        protected PrimInterruptSemaphoreNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final AbstractSqueakObject get(final AbstractSqueakObject receiver, final AbstractSqueakObject semaphore) {
            code.image.registerSemaphore(semaphore, SPECIAL_OBJECT_INDEX.TheInterruptSemaphore);
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

        @Specialization
        protected final AbstractSqueakObject doSignal(final AbstractSqueakObject receiver, final AbstractSqueakObject semaphore, final long msTime) {
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
        protected long doClock(@SuppressWarnings("unused") final ClassObject receiver) {
            return code.image.wrap(currentMicrosecondsLocal() / 1000000);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 141)
    protected abstract static class PrimClipboardTextNode extends AbstractPrimitiveNode {
        @CompilationFinal private final boolean isHeadless = GraphicsEnvironment.isHeadless();
        private String headlessClipboardContents = "";

        protected PrimClipboardTextNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected Object doClipboard(final Object receiver, final NotProvided value) {
            String text;
            if (!isHeadless) {
                try {
                    text = (String) getClipboard().getData(DataFlavor.stringFlavor);
                } catch (UnsupportedFlavorException | IOException | IllegalStateException e) {
                    text = "";
                }
            } else {
                text = headlessClipboardContents;
            }
            return code.image.wrap(text);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected Object doClipboard(final Object receiver, final NativeObject value) {
            final String text = value.toString();
            if (!isHeadless) {
                final StringSelection selection = new StringSelection(text);
                getClipboard().setContents(selection, selection);
            } else {
                headlessClipboardContents = text;
            }
            return value;
        }

        private static Clipboard getClipboard() {
            return Toolkit.getDefaultToolkit().getSystemClipboard();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 142)
    protected abstract static class PrimVMPathNode extends AbstractPrimitiveNode {

        protected PrimVMPathNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected AbstractSqueakObject goVMPath(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            return code.image.wrap(System.getProperty("java.home") + File.separatorChar);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 145)
    protected abstract static class PrimConstantFillNode extends AbstractPrimitiveNode {
        private final ValueProfile storageType = ValueProfile.createClassProfile();

        protected PrimConstantFillNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = "receiver.isByteType()")
        protected final NativeObject doNativeBytes(final NativeObject receiver, final long value) {
            Arrays.fill(receiver.getByteStorage(storageType), (byte) value);
            return receiver;
        }

        @Specialization(guards = "receiver.isShortType()")
        protected final NativeObject doNativeShorts(final NativeObject receiver, final long value) {
            Arrays.fill(receiver.getShortStorage(storageType), (short) value);
            return receiver;
        }

        @Specialization(guards = "receiver.isIntType()")
        protected final NativeObject doNativeInts(final NativeObject receiver, final long value) {
            Arrays.fill(receiver.getIntStorage(storageType), (int) value);
            return receiver;
        }

        @Specialization(guards = "receiver.isLongType()")
        protected final NativeObject doNativeLongs(final NativeObject receiver, final long value) {
            Arrays.fill(receiver.getLongStorage(storageType), value);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 148)
    protected abstract static class PrimShallowCopyNode extends AbstractPrimitiveNode {
        private final ValueProfile storageType = ValueProfile.createClassProfile();

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
            return NativeObject.newNativeBytes(code.image, receiver.getSqClass(), receiver.getByteStorage(storageType).clone());
        }

        @Specialization(guards = "receiver.isShortType()")
        protected final Object doNativeShorts(final NativeObject receiver) {
            return NativeObject.newNativeShorts(code.image, receiver.getSqClass(), receiver.getShortStorage(storageType).clone());
        }

        @Specialization(guards = "receiver.isIntType()")
        protected final Object doNativeInts(final NativeObject receiver) {
            return NativeObject.newNativeInts(code.image, receiver.getSqClass(), receiver.getIntStorage(storageType).clone());
        }

        @Specialization(guards = "receiver.isLongType()")
        protected final Object doNativeLongs(final NativeObject receiver) {
            return NativeObject.newNativeLongs(code.image, receiver.getSqClass(), receiver.getLongStorage(storageType).clone());
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
        protected Object doGet(@SuppressWarnings("unused") final Object image, final long longIndex) {
            final int index = (int) longIndex;
            if (index == 0) {
                final String separator = System.getProperty("file.separator");
                return code.image.wrap(System.getProperty("java.home") + separator + "bin" + separator + "java");
            } else if (index == 1) {
                return code.image.wrap(code.image.config.getImagePath());
            }
            if (index >= 2 && index <= 1000) {
                final String[] restArgs = code.image.config.getRestArgs();
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
                    return code.image.wrap(System.getProperty("os.version"));
                case 1003:  // this platform's processor type
                    return code.image.wrap("intel");
                case 1004:  // vm version
                    return code.image.wrap(System.getProperty("java.version"));
                case 1005:  // window system name
                    return code.image.wrap("Aqua");
                case 1006:  // vm build id
                    return code.image.wrap(SqueakLanguage.NAME + " on " + Truffle.getRuntime().getName());
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
    @SqueakPrimitive(index = 176)
    protected abstract static class PrimMaxIdentityHashNode extends AbstractPrimitiveNode {
        protected PrimMaxIdentityHashNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected Object copy(@SuppressWarnings("unused") final AbstractSqueakObject receiver) {
            return asFloatObject(Math.pow(2, 22) - 1);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 177)
    protected abstract static class PrimAllInstancesNode extends AbstractPrimitiveNode {

        protected PrimAllInstancesNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        protected boolean hasNoInstances(final ClassObject classObject) {
            return code.image.objects.getClassesWithNoInstances().contains(classObject);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "hasNoInstances(classObject)")
        protected PointersObject noInstances(final ClassObject classObject) {
            return code.image.newList(new Object[0]);
        }

        @Specialization
        protected PointersObject allInstances(final ClassObject classObject) {
            return code.image.newList(ArrayUtils.toArray(code.image.objects.allInstances(classObject)));
        }

        @SuppressWarnings("unused")
        @Fallback
        protected PointersObject allInstances(final Object object) {
            throw new PrimitiveFailed();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 240)
    protected abstract static class PrimUTCClockNode extends AbstractClockPrimitiveNode {

        protected PrimUTCClockNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected long time(@SuppressWarnings("unused") final Object receiver) {
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
        protected long time(@SuppressWarnings("unused") final Object receiver) {
            return currentMicrosecondsLocal();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 242)
    protected abstract static class PrimSignalAtUTCMicrosecondsNode extends AbstractSignalAtPrimitiveNode {

        protected PrimSignalAtUTCMicrosecondsNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected AbstractSqueakObject doSignal(final AbstractSqueakObject receiver, final AbstractSqueakObject semaphore, final long usecsUTC) {
            final long msTime = (usecsUTC - AbstractClockPrimitiveNode.EPOCH_DELTA_MICROSECONDS) / 1000;
            signalAtMilliseconds(semaphore, msTime);
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
        protected Object getVMParameters(final Object receiver, final NotProvided index, final NotProvided value) {
            final Object[] vmParameters = new Object[PARAMS_ARRAY_SIZE];
            for (int i = 0; i < PARAMS_ARRAY_SIZE; i++) {
                vmParameters[i] = vmParameterAt(i);
            }
            return code.image.newList(vmParameters);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"index >= 1", "index < PARAMS_ARRAY_SIZE"})
        protected Object getVMParameters(final Object receiver, final long index, final NotProvided value) {
            return vmParameterAt((int) index);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isNotProvided(value)")
        protected Object getVMParameters(final Object receiver, final long index, final Object value) {
            return code.image.nil; // ignore writes
        }

        private Object vmParameterAt(final int index) {
            //@formatter:off
            switch (index) {
                case 1: // end (v3)/size(Spur) of old-space (0-based, read-only)
                case 2: return 1L; // end (v3)/size(Spur) of young/new-space (read-only)
                case 3: // end (v3)/size(Spur) of heap (read-only)
                case 4: return code.image.nil; // nil (was allocationCount (read-only))
                case 5: return code.image.nil; // nil (was allocations between GCs (read-write)
                case 6: // survivor count tenuring threshold (read-write)
                case 7: return ManagementFactory.getGarbageCollectorMXBeans().get(1).getCollectionCount(); // full GCs since startup (read-only)
                case 8: return 1L; // total milliseconds in full GCs since startup (read-only)
                case 9: return ManagementFactory.getGarbageCollectorMXBeans().get(0).getCollectionCount(); // incremental GCs (SqueakV3) or scavenges (Spur) since startup (read-only)
                case 10: // total milliseconds in incremental GCs (SqueakV3) or scavenges (Spur) since startup (read-only)
                case 11: // tenures of surving objects since startup (read-only)
                // case 12-20 were specific to ikp's JITTER VM, now 12-19 are open for use
                case 20: // utc microseconds at VM start-up (actually at time initialization, which precedes image load).
                case 21: // root table size (read-only)
                case 22: return 0L; // root table overflows since startup (read-only)
                case 23: // bytes of extra memory to reserve for VM buffers, plugins, etc (stored in image file header).
                case 24: // memory threshold above which shrinking object memory (rw)
                case 25: // memory headroom when growing object memory (rw)
                case 26: return InterruptHandlerNode.getInterruptChecksEveryNms(); // interruptChecksEveryNms - force an ioProcessEvents every N milliseconds (rw)
                case 27: // number of times mark loop iterated for current IGC/FGC (read-only) includes ALL marking
                case 28: // number of times sweep loop iterated for current IGC/FGC (read-only)
                case 29: // number of times make forward loop iterated for current IGC/FGC (read-only)
                case 30: // number of times compact move loop iterated for current IGC/FGC (read-only)
                case 31: // number of grow memory requests (read-only)
                case 32: // number of shrink memory requests (read-only)
                case 33: // number of root table entries used for current IGC/FGC (read-only)
                case 34: // number of allocations done before current IGC/FGC (read-only)
                case 35: // number of survivor objects after current IGC/FGC (read-only)
                case 36: // millisecond clock when current IGC/FGC completed (read-only)
                case 37: // number of marked objects for Roots of the world, not including Root Table entries for current IGC/FGC (read-only)
                case 38: // milliseconds taken by current IGC (read-only)
                case 39: // Number of finalization signals for Weak Objects pending when current IGC/FGC completed (read-only)
                case 40: return 4L; // BytesPerOop for this image
                case 41: return 6521L; // imageFormatVersion for the VM
                case 42: // number of stack pages in use
                case 43: // desired number of stack pages (stored in image file header, max 65535)
                case 44: return 0L; // size of eden, in bytes
                case 45: // desired size of eden, in bytes (stored in image file header)
                case 46: // machine code zone size, in bytes (Cog only; otherwise nil)
                case 47: // desired machine code zone size (stored in image file header; Cog only; otherwise nil)
                case 48: return 0L; // various header flags.  See getCogVMFlags.
                case 49: // max size the image promises to grow the external semaphore table to (0 sets to default, which is 256 as of writing)
                case 50: case 51: return code.image.nil; // nil; reserved for VM parameters that persist in the image (such as eden above)
                case 52: // root table capacity
                case 53: // number of segments (Spur only; otherwise nil)
                case 54: // total size of free old space (Spur only, otherwise nil)
                case 55: // ratio of growth and image size at or above which a GC will be performed post scavenge
                case 56: // number of process switches since startup (read-only)
                case 57: // number of ioProcessEvents calls since startup (read-only)
                case 58: // number of ForceInterruptCheck calls since startup (read-only)
                case 59: // number of check event calls since startup (read-only)
                case 60: // number of stack page overflows since startup (read-only)
                case 61: // number of stack page divorces since startup (read-only)
                case 62: // compiled code compactions since startup (read-only; Cog only; otherwise nil)
                case 63: // total milliseconds in compiled code compactions since startup (read-only; Cog only; otherwise nil)
                case 64: // the number of methods that currently have jitted machine-code
                case 65: // whether the VM supports a certain feature, MULTIPLE_BYTECODE_SETS is bit 0, IMMTABILITY is bit 1
                case 66: // the byte size of a stack page
                case 67: // the max allowed size of old space (Spur only; nil otherwise; 0 implies no limit except that of the underlying platform)
                case 68: // the average number of live stack pages when scanned by GC (at scavenge/gc/become et al)
                case 69: // the maximum number of live stack pages when scanned by GC (at scavenge/gc/become et al)
                case 70: return 1L; // the vmProxyMajorVersion (the interpreterProxy VM_MAJOR_VERSION)
                case 71: return 13L; // the vmProxyMinorVersion (the interpreterProxy VM_MINOR_VERSION)
                default: return 0L;
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
                final String target = Truffle.getRuntime().getCallerFrame().getCallTarget().toString();
                code.image.getError().println("Simulation primitive failed: " + target + "; reasonCode: " + reasonCode);
            }
            throw new SimulationPrimitiveFailed(reasonCode);
        }
    }

    /*
     * List all plugins as external modules (prim 572 is for builtins but is not used).
     */
    @GenerateNodeFactory
    @SqueakPrimitive(index = 573)
    protected abstract static class PrimListExternalModuleNode extends AbstractPrimitiveNode {
        @CompilationFinal private List<String> externalModuleNames;

        public PrimListExternalModuleNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final Object doGet(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final long index) {
            try {
                return code.image.wrap(getList().get((int) index - 1));
            } catch (IndexOutOfBoundsException e) {
                return code.image.nil;
            }
        }

        private List<String> getList() {
            if (externalModuleNames == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                externalModuleNames = new ArrayList<>(PrimitiveNodeFactory.getPluginNames());
                Collections.sort(externalModuleNames);
            }
            return externalModuleNames;
        }
    }

    /*
     * A simulation primitive is neither a SqueakPrimitive nor a GenerateNodeFactory. Instead, it is
     * directly used by PrimitiveNodeFactory.
     */
    @GenerateNodeFactory
    public abstract static class SimulationPrimitiveNode extends AbstractPrimitiveNode {
        @CompilationFinal public static final String SIMULATE_PRIMITIVE_SELECTOR = "simulatePrimitive:args:";
        @CompilationFinal protected CompiledMethodObject simulationMethod; // different method per
                                                                           // simulation
        @CompilationFinal protected final String moduleName;
        @CompilationFinal protected final NativeObject functionName;
        @CompilationFinal protected final boolean bitBltSimulationNotFound = code.image.simulatePrimitiveArgs.isNil();
        @Child protected LookupNode lookupNode = LookupNode.create();
        @Child protected DispatchNode dispatchNode = DispatchNode.create();
        @Child protected SqueakLookupClassNode lookupClassNode;
        @Child protected GetOrCreateContextNode getOrCreateContextNode = GetOrCreateContextNode.create();
        @Child private IsDoesNotUnderstandNode isDoesNotUnderstandNode;

        protected SimulationPrimitiveNode(final CompiledMethodObject method, final int numArguments, final String moduleName, final String functionName) {
            super(method, numArguments);
            this.moduleName = moduleName;
            this.functionName = code.image.wrap(functionName);
            lookupClassNode = SqueakLookupClassNode.create(method.image);
            isDoesNotUnderstandNode = IsDoesNotUnderstandNode.create(method.image);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected final Object doSimulation(final VirtualFrame frame, final Object receiver, final NotProvided arg1, final NotProvided arg2, final NotProvided arg3,
                        final NotProvided arg4, final NotProvided arg5, final NotProvided arg6, final NotProvided arg7, final NotProvided arg8) {
            return doSimulation(frame, receiver, code.image.newList(new Object[]{}));
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isNotProvided(arg1)"})
        protected final Object doSimulation(final VirtualFrame frame, final Object receiver, final Object arg1, final NotProvided arg2, final NotProvided arg3,
                        final NotProvided arg4, final NotProvided arg5, final NotProvided arg6, final NotProvided arg7, final NotProvided arg8) {
            return doSimulation(frame, receiver, code.image.newList(new Object[]{arg1}));
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isNotProvided(arg1)", "!isNotProvided(arg2)"})
        protected final Object doSimulation(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final NotProvided arg3,
                        final NotProvided arg4, final NotProvided arg5, final NotProvided arg6, final NotProvided arg7, final NotProvided arg8) {
            return doSimulation(frame, receiver, code.image.newList(new Object[]{arg1, arg2}));
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isNotProvided(arg1)", "!isNotProvided(arg2)", "!isNotProvided(arg3)"})
        protected final Object doSimulation(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3,
                        final NotProvided arg4, final NotProvided arg5, final NotProvided arg6, final NotProvided arg7, final NotProvided arg8) {
            return doSimulation(frame, receiver, code.image.newList(new Object[]{arg1, arg2, arg3}));
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isNotProvided(arg1)", "!isNotProvided(arg2)", "!isNotProvided(arg3)", "!isNotProvided(arg4)"})
        protected final Object doSimulation(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4,
                        final NotProvided arg5, final NotProvided arg6, final NotProvided arg7, final NotProvided arg8) {
            return doSimulation(frame, receiver, code.image.newList(new Object[]{arg1, arg2, arg3, arg4}));
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isNotProvided(arg1)", "!isNotProvided(arg2)", "!isNotProvided(arg3)", "!isNotProvided(arg4)", "!isNotProvided(arg5)"})
        protected final Object doSimulation(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4,
                        final Object arg5, final NotProvided arg6, final NotProvided arg7, final NotProvided arg8) {
            return doSimulation(frame, receiver, code.image.newList(new Object[]{arg1, arg2, arg3, arg4, arg5}));
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isNotProvided(arg1)", "!isNotProvided(arg2)", "!isNotProvided(arg3)", "!isNotProvided(arg4)", "!isNotProvided(arg5)", "!isNotProvided(arg6)"})
        protected final Object doSimulation(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4,
                        final Object arg5, final Object arg6, final NotProvided arg7, final NotProvided arg8) {
            return doSimulation(frame, receiver, code.image.newList(new Object[]{arg1, arg2, arg3, arg4, arg5, arg6}));
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isNotProvided(arg1)", "!isNotProvided(arg2)", "!isNotProvided(arg3)", "!isNotProvided(arg4)", "!isNotProvided(arg5)", "!isNotProvided(arg6)",
                        "!isNotProvided(arg7)"})
        protected final Object doSimulation(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4,
                        final Object arg5, final Object arg6, final Object arg7, final NotProvided arg8) {
            return doSimulation(frame, receiver, code.image.newList(new Object[]{arg1, arg2, arg3, arg4, arg5, arg6, arg7}));
        }

        @Specialization(guards = {"!isNotProvided(arg1)", "!isNotProvided(arg2)", "!isNotProvided(arg3)", "!isNotProvided(arg4)", "!isNotProvided(arg5)", "!isNotProvided(arg6)",
                        "!isNotProvided(arg7)", "!isNotProvided(arg8)"})
        protected final Object doSimulation(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4,
                        final Object arg5, final Object arg6, final Object arg7, final Object arg8) {
            return doSimulation(frame, receiver, code.image.newList(new Object[]{arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8}));
        }

        private Object doSimulation(final VirtualFrame frame, final Object receiver, final PointersObject arguments) {
            final Object[] newRcvrAndArgs = new Object[]{receiver, functionName, arguments};
            code.image.interrupt.disable();
            try {
                return dispatchNode.executeDispatch(frame, getSimulateMethod(receiver), newRcvrAndArgs, getContextOrMarker(frame));
            } catch (SimulationPrimitiveFailed e) {
                throw new PrimitiveFailed(e.getReasonCode());
            } finally {
                code.image.interrupt.enable();
            }
        }

        protected CompiledMethodObject getSimulateMethod(final Object receiver) {
            if (simulationMethod == null) {
                if (bitBltSimulationNotFound) {
                    throw new PrimitiveFailed();
                }
                final Object lookupResult;
                if (receiver instanceof ClassObject) {
                    lookupResult = lookupNode.executeLookup(receiver, code.image.simulatePrimitiveArgs);
                } else {
                    final ClassObject rcvrClass = lookupClassNode.executeLookup(receiver);
                    lookupResult = lookupNode.executeLookup(rcvrClass, code.image.simulatePrimitiveArgs);
                }
                if (lookupResult instanceof CompiledMethodObject) {
                    final CompiledMethodObject result = (CompiledMethodObject) lookupResult;
                    if (!isDoesNotUnderstandNode.execute(result)) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        simulationMethod = result;
                        return result;
                    }
                }
                throw new PrimitiveFailed(); // otherwise fail (e.g. Form>>primPixelValueAtX:y:)
            }
            return simulationMethod;
        }
    }
}
