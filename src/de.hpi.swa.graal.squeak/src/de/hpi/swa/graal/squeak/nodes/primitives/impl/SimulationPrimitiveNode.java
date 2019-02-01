package de.hpi.swa.graal.squeak.nodes.primitives.impl;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.SimulationPrimitiveFailed;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NotProvided;
import de.hpi.swa.graal.squeak.nodes.DispatchNode;
import de.hpi.swa.graal.squeak.nodes.LookupMethodNode;
import de.hpi.swa.graal.squeak.nodes.SqueakNode;
import de.hpi.swa.graal.squeak.nodes.context.ArgumentNode;
import de.hpi.swa.graal.squeak.nodes.context.LookupClassNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.SeptenaryPrimitive;

@GenerateNodeFactory
public abstract class SimulationPrimitiveNode extends AbstractPrimitiveNode implements SeptenaryPrimitive {
    public static final byte[] SIMULATE_PRIMITIVE_SELECTOR = "simulatePrimitive:args:".getBytes();

    // different CompiledMethodObject per simulation
    @CompilationFinal protected CompiledMethodObject simulationMethod;

    private final NativeObject functionName;
    private final ArrayObject emptyList;
    private final BranchProfile simulationFailedProfile = BranchProfile.create();

    @Child private DispatchNode dispatchNode = DispatchNode.create();

    protected SimulationPrimitiveNode(final CompiledMethodObject method, @SuppressWarnings("unused") final String moduleName, final String functionName) {
        super(method);
        this.functionName = code.image.wrap(functionName);
        emptyList = code.image.newList(new Object[]{});
    }

    public static SimulationPrimitiveNode create(final CompiledMethodObject method, final String moduleName, final String functionName) {
        final NodeFactory<SimulationPrimitiveNode> nodeFactory = SimulationPrimitiveNodeFactory.getInstance();
        final int primitiveArity = nodeFactory.getExecutionSignature().size();
        final SqueakNode[] argumentNodes = new SqueakNode[primitiveArity];
        for (int j = 0; j < primitiveArity; j++) {
            argumentNodes[j] = ArgumentNode.create(method, j);
        }
        return nodeFactory.createNode(method, primitiveArity, moduleName, functionName, argumentNodes);
    }

    @SuppressWarnings("unused")
    @Specialization
    protected final Object doSimulation(final VirtualFrame frame, final Object receiver,
                    final NotProvided arg1, final NotProvided arg2, final NotProvided arg3, final NotProvided arg4, final NotProvided arg5, final NotProvided arg6) {
        return doSimulation(frame, receiver, emptyList);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isNotProvided(arg1)"})
    protected final Object doSimulation(final VirtualFrame frame, final Object receiver,
                    final Object arg1, final NotProvided arg2, final NotProvided arg3, final NotProvided arg4, final NotProvided arg5, final NotProvided arg6) {
        return doSimulation(frame, receiver, code.image.newList(new Object[]{arg1}));
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isNotProvided(arg1)", "!isNotProvided(arg2)"})
    protected final Object doSimulation(final VirtualFrame frame, final Object receiver,
                    final Object arg1, final Object arg2, final NotProvided arg3, final NotProvided arg4, final NotProvided arg5, final NotProvided arg6) {
        return doSimulation(frame, receiver, code.image.newList(new Object[]{arg1, arg2}));
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isNotProvided(arg1)", "!isNotProvided(arg2)", "!isNotProvided(arg3)"})
    protected final Object doSimulation(final VirtualFrame frame, final Object receiver,
                    final Object arg1, final Object arg2, final Object arg3, final NotProvided arg4, final NotProvided arg5, final NotProvided arg6) {
        return doSimulation(frame, receiver, code.image.newList(new Object[]{arg1, arg2, arg3}));
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isNotProvided(arg1)", "!isNotProvided(arg2)", "!isNotProvided(arg3)", "!isNotProvided(arg4)"})
    protected final Object doSimulation(final VirtualFrame frame, final Object receiver,
                    final Object arg1, final Object arg2, final Object arg3, final Object arg4, final NotProvided arg5, final NotProvided arg6) {
        return doSimulation(frame, receiver, code.image.newList(new Object[]{arg1, arg2, arg3, arg4}));
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!isNotProvided(arg1)", "!isNotProvided(arg2)", "!isNotProvided(arg3)", "!isNotProvided(arg4)", "!isNotProvided(arg5)"})
    protected final Object doSimulation(final VirtualFrame frame, final Object receiver,
                    final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final NotProvided arg6) {
        return doSimulation(frame, receiver, code.image.newList(new Object[]{arg1, arg2, arg3, arg4, arg5}));
    }

    @Specialization(guards = {"!isNotProvided(arg1)", "!isNotProvided(arg2)", "!isNotProvided(arg3)", "!isNotProvided(arg4)", "!isNotProvided(arg5)", "!isNotProvided(arg6)"})
    protected final Object doSimulation(final VirtualFrame frame, final Object receiver,
                    final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6) {
        return doSimulation(frame, receiver, code.image.newList(new Object[]{arg1, arg2, arg3, arg4, arg5, arg6}));
    }

    private Object doSimulation(final VirtualFrame frame, final Object receiver, final ArrayObject arguments) {
        final Object[] newRcvrAndArgs = new Object[]{receiver, functionName, arguments};
        try {
            final boolean wasActive = code.image.interrupt.isActive();
            code.image.interrupt.deactivate();
            try {
                return dispatchNode.executeDispatch(frame, getSimulateMethod(receiver), newRcvrAndArgs, getContextOrMarker(frame));
            } finally {
                if (wasActive) {
                    code.image.interrupt.activate();
                }
            }
        } catch (SimulationPrimitiveFailed e) {
            simulationFailedProfile.enter();
            throw new PrimitiveFailed(e.getReasonCode());
        }
    }

    private CompiledMethodObject getSimulateMethod(final Object receiver) {
        if (simulationMethod == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            assert code.image.getSimulatePrimitiveArgsSelector() != null : "BitBlt simulation not found in image.";
            final Object lookupResult; // TODO: Nodes!
            final LookupMethodNode lookupMethodNode = LookupMethodNode.create(code.image);
            if (receiver instanceof ClassObject) {
                lookupResult = lookupMethodNode.executeLookup(receiver, code.image.getSimulatePrimitiveArgsSelector());
            } else {
                final ClassObject rcvrClass = LookupClassNode.create(code.image).executeLookup(receiver);
                lookupResult = lookupMethodNode.executeLookup(rcvrClass, code.image.getSimulatePrimitiveArgsSelector());
            }
            if (lookupResult instanceof CompiledMethodObject) {
                final CompiledMethodObject result = (CompiledMethodObject) lookupResult;
                if (result != null) {
                    simulationMethod = result;
                    return result;
                }
            }
            throw new SqueakException("Unable to find simulationMethod.");
        }
        return simulationMethod;
    }
}
