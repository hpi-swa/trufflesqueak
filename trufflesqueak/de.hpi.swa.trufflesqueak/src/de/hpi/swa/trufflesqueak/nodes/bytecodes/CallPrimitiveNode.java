package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags.StatementTag;
import com.oracle.truffle.api.source.SourceSection;

import de.hpi.swa.trufflesqueak.exceptions.LocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeFactory;

public class CallPrimitiveNode extends AbstractBytecodeNode {
    @Child private PrimitiveNode primitiveNode;
    @CompilationFinal private final int primitiveIndex;

    public CallPrimitiveNode(CompiledCodeObject code, int index, int numBytecodes, int byte1, int byte2) {
        super(code, index, numBytecodes);
        primitiveIndex = code.hasPrimitive() ? byte1 + (byte2 << 8) : 0;
        primitiveNode = PrimitiveNodeFactory.forIdx(code, primitiveIndex);
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        CompilerAsserts.compilationConstant(index);
        try {
            throw new LocalReturn(primitiveNode.executeGeneric(frame));
        } catch (UnsupportedSpecializationException
                        | PrimitiveFailed
                        | IndexOutOfBoundsException e) {
        }
    }

    @Override
    public String toString() {
        return "callPrimitive: " + primitiveIndex;
    }

    @Override
    protected boolean isTaggedWith(Class<?> tag) {
        return tag == StatementTag.class;
    }

    @Override
    public void setSourceSection(SourceSection section) {
        super.setSourceSection(section);
        primitiveNode.setSourceSection(section);
    }
}
