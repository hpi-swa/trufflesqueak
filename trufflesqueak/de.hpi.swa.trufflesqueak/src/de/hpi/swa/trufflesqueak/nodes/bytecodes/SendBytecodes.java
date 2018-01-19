package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.model.SpecialSelectorObject;
import de.hpi.swa.trufflesqueak.nodes.DispatchNode;
import de.hpi.swa.trufflesqueak.nodes.LookupNode;
import de.hpi.swa.trufflesqueak.nodes.SqueakTypesGen;
import de.hpi.swa.trufflesqueak.nodes.context.HaltNode;
import de.hpi.swa.trufflesqueak.nodes.context.SqueakLookupClassNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PopNReversedStackNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PushStackNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeFactory;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class SendBytecodes {

    private static abstract class AbstractSendNode extends AbstractBytecodeNode {
        @CompilationFinal protected final Object selector;
        @CompilationFinal private final int argumentCount;
        @Child protected SqueakLookupClassNode lookupClassNode;
        @Child private LookupNode lookupNode = LookupNode.create();
        @Child private DispatchNode dispatchNode = DispatchNode.create();
        @Child private PopNReversedStackNode popNReversedNode;
        @Child private PushStackNode pushNode;

        private AbstractSendNode(CompiledCodeObject code, int index, int numBytecodes, Object sel, int argcount) {
            super(code, index, numBytecodes);
            selector = sel;
            argumentCount = argcount;
            lookupClassNode = SqueakLookupClassNode.create(code);
            pushNode = PushStackNode.create(code);
            popNReversedNode = PopNReversedStackNode.create(code, 1 + argumentCount);
        }

        public Object executeSend(VirtualFrame frame) {
            code.image.interrupt.executeCheck(frame);
            Object[] rcvrAndArgs = (Object[]) popNReversedNode.executeGeneric(frame);
            ClassObject rcvrClass;
            try {
                rcvrClass = SqueakTypesGen.expectClassObject(lookupClassNode.executeLookup(rcvrAndArgs[0]));
            } catch (UnexpectedResultException e) {
                throw new RuntimeException("receiver has no class");
            }
            Object lookupResult = lookupNode.executeLookup(rcvrClass, selector);
            if (!(lookupResult instanceof CompiledCodeObject)) {
                throw new RuntimeException("lookupResult not yet support. Object as method?");
            }
            Object[] frameArguments = FrameAccess.newWith(code, getContext(frame), null, rcvrAndArgs);
            return dispatchNode.executeDispatch(lookupResult, frameArguments);
        }

        @Override
        public void executeVoid(VirtualFrame frame) {
            Object result = executeSend(frame);
            if (result != null) { // primitive produced no result
                pushNode.executeWrite(frame, result);
            }
            // TODO: Object as Method
        }

        public Object getSelector() {
            return selector;
        }

        @Override
        protected boolean isTaggedWith(Class<?> tag) {
            return ((tag == StandardTags.StatementTag.class) || (tag == StandardTags.CallTag.class));
        }

        @Override
        public String toString() {
            return "send: " + selector.toString();
        }
    }

    public static class EagerSendSpecialSelectorNode extends AbstractBytecodeNode {
        public static AbstractBytecodeNode create(CompiledCodeObject code, int index, int selectorIndex) {
            SpecialSelectorObject specialSelector = code.image.specialSelectorsArray[selectorIndex];
            if (code instanceof CompiledMethodObject && specialSelector.getPrimitiveIndex() > 0) {
                AbstractPrimitiveNode primitiveNode;
                primitiveNode = PrimitiveNodeFactory.forSpecialSelector((CompiledMethodObject) code,
                                specialSelector);
                return new EagerSendSpecialSelectorNode(code, index, specialSelector, primitiveNode);
            }
            return getFallbackNode(code, index, specialSelector);
        }

        private static SendSelectorNode getFallbackNode(CompiledCodeObject code, int index, SpecialSelectorObject specialSelector) {
            return new SendSelectorNode(code, index, 1, specialSelector, specialSelector.getNumArguments());
        }

        @CompilationFinal private final SpecialSelectorObject specialSelector;

        @Child private AbstractPrimitiveNode primitiveNode;

        @Child private PushStackNode pushStackNode;

        public EagerSendSpecialSelectorNode(CompiledCodeObject code, int index, SpecialSelectorObject specialSelector, AbstractPrimitiveNode primitiveNode) {
            super(code, index);
            this.pushStackNode = PushStackNode.create(code);
            this.specialSelector = specialSelector;
            this.primitiveNode = primitiveNode;
        }

        @Override
        public void executeVoid(VirtualFrame frame) {
            try {
                Object result = primitiveNode.executeGeneric(frame);
                // Success! Manipulate the sp to quick pop receiver and arguments and push result.
                ContextObject context = getContext(frame);
                int spOffset = 1 + specialSelector.getNumArguments();
                if (context == null) {
                    frame.setInt(code.stackPointerSlot, frame.getInt(code.stackPointerSlot) - spOffset);
                } else {
                    context.atput0(CONTEXT.STACKPOINTER, (int) context.at0(CONTEXT.STACKPOINTER) - spOffset);
                }
                if (result != null) { // primitive produced no result
                    pushStackNode.executeWrite(frame, result);
                }
            } catch (PrimitiveFailed | ArithmeticException | UnsupportedSpecializationException e) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                replace(getFallbackNode(code, index, specialSelector)).executeVoid(frame);
            } catch (FrameSlotTypeException e) {
                throw new RuntimeException("Unable to set stack pointer");
            }
        }

        public Object getSpecialSelector() {
            return specialSelector;
        }

        @Override
        public String toString() {
            return "send: " + specialSelector.toString();
        }
    }

    public static class SecondExtendedSendNode extends AbstractSendNode {
        public SecondExtendedSendNode(CompiledCodeObject code, int index, int numBytecodes, int i) {
            super(code, index, numBytecodes, code.getLiteral(i & 63), i >> 6);
        }
    }

    public static class SendLiteralSelectorNode extends AbstractSendNode {
        public static AbstractBytecodeNode create(CompiledCodeObject code, int index, int numBytecodes, int literalIndex, int argCount) {
            Object selector = code.getLiteral(literalIndex);
            if (selector != null && selector.toString().equals("halt")) {
                return new HaltNode(code, index);
            }
            return new SendLiteralSelectorNode(code, index, numBytecodes, selector, argCount);
        }

        public SendLiteralSelectorNode(CompiledCodeObject code, int index, int numBytecodes, Object selector, int argCount) {
            super(code, index, numBytecodes, selector, argCount);
        }
    }

    public static class SendSelectorNode extends AbstractSendNode {
        public SendSelectorNode(CompiledCodeObject code, int index, int numBytecodes, BaseSqueakObject sel, int argcount) {
            super(code, index, numBytecodes, sel, argcount);
        }
    }

    public static class SendSelfSelector extends AbstractSendNode {
        public SendSelfSelector(CompiledCodeObject code, int index, int numBytecodes, Object selector, int numArgs) {
            super(code, index, numBytecodes, selector, numArgs);
        }
    }

    public static class SingleExtendedSendNode extends AbstractSendNode {
        public SingleExtendedSendNode(CompiledCodeObject code, int index, int numBytecodes, int param) {
            super(code, index, numBytecodes, code.getLiteral(param & 31), param >> 5);
        }
    }

    public static class SingleExtendedSuperNode extends AbstractSendNode {
        private static class SqueakLookupClassSuperNode extends SqueakLookupClassNode {
            public SqueakLookupClassSuperNode(CompiledCodeObject code) {
                super(code);
            }

            @Override
            public Object executeLookup(Object receiver) {
                return code.getCompiledInClass().getSuperclass();
            }
        }

        public SingleExtendedSuperNode(CompiledCodeObject code, int index, int numBytecodes, int rawByte) {
            this(code, index, numBytecodes, rawByte & 31, rawByte >> 5);
        }

        public SingleExtendedSuperNode(CompiledCodeObject code, int index, int numBytecodes, int literalIndex, int numArgs) {
            super(code, index, numBytecodes, code.getLiteral(literalIndex), numArgs);
            lookupClassNode = new SqueakLookupClassSuperNode(code);
        }

        @Override
        public String toString() {
            return "sendSuper: " + selector.toString();
        }
    }
}
