package de.hpi.swa.graal.squeak.nodes.context;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;
import de.hpi.swa.graal.squeak.nodes.SqueakNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAtPut0Node;

/**
 * This node should only be used for stores into associations, receivers, and remote temps as it
 * also marks {@link ContextObject}s as escaped when stored.
 */
@NodeChild(value = "objectNode", type = SqueakNode.class)
@NodeChild(value = "valueNode", type = SqueakNode.class)
@NodeInfo(cost = NodeCost.NONE)
@ReportPolymorphism
public abstract class SqueakObjectAtPutAndMarkContextsNode extends AbstractNode {
    @Child private SqueakObjectAtPut0Node atPut0Node = SqueakObjectAtPut0Node.create();
    private final long index;

    protected SqueakObjectAtPutAndMarkContextsNode(final long variableIndex) {
        index = variableIndex;
    }

    public static SqueakObjectAtPutAndMarkContextsNode create(final long index, final SqueakNode object, final SqueakNode value) {
        return SqueakObjectAtPutAndMarkContextsNodeGen.create(index, object, value);
    }

    public abstract void executeWrite(VirtualFrame frame);

    @Specialization(guards = {"!isNativeObject(object)"})
    protected final void doContext(final AbstractSqueakObject object, final ContextObject value) {
        value.markEscaped();
        atPut0Node.execute(object, index, value);
    }

    @Specialization(guards = {"!isNativeObject(object)", "!isContextObject(value)"})
    protected final void doSqueakObject(final AbstractSqueakObject object, final Object value) {
        atPut0Node.execute(object, index, value);
    }
}
