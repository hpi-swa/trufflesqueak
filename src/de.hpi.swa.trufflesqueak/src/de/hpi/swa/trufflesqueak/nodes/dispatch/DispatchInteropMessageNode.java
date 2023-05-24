/*
 * Copyright (c) 2023 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2023 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.Message;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.interop.WrapToSqueakNode;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.LookupMethodNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public abstract class DispatchInteropMessageNode extends AbstractNode {
    public static final Message IS_NULL = Message.resolve(InteropLibrary.class, "isNull");
    public static final Message IS_BOOLEAN = Message.resolve(InteropLibrary.class, "isBoolean");
    public static final Message AS_BOOLEAN = Message.resolve(InteropLibrary.class, "asBoolean");
    public static final Message IS_EXECUTABLE = Message.resolve(InteropLibrary.class, "isExecutable");
    public static final Message EXECUTE = Message.resolve(InteropLibrary.class, "execute");
    public static final Message HAS_EXECUTABLE_NAME = Message.resolve(InteropLibrary.class, "hasExecutableName");
    public static final Message GET_EXECUTABLE_NAME = Message.resolve(InteropLibrary.class, "getExecutableName");
    public static final Message HAS_DECLARING_META_OBJECT = Message.resolve(InteropLibrary.class, "hasDeclaringMetaObject");
    public static final Message GET_DECLARING_META_OBJECT = Message.resolve(InteropLibrary.class, "getDeclaringMetaObject");
    public static final Message IS_INSTANTIABLE = Message.resolve(InteropLibrary.class, "isInstantiable");
    public static final Message INSTANTIATE = Message.resolve(InteropLibrary.class, "instantiate");
    public static final Message IS_STRING = Message.resolve(InteropLibrary.class, "isString");
    public static final Message AS_STRING = Message.resolve(InteropLibrary.class, "asString");
    public static final Message AS_TRUFFLE_STRING = Message.resolve(InteropLibrary.class, "asTruffleString");
    public static final Message IS_NUMBER = Message.resolve(InteropLibrary.class, "isNumber");
    public static final Message FITS_IN_BYTE = Message.resolve(InteropLibrary.class, "fitsInByte");
    public static final Message FITS_IN_SHORT = Message.resolve(InteropLibrary.class, "fitsInShort");
    public static final Message FITS_IN_INT = Message.resolve(InteropLibrary.class, "fitsInInt");
    public static final Message FITS_IN_LONG = Message.resolve(InteropLibrary.class, "fitsInLong");
    public static final Message FITS_IN_BIG_INTEGER = Message.resolve(InteropLibrary.class, "fitsInBigInteger");
    public static final Message FITS_IN_FLOAT = Message.resolve(InteropLibrary.class, "fitsInFloat");
    public static final Message FITS_IN_DOUBLE = Message.resolve(InteropLibrary.class, "fitsInDouble");
    public static final Message AS_BYTE = Message.resolve(InteropLibrary.class, "asByte");
    public static final Message AS_SHORT = Message.resolve(InteropLibrary.class, "asShort");
    public static final Message AS_INT = Message.resolve(InteropLibrary.class, "asInt");
    public static final Message AS_LONG = Message.resolve(InteropLibrary.class, "asLong");
    public static final Message AS_BIG_INTEGER = Message.resolve(InteropLibrary.class, "asBigInteger");
    public static final Message AS_FLOAT = Message.resolve(InteropLibrary.class, "asFloat");
    public static final Message AS_DOUBLE = Message.resolve(InteropLibrary.class, "asDouble");
    public static final Message HAS_MEMBERS = Message.resolve(InteropLibrary.class, "hasMembers");
    public static final Message GET_MEMBERS = Message.resolve(InteropLibrary.class, "getMembers");
    public static final Message IS_MEMBER_READABLE = Message.resolve(InteropLibrary.class, "isMemberReadable");
    public static final Message READ_MEMBER = Message.resolve(InteropLibrary.class, "readMember");
    public static final Message IS_MEMBER_MODIFIABLE = Message.resolve(InteropLibrary.class, "isMemberModifiable");
    public static final Message IS_MEMBER_INSERTABLE = Message.resolve(InteropLibrary.class, "isMemberInsertable");
    public static final Message WRITE_MEMBER = Message.resolve(InteropLibrary.class, "writeMember");
    public static final Message IS_MEMBER_REMOVABLE = Message.resolve(InteropLibrary.class, "isMemberRemovable");
    public static final Message REMOVE_MEMBER = Message.resolve(InteropLibrary.class, "removeMember");
    public static final Message IS_MEMBER_INVOCABLE = Message.resolve(InteropLibrary.class, "isMemberInvocable");
    public static final Message INVOKE_MEMBER = Message.resolve(InteropLibrary.class, "invokeMember");
    public static final Message IS_MEMBER_INTERNAL = Message.resolve(InteropLibrary.class, "isMemberInternal");
    public static final Message HAS_MEMBER_READ_SIDE_EFFECTS = Message.resolve(InteropLibrary.class, "hasMemberReadSideEffects");
    public static final Message HAS_MEMBER_WRITE_SIDE_EFFECTS = Message.resolve(InteropLibrary.class, "hasMemberWriteSideEffects");
    public static final Message HAS_HASH_ENTRIES = Message.resolve(InteropLibrary.class, "hasHashEntries");
    public static final Message GET_HASH_SIZE = Message.resolve(InteropLibrary.class, "getHashSize");
    public static final Message IS_HASH_ENTRY_READABLE = Message.resolve(InteropLibrary.class, "isHashEntryReadable");
    public static final Message READ_HASH_VALUE = Message.resolve(InteropLibrary.class, "readHashValue");
    public static final Message READ_HASH_VALUE_OR_DEFAULT = Message.resolve(InteropLibrary.class, "readHashValueOrDefault");
    public static final Message IS_HASH_ENTRY_MODIFIABLE = Message.resolve(InteropLibrary.class, "isHashEntryModifiable");
    public static final Message IS_HASH_ENTRY_INSERTABLE = Message.resolve(InteropLibrary.class, "isHashEntryInsertable");
    public static final Message IS_HASH_ENTRY_WRITABLE = Message.resolve(InteropLibrary.class, "isHashEntryWritable");
    public static final Message WRITE_HASH_ENTRY = Message.resolve(InteropLibrary.class, "writeHashEntry");
    public static final Message IS_HASH_ENTRY_REMOVABLE = Message.resolve(InteropLibrary.class, "isHashEntryRemovable");
    public static final Message REMOVE_HASH_ENTRY = Message.resolve(InteropLibrary.class, "removeHashEntry");
    public static final Message IS_HASH_ENTRY_EXISTING = Message.resolve(InteropLibrary.class, "isHashEntryExisting");
    public static final Message GET_HASH_ENTRIES_ITERATOR = Message.resolve(InteropLibrary.class, "getHashEntriesIterator");
    public static final Message GET_HASH_KEYS_ITERATOR = Message.resolve(InteropLibrary.class, "getHashKeysIterator");
    public static final Message GET_HASH_VALUES_ITERATOR = Message.resolve(InteropLibrary.class, "getHashValuesIterator");
    public static final Message HAS_ARRAY_ELEMENTS = Message.resolve(InteropLibrary.class, "hasArrayElements");
    public static final Message READ_ARRAY_ELEMENT = Message.resolve(InteropLibrary.class, "readArrayElement");
    public static final Message GET_ARRAY_SIZE = Message.resolve(InteropLibrary.class, "getArraySize");
    public static final Message IS_ARRAY_ELEMENT_READABLE = Message.resolve(InteropLibrary.class, "isArrayElementReadable");
    public static final Message WRITE_ARRAY_ELEMENT = Message.resolve(InteropLibrary.class, "writeArrayElement");
    public static final Message REMOVE_ARRAY_ELEMENT = Message.resolve(InteropLibrary.class, "removeArrayElement");
    public static final Message IS_ARRAY_ELEMENT_MODIFIABLE = Message.resolve(InteropLibrary.class, "isArrayElementModifiable");
    public static final Message IS_ARRAY_ELEMENT_INSERTABLE = Message.resolve(InteropLibrary.class, "isArrayElementInsertable");
    public static final Message IS_ARRAY_ELEMENT_REMOVABLE = Message.resolve(InteropLibrary.class, "isArrayElementRemovable");
    public static final Message HAS_BUFFER_ELEMENTS = Message.resolve(InteropLibrary.class, "hasBufferElements");
    public static final Message IS_BUFFER_WRITABLE = Message.resolve(InteropLibrary.class, "isBufferWritable");
    public static final Message GET_BUFFER_SIZE = Message.resolve(InteropLibrary.class, "getBufferSize");
    public static final Message READ_BUFFER_BYTE = Message.resolve(InteropLibrary.class, "readBufferByte");
    public static final Message WRITE_BUFFER_BYTE = Message.resolve(InteropLibrary.class, "writeBufferByte");
    public static final Message READ_BUFFER_SHORT = Message.resolve(InteropLibrary.class, "readBufferShort");
    public static final Message WRITE_BUFFER_SHORT = Message.resolve(InteropLibrary.class, "writeBufferShort");
    public static final Message READ_BUFFER_INT = Message.resolve(InteropLibrary.class, "readBufferInt");
    public static final Message WRITE_BUFFER_INT = Message.resolve(InteropLibrary.class, "writeBufferInt");
    public static final Message READ_BUFFER_LONG = Message.resolve(InteropLibrary.class, "readBufferLong");
    public static final Message WRITE_BUFFER_LONG = Message.resolve(InteropLibrary.class, "writeBufferLong");
    public static final Message READ_BUFFER_FLOAT = Message.resolve(InteropLibrary.class, "readBufferFloat");
    public static final Message WRITE_BUFFER_FLOAT = Message.resolve(InteropLibrary.class, "writeBufferFloat");
    public static final Message READ_BUFFER_DOUBLE = Message.resolve(InteropLibrary.class, "readBufferDouble");
    public static final Message WRITE_BUFFER_DOUBLE = Message.resolve(InteropLibrary.class, "writeBufferDouble");
    public static final Message IS_POINTER = Message.resolve(InteropLibrary.class, "isPointer");
    public static final Message AS_POINTER = Message.resolve(InteropLibrary.class, "asPointer");
    public static final Message TO_NATIVE = Message.resolve(InteropLibrary.class, "toNative");
    public static final Message AS_INSTANT = Message.resolve(InteropLibrary.class, "asInstant");
    public static final Message IS_TIME_ZONE = Message.resolve(InteropLibrary.class, "isTimeZone");
    public static final Message AS_TIME_ZONE = Message.resolve(InteropLibrary.class, "asTimeZone");
    public static final Message IS_DATE = Message.resolve(InteropLibrary.class, "isDate");
    public static final Message AS_DATE = Message.resolve(InteropLibrary.class, "asDate");
    public static final Message IS_TIME = Message.resolve(InteropLibrary.class, "isTime");
    public static final Message AS_TIME = Message.resolve(InteropLibrary.class, "asTime");
    public static final Message IS_DURATION = Message.resolve(InteropLibrary.class, "isDuration");
    public static final Message AS_DURATION = Message.resolve(InteropLibrary.class, "asDuration");
    public static final Message IS_EXCEPTION = Message.resolve(InteropLibrary.class, "isException");
    public static final Message THROW_EXCEPTION = Message.resolve(InteropLibrary.class, "throwException");
    public static final Message GET_EXCEPTION_TYPE = Message.resolve(InteropLibrary.class, "getExceptionType");
    public static final Message IS_EXCEPTION_INCOMPLETE_SOURCE = Message.resolve(InteropLibrary.class, "isExceptionIncompleteSource");
    public static final Message GET_EXCEPTION_EXIT_STATUS = Message.resolve(InteropLibrary.class, "getExceptionExitStatus");
    public static final Message HAS_EXCEPTION_CAUSE = Message.resolve(InteropLibrary.class, "hasExceptionCause");
    public static final Message GET_EXCEPTION_CAUSE = Message.resolve(InteropLibrary.class, "getExceptionCause");
    public static final Message HAS_EXCEPTION_MESSAGE = Message.resolve(InteropLibrary.class, "hasExceptionMessage");
    public static final Message GET_EXCEPTION_MESSAGE = Message.resolve(InteropLibrary.class, "getExceptionMessage");
    public static final Message HAS_EXCEPTION_STACK_TRACE = Message.resolve(InteropLibrary.class, "hasExceptionStackTrace");
    public static final Message GET_EXCEPTION_STACK_TRACE = Message.resolve(InteropLibrary.class, "getExceptionStackTrace");
    public static final Message HAS_ITERATOR = Message.resolve(InteropLibrary.class, "hasIterator");
    public static final Message GET_ITERATOR = Message.resolve(InteropLibrary.class, "getIterator");
    public static final Message IS_ITERATOR = Message.resolve(InteropLibrary.class, "isIterator");
    public static final Message HAS_ITERATOR_NEXT_ELEMENT = Message.resolve(InteropLibrary.class, "hasIteratorNextElement");
    public static final Message GET_ITERATOR_NEXT_ELEMENT = Message.resolve(InteropLibrary.class, "getIteratorNextElement");
    public static final Message HAS_SOURCE_LOCATION = Message.resolve(InteropLibrary.class, "hasSourceLocation");
    public static final Message GET_SOURCE_LOCATION = Message.resolve(InteropLibrary.class, "getSourceLocation");
    public static final Message HAS_LANGUAGE = Message.resolve(InteropLibrary.class, "hasLanguage");
    public static final Message GET_LANGUAGE = Message.resolve(InteropLibrary.class, "getLanguage");
    public static final Message HAS_META_OBJECT = Message.resolve(InteropLibrary.class, "hasMetaObject");
    public static final Message GET_META_OBJECT = Message.resolve(InteropLibrary.class, "getMetaObject");
    public static final Message TO_DISPLAY_STRING = Message.resolve(InteropLibrary.class, "toDisplayString");
    public static final Message IS_META_OBJECT = Message.resolve(InteropLibrary.class, "isMetaObject");
    public static final Message GET_META_QUALIFIED_NAME = Message.resolve(InteropLibrary.class, "getMetaQualifiedName");
    public static final Message GET_META_SIMPLE_NAME = Message.resolve(InteropLibrary.class, "getMetaSimpleName");
    public static final Message IS_META_INSTANCE = Message.resolve(InteropLibrary.class, "isMetaInstance");
    public static final Message HAS_META_PARENTS = Message.resolve(InteropLibrary.class, "hasMetaParents");
    public static final Message GET_META_PARENTS = Message.resolve(InteropLibrary.class, "getMetaParents");
    public static final Message IS_IDENTICAL_OR_UNDEFINED = Message.resolve(InteropLibrary.class, "isIdenticalOrUndefined");
    public static final Message IS_IDENTICAL = Message.resolve(InteropLibrary.class, "isIdentical");
    public static final Message IDENTITY_HASH_CODE = Message.resolve(InteropLibrary.class, "identityHashCode");
    public static final Message IS_SCOPE = Message.resolve(InteropLibrary.class, "isScope");
    public static final Message HAS_SCOPE_PARENT = Message.resolve(InteropLibrary.class, "hasScopeParent");
    public static final Message GET_SCOPE_PARENT = Message.resolve(InteropLibrary.class, "getScopeParent");

    protected final Message interopMessage;

    protected DispatchInteropMessageNode(final Message interopMessage) {
        this.interopMessage = interopMessage;
    }

    @NeverDefault
    public static DispatchInteropMessageNode create(final Message interopMessage) {
        return DispatchInteropMessageNodeGen.create(interopMessage);
    }

    public final boolean executeBoolean(final AbstractSqueakObject receiver, final Object... arguments) {
        return Boolean.TRUE.equals(executeSpecialized(receiver, arguments));
    }

    public final Object execute(final AbstractSqueakObject receiver, final Object... arguments) {
        return executeSpecialized(receiver, arguments);
    }

    protected abstract Object executeSpecialized(AbstractSqueakObject receiver, Object[] arguments);

    @SuppressWarnings("unused")
    @ExplodeLoop
    @Specialization(guards = {"classNode.executeLookup(receiver) == cachedClass", "cachedMethod != null"}, limit = "8", //
                    assumptions = {"cachedClass.getClassHierarchyStable()", "cachedClass.getMethodDictStable()", "cachedMethod.getCallTargetStable()"})
    protected final Object doCached(final AbstractSqueakObject receiver, final Object[] arguments,
                    @Cached final SqueakObjectClassNode classNode,
                    @Cached("classNode.executeLookup(receiver)") final ClassObject cachedClass,
                    @Cached("lookupMethod(cachedClass, interopMessage)") final CompiledCodeObject cachedMethod,
                    @Cached("create(cachedMethod.getCallTarget())") final DirectCallNode callNode,
                    @Cached final WrapToSqueakNode wrapNode) {
        final int numArgs = interopMessage.getParameterCount() - 1;
        assert numArgs == arguments.length;
        final Object[] frameArguments = FrameAccess.newWith(NilObject.SINGLETON, null, interopMessage.getParameterCount());
        frameArguments[FrameAccess.getReceiverStartIndex()] = receiver;
        for (int i = 0; i < interopMessage.getParameterCount() - 1; i++) {
            frameArguments[FrameAccess.getArgumentStartIndex() + i] = wrapNode.executeWrap(arguments[i]);
        }
        return callNode.call(frameArguments);
    }

    protected static final CompiledCodeObject lookupMethod(final ClassObject clazz, final Message message) {
        return clazz.lookupMethodInMethodDictSlow(SqueakImageContext.get(null).toInteropSelector(message));
    }

    @TruffleBoundary
    @ReportPolymorphism.Megamorphic
    @Specialization(replaces = "doCached")
    protected final Object doGeneric(final AbstractSqueakObject receiver, final Object[] arguments,
                    @Cached final LookupMethodNode lookupNode,
                    @Cached final SqueakObjectClassNode classNode,
                    @Cached final DispatchUneagerlyNode dispatchNode,
                    @Cached final WrapToSqueakNode wrapNode) {
        final SqueakImageContext image = SqueakImageContext.get(lookupNode);
        assert interopMessage.getLibraryClass() == InteropLibrary.class;
        final NativeObject selector = image.toInteropSelector(interopMessage);
        final Object methodObject = lookupNode.executeLookup(classNode.executeLookup(receiver), selector);
        if (methodObject instanceof final CompiledCodeObject method) {
            assert interopMessage.getLibraryClass() == InteropLibrary.class;
            final Object[] receiverAndArguments = new Object[interopMessage.getParameterCount()];
            receiverAndArguments[0] = receiver;
            for (int i = 0; i < arguments.length; i++) {
                receiverAndArguments[1 + i] = wrapNode.executeWrap(arguments[i]);
            }
            try {
                return dispatchNode.executeDispatch(method, receiverAndArguments, NilObject.SINGLETON);
            } catch (final ProcessSwitch ps) {
                CompilerDirectives.transferToInterpreter();
                image.printToStdErr(ps);
                throw new IllegalArgumentException();
            }
        } else {
            image.printToStdErr(selector, "method:", methodObject);
            throw PrimitiveFailed.andTransferToInterpreter();
        }
    }
}
