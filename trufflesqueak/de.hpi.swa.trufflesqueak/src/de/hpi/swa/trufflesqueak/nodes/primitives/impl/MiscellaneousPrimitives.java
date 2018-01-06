package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;

public class MiscellaneousPrimitives extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return MiscellaneousPrimitivesFactory.getFactories();
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
        protected BaseSqueakObject get(BaseSqueakObject receiver, @SuppressWarnings("unused") int numBytes) {
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
        protected BaseSqueakObject doClock(@SuppressWarnings("unused") ClassObject receiver) {
            return code.image.wrap(System.currentTimeMillis() - code.image.startUpTime);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 136, numArguments = 3)
    protected static abstract class PrimSignalAtMillisecondsNode extends AbstractPrimitiveNode {

        protected PrimSignalAtMillisecondsNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected BaseSqueakObject doSignal(BaseSqueakObject receiver, BaseSqueakObject semaphore, int msTime) {
            if (semaphore.isSpecialKindAt(SPECIAL_OBJECT_INDEX.ClassSemaphore)) {
                code.image.registerSemaphore(semaphore, SPECIAL_OBJECT_INDEX.TheTimerSemaphore);
                code.image.interrupt.nextWakeupTick(msTime);
            } else {
                code.image.registerSemaphore(code.image.nil, SPECIAL_OBJECT_INDEX.TheTimerSemaphore);
                code.image.interrupt.nextWakeupTick(0);
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 141, variableArguments = true)
    protected static abstract class PrimClipboardTextNode extends AbstractPrimitiveNode {

        protected PrimClipboardTextNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object doClipboard(Object[] rcvrAndArgs) {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            if (rcvrAndArgs.length == 1) {
                String text;
                try {
                    text = (String) code.image.wrap(clipboard.getData(DataFlavor.stringFlavor));
                } catch (UnsupportedFlavorException | IOException | IllegalStateException e) {
                    text = "";
                }
                return code.image.wrap(text);
            } else if (rcvrAndArgs.length == 2 && (rcvrAndArgs[1] instanceof NativeObject)) {
                String text = ((NativeObject) rcvrAndArgs[1]).toString();
                StringSelection selection = new StringSelection(text);
                clipboard.setContents(selection, selection);
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
        protected Object getSystemAttribute(@SuppressWarnings("unused") Object image, int index) {
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
    protected static abstract class PrimUTCClockNode extends AbstractPrimitiveNode {
        // The Delta between Squeak Epoch (Jan 1st 1901) and POSIX Epoch (Jan 1st 1970)
        private final long SQUEAK_EPOCH_DELTA_MICROSECONDS = 2177452800000000L;
        private final long SEC2USEC = 1000 * 1000;
        private final long USEC2NANO = 1000;

        protected PrimUTCClockNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected long time(@SuppressWarnings("unused") Object receiver) {
            Instant now = Instant.now();
            long epochSecond = now.getEpochSecond();
            int nano = now.getNano();
            return epochSecond * SEC2USEC + nano / USEC2NANO + SQUEAK_EPOCH_DELTA_MICROSECONDS;
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
            int numRcvrAndArgs = rcvrAndArgs.length;
            if (numRcvrAndArgs == 1) {
                Object[] vmParameters = new Object[71];
                Arrays.fill(vmParameters, code.image.wrap(0));
                return code.image.newList(vmParameters);
            }
            int index;
            try {
                index = (int) rcvrAndArgs[1];
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
}
