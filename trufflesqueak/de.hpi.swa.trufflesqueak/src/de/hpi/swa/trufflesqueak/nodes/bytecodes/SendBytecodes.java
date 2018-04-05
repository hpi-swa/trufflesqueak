package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.exceptions.PrimitiveExceptions.PrimitiveWithoutResultException;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.MESSAGE;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.SpecialSelectorObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNodeWithImage;
import de.hpi.swa.trufflesqueak.nodes.DispatchNode;
import de.hpi.swa.trufflesqueak.nodes.LookupNode;
import de.hpi.swa.trufflesqueak.nodes.context.SqueakLookupClassNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameSlotReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.StackPopNReversedNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.StackPushNode;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;

public final class SendBytecodes {

    public static abstract class AbstractSendNode extends AbstractBytecodeNode {
        @CompilationFinal protected final Object selector;
        @CompilationFinal private final int argumentCount;
        @Child protected SqueakLookupClassNode lookupClassNode;
        @Child private LookupNode lookupNode = LookupNode.create();
        @Child private DispatchNode dispatchNode = DispatchNode.create();
        @Child private SendDoesNotUnderstandNode sendDoesNotUnderstandNode;
        @Child private SendObjectAsMethodNode sendObjectAsMethodNode;
        @Child private StackPopNReversedNode popNReversedNode;
        @Child private StackPushNode pushNode;
        @Child private FrameSlotReadNode readContextNode;

        private AbstractSendNode(CompiledCodeObject code, int index, int numBytecodes, Object sel, int argcount) {
            super(code, index, numBytecodes);
            selector = sel;
            argumentCount = argcount;
            lookupClassNode = SqueakLookupClassNode.create(code.image);
            pushNode = StackPushNode.create(code);
            popNReversedNode = StackPopNReversedNode.create(code, 1 + argumentCount);
            readContextNode = FrameSlotReadNode.create(code.thisContextOrMarkerSlot);
            sendDoesNotUnderstandNode = SendDoesNotUnderstandNode.create(code.image);
            sendObjectAsMethodNode = SendObjectAsMethodNode.create(code.image);
        }

        @Override
        public void executeVoid(VirtualFrame frame) {
            Object result;
            try {
                result = executeSend(frame);
            } catch (PrimitiveWithoutResultException e) {
                return; // ignoring result
            }
            pushNode.executeWrite(frame, result);
        }

        public Object executeSend(VirtualFrame frame) {
            Object[] rcvrAndArgs = (Object[]) popNReversedNode.executeRead(frame);
            ClassObject rcvrClass = lookupClassNode.executeLookup(rcvrAndArgs[0]);
            Object lookupResult = lookupNode.executeLookup(rcvrClass, selector);
            Object contextOrMarker = readContextNode.executeRead(frame);
            if (!(lookupResult instanceof CompiledCodeObject)) {
                return sendObjectAsMethodNode.execute(frame, selector, rcvrAndArgs, lookupResult, contextOrMarker);
            } else if (((CompiledCodeObject) lookupResult).isDoesNotUnderstand()) {
                return sendDoesNotUnderstandNode.execute(frame, selector, rcvrAndArgs, rcvrClass, lookupResult, contextOrMarker);
            } else {
                return dispatchNode.executeDispatch(frame, lookupResult, rcvrAndArgs, contextOrMarker);
            }
        }

        public Object getSelector() {
            return selector;
        }

        @Override
        public boolean hasTag(Class<? extends Tag> tag) {
            return ((tag == StandardTags.StatementTag.class) || (tag == StandardTags.CallTag.class));
        }

        @Override
        public String toString() {
            return "send: " + selector.toString();
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
// if (selector != null && selector.toString().equals("halt")) {
// return new HaltNode(code, index);
// }
            return new SendLiteralSelectorNode(code, index, numBytecodes, selector, argCount);
        }

        public SendLiteralSelectorNode(CompiledCodeObject code, int index, int numBytecodes, Object selector, int argCount) {
            super(code, index, numBytecodes, selector, argCount);
        }
    }

