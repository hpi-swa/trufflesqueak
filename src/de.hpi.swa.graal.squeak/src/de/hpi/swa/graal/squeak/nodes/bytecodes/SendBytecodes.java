package de.hpi.swa.graal.squeak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveWithoutResultException;
import de.hpi.swa.graal.squeak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.graal.squeak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.nodes.DispatchSendNode;
import de.hpi.swa.graal.squeak.nodes.LookupClassNodes.AbstractLookupClassNode;
import de.hpi.swa.graal.squeak.nodes.LookupClassNodes.LookupClassNode;
import de.hpi.swa.graal.squeak.nodes.LookupClassNodes.LookupSuperClassNode;
import de.hpi.swa.graal.squeak.nodes.LookupMethodNode;
import de.hpi.swa.graal.squeak.nodes.context.stack.StackPopNReversedNode;
import de.hpi.swa.graal.squeak.nodes.context.stack.StackPushNode;

public final class SendBytecodes {

    public abstract static class AbstractSendNode extends AbstractBytecodeNode {
        protected final NativeObject selector;
        private final int argumentCount;

        @Child private AbstractLookupClassNode lookupClassNode;
        @Child private LookupMethodNode lookupMethodNode = LookupMethodNode.create();
        @Child private DispatchSendNode dispatchSendNode;
        @Child private StackPopNReversedNode popNReversedNode;
        @Child private StackPushNode pushNode;

        private final BranchProfile nlrProfile = BranchProfile.create();
        private final BranchProfile nvrProfile = BranchProfile.create();

        private AbstractSendNode(final CompiledCodeObject code, final int index, final int numBytecodes, final Object sel, final int argcount) {
            this(code, index, numBytecodes, sel, argcount, LookupClassNode.create());
        }

        private AbstractSendNode(final CompiledCodeObject code, final int index, final int numBytecodes, final Object sel, final int argcount, final AbstractLookupClassNode lookupClassNode) {
            super(code, index, numBytecodes);
            selector = sel instanceof NativeObject ? (NativeObject) sel : code.image.doesNotUnderstand;
            argumentCount = argcount;
            this.lookupClassNode = lookupClassNode;
            dispatchSendNode = DispatchSendNode.create(code.image);
            popNReversedNode = StackPopNReversedNode.create(code, 1 + argumentCount);
        }

        protected AbstractSendNode(final AbstractSendNode original) {
            this(original.code, original.index, original.numBytecodes, original.selector, original.argumentCount);
        }

        @Override
        public final void executeVoid(final VirtualFrame frame) {
            final Object result;
            try {
                /**
                 * Inline copy of {@link AbstractSendNode#executeSend} for better send performance.
                 */
                final Object[] rcvrAndArgs = popNReversedNode.executePopN(frame);
                final ClassObject rcvrClass = lookupClassNode.executeLookup(rcvrAndArgs[0]);
                final Object lookupResult = lookupMethodNode.executeLookup(rcvrClass, selector);
                result = dispatchSendNode.executeSend(frame, selector, lookupResult, rcvrClass, rcvrAndArgs, getContextOrMarker(frame));
                assert result != null : "Result of a message send should not be null";
                getPushNode().executeWrite(frame, result);
            } catch (final PrimitiveWithoutResultException e) {
                return; // ignoring result
            } catch (final NonLocalReturn nlr) {
                nlrProfile.enter();
                if (nlr.getTargetContextOrMarker() == getMarker(frame) || nlr.getTargetContextOrMarker() == getContext(frame)) {
                    getPushNode().executeWrite(frame, nlr.getReturnValue());
                } else {
                    throw nlr;
                }
            } catch (final NonVirtualReturn nvr) {
                nvrProfile.enter();
                if (nvr.getTargetContext() == getContext(frame)) {
                    getPushNode().executeWrite(frame, nvr.getReturnValue());
                } else {
                    throw nvr;
                }
            }
        }

        public final Object executeSend(final VirtualFrame frame) {
            final Object[] rcvrAndArgs = popNReversedNode.executePopN(frame);
            final ClassObject rcvrClass = lookupClassNode.executeLookup(rcvrAndArgs[0]);
            final Object lookupResult = lookupMethodNode.executeLookup(rcvrClass, selector);
            return dispatchSendNode.executeSend(frame, selector, lookupResult, rcvrClass, rcvrAndArgs, getContextOrMarker(frame));
        }

        private Object getContextOrMarker(final VirtualFrame frame) {
            final ContextObject context = getContext(frame);
            return context != null ? context : getMarker(frame);
        }

        private StackPushNode getPushNode() {
            if (pushNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                pushNode = insert(StackPushNode.create(code));
            }
            return pushNode;
        }

        public final Object getSelector() {
            return selector;
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "send: " + selector.asStringUnsafe();
        }
    }

    public static final class SecondExtendedSendNode extends AbstractSendNode {
        public SecondExtendedSendNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int i) {
            super(code, index, numBytecodes, code.getLiteral(i & 63), i >> 6);
        }
    }

    public static final class SendLiteralSelectorNode extends AbstractSendNode {
        public SendLiteralSelectorNode(final CompiledCodeObject code, final int index, final int numBytecodes, final Object selector, final int argCount) {
            super(code, index, numBytecodes, selector, argCount);
        }

        public static AbstractBytecodeNode create(final CompiledCodeObject code, final int index, final int numBytecodes, final int literalIndex, final int argCount) {
            final Object selector = code.getLiteral(literalIndex);
            return new SendLiteralSelectorNode(code, index, numBytecodes, selector, argCount);
        }
    }

    public static final class SendSelectorNode extends AbstractSendNode {
        public SendSelectorNode(final CompiledCodeObject code, final int index, final int numBytecodes, final Object selector, final int argcount) {
            super(code, index, numBytecodes, selector, argcount);
        }

        public static SendSelectorNode createForSpecialSelector(final CompiledCodeObject code, final int index, final int selectorIndex) {
            final NativeObject specialSelector = code.image.specialSelectorsArray[selectorIndex];
            final int numArguments = code.image.specialSelectorsNumArgs[selectorIndex];
            return new SendSelectorNode(code, index, 1, specialSelector, numArguments);
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

        public SingleExtendedSuperNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int rawByte) {
            this(code, index, numBytecodes, rawByte & 31, rawByte >> 5);
        }

        public SingleExtendedSuperNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int literalIndex, final int numArgs) {
            super(code, index, numBytecodes, code.getLiteral(literalIndex), numArgs, LookupSuperClassNode.create(code));
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "sendSuper: " + selector.asStringUnsafe();
        }
    }
}
