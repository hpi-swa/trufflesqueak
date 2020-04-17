/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakError;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakSyntaxError;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.DispatchSendFromStackNodeFactory.DispatchSendSelectorNodeGen;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackPopNNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackPopNode;
import de.hpi.swa.graal.squeak.util.MiscUtils;

@NodeInfo(cost = NodeCost.NONE)
public abstract class DispatchSendFromStackNode extends AbstractNode {

    public static DispatchSendFromStackNode create(final NativeObject selector, final CompiledCodeObject code, final int argumentCount) {
        if (code.image.isHeadless()) {
            if (selector.isDebugErrorSelector()) {
                return new DispatchSendHeadlessErrorNode();
            } else if (selector.isDebugSyntaxErrorSelector()) {
                return new DispatchSendSyntaxErrorNode(argumentCount);
            }
        }
        return DispatchSendSelectorNodeGen.create(argumentCount);
    }

    public abstract Object executeSend(VirtualFrame frame, NativeObject selector, Object lookupResult, Object receiver, ClassObject rcvrClass);

    public abstract static class DispatchSendSelectorNode extends DispatchSendFromStackNode {
        protected final int argumentCount;

        protected DispatchSendSelectorNode(final int argumentCount) {
            this.argumentCount = argumentCount;
        }

        public static DispatchSendSelectorNode create(final CompiledCodeObject code, final int argumentCount) {
            return DispatchSendSelectorNodeGen.create(code, argumentCount);
        }

        @Specialization(guards = {"lookupResult != null"})
        protected static final Object doDispatch(final VirtualFrame frame, @SuppressWarnings("unused") final NativeObject selector, final CompiledMethodObject lookupResult,
                        @SuppressWarnings("unused") final Object receiver, @SuppressWarnings("unused") final ClassObject rcvrClass,
                        @Cached("create(argumentCount)") final DispatchEagerlyFromStackNode dispatchNode) {
            return dispatchNode.executeDispatch(frame, lookupResult);
        }

        @Specialization(guards = {"lookupResult == null"})
        protected static final Object doDoesNotUnderstand(final VirtualFrame frame, final NativeObject selector, @SuppressWarnings("unused") final Object lookupResult, final Object receiver,
                        final ClassObject rcvrClass,
                        @Shared("writeNode") @Cached final AbstractPointersObjectWriteNode writeNode,
                        @Cached final LookupMethodNode lookupNode,
                        @Cached("create(argumentCount)") final FrameStackPopNNode popArguments,
                        @Cached final FrameStackPopNode popReceiver,
                        @Cached("create()") final DispatchEagerlyNode dispatchNode,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            final CompiledMethodObject doesNotUnderstandMethod = (CompiledMethodObject) lookupNode.executeLookup(rcvrClass, image.doesNotUnderstand);
            final PointersObject message = image.newMessage(writeNode, selector, rcvrClass, popArguments.execute(frame));
            final Object poppedReceiver = popReceiver.execute(frame);
            assert receiver == poppedReceiver;
            return dispatchNode.executeDispatch(frame, doesNotUnderstandMethod, new Object[]{receiver, message});
        }

        @Specialization(guards = {"!isCompiledMethodObject(targetObject)"})
        protected static final Object doObjectAsMethod(final VirtualFrame frame, final NativeObject selector, final Object targetObject, final Object receiver,
                        @SuppressWarnings("unused") final ClassObject rcvrClass,
                        @Cached final SqueakObjectClassNode classNode,
                        @Shared("writeNode") @Cached final AbstractPointersObjectWriteNode writeNode,
                        @Cached final LookupMethodNode lookupNode,
                        @Cached("create(argumentCount)") final FrameStackPopNNode popArguments,
                        @Cached final FrameStackPopNode popReceiver,
                        @Cached("createBinaryProfile()") final ConditionProfile isDoesNotUnderstandProfile,
                        @Cached("create()") final DispatchEagerlyNode dispatchNode,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            final Object[] arguments = popArguments.execute(frame);
            final Object poppedReceiver = popReceiver.execute(frame);
            assert receiver == poppedReceiver;
            final ClassObject targetClass = classNode.executeLookup(targetObject);
            final Object newLookupResult = lookupNode.executeLookup(targetClass, image.runWithInSelector);
            if (isDoesNotUnderstandProfile.profile(newLookupResult == null)) {
                final Object doesNotUnderstandMethod = lookupNode.executeLookup(targetClass, image.doesNotUnderstand);
                return dispatchNode.executeDispatch(frame, (CompiledMethodObject) doesNotUnderstandMethod,
                                new Object[]{targetObject, image.newMessage(writeNode, selector, targetClass, arguments)});
            } else {
                return dispatchNode.executeDispatch(frame, (CompiledMethodObject) newLookupResult, new Object[]{targetObject, selector, image.asArrayOfObjects(arguments), receiver});
            }
        }
    }

    private static final class DispatchSendHeadlessErrorNode extends DispatchSendFromStackNode {
        @Override
        public Object executeSend(final VirtualFrame frame, final NativeObject selector, final Object lookupResult, final Object receiver, final ClassObject rcvrClass) {
            CompilerDirectives.transferToInterpreter();
            throw new SqueakError(this, MiscUtils.format("%s>>#%s detected in headless mode. Aborting...", rcvrClass.getSqueakClassName(), selector.asStringUnsafe()));
        }
    }

    private static final class DispatchSendSyntaxErrorNode extends DispatchSendFromStackNode {
        private final int argumentCount;

        private DispatchSendSyntaxErrorNode(final int argumentCount) {
            this.argumentCount = argumentCount;
        }

        @Override
        public Object executeSend(final VirtualFrame frame, final NativeObject selector, final Object lookupResult, final Object receiver, final ClassObject rcvrClass) {
            CompilerDirectives.transferToInterpreter();
            // TODO: make this better
            throw new SqueakSyntaxError((PointersObject) FrameStackPopNNode.create(argumentCount).execute(frame)[0]);
        }
    }
}