    public static class SendSelectorNode extends AbstractSendNode {
        public static SendSelectorNode createForSpecialSelector(CompiledCodeObject code, int index, int selectorIndex) {
            SpecialSelectorObject specialSelector = code.image.specialSelectorsArray[selectorIndex];
            return new SendSelectorNode(code, index, 1, specialSelector, specialSelector.getNumArguments());
        }

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
        protected static class SqueakLookupClassSuperNode extends SqueakLookupClassNode {
            @CompilationFinal private final CompiledCodeObject code;

            public SqueakLookupClassSuperNode(CompiledCodeObject code) {
                super(code.image);
                this.code = code; // storing both, image and code, because of class hierarchy
            }

            @Override
            public ClassObject executeLookup(Object receiver) {
                ClassObject compiledInClass = code.getCompiledInClass();
                Object superclass = compiledInClass.getSuperclass();
                if (superclass == code.image.nil) {
                    return compiledInClass;
                } else {
                    return (ClassObject) superclass;
                }
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

    public static class SendDoesNotUnderstandNode extends AbstractNodeWithImage {
        @Child private DispatchNode dispatchNode = DispatchNode.create();
        @CompilationFinal private ClassObject messageClass;

        public static SendDoesNotUnderstandNode create(SqueakImageContext image) {
            return new SendDoesNotUnderstandNode(image);
        }

        private SendDoesNotUnderstandNode(SqueakImageContext image) {
            super(image);
        }

        public Object execute(VirtualFrame frame, Object selector, Object[] rcvrAndArgs, ClassObject rcvrClass, Object lookupDNU, Object contextOrMarker) {
            PointersObject message = (PointersObject) getMessageClass().newInstance();
            message.atput0(MESSAGE.SELECTOR, selector);
            Object[] arguments = ArrayUtils.allButFirst(rcvrAndArgs);
            message.atput0(MESSAGE.ARGUMENTS, image.newList(arguments));
            if (message.instsize() > MESSAGE.LOOKUP_CLASS) { // early versions do not have
                                                             // lookupClass
                message.atput0(MESSAGE.LOOKUP_CLASS, rcvrClass);
            }
            return dispatchNode.executeDispatch(frame, lookupDNU, new Object[]{rcvrAndArgs[0], message}, contextOrMarker);
        }

        private ClassObject getMessageClass() {
            if (messageClass == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                messageClass = (ClassObject) image.specialObjectsArray.at0(SPECIAL_OBJECT_INDEX.ClassMessage);
            }
            return messageClass;
        }
    }

    public static class SendObjectAsMethodNode extends AbstractNodeWithImage {
        @Child private DispatchNode dispatchNode = DispatchNode.create();
        @Child private LookupNode lookupNode = LookupNode.create();
        @Child protected SqueakLookupClassNode lookupClassNode;
        @CompilationFinal private NativeObject runWithIn;

        public static SendObjectAsMethodNode create(SqueakImageContext image) {
            return new SendObjectAsMethodNode(image);
        }

        private SendObjectAsMethodNode(SqueakImageContext image) {
            super(image);
            lookupClassNode = SqueakLookupClassNode.create(image);
        }

        public Object execute(VirtualFrame frame, Object selector, Object[] rcvrAndArgs, Object lookupResult, Object contextOrMarker) {
            Object[] arguments = ArrayUtils.allButFirst(rcvrAndArgs);
            ClassObject rcvrClass = lookupClassNode.executeLookup(lookupResult);
            Object newLookupResult = lookupNode.executeLookup(rcvrClass, getRunWithIn());
            return dispatchNode.executeDispatch(frame, newLookupResult, new Object[]{lookupResult, selector, image.newList(arguments), rcvrAndArgs[0]}, contextOrMarker);
        }

        private NativeObject getRunWithIn() {
            if (runWithIn == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                runWithIn = (NativeObject) image.specialObjectsArray.at0(SPECIAL_OBJECT_INDEX.SelectorRunWithIn);
            }
            return runWithIn;
        }
    }
}
