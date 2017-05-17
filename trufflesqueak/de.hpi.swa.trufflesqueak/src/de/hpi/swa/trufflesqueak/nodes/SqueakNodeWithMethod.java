package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableFactory.WrapperNode;
import com.oracle.truffle.api.source.SourceSection;

import de.hpi.swa.trufflesqueak.instrumentation.SqueakSource;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

/**
 * This is the base class for Squeak bytecode evaluation.
 */
@TypeSystemReference(SqueakTypes.class)
public abstract class SqueakNodeWithMethod extends SqueakNode {
    protected final CompiledCodeObject method;
    @CompilationFinal protected SourceSection sourceSection;

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

    @Override
    public SourceSection getSourceSection() {
        if (sourceSection == null) {
            if (this instanceof WrapperNode) {
                sourceSection = SqueakSource.noSource();
            } else {
                sourceSection = SqueakSource.build(method, this);
            }
        }
        return sourceSection;
    }
}
