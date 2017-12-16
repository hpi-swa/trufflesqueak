package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

/**
 * This is the base class for Squeak bytecode evaluation.
 */
@TypeSystemReference(SqueakTypes.class)
public abstract class SqueakNodeWithCode extends SqueakNode {
    @CompilationFinal private SourceSection sourceSection;
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

    @Override
    public void setSourceSection(SourceSection section) {
        sourceSection = section;
    }

    @Override
    public SourceSection getSourceSection() {
        if (sourceSection == null) {
            sourceSection = code.getSource().createSection(1);
        }
        return sourceSection;
    }
}
