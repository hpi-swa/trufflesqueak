package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveBinaryOperation;

public class PrimLessOrEqual extends PrimitiveBinaryOperation {
    protected PrimLessOrEqual(CompiledMethodObject cm) {
        super(cm);
    }

    @Specialization
    boolean add(int a, int b) {
        return a <= b;
    }

    @Specialization
    boolean add(long a, long b) {
        return a <= b;
    }

    @Specialization
    boolean add(BigInteger a, BigInteger b) {
        return a.compareTo(b) <= 0;
    }
}
