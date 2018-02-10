package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
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
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.trufflesqueak.nodes.DispatchNode;
import de.hpi.swa.trufflesqueak.nodes.LookupNode;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.context.SqueakLookupClassNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.MiscellaneousPrimitivesFactory.PrimBitBltSimulateNodeGen;

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
                code.image.interrupt.nextWakeupTick((int) msTime);
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
        @CompilationFinal private boolean isHeadless = false;
        private String headlessClipboardContents = "";

        protected PrimClipboardTextNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object doClipboard(Object[] rcvrAndArgs) {
            Clipboard clipboard = null;
            try {
                clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            } catch (HeadlessException e) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                isHeadless = true;
            }
            if (rcvrAndArgs.length == 1) {
                String text;
                if (isHeadless) {
                    text = headlessClipboardContents;
                } else {
                    try {
                        text = (String) clipboard.getData(DataFlavor.stringFlavor);
                    } catch (UnsupportedFlavorException | IOException | IllegalStateException e) {
                        text = "";
                    }
                }
                return code.image.wrap(text);
            } else if (rcvrAndArgs.length == 2 && (rcvrAndArgs[1] instanceof NativeObject)) {
                String text = ((NativeObject) rcvrAndArgs[1]).toString();
                if (isHeadless) {
                    headlessClipboardContents = text;
                } else {
                    StringSelection selection = new StringSelection(text);
                    clipboard.setContents(selection, selection);
                }
                return rcvrAndArgs[1];
            }
            throw new PrimitiveFailed();
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
            return code.image.wrap(System.getProperty("java.home"));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 148)
    protected static abstract class PrimShallowCopyNode extends AbstractPrimitiveNode {
        protected PrimShallowCopyNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object copy(BaseSqueakObject self) {
            return self.shallowCopy();
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
            long msTime = usecsUTC / 1000 + AbstractClockPrimitiveNode.EPOCH_DELTA_MICROSECONDS - code.image.startUpMillis;
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

        @Specialization
        protected Object doTwoArguments(Object[] rcvrAndArgs) {
            long numRcvrAndArgs = rcvrAndArgs.length;
            if (numRcvrAndArgs == 1) {
                long[] vmParameters = new long[71];
                return code.image.wrap(vmParameters);
            }
            long index;
            try {
                index = (long) rcvrAndArgs[1];
            } catch (ClassCastException e) {
                throw new PrimitiveFailed();
            }
            if (numRcvrAndArgs <= 3) {
                // when two args are provided, do nothing and return old value
                return code.image.wrap(0);
            }
            throw new PrimitiveFailed();
        }
    }

    /*
     * The BitBlt simulation primitive is neither a SqueakPrimitive nor a GenerateNodeFactory. Instead,
     * it is directly used by PrimitiveNodeFactory.
     */
    public static abstract class PrimBitBltSimulateNode extends AbstractPrimitiveNode {
        @CompilationFinal private static final String SIMULATE_PRIMITIVE_SELECTOR = "simulatePrimitive:args:";
        @CompilationFinal private static CompiledMethodObject simulationMethod;
        @CompilationFinal private final String moduleName;
        @CompilationFinal private final String functionName;
        @Child private LookupNode lookupNode = simulationMethod == null ? LookupNode.create() : null;
        @Child private DispatchNode dispatchNode = simulationMethod == null ? DispatchNode.create() : null;
        @Child protected SqueakLookupClassNode lookupClassNode;

        public static PrimBitBltSimulateNode create(CompiledMethodObject method, String moduleName, String functionName, SqueakNode[] arguments) {
            return PrimBitBltSimulateNodeGen.create(method, moduleName, functionName, arguments);
        }

        protected PrimBitBltSimulateNode(CompiledMethodObject method, String moduleName, String functionName) {
            super(method);
            this.moduleName = moduleName;
            this.functionName = functionName;
            lookupClassNode = simulationMethod == null ? SqueakLookupClassNode.create(code) : null;
        }

        @Specialization
        protected Object doSimulation(VirtualFrame frame, Object[] rcvrAndArguments) {
            if (simulationMethod == null) {
                simulationMethod = getSimulateMethod(rcvrAndArguments[0]);
            }
            Object[] newRcvrAndArgs = new Object[]{rcvrAndArguments[0], code.image.wrap(functionName), code.image.newList(rcvrAndArguments)};
            return dispatchNode.executeDispatch(frame, simulationMethod, newRcvrAndArgs, getContextOrMarker(frame));
        }

        private CompiledMethodObject getSimulateMethod(Object receiver) { // TODO: cache method for a given module name
            ClassObject rcvrClass = lookupClassNode.executeLookup(receiver);
            Object lookupResult = lookupNode.executeLookup(rcvrClass, code.image.wrap(SIMULATE_PRIMITIVE_SELECTOR));
            if (lookupResult instanceof CompiledMethodObject) {
                CompiledMethodObject result = (CompiledMethodObject) lookupResult;
                if (!result.isDoesNotUnderstand()) {
                    return result;
                }
            }
            throw new PrimitiveFailed();
        }
    }
}
