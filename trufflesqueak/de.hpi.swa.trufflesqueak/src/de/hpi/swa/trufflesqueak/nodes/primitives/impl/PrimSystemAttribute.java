package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveBinaryOperation;

public class PrimSystemAttribute extends PrimitiveBinaryOperation {
    public PrimSystemAttribute(CompiledMethodObject cm) {
        super(cm);
    }

    @Specialization
    @TruffleBoundary
    public Object getSystemAttribute(@SuppressWarnings("unused") Object image, int idx) {
        if (idx >= 2 && idx <= 1000) {
            String[] restArgs = method.image.config.getRestArgs();
            if (restArgs.length > idx - 2) {
                return method.image.wrap(restArgs[idx - 2]);
            } else {
                return null;
            }
        }
        switch (idx) {
            case 1001:
                return method.image.wrap("java");
            case 1002:
                return method.image.wrap(System.getProperty("java.version"));
        }
        return null;
    }
}
