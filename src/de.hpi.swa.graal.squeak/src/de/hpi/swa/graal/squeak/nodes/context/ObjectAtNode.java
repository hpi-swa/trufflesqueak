package de.hpi.swa.graal.squeak.nodes.context;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ValueProfile;

import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.nodes.SqueakNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAt0Node;

@NodeChild(value = "objectNode", type = SqueakNode.class)
public abstract class ObjectAtNode extends AbstractObjectAtNode {
    @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();
    private final ValueProfile classProfile = ValueProfile.createClassProfile();
    private final long index;

    public static ObjectAtNode create(final long i, final SqueakNode object) {
        return ObjectAtNodeGen.create(i, object);
    }

    protected ObjectAtNode(final long variableIndex) {
        index = variableIndex;
    }

    public abstract Object executeGeneric(VirtualFrame frame);

    @Specialization(guards = "!isNativeObject(object)")
    protected final Object read(final AbstractSqueakObject object) {
        return at0Node.execute(classProfile.profile(object), index);
    }
}
