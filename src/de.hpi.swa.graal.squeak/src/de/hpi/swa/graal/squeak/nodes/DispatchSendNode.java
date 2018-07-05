package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.MESSAGE;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.accessing.CompiledCodeNodes.IsDoesNotUnderstandNode;
import de.hpi.swa.graal.squeak.nodes.context.SqueakLookupClassNode;
import de.hpi.swa.graal.squeak.util.ArrayUtils;

public abstract class DispatchSendNode extends AbstractNodeWithImage {
    @Child protected IsDoesNotUnderstandNode isDoesNotUnderstandNode;
    @Child private DispatchNode dispatchNode = DispatchNode.create();
    @Child private LookupNode lookupNode;
    @Child private SqueakLookupClassNode lookupClassNode;

    @CompilationFinal private ClassObject messageClass;
    @CompilationFinal private NativeObject runWithIn;

    public static DispatchSendNode create(final SqueakImageContext image) {
        return DispatchSendNodeGen.create(image);
    }

    protected DispatchSendNode(final SqueakImageContext image) {
        super(image);
        isDoesNotUnderstandNode = IsDoesNotUnderstandNode.create(image);
        lookupNode = LookupNode.create(image);
        lookupClassNode = SqueakLookupClassNode.create(image);
    }

    public abstract Object executeSend(VirtualFrame frame, NativeObject selector, Object lookupResult, ClassObject rcvrClass, Object[] receiverAndArguments, Object contextOrMarker);

    @Specialization(guards = {"!isDoesNotUnderstandNode.execute(lookupResult)"})
    protected final Object doDispatch(final VirtualFrame frame, @SuppressWarnings("unused") final NativeObject selector, final CompiledMethodObject lookupResult,
                    @SuppressWarnings("unused") final ClassObject rcvrClass, final Object[] rcvrAndArgs,
                    final Object contextOrMarker) {
        return dispatchNode.executeDispatch(frame, lookupResult, rcvrAndArgs, contextOrMarker);
    }

    @Specialization(guards = {"isDoesNotUnderstandNode.execute(lookupResult)"})
    protected final Object doDoesNotUnderstand(final VirtualFrame frame, final NativeObject selector, final CompiledMethodObject lookupResult, final ClassObject rcvrClass, final Object[] rcvrAndArgs,
                    final Object contextOrMarker) {
        final PointersObject message = createMessage(selector, rcvrClass, ArrayUtils.allButFirst(rcvrAndArgs));
        return dispatchNode.executeDispatch(frame, lookupResult, new Object[]{rcvrAndArgs[0], message}, contextOrMarker);
    }

    private PointersObject createMessage(final NativeObject selector, final ClassObject rcvrClass, final Object[] arguments) {
        final PointersObject message = (PointersObject) getMessageClass().newInstance();
        message.atput0(MESSAGE.SELECTOR, selector);
        message.atput0(MESSAGE.ARGUMENTS, image.newList(arguments));
        if (message.instsize() > MESSAGE.LOOKUP_CLASS) {
            // early versions do not have lookupClass
            message.atput0(MESSAGE.LOOKUP_CLASS, rcvrClass);
        }
        return message;
    }

    private ClassObject getMessageClass() {
        if (messageClass == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            messageClass = (ClassObject) image.specialObjectsArray.at0(SPECIAL_OBJECT_INDEX.ClassMessage);
        }
        return messageClass;
    }

    @Fallback
    protected final Object doObjectAsMethod(final VirtualFrame frame, final NativeObject selector, final Object targetObject, @SuppressWarnings("unused") final ClassObject rcvrClass,
                    final Object[] rcvrAndArgs, final Object contextOrMarker) {
        final Object[] arguments = ArrayUtils.allButFirst(rcvrAndArgs);
        final ClassObject targetClass = lookupClassNode.executeLookup(targetObject);
        final Object newLookupResult = lookupNode.executeLookup(targetClass, getRunWithIn());
        if (isDoesNotUnderstandNode.execute(newLookupResult)) {
            return dispatchNode.executeDispatch(frame, newLookupResult, new Object[]{targetObject, createMessage(selector, targetClass, arguments)}, contextOrMarker);
        } else {
            return dispatchNode.executeDispatch(frame, newLookupResult, new Object[]{targetObject, selector, image.newList(arguments), rcvrAndArgs[0]}, contextOrMarker);
        }
    }

    private NativeObject getRunWithIn() {
        if (runWithIn == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            runWithIn = (NativeObject) image.specialObjectsArray.at0(SPECIAL_OBJECT_INDEX.SelectorRunWithIn);
        }
        return runWithIn;
    }
}
