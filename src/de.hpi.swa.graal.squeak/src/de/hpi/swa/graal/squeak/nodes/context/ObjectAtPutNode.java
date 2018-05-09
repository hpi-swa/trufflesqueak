package de.hpi.swa.graal.squeak.nodes.context;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ValueProfile;

import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.nodes.SqueakObjectAtPut0Node;
import de.hpi.swa.graal.squeak.nodes.SqueakNode;

@NodeChild(value = "objectNode", type = SqueakNode.class)
@NodeChild(value = "valueNode", type = SqueakNode.class)
public abstract class ObjectAtPutNode extends AbstractObjectAtNode {
    @Child private SqueakObjectAtPut0Node atPut0Node = SqueakObjectAtPut0Node.create();
    @CompilationFinal private final ValueProfile classProfile = ValueProfile.createClassProfile();
    @CompilationFinal private final long index;

    public static ObjectAtPutNode create(final long index, final SqueakNode object, final SqueakNode value) {
        return ObjectAtPutNodeGen.create(index, object, value);
    }

    protected ObjectAtPutNode(final long variableIndex) {
        index = variableIndex;
    }

    public abstract void executeWrite(VirtualFrame frame);

    @Specialization
    protected final void doNativeObject(final NativeObject object, final long value) {
        classProfile.profile(object).setNativeAt0(index, value);
    }

    @Specialization(guards = "!isNativeObject(object)")
    protected final void doSqueakObject(final AbstractSqueakObject object, final Object value) {
        atPut0Node.execute(classProfile.profile(object), index, value);
    }

}
