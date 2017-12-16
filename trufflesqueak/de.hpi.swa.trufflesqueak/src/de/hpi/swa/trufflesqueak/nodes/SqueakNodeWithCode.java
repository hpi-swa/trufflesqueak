package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

/**
 * This is the base class for Squeak bytecode evaluation.
 */
@TypeSystemReference(SqueakTypes.class)
public abstract class SqueakNodeWithCode extends SqueakNode {
    protected final CompiledCodeObject code;

    public SqueakNodeWithCode(CompiledCodeObject code) {
        this.code = code;
    }

    public final Object getClosure(VirtualFrame frame) {
        try {
            return frame.getObject(code.closureSlot);
        } catch (FrameSlotTypeException e) {
            throw new RuntimeException(e);
        }
    }
}
