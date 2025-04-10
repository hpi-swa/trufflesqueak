/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import java.lang.reflect.Method;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.library.Library;
import com.oracle.truffle.api.library.LibraryFactory;
import com.oracle.truffle.api.library.Message;
import com.oracle.truffle.api.library.ReflectionLibrary;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.interop.WrapToSqueakNode;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.LookupMethodNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNode.DispatchDirectNaryNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNode.DispatchIndirectNaryNode.TryPrimitiveNaryNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.LookupClassGuard;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

@SuppressWarnings("static-method")
@ExportLibrary(ReflectionLibrary.class)
public abstract class AbstractSqueakObject implements TruffleObject {
    private static final Object DEFAULT = new Object();

    public abstract long getOrCreateSqueakHash();

    public abstract int getNumSlots();

    public abstract int instsize();

    public abstract int size();

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "a " + getClass().getSimpleName() + " @" + Integer.toHexString(hashCode());
    }

    @ExportMessage
    protected static final Object send(final AbstractSqueakObject receiver, final Message message, final Object[] arguments,
                    @SuppressWarnings("unused") @Bind final Node node,
                    @Cached final PerformInteropSendNode performInteropSendNode) throws Exception {
        final SqueakImageContext image = SqueakImageContext.get(node);
        final boolean wasActive = image.interrupt.isActive();
        image.interrupt.deactivate();
        try {
            return performInteropSendNode.execute(node, receiver, message, arguments);
        } catch (final ProcessSwitch ps) {
            CompilerDirectives.transferToInterpreter();
            if (image.options.isHeadless()) {
                image.printToStdErr(ps);
                throw new IllegalArgumentException();
            } else {
                throw ps; // open debugger in interactive mode
            }
        } finally {
            if (wasActive) {
                image.interrupt.activate();
            }
        }
    }

    @GenerateInline
    @GenerateUncached
    @GenerateCached(false)
    protected abstract static class PerformInteropSendNode extends AbstractNode {
        protected abstract Object execute(Node node, AbstractSqueakObject receiver, Message message, Object[] arguments) throws Exception;

        @ExplodeLoop
        @Specialization(guards = {"message == cachedMessage", "guard.check(receiver)"}, limit = "8", assumptions = "dispatchNode.getAssumptions()")
        protected static final Object doSendCached(final Node node, final AbstractSqueakObject receiver, @SuppressWarnings("unused") final Message message, final Object[] rawArguments,
                        @Cached("message") final Message cachedMessage,
                        @SuppressWarnings("unused") @Cached("lookupMethod(receiver, cachedMessage)") final CompiledCodeObject cachedMethod,
                        @SuppressWarnings("unused") @Cached("create(receiver)") final LookupClassGuard guard,
                        @Shared("wrapNode") @Cached final WrapToSqueakNode wrapNode,
                        @Cached("create(cachedMethod, guard)") final DispatchDirectNaryNode dispatchNode) {
            final int numArgs = cachedMessage.getParameterCount() - 1;
            assert numArgs == rawArguments.length;
            final Object[] arguments = new Object[numArgs];
            for (int i = 0; i < numArgs; i++) {
                arguments[i] = wrapNode.executeWrap(node, rawArguments[i]);
            }
            return dispatchNode.execute(SqueakImageContext.get(node).externalSenderFrame, receiver, arguments);
        }

        protected static final CompiledCodeObject lookupMethod(final AbstractSqueakObject receiver, final Message message) {
            final ClassObject receiverClass = SqueakObjectClassNode.executeUncached(receiver);
            return receiverClass.lookupMethodInMethodDictSlow(SqueakImageContext.get(null).toInteropSelector(message));
        }

        // TODO: Add Specialization for non-InteropLibrary messages to avoid slowing down other
        // Truffle libraries?

        @TruffleBoundary
        @ReportPolymorphism.Megamorphic
        @Specialization(replaces = "doSendCached")
        protected static final Object doSendGeneric(final Node node, final AbstractSqueakObject receiver, final Message message, final Object[] rawArguments,
                        @Cached final LookupMethodNode lookupNode,
                        @Cached final SqueakObjectClassNode classNode,
                        @Shared("wrapNode") @Cached final WrapToSqueakNode wrapNode,
                        @Cached(inline = false) final TryPrimitiveNaryNode tryPrimitiveNode,
                        @Cached(inline = false) final IndirectCallNode callNode) throws Exception {
            final SqueakImageContext image = SqueakImageContext.get(node);
            if (message.getLibraryClass() == InteropLibrary.class) {
                final NativeObject selector = image.toInteropSelector(message);
                final ClassObject receiverClass = classNode.executeLookup(node, receiver);
                final Object methodObject = lookupNode.executeLookup(node, receiverClass, selector);
                if (methodObject instanceof final CompiledCodeObject method) {
                    assert 1 + method.getNumArgs() == message.getParameterCount();
                    final Object[] arguments = new Object[method.getNumArgs()];
                    for (int i = 0; i < arguments.length; i++) {
                        arguments[i] = wrapNode.executeWrap(node, rawArguments[i]);
                    }
                    final Object result = tryPrimitiveNode.execute(image.externalSenderFrame, method, receiver, arguments);
                    if (result != null) {
                        return result;
                    } else {
                        return callNode.call(method.getCallTarget(), FrameAccess.newWith(NilObject.SINGLETON, null, receiver, arguments));
                    }
                } else {
                    image.printToStdErr(selector, "method:", methodObject);
                }
            }
            CompilerDirectives.transferToInterpreter();
            // Fall back to other, concrete or the default library implementation
            // FIXME: avoid reflection here somehow, the following does not work:
            // return ReflectionLibrary.getFactory().getUncached(receiver).send(DEFAULT,
            // message, arguments);
            final LibraryFactory<?> lib = message.getFactory();
            final Method genericDispatchMethod = lib.getClass().getDeclaredMethod("genericDispatch", Library.class, Object.class, Message.class, Object[].class, int.class);
            genericDispatchMethod.setAccessible(true);
            return genericDispatchMethod.invoke(lib, lib.getUncached(DEFAULT), receiver, message, rawArguments, 0);
        }
    }
}
