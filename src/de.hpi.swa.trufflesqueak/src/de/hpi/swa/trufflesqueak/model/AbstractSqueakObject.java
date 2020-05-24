/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.ControlFlowException;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.interop.BoundMethod;
import de.hpi.swa.trufflesqueak.interop.InteropArray;
import de.hpi.swa.trufflesqueak.interop.LookupMethodByStringNode;
import de.hpi.swa.trufflesqueak.interop.WrapToSqueakNode;
import de.hpi.swa.trufflesqueak.nodes.DispatchUneagerlyNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;

@ExportLibrary(InteropLibrary.class)
public abstract class AbstractSqueakObject implements TruffleObject {

    public abstract int getNumSlots();

    public abstract int instsize();

    public abstract int size();

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "a " + getClass().getSimpleName() + " @" + Integer.toHexString(hashCode());
    }

    /*
     * INTEROPERABILITY
     */

    @SuppressWarnings("static-method")
    @ExportMessage
    protected final boolean hasMembers() {
        return true;
    }

    @ExportMessage
    protected final Object getMembers(@SuppressWarnings("unused") final boolean includeInternal,
                    @Shared("classNode") @Cached final SqueakObjectClassNode classNode) {
        return new InteropArray(classNode.executeLookup(this).listInteropMembers());
    }

    @ExportMessage(name = "isMemberReadable")
    public boolean isMemberReadable(final String member,
                    @Shared("lookupNode") @Cached final LookupMethodByStringNode lookupNode,
                    @Shared("classNode") @Cached final SqueakObjectClassNode classNode) {
        return lookupNode.executeLookup(classNode.executeLookup(this), toSelector(member)) != null;
    }

    @ExportMessage(name = "isMemberInvocable")
    public boolean isMemberInvocable(final String member,
                    @Shared("lookupNode") @Cached final LookupMethodByStringNode lookupNode,
                    @Shared("classNode") @Cached final SqueakObjectClassNode classNode) {
        return lookupNode.executeLookup(classNode.executeLookup(this), toSelector(member)) instanceof CompiledMethodObject;
    }

    @ExportMessage
    public Object readMember(final String member,
                    @Shared("lookupNode") @Cached final LookupMethodByStringNode lookupNode,
                    @Shared("classNode") @Cached final SqueakObjectClassNode classNode) throws UnknownIdentifierException {
        final Object methodObject = lookupNode.executeLookup(classNode.executeLookup(this), toSelector(member));
        if (methodObject instanceof CompiledMethodObject) {
            return new BoundMethod((CompiledMethodObject) methodObject, this);
        } else {
            throw UnknownIdentifierException.create(member);
        }
    }

    @ExportMessage
    public static class InvokeMember {
        @Specialization(rewriteOn = RespecializeException.class)
        protected static final Object invokeMember(final AbstractSqueakObject receiver, final String member, final Object[] arguments,
                        @Shared("lookupNode") @Cached final LookupMethodByStringNode lookupNode,
                        @Shared("classNode") @Cached final SqueakObjectClassNode classNode,
                        @Exclusive @Cached final WrapToSqueakNode wrapNode,
                        @Exclusive @Cached final DispatchUneagerlyNode dispatchNode) throws ArityException {
            final Object methodObject = lookupNode.executeLookup(classNode.executeLookup(receiver), toSelector(member));
            if (methodObject == null) {
                CompilerDirectives.transferToInterpreter();
                /* DoesNotUnderstand, rewrite this specialization. */
                throw new RespecializeException();
            }
            final CompiledMethodObject method = (CompiledMethodObject) methodObject;
            final int actualArity = arguments.length;
            final int expectedArity = method.getNumArgs();
            if (actualArity == expectedArity) {
                return dispatchNode.executeDispatch(method, ArrayUtils.copyWithFirst(wrapNode.executeObjects(arguments), receiver), NilObject.SINGLETON);
            } else {
                throw ArityException.create(1 + expectedArity, 1 + actualArity);
            }
        }

        protected static final class RespecializeException extends ControlFlowException {
            private static final long serialVersionUID = 1L;
        }

        @Specialization(replaces = "invokeMember")
        protected static final Object invokeMemberWithDNU(final AbstractSqueakObject receiver, final String member, final Object[] arguments,
                        @Shared("lookupNode") @Cached final LookupMethodByStringNode lookupNode,
                        @Shared("classNode") @Cached final SqueakObjectClassNode classNode,
                        @Exclusive @Cached final WrapToSqueakNode wrapNode,
                        @Exclusive @Cached final DispatchUneagerlyNode dispatchNode,
                        @Exclusive @Cached final AbstractPointersObjectWriteNode writeNode,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) throws UnsupportedMessageException, ArityException {
            final Object methodObject = lookupNode.executeLookup(classNode.executeLookup(receiver), toSelector(member));
            if (methodObject instanceof CompiledMethodObject) {
                final CompiledMethodObject method = (CompiledMethodObject) methodObject;
                final int actualArity = arguments.length;
                final int expectedArity = method.getNumArgs();
                if (actualArity == expectedArity) {
                    return dispatchNode.executeDispatch(method, ArrayUtils.copyWithFirst(wrapNode.executeObjects(arguments), receiver), NilObject.SINGLETON);
                } else {
                    throw ArityException.create(1 + expectedArity, 1 + actualArity);
                }
            } else {
                final CompiledMethodObject doesNotUnderstandMethodObject = (CompiledMethodObject) lookupNode.executeLookup(classNode.executeLookup(receiver), "doesNotUnderstand:");
                final NativeObject symbol = (NativeObject) image.asByteString(toSelector(member)).send("asSymbol");
                final PointersObject message = image.newMessage(writeNode, symbol, classNode.executeLookup(receiver), arguments);
                try {
                    return dispatchNode.executeDispatch(doesNotUnderstandMethodObject, new Object[]{receiver, message}, NilObject.SINGLETON);
                } catch (final ProcessSwitch ps) {
                    /*
                     * A ProcessSwitch exception is thrown in case Squeak/Smalltalk wants to open
                     * the debugger. This needs to be avoided in headless mode.
                     */
                    if (image.isHeadless()) {
                        throw UnsupportedMessageException.create();
                    } else {
                        throw ps;
                    }
                }
            }
        }
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    public final boolean hasMetaObject() {
        return true;
    }

    @ExportMessage
    public final Object getMetaObject(@Shared("classNode") @Cached final SqueakObjectClassNode classNode) {
        return classNode.executeLookup(this);
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    public final boolean hasLanguage() {
        return true;
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    public final Class<? extends TruffleLanguage<?>> getLanguage() {
        return SqueakLanguage.class;
    }

    @ExportMessage
    @TruffleBoundary
    public final Object toDisplayString(@SuppressWarnings("unused") final boolean allowSideEffects) {
        return toString();
    }

    /**
     * Converts an interop identifier to a Smalltalk selector. Most languages do not allow colons in
     * identifiers, so treat underscores as colons as well.
     *
     * @param identifier for interop
     * @return Smalltalk selector
     */
    @TruffleBoundary
    private static String toSelector(final String identifier) {
        return identifier.replace('_', ':');
    }
}
