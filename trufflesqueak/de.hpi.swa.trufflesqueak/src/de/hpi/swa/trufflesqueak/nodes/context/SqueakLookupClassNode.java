package de.hpi.swa.trufflesqueak.nodes.context;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.SqueakObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNodeWithCode;
import de.hpi.swa.trufflesqueak.nodes.SqueakTypesGen;

public abstract class SqueakLookupClassNode extends AbstractNodeWithCode {
    public static SqueakLookupClassNode create(CompiledCodeObject code) {
        return SqueakLookupClassNodeGen.create(code);
    }

    protected SqueakLookupClassNode(CompiledCodeObject code) {
        super(code);
    }

    public abstract Object executeLookup(Object receiver);

    @Specialization
    protected ClassObject squeakClass(boolean object) {
        if (object) {
            return code.image.trueClass;
        } else {
            return code.image.falseClass;
        }
    }

    protected boolean isNil(Object object) {
        return object == code.image.nil;
    }

    @SuppressWarnings("unused")
    @Specialization
    protected ClassObject squeakClass(int object) {
        return code.image.smallIntegerClass;
    }

    @SuppressWarnings("unused")
    @Specialization
    protected ClassObject squeakClass(long object) {
        return code.image.smallIntegerClass;
    }

    @SuppressWarnings("unused")
    @Specialization
    protected ClassObject squeakClass(char object) {
        return code.image.characterClass;
    }

    @SuppressWarnings("unused")
    @Specialization
    protected ClassObject squeakClass(double object) {
        return code.image.floatClass;
    }

    @Specialization
    protected ClassObject squeakClass(BigInteger object) {
        if (object.signum() >= 0) {
            return code.image.largePositiveIntegerClass;
        } else {
            return code.image.largeNegativeIntegerClass;
        }
    }

    @Specialization
    protected ClassObject squeakClass(BlockClosureObject ch) {
        return ch.getSqClass();
    }

    @Specialization
    protected ClassObject squeakClass(@SuppressWarnings("unused") ContextObject ch) {
        return code.image.methodContextClass;
    }

    @Specialization(rewriteOn = UnexpectedResultException.class)
    protected ClassObject squeakClass(SqueakObject object) throws UnexpectedResultException {
        return SqueakTypesGen.expectClassObject(object.getSqClass());
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "isNil(object)")
    protected ClassObject nilClass(Object object) {
        return code.image.nilClass;
    }
}
