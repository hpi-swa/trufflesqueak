package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

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
    public static abstract class PrimSomeInstanceNode extends AbstractPrimitiveNode {

        public PrimSomeInstanceNode(CompiledMethodObject method) {
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
        ListObject allInstances(ClassObject classObject) {
            throw new PrimitiveFailed();
        }

        @Specialization(guards = "isClassObject(classObject)")
        BaseSqueakObject someInstance(ClassObject classObject) {
            try {
                return code.image.objects.someInstance(classObject).get(0);
            } catch (IndexOutOfBoundsException e) {
                throw new PrimitiveFailed();
            }
        }

        @SuppressWarnings("unused")
        @Fallback
        ListObject allInstances(Object object) {
            throw new PrimitiveFailed();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 121)
    public static abstract class PrimImageNameNode extends AbstractPrimitiveNode {

        public PrimImageNameNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        BaseSqueakObject get(@SuppressWarnings("unused") BaseSqueakObject receiver) {
            return code.image.wrap(code.image.config.getImagePath());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 124, numArguments = 2)
    public static abstract class PrimLowSpaceSemaphoreNode extends AbstractPrimitiveNode {

        public PrimLowSpaceSemaphoreNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        BaseSqueakObject get(BaseSqueakObject receiver, BaseSqueakObject semaphore) {
            code.image.registerSemaphore(semaphore, SPECIAL_OBJECT_INDEX.TheLowSpaceSemaphore);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 125, numArguments = 2)
    public static abstract class PrimSetLowSpaceThresholdNode extends AbstractPrimitiveNode {

        public PrimSetLowSpaceThresholdNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        BaseSqueakObject get(BaseSqueakObject receiver, @SuppressWarnings("unused") int numBytes) {
            // TODO: do something with numBytes
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 134, numArguments = 2)
    public static abstract class PrimInterruptSemaphoreNode extends AbstractPrimitiveNode {

        public PrimInterruptSemaphoreNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        BaseSqueakObject get(BaseSqueakObject receiver, BaseSqueakObject semaphore) {
            code.image.registerSemaphore(semaphore, SPECIAL_OBJECT_INDEX.TheInterruptSemaphore);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 148)
    public static abstract class PrimShallowCopyNode extends AbstractPrimitiveNode {
        public PrimShallowCopyNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        Object copy(BaseSqueakObject self) {
            return self.shallowCopy();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 149, numArguments = 2)
    public static abstract class PrimSystemAttributeNode extends AbstractPrimitiveNode {
        public PrimSystemAttributeNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary
        public Object getSystemAttribute(@SuppressWarnings("unused") Object image, int index) {
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
    @SqueakPrimitive(index = 177)
    public static abstract class PrimAllInstancesNode extends AbstractPrimitiveNode {

        public PrimAllInstancesNode(CompiledMethodObject method) {
            super(method);
        }

        protected boolean hasNoInstances(ClassObject classObject) {
            return code.image.objects.getClassesWithNoInstances().contains(classObject);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "hasNoInstances(classObject)")
        ListObject noInstances(ClassObject classObject) {
            return code.image.wrap(new Object[0]);
        }

        @Specialization
        ListObject allInstances(ClassObject classObject) {
            return code.image.wrap(code.image.objects.allInstances(classObject).toArray());
        }

        @SuppressWarnings("unused")
        @Fallback
        ListObject allInstances(Object object) {
            throw new PrimitiveFailed();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 240)
    public static abstract class PrimUTCClockNode extends AbstractPrimitiveNode {
        // The Delta between Squeak Epoch (Jan 1st 1901) and POSIX Epoch (Jan 1st 1970)
        private final long SQUEAK_EPOCH_DELTA_MICROSECONDS = 2177452800000000L;
        private final long SEC2USEC = 1000 * 1000;
        private final long USEC2NANO = 1000;

        public PrimUTCClockNode(CompiledMethodObject method) {
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
    public static abstract class PrimVMParametersNode extends AbstractPrimitiveNode {
        public PrimVMParametersNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object doTwoArguments(Object[] rcvrAndArgs) {
            int numRcvrAndArgs = rcvrAndArgs.length;
            Object[] vmParameters = new Object[71];
            Arrays.fill(vmParameters, code.image.wrap(0));
            if (numRcvrAndArgs == 1) {
                return code.image.wrap(vmParameters);
            }
            int index;
            try {
                index = (int) rcvrAndArgs[1];
            } catch (ClassCastException e) {
                throw new PrimitiveFailed();
            }
            if (numRcvrAndArgs <= 3) {
                // when two args are provided, do nothing and return old value
                return vmParameters[index];
            }
            throw new PrimitiveFailed();
        }
    }
}
