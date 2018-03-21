package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

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
import java.util.Calendar;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.exceptions.PrimitiveExceptions.SimulationPrimitiveFailed;
import de.hpi.swa.trufflesqueak.exceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.FloatObject;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.DispatchNode;
import de.hpi.swa.trufflesqueak.nodes.GetOrCreateContextNode;
import de.hpi.swa.trufflesqueak.nodes.LookupNode;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.context.SqueakLookupClassNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.MiscellaneousPrimitivesFactory.PrimBalloonEngineSimulateNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.MiscellaneousPrimitivesFactory.PrimBitBltSimulateNodeGen;
import de.hpi.swa.trufflesqueak.util.InterruptHandlerNode;

public class MiscellaneousPrimitives extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return MiscellaneousPrimitivesFactory.getFactories();
    }

    private static abstract class AbstractClockPrimitiveNode extends AbstractPrimitiveNode {
        // The delta between Squeak Epoch (January 1st 1901) and POSIX Epoch (January 1st 1970)
        @CompilationFinal private static final long EPOCH_DELTA_MICROSECONDS = (long) (69 * 365 + 17) * 24 * 3600 * 1000 * 1000;
        @CompilationFinal private static final long SEC_TO_USEC = 1000 * 1000;
        @CompilationFinal private static final long USEC_TO_NANO = 1000;
        @CompilationFinal private final long timeZoneOffsetMicroseconds;

        private AbstractClockPrimitiveNode(CompiledMethodObject method) {
            super(method);
            Calendar rightNow = Calendar.getInstance();
            timeZoneOffsetMicroseconds = (((long) rightNow.get(Calendar.ZONE_OFFSET)) + rightNow.get(Calendar.DST_OFFSET)) * 1000;
        }

        protected long currentMicrosecondsUTC() {
            Instant now = Instant.now();
            return now.getEpochSecond() * SEC_TO_USEC + now.getNano() / USEC_TO_NANO + EPOCH_DELTA_MICROSECONDS;
        }

        protected long currentMicrosecondsLocal() {
            return currentMicrosecondsUTC() + timeZoneOffsetMicroseconds;
        }
    }

    private static abstract class AbstractSignalAtPrimitiveNode extends AbstractPrimitiveNode {

        protected AbstractSignalAtPrimitiveNode(CompiledMethodObject method) {
            super(method);
        }

        protected void signalAtMilliseconds(BaseSqueakObject semaphore, long msTime) {
            if (semaphore.isSpecialKindAt(SPECIAL_OBJECT_INDEX.ClassSemaphore)) {
                code.image.registerSemaphore(semaphore, SPECIAL_OBJECT_INDEX.TheTimerSemaphore);
                code.image.interrupt.nextWakeupTick(msTime);
            } else {
                code.image.registerSemaphore(code.image.nil, SPECIAL_OBJECT_INDEX.TheTimerSemaphore);
                code.image.interrupt.nextWakeupTick(0);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 77)
    protected static abstract class PrimSomeInstanceNode extends AbstractPrimitiveNode {

        protected PrimSomeInstanceNode(CompiledMethodObject method) {
            super(method);
        }

        protected boolean isSmallIntegerClass(ClassObject classObject) {
            return classObject.equals(code.image.smallIntegerClass);
        }

        protected boolean isClassObject(ClassObject classObject) {
            return classObject.isClass();
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "isSmallIntegerClass(classObject)")
        protected ListObject allInstances(ClassObject classObject) {
            throw new PrimitiveFailed();
        }

        @Specialization(guards = "isClassObject(classObject)")
        protected BaseSqueakObject someInstance(ClassObject classObject) {
            try {
                return code.image.objects.someInstance(classObject).get(0);
            } catch (IndexOutOfBoundsException e) {
                throw new PrimitiveFailed();
            }
        }

        @SuppressWarnings("unused")
        @Fallback
        protected ListObject allInstances(Object object) {
            throw new PrimitiveFailed();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 121)
    protected static abstract class PrimImageNameNode extends AbstractPrimitiveNode {

        protected PrimImageNameNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected BaseSqueakObject get(@SuppressWarnings("unused") BaseSqueakObject receiver) {
            return code.image.wrap(code.image.config.getImagePath());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 124, numArguments = 2)
    protected static abstract class PrimLowSpaceSemaphoreNode extends AbstractPrimitiveNode {

        protected PrimLowSpaceSemaphoreNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected BaseSqueakObject get(BaseSqueakObject receiver, BaseSqueakObject semaphore) {
            code.image.registerSemaphore(semaphore, SPECIAL_OBJECT_INDEX.TheLowSpaceSemaphore);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 125, numArguments = 2)
    protected static abstract class PrimSetLowSpaceThresholdNode extends AbstractPrimitiveNode {

        protected PrimSetLowSpaceThresholdNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected BaseSqueakObject get(BaseSqueakObject receiver, @SuppressWarnings("unused") long numBytes) {
            // TODO: do something with numBytes
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 134, numArguments = 2)
    protected static abstract class PrimInterruptSemaphoreNode extends AbstractPrimitiveNode {

        protected PrimInterruptSemaphoreNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected BaseSqueakObject get(BaseSqueakObject receiver, BaseSqueakObject semaphore) {
            code.image.registerSemaphore(semaphore, SPECIAL_OBJECT_INDEX.TheInterruptSemaphore);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 135)
    protected static abstract class PrimMillisecondClockNode extends AbstractPrimitiveNode {

        protected PrimMillisecondClockNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected long doClock(@SuppressWarnings("unused") ClassObject receiver) {
            return code.image.wrap(System.currentTimeMillis() - code.image.startUpMillis);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 136, numArguments = 3)
    protected static abstract class PrimSignalAtMillisecondsNode extends AbstractSignalAtPrimitiveNode {

        protected PrimSignalAtMillisecondsNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected BaseSqueakObject doSignal(BaseSqueakObject receiver, BaseSqueakObject semaphore, long msTime) {
            signalAtMilliseconds(semaphore, msTime);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 137)
    protected static abstract class PrimSecondClockNode extends AbstractClockPrimitiveNode {

        protected PrimSecondClockNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected long doClock(@SuppressWarnings("unused") ClassObject receiver) {
            return code.image.wrap(currentMicrosecondsLocal() / 1000000);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 141, variableArguments = true)
    protected static abstract class PrimClipboardTextNode extends AbstractPrimitiveNode {
        @CompilationFinal private final boolean isHeadless = GraphicsEnvironment.isHeadless();
        private String headlessClipboardContents = "";

        protected PrimClipboardTextNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object doClipboard(Object[] rcvrAndArgs) {
            if (rcvrAndArgs.length == 1) {
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
            } else if (rcvrAndArgs.length == 2 && (rcvrAndArgs[1] instanceof NativeObject)) {
                String text = ((NativeObject) rcvrAndArgs[1]).toString();
                if (!isHeadless) {
                    StringSelection selection = new StringSelection(text);
                    getClipboard().setContents(selection, selection);
                } else {
                    headlessClipboardContents = text;
                }
                return rcvrAndArgs[1];
            }
            throw new PrimitiveFailed();
        }

        private static Clipboard getClipboard() {
            return Toolkit.getDefaultToolkit().getSystemClipboard();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 142)
    protected static abstract class PrimVMPathNode extends AbstractPrimitiveNode {

        protected PrimVMPathNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected BaseSqueakObject goVMPath(@SuppressWarnings("unused") BaseSqueakObject receiver) {
            return code.image.wrap(System.getProperty("java.home") + File.separatorChar);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 145, numArguments = 2)
    protected static abstract class PrimConstantFillNode extends AbstractPrimitiveNode {

        protected PrimConstantFillNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected BaseSqueakObject doFill(NativeObject receiver, Object value) {
            receiver.fillWith(value);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 148)
    protected static abstract class PrimShallowCopyNode extends AbstractPrimitiveNode {
        protected PrimShallowCopyNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object doDouble(final double value) {
            return new FloatObject(code.image, value);
        }

        @Specialization
        protected Object doSqueakObject(BaseSqueakObject receiver) {
            return receiver.shallowCopy();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 149, numArguments = 2)
    protected static abstract class PrimSystemAttributeNode extends AbstractPrimitiveNode {
        protected PrimSystemAttributeNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary
        protected Object getSystemAttribute(@SuppressWarnings("unused") Object image, long longIndex) {
            int index = (int) longIndex;
            if (index == 0) {
                String separator = System.getProperty("file.separator");
                return code.image.wrap(System.getProperty("java.home") + separator + "bin" + separator + "java");
            } else if (index == 1) {
                return code.image.wrap(code.image.config.getImagePath());
            }
            if (index >= 2 && index <= 1000) {
                String[] restArgs = code.image.config.getRestArgs();
                if (restArgs.length > index - 2) {
                    return code.image.wrap(restArgs[index - 2]);
                } else {
                    return code.image.nil;
                }
            }
            switch (index) {
                case 1001:
                    return code.image.wrap(code.image.os.getSqOSName());
                case 1002:
                    return code.image.wrap(System.getProperty("os.version"));
                case 1003:
                    return code.image.wrap("intel");
                case 1004:
                    return code.image.wrap(System.getProperty("java.version"));
                case 1201:
                    if (code.image.os.isMacOS()) {
                        return code.image.wrap("255");
                    }
            }
            return code.image.nil;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 176)
    protected static abstract class PrimMaxIdentityHashNode extends AbstractPrimitiveNode {
        protected PrimMaxIdentityHashNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object copy(@SuppressWarnings("unused") BaseSqueakObject receiver) {
            return code.image.wrap(Math.pow(2, 22) - 1);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 177)
    protected static abstract class PrimAllInstancesNode extends AbstractPrimitiveNode {

        protected PrimAllInstancesNode(CompiledMethodObject method) {
            super(method);
        }

        protected boolean hasNoInstances(ClassObject classObject) {
            return code.image.objects.getClassesWithNoInstances().contains(classObject);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "hasNoInstances(classObject)")
        protected ListObject noInstances(ClassObject classObject) {
            return code.image.newList(new Object[0]);
        }

        @Specialization
        protected ListObject allInstances(ClassObject classObject) {
            return code.image.newList(code.image.objects.allInstances(classObject).toArray());
        }

        @SuppressWarnings("unused")
        @Fallback
        protected ListObject allInstances(Object object) {
            throw new PrimitiveFailed();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 240)
    protected static abstract class PrimUTCClockNode extends AbstractClockPrimitiveNode {

        protected PrimUTCClockNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected long time(@SuppressWarnings("unused") Object receiver) {
            return currentMicrosecondsUTC();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 241)
    protected static abstract class PrimLocalMicrosecondsClockNode extends AbstractClockPrimitiveNode {

        protected PrimLocalMicrosecondsClockNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected long time(@SuppressWarnings("unused") Object receiver) {
            return currentMicrosecondsLocal();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 242, numArguments = 3)
    protected static abstract class PrimSignalAtUTCMicrosecondsNode extends AbstractSignalAtPrimitiveNode {

        protected PrimSignalAtUTCMicrosecondsNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected BaseSqueakObject doSignal(BaseSqueakObject receiver, BaseSqueakObject semaphore, long usecsUTC) {
            long msTime = (usecsUTC - AbstractClockPrimitiveNode.EPOCH_DELTA_MICROSECONDS) / 1000;
            signalAtMilliseconds(semaphore, msTime);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 254, variableArguments = true)
    protected static abstract class PrimVMParametersNode extends AbstractPrimitiveNode {
        protected PrimVMParametersNode(CompiledMethodObject method) {
            super(method);
        }

        @Override
        public final Object executeWithArguments(VirtualFrame frame, Object... rcvrAndArgs) {
            return getVMParameters(rcvrAndArgs);
        }

        @Specialization
        protected Object getVMParameters(Object[] rcvrAndArgs) {
            int paramsArraySize = 71;
            /**
             * Behaviour depends on argument count:
             *
             * <pre>
             * 0 args: return an Array of VM parameter values;
             * 1 arg:  return the indicated VM parameter;
             * 2 args: set the VM indicated parameter.
             * </pre>
             */
            switch (rcvrAndArgs.length) {
                case 1:
                    Object[] vmParameters = new Object[paramsArraySize];
                    for (int i = 0; i < paramsArraySize; i++) {
                        vmParameters[i] = vmParameterAt(i);
                    }
                    return code.image.newList(vmParameters);
                case 2:
                    int index;
                    try {
                        index = ((Long) rcvrAndArgs[1]).intValue();
                    } catch (ClassCastException e) {
                        throw new PrimitiveFailed();
                    }
                    if (index < 1 || index > paramsArraySize) {
                        throw new PrimitiveFailed();
                    } else {
                        return vmParameterAt(index);
                    }
                case 3:
                    return code.image.nil; // ignore writes
                default:
                    throw new PrimitiveFailed();
            }
        }

        private Object vmParameterAt(int index) {
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
                case 26: return InterruptHandlerNode.interruptChecksEveryNms; // interruptChecksEveryNms - force an ioProcessEvents every N milliseconds (rw)
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
            }
            //@formatter:on
            return 0L;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 255, numArguments = 2)
    protected static abstract class PrimMetaFailNode extends AbstractPrimitiveNode {
        public PrimMetaFailNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object doFail(@SuppressWarnings("unused") final PointersObject proxy, final long reasonCode) {
            throw new SimulationPrimitiveFailed(reasonCode);
        }
    }

    /*
     * A simulation primitive is neither a SqueakPrimitive nor a GenerateNodeFactory. Instead, it is
     * directly used by PrimitiveNodeFactory.
     */
    public static abstract class AbstractSimulationPrimitiveNode extends AbstractPrimitiveNode {
        @CompilationFinal public static final String SIMULATE_PRIMITIVE_SELECTOR = "simulatePrimitive:args:";
        @CompilationFinal protected static CompiledMethodObject simulationMethod;
        @CompilationFinal protected final String moduleName;
        @CompilationFinal protected final NativeObject functionName;
        @CompilationFinal protected final boolean bitBltSimulationNotFound = code.image.simulatePrimitiveArgs == code.image.nil;
        @Child protected LookupNode lookupNode = LookupNode.create();
        @Child protected DispatchNode dispatchNode = DispatchNode.create();
        @Child protected GetOrCreateContextNode getOrCreateContextNode;

        protected AbstractSimulationPrimitiveNode(CompiledMethodObject method, String moduleName, String functionName) {
            super(method);
            this.moduleName = moduleName;
            this.functionName = code.image.wrap(functionName);
            getOrCreateContextNode = GetOrCreateContextNode.create(method);
        }

        @Override
        public final Object executeWithArguments(VirtualFrame frame, Object... rcvrAndArgs) {
            return doSimulation(frame, rcvrAndArgs);
        }

        @Specialization
        protected Object doSimulation(VirtualFrame frame, Object[] rcvrAndArguments) {
            Object receiver = rcvrAndArguments[0];
            Object[] arguments = new Object[rcvrAndArguments.length - 1];
            for (int i = 0; i < arguments.length; i++) {
                arguments[i] = rcvrAndArguments[1 + i];
            }
            Object[] newRcvrAndArgs = new Object[]{receiver, functionName, code.image.newList(arguments)};
            code.image.interrupt.setDisabled(true);
            try {
                return dispatchNode.executeDispatch(frame, getSimulateMethod(receiver), newRcvrAndArgs, getContextOrMarker(frame));
            } catch (SimulationPrimitiveFailed e) {
                // TODO: put error into `ec`?
                // if (e.getReason() != 0) {
                // ContextObject thisContext = getOrCreateContextNode.executeGet(frame, true);
                // thisContext.atTempPut(0, code.image.lookupError(e.getReason()));
                // }
                throw new PrimitiveFailed();
            } finally {
                code.image.interrupt.setDisabled(false);
            }
        }

        protected CompiledMethodObject getSimulateMethod(Object receiver) {
            if (simulationMethod == null) {
                if (bitBltSimulationNotFound) {
                    throw new PrimitiveFailed();
                }
                Object lookupResult = lookupSimulationMethod(receiver);
                if (lookupResult instanceof CompiledMethodObject) {
                    CompiledMethodObject result = (CompiledMethodObject) lookupResult;
                    if (!result.isDoesNotUnderstand()) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        simulationMethod = result;
                        return result;
                    }
                }
                throw new SqueakException("Unable to find " + code.image.simulatePrimitiveArgs + " in " + receiver);
            }
            return simulationMethod;
        }

        protected Object lookupSimulationMethod(@SuppressWarnings("unused") Object receiver) {
            throw new SqueakException("lookupSimulationMethod must be overriden");
        }
    }

    public static abstract class PrimBitBltSimulateNode extends AbstractSimulationPrimitiveNode {
        @Child protected SqueakLookupClassNode lookupClassNode;

        public static PrimBitBltSimulateNode create(CompiledMethodObject method, String moduleName, String functionName, SqueakNode[] arguments) {
            return PrimBitBltSimulateNodeGen.create(method, moduleName, functionName, arguments);
        }

        protected PrimBitBltSimulateNode(CompiledMethodObject method, String moduleName, String functionName) {
            super(method, moduleName, functionName);
            lookupClassNode = SqueakLookupClassNode.create(method.image);
        }

        @Override
        protected Object lookupSimulationMethod(Object receiver) {
            ClassObject rcvrClass = lookupClassNode.executeLookup(receiver);
            return lookupNode.executeLookup(rcvrClass, code.image.simulatePrimitiveArgs);
        }
    }

    public static abstract class PrimBalloonEngineSimulateNode extends AbstractSimulationPrimitiveNode {

        public static PrimBalloonEngineSimulateNode create(CompiledMethodObject method, String moduleName, String functionName, SqueakNode[] arguments) {
            return PrimBalloonEngineSimulateNodeGen.create(method, moduleName, functionName, arguments);
        }

        protected PrimBalloonEngineSimulateNode(CompiledMethodObject method, String moduleName, String functionName) {
            super(method, moduleName, functionName);
        }

        @Override
        protected Object lookupSimulationMethod(Object receiver) {
            return lookupNode.executeLookup(receiver, code.image.simulatePrimitiveArgs);
        }
    }
}
