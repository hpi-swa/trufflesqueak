package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakTypesGen;

public abstract class SqueakLookupClassNode extends Node {
    protected final CompiledMethodObject method;

    public SqueakLookupClassNode(CompiledMethodObject cm) {
        method = cm;
    }

    public abstract Object executeLookup(Object receiver);

    @Specialization
    public ClassObject squeakClass(boolean object) {
        if (object) {
            return (ClassObject) method.getImage().sqTrue.getSqClass();
        } else {
            return (ClassObject) method.getImage().sqFalse.getSqClass();
        }
    }

    protected static boolean isNull(Object object) {
        return object == null;
    }

    @Specialization(guards = "isNull(object)", rewriteOn = UnexpectedResultException.class)
    public ClassObject nilClass(@SuppressWarnings("unused") Object object) throws UnexpectedResultException {
        return SqueakTypesGen.expectClassObject(method.getImage().nil.getSqClass());
    }

    @Specialization
    public ClassObject squeakClass(@SuppressWarnings("unused") int object) {
        return method.getImage().smallIntegerClass;
    }

    @Specialization(rewriteOn = UnexpectedResultException.class)
    public ClassObject squeakClass(BaseSqueakObject object) throws UnexpectedResultException {
        return SqueakTypesGen.expectClassObject(object.getSqClass());
    }

    @Specialization
    public BaseSqueakObject squeakClass(@SuppressWarnings("unused") Object object) {
        return method.getImage().nil;
    }
}
