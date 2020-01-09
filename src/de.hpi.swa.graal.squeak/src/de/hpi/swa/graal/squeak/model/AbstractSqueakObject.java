/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.model;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

import de.hpi.swa.graal.squeak.interop.InteropArray;
import de.hpi.swa.graal.squeak.interop.LookupMethodByStringNode;
import de.hpi.swa.graal.squeak.interop.WrapToSqueakNode;
import de.hpi.swa.graal.squeak.nodes.DispatchUneagerlyNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.graal.squeak.util.ArrayUtils;

@ExportLibrary(InteropLibrary.class)
public abstract class AbstractSqueakObject implements TruffleObject {

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
        return new InteropArray(classNode.executeLookup(this).listMethods());
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
    public Object invokeMember(final String member, final Object[] arguments,
                    @Shared("lookupNode") @Cached final LookupMethodByStringNode lookupNode,
                    @Shared("classNode") @Cached final SqueakObjectClassNode classNode,
                    @Exclusive @Cached final WrapToSqueakNode wrapNode,
                    @Exclusive @Cached final DispatchUneagerlyNode dispatchNode) throws UnsupportedMessageException, ArityException {
        final Object methodObject = lookupNode.executeLookup(classNode.executeLookup(this), toSelector(member));
        if (methodObject instanceof CompiledMethodObject) {
            final CompiledMethodObject method = (CompiledMethodObject) methodObject;
            final int actualArity = arguments.length;
            final int expectedArity = method.getNumArgs();
            if (actualArity == expectedArity) {
                return dispatchNode.executeDispatch(method, ArrayUtils.copyWithFirst(wrapNode.executeObjects(arguments), this), NilObject.SINGLETON);
            } else {
                throw ArityException.create(1 + expectedArity, 1 + actualArity);  // +1 for receiver
            }
        } else {
            throw UnsupportedMessageException.create();
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
