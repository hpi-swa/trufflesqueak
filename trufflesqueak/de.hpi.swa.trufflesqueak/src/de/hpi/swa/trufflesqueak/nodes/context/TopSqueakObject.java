package de.hpi.swa.trufflesqueak.nodes.context;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.SmallInteger;

@NodeChildren({@NodeChild(value = "top", type = Top.class)})
public abstract class TopSqueakObject extends ContextAccessNode {
    public TopSqueakObject(CompiledMethodObject cm) {
        super(cm);
    }

    public static TopSqueakObject create(CompiledMethodObject cm, int offset) {
        return TopSqueakObjectNodeGen.create(cm, new Top(cm, offset));
    }

    @Specialization
    public BaseSqueakObject top(boolean top) {
        if (top) {
            return getMethod().getImage().sqTrue;
        } else {
            return getMethod().getImage().sqFalse;
        }
    }

    @Specialization
    public BaseSqueakObject top(int top) {
        return new SmallInteger(top);
    }

    @Specialization
    public BaseSqueakObject top(BigInteger top) {
        // TODO: create LNI and LPI objects
        // top.abs().toByteArray();
        throw new RuntimeException("not implemented");
    }

    @Specialization
    public BaseSqueakObject top(BaseSqueakObject top) {
        return top;
    }

    @Fallback
    public BaseSqueakObject top(Object top) {
        throw new RuntimeException("not implemented");
    }
}
