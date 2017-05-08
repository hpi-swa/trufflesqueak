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
            return method.image.trueClass;
        } else {
            return method.image.falseClass;
        }
    }

    protected static boolean isNull(Object object) {
        return object == null;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "isNull(object)", rewriteOn = UnexpectedResultException.class)
    public ClassObject nilClass(Object object) throws UnexpectedResultException {
        return method.image.nilClass;
    }

    @SuppressWarnings("unused")
    @Specialization
    public ClassObject squeakClass(int object) {
        return method.image.smallIntegerClass;
    }

    @SuppressWarnings("unused")
    @Specialization
    public ClassObject squeakClass(long object) {
        return method.image.smallIntegerClass;
    }

    @Specialization(rewriteOn = UnexpectedResultException.class)
    public ClassObject squeakClass(BaseSqueakObject object) throws UnexpectedResultException {
        return SqueakTypesGen.expectClassObject(object.getSqClass());
    }

    @Specialization
    public BaseSqueakObject squeakClass(@SuppressWarnings("unused") Object object) {
        return method.image.nil;
    }
}
