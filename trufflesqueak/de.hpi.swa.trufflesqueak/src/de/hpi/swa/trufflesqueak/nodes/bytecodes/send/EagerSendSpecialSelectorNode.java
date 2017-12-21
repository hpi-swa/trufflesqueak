package de.hpi.swa.trufflesqueak.nodes.bytecodes.send;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.SpecialSelector;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.AbstractBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PushStackNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeFactory;

public class EagerSendSpecialSelectorNode extends AbstractBytecodeNode {
    @CompilationFinal private final SpecialSelector specialSelector;
    @Child private AbstractPrimitiveNode primitiveNode;
    @Child private PushStackNode pushStackNode;

    public static AbstractBytecodeNode create(CompiledCodeObject code, int index, int selectorIndex) {
        SpecialSelector specialSelector = code.image.specialSelectorsArray[selectorIndex];
        if (code instanceof CompiledMethodObject && specialSelector.getPrimitiveIndex() > 0) {
            AbstractPrimitiveNode primitiveNode;
            primitiveNode = PrimitiveNodeFactory.forSpecialSelector((CompiledMethodObject) code, specialSelector);
            return new EagerSendSpecialSelectorNode(code, index, specialSelector, primitiveNode);
        }
        return getFallbackNode(code, index, specialSelector);
    }

    public EagerSendSpecialSelectorNode(CompiledCodeObject code, int index, SpecialSelector specialSelector, AbstractPrimitiveNode primitiveNode) {
        super(code, index);
        this.pushStackNode = new PushStackNode(code);
        this.specialSelector = specialSelector;
        this.primitiveNode = primitiveNode;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        try {
            Object result = primitiveNode.executeGeneric(frame);
            // Success! Manipulate the sp to quick pop receiver and arguments and push result.
            frame.setInt(code.stackPointerSlot, frame.getInt(code.stackPointerSlot) - 1 - specialSelector.getNumArguments());
            pushStackNode.executeWrite(frame, result);
        } catch (UnsupportedSpecializationException e) {
            replace(getFallbackNode(code, index, specialSelector)).executeVoid(frame);
        } catch (PrimitiveFailed | ArithmeticException | FrameSlotTypeException e) {
            replace(getFallbackNode(code, index, specialSelector)).executeVoid(frame);
        }
    }

    private static SendSelectorNode getFallbackNode(CompiledCodeObject code, int index, SpecialSelector specialSelector) {
        return new SendSelectorNode(code, index, 1, specialSelector, specialSelector.getNumArguments());
    }

    @Override
    public String toString() {
        return "send: " + specialSelector.toString();
    }
}
