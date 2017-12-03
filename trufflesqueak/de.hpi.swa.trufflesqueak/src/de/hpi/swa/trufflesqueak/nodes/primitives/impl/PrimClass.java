package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.context.SqueakLookupClassNode;
import de.hpi.swa.trufflesqueak.nodes.context.SqueakLookupClassNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveUnaryOperation;

public abstract class PrimClass extends PrimitiveUnaryOperation {
    @Child SqueakLookupClassNode node;

    public PrimClass(CompiledMethodObject code) {
        super(code);
        node = SqueakLookupClassNodeGen.create(code);
    }

    @Specialization
    public Object lookup(Object arg) {
        return node.executeLookup(arg);
    }
}
