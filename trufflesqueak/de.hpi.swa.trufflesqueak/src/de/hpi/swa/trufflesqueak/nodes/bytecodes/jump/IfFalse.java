package de.hpi.swa.trufflesqueak.nodes.bytecodes.jump;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

@NodeChildren({@NodeChild(value = "cond", type = SqueakNode.class)})
public abstract class IfFalse extends SqueakNode {
    @Specialization
    public boolean checkCondition(boolean cond) {
        return !cond;
    }

    @Specialization
    public Object checkCondition(Object cond) {
        if (cond instanceof Boolean) {
            return !(boolean) cond;
        }
        return null;
    }
}
