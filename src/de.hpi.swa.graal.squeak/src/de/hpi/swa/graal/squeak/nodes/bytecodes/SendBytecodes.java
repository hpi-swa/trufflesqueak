/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.debug.DebuggerTags;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.graal.squeak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.graal.squeak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.nodes.AbstractLookupMethodWithSelectorNodes.AbstractLookupMethodWithSelectorNode;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;
import de.hpi.swa.graal.squeak.nodes.DispatchSendNode;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackPopNNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackPushNode;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.ControlPrimitives.PrimExitToDebuggerNode;

public final class SendBytecodes {
    public abstract static class AbstractSendNode extends AbstractInstrumentableBytecodeNode {
        public static final Object NO_RESULT = new Object();

        protected final NativeObject selector;
        private final int argumentCount;

        @Child private AbstractLookupClassNode lookupClassNode;
        @Child private AbstractLookupMethodWithSelectorNode lookupMethodNode;
        @Child private DispatchSendNode dispatchSendNode;
        @Child private FrameStackPopNNode popNNode;
        @Child private FrameStackPushNode pushNode;

        private final ConditionProfile noResultProfile = ConditionProfile.createBinaryProfile();
        private final BranchProfile nlrProfile = BranchProfile.create();
        private final BranchProfile nvrProfile = BranchProfile.create();

        private AbstractSendNode(final CompiledCodeObject code, final int index, final int numBytecodes, final Object sel, final int argcount) {
            this(code, index, numBytecodes, sel, argcount, new LookupClassNode());
        }

        private AbstractSendNode(final CompiledCodeObject code, final int index, final int numBytecodes, final Object sel, final int argcount, final AbstractLookupClassNode lookupClassNode) {
            super(code, index, numBytecodes);
            selector = sel instanceof NativeObject ? (NativeObject) sel : code.image.doesNotUnderstand;
            argumentCount = argcount;
            this.lookupClassNode = lookupClassNode;
            lookupMethodNode = AbstractLookupMethodWithSelectorNode.create(selector);
            dispatchSendNode = DispatchSendNode.create(code);
            popNNode = FrameStackPopNNode.create(code, 1 + argumentCount); // receiver + arguments.
        }

        protected AbstractSendNode(final AbstractSendNode original) {
            this(original.code, original.index, original.numBytecodes, original.selector, original.argumentCount);
        }

        @Override
        public final void executeVoid(final VirtualFrame frame) {
            final Object result;
            try {
                final Object[] rcvrAndArgs = popNNode.execute(frame);
                final ClassObject rcvrClass = lookupClassNode.executeLookup(rcvrAndArgs[0]);
                final Object lookupResult = lookupMethodNode.executeLookup(rcvrClass);
                result = dispatchSendNode.executeSend(frame, selector, lookupResult, rcvrClass, rcvrAndArgs);
                assert result != null : "Result of a message send should not be null";
                if (noResultProfile.profile(result != NO_RESULT)) {
                    getPushNode().execute(frame, result);
                }
            } catch (final NonLocalReturn nlr) {
                nlrProfile.enter();
                if (nlr.getTargetContextOrMarker() == getMarker(frame) || nlr.getTargetContextOrMarker() == getContext(frame)) {
                    getPushNode().execute(frame, nlr.getReturnValue());
                } else {
                    throw nlr;
                }
            } catch (final NonVirtualReturn nvr) {
                nvrProfile.enter();
                if (nvr.getTargetContext() == getContext(frame)) {
                    getPushNode().execute(frame, nvr.getReturnValue());
                } else {
                    throw nvr;
                }
            }
        }

        private FrameStackPushNode getPushNode() {
            if (pushNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                pushNode = insert(FrameStackPushNode.create(code));
            }
            return pushNode;
        }

        public final Object getSelector() {
            return selector;
        }

        @Override
        public final boolean hasTag(final Class<? extends Tag> tag) {
            if (tag == StandardTags.CallTag.class) {
                return true;
            }
            if (tag == DebuggerTags.AlwaysHalt.class) {
                return PrimExitToDebuggerNode.SELECTOR_NAME.equals(selector.asStringUnsafe());
            }
            return super.hasTag(tag);
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "send: " + selector.asStringUnsafe();
        }
    }

    protected abstract static class AbstractLookupClassNode extends AbstractNode {
        protected abstract ClassObject executeLookup(Object receiver);
    }

    @NodeInfo(cost = NodeCost.NONE)
    protected static final class LookupClassNode extends AbstractLookupClassNode {
        @Child private SqueakObjectClassNode lookupClassNode = SqueakObjectClassNode.create();

        @Override
        protected ClassObject executeLookup(final Object receiver) {
            return lookupClassNode.executeLookup(receiver);
        }
    }

    protected static final class LookupSuperClassNode extends AbstractLookupClassNode {
        @Child private AbstractPointersObjectReadNode readNode = AbstractPointersObjectReadNode.create();

        private final ConditionProfile hasSuperclassProfile = ConditionProfile.createBinaryProfile();
        private final CompiledMethodObject method;

        protected LookupSuperClassNode(final CompiledCodeObject code) {
            method = code.getMethod();
        }

        @Override
        protected ClassObject executeLookup(final Object receiver) {
            final ClassObject methodClass = method.getMethodClass(readNode);
            final ClassObject superclass = methodClass.getSuperclassOrNull();
            return hasSuperclassProfile.profile(superclass == null) ? methodClass : superclass;
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

        public static AbstractInstrumentableBytecodeNode create(final CompiledCodeObject code, final int index, final int numBytecodes, final int literalIndex, final int argCount) {
            final Object selector = code.getLiteral(literalIndex);
            return new SendLiteralSelectorNode(code, index, numBytecodes, selector, argCount);
        }
    }

    public static final class SendSpecialSelectorNode extends AbstractSendNode {
        private SendSpecialSelectorNode(final CompiledCodeObject code, final int index, final int numBytecodes, final Object selector, final int argcount) {
            super(code, index, numBytecodes, selector, argcount);
        }

        public static SendSpecialSelectorNode create(final CompiledCodeObject code, final int index, final int selectorIndex) {
            final NativeObject specialSelector = code.image.getSpecialSelector(selectorIndex);
            final int numArguments = code.image.getSpecialSelectorNumArgs(selectorIndex);
            return new SendSpecialSelectorNode(code, index, 1, specialSelector, numArguments);
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
            super(code, index, numBytecodes, code.getLiteral(literalIndex), numArgs, new LookupSuperClassNode(code));
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "sendSuper: " + selector.asStringUnsafe();
        }
    }
}
