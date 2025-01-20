/*
 * Copyright (c) 2021-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.interop;

import static com.oracle.truffle.api.CompilerDirectives.shouldNotReachHere;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector0Node.DispatchDirect0Node;
import de.hpi.swa.trufflesqueak.nodes.dispatch.LookupClassGuard;

@SuppressWarnings("static-method")
@ExportLibrary(value = InteropLibrary.class, delegateTo = "delegate")
public final class SqueakLanguageView implements TruffleObject {
    protected final Object delegate;

    public SqueakLanguageView(final Object delegate) {
        this.delegate = delegate;
    }

    @ExportMessage
    protected boolean hasLanguage() {
        return true;
    }

    @ExportMessage
    protected Class<? extends TruffleLanguage<?>> getLanguage() {
        return SqueakLanguage.class;
    }

    @ExportMessage
    protected boolean hasMetaObject() {
        return true;
    }

    @ExportMessage
    protected Object getMetaObject(@Bind final Node node, @Cached final SqueakObjectClassNode classNode) {
        return classNode.executeLookup(node, delegate);
    }

    @ExportMessage
    protected abstract static class ToDisplayString {
        @SuppressWarnings("unused")
        @Specialization(guards = "guard.check(view.delegate)", assumptions = "dispatchNode.getAssumptions()", limit = "1")
        protected static final Object toDisplayString(final SqueakLanguageView view, final boolean allowSideEffects,
                        @Cached(value = "create(view.delegate)", allowUncached = true) final LookupClassGuard guard,
                        @Bind final Node node,
                        @Cached(value = "create(guard)", allowUncached = true) final DispatchDirect0Node dispatchNode) {
            return dispatchNode.execute(SqueakImageContext.get(node).externalSenderFrame, view.delegate);
        }

        protected static final DispatchDirect0Node create(final LookupClassGuard guard) {
            final Object lookupResult = LookupMethodByStringNode.executeUncached(guard.getSqueakClass(null), "asString");
            if (lookupResult instanceof CompiledCodeObject method) {
                return DispatchDirect0Node.create(method, guard);
            } else {
                throw SqueakException.create("Failed to lookup asString");
            }
        }
    }

    public static Object create(final Object value) {
        assert isPrimitiveOrFromOtherLanguage(value);
        return new SqueakLanguageView(value);
    }

    /*
     * Language views are intended to be used only for primitives and other language values.
     */
    private static boolean isPrimitiveOrFromOtherLanguage(final Object value) {
        final InteropLibrary interop = InteropLibrary.getFactory().getUncached(value);
        try {
            return !interop.hasLanguage(value) || interop.getLanguage(value) != SqueakLanguage.class;
        } catch (final UnsupportedMessageException e) {
            throw shouldNotReachHere(e);
        }
    }
}
