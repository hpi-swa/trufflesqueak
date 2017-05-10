package de.hpi.swa.trufflesqueak.nodes.bytecodes.jump;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.FalseObject;
import de.hpi.swa.trufflesqueak.model.TrueObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

@NodeChildren({@NodeChild(value = "cond", type = SqueakNode.class)})
public abstract class IfTrue extends SqueakNode {
    @Specialization
    public boolean checkCondition(boolean cond) {
        return cond;
    }

    @Specialization
    public boolean checkCondition(@SuppressWarnings("unused") FalseObject cond) {
        return false;
    }

    @Specialization
    public boolean checkCondition(@SuppressWarnings("unused") TrueObject cond) {
        return true;
    }

    @Fallback
    public Object checkCondition(@SuppressWarnings("unused") Object cond) {
        return null;
    }
}
