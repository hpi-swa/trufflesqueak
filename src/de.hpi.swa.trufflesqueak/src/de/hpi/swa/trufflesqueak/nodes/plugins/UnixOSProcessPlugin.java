/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.io.IOException;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Stream;

import org.graalvm.polyglot.io.FileChannel;
import org.graalvm.polyglot.io.FileLock;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.TruffleLanguage.Env;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.plugins.FilePlugin.STDIO_HANDLES;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive0;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive2WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive3WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive4WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.util.MiscUtils;

public final class UnixOSProcessPlugin extends AbstractOSProcessPlugin {
    protected abstract static class AbstractFilePrimitiveNode extends AbstractPrimitiveNode {

        @TruffleBoundary
        private static long decodePermissions(final Set<PosixFilePermission> permissions, final PosixFilePermission read, final PosixFilePermission write, final PosixFilePermission execute) {
            return (permissions.contains(read) ? 4 : 0) | (permissions.contains(write) ? 2 : 0) | (permissions.contains(execute) ? 1 : 0);
        }

        protected static final ArrayObject getProtectionMask(final SqueakImageContext image, final Set<PosixFilePermission> permissions) {
            final long owner = decodePermissions(permissions, PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE);
            final long group = decodePermissions(permissions, PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_WRITE, PosixFilePermission.GROUP_EXECUTE);
            final long others = decodePermissions(permissions, PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_WRITE, PosixFilePermission.OTHERS_EXECUTE);
            return image.asArrayOfLongs(0L, owner, group, others);
        }
    }

    protected abstract static class AbstractKillPrimitiveNode extends AbstractSysCallPrimitiveNode {
        @Override
        protected final String getFunctionName() {
            return "kill";
        }

