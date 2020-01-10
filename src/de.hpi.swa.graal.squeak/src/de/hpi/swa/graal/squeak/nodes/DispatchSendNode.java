/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakError;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakSyntaxError;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.AbstractLookupMethodWithSelectorNodes.AbstractLookupMethodWithSelectorNode;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.graal.squeak.util.ArrayUtils;
import de.hpi.swa.graal.squeak.util.MiscUtils;

@NodeInfo(cost = NodeCost.NONE)
public abstract class DispatchSendNode extends AbstractNodeWithCode {
    @Child private DispatchEagerlyNode dispatchNode;

    protected DispatchSendNode(final CompiledCodeObject code) {
        super(code);
        dispatchNode = DispatchEagerlyNode.create(code);
    }

    public static DispatchSendNode create(final CompiledCodeObject code) {
        return DispatchSendNodeGen.create(code);
    }

    public abstract Object executeSend(VirtualFrame frame, NativeObject selector, Object lookupResult, ClassObject rcvrClass, Object[] receiverAndArguments);

    @Specialization(guards = {"!code.image.isHeadless() || selector.isAllowedInHeadlessMode()", "lookupResult != null"})
    protected final Object doDispatch(final VirtualFrame frame, @SuppressWarnings("unused") final NativeObject selector, @SuppressWarnings("unused") final CompiledMethodObject lookupResult,
                    @SuppressWarnings("unused") final ClassObject rcvrClass, final Object[] rcvrAndArgs) {
        return dispatchNode.executeDispatch(frame, lookupResult, rcvrAndArgs);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"code.image.isHeadless()", "selector.isDebugErrorSelector()", "lookupResult != null"})
    protected final Object doDispatchHeadlessError(final VirtualFrame frame, final NativeObject selector, final CompiledMethodObject lookupResult,
                    final ClassObject rcvrClass, final Object[] rcvrAndArgs) {
        throw new SqueakError(this, MiscUtils.format("%s>>#%s detected in headless mode. Aborting...", rcvrClass.getSqueakClassName(), selector.asStringUnsafe()));
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"code.image.isHeadless()", "selector.isDebugSyntaxErrorSelector()", "lookupResult != null"})
    protected static final Object doDispatchHeadlessSyntaxError(final VirtualFrame frame, final NativeObject selector, final CompiledMethodObject lookupResult,
                    final ClassObject rcvrClass, final Object[] rcvrAndArgs) {
        throw new SqueakSyntaxError((PointersObject) rcvrAndArgs[1]);
    }

    @Specialization(guards = {"lookupResult == null"})
    protected final Object doDoesNotUnderstand(final VirtualFrame frame, final NativeObject selector, @SuppressWarnings("unused") final Object lookupResult, final ClassObject rcvrClass,
                    final Object[] rcvrAndArgs,
                    @Shared("writeNode") @Cached final AbstractPointersObjectWriteNode writeNode,
                    @Cached(value = "create(code.image.doesNotUnderstand)", allowUncached = true) final AbstractLookupMethodWithSelectorNode lookupNode) {
        final CompiledMethodObject doesNotUnderstandMethod = (CompiledMethodObject) lookupNode.executeLookup(rcvrClass);
        final PointersObject message = code.image.newMessage(writeNode, selector, rcvrClass, ArrayUtils.allButFirst(rcvrAndArgs));
        return dispatchNode.executeDispatch(frame, doesNotUnderstandMethod, new Object[]{rcvrAndArgs[0], message});
    }

    @Specialization(guards = {"!isCompiledMethodObject(targetObject)"})
    protected final Object doObjectAsMethod(final VirtualFrame frame, final NativeObject selector, final Object targetObject, @SuppressWarnings("unused") final ClassObject rcvrClass,
                    final Object[] rcvrAndArgs,
                    @Cached final SqueakObjectClassNode classNode,
                    @Shared("writeNode") @Cached final AbstractPointersObjectWriteNode writeNode,
                    @Cached(value = "create(code.image.runWithInSelector)", allowUncached = true) final AbstractLookupMethodWithSelectorNode lookupNode1,
                    @Cached(value = "create(code.image.doesNotUnderstand)", allowUncached = true) final AbstractLookupMethodWithSelectorNode lookupNode2,
                    @Cached("createBinaryProfile()") final ConditionProfile isDoesNotUnderstandProfile) {
        final Object[] arguments = ArrayUtils.allButFirst(rcvrAndArgs);
        final ClassObject targetClass = classNode.executeLookup(targetObject);
        final CompiledMethodObject newLookupResult = (CompiledMethodObject) lookupNode1.executeLookup(targetClass);
        if (isDoesNotUnderstandProfile.profile(newLookupResult == null)) {
            final CompiledMethodObject doesNotUnderstandMethod = (CompiledMethodObject) lookupNode2.executeLookup(targetClass);
            return dispatchNode.executeDispatch(frame, doesNotUnderstandMethod, new Object[]{targetObject, code.image.newMessage(writeNode, selector, targetClass, arguments)});
        } else {
            return dispatchNode.executeDispatch(frame, newLookupResult, new Object[]{targetObject, selector, code.image.asArrayOfObjects(arguments), rcvrAndArgs[0]});
        }
    }
}
