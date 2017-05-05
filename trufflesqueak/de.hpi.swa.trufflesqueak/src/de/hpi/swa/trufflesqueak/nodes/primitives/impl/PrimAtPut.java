package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveBinaryOperation;

public class PrimAtPut extends PrimitiveBinaryOperation {
    public PrimAtPut(CompiledMethodObject cm) {
        super(cm);
    }
}
