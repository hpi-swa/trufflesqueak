package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.graal.squeak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.graal.squeak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.FrameMarker;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public final class ExecuteSendNode extends AbstractNodeWithCode {

    @Child private AbstractLookupClassNode lookupClassNode;
    @Child private LookupMethodNode lookupMethodNode = LookupMethodNode.create();
    @Child private DispatchSendNode dispatchSendNode;

    private final ConditionProfile noResultProfile = ConditionProfile.createBinaryProfile();
    private final BranchProfile nlrProfile = BranchProfile.create();
    private final BranchProfile nvrProfile = BranchProfile.create();

    private ExecuteSendNode(final CompiledCodeObject code, final boolean isSuper) {
        super(code);
        dispatchSendNode = DispatchSendNode.create(code);
        if (isSuper) {
            lookupClassNode = new LookupSuperClassNode(code);
        } else {
            lookupClassNode = new LookupClassNode();
        }
    }

    public static ExecuteSendNode create(final CompiledCodeObject code, final boolean isSuper) {
        return new ExecuteSendNode(code, isSuper);
    }

    public Object execute(final VirtualFrame frame, final NativeObject selector, final Object[] receiverAndArguments) {
        final ClassObject rcvrClass = lookupClassNode.executeLookup(receiverAndArguments[0]);
        final Object lookupResult = lookupMethodNode.executeLookup(rcvrClass, selector);
        try {
            final Object result = dispatchSendNode.executeSend(frame, selector, lookupResult, rcvrClass, receiverAndArguments);
            assert result != null : "Result of a message send should not be null";
            return result;
        } catch (final NonLocalReturn nlr) {
            nlrProfile.enter();
            if (nlr.getTargetContextOrMarker() == getMarker(frame) || nlr.getTargetContextOrMarker() == getContext(frame)) {
                return nlr.getReturnValue();
            } else {
                throw nlr;
            }
        } catch (final NonVirtualReturn nvr) {
            nvrProfile.enter();
            if (nvr.getTargetContext() == getContext(frame)) {
                return nvr.getReturnValue();
            } else {
                throw nvr;
            }
        }
    }

    protected FrameMarker getMarker(final VirtualFrame frame) {
        return FrameAccess.getMarker(frame, code);
    }

    protected abstract static class AbstractLookupClassNode extends AbstractNode {
        protected abstract ClassObject executeLookup(Object receiver);
    }

    @NodeInfo(cost = NodeCost.NONE)
    protected static final class LookupClassNode extends AbstractLookupClassNode {
        @Child private SqueakObjectClassNode lookupClassNode = SqueakObjectClassNode.create();

        @Override
        protected ClassObject executeLookup(final Object receiver) {
            return lookupClassNode.executeLookup(receiver);
        }
    }

    protected static final class LookupSuperClassNode extends AbstractLookupClassNode {
        @Child private AbstractPointersObjectReadNode readNode = AbstractPointersObjectReadNode.create();

        private final ConditionProfile hasSuperclassProfile = ConditionProfile.createBinaryProfile();
        private final CompiledMethodObject method;

        protected LookupSuperClassNode(final CompiledCodeObject code) {
            method = code.getMethod();
        }

        @Override
        protected ClassObject executeLookup(final Object receiver) {
            final ClassObject methodClass = method.getMethodClass(readNode);
            final ClassObject superclass = methodClass.getSuperclassOrNull();
            return hasSuperclassProfile.profile(superclass == null) ? methodClass : superclass;
        }
    }
}
