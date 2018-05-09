package de.hpi.swa.graal.squeak.nodes.context;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ValueProfile;

import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.nodes.SqueakObjectAt0Node;
import de.hpi.swa.graal.squeak.nodes.SqueakNode;

@NodeChild(value = "objectNode", type = SqueakNode.class)
public abstract class ObjectAtNode extends AbstractObjectAtNode {
    @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();
    @CompilationFinal private final ValueProfile classProfile = ValueProfile.createClassProfile();
    @CompilationFinal private final long index;

    public static ObjectAtNode create(final long i, final SqueakNode object) {
        return ObjectAtNodeGen.create(i, object);
    }

    protected ObjectAtNode(final long variableIndex) {
        index = variableIndex;
    }

    public abstract Object executeGeneric(VirtualFrame frame);

    @Specialization
    protected Object read(final NativeObject object) {
        return classProfile.profile(object).getNativeAt0(index);
    }

    @Specialization(guards = "!isNativeObject(object)")
    protected Object read(final AbstractSqueakObject object) {
        return at0Node.execute(classProfile.profile(object), index);
    }
}
