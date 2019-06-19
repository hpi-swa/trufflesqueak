package de.hpi.swa.graal.squeak.model;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

import de.hpi.swa.graal.squeak.interop.InteropArray;
import de.hpi.swa.graal.squeak.interop.LookupMethodByStringNode;
import de.hpi.swa.graal.squeak.interop.WrapToSqueakNode;
import de.hpi.swa.graal.squeak.nodes.DispatchUneagerlyNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAtPut0Node;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectSizeNode;
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
    protected final boolean hasArrayElements(@Shared("classNode") @Cached final SqueakObjectClassNode classNode) {
        return classNode.executeLookup(this).isVariable();
    }

    @ExportMessage
    protected final long getArraySize(@Shared("sizeNode") @Cached final SqueakObjectSizeNode sizeNode) {
        return sizeNode.execute(this);
    }

    @SuppressWarnings("static-method")
    @ExportMessage(name = "isArrayElementReadable")
    @ExportMessage(name = "isArrayElementModifiable")
    @ExportMessage(name = "isArrayElementInsertable")
    protected final boolean isArrayElementReadable(final long index, @Shared("sizeNode") @Cached final SqueakObjectSizeNode sizeNode) {
        return 0 <= index && index < sizeNode.execute(this);
    }

    @ExportMessage
    protected final Object readArrayElement(final long index, @Cached final SqueakObjectAt0Node at0Node) {
        return at0Node.execute(this, index);
    }

    @ExportMessage
    protected final void writeArrayElement(final long index, final Object value,
                    @Shared("wrapNode") @Cached final WrapToSqueakNode wrapNode,
                    @Cached final SqueakObjectAtPut0Node atput0Node) throws InvalidArrayIndexException {
        try {
            atput0Node.execute(this, index, wrapNode.executeWrap(value));
        } catch (final ArrayIndexOutOfBoundsException e) {
            throw InvalidArrayIndexException.create(index);
        }
    }

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
    @ExportMessage(name = "isMemberModifiable")
    @ExportMessage(name = "isMemberInvocable")
    public boolean isMemberReadable(final String key,
                    @Shared("lookupNode") @Cached final LookupMethodByStringNode lookupNode,
                    @Shared("classNode") @Cached final SqueakObjectClassNode classNode) {
        return lookupNode.executeLookup(classNode.executeLookup(this), toSelector(key)) != null;
    }

    @ExportMessage
    public Object readMember(final String key,
                    @Shared("lookupNode") @Cached final LookupMethodByStringNode lookupNode,
                    @Shared("classNode") @Cached final SqueakObjectClassNode classNode) {
        return lookupNode.executeLookup(classNode.executeLookup(this), toSelector(key));
    }

    @ExportMessage
    public boolean isMemberInsertable(@SuppressWarnings("unused") final String member) {
        return false;
    }

    @ExportMessage
    public void writeMember(final String member, final Object value,
                    @Shared("lookupNode") @Cached final LookupMethodByStringNode lookupNode,
                    @Shared("classNode") @Cached final SqueakObjectClassNode classNode,
                    @Shared("wrapNode") @Cached final WrapToSqueakNode wrapNode,
                    @Shared("dispatchNode") @Cached final DispatchUneagerlyNode dispatchNode) throws UnsupportedMessageException {
        final Object methodObject = lookupNode.executeLookup(classNode.executeLookup(this), toSelector(member));
        if (methodObject instanceof CompiledMethodObject) {
            final CompiledMethodObject method = (CompiledMethodObject) methodObject;
            if (method.getNumArgs() == 1) {
                dispatchNode.executeDispatch(method, new Object[]{this, wrapNode.executeWrap(value)}, NilObject.SINGLETON);
            } else {
                throw UnsupportedMessageException.create();
            }
        } else {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage
    public Object invokeMember(final String member, final Object[] arguments,
                    @Shared("lookupNode") @Cached final LookupMethodByStringNode lookupNode,
                    @Shared("classNode") @Cached final SqueakObjectClassNode classNode,
                    @Shared("wrapNode") @Cached final WrapToSqueakNode wrapNode,
                    @Shared("dispatchNode") @Cached final DispatchUneagerlyNode dispatchNode) throws UnsupportedMessageException, ArityException {
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

    /**
     * Converts an interop identifier to a Smalltalk selector. Most languages do not allow colons in
     * identifiers, so treat underscores as colons as well.
     *
     * @param identifier for interop
     * @return Smalltalk selector
     */
    private static String toSelector(final String identifier) {
        return identifier.replace('_', ':');
    }
}
