/*
 * Copyright (c) 2017-2021 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.library.Message;
import com.oracle.truffle.api.library.ReflectionLibrary;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.interop.WrapToSqueakNode;
import de.hpi.swa.trufflesqueak.nodes.LookupMethodNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchUneagerlyNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

@SuppressWarnings("static-method")
@ExportLibrary(ReflectionLibrary.class)
public abstract class AbstractSqueakObject implements TruffleObject {

    public abstract long getSqueakHash();

    public abstract int getNumSlots();

    public abstract int instsize();

    public abstract int size();

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "a " + getClass().getSimpleName() + " @" + Integer.toHexString(hashCode());
    }

    @ExportMessage
    protected static class Send {
        @SuppressWarnings("unused")
        @ExplodeLoop
        @Specialization(guards = {"message == cachedMessage", "classNode.executeLookup(receiver) == cachedClass", "cachedMethod != null"}, limit = "8", //
                        assumptions = {"cachedClass.getClassHierarchyStable()", "cachedClass.getMethodDictStable()", "cachedMethod.getCallTargetStable()"})
        protected static final Object doSendCached(final AbstractSqueakObject receiver, final Message message, final Object[] arguments,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image,
                        @Cached final SqueakObjectClassNode classNode,
                        @Cached("message") final Message cachedMessage,
                        @Cached("classNode.executeLookup(receiver)") final ClassObject cachedClass,
                        @Cached("cachedClass.lookupMethodInMethodDictSlow(image.toInteropSelector(message))") final CompiledCodeObject cachedMethod,
                        @Cached("create(cachedMethod.getCallTarget())") final DirectCallNode callNode,
                        @Cached final WrapToSqueakNode wrapNode) {
            final int numArgs = cachedMessage.getParameterCount() - 1;
            assert numArgs == arguments.length;
            final Object[] frameArguments = FrameAccess.newWith(cachedMethod, NilObject.SINGLETON, null, cachedMessage.getParameterCount());
            frameArguments[FrameAccess.getReceiverStartIndex()] = receiver;
            for (int i = 0; i < cachedMessage.getParameterCount() - 1; i++) {
                frameArguments[FrameAccess.getArgumentStartIndex() + i] = wrapNode.executeWrap(arguments[i]);
            }
            return callNode.call(frameArguments);
        }

        @TruffleBoundary
        @Specialization(replaces = "doSendCached")
        protected static final Object doSendGeneric(final AbstractSqueakObject receiver, final Message message, final Object[] arguments,
                        @Cached final LookupMethodNode lookupNode,
                        @Cached final SqueakObjectClassNode classNode,
                        @Cached final DispatchUneagerlyNode dispatchNode,
                        @Cached final WrapToSqueakNode wrapNode,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) throws Exception {
            assert message.getLibraryClass() == InteropLibrary.class;
            final NativeObject selector = image.toInteropSelector(message);
            final Object method = lookupNode.executeLookup(classNode.executeLookup(receiver), selector);
            if (method instanceof CompiledCodeObject) {
                final Object[] receiverAndArguments = new Object[message.getParameterCount()];
                receiverAndArguments[0] = receiver;
                for (int i = 0; i < arguments.length; i++) {
                    receiverAndArguments[1 + i] = wrapNode.executeWrap(arguments[i]);
                }
                try {
                    return dispatchNode.executeDispatch((CompiledCodeObject) method, receiverAndArguments, NilObject.SINGLETON);
                } catch (final ProcessSwitch ps) {
                    CompilerDirectives.transferToInterpreter();
                    image.printToStdErr(ps);
                    throw new IllegalArgumentException();
                }
            } else {
                CompilerDirectives.transferToInterpreter();
                image.printToStdErr(selector, "method:", method);
                throw new IllegalArgumentException();
            }
        }
    }
}
