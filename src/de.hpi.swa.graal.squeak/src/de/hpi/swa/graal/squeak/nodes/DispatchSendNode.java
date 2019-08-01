package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Cached;
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
import de.hpi.swa.graal.squeak.model.ObjectLayouts.MESSAGE;
import de.hpi.swa.graal.squeak.model.PointersObject;
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

    @Specialization(guards = {"!code.image.isHeadless() || isAllowedInHeadlessMode(selector)", "lookupResult != null"})
    protected final Object doDispatch(final VirtualFrame frame, @SuppressWarnings("unused") final NativeObject selector, @SuppressWarnings("unused") final CompiledMethodObject lookupResult,
                    @SuppressWarnings("unused") final ClassObject rcvrClass, final Object[] rcvrAndArgs) {
        return dispatchNode.executeDispatch(frame, lookupResult, rcvrAndArgs);
    }

    @Specialization(guards = {"!code.image.isHeadless() || isAllowedInHeadlessMode(selector)", "lookupResult != null"})
    protected final Object doDispatchNeedsSender(final VirtualFrame frame, @SuppressWarnings("unused") final NativeObject selector, final CompiledMethodObject lookupResult,
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
                    @Cached final LookupMethodNode lookupNode) {
        final CompiledMethodObject doesNotUnderstandMethod = (CompiledMethodObject) lookupNode.executeLookup(rcvrClass, code.image.doesNotUnderstand);
        final PointersObject message = createMessage(selector, rcvrClass, ArrayUtils.allButFirst(rcvrAndArgs));
        return dispatchNode.executeDispatch(frame, doesNotUnderstandMethod, new Object[]{rcvrAndArgs[0], message});
    }

    @Specialization(guards = {"!isCompiledMethodObject(targetObject)"})
    protected final Object doObjectAsMethod(final VirtualFrame frame, final NativeObject selector, final Object targetObject, @SuppressWarnings("unused") final ClassObject rcvrClass,
                    final Object[] rcvrAndArgs,
                    @Cached final SqueakObjectClassNode classNode,
                    @Cached final LookupMethodNode lookupNode,
                    @Cached("createBinaryProfile()") final ConditionProfile isDoesNotUnderstandProfile) {
        final Object[] arguments = ArrayUtils.allButFirst(rcvrAndArgs);
        final ClassObject targetClass = classNode.executeLookup(targetObject);
        final Object newLookupResult = lookupNode.executeLookup(targetClass, code.image.runWithInSelector);
        if (isDoesNotUnderstandProfile.profile(newLookupResult == null)) {
            final Object doesNotUnderstandMethod = lookupNode.executeLookup(targetClass, code.image.doesNotUnderstand);
            return dispatchNode.executeDispatch(frame, (CompiledMethodObject) doesNotUnderstandMethod, new Object[]{targetObject, createMessage(selector, targetClass, arguments)});
        } else {
            return dispatchNode.executeDispatch(frame, (CompiledMethodObject) newLookupResult, new Object[]{targetObject, selector, code.image.asArrayOfObjects(arguments), rcvrAndArgs[0]});
        }
    }

    protected static final boolean isAllowedInHeadlessMode(final NativeObject selector) {
        return !selector.isDebugErrorSelector() && !selector.isDebugSyntaxErrorSelector();
    }

    private PointersObject createMessage(final NativeObject selector, final ClassObject rcvrClass, final Object[] arguments) {
        final PointersObject message = new PointersObject(code.image, code.image.messageClass, code.image.messageClass.getBasicInstanceSize());
        message.atput0(MESSAGE.SELECTOR, selector);
        message.atput0(MESSAGE.ARGUMENTS, code.image.asArrayOfObjects(arguments));
        if (message.instsize() > MESSAGE.LOOKUP_CLASS) { // Early versions do not have lookupClass.
            message.atput0(MESSAGE.LOOKUP_CLASS, rcvrClass);
        }
        return message;
    }
}
