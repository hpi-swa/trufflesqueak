package de.hpi.swa.graal.squeak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveWithoutResultException;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.nodes.DispatchSendNode;
import de.hpi.swa.graal.squeak.nodes.LookupMethodNode;
import de.hpi.swa.graal.squeak.nodes.accessing.CompiledCodeNodes.GetCompiledMethodNode;
import de.hpi.swa.graal.squeak.nodes.context.LookupClassNode;
import de.hpi.swa.graal.squeak.nodes.context.stack.StackPopNReversedNode;
import de.hpi.swa.graal.squeak.nodes.context.stack.StackPushNode;

public final class SendBytecodes {

    public abstract static class AbstractSendNode extends AbstractBytecodeNode {
        protected final NativeObject selector;
        private final int argumentCount;

        @Child protected LookupClassNode lookupClassNode;
        @Child private LookupMethodNode lookupMethodNode;
        @Child private DispatchSendNode dispatchSendNode;
        @Child private StackPopNReversedNode popNReversedNode;
        @Child private StackPushNode pushNode;

        private AbstractSendNode(final CompiledCodeObject code, final int index, final int numBytecodes, final Object sel, final int argcount) {
            super(code, index, numBytecodes);
            selector = sel instanceof NativeObject ? (NativeObject) sel : code.image.doesNotUnderstand;
            argumentCount = argcount;
            lookupMethodNode = LookupMethodNode.create(code.image);
            lookupClassNode = LookupClassNode.create(code.image);
            dispatchSendNode = DispatchSendNode.create(code.image);
            popNReversedNode = StackPopNReversedNode.create(code, 1 + argumentCount);
            pushNode = StackPushNode.create(code);
        }

        protected AbstractSendNode(final AbstractSendNode original) {
            this(original.code, original.index, original.numBytecodes, original.selector, original.argumentCount);
        }

        @Override
        public final void executeVoid(final VirtualFrame frame) {
            final Object result;
            try {
                result = executeSend(frame);
                assert result != null : "Result of a message send should not be null";
            } catch (PrimitiveWithoutResultException e) {
                return; // ignoring result
            }
            pushNode.executeWrite(frame, result);
        }

        public final Object executeSend(final VirtualFrame frame) {
            final Object[] rcvrAndArgs = (Object[]) popNReversedNode.executeRead(frame);
            final ClassObject rcvrClass = lookupClassNode.executeLookup(rcvrAndArgs[0]);
            final Object lookupResult = lookupMethodNode.executeLookup(rcvrClass, selector);
            final Object contextOrMarker = getContextOrMarker(frame);
            return dispatchSendNode.executeSend(frame, selector, lookupResult, rcvrClass, rcvrAndArgs, contextOrMarker);
        }

        public final Object getSelector() {
            return selector;
        }

        @Override
        public String toString() {
            return "send: " + selector.asString();
        }
    }

    public static final class SecondExtendedSendNode extends AbstractSendNode {
        public SecondExtendedSendNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int i) {
            super(code, index, numBytecodes, code.getLiteral(i & 63), i >> 6);
        }
    }

    public static final class SendLiteralSelectorNode extends AbstractSendNode {
        public static AbstractBytecodeNode create(final CompiledCodeObject code, final int index, final int numBytecodes, final int literalIndex, final int argCount) {
            final Object selector = code.getLiteral(literalIndex);
            return new SendLiteralSelectorNode(code, index, numBytecodes, selector, argCount);
        }

        public SendLiteralSelectorNode(final CompiledCodeObject code, final int index, final int numBytecodes, final Object selector, final int argCount) {
            super(code, index, numBytecodes, selector, argCount);
        }
    }

    public static final class SendSelectorNode extends AbstractSendNode {
        public static SendSelectorNode createForSpecialSelector(final CompiledCodeObject code, final int index, final int selectorIndex) {
            final NativeObject specialSelector = code.image.specialSelectorsArray[selectorIndex];
            final int numArguments = code.image.specialSelectorsNumArgs[selectorIndex];
            return new SendSelectorNode(code, index, 1, specialSelector, numArguments);
        }

        public SendSelectorNode(final CompiledCodeObject code, final int index, final int numBytecodes, final Object selector, final int argcount) {
            super(code, index, numBytecodes, selector, argcount);
        }
    }

    public static final class SendSelfSelector extends AbstractSendNode {
        public SendSelfSelector(final CompiledCodeObject code, final int index, final int numBytecodes, final Object selector, final int numArgs) {
            super(code, index, numBytecodes, selector, numArgs);
        }
    }

    public static final class SingleExtendedSendNode extends AbstractSendNode {
        public SingleExtendedSendNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int param) {
            super(code, index, numBytecodes, code.getLiteral(param & 31), param >> 5);
        }
    }

    public static final class SingleExtendedSuperNode extends AbstractSendNode {

        protected static class SqueakLookupClassSuperNode extends LookupClassNode {
            @Child private GetCompiledMethodNode getMethodNode = GetCompiledMethodNode.create();
            private final CompiledCodeObject code;

            public SqueakLookupClassSuperNode(final CompiledCodeObject code) {
                super(code.image);
                this.code = code; // storing both, image and code, because of class hierarchy
            }

            @Override
            public ClassObject executeLookup(final Object receiver) {
                final ClassObject compiledInClass = getMethodNode.execute(code).getCompiledInClass();
                final Object superclass = compiledInClass.getSuperclass();
                if (superclass == code.image.nil) {
                    return compiledInClass;
                } else {
                    return (ClassObject) superclass;
                }
            }
        }

        public SingleExtendedSuperNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int rawByte) {
            this(code, index, numBytecodes, rawByte & 31, rawByte >> 5);
        }

        public SingleExtendedSuperNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int literalIndex, final int numArgs) {
            super(code, index, numBytecodes, code.getLiteral(literalIndex), numArgs);
            lookupClassNode = new SqueakLookupClassSuperNode(code);
        }

        @Override
        public String toString() {
            return "sendSuper: " + selector.asString();
        }
    }
}
