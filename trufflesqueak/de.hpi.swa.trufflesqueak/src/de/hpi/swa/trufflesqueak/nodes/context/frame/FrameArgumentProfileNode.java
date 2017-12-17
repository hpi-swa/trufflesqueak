package de.hpi.swa.trufflesqueak.nodes.context.frame;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.PrimitiveValueProfile;
import com.oracle.truffle.api.profiles.ValueProfile;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

@NodeInfo(cost = NodeCost.NONE)
public class FrameArgumentProfileNode extends SqueakNode {
    @Child private SqueakNode argumentNode;
    @CompilationFinal private final ConditionProfile objectProfile = ConditionProfile.createBinaryProfile();
    @CompilationFinal private final ValueProfile primitiveProfile = PrimitiveValueProfile.createEqualityProfile();
    @CompilationFinal private final ValueProfile classProfile = ValueProfile.createClassProfile();

    public FrameArgumentProfileNode(SqueakNode argNode) {
        argumentNode = argNode;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        final Object value = argumentNode.executeGeneric(frame);
        if (objectProfile.profile(value instanceof BaseSqueakObject)) {
            return classProfile.profile(value);
        } else {
            return primitiveProfile.profile(value);
        }
    }
}
