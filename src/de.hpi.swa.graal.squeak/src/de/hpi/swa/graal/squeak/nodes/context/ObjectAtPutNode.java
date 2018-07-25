package de.hpi.swa.graal.squeak.nodes.context;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ValueProfile;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.nodes.SqueakNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAtPut0Node;

/**
 * This node should only be used for stores into associations, receivers, and remote temps as it
 * also marks {@link ContextObject}s as escaped when stored.
 */
@NodeChild(value = "objectNode", type = SqueakNode.class)
@NodeChild(value = "valueNode", type = SqueakNode.class)
public abstract class ObjectAtPutNode extends AbstractObjectAtNode {
    @Child private SqueakObjectAtPut0Node atPut0Node = SqueakObjectAtPut0Node.create();

    private final ValueProfile classProfile = ValueProfile.createClassProfile();
    private final long index;

    public static ObjectAtPutNode create(final long index, final SqueakNode object, final SqueakNode value) {
        return ObjectAtPutNodeGen.create(index, object, value);
    }

    protected ObjectAtPutNode(final long variableIndex) {
        index = variableIndex;
    }

    public abstract void executeWrite(VirtualFrame frame);

    @Specialization(guards = {"!isNativeObject(object)"})
    protected final void doContext(final AbstractSqueakObject object, final ContextObject value) {
        value.markEscaped();
        atPut0Node.execute(classProfile.profile(object), index, value);
    }

    @Specialization(guards = {"!isNativeObject(object)", "!isContextObject(value)"})
    protected final void doSqueakObject(final AbstractSqueakObject object, final Object value) {
        atPut0Node.execute(classProfile.profile(object), index, value);
    }

    @Fallback
    protected final void doFail(final Object object, final Object value) {
        throw new SqueakException(object, "at:", index, "put:", value, "failed");
    }
}
