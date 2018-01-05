package de.hpi.swa.trufflesqueak.nodes.context;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.MethodContextObject;
import de.hpi.swa.trufflesqueak.model.SqueakObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakTypesGen;

public abstract class SqueakLookupClassNode extends Node {
    protected final CompiledCodeObject code;

    public SqueakLookupClassNode(CompiledCodeObject code) {
        this.code = code;
    }

    public abstract Object executeLookup(Object receiver);

    @Specialization
    public ClassObject squeakClass(boolean object) {
        if (object) {
            return code.image.trueClass;
        } else {
            return code.image.falseClass;
        }
    }

    protected boolean isNil(Object object) {
        return object.equals(code.image.nil);
    }

    @SuppressWarnings("unused")
    @Specialization
    public ClassObject squeakClass(int object) {
        return code.image.smallIntegerClass;
    }

    @SuppressWarnings("unused")
    @Specialization
    public ClassObject squeakClass(long object) {
        return code.image.smallIntegerClass;
    }

    @SuppressWarnings("unused")
    @Specialization
    public ClassObject squeakClass(char object) {
        return code.image.characterClass;
    }

    @SuppressWarnings("unused")
    @Specialization
    public ClassObject squeakClass(double object) {
        return code.image.floatClass;
    }

    @Specialization
    public ClassObject squeakClass(BigInteger object) {
        if (object.signum() >= 0) {
            return code.image.largePositiveIntegerClass;
        } else {
            return code.image.largeNegativeIntegerClass;
        }
    }

    @Specialization
    public ClassObject squeakClass(BlockClosureObject ch) {
        return ch.getSqClass();
    }

    @Specialization
    public ClassObject squeakClass(@SuppressWarnings("unused") MethodContextObject ch) {
        return code.image.methodContextClass;
    }

    @Specialization(rewriteOn = UnexpectedResultException.class)
    public ClassObject squeakClass(SqueakObject object) throws UnexpectedResultException {
        return SqueakTypesGen.expectClassObject(object.getSqClass());
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "isNil(object)")
    public ClassObject nilClass(Object object) {
        return code.image.nilClass;
    }

    public static SqueakLookupClassNode create(CompiledCodeObject code) {
        return SqueakLookupClassNodeGen.create(code);
    }
}
