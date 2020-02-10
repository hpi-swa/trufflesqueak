/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.model;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
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

import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.exceptions.ProcessSwitch;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.interop.InteropArray;
import de.hpi.swa.graal.squeak.interop.LookupMethodByStringNode;
import de.hpi.swa.graal.squeak.interop.WrapToSqueakNode;
import de.hpi.swa.graal.squeak.nodes.DispatchUneagerlyNode;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.graal.squeak.util.ArrayUtils;

@ExportLibrary(InteropLibrary.class)
public abstract class AbstractSqueakObject implements TruffleObject {

    public abstract int getNumSlots();

    public abstract int instsize();

    public abstract int size();

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
        if (methodObject == null) {
            throw UnknownIdentifierException.create(member);
        } else {
            return methodObject;
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

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "a " + getClass().getSimpleName() + " @" + Integer.toHexString(hashCode());
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
