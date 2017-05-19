package de.hpi.swa.trufflesqueak.nodes.context;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.exceptions.UnwrappingError;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;

@NodeChildren({@NodeChild(value = "objectNode", type = SqueakNode.class)})
public abstract class ObjectAtNode extends SqueakNode {
    private final int index;

    protected ObjectAtNode(int variableIndex) {
        index = variableIndex;
    }

    @Specialization(rewriteOn = {UnwrappingError.class, ArithmeticException.class})
    protected int readInt(BaseSqueakObject object) throws UnwrappingError {
        return safeObject(object).unwrapInt();
    }

    @Specialization(rewriteOn = {UnwrappingError.class, ArithmeticException.class})
    protected long readLong(BaseSqueakObject object) throws UnwrappingError {
        return safeObject(object).unwrapLong();
    }

    @Specialization(rewriteOn = UnwrappingError.class)
    protected BigInteger readBigInteger(BaseSqueakObject object) throws UnwrappingError {
        return safeObject(object).unwrapBigInt();
    }

    private BaseSqueakObject safeObject(BaseSqueakObject o) throws UnwrappingError {
        BaseSqueakObject at0 = o.at0(index);
        if (at0 == null) {
            throw new UnwrappingError();
        }
        return at0;
    }

    @Specialization
    protected Object readObject(BaseSqueakObject object) {
        return object.at0(index);
    }
}
