package de.hpi.swa.trufflesqueak.nodes.bytecodes.send;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.AbstractBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PushStackNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeFactory;

public class EagerSendSelectorNode extends AbstractBytecodeNode {
    @Child private AbstractPrimitiveNode primitiveNode;
    @CompilationFinal private final int selectorIndex;
    @CompilationFinal private final int numArguments;
    @Child private PushStackNode pushStackNode;

    public EagerSendSelectorNode(CompiledCodeObject code, int index, int selectorIndex, int numArguments, AbstractPrimitiveNode primitiveNode) {
        super(code, index);
        this.pushStackNode = new PushStackNode(code);
        this.selectorIndex = selectorIndex;
        this.numArguments = numArguments;
        this.primitiveNode = primitiveNode;
    }

    public static AbstractBytecodeNode create(CompiledCodeObject code, int index, int selectorIndex, int numArguments) {
        AbstractPrimitiveNode primitiveNode;
        try {
            primitiveNode = PrimitiveNodeFactory.forSelector(code, getSpecialSelector(code, selectorIndex), 1 + numArguments);
        } catch (NullPointerException | IllegalArgumentException e) {
            // Not found in eagerPrimitiveTable or code was not a CompiledMethodObject.
            return getFallbackNode(code, index, selectorIndex, numArguments);
        }
        return new EagerSendSelectorNode(code, index, selectorIndex, numArguments, primitiveNode);
    }

    private static SendSelectorNode getFallbackNode(CompiledCodeObject code, int index, int selectorIndex, int numArguments) {
        return new SendSelectorNode(code, index, 1, getSpecialSelector(code, selectorIndex), numArguments);
    }

    private static NativeObject getSpecialSelector(CompiledCodeObject code, int selectorIndex) {
        return code.image.nativeSpecialSelectors[selectorIndex];
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        try {
            Object result = primitiveNode.executeGeneric(frame);
            // Success! Manipulate the sp to quick pop receiver and arguments and push result.
            frame.setInt(code.stackPointerSlot, frame.getInt(code.stackPointerSlot) - 1 - numArguments);
            pushStackNode.executeWrite(frame, result);
        } catch (PrimitiveFailed | UnsupportedSpecializationException | FrameSlotTypeException e) {
            replace(getFallbackNode(code, index, selectorIndex, numArguments));
        }
    }

    @Override
    public String toString() {
        return "send: " + getSpecialSelector(code, selectorIndex);
    }
}
