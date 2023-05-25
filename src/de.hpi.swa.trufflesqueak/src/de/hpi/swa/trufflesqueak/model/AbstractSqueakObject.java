/*
 * Copyright (c) 2017-2023 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2023 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import java.math.BigInteger;
import java.nio.ByteOrder;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.ExceptionType;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.InvalidBufferOffsetException;
import com.oracle.truffle.api.interop.StopIterationException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnknownKeyException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.TriState;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchInteropMessageNode;

@SuppressWarnings("static-method")
@ExportLibrary(InteropLibrary.class)
@ImportStatic(DispatchInteropMessageNode.class)
public abstract class AbstractSqueakObject extends DynamicObject implements TruffleObject {

    protected AbstractSqueakObject(final Shape shape) {
        super(shape);
    }

    protected AbstractSqueakObject() {
        super(SqueakLanguage.EMPTY_SHAPE);
    }

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
    protected static final boolean isNull(final AbstractSqueakObject receiver,
                    @Cached(value = "create(IS_NULL)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver);
    }

    // Boolean Messages

    @ExportMessage
    protected static final boolean isBoolean(final AbstractSqueakObject receiver,
                    @Cached(value = "create(IS_BOOLEAN)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver);
    }

    @ExportMessage
    protected static final boolean asBoolean(final AbstractSqueakObject receiver,
                    @Cached(value = "create(AS_BOOLEAN)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException {
        return dispatchNode.executeBoolean(receiver);
    }

    @ExportMessage
    protected static final boolean isExecutable(final AbstractSqueakObject receiver,
                    @Cached(value = "create(IS_EXECUTABLE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver);
    }

    @ExportMessage
    protected static final Object execute(final AbstractSqueakObject receiver, final Object[] arguments,
                    @Cached(value = "create(EXECUTE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.execute(receiver, (Object) arguments);
    }

    @ExportMessage
    protected static final boolean hasExecutableName(final AbstractSqueakObject receiver,
                    @Cached(value = "create(HAS_EXECUTABLE_NAME)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver);
    }

    @ExportMessage
    protected static final Object getExecutableName(final AbstractSqueakObject receiver,
                    @Cached(value = "create(GET_EXECUTABLE_NAME)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException {
        return dispatchNode.execute(receiver);
    }

    @ExportMessage
    protected static final boolean hasDeclaringMetaObject(final AbstractSqueakObject receiver,
                    @Cached(value = "create(HAS_DECLARING_META_OBJECT)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver);
    }

    @ExportMessage
    protected static final Object getDeclaringMetaObject(final AbstractSqueakObject receiver,
                    @Cached(value = "create(GET_DECLARING_META_OBJECT)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException {
        return dispatchNode.execute(receiver);
    }

    // Instantiable Messages

    @ExportMessage
    protected static final boolean isInstantiable(final AbstractSqueakObject receiver,
                    @Cached(value = "create(IS_INSTANTIABLE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver);
    }

    @ExportMessage
    protected static final Object instantiate(final AbstractSqueakObject receiver, final Object[] arguments,
                    @Cached(value = "create(INSTANTIATE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode)
                    throws UnsupportedTypeException, ArityException, UnsupportedMessageException {
        return dispatchNode.execute(receiver, (Object) arguments);
    }

    // String Messages

    @ExportMessage
    protected static final boolean isString(final AbstractSqueakObject receiver,
                    @Cached(value = "create(IS_STRING)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver);
    }

    @ExportMessage
    protected static final String asString(final AbstractSqueakObject receiver,
                    @Cached(value = "create(AS_STRING)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException {
        return (String) dispatchNode.execute(receiver);
    }

    // Number Messages

    @ExportMessage
    protected static final boolean isNumber(final AbstractSqueakObject receiver,
                    @Cached(value = "create(IS_NUMBER)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver);
    }

    @ExportMessage
    protected static final boolean fitsInByte(final AbstractSqueakObject receiver,
                    @Cached(value = "create(FITS_IN_BYTE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver);
    }

    @ExportMessage
    protected static final boolean fitsInShort(final AbstractSqueakObject receiver,
                    @Cached(value = "create(FITS_IN_SHORT)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver);
    }

    @ExportMessage
    protected static final boolean fitsInInt(final AbstractSqueakObject receiver,
                    @Cached(value = "create(FITS_IN_INT)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver);
    }

    @ExportMessage
    protected static final boolean fitsInLong(final AbstractSqueakObject receiver,
                    @Cached(value = "create(FITS_IN_LONG)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver);
    }

    @ExportMessage
    protected static final boolean fitsInBigInteger(final AbstractSqueakObject receiver,
                    @Cached(value = "create(FITS_IN_BIG_INTEGER)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver);
    }

    @ExportMessage
    protected static final boolean fitsInFloat(final AbstractSqueakObject receiver,
                    @Cached(value = "create(FITS_IN_FLOAT)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver);
    }

    @ExportMessage
    protected static final boolean fitsInDouble(final AbstractSqueakObject receiver,
                    @Cached(value = "create(FITS_IN_DOUBLE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver);
    }

    @ExportMessage
    protected static final byte asByte(final AbstractSqueakObject receiver,
                    @Cached(value = "create(AS_BYTE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException {
        return (byte) dispatchNode.execute(receiver);
    }

    @ExportMessage
    protected static final short asShort(final AbstractSqueakObject receiver,
                    @Cached(value = "create(AS_SHORT)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException {
        return (short) dispatchNode.execute(receiver);
    }

    @ExportMessage
    protected static final int asInt(final AbstractSqueakObject receiver,
                    @Cached(value = "create(AS_INT)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException {
        return (int) dispatchNode.execute(receiver);
    }

    @ExportMessage
    protected static final long asLong(final AbstractSqueakObject receiver,
                    @Cached(value = "create(AS_LONG)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException {
        return (long) dispatchNode.execute(receiver);
    }

//
    @ExportMessage
    protected static final BigInteger asBigInteger(final AbstractSqueakObject receiver,
                    @Cached(value = "create(AS_BIG_INTEGER)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException {
        return (BigInteger) dispatchNode.execute(receiver);
    }

    @ExportMessage
    protected static final float asFloat(final AbstractSqueakObject receiver,
                    @Cached(value = "create(AS_FLOAT)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException {
        return (float) dispatchNode.execute(receiver);
    }

    @ExportMessage
    protected static final double asDouble(final AbstractSqueakObject receiver,
                    @Cached(value = "create(AS_DOUBLE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException {
        return (double) dispatchNode.execute(receiver);
    }

    // Member Messages

    @ExportMessage
    protected static final boolean hasMembers(final AbstractSqueakObject receiver,
                    @Cached(value = "create(HAS_MEMBERS)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver);
    }

    @ExportMessage
    protected static final Object getMembers(final AbstractSqueakObject receiver, final boolean includeInternal,
                    @Cached(value = "create(GET_MEMBERS)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException {
        return dispatchNode.execute(receiver, includeInternal);
    }

    @ExportMessage
    protected static final boolean isMemberReadable(final AbstractSqueakObject receiver, final String member,
                    @Cached(value = "create(IS_MEMBER_READABLE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver, member);
    }

    @ExportMessage
    protected static final Object readMember(final AbstractSqueakObject receiver, final String member,
                    @Cached(value = "create(READ_MEMBER)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException, UnknownIdentifierException {
        return dispatchNode.execute(receiver, member);
    }

    @ExportMessage
    protected static final boolean isMemberModifiable(final AbstractSqueakObject receiver, final String member,
                    @Cached(value = "create(IS_MEMBER_MODIFIABLE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver, member);
    }

    @ExportMessage
    protected static final boolean isMemberInsertable(final AbstractSqueakObject receiver, final String member,
                    @Cached(value = "create(IS_MEMBER_INSERTABLE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver, member);
    }

    @ExportMessage
    protected static final void writeMember(final AbstractSqueakObject receiver, final String member, final Object value,
                    @Cached(value = "create(WRITE_MEMBER)", allowUncached = true) final DispatchInteropMessageNode dispatchNode)
                    throws UnsupportedMessageException, UnknownIdentifierException, UnsupportedTypeException {
        dispatchNode.execute(receiver, member, value);
    }

    @ExportMessage
    protected static final boolean isMemberRemovable(final AbstractSqueakObject receiver, final String member,
                    @Cached(value = "create(IS_MEMBER_REMOVABLE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver, member);
    }

    @ExportMessage
    protected static final void removeMember(final AbstractSqueakObject receiver, final String member,
                    @Cached(value = "create(REMOVE_MEMBER)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException, UnknownIdentifierException {
        dispatchNode.execute(receiver, member);
    }

    @ExportMessage
    protected static final boolean isMemberInvocable(final AbstractSqueakObject receiver, final String member,
                    @Cached(value = "create(IS_MEMBER_INVOCABLE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver, member);
    }

    @ExportMessage
    protected static final Object invokeMember(final AbstractSqueakObject receiver, final String member, final Object[] arguments,
                    @Cached(value = "create(INVOKE_MEMBER)", allowUncached = true) final DispatchInteropMessageNode dispatchNode)
                    throws UnsupportedMessageException, ArityException, UnknownIdentifierException, UnsupportedTypeException {
        return dispatchNode.execute(receiver, member, arguments);
    }

    @ExportMessage
    protected static final boolean isMemberInternal(final AbstractSqueakObject receiver, final String member,
                    @Cached(value = "create(IS_MEMBER_INTERNAL)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver, member);
    }

    // protected static final final boolean isMemberWritable(final AbstractSqueakObject receiver,
    // final
    // String
    // member,
    // protected static final final boolean isMemberExisting(final AbstractSqueakObject receiver,
    // final
    // String
    // member,

    @ExportMessage
    protected static final boolean hasMemberReadSideEffects(final AbstractSqueakObject receiver, final String member,
                    @Cached(value = "create(HAS_MEMBER_READ_SIDE_EFFECTS)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver, member);
    }

    @ExportMessage
    protected static final boolean hasMemberWriteSideEffects(final AbstractSqueakObject receiver, final String member,
                    @Cached(value = "create(HAS_MEMBER_WRITE_SIDE_EFFECTS)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver, member);
    }

    // Hashes

    @ExportMessage
    protected static final boolean hasHashEntries(final AbstractSqueakObject receiver,
                    @Cached(value = "create(HAS_HASH_ENTRIES)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver);
    }

    @ExportMessage
    protected static final long getHashSize(final AbstractSqueakObject receiver,
                    @Cached(value = "create(GET_HASH_SIZE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException {
        return (long) dispatchNode.execute(receiver);
    }

    @ExportMessage
    protected static final boolean isHashEntryReadable(final AbstractSqueakObject receiver, final Object key,
                    @Cached(value = "create(IS_HASH_ENTRY_READABLE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver, key);
    }

    @ExportMessage
    protected static final Object readHashValue(final AbstractSqueakObject receiver, final Object key,
                    @Cached(value = "create(READ_HASH_VALUE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException, UnknownKeyException {
        return dispatchNode.execute(receiver, key);
    }

    @ExportMessage
    protected static final Object readHashValueOrDefault(final AbstractSqueakObject receiver, final Object key, final Object defaultValue,
                    @Cached(value = "create(READ_HASH_VALUE_OR_DEFAULT)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException {
        return dispatchNode.execute(receiver, key, defaultValue);
    }

    @ExportMessage
    protected static final boolean isHashEntryModifiable(final AbstractSqueakObject receiver, final Object key,
                    @Cached(value = "create(IS_HASH_ENTRY_MODIFIABLE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver, key);
    }

    @ExportMessage
    protected static final boolean isHashEntryInsertable(final AbstractSqueakObject receiver, final Object key,
                    @Cached(value = "create(IS_HASH_ENTRY_INSERTABLE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver, key);
    }

    @ExportMessage
    protected static final boolean isHashEntryWritable(final AbstractSqueakObject receiver, final Object key,
                    @Cached(value = "create(IS_HASH_ENTRY_WRITABLE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver, key);
    }

    @ExportMessage
    protected static final void writeHashEntry(final AbstractSqueakObject receiver, final Object key, final Object value,
                    @Cached(value = "create(WRITE_HASH_ENTRY)", allowUncached = true) final DispatchInteropMessageNode dispatchNode)
                    throws UnsupportedMessageException, UnknownKeyException, UnsupportedTypeException {
        dispatchNode.execute(receiver, key, value);
    }

    @ExportMessage
    protected static final boolean isHashEntryRemovable(final AbstractSqueakObject receiver, final Object key,
                    @Cached(value = "create(IS_HASH_ENTRY_REMOVABLE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver, key);
    }

    @ExportMessage
    protected static final void removeHashEntry(final AbstractSqueakObject receiver, final Object key,
                    @Cached(value = "create(REMOVE_HASH_ENTRY)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException, UnknownKeyException {
        dispatchNode.execute(receiver, key);
    }

    @ExportMessage
    protected static final boolean isHashEntryExisting(final AbstractSqueakObject receiver, final Object key,
                    @Cached(value = "create(IS_HASH_ENTRY_EXISTING)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver, key);
    }

    @ExportMessage
    protected static final Object getHashEntriesIterator(final AbstractSqueakObject receiver,
                    @Cached(value = "create(GET_HASH_ENTRIES_ITERATOR)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException {
        return dispatchNode.execute(receiver);
    }

    @ExportMessage
    protected static final Object getHashKeysIterator(final AbstractSqueakObject receiver,
                    @Cached(value = "create(GET_HASH_KEYS_ITERATOR)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException {
        return dispatchNode.execute(receiver);
    }

    @ExportMessage
    protected static final Object getHashValuesIterator(final AbstractSqueakObject receiver,
                    @Cached(value = "create(GET_HASH_VALUES_ITERATOR)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException {
        return dispatchNode.execute(receiver);
    }

    // Array Messages

    @ExportMessage
    protected static final boolean hasArrayElements(final AbstractSqueakObject receiver,
                    @Cached(value = "create(HAS_ARRAY_ELEMENTS)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver);
    }

    @ExportMessage
    protected static final Object readArrayElement(final AbstractSqueakObject receiver, final long index,
                    @Cached(value = "create(READ_ARRAY_ELEMENT)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException, InvalidArrayIndexException {
        return dispatchNode.execute(receiver, index);
    }

    @ExportMessage
    protected static final long getArraySize(final AbstractSqueakObject receiver,
                    @Cached(value = "create(GET_ARRAY_SIZE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException {
        return (long) dispatchNode.execute(receiver);
    }

    @ExportMessage
    protected static final boolean isArrayElementReadable(final AbstractSqueakObject receiver, final long index,
                    @Cached(value = "create(IS_ARRAY_ELEMENT_READABLE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver, index);
    }

    @ExportMessage
    protected static final void writeArrayElement(final AbstractSqueakObject receiver, final long index, final Object value,
                    @Cached(value = "create(WRITE_ARRAY_ELEMENT)", allowUncached = true) final DispatchInteropMessageNode dispatchNode)
                    throws UnsupportedMessageException, UnsupportedTypeException, InvalidArrayIndexException {
        dispatchNode.execute(receiver, index, value);
    }

    @ExportMessage
    protected static final void removeArrayElement(final AbstractSqueakObject receiver, final long index,
                    @Cached(value = "create(REMOVE_ARRAY_ELEMENT)", allowUncached = true) final DispatchInteropMessageNode dispatchNode)
                    throws UnsupportedMessageException, InvalidArrayIndexException {
        dispatchNode.execute(receiver, index);
    }

    @ExportMessage
    protected static final boolean isArrayElementModifiable(final AbstractSqueakObject receiver, final long index,
                    @Cached(value = "create(IS_ARRAY_ELEMENT_MODIFIABLE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver, index);
    }

    @ExportMessage
    protected static final boolean isArrayElementInsertable(final AbstractSqueakObject receiver, final long index,
                    @Cached(value = "create(IS_ARRAY_ELEMENT_INSERTABLE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver, index);
    }

    @ExportMessage
    protected static final boolean isArrayElementRemovable(final AbstractSqueakObject receiver, final long index,
                    @Cached(value = "create(IS_ARRAY_ELEMENT_REMOVABLE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver, index);
    }

    // region Buffer Messages

    @ExportMessage
    protected static final boolean hasBufferElements(final AbstractSqueakObject receiver,
                    @Cached(value = "create(HAS_BUFFER_ELEMENTS)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver);
    }

    @ExportMessage
    protected static final boolean isBufferWritable(final AbstractSqueakObject receiver,
                    @Cached(value = "create(IS_BUFFER_WRITABLE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException {
        return dispatchNode.executeBoolean(receiver);
    }

    @ExportMessage
    protected static final long getBufferSize(final AbstractSqueakObject receiver,
                    @Cached(value = "create(GET_BUFFER_SIZE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException {
        return (long) dispatchNode.execute(receiver);
    }

    @ExportMessage
    protected static final byte readBufferByte(final AbstractSqueakObject receiver, final long byteOffset,
                    @Cached(value = "create(READ_BUFFER_BYTE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode)
                    throws UnsupportedMessageException, InvalidBufferOffsetException {
        return (byte) dispatchNode.execute(receiver, byteOffset);
    }

    @ExportMessage
    protected static final void writeBufferByte(final AbstractSqueakObject receiver, final long byteOffset, final byte value,
                    @Cached(value = "create(WRITE_BUFFER_BYTE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode)
                    throws UnsupportedMessageException, InvalidBufferOffsetException {
        dispatchNode.execute(receiver, byteOffset, value);
    }

    @ExportMessage
    protected static final short readBufferShort(final AbstractSqueakObject receiver, final ByteOrder order, final long byteOffset,
                    @Cached(value = "create(READ_BUFFER_SHORT)", allowUncached = true) final DispatchInteropMessageNode dispatchNode)
                    throws UnsupportedMessageException, InvalidBufferOffsetException {
        return (short) dispatchNode.execute(receiver, order, byteOffset);
    }

    @ExportMessage
    protected static final void writeBufferShort(final AbstractSqueakObject receiver, final ByteOrder order, final long byteOffset, final short value,
                    @Cached(value = "create(WRITE_BUFFER_SHORT)", allowUncached = true) final DispatchInteropMessageNode dispatchNode)
                    throws UnsupportedMessageException, InvalidBufferOffsetException {
        dispatchNode.execute(receiver, order, byteOffset, value);
    }

    @ExportMessage
    protected static final int readBufferInt(final AbstractSqueakObject receiver, final ByteOrder order, final long byteOffset,
                    @Cached(value = "create(READ_BUFFER_INT)", allowUncached = true) final DispatchInteropMessageNode dispatchNode)
                    throws UnsupportedMessageException, InvalidBufferOffsetException {
        return (int) dispatchNode.execute(receiver, order, byteOffset);
    }

    @ExportMessage
    protected static final void writeBufferInt(final AbstractSqueakObject receiver, final ByteOrder order, final long byteOffset, final int value,
                    @Cached(value = "create(WRITE_BUFFER_INT)", allowUncached = true) final DispatchInteropMessageNode dispatchNode)
                    throws UnsupportedMessageException, InvalidBufferOffsetException {
        dispatchNode.execute(receiver, order, byteOffset, value);
    }

    @ExportMessage
    protected static final long readBufferLong(final AbstractSqueakObject receiver, final ByteOrder order, final long byteOffset,
                    @Cached(value = "create(READ_BUFFER_LONG)", allowUncached = true) final DispatchInteropMessageNode dispatchNode)
                    throws UnsupportedMessageException, InvalidBufferOffsetException {
        return (long) dispatchNode.execute(receiver, order, byteOffset);
    }

    @ExportMessage
    protected static final void writeBufferLong(final AbstractSqueakObject receiver, final ByteOrder order, final long byteOffset, final long value,
                    @Cached(value = "create(WRITE_BUFFER_LONG)", allowUncached = true) final DispatchInteropMessageNode dispatchNode)
                    throws UnsupportedMessageException, InvalidBufferOffsetException {
        dispatchNode.execute(receiver, order, byteOffset, value);
    }

    @ExportMessage
    protected static final float readBufferFloat(final AbstractSqueakObject receiver, final ByteOrder order, final long byteOffset,
                    @Cached(value = "create(READ_BUFFER_FLOAT)", allowUncached = true) final DispatchInteropMessageNode dispatchNode)
                    throws UnsupportedMessageException, InvalidBufferOffsetException {
        return (float) dispatchNode.execute(receiver, order, byteOffset);
    }

    @ExportMessage
    protected static final void writeBufferFloat(final AbstractSqueakObject receiver, final ByteOrder order, final long byteOffset, final float value,
                    @Cached(value = "create(WRITE_BUFFER_FLOAT)", allowUncached = true) final DispatchInteropMessageNode dispatchNode)
                    throws UnsupportedMessageException, InvalidBufferOffsetException {
        dispatchNode.execute(receiver, order, byteOffset, value);
    }

    @ExportMessage
    protected static final double readBufferDouble(final AbstractSqueakObject receiver, final ByteOrder order, final long byteOffset,
                    @Cached(value = "create(READ_BUFFER_DOUBLE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode)
                    throws UnsupportedMessageException, InvalidBufferOffsetException {
        return (double) dispatchNode.execute(receiver, order, byteOffset);
    }

    @ExportMessage
    protected static final void writeBufferDouble(final AbstractSqueakObject receiver, final ByteOrder order, final long byteOffset, final double value,
                    @Cached(value = "create(WRITE_BUFFER_DOUBLE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode)
                    throws UnsupportedMessageException, InvalidBufferOffsetException {
        dispatchNode.execute(receiver, order, byteOffset, value);
    }

    // endregion

    @ExportMessage
    protected static final boolean isPointer(final AbstractSqueakObject receiver,
                    @Cached(value = "create(IS_POINTER)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver);
    }

    @ExportMessage
    protected static final long asPointer(final AbstractSqueakObject receiver,
                    @Cached(value = "create(AS_POINTER)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException {
        return (long) dispatchNode.execute(receiver);
    }

    @ExportMessage
    protected static final void toNative(final AbstractSqueakObject receiver,
                    @Cached(value = "create(TO_NATIVE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        dispatchNode.execute(receiver);
    }

// protected static final Instant asInstant(final AbstractSqueakObject receiver,

    @ExportMessage
    protected static final boolean isTimeZone(final AbstractSqueakObject receiver,
                    @Cached(value = "create(IS_TIME_ZONE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver);
    }

    @ExportMessage
    protected static final ZoneId asTimeZone(final AbstractSqueakObject receiver,
                    @Cached(value = "create(AS_TIME_ZONE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException {
        return (ZoneId) dispatchNode.execute(receiver);
    }

    @ExportMessage
    protected static final boolean isDate(final AbstractSqueakObject receiver,
                    @Cached(value = "create(IS_DATE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver);
    }

    @ExportMessage
    protected static final LocalDate asDate(final AbstractSqueakObject receiver,
                    @Cached(value = "create(AS_DATE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException {
        return (LocalDate) dispatchNode.execute(receiver);
    }

    @ExportMessage
    protected static final boolean isTime(final AbstractSqueakObject receiver,
                    @Cached(value = "create(IS_TIME)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver);
    }

    @ExportMessage
    protected static final LocalTime asTime(final AbstractSqueakObject receiver,
                    @Cached(value = "create(AS_TIME)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException {
        return (LocalTime) dispatchNode.execute(receiver);
    }

    @ExportMessage
    protected static final boolean isDuration(final AbstractSqueakObject receiver,
                    @Cached(value = "create(IS_DURATION)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver);
    }

    @ExportMessage
    protected static final Duration asDuration(final AbstractSqueakObject receiver,
                    @Cached(value = "create(AS_DURATION)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException {
        return (Duration) dispatchNode.execute(receiver);
    }

    @ExportMessage
    protected static final boolean isException(final AbstractSqueakObject receiver,
                    @Cached(value = "create(IS_EXCEPTION)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver);
    }

    @ExportMessage
    protected static final RuntimeException throwException(final AbstractSqueakObject receiver,
                    @Cached(value = "create(THROW_EXCEPTION)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException {
        return (RuntimeException) dispatchNode.execute(receiver);
    }

    @ExportMessage
    protected static final ExceptionType getExceptionType(final AbstractSqueakObject receiver,
                    @Cached(value = "create(GET_EXCEPTION_TYPE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException {
        return (ExceptionType) dispatchNode.execute(receiver);
    }

    @ExportMessage
    protected static final boolean isExceptionIncompleteSource(final AbstractSqueakObject receiver,
                    @Cached(value = "create(IS_EXCEPTION_INCOMPLETE_SOURCE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException {
        return dispatchNode.executeBoolean(receiver);
    }

    @ExportMessage
    protected static final int getExceptionExitStatus(final AbstractSqueakObject receiver,
                    @Cached(value = "create(GET_EXCEPTION_EXIT_STATUS)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException {
        return (int) dispatchNode.execute(receiver);
    }

    @ExportMessage
    protected static final boolean hasExceptionCause(final AbstractSqueakObject receiver,
                    @Cached(value = "create(HAS_EXCEPTION_CAUSE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver);
    }

    @ExportMessage
    protected static final Object getExceptionCause(final AbstractSqueakObject receiver,
                    @Cached(value = "create(GET_EXCEPTION_CAUSE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException {
        return dispatchNode.execute(receiver);
    }

    @ExportMessage
    protected static final boolean hasExceptionMessage(final AbstractSqueakObject receiver,
                    @Cached(value = "create(HAS_EXCEPTION_MESSAGE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver);
    }

    @ExportMessage
    protected static final Object getExceptionMessage(final AbstractSqueakObject receiver,
                    @Cached(value = "create(GET_EXCEPTION_MESSAGE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException {
        return dispatchNode.execute(receiver);
    }

    @ExportMessage
    protected static final boolean hasExceptionStackTrace(final AbstractSqueakObject receiver,
                    @Cached(value = "create(HAS_EXCEPTION_STACK_TRACE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver);
    }

    @ExportMessage
    protected static final Object getExceptionStackTrace(final AbstractSqueakObject receiver,
                    @Cached(value = "create(GET_EXCEPTION_STACK_TRACE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException {
        return dispatchNode.execute(receiver);
    }

    @ExportMessage
    protected static final boolean hasIterator(final AbstractSqueakObject receiver,
                    @Cached(value = "create(HAS_ITERATOR)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver);
    }

    @ExportMessage
    protected static final Object getIterator(final AbstractSqueakObject receiver,
                    @Cached(value = "create(GET_ITERATOR)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException {
        return dispatchNode.execute(receiver);
    }

    @ExportMessage
    protected static final boolean isIterator(final AbstractSqueakObject receiver,
                    @Cached(value = "create(IS_ITERATOR)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver);
    }

    @ExportMessage
    protected static final boolean hasIteratorNextElement(final AbstractSqueakObject receiver,
                    @Cached(value = "create(HAS_ITERATOR_NEXT_ELEMENT)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException {
        return dispatchNode.executeBoolean(receiver);
    }

    @ExportMessage
    protected static final Object getIteratorNextElement(final AbstractSqueakObject receiver,
                    @Cached(value = "create(GET_ITERATOR_NEXT_ELEMENT)", allowUncached = true) final DispatchInteropMessageNode dispatchNode)
                    throws UnsupportedMessageException, StopIterationException {
        return dispatchNode.execute(receiver);
    }

    @ExportMessage
    protected static final boolean hasSourceLocation(final AbstractSqueakObject receiver,
                    @Cached(value = "create(HAS_SOURCE_LOCATION)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver);
    }

    @ExportMessage
    protected static final SourceSection getSourceLocation(final AbstractSqueakObject receiver,
                    @Cached(value = "create(GET_SOURCE_LOCATION)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException {
        return (SourceSection) dispatchNode.execute(receiver);
    }

    @ExportMessage
    protected static final boolean hasLanguage(final AbstractSqueakObject receiver,
                    @Cached(value = "create(HAS_LANGUAGE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver);
    }

    @ExportMessage
    protected static final Class<? extends TruffleLanguage<?>> getLanguage(final AbstractSqueakObject receiver,
                    @Cached(value = "create(GET_LANGUAGE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException {
        return (Class<? extends TruffleLanguage<?>>) dispatchNode.execute(receiver);
    }

    @ExportMessage
    protected static final boolean hasMetaObject(final AbstractSqueakObject receiver,
                    @Cached(value = "create(HAS_META_OBJECT)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver);
    }

    @ExportMessage
    protected static final Object getMetaObject(final AbstractSqueakObject receiver,
                    @Cached(value = "create(GET_META_OBJECT)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException {
        return dispatchNode.execute(receiver);
    }

    @ExportMessage
    protected static final Object toDisplayString(final AbstractSqueakObject receiver, final boolean allowSideEffects,
                    @Cached(value = "create(TO_DISPLAY_STRING)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.execute(receiver, allowSideEffects);
    }

    @ExportMessage
    protected static final boolean isMetaObject(final AbstractSqueakObject receiver,
                    @Cached(value = "create(IS_META_OBJECT)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver);
    }

    @ExportMessage
    protected static final Object getMetaQualifiedName(final AbstractSqueakObject receiver,
                    @Cached(value = "create(GET_META_QUALIFIED_NAME)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException {
        return dispatchNode.execute(receiver);
    }

    @ExportMessage
    protected static final Object getMetaSimpleName(final AbstractSqueakObject receiver,
                    @Cached(value = "create(GET_META_SIMPLE_NAME)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException {
        return dispatchNode.execute(receiver);
    }

    @ExportMessage
    protected static final boolean isMetaInstance(final AbstractSqueakObject receiver, final Object instance,
                    @Cached(value = "create(IS_META_INSTANCE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException {
        return dispatchNode.executeBoolean(receiver, instance);
    }

    @ExportMessage
    protected static final boolean hasMetaParents(final AbstractSqueakObject receiver,
                    @Cached(value = "create(HAS_META_PARENTS)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver);
    }

    @ExportMessage
    protected static final Object getMetaParents(final AbstractSqueakObject receiver,
                    @Cached(value = "create(GET_META_PARENTS)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException {
        return dispatchNode.execute(receiver);
    }

    @ExportMessage
    protected static final TriState isIdenticalOrUndefined(final AbstractSqueakObject receiver, final Object other,
                    @Cached(value = "create(IS_IDENTICAL_OR_UNDEFINED)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return (TriState) dispatchNode.execute(receiver, other);
    }

    @ExportMessage
    protected static final boolean isIdentical(final AbstractSqueakObject receiver, final Object other, final InteropLibrary otherInterop,
                    @Cached(value = "create(IS_IDENTICAL)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver, other, otherInterop);
    }

    @ExportMessage
    protected static final int identityHashCode(final AbstractSqueakObject receiver,
                    @Cached(value = "create(IDENTITY_HASH_CODE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException {
        return (int) dispatchNode.execute(receiver);
    }

    @ExportMessage
    protected static final boolean isScope(final AbstractSqueakObject receiver,
                    @Cached(value = "create(IS_SCOPE)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver);
    }

    @ExportMessage
    protected static final boolean hasScopeParent(final AbstractSqueakObject receiver,
                    @Cached(value = "create(HAS_SCOPE_PARENT)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) {
        return dispatchNode.executeBoolean(receiver);
    }

    @ExportMessage
    protected static final Object getScopeParent(final AbstractSqueakObject receiver,
                    @Cached(value = "create(GET_SCOPE_PARENT)", allowUncached = true) final DispatchInteropMessageNode dispatchNode) throws UnsupportedMessageException {
        return dispatchNode.execute(receiver);
    }
}
