package de.hpi.swa.trufflesqueak.nodes.primitives;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.ImmediateCharacter;
import de.hpi.swa.trufflesqueak.model.SmallInteger;

public abstract class PrimEquivalent extends PrimitiveBinaryOperation {
    public PrimEquivalent(CompiledMethodObject cm) {
        super(cm);
    }

    public static PrimitiveNode create(CompiledMethodObject cm) {
        return PrimEquivalentNodeGen.create(cm, arg(0), arg(1));
    }

    @Specialization
    boolean equivalent(SmallInteger a, SmallInteger b) {
        return a.getValue() == b.getValue();
    }

    @Specialization
    boolean equivalent(ImmediateCharacter a, ImmediateCharacter b) {
        return a.getValue() == b.getValue();
    }

    @Specialization
    boolean equivalent(BaseSqueakObject a, BaseSqueakObject b) {
        return a == b;
    }
}
