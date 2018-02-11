package de.hpi.swa.trufflesqueak.nodes.context;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNodeWithCode;

public abstract class SqueakLookupClassNode extends AbstractNodeWithCode {
    public static SqueakLookupClassNode create(CompiledCodeObject code) {
        return SqueakLookupClassNodeGen.create(code);
    }

    protected SqueakLookupClassNode(CompiledCodeObject code) {
        super(code);
    }

    public abstract ClassObject executeLookup(Object receiver);

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
    protected ClassObject squeakClass(@SuppressWarnings("unused") BlockClosureObject ch) {
        return code.image.blockClosureClass;
    }

    @Specialization
    protected ClassObject squeakClass(@SuppressWarnings("unused") ContextObject ch) {
        return code.image.methodContextClass;
    }

    @SuppressWarnings("unused")
    @Specialization
    protected ClassObject nilClass(NilObject object) {
        return code.image.nilClass;
    }

    @Specialization
    protected ClassObject squeakClass(BaseSqueakObject object) {
        return object.getSqClass();
    }
}
