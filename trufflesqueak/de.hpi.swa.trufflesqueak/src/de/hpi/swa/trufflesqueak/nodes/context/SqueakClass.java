package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakTypesGen;

@NodeChildren({@NodeChild(value = "object", type = ContextAccessNode.class)})
public abstract class SqueakClass extends ContextAccessNode {
    public SqueakClass(CompiledMethodObject cm) {
        super(cm);
    }

    @Specialization
    public ClassObject squeakClass(boolean object) {
        if (object) {
            return (ClassObject) getMethod().getImage().sqTrue.getSqClass();
        } else {
            return (ClassObject) getMethod().getImage().sqFalse.getSqClass();
        }
    }

    @Specialization
    public ClassObject squeakClass(int object) {
        return getMethod().getImage().smallIntegerClass;
    }

    @Specialization(rewriteOn = UnexpectedResultException.class)
    public ClassObject squeakClass(BaseSqueakObject object) throws UnexpectedResultException {
        return SqueakTypesGen.expectClassObject(object.getSqClass());
    }

    @Specialization
    public BaseSqueakObject squeakClass(Object object) {
        return getMethod().getImage().nil;
    }
}
