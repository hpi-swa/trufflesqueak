package de.hpi.swa.graal.squeak.nodes.context;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ValueProfile;

import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.nodes.SqueakNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAtPut0Node;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameSlotReadNode;

@NodeChild(value = "objectNode", type = SqueakNode.class)
@NodeChild(value = "valueNode", type = SqueakNode.class)
public abstract class ObjectAtPutNode extends AbstractObjectAtNode {
    @Child private SqueakObjectAtPut0Node atPut0Node = SqueakObjectAtPut0Node.create();
    @Child private FrameSlotReadNode contextOrMarkerReadNode = FrameSlotReadNode.createForContextOrMarker();

    private final ValueProfile classProfile = ValueProfile.createClassProfile();
    private final long index;

    public static ObjectAtPutNode create(final long index, final SqueakNode object, final SqueakNode value) {
        return ObjectAtPutNodeGen.create(index, object, value);
    }

    protected ObjectAtPutNode(final long variableIndex) {
        index = variableIndex;
    }

    public abstract void executeWrite(VirtualFrame frame);

    @Specialization(guards = {"!isNativeObject(object)", "!isFullyVirtualized(frame)"})
    protected final void doContext(final VirtualFrame frame, final AbstractSqueakObject object, final ContextObject value) {
        if (contextOrMarkerReadNode.executeRead(frame) == value) {
            value.markEscaped();
        }
        atPut0Node.execute(classProfile.profile(object), index, value);
    }

    @Specialization(guards = {"!isNativeObject(object)", "isFullyVirtualized(frame)"})
    protected final void doContextVirtualized(@SuppressWarnings("unused") final VirtualFrame frame, final AbstractSqueakObject object, final ContextObject value) {
        atPut0Node.execute(classProfile.profile(object), index, value);
    }

    @Specialization(guards = {"!isNativeObject(object)", "!isContextObject(value)"})
    protected final void doSqueakObject(final AbstractSqueakObject object, final Object value) {
        atPut0Node.execute(classProfile.profile(object), index, value);
    }

    protected static final boolean isContextObject(final Object value) {
        return value instanceof ContextObject;
    }

    protected final boolean isFullyVirtualized(final VirtualFrame frame) {
        // true if slot holds FrameMarker or null (when entering code object)
        return !(contextOrMarkerReadNode.executeRead(frame) instanceof ContextObject);
    }
}
