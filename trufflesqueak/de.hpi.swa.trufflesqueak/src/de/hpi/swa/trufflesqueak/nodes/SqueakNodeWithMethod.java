package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

/**
 * This is the base class for Squeak bytecode evaluation.
 */
@TypeSystemReference(SqueakTypes.class)
public abstract class SqueakNodeWithMethod extends SqueakNode {
    protected final CompiledCodeObject method;

    public SqueakNodeWithMethod(CompiledCodeObject method2) {
        method = method2;
    }

    public final Object getClosure(VirtualFrame frame) {
        try {
            return frame.getObject(method.closureSlot);
        } catch (FrameSlotTypeException e) {
            throw new RuntimeException(e);
        }
    }
}
