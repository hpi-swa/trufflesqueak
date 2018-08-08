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
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NotProvided;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.DispatchNode;
import de.hpi.swa.graal.squeak.nodes.LookupNode;
import de.hpi.swa.graal.squeak.nodes.SqueakNode;
import de.hpi.swa.graal.squeak.nodes.accessing.CompiledCodeNodes.IsDoesNotUnderstandNode;
import de.hpi.swa.graal.squeak.nodes.context.ArgumentNode;
import de.hpi.swa.graal.squeak.nodes.context.SqueakLookupClassNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;

@GenerateNodeFactory
public abstract class SimulationPrimitiveNode extends AbstractPrimitiveNode {
    public static final byte[] SIMULATE_PRIMITIVE_SELECTOR = "simulatePrimitive:args:".getBytes();

    // different CompiledMethodObject per simulation
    @CompilationFinal protected CompiledMethodObject simulationMethod;

    private final NativeObject functionName;
    private final boolean bitBltSimulationNotFound = code.image.getSimulatePrimitiveArgsSelector() == null;
    private final PointersObject emptyList;
    private final BranchProfile simulationFailedProfile = BranchProfile.create();

    @Child private LookupNode lookupNode;
    @Child private DispatchNode dispatchNode = DispatchNode.create();
    @Child private SqueakLookupClassNode lookupClassNode;
    @Child private IsDoesNotUnderstandNode isDoesNotUnderstandNode;

    public static SimulationPrimitiveNode create(final CompiledMethodObject method, final String moduleName, final String functionName) {
        final NodeFactory<SimulationPrimitiveNode> nodeFactory = SimulationPrimitiveNodeFactory.getInstance();
        final int primitiveArity = nodeFactory.getExecutionSignature().size();
        final SqueakNode[] argumentNodes = new SqueakNode[primitiveArity];
        for (int j = 0; j < primitiveArity; j++) {
            argumentNodes[j] = ArgumentNode.create(method, j);
        }
        return nodeFactory.createNode(method, primitiveArity, moduleName, functionName, argumentNodes);
    }

    protected SimulationPrimitiveNode(final CompiledMethodObject method, final int numArguments, @SuppressWarnings("unused") final String moduleName, final String functionName) {
        super(method, numArguments);
        this.functionName = code.image.wrap(functionName);
        lookupNode = LookupNode.create(method.image);
        lookupClassNode = SqueakLookupClassNode.create(method.image);
        isDoesNotUnderstandNode = IsDoesNotUnderstandNode.create(method.image);
        emptyList = code.image.newList(new Object[]{});
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

    private Object doSimulation(final VirtualFrame frame, final Object receiver, final PointersObject arguments) {
        final Object[] newRcvrAndArgs = new Object[]{receiver, functionName, arguments};
        final boolean wasDisabled = code.image.interrupt.disabled();
        code.image.interrupt.disable();
        try {
            return dispatchNode.executeDispatch(frame, getSimulateMethod(receiver), newRcvrAndArgs, getContextOrMarker(frame));
        } catch (SimulationPrimitiveFailed e) {
            simulationFailedProfile.enter();
            throw new PrimitiveFailed(e.getReasonCode());
        } finally {
            if (wasDisabled) {
                code.image.interrupt.enable();
            }
        }
    }

    private CompiledMethodObject getSimulateMethod(final Object receiver) {
        if (simulationMethod == null) {
            if (bitBltSimulationNotFound) {
                throw new PrimitiveFailed();
            }
            final Object lookupResult; // TODO: Nodes!
            if (receiver instanceof ClassObject) {
                lookupResult = lookupNode.executeLookup(receiver, code.image.getSimulatePrimitiveArgsSelector());
            } else {
                final ClassObject rcvrClass = lookupClassNode.executeLookup(receiver);
                lookupResult = lookupNode.executeLookup(rcvrClass, code.image.getSimulatePrimitiveArgsSelector());
            }
            if (lookupResult instanceof CompiledMethodObject) {
                final CompiledMethodObject result = (CompiledMethodObject) lookupResult;
                if (!isDoesNotUnderstandNode.execute(result)) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    simulationMethod = result;
                    return result;
                }
            }
            throw new SqueakException("Unable to find simulationMethod.");
        }
        return simulationMethod;
    }
}
