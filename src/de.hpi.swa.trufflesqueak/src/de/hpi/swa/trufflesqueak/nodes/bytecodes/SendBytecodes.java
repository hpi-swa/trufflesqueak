/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.debug.DebuggerTags;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.AbstractPointersObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.POINT;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodesFactory.SendSpecialSelectorQuickPointXNodeGen;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodesFactory.SendSpecialSelectorQuickPointYNodeGen;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackPushNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackWriteNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.LookupClassNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.DispatchPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeFactory;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeFactory.ArgumentsLocation;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ControlPrimitives.PrimExitToDebuggerNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class SendBytecodes {
    public abstract static class AbstractSendNode extends AbstractInstrumentableBytecodeNode {
        protected final int argumentCount;
        @CompilationFinal private int stackPointer = -1;

        @Child private FrameStackPushNode pushNode;

        private final ConditionProfile nlrProfile = ConditionProfile.create();
        private final ConditionProfile nvrProfile = ConditionProfile.create();

        private AbstractSendNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int numArgs) {
            super(code, index, numBytecodes);
            argumentCount = numArgs;
        }

        @Override
        public final void executeVoid(final VirtualFrame frame) {
            Object result;
            try {
                decrementStackPointer(frame);
                result = dispatchSend(frame);
            } catch (final NonLocalReturn nlr) {
                if (nlrProfile.profile(nlr.getTargetContextOrMarker() == FrameAccess.getMarker(frame) || nlr.getTargetContextOrMarker() == FrameAccess.getContext(frame))) {
                    result = nlr.getReturnValue();
                } else {
                    throw nlr;
                }
            } catch (final NonVirtualReturn nvr) {
                if (nvrProfile.profile(nvr.getTargetContext() == FrameAccess.getContext(frame))) {
                    result = nvr.getReturnValue();
                } else {
                    throw nvr;
                }
            }
            assert result != null : "Result of a message send should not be null";
            getPushNode().execute(frame, result);
        }

        private void decrementStackPointer(final VirtualFrame frame) {
            if (stackPointer == -1) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                stackPointer = FrameAccess.getStackPointer(frame) - numPop();
                assert stackPointer >= 0 : "Bad stack pointer";
            }
            FrameAccess.setStackPointer(frame, stackPointer);
        }

        protected int numPop() {
            return 1 + argumentCount;
        }

        protected abstract Object dispatchSend(VirtualFrame frame);

        private FrameStackPushNode getPushNode() {
            if (pushNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                pushNode = insert(FrameStackPushNode.create());
            }
            return pushNode;
        }

        public abstract NativeObject getSelector();

        @Override
        public final boolean hasTag(final Class<? extends Tag> tag) {
            if (tag == StandardTags.CallTag.class) {
                return true;
            }
            if (tag == DebuggerTags.AlwaysHalt.class) {
                return PrimExitToDebuggerNode.SELECTOR_NAME.equals(getSelector().asStringUnsafe());
            }
            return super.hasTag(tag);
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "send: " + getSelector().asStringUnsafe();
        }
    }

    public static final class SelfSendNode extends AbstractSendNode {
        @Child private DispatchSelectorNode dispatchNode;

        private SelfSendNode(final VirtualFrame frame, final CompiledCodeObject code, final int index, final int numBytecodes, final NativeObject selector, final int numArgs) {
            super(code, index, numBytecodes, numArgs);
            dispatchNode = DispatchSelectorNode.create(frame, selector, numArgs);
        }

        public static SelfSendNode create(final VirtualFrame frame, final CompiledCodeObject code, final int index, final int numBytecodes, final NativeObject selector, final int numArgs) {
            return new SelfSendNode(frame, code, index, numBytecodes, selector, numArgs);
        }

        @Override
        protected Object dispatchSend(final VirtualFrame frame) {
            return dispatchNode.execute(frame);
        }

        @Override
        public NativeObject getSelector() {
            return dispatchNode.getSelector();
        }
    }

    public static final class SuperSendNode extends AbstractSendNode {
        @Child private DispatchSelectorNode dispatchNode;

        public SuperSendNode(final VirtualFrame frame, final CompiledCodeObject code, final int index, final int numBytecodes, final byte param) {
            this(frame, code, index, numBytecodes, param & 31, Byte.toUnsignedInt(param) >> 5);
        }

        public SuperSendNode(final VirtualFrame frame, final CompiledCodeObject code, final int index, final int numBytecodes, final int literalIndex, final int numArgs) {
            super(code, index, numBytecodes, numArgs);
            final NativeObject selector = (NativeObject) code.getLiteral(literalIndex);
            dispatchNode = DispatchSelectorNode.createSuper(frame, code, selector, numArgs);
        }

        @Override
        protected Object dispatchSend(final VirtualFrame frame) {
            return dispatchNode.execute(frame);
        }

        @Override
        public NativeObject getSelector() {
            return dispatchNode.getSelector();
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "sendSuper: " + getSelector().asStringUnsafe();
        }
    }

    public static final class DirectedSuperSendNode extends AbstractSendNode {
        @Child private DispatchSelectorNode dispatchNode;

        public DirectedSuperSendNode(final VirtualFrame frame, final CompiledCodeObject code, final int index, final int numBytecodes, final int selectorLiteralIndex, final int numArgs) {
            super(code, index, numBytecodes, numArgs);
            assert 0 <= selectorLiteralIndex && selectorLiteralIndex < 65535 : "selectorLiteralIndex out of range";
            assert 0 <= numArgs && numArgs <= 31 : "numArgs out of range";
            final NativeObject selector = (NativeObject) code.getLiteral(selectorLiteralIndex);
            dispatchNode = DispatchSelectorNode.createDirectedSuper(frame, selector, numArgs);
        }

        @Override
        protected Object dispatchSend(final VirtualFrame frame) {
            return dispatchNode.execute(frame);
        }

        @Override
        protected int numPop() {
            return 1 + 1 + argumentCount; // pop directed class, receiver, args
        }

        @Override
        public NativeObject getSelector() {
            return dispatchNode.getSelector();
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "directedSuperSend: " + getSelector().asStringUnsafe();
        }
    }

    /*
     * Try to execute primitive for special selectors. Replaces itself with a normal send on
     * primitive failure. Modifies the stack only once, primitives read their arguments directly
     * from the stack.
     */
    public abstract static class AbstractSendSpecialSelectorQuickNode extends AbstractInstrumentableBytecodeNode {
        protected final int selectorIndex;

        @Child protected FrameStackWriteNode writeNode;

        protected AbstractSendSpecialSelectorQuickNode(final CompiledCodeObject code, final int index, final int selectorIndex) {
            super(code, index, 1);
            this.selectorIndex = selectorIndex;
        }

        public static AbstractBytecodeNode create(final VirtualFrame frame, final CompiledCodeObject code, final int index, final int selectorIndex) {
            int primitiveIndex = -1;
            final SqueakImageContext image = code.getSqueakClass().getImage();
            final NativeObject specialSelector = image.getSpecialSelector(selectorIndex);
            final int numArguments = image.getSpecialSelectorNumArgs(selectorIndex);
            if (0 <= selectorIndex && selectorIndex <= 15) { // arithmetic primitives
                /*
                 * Peek at receiver and only use a primitive if it is a SmallInteger (see
                 * #arithmeticSelectorPrimitive).
                 */
                final int receiverStackIndex = FrameAccess.getStackPointer(frame) - 2;
                final Object receiver = FrameAccess.getStackValue(frame, receiverStackIndex, FrameAccess.getNumArguments(frame));
                if (receiver instanceof Long || receiver instanceof Double) {
                    // TODO: can this be expanded to Characters and others?
                    final Object lookupResult = SqueakObjectClassNode.executeUncached(receiver).lookupInMethodDictSlow(specialSelector);
                    if (lookupResult instanceof CompiledCodeObject method && method.hasPrimitive()) {
                        assert method.getNumArgs() == numArguments;
                        primitiveIndex = method.primitiveIndex();
                    }
                }
            } else if (selectorIndex == 16 || selectorIndex == 17) { // #at:, #at:put:
                return new SendSpecialSelectorQuickWithClassCheck1OrMoreArgumentsNode(code, index, selectorIndex);
            } else if (selectorIndex == 18) { // #size
                return new SendSpecialSelectorQuickWithClassCheck0ArgumentsNode(code, index, selectorIndex);
            } else if (selectorIndex == 22) { // #==
                primitiveIndex = 110;
            } else if (selectorIndex == 23) { // #class
                primitiveIndex = 111;
            } else if (selectorIndex == 24) { // #~~
                primitiveIndex = 169;
            } else if (selectorIndex == 25 || selectorIndex == 26) { // #value, #value:
                /*
                 * Closure primitives must go through the normal send infrastructure. This node does
                 * not handle NonLocalReturn and NonVirtualReturn.
                 */
            } else if (selectorIndex == 28) { // #new
                return new SendSpecialSelectorQuickWithClassCheck0ArgumentsNode(code, index, selectorIndex);
            } else if (selectorIndex == 29) { // #new:
                return new SendSpecialSelectorQuickWithClassCheck1OrMoreArgumentsNode(code, index, selectorIndex);
            } else if (selectorIndex == 30) { // #x
                return SendSpecialSelectorQuickPointXNodeGen.create(code, index, selectorIndex);
            } else if (selectorIndex == 31) { // #y
                return SendSpecialSelectorQuickPointYNodeGen.create(code, index, selectorIndex);
            }
            if (primitiveIndex > 0) {
                final int numReceiverAndArguments = 1 + numArguments;
                final AbstractPrimitiveNode primitiveNode = PrimitiveNodeFactory.getOrCreateIndexed(primitiveIndex, numReceiverAndArguments);
                assert primitiveNode != null;
                final DispatchPrimitiveNode dispatchPrimitiveNode = DispatchPrimitiveNode.create(primitiveNode, ArgumentsLocation.ON_STACK_REVERSED, numReceiverAndArguments);
                if (numArguments == 0) {
                    return new SendSpecialSelectorQuick0ArgumentsNode(code, index, selectorIndex, dispatchPrimitiveNode);
                } else {
                    return new SendSpecialSelectorQuick1OrMoreArgumentsNode(code, index, selectorIndex, dispatchPrimitiveNode);
                }
            } else {
                return new SelfSendNode(frame, code, index, 1, specialSelector, numArguments);
            }
        }

        protected final void replaceWithSend(final VirtualFrame frame) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            // Replace with normal send (pc needs to be written)
            FrameAccess.setInstructionPointer(frame, getSuccessorIndex());
            // Lookup specialSelector and replace with normal send
            final SqueakImageContext image = SqueakImageContext.get(this);
            final NativeObject specialSelector = image.getSpecialSelector(selectorIndex);
            final int numArguments = image.getSpecialSelectorNumArgs(selectorIndex);
            final CompiledCodeObject code = FrameAccess.getCodeObject(frame);
            replace(new SelfSendNode(frame, code, index - code.getInitialPC(), 1, specialSelector, numArguments)).executeVoid(frame);
        }

        protected final int findNewStackPointer(final VirtualFrame frame) {
            final int numArguments = findNumArguments();
            final int stackPointer = FrameAccess.getStackPointer(frame) - (1 + numArguments) + 1;
            assert stackPointer <= CONTEXT.MAX_STACK_SIZE : "Bad stack pointer";
            return stackPointer;
        }

        protected final FrameStackWriteNode createFrameSlotWriteNode(final VirtualFrame frame) {
            return createFrameSlotWriteNode(frame, findNewStackPointer(frame));
        }

        protected static final FrameStackWriteNode createFrameSlotWriteNode(final VirtualFrame frame, final int newStackPointer) {
            return FrameStackWriteNode.create(frame, newStackPointer - 1);
        }

        protected final NativeObject findSelector() {
            return SqueakImageContext.get(this).getSpecialSelector(selectorIndex);
        }

        protected final int findNumArguments() {
            return SqueakImageContext.get(this).getSpecialSelectorNumArgs(selectorIndex);
        }

        @Override
        public final String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "send: " + findSelector().asStringUnsafe();
        }
    }

    private abstract static class SendSpecialSelectorQuickNode extends AbstractSendSpecialSelectorQuickNode {
        @Child protected DispatchPrimitiveNode primitiveNode;

        private SendSpecialSelectorQuickNode(final CompiledCodeObject code, final int index, final int selectorIndex, final DispatchPrimitiveNode primitiveNode) {
            super(code, index, selectorIndex);
            this.primitiveNode = primitiveNode;
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            try {
                popArgumentAndPush(frame, primitiveNode.execute(frame));
            } catch (final PrimitiveFailed pf) {
                replaceWithSend(frame);
            }
        }

        protected abstract void popArgumentAndPush(VirtualFrame frame, Object result);
    }

    private static final class SendSpecialSelectorQuick0ArgumentsNode extends SendSpecialSelectorQuickNode {
        private SendSpecialSelectorQuick0ArgumentsNode(final CompiledCodeObject code, final int index, final int selectorIndex, final DispatchPrimitiveNode primitiveNode) {
            super(code, index, selectorIndex, primitiveNode);
        }

        @Override
        protected void popArgumentAndPush(final VirtualFrame frame, final Object result) {
            if (writeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                assert findNumArguments() == 0;
                writeNode = insert(createFrameSlotWriteNode(frame));
            }
            writeNode.executeWrite(frame, result);
        }
    }

    private static final class SendSpecialSelectorQuick1OrMoreArgumentsNode extends SendSpecialSelectorQuickNode {
        @CompilationFinal private int stackPointer;

        private SendSpecialSelectorQuick1OrMoreArgumentsNode(final CompiledCodeObject code, final int index, final int selectorIndex, final DispatchPrimitiveNode primitiveNode) {
            super(code, index, selectorIndex, primitiveNode);
        }

        @Override
        protected void popArgumentAndPush(final VirtualFrame frame, final Object result) {
            if (writeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                assert findNumArguments() > 0;
                stackPointer = findNewStackPointer(frame);
                writeNode = insert(createFrameSlotWriteNode(frame, stackPointer));
            }
            FrameAccess.setStackPointer(frame, stackPointer);
            writeNode.executeWrite(frame, result);
        }
    }

    private abstract static class SendSpecialSelectorQuickWithClassCheckNode extends AbstractSendSpecialSelectorQuickNode {
        @CompilationFinal private ClassObject cachedReceiverClass;
        @CompilationFinal private Assumption cachedCallTargetStableAssumption;

        @Child protected DispatchPrimitiveNode primitiveNode;
        @Child private FrameStackReadNode peekAtReceiverNode;
        @Child private LookupClassNode lookupClassNode = LookupClassNode.create();

        private SendSpecialSelectorQuickWithClassCheckNode(final CompiledCodeObject code, final int index, final int selectorIndex) {
            super(code, index, selectorIndex);
        }

        @Override
        public void executeVoid(final VirtualFrame frame) {
            final Object receiver = peekAtReceiver(frame);
            if (doesNotMatchClassOrMethodInvalidated(lookupClassNode.execute(receiver))) {
                replaceWithSend(frame);
                return;
            }
            try {
                popArgumentsAndPush(frame, primitiveNode.execute(frame));
            } catch (final PrimitiveFailed pf) {
                replaceWithSend(frame);
            }
        }

        protected abstract void popArgumentsAndPush(VirtualFrame frame, Object result);

        private boolean doesNotMatchClassOrMethodInvalidated(final ClassObject actualClass) {
            if (primitiveNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                assert actualClass != null;
                final Object lookupResult = actualClass.lookupInMethodDictSlow(findSelector());
                if (lookupResult instanceof final CompiledCodeObject primitiveMethod && primitiveMethod.hasPrimitive()) {
                    assert primitiveMethod.getNumArgs() == findNumArguments();
                    final DispatchPrimitiveNode node = PrimitiveNodeFactory.getOrCreateIndexedOrNamed(primitiveMethod, ArgumentsLocation.ON_STACK_REVERSED);
                    if (node == null) {
                        return true; // primitive not found / supported
                    }
                    primitiveNode = insert(node);
                    cachedReceiverClass = actualClass;
                    cachedCallTargetStableAssumption = primitiveMethod.getCallTargetStable();
                } else {
                    return true;
                }
            }
            return actualClass != cachedReceiverClass || !cachedCallTargetStableAssumption.isValid();
        }

        private Object peekAtReceiver(final VirtualFrame frame) {
            if (peekAtReceiverNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                final int currentStackPointer = FrameAccess.getStackPointer(frame) - (1 + findNumArguments());
                peekAtReceiverNode = insert(FrameStackReadNode.create(frame, currentStackPointer, false));
            }
            return peekAtReceiverNode.executeRead(frame);
        }
    }

    private static final class SendSpecialSelectorQuickWithClassCheck0ArgumentsNode extends SendSpecialSelectorQuickWithClassCheckNode {
        private SendSpecialSelectorQuickWithClassCheck0ArgumentsNode(final CompiledCodeObject code, final int index, final int selectorIndex) {
            super(code, index, selectorIndex);
        }

        @Override
        protected void popArgumentsAndPush(final VirtualFrame frame, final Object result) {
            if (writeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                assert findNumArguments() == 0;
                writeNode = insert(createFrameSlotWriteNode(frame));
            }
            writeNode.executeWrite(frame, result);
        }
    }

    private static final class SendSpecialSelectorQuickWithClassCheck1OrMoreArgumentsNode extends SendSpecialSelectorQuickWithClassCheckNode {
        @CompilationFinal private int stackPointer;

        private SendSpecialSelectorQuickWithClassCheck1OrMoreArgumentsNode(final CompiledCodeObject code, final int index, final int selectorIndex) {
            super(code, index, selectorIndex);
        }

        @Override
        protected void popArgumentsAndPush(final VirtualFrame frame, final Object result) {
            if (writeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                assert findNumArguments() > 0;
                stackPointer = findNewStackPointer(frame);
                writeNode = insert(createFrameSlotWriteNode(frame, stackPointer));
            }
            FrameAccess.setStackPointer(frame, stackPointer);
            writeNode.executeWrite(frame, result);
        }
    }

    @GenerateCached(false)
    protected abstract static class AbstractSendSpecialSelectorQuickPointXYNode extends AbstractSendSpecialSelectorQuickNode {
        @Child private FrameStackReadNode peekAtReceiverNode;
        @Child private LookupClassNode lookupClassNode = LookupClassNode.create();
        @CompilationFinal private ClassObject pointClass;

        protected AbstractSendSpecialSelectorQuickPointXYNode(final CompiledCodeObject code, final int index, final int selectorIndex) {
            super(code, index, selectorIndex);
        }

        @Specialization
        protected final void doSend(final VirtualFrame frame,
                        @Bind("this") final Node node,
                        @Cached final AbstractPointersObjectReadNode readNode) {
            final Object receiver = peekAtReceiver(frame);
            if (lookupClassNode.execute(receiver) != getPointClass()) {
                replaceWithSend(frame);
                return;
            }
            popArgumentAndPush(frame, readNode.execute(node, (AbstractPointersObject) receiver, getPointInstVarIndex()));
        }

        private ClassObject getPointClass() {
            if (pointClass == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                pointClass = getContext().pointClass;
            }
            return pointClass;
        }

        protected abstract int getPointInstVarIndex();

        private void popArgumentAndPush(final VirtualFrame frame, final Object value) {
            if (writeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                assert findNumArguments() == 0;
                writeNode = insert(createFrameSlotWriteNode(frame));
            }
            writeNode.executeWrite(frame, value);
        }

        private Object peekAtReceiver(final VirtualFrame frame) {
            if (peekAtReceiverNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                final int currentStackPointer = FrameAccess.getStackPointer(frame) - 1;
                peekAtReceiverNode = insert(FrameStackReadNode.create(frame, currentStackPointer, false));
            }
            return peekAtReceiverNode.executeRead(frame);
        }
    }

    protected abstract static class SendSpecialSelectorQuickPointXNode extends AbstractSendSpecialSelectorQuickPointXYNode {
        protected SendSpecialSelectorQuickPointXNode(final CompiledCodeObject code, final int index, final int selectorIndex) {
            super(code, index, selectorIndex);
        }

        @Override
        protected final int getPointInstVarIndex() {
            return POINT.X;
        }
    }

    protected abstract static class SendSpecialSelectorQuickPointYNode extends AbstractSendSpecialSelectorQuickPointXYNode {
        protected SendSpecialSelectorQuickPointYNode(final CompiledCodeObject code, final int index, final int selectorIndex) {
            super(code, index, selectorIndex);
        }

        @Override
        protected final int getPointInstVarIndex() {
            return POINT.Y;
        }
    }
}
