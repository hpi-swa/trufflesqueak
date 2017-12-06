package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags.StatementTag;
import com.oracle.truffle.api.source.SourceSection;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeFactory;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveQuickReturnNode;

public class CallPrimitiveNode extends SqueakBytecodeNode {
    @Child PrimitiveNode primitive;

    @SuppressWarnings("unused")
    public CallPrimitiveNode(CompiledCodeObject code, int index, int unusedA, int unusedB) {
        super(code, index);
        primitive = PrimitiveNodeFactory.forIdx(code, code.primitiveIndex());
        // the unused two bytes are skipped but belong to this bytecode
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        assert successorIndex == 3; // two skipped bytes plus next
        primitive.executeGeneric(frame);
        return code.image.nil;
// try {
// throw new LocalReturn();
// } catch (UnsupportedSpecializationException
// | PrimitiveFailed
// | IndexOutOfBoundsException e) {
// return code.image.nil;
// }
    }

    @Override
    public boolean isReturn() {
        return primitive instanceof PrimitiveQuickReturnNode;
    }

    @Override
    protected boolean isTaggedWith(Class<?> tag) {
        return tag == StatementTag.class;
    }

    @Override
    public void setSourceSection(SourceSection section) {
        super.setSourceSection(section);
        primitive.setSourceSection(section);
    }
}
