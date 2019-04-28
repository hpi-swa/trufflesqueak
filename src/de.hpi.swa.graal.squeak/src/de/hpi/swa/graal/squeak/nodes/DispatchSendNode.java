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
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.MESSAGE;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.LookupClassNodes.LookupClassNode;
import de.hpi.swa.graal.squeak.util.ArrayUtils;
import de.hpi.swa.graal.squeak.util.MiscUtils;

@NodeInfo(cost = NodeCost.NONE)
public abstract class DispatchSendNode extends AbstractNodeWithImage {
    @Child private DispatchEagerlyNode dispatchNode = DispatchEagerlyNode.create();
    @Child private LookupMethodNode lookupNode;

    protected DispatchSendNode(final SqueakImageContext image) {
        super(image);
    }

    public static DispatchSendNode create(final SqueakImageContext image) {
        return DispatchSendNodeGen.create(image);
    }

    public abstract Object executeSend(VirtualFrame frame, NativeObject selector, Object lookupResult, ClassObject rcvrClass, Object[] receiverAndArguments, Object contextOrMarker);

    @Specialization(guards = {"!image.isHeadless() || isAllowedInHeadlessMode(selector)", "lookupResult != null"})
    protected final Object doDispatch(final VirtualFrame frame, @SuppressWarnings("unused") final NativeObject selector, final CompiledMethodObject lookupResult,
                    @SuppressWarnings("unused") final ClassObject rcvrClass, final Object[] rcvrAndArgs, final Object contextOrMarker) {
        return dispatchNode.executeDispatch(frame, lookupResult, rcvrAndArgs, contextOrMarker);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"image.isHeadless()", "selector.isDebugErrorSelector()", "lookupResult != null"})
    protected final Object doDispatchHeadlessError(final VirtualFrame frame, final NativeObject selector, final CompiledMethodObject lookupResult,
                    final ClassObject rcvrClass, final Object[] rcvrAndArgs, final Object contextOrMarker) {
        throw new SqueakError(this, MiscUtils.format("%s>>#%s detected in headless mode. Aborting...", rcvrClass.getSqueakClassName(), selector.asStringUnsafe()));
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"image.isHeadless()", "selector.isDebugSyntaxErrorSelector()", "lookupResult != null"})
    protected static final Object doDispatchHeadlessSyntaxError(final VirtualFrame frame, final NativeObject selector, final CompiledMethodObject lookupResult,
                    final ClassObject rcvrClass, final Object[] rcvrAndArgs, final Object contextOrMarker) {
        throw new SqueakSyntaxError((PointersObject) rcvrAndArgs[1]);
    }

    @Specialization(guards = {"lookupResult == null"})
    protected final Object doDoesNotUnderstand(final VirtualFrame frame, final NativeObject selector, @SuppressWarnings("unused") final Object lookupResult, final ClassObject rcvrClass,
                    final Object[] rcvrAndArgs, final Object contextOrMarker) {
        final CompiledMethodObject doesNotUnderstandMethod = (CompiledMethodObject) getLookupNode().executeLookup(rcvrClass, image.doesNotUnderstand);
        final PointersObject message = createMessage(selector, rcvrClass, ArrayUtils.allButFirst(rcvrAndArgs));
        return dispatchNode.executeDispatch(frame, doesNotUnderstandMethod, new Object[]{rcvrAndArgs[0], message}, contextOrMarker);
    }

    @Specialization(guards = {"!isCompiledMethodObject(targetObject)"})
    protected final Object doObjectAsMethod(final VirtualFrame frame, final NativeObject selector, final Object targetObject, @SuppressWarnings("unused") final ClassObject rcvrClass,
                    final Object[] rcvrAndArgs, final Object contextOrMarker,
                    @Cached final LookupClassNode lookupClassNode,
                    @Cached("createBinaryProfile()") final ConditionProfile isDoesNotUnderstandProfile) {
        final Object[] arguments = ArrayUtils.allButFirst(rcvrAndArgs);
        final ClassObject targetClass = lookupClassNode.executeLookup(targetObject);
        final Object newLookupResult = getLookupNode().executeLookup(targetClass, image.runWithInSelector);
        if (isDoesNotUnderstandProfile.profile(newLookupResult == null)) {
            final Object doesNotUnderstandMethod = getLookupNode().executeLookup(targetClass, image.doesNotUnderstand);
            return dispatchNode.executeDispatch(frame, (CompiledMethodObject) doesNotUnderstandMethod, new Object[]{targetObject, createMessage(selector, targetClass, arguments)}, contextOrMarker);
        } else {
            return dispatchNode.executeDispatch(frame, (CompiledMethodObject) newLookupResult, new Object[]{targetObject, selector, image.asArrayOfObjects(arguments), rcvrAndArgs[0]},
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
        final PointersObject message = new PointersObject(image, image.messageClass, image.messageClass.getBasicInstanceSize());
        message.atput0(MESSAGE.SELECTOR, selector);
        message.atput0(MESSAGE.ARGUMENTS, image.asArrayOfObjects(arguments));
        if (message.instsize() > MESSAGE.LOOKUP_CLASS) { // Early versions do not have lookupClass.
            message.atput0(MESSAGE.LOOKUP_CLASS, rcvrClass);
        }
        return message;
    }
}
