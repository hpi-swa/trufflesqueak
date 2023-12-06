/*
 * Copyright (c) 2017-2023 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2023 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakSyntaxError;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.LookupMethodNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSendNodeFactory.DispatchSendSelectorNodeGen;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.MiscUtils;

@NodeInfo(cost = NodeCost.NONE)
public abstract class DispatchSendNode extends AbstractNode {

    public abstract Object executeSend(VirtualFrame frame, NativeObject selector, Object lookupResult, ClassObject rcvrClass, Object[] receiverAndArguments);

    public abstract static class DispatchSendSelectorNode extends DispatchSendNode {
        @Child private DispatchEagerlyNode dispatchNode = DispatchEagerlyNode.create();

        @NeverDefault
        public static DispatchSendSelectorNode create() {
            return DispatchSendSelectorNodeGen.create();
        }

        @Specialization(guards = {"lookupResult != null"})
        protected final Object doDispatch(final VirtualFrame frame, @SuppressWarnings("unused") final NativeObject selector, final CompiledCodeObject lookupResult,
                        @SuppressWarnings("unused") final ClassObject rcvrClass, final Object[] rcvrAndArgs) {
            return dispatchNode.executeDispatch(frame, lookupResult, rcvrAndArgs);
        }

        @Specialization(guards = {"lookupResult == null"})
        protected final Object doDoesNotUnderstand(final VirtualFrame frame, final NativeObject selector, @SuppressWarnings("unused") final Object lookupResult, final ClassObject rcvrClass,
                        final Object[] rcvrAndArgs,
                        @Shared("writeNode") @Cached final AbstractPointersObjectWriteNode writeNode,
                        @Shared("lookupNode") @Cached final LookupMethodNode lookupNode) {
            final SqueakImageContext image = getContext();
            final CompiledCodeObject doesNotUnderstandMethod = (CompiledCodeObject) lookupNode.executeLookup(rcvrClass, image.doesNotUnderstand);
            final PointersObject message = image.newMessage(writeNode, selector, rcvrClass, ArrayUtils.allButFirst(rcvrAndArgs));
            return dispatchNode.executeDispatch(frame, doesNotUnderstandMethod, new Object[]{rcvrAndArgs[0], message});
        }

        @Specialization(guards = {"!isCompiledCodeObject(targetObject)"})
        protected final Object doObjectAsMethod(final VirtualFrame frame, final NativeObject selector, final Object targetObject, @SuppressWarnings("unused") final ClassObject rcvrClass,
                        final Object[] rcvrAndArgs,
                        @Bind("this") final Node node,
                        @Cached final SqueakObjectClassNode classNode,
                        @Shared("writeNode") @Cached final AbstractPointersObjectWriteNode writeNode,
                        @Shared("lookupNode") @Cached final LookupMethodNode lookupNode,
                        @Cached final InlinedConditionProfile isDoesNotUnderstandProfile) {
            final SqueakImageContext image = getContext(node);
            final Object[] arguments = ArrayUtils.allButFirst(rcvrAndArgs);
            final ClassObject targetClass = classNode.executeLookup(targetObject);
            final Object newLookupResult = lookupNode.executeLookup(targetClass, image.runWithInSelector);
            if (isDoesNotUnderstandProfile.profile(node, newLookupResult == null)) {
                final Object doesNotUnderstandMethod = lookupNode.executeLookup(targetClass, image.doesNotUnderstand);
                return dispatchNode.executeDispatch(frame, (CompiledCodeObject) doesNotUnderstandMethod,
                                new Object[]{targetObject, image.newMessage(writeNode, selector, targetClass, arguments)});
            } else {
                return dispatchNode.executeDispatch(frame, (CompiledCodeObject) newLookupResult, new Object[]{targetObject, selector, image.asArrayOfObjects(arguments), rcvrAndArgs[0]});
            }
        }
    }

    public static final class DispatchSendHeadlessErrorNode extends DispatchSendNode {
        @Override
        public Object executeSend(final VirtualFrame frame, final NativeObject selector, final Object lookupResult, final ClassObject rcvrClass, final Object[] receiverAndArguments) {
            CompilerDirectives.transferToInterpreter();
            throw new SqueakException(MiscUtils.format("%s>>#%s detected in headless mode. Aborting...", rcvrClass.getSqueakClassName(), selector.asStringUnsafe()), this);
        }
    }

    public static final class DispatchSendSyntaxErrorNode extends DispatchSendNode {
        @Override
        public Object executeSend(final VirtualFrame frame, final NativeObject selector, final Object lookupResult, final ClassObject rcvrClass, final Object[] receiverAndArguments) {
            CompilerDirectives.transferToInterpreter();
            throw new SqueakSyntaxError((PointersObject) receiverAndArguments[1]);
        }
    }
}
