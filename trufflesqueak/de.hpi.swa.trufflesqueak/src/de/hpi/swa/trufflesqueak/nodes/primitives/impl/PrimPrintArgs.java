package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNode;

public class PrimPrintArgs extends PrimitiveNode {
    public PrimPrintArgs(CompiledMethodObject cm) {
        super(cm);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        for (Object o : frame.getArguments()) {
            if (o instanceof NativeObject) {
                System.out.println(((NativeObject) o).toString());
            } else {
                System.out.println(o.toString());
            }
        }
        throw new PrimitiveFailed();
    }
}