        @Override
        protected final FunctionDescriptor getFunctionSignature() {
            return FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveArgumentAt")
    protected abstract static class PrimArgumentAtNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected final Object doAt(@SuppressWarnings("unused") final Object receiver, final long index) {
            final SqueakImageContext image = getContext();
            if (index == 1) {
                return image.asByteString(MiscUtils.getVMPath());
            } else {
                final String[] imageArguments = image.getImageArguments();
                if (1 < index && index <= (long) imageArguments.length + 1) {
                    return image.asByteString(imageArguments[(int) (index - 2)]);
                }
                return NilObject.SINGLETON;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveCanReceiveSignals")
    protected abstract static class PrimCanReceiveSignalsNode extends AbstractKillPrimitiveNode implements Primitive1WithFallback {
        @SuppressWarnings("unused")
        @Specialization(guards = "!isLong(pid)")
        protected static final boolean doCanReceiveSignals(final Object receiver, final Object pid) {
            return BooleanObject.FALSE;
        }

        @Specialization
        protected final boolean doCanReceiveSignals(@SuppressWarnings("unused") final Object receiver, final long pid) {
            return BooleanObject.wrap(setValue(pid, SIGNALS.SIG_DFL) == 0);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveCreatePipe")
    protected abstract static class PrimCreatePipeNode extends AbstractPrimitiveNode implements Primitive0 {
        /* Answers an Array with the file handles of the read end and of the write end. */
        @Specialization
        @TruffleBoundary
        protected final ArrayObject doCreatePipe(@SuppressWarnings("unused") final Object receiver) {
            final SqueakImageContext image = getContext();
            final AbstractPipeEndChannel readEnd;
            final AbstractPipeEndChannel writeEnd;
            try {
                final Pipe pipe = Pipe.open();
                readEnd = new PipeReadEndChannel(pipe.source());
                writeEnd = new PipeWriteEndChannel(pipe.sink());
            } catch (final IOException e) {
                throw PrimitiveFailed.GENERIC_ERROR;
            }
            image.env.registerOnDispose(readEnd);
            image.env.registerOnDispose(writeEnd);
            return image.asArrayOfObjects(PointersObject.newHandleWithHiddenObject(image, readEnd), PointersObject.newHandleWithHiddenObject(image, writeEnd));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveLockFileRegion")
    protected abstract static class PrimLockFileRegionNode extends AbstractFileHandlePrimitiveNode implements Primitive4WithFallback {
        /* Answers 0 if the region was locked and -1 otherwise, like fcntl(). */
        @Specialization(guards = {"isLockableHandle(handle)", "offset >= 0", "length >= 0"})
        protected static final long doLock(@SuppressWarnings("unused") final Object receiver, final PointersObject handle, final long offset, final long length, final boolean exclusive) {
            return FileLocks.lock(getFileChannel(handle), offset, length, exclusive) ? 0L : -1L;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveUnlockFileRegion")
    protected abstract static class PrimUnlockFileRegionNode extends AbstractFileHandlePrimitiveNode implements Primitive3WithFallback {
        /* Answers 0 if the region is unlocked and -1 otherwise, like fcntl(). */
        @Specialization(guards = {"isLockableHandle(handle)", "offset >= 0", "length >= 0"})
        protected static final long doUnlock(@SuppressWarnings("unused") final Object receiver, final PointersObject handle, final long offset, final long length) {
            return FileLocks.unlock(getFileChannel(handle), offset, length) ? 0L : -1L;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveTestLockableFileRegion")
    protected abstract static class PrimTestLockableFileRegionNode extends AbstractFileHandlePrimitiveNode implements Primitive4WithFallback {
        /*
         * Answers an Array like the OpenSmalltalk VM. Its first element tells whether the region
         * can be locked. The remaining elements describe a conflicting lock (pid, type, whence,
         * start, and length), which is not known here.
         */
        @Specialization(guards = {"isLockableHandle(handle)", "offset >= 0", "length >= 0"})
        protected final ArrayObject doTest(@SuppressWarnings("unused") final Object receiver, final PointersObject handle, final long offset, final long length, final boolean exclusive) {
            final boolean isLockable = FileLocks.isLockable(getFileChannel(handle), offset, length, exclusive);
            return getContext().asArrayOfObjects(BooleanObject.wrap(isLockable), NilObject.SINGLETON, NilObject.SINGLETON, NilObject.SINGLETON, NilObject.SINGLETON, NilObject.SINGLETON);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSQFileSetBlocking")
    protected abstract static class PrimSQFileSetBlockingNode extends AbstractFileHandlePrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "isFileHandle(handle)")
        protected static final long doSetBlocking(@SuppressWarnings("unused") final Object receiver, final PointersObject handle) {
            return configureBlocking(handle, true);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSQFileSetNonBlocking")
    protected abstract static class PrimSQFileSetNonBlockingNode extends AbstractFileHandlePrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "isFileHandle(handle)")
        protected static final long doSetNonBlocking(@SuppressWarnings("unused") final Object receiver, final PointersObject handle) {
            return configureBlocking(handle, false);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSQFileFlush")
    protected abstract static class PrimSQFileFlushNode extends AbstractFileHandlePrimitiveNode implements Primitive1WithFallback {
        /* Channels are not buffered, so there is nothing to flush. */
        @SuppressWarnings("unused")
        @Specialization(guards = "isFileHandle(handle) || isStdioHandle(handle)")
        protected static final long doFlush(final Object receiver, final PointersObject handle) {
            return 0L;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveTestEndOfFileFlag")
    protected abstract static class PrimTestEndOfFileFlagNode extends AbstractFileHandlePrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "isPipeHandle(handle)")
        protected static final boolean doTest(@SuppressWarnings("unused") final Object receiver, final PointersObject handle) {
            return BooleanObject.wrap(((AbstractPipeEndChannel) handle.getHiddenObject()).isAtEndOfFile());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveEnvironmentAt")
    protected abstract static class PrimEnvironmentAtNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "index > 0")
        @TruffleBoundary
        protected final NativeObject doAt(@SuppressWarnings("unused") final Object receiver, final long index) {
            final SqueakImageContext image = getContext();
            final List<String> entries = Environment.entries(image.env);
            if (index > entries.size()) {
                throw PrimitiveFailed.GENERIC_ERROR;
            }
            return image.asByteString(entries.get((int) index - 1));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveEnvironmentAtSymbol")
    protected abstract static class PrimEnvironmentAtSymbolNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "aSymbol.isByteType()")
        protected final NativeObject doAt(@SuppressWarnings("unused") final Object receiver, final NativeObject aSymbol) {
            final SqueakImageContext image = getContext();
            final String key = aSymbol.asStringUnsafe();
            final String value = systemGetEnv(image.env, key);
            if (value == null) {
                throw PrimitiveFailed.GENERIC_ERROR;
            } else {
                return image.asByteString(value);
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitivePutEnv")
    protected abstract static class PrimPutEnvNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        /* The argument is a null-terminated "KEY=value" string. */
        @Specialization(guards = "keyValueString.isByteType()")
        protected static final NativeObject doPutEnv(@SuppressWarnings("unused") final Object receiver, final NativeObject keyValueString) {
            if (Environment.putKeyValue(keyValueString)) {
                return keyValueString;
            } else {
                throw PrimitiveFailed.GENERIC_ERROR;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveUnsetEnv")
    protected abstract static class PrimUnsetEnvNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        /* The argument is a null-terminated string. */
        @Specialization(guards = "keyString.isByteType()")
        protected static final Object doUnsetEnv(final Object receiver, final NativeObject keyString) {
            if (Environment.unset(keyString)) {
                return receiver;
            } else {
                throw PrimitiveFailed.GENERIC_ERROR;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveErrorMessageAt")
    protected abstract static class PrimErrorMessageAtNode extends AbstractSysCallPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected final NativeObject doErrorMessageAt(@SuppressWarnings("unused") final Object receiver, final long index) {
            return getContext().asByteString(getErrorMessage((int) index));
        }

        @TruffleBoundary
        @SuppressWarnings("restricted")
        private String getErrorMessage(final int errnum) {
            try {
                final MemorySegment cStringPtr = (MemorySegment) getHandle().invokeExact(errnum);
                /* The returned pointer has a size of zero and must be resized to read the string. */
                return cStringPtr.reinterpret(Long.MAX_VALUE).getString(0);
            } catch (final Throwable e) {
                throw PrimitiveFailed.andTransferToInterpreterWithError(e);
            }
        }

        @Override
        protected final String getFunctionName() {
            return "strerror";
        }

        @Override
        protected final FunctionDescriptor getFunctionSignature() {
            return FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_INT);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFileProtectionMask")
    protected abstract static class PrimFileProtectionMaskNode extends AbstractFilePrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "pathString.isByteType()")
        protected final ArrayObject doFileProtectionMask(@SuppressWarnings("unused") final Object receiver, final NativeObject pathString,
                        @Bind final Node node,
                        @Cached final InlinedBranchProfile errorProfile) {
            final SqueakImageContext image = getContext();
            try {
                final TruffleFile file = image.env.getPublicTruffleFile(pathString.asStringUnsafe());
                return getProtectionMask(image, file.getPosixPermissions());
            } catch (final IOException | UnsupportedOperationException | SecurityException e) {
                errorProfile.enter(node);
                throw PrimitiveFailed.GENERIC_ERROR;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFileStat")
    protected abstract static class PrimFileStatNode extends AbstractFilePrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "pathString.isByteType()")
        protected final ArrayObject doFileStat(@SuppressWarnings("unused") final Object receiver, final NativeObject pathString,
                        @Bind final Node node,
                        @Cached final InlinedBranchProfile errorProfile) {
            final SqueakImageContext image = getContext();
            try {
                final TruffleFile file = image.env.getPublicTruffleFile(pathString.asStringUnsafe());
                final long uid = file.getOwner().hashCode();
                final long gid = file.getGroup().hashCode();
                final ArrayObject mask = getProtectionMask(image, file.getPosixPermissions());
                return image.asArrayOfObjects(uid, gid, mask);
            } catch (final IOException | UnsupportedOperationException | SecurityException e) {
                errorProfile.enter(node);
                throw PrimitiveFailed.GENERIC_ERROR;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveForwardSignalToSemaphore")
    protected abstract static class PrimForwardSignalToSemaphoreNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @SuppressWarnings("unused")
        @Specialization
        protected static final boolean doForward(final Object receiver, final long signalNumber, final long semaphoreIndex) {
            return BooleanObject.TRUE; // TODO: implement
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetEGid")
    protected abstract static class PrimGetEGidNode extends AbstractSysCallPrimitiveNode implements Primitive0 {
        @Specialization
        protected final long doGetEGid(@SuppressWarnings("unused") final Object receiver) {
            return getValue();
        }

        @Override
        protected final String getFunctionName() {
            return "getegid";
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetEUid")
    protected abstract static class PrimGetEUidNode extends AbstractSysCallPrimitiveNode implements Primitive0 {
        @Specialization
        protected final long doGetEUid(@SuppressWarnings("unused") final Object receiver) {
            return getValue();
        }

        @Override
        protected final String getFunctionName() {
            return "geteuid";
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetGid")
    protected abstract static class PrimGetGidNode extends AbstractSysCallPrimitiveNode implements Primitive0 {
        @Specialization
        protected final long doGetGid(@SuppressWarnings("unused") final Object receiver) {
            return getValue();
        }

        @Override
        protected final String getFunctionName() {
            return "getgid";
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetPGid")
    protected abstract static class PrimGetPGidNode extends AbstractSysCallPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected final long doGetPGid(@SuppressWarnings("unused") final Object receiver, final long pid,
                        @Bind final Node node,
                        @Cached final InlinedBranchProfile errorProfile) {
            return failIfMinusOne(getValue(pid), errorProfile, node);
        }

        @Override
        protected final String getFunctionName() {
            return "getpgid";
        }

        @Override
        protected final FunctionDescriptor getFunctionSignature() {
            return FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetPGrp")
    protected abstract static class PrimGetPGrpNode extends AbstractSysCallPrimitiveNode implements Primitive0 {
        @Specialization
        protected final long doGetPGrp(@SuppressWarnings("unused") final Object receiver,
                        @Bind final Node node,
                        @Cached final InlinedBranchProfile errorProfile) {
            return failIfMinusOne(getValue(), errorProfile, node);
        }

        @Override
        protected final String getFunctionName() {
            return "getpgrp";
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetPPid")
    protected abstract static class PrimGetPPidNode extends AbstractSysCallPrimitiveNode implements Primitive0 {
        @Specialization
        protected final long doGetPPid(@SuppressWarnings("unused") final Object receiver) {
            return getValue();
        }

        @Override
        protected final String getFunctionName() {
            return "getppid";
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetStdErrHandle")
    protected abstract static class PrimGetStdErrHandleNode extends AbstractPrimitiveNode implements Primitive0 {
        @Specialization
        protected final PointersObject doGet(@SuppressWarnings("unused") final Object receiver) {
            return FilePlugin.createStdioFileHandle(getContext(), STDIO_HANDLES.ERROR);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetStdInHandle")
    protected abstract static class PrimGetStdInHandleNode extends AbstractPrimitiveNode implements Primitive0 {
        @Specialization
        protected final PointersObject doGet(@SuppressWarnings("unused") final Object receiver) {
            return FilePlugin.createStdioFileHandle(getContext(), STDIO_HANDLES.IN);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetStdOutHandle")
    protected abstract static class PrimGetStdOutHandleNode extends AbstractPrimitiveNode implements Primitive0 {
        @Specialization
        protected final PointersObject doGet(@SuppressWarnings("unused") final Object receiver) {
            return FilePlugin.createStdioFileHandle(getContext(), STDIO_HANDLES.OUT);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetUid")
    protected abstract static class PrimGetUidNode extends AbstractSysCallPrimitiveNode implements Primitive0 {
        @Specialization
        protected final long doGetUid(@SuppressWarnings("unused") final Object receiver) {
            return getValue();
        }

        @Override
        protected final String getFunctionName() {
            return "getuid";
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveRealpath")
    protected abstract static class PrimRealpathNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "pathString.isByteType()")
        protected final NativeObject doRealpath(@SuppressWarnings("unused") final Object receiver, final NativeObject pathString,
                        @Bind final Node node,
                        @Cached final InlinedBranchProfile errorProfile) {
            final SqueakImageContext image = getContext();
            try {
                return image.asByteString(image.env.getPublicTruffleFile(pathString.asStringUnsafe()).getCanonicalFile().getPath());
            } catch (final IOException e) {
                errorProfile.enter(node);
                throw PrimitiveFailed.GENERIC_ERROR;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSendSigabrtTo")
    protected abstract static class PrimSendSigabrtToNode extends AbstractKillPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected final long doSendSigabrtTo(@SuppressWarnings("unused") final Object receiver, final long pid) {
            return setValue(pid, SIGNALS.SIGABRT);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSendSigalrmTo")
    protected abstract static class PrimSendSigalrmToNode extends AbstractKillPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected final long doSendSigalrmTo(@SuppressWarnings("unused") final Object receiver, final long pid) {
            return setValue(pid, SIGNALS.SIGALRM);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSendSigchldTo")
    protected abstract static class PrimSendSigchldToNode extends AbstractKillPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"isMacOS()"})
        protected final long doSendSigchldToMacOS(@SuppressWarnings("unused") final Object receiver, final long pid) {
            return setValue(pid, SIGNALS.SIGCHLD_MACOS);
        }

        @Specialization(guards = {"isLinux()"})
        protected final long doSendSigchldToUnix(@SuppressWarnings("unused") final Object receiver, final long pid) {
            return setValue(pid, SIGNALS.SIGCHLD_UNIX);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSendSigcontTo")
    protected abstract static class PrimSendSigcontToNode extends AbstractKillPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected final long doSendSigcontTo(@SuppressWarnings("unused") final Object receiver, final long pid) {
            return setValue(pid, SIGNALS.SIGCONT);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSendSighupTo")
    protected abstract static class PrimSendSighupToNode extends AbstractKillPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected final long doSendSighupTo(@SuppressWarnings("unused") final Object receiver, final long pid) {
            return setValue(pid, SIGNALS.SIGHUP);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSendSigintTo")
    protected abstract static class PrimSendSigintToNode extends AbstractKillPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected final long doSendSigintTo(@SuppressWarnings("unused") final Object receiver, final long pid) {
            return setValue(pid, SIGNALS.SIGINT);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSendSigkillTo")
    protected abstract static class PrimSendSigkillToNode extends AbstractKillPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected final long doSendSigkillTo(@SuppressWarnings("unused") final Object receiver, final long pid) {
            return setValue(pid, SIGNALS.SIGKILL);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSendSigpipeTo")
    protected abstract static class PrimSendSigpipeToNode extends AbstractKillPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected final long doSendSigpipeTo(@SuppressWarnings("unused") final Object receiver, final long pid) {
            return setValue(pid, SIGNALS.SIGPIPE);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSendSigquitTo")
    protected abstract static class PrimSendSigquitToNode extends AbstractKillPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected final long doSendSigquitTo(@SuppressWarnings("unused") final Object receiver, final long pid) {
            return setValue(pid, SIGNALS.SIGQUIT);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSendSigstopTo")
    protected abstract static class PrimSendSigstopToNode extends AbstractKillPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected final long doSendSigstopTo(@SuppressWarnings("unused") final Object receiver, final long pid) {
            return setValue(pid, SIGNALS.SIGSTOP);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSendSigtermTo")
    protected abstract static class PrimSendSigtermToNode extends AbstractKillPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected final long doSendSigtermTo(@SuppressWarnings("unused") final Object receiver, final long pid) {
            return setValue(pid, SIGNALS.SIGTERM);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSendSigusr1To")
    protected abstract static class PrimSendSigusr1ToNode extends AbstractKillPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"isMacOS()"})
        protected final long doSendSigusr1ToMacOS(@SuppressWarnings("unused") final Object receiver, final long pid) {
            return setValue(pid, SIGNALS.SIGUSR1_MACOS);
        }

        @Specialization(guards = {"isLinux()"})
        protected final long doSendSigusr1ToUnix(@SuppressWarnings("unused") final Object receiver, final long pid) {
            return setValue(pid, SIGNALS.SIGUSR1_UNIX);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSendSigusr2To")
    protected abstract static class PrimSendSigusr2ToNode extends AbstractKillPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"isMacOS()"})
        protected final long doSendSigusr2ToMacOS(@SuppressWarnings("unused") final Object receiver, final long pid) {
            return setValue(pid, SIGNALS.SIGUSR2_MACOS);
        }

        @Specialization(guards = {"isLinux()"})
        protected final long doSendSigusr2ToUnix(@SuppressWarnings("unused") final Object receiver, final long pid) {
            return setValue(pid, SIGNALS.SIGUSR2_UNIX);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSetPGid")
    protected abstract static class PrimSetPGidNode extends AbstractSysCallPrimitiveNode implements Primitive2WithFallback {
        @Specialization
        protected final long doSetPGid(@SuppressWarnings("unused") final Object receiver, final long pid, final long pgid) {
            return setValue(pid, pgid);
        }

        @Override
        protected final String getFunctionName() {
            return "setpgid";
        }

        @Override
        protected final FunctionDescriptor getFunctionSignature() {
            return FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSetPGrp")
    protected abstract static class PrimSetPGrpNode extends AbstractSysCallPrimitiveNode implements Primitive0 {
        @Specialization
        protected final long doSetPGid(@SuppressWarnings("unused") final Object receiver) {
            return setValue(0, 0);
        }

        @Override
        protected final String getFunctionName() {
            return "setpgid";
        }

        @Override
        protected final FunctionDescriptor getFunctionSignature() {
            return FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSetSid")
    protected abstract static class PrimSetSidNode extends AbstractSysCallPrimitiveNode implements Primitive0 {
        @Specialization
        protected final long doSetSid(@SuppressWarnings("unused") final Object receiver,
                        @Bind final Node node,
                        @Cached final InlinedBranchProfile errorProfile) {
            return failIfMinusOne(getValue(), errorProfile, node);
        }

        @Override
        protected final String getFunctionName() {
            return "setsid";
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSigChldNumber")
    protected abstract static class PrimSigChldNumberNode extends AbstractPrimitiveNode implements Primitive0 {
        @Specialization(guards = "isMacOS()")
        protected static final long doSigChldNumberMacOS(@SuppressWarnings("unused") final Object receiver) {
            return SIGNALS.SIGCHLD_MACOS;
        }

        @Specialization(guards = "isLinux()")
        protected static final long doSigChldNumberUnix(@SuppressWarnings("unused") final Object receiver) {
            return SIGNALS.SIGCHLD_UNIX;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSigHupNumber")
    protected abstract static class PrimSigHupNumberNode extends AbstractPrimitiveNode implements Primitive0 {
        @Specialization
        protected static final long doSigHupNumber(@SuppressWarnings("unused") final Object receiver) {
            return SIGNALS.SIGHUP;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSigIntNumber")
    protected abstract static class PrimSigIntNumberNode extends AbstractPrimitiveNode implements Primitive0 {
        @Specialization
        protected static final long doSigIntNumber(@SuppressWarnings("unused") final Object receiver) {
            return SIGNALS.SIGINT;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSigKillNumber")
    protected abstract static class PrimSigKillNumberNode extends AbstractPrimitiveNode implements Primitive0 {
        @Specialization
        protected static final long doSigKillNumber(@SuppressWarnings("unused") final Object receiver) {
            return SIGNALS.SIGKILL;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSigPipeNumber")
    protected abstract static class PrimSigPipeNumberNode extends AbstractPrimitiveNode implements Primitive0 {
        @Specialization
        protected static final long doSigPipeNumber(@SuppressWarnings("unused") final Object receiver) {
            return SIGNALS.SIGPIPE;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSigQuitNumber")
    protected abstract static class PrimSigQuitNumberNode extends AbstractPrimitiveNode implements Primitive0 {
        @Specialization
        protected static final long doSigQuitNumber(@SuppressWarnings("unused") final Object receiver) {
            return SIGNALS.SIGQUIT;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSigTermNumber")
    protected abstract static class PrimSigTermNumberNode extends AbstractPrimitiveNode implements Primitive0 {
        @Specialization
        protected static final long doSigTermNumber(@SuppressWarnings("unused") final Object receiver) {
            return SIGNALS.SIGTERM;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSigUsr1Number")
    protected abstract static class PrimSigUsr1NumberNode extends AbstractPrimitiveNode implements Primitive0 {
        @Specialization(guards = "isMacOS()")
        protected static final long doSigUsr1NumberMacOS(@SuppressWarnings("unused") final Object receiver) {
            return SIGNALS.SIGUSR1_MACOS;
        }

        @Specialization(guards = "isLinux()")
        protected static final long doSigUsr1NumberUnix(@SuppressWarnings("unused") final Object receiver) {
            return SIGNALS.SIGUSR1_UNIX;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSigUsr2Number")
    protected abstract static class PrimSigUsr2NumberNode extends AbstractPrimitiveNode implements Primitive0 {
        @Specialization(guards = "isLinux()")
        protected static final long doSigUsr2NumberLinux(@SuppressWarnings("unused") final Object receiver) {
            return SIGNALS.SIGUSR2_UNIX;
        }

        @Specialization(guards = "isMacOS()")
        protected static final long doSigUsr2NumberMacOS(@SuppressWarnings("unused") final Object receiver) {
            return SIGNALS.SIGUSR2_MACOS;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSizeOfPointer")
    protected abstract static class PrimSizeOfPointerNode extends AbstractPrimitiveNode implements Primitive0 {
        @Specialization
        protected static final long doSizeOfPointer(@SuppressWarnings("unused") final Object receiver) {
            return 8L;
        }
    }

    private static final class SIGNALS {
        private static final int SIG_DFL = 0;
        private static final int SIGHUP = 1;
        private static final int SIGINT = 2;
        private static final int SIGQUIT = 3;
        private static final int SIGABRT = 6;
        private static final int SIGKILL = 9;
        private static final int SIGUSR1_UNIX = 10;
        private static final int SIGUSR1_MACOS = 30;
        private static final int SIGUSR2_UNIX = 12;
        private static final int SIGUSR2_MACOS = 31;
        private static final int SIGPIPE = 13;
        private static final int SIGALRM = 14;
        private static final int SIGTERM = 15;
        private static final int SIGCHLD_UNIX = 17;
        private static final int SIGCHLD_MACOS = 20;
        private static final int SIGCONT = 18;
        private static final int SIGSTOP = 19;
    }

    private static String systemGetEnv(final Env env, final String key) {
        return Environment.get(env, key);
    }

    /**
     * The environment of a JVM cannot be modified, and changing it through libc is not thread-safe.
     * Changes made via {@code primitivePutEnv} and {@code primitiveUnsetEnv} are therefore only
     * recorded here and are visible to the primitives of this plugin, but not to the rest of the
     * process.
     */
    private static final class Environment {
        /* Maps keys to their new value, or to null if they were unset. */
        private static final Map<String, String> CHANGES = new HashMap<>();

        private static String toCString(final NativeObject bytes) {
            final String string = bytes.asStringUnsafe();
            final int terminatorIndex = string.indexOf('\0');
            return terminatorIndex >= 0 ? string.substring(0, terminatorIndex) : string;
        }

        @TruffleBoundary
        private static synchronized String get(final Env env, final String key) {
            return CHANGES.containsKey(key) ? CHANGES.get(key) : env.getEnvironment().get(key);
        }

        /* Answers false if the string is not of the form "KEY=value". */
        @TruffleBoundary
        private static synchronized boolean putKeyValue(final NativeObject keyValueString) {
            final String keyValue = toCString(keyValueString);
            final int separatorIndex = keyValue.indexOf('=');
            if (separatorIndex <= 0) {
                return false;
            }
            CHANGES.put(keyValue.substring(0, separatorIndex), keyValue.substring(separatorIndex + 1));
            return true;
        }

        /* Answers false if the string is not a valid key. */
        @TruffleBoundary
        private static synchronized boolean unset(final NativeObject keyString) {
            final String key = toCString(keyString);
            if (key.isEmpty() || key.indexOf('=') >= 0) {
                return false;
            }
            CHANGES.put(key, null);
            return true;
        }

        @TruffleBoundary
        private static synchronized List<String> entries(final Env env) {
            final List<String> entries = new ArrayList<>();
            for (final Map.Entry<String, String> entry : env.getEnvironment().entrySet()) {
                if (!CHANGES.containsKey(entry.getKey())) {
                    entries.add(entry.getKey() + "=" + entry.getValue());
                }
            }
            for (final Map.Entry<String, String> entry : CHANGES.entrySet()) {
                if (entry.getValue() != null) {
                    entries.add(entry.getKey() + "=" + entry.getValue());
                }
            }
            return entries;
        }
    }

    /**
     * Pipes are file handles in Squeak, which the FilePlugin represents by
     * {@link SeekableByteChannel}s. The two ends of a {@link Pipe} are therefore wrapped in channels
     * that are not seekable. As with {@code feof()}, the end of file is only reached after an
     * attempt to read from a pipe whose write end is closed.
     */
    private abstract static class AbstractPipeEndChannel implements SeekableByteChannel {
        protected long position;

        protected abstract void configureBlocking(boolean block) throws IOException;

        protected boolean isAtEndOfFile() {
            return false;
        }

        @Override
        public int read(final ByteBuffer dst) throws IOException {
            throw new IOException("Not readable");
        }

        @Override
        public int write(final ByteBuffer src) throws IOException {
            throw new IOException("Not writable");
        }

        @Override
        public final long position() {
            return position;
        }

        @Override
        public final SeekableByteChannel position(final long newPosition) throws IOException {
            throw new IOException("Not seekable");
        }

        @Override
        public long size() {
            return position;
        }

        @Override
        public final SeekableByteChannel truncate(final long size) throws IOException {
            throw new IOException("Not seekable");
        }
    }

    private static final class PipeReadEndChannel extends AbstractPipeEndChannel {
        private final Pipe.SourceChannel source;
        private boolean endOfFile;

        private PipeReadEndChannel(final Pipe.SourceChannel source) {
            this.source = source;
        }

        @Override
        protected void configureBlocking(final boolean block) throws IOException {
            source.configureBlocking(block);
        }

        @Override
        protected boolean isAtEndOfFile() {
            return endOfFile;
        }

        @Override
        public int read(final ByteBuffer dst) throws IOException {
            final int count = source.read(dst);
            if (count < 0) {
                endOfFile = true;
            } else {
                position += count;
            }
            return count;
        }

        @Override
        public long size() {
            /* The FilePlugin considers a file to be at its end if position >= size. */
            return endOfFile ? position : Long.MAX_VALUE;
        }

        @Override
        public boolean isOpen() {
            return source.isOpen();
        }

        @Override
        public void close() throws IOException {
            source.close();
        }
    }

    private static final class PipeWriteEndChannel extends AbstractPipeEndChannel {
        private final Pipe.SinkChannel sink;

        private PipeWriteEndChannel(final Pipe.SinkChannel sink) {
            this.sink = sink;
        }

        @Override
        protected void configureBlocking(final boolean block) throws IOException {
            sink.configureBlocking(block);
        }

        @Override
        public int write(final ByteBuffer src) throws IOException {
            final int count = sink.write(src);
            position += count;
            return count;
        }

        @Override
        public boolean isOpen() {
            return sink.isOpen();
        }

        @Override
        public void close() throws IOException {
            sink.close();
        }
    }

    /**
     * File locks are implemented with {@link FileLock}s. With the default file system, these are
     * advisory locks of the operating system and thus respected by other processes. POSIX locks never conflict with locks of the
     * same process, while a JVM refuses to lock a region that overlaps with one of its own locks.
     * Locks held through the same file handle are therefore released before an overlapping region
     * is locked. Unlike with POSIX, this also unlocks the parts of those locks that are outside of
     * the new region.
     */
    private static final class FileLocks {
        private static final Map<FileChannel, List<FileLock>> LOCKS = new WeakHashMap<>();

        private static long size(final long length) {
            return length == 0 ? Long.MAX_VALUE : length; // zero means "until the end of the file"
        }

        private static long end(final long position, final long size) {
            return position + size < 0 ? Long.MAX_VALUE : position + size;
        }

        private static List<FileLock> overlapping(final FileChannel channel, final long offset, final long length) {
            final List<FileLock> result = new ArrayList<>();
            final List<FileLock> locks = LOCKS.get(channel);
            if (locks != null) {
                locks.removeIf(lock -> !lock.isValid());
                for (final FileLock lock : locks) {
                    if (offset < end(lock.position(), lock.size()) && lock.position() < end(offset, size(length))) {
                        result.add(lock);
                    }
                }
            }
            return result;
        }

        private static void release(final FileChannel channel, final List<FileLock> locks) throws IOException {
            for (final FileLock lock : locks) {
                lock.close();
            }
            LOCKS.get(channel).removeAll(locks);
        }

        /* Answers whether the region could be locked. */
        @TruffleBoundary
        private static synchronized boolean lock(final FileChannel channel, final long offset, final long length, final boolean exclusive) {
            try {
                final List<FileLock> ownLocks = overlapping(channel, offset, length);
                if (!ownLocks.isEmpty()) {
                    release(channel, ownLocks);
                }
                final FileLock lock = channel.tryLock(offset, size(length), !exclusive);
                if (lock == null) {
                    return false;
                }
                LOCKS.computeIfAbsent(channel, _ -> new ArrayList<>()).add(lock);
                return true;
            } catch (final IOException | RuntimeException e) {
                /* For example, if the channel is closed or locked through another file handle. */
                return false;
            }
        }

        @TruffleBoundary
        private static synchronized boolean unlock(final FileChannel channel, final long offset, final long length) {
            if (!channel.isOpen()) {
                return false;
            }
            try {
                final List<FileLock> ownLocks = overlapping(channel, offset, length);
                if (!ownLocks.isEmpty()) {
                    release(channel, ownLocks);
                }
                return true;
            } catch (final IOException e) {
                return false;
            }
        }

        /* Answers whether the region is locked through this file handle or could be locked. */
        @TruffleBoundary
        private static synchronized boolean isLockable(final FileChannel channel, final long offset, final long length, final boolean exclusive) {
            if (!channel.isOpen()) {
                return false;
            }
            if (!overlapping(channel, offset, length).isEmpty()) {
                return true;
            }
            try {
                final FileLock lock = channel.tryLock(offset, size(length), !exclusive);
                if (lock == null) {
                    return false;
                }
                lock.close();
                return true;
            } catch (final IOException | RuntimeException e) {
                return false;
            }
        }
    }

    protected abstract static class AbstractFileHandlePrimitiveNode extends AbstractPrimitiveNode {
        protected static final boolean isLockableHandle(final PointersObject handle) {
            return handle.getHiddenObject() instanceof FileChannel;
        }

        protected static final FileChannel getFileChannel(final PointersObject handle) {
            return (FileChannel) handle.getHiddenObject();
        }

        protected static final boolean isStdioHandle(final PointersObject handle) {
            return handle.getHiddenObject() instanceof Byte;
        }

        protected static final boolean isPipeHandle(final PointersObject handle) {
            return handle.getHiddenObject() instanceof AbstractPipeEndChannel;
        }

        protected static final boolean isFileHandle(final PointersObject handle) {
            return handle.getHiddenObject() instanceof SeekableByteChannel;
        }

        @TruffleBoundary
        protected static final long configureBlocking(final PointersObject handle, final boolean block) {
            if (handle.getHiddenObject() instanceof final AbstractPipeEndChannel channel) {
                try {
                    channel.configureBlocking(block);
                } catch (final IOException e) {
                    throw PrimitiveFailed.GENERIC_ERROR;
                }
            }
            /* The blocking mode has no effect on regular files. */
            return 0L;
        }
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return Stream.concat(UnixOSProcessPluginFactory.getFactories().stream(), AbstractOSProcessPluginFactory.getFactories().stream()).toList();
    }
}
