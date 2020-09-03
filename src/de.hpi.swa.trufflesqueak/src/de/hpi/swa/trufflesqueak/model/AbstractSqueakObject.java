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
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.utilities.TriState;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.interop.BoundMethod;
import de.hpi.swa.trufflesqueak.interop.InteropArray;
import de.hpi.swa.trufflesqueak.interop.LookupMethodByStringNode;
import de.hpi.swa.trufflesqueak.interop.WrapToSqueakNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchUneagerlyNode;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;

@ExportLibrary(InteropLibrary.class)
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

    @ExportMessage(name = "isMemberInvocable")
    @ExportMessage(name = "isMemberReadable")
    protected final boolean isMemberInvocable(final String member,
                    @Shared("lookupNode") @Cached final LookupMethodByStringNode lookupNode,
                    @Shared("classNode") @Cached final SqueakObjectClassNode classNode,
                    @Exclusive @Cached final ConditionProfile alternativeProfile) {
        final String selectorString = toSelector(member);
        final ClassObject classObject = classNode.executeLookup(this);
        if (alternativeProfile.profile(lookupNode.executeLookup(classObject, selectorString) instanceof CompiledCodeObject)) {
            return true;
        } else {
            return lookupNode.executeLookup(classObject, toAlternativeSelector(selectorString)) instanceof CompiledCodeObject;
        }
    }

    @ExportMessage
    protected final Object readMember(final String member,
                    @Shared("lookupNode") @Cached final LookupMethodByStringNode lookupNode,
                    @Shared("classNode") @Cached final SqueakObjectClassNode classNode,
                    @Exclusive @Cached final ConditionProfile alternativeProfile) throws UnknownIdentifierException {
        final ClassObject classObject = classNode.executeLookup(this);
        final String selectorString = toSelector(member);
        final Object methodObject = lookupNode.executeLookup(classObject, selectorString);
        if (alternativeProfile.profile(methodObject instanceof CompiledCodeObject)) {
            return new BoundMethod((CompiledCodeObject) methodObject, this);
        } else {
            final Object methodObjectAlternative = lookupNode.executeLookup(classObject, toAlternativeSelector(selectorString));
            if (methodObjectAlternative instanceof CompiledCodeObject) {
                return new BoundMethod((CompiledCodeObject) methodObjectAlternative, this);
            } else {
                throw UnknownIdentifierException.create(member);
            }
        }
    }

    @ExportMessage
    protected static class InvokeMember {
        @Specialization(rewriteOn = RespecializeException.class)
        protected static final Object invokeMember(final AbstractSqueakObject receiver, final String member, final Object[] arguments,
                        @Shared("lookupNode") @Cached final LookupMethodByStringNode lookupNode,
                        @Shared("classNode") @Cached final SqueakObjectClassNode classNode,
                        @Exclusive @Cached final WrapToSqueakNode wrapNode,
                        @Exclusive @Cached final DispatchUneagerlyNode dispatchNode) throws ArityException {
            final int actualArity = arguments.length;
            final Object methodObject = lookupNode.executeLookup(classNode.executeLookup(receiver), toSelector(member, actualArity));
            if (methodObject == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                /* DoesNotUnderstand, rewrite this specialization. */
                throw new RespecializeException();
            }
            final CompiledCodeObject method = (CompiledCodeObject) methodObject;
            final int expectedArity = method.getNumArgs();
            if (actualArity == expectedArity) {
                return dispatchNode.executeDispatch(method, ArrayUtils.copyWithFirst(wrapNode.executeObjects(arguments), receiver), InteropSenderMarker.SINGLETON);
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
                        @Exclusive @Cached final ConditionProfile hasMethodProfile,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) throws UnsupportedMessageException, ArityException {
            final int actualArity = arguments.length;
            final String selector = toSelector(member, actualArity);
            final ClassObject classObject = classNode.executeLookup(receiver);
            final Object methodObject = lookupNode.executeLookup(classObject, selector);
            if (hasMethodProfile.profile(methodObject instanceof CompiledCodeObject)) {
                final CompiledCodeObject method = (CompiledCodeObject) methodObject;
                final int expectedArity = method.getNumArgs();
                if (actualArity == expectedArity) {
                    return dispatchNode.executeDispatch(method, ArrayUtils.copyWithFirst(wrapNode.executeObjects(arguments), receiver), InteropSenderMarker.SINGLETON);
                } else {
                    throw ArityException.create(1 + expectedArity, 1 + actualArity);
                }
            } else {
                final CompiledCodeObject doesNotUnderstandMethodObject = (CompiledCodeObject) lookupNode.executeLookup(classObject, "doesNotUnderstand:");
                final NativeObject symbol = (NativeObject) image.asByteString(selector).send("asSymbol");
                final PointersObject message = image.newMessage(writeNode, symbol, classObject, arguments);
                try {
                    return dispatchNode.executeDispatch(doesNotUnderstandMethodObject, new Object[]{receiver, message}, InteropSenderMarker.SINGLETON);
                } catch (final ProcessSwitch ps) {
                    CompilerDirectives.transferToInterpreter();
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
    protected final boolean hasMetaObject() {
        return true;
    }

    @ExportMessage
    protected final Object getMetaObject(@Shared("classNode") @Cached final SqueakObjectClassNode classNode) {
        return classNode.executeLookup(this);
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    protected final boolean hasLanguage() {
        return true;
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    protected final Class<? extends TruffleLanguage<?>> getLanguage() {
        return SqueakLanguage.class;
    }

    @ExportMessage
    protected static final class IsIdenticalOrUndefined {
        @Specialization
        protected static TriState doAbstractSqueakObject(final AbstractSqueakObject receiver, final AbstractSqueakObject other) {
            return TriState.valueOf(receiver == other);
        }

        @Fallback
        @SuppressWarnings("unused")
        protected static TriState doOther(final AbstractSqueakObject receiver, final Object other) {
            return TriState.UNDEFINED;
        }
    }

    @ExportMessage
    protected final int identityHashCode() {
        return (int) getSqueakHash();
    }

    @ExportMessage
    @TruffleBoundary
    protected final Object toDisplayString(@SuppressWarnings("unused") final boolean allowSideEffects) {
        return toString();
    }

    /**
     * Converts an interop identifier to a Smalltalk selector. Most languages do not allow colons in
     * identifiers, so treat underscores as colons as well.
     *
     * @param identifier for interop
     * @return String for Smalltalk selector
     */
    @TruffleBoundary
    private static String toSelector(final String identifier) {
        return identifier.replace('_', ':');
    }

    /**
     * The same as {@link #toSelector(String)}, but may return alternative selector depending on
     * actualArity.
     *
     * @param identifier for interop
     * @param actualArity of selector
     * @return String for Smalltalk selector
     */
    @TruffleBoundary
    private static String toSelector(final String identifier, final int actualArity) throws ArityException {
        final String selector = identifier.replace('_', ':');
        final int expectedArity = (int) selector.chars().filter(ch -> ch == ':').count();
        if (expectedArity == actualArity) {
            return selector;
        } else {
            if (expectedArity + 1 == actualArity && !selector.endsWith(":")) {
                return selector + ":";
            } else {
                throw ArityException.create(1 + expectedArity, 1 + actualArity);
            }
        }
    }

    @TruffleBoundary
    private static String toAlternativeSelector(final String selector) {
        return selector + ":";
    }
}
