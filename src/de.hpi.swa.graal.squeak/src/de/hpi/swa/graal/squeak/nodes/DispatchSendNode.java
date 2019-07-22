package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.CompilerDirectives;
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
    @Child private DispatchEagerlyNode dispatchNode = DispatchEagerlyNode.create();
    @Child private LookupMethodNode lookupNode;

    protected DispatchSendNode(final CompiledCodeObject code) {
        super(code);
    }

    public static DispatchSendNode create(final CompiledCodeObject code) {
        return DispatchSendNodeGen.create(code);
    }

    public abstract Object executeSend(VirtualFrame frame, NativeObject selector, Object lookupResult, ClassObject rcvrClass, Object[] receiverAndArguments, Object contextOrMarker);

    @Specialization(guards = {"!code.image.isHeadless() || isAllowedInHeadlessMode(selector)", "lookupResult != null", "lookupResult == cachedMethod"}, //
                    assumptions = {"cachedMethod.getDoesNotNeedSenderAssumption()"})
    protected final Object doDispatch(final VirtualFrame frame, @SuppressWarnings("unused") final NativeObject selector, final CompiledMethodObject lookupResult,
                    @SuppressWarnings("unused") final ClassObject rcvrClass, final Object[] rcvrAndArgs, final Object contextOrMarker,
                    @SuppressWarnings("unused") @Cached("lookupResult") final CompiledMethodObject cachedMethod) {
        return dispatchNode.executeDispatch(frame, lookupResult, rcvrAndArgs, contextOrMarker);
    }

    @Specialization(guards = {"!code.image.isHeadless() || isAllowedInHeadlessMode(selector)", "lookupResult != null", "lookupResult == cachedMethod",
                    "!cachedMethod.getDoesNotNeedSenderAssumption().isValid()"})
    protected final Object doDispatchFoo(final VirtualFrame frame, @SuppressWarnings("unused") final NativeObject selector, final CompiledMethodObject lookupResult,
                    @SuppressWarnings("unused") final ClassObject rcvrClass, final Object[] rcvrAndArgs, @SuppressWarnings("unused") final Object contextOrMarker,
                    @SuppressWarnings("unused") @Cached("lookupResult") final CompiledMethodObject cachedMethod,
                    @Cached("create(code)") final GetOrCreateContextNode getOrCreateContextNode) {
        return dispatchNode.executeDispatch(frame, lookupResult, rcvrAndArgs, getOrCreateContextNode.executeGet(frame));
    }

    @Specialization(guards = {"!code.image.isHeadless() || isAllowedInHeadlessMode(selector)", "lookupResult != null"})
    protected final Object doDispatchNeedsSender(final VirtualFrame frame, @SuppressWarnings("unused") final NativeObject selector, final CompiledMethodObject lookupResult,
                    @SuppressWarnings("unused") final ClassObject rcvrClass, final Object[] rcvrAndArgs, @SuppressWarnings("unused") final Object contextOrMarker,
                    @Cached("create(code)") final GetOrCreateContextNode getOrCreateContextNode) {
        return dispatchNode.executeDispatch(frame, lookupResult, rcvrAndArgs, getOrCreateContextNode.executeGet(frame));
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"code.image.isHeadless()", "selector.isDebugErrorSelector()", "lookupResult != null"})
    protected final Object doDispatchHeadlessError(final VirtualFrame frame, final NativeObject selector, final CompiledMethodObject lookupResult,
                    final ClassObject rcvrClass, final Object[] rcvrAndArgs, final Object contextOrMarker) {
        throw new SqueakError(this, MiscUtils.format("%s>>#%s detected in headless mode. Aborting...", rcvrClass.getSqueakClassName(), selector.asStringUnsafe()));
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"code.image.isHeadless()", "selector.isDebugSyntaxErrorSelector()", "lookupResult != null"})
    protected static final Object doDispatchHeadlessSyntaxError(final VirtualFrame frame, final NativeObject selector, final CompiledMethodObject lookupResult,
                    final ClassObject rcvrClass, final Object[] rcvrAndArgs, final Object contextOrMarker) {
        throw new SqueakSyntaxError((PointersObject) rcvrAndArgs[1]);
    }

    @Specialization(guards = {"lookupResult == null"})
    protected final Object doDoesNotUnderstand(final VirtualFrame frame, final NativeObject selector, @SuppressWarnings("unused") final Object lookupResult, final ClassObject rcvrClass,
                    final Object[] rcvrAndArgs, final Object contextOrMarker) {
        final CompiledMethodObject doesNotUnderstandMethod = (CompiledMethodObject) getLookupNode().executeLookup(rcvrClass, code.image.doesNotUnderstand);
        final PointersObject message = createMessage(selector, rcvrClass, ArrayUtils.allButFirst(rcvrAndArgs));
        return dispatchNode.executeDispatch(frame, doesNotUnderstandMethod, new Object[]{rcvrAndArgs[0], message}, contextOrMarker);
    }

    @Specialization(guards = {"!isCompiledMethodObject(targetObject)"})
    protected final Object doObjectAsMethod(final VirtualFrame frame, final NativeObject selector, final Object targetObject, @SuppressWarnings("unused") final ClassObject rcvrClass,
                    final Object[] rcvrAndArgs, final Object contextOrMarker,
                    @Cached final SqueakObjectClassNode classNode,
                    @Cached("createBinaryProfile()") final ConditionProfile isDoesNotUnderstandProfile) {
        final Object[] arguments = ArrayUtils.allButFirst(rcvrAndArgs);
        final ClassObject targetClass = classNode.executeLookup(targetObject);
        final Object newLookupResult = getLookupNode().executeLookup(targetClass, code.image.runWithInSelector);
        if (isDoesNotUnderstandProfile.profile(newLookupResult == null)) {
            final Object doesNotUnderstandMethod = getLookupNode().executeLookup(targetClass, code.image.doesNotUnderstand);
            return dispatchNode.executeDispatch(frame, (CompiledMethodObject) doesNotUnderstandMethod, new Object[]{targetObject, createMessage(selector, targetClass, arguments)}, contextOrMarker);
        } else {
            return dispatchNode.executeDispatch(frame, (CompiledMethodObject) newLookupResult, new Object[]{targetObject, selector, code.image.asArrayOfObjects(arguments), rcvrAndArgs[0]},
                            contextOrMarker);
        }
    }

    protected static final boolean isAllowedInHeadlessMode(final NativeObject selector) {
        return !selector.isDebugErrorSelector() && !selector.isDebugSyntaxErrorSelector();
    }

    private LookupMethodNode getLookupNode() {
        if (lookupNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            lookupNode = insert(LookupMethodNode.create());
        }
        return lookupNode;
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
