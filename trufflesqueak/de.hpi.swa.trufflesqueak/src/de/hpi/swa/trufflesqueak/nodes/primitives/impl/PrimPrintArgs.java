package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNode;

public abstract class PrimPrintArgs extends PrimitiveNode {
    public PrimPrintArgs(CompiledMethodObject code) {
        super(code);
    }

    @TruffleBoundary
    private static void debugPrint(Object o) {
        if (o instanceof NativeObject) {
            System.out.println(((NativeObject) o).toString());
        } else {
            System.out.println(o.toString());
        }
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        Object[] arguments = frame.getArguments();
        for (int i = 1; i < arguments.length; i++) {
            debugPrint(arguments[i]);
        }
        return null;
    }
}
