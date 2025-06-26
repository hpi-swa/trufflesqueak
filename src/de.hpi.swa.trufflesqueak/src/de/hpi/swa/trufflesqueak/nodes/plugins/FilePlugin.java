/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.TruffleFile.AttributeDescriptor;
import com.oracle.truffle.api.TruffleFile.Attributes;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import com.oracle.truffle.api.strings.TruffleString;
import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.FloatObject;
import de.hpi.swa.trufflesqueak.model.LargeIntegerObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive0;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive2WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive3WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive4WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.util.LogUtils;
import de.hpi.swa.trufflesqueak.util.MiscUtils;
import de.hpi.swa.trufflesqueak.util.OS;
import de.hpi.swa.trufflesqueak.util.VarHandleUtils;

public final class FilePlugin extends AbstractPrimitiveFactoryHolder {
    private static final List<AttributeDescriptor<? extends Comparable<?>>> ENTRY_ATTRIBUTES = List.of(
                    TruffleFile.LAST_MODIFIED_TIME, TruffleFile.CREATION_TIME, TruffleFile.IS_DIRECTORY, TruffleFile.SIZE);
    private static final EnumSet<StandardOpenOption> OPTIONS_DEFAULT = EnumSet.of(StandardOpenOption.READ);
    private static final EnumSet<StandardOpenOption> OPTIONS_WRITEABLE = EnumSet.of(StandardOpenOption.WRITE, StandardOpenOption.READ, StandardOpenOption.CREATE);

    public static final class STDIO_HANDLES {
        public static final byte IN = 0;
        public static final byte OUT = 1;
        public static final byte ERROR = 2;
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return FilePluginFactory.getFactories();
    }

    protected abstract static class AbstractFilePluginPrimitiveNode extends AbstractPrimitiveNode {

        @TruffleBoundary(transferToInterpreterOnException = false)
        protected static final SeekableByteChannel getChannelOrPrimFail(final PointersObject handle) {
            final Object hiddenObject = getChannelOrNil(handle);
            if (hiddenObject instanceof final SeekableByteChannel o) {
                return o;
            } else {
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }

        protected static final Object getChannelOrNil(final PointersObject handle) {
            return handle.getHiddenObject();
        }

        protected final TruffleFile asPublicTruffleFile(final NativeObject obj) {
            return asPublicTruffleFile(obj.asStringUnsafe());
        }

        protected final TruffleFile asPublicTruffleFile(final String obj) {
            return getContext().env.getPublicTruffleFile(obj);
        }

        protected static final boolean inBounds(final long startIndex, final long count, final int slotSize) {
            return startIndex >= 1 && startIndex + count - 1 <= slotSize;
        }

        protected static final boolean isStdioFileDescriptor(final PointersObject fd) {
            return fd.getHiddenObject() instanceof Byte;
        }

        protected static final boolean isStdoutFileDescriptor(final PointersObject fd) {
            return isStdioFileDescriptor(fd) && (byte) fd.getHiddenObject() == STDIO_HANDLES.OUT;
        }

        protected static final boolean isStderrFileDescriptor(final PointersObject fd) {
            return isStdioFileDescriptor(fd) && (byte) fd.getHiddenObject() == STDIO_HANDLES.ERROR;
        }
    }

    protected static PointersObject createFileHandleOrPrimFail(final SqueakImageContext image, final TruffleFile truffleFile, final boolean writableFlag) {
        return PointersObject.newHandleWithHiddenObject(image, createChannelOrPrimFail(image, truffleFile, writableFlag));
    }

    public static PointersObject createStdioFileHandle(final SqueakImageContext image, final byte type) {
        return PointersObject.newHandleWithHiddenObject(image, type);
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    private static SeekableByteChannel createChannelOrPrimFail(final SqueakImageContext image, final TruffleFile truffleFile, final boolean writableFlag) {
        try {
            final SeekableByteChannel channel = truffleFile.newByteChannel(writableFlag ? OPTIONS_WRITEABLE : OPTIONS_DEFAULT);
            image.env.registerOnDispose(channel);
            return channel;
        } catch (IOException | UnsupportedOperationException | SecurityException e) {
            log("Failed to create SeekableByteChannel", e);
            throw PrimitiveFailed.GENERIC_ERROR;
        }
    }

    private static Object newFileEntry(final SqueakImageContext image, final TruffleFile file) {
        return newFileEntry(image, file, file.getName());
    }

    private static Object newFileEntry(final SqueakImageContext image, final TruffleFile file, final String fileName) {
        final Attributes attributes;
        try {
            attributes = file.getAttributes(ENTRY_ATTRIBUTES);
        } catch (final IOException e) {
            // TODO: make this better
            LogUtils.IO.warning(() -> "File must exist: " + file + " (" + e + "). Falling back to nil...");
            return NilObject.SINGLETON;
        }
        final Long creationTime = MiscUtils.toSqueakSecondsLocal(attributes.get(TruffleFile.CREATION_TIME).to(TimeUnit.SECONDS));
        final Long lastModifiedTime = MiscUtils.toSqueakSecondsLocal(attributes.get(TruffleFile.LAST_MODIFIED_TIME).to(TimeUnit.SECONDS));
        final Boolean isDirectory = attributes.get(TruffleFile.IS_DIRECTORY);
        final Long size = attributes.get(TruffleFile.SIZE);
        return image.asArrayOfObjects(image.asByteString(fileName), creationTime, lastModifiedTime, isDirectory, size);
    }

    private static void log(final String message, final Throwable e) {
        LogUtils.IO.log(Level.FINE, message, e);
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDirectoryCreate")
    protected abstract static class PrimDirectoryCreateNode extends AbstractFilePluginPrimitiveNode implements Primitive1WithFallback {

        @Specialization(guards = "fullPath.isTruffleStringType()")
        protected final Object doCreate(final Object receiver, final NativeObject fullPath) {
            try {
                asPublicTruffleFile(fullPath).createDirectory();
            } catch (IOException | UnsupportedOperationException | SecurityException e) {
                log("Failed to create directory", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDirectoryDelete")
    protected abstract static class PrimDirectoryDeleteNode extends AbstractFilePluginPrimitiveNode implements Primitive1WithFallback {

        @Specialization(guards = "fullPath.isTruffleStringType()")
        protected final Object doDelete(final Object receiver, final NativeObject fullPath) {
            try {
                asPublicTruffleFile(fullPath).delete();
            } catch (IOException | UnsupportedOperationException | SecurityException e) {
                log("Failed to delete directory", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDirectoryDelimitor")
    protected abstract static class PrimDirectoryDelimitorNode extends AbstractPrimitiveNode implements Primitive0 {

        @Specialization
        protected final char doDelimitor(@SuppressWarnings("unused") final Object receiver) {
            return getContext().env.getFileNameSeparator().charAt(0);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDirectoryEntry")
    protected abstract static class PrimDirectoryEntryNode extends AbstractFilePluginPrimitiveNode implements Primitive2WithFallback {

        @TruffleBoundary(transferToInterpreterOnException = false)
        @Specialization(guards = {"fullPath.isTruffleStringType()", "fName.isTruffleStringType()"})
        protected final Object doEntry(@SuppressWarnings("unused") final Object receiver, final NativeObject fullPath, final NativeObject fName) {
            final String pathName = fullPath.asStringUnsafe();
            final String fileName = fName.asStringUnsafe();
            final String path;
            if (".".equals(fileName)) {
                path = pathName;
            } else if (OS.isWindows() && pathName.isEmpty() && fileName.endsWith(":")) {
                path = fileName + "\\";
            } else {
                path = pathName + getContext().env.getFileNameSeparator() + fileName;
            }
            final TruffleFile file;
            try {
                file = asPublicTruffleFile(path);
            } catch (final InvalidPathException e) {
                return NilObject.SINGLETON;
            }
            if (file.exists()) {
                return newFileEntry(getContext(), file, fileName);
            } else {
                return NilObject.SINGLETON;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDirectoryLookup")
    protected abstract static class PrimDirectoryLookupNode extends AbstractFilePluginPrimitiveNode implements Primitive2WithFallback {

        @Specialization(guards = {"longIndex > 0", "nativePathName.isTruffleStringType()", "nativePathName.getTruffleStringByteLength() == 0"})
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doLookupEmptyString(@SuppressWarnings("unused") final Object receiver, @SuppressWarnings("unused") final NativeObject nativePathName, final long longIndex) {
            assert OS.isWindows() : "Unexpected empty path on a non-Windows system.";
            final ArrayList<TruffleFile> fileList = new ArrayList<>();
            // TODO: avoid to use Path and FileSystems here.
            for (final Path path : FileSystems.getDefault().getRootDirectories()) {
                fileList.add(getContext().env.getPublicTruffleFile(path.toUri()));
            }
            final int index = (int) longIndex - 1;
            if (index < fileList.size()) {
                final TruffleFile file = fileList.get(index);
                // Use getPath here, getName returns empty string on root path.
                // Squeak strips the trailing backslash from C:\ on Windows.
                return newFileEntry(getContext(), file, file.getPath().replace("\\", ""));
            } else {
                return NilObject.SINGLETON;
            }
        }

        @Specialization(guards = {"index > 0", "nativePathName.isTruffleStringType()", "nativePathName.getTruffleStringByteLength() > 0"})
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final Object doLookup(@SuppressWarnings("unused") final Object receiver, final NativeObject nativePathName, final long index) {
            String pathName = nativePathName.asStringUnsafe();
            if (OS.isWindows() && !pathName.endsWith("\\")) {
                pathName += "\\"; // new File("C:") will fail, we need to add a trailing backslash.
            }
            final TruffleFile directory = asPublicTruffleFile(pathName);
            if (!directory.isDirectory()) {
                throw PrimitiveFailed.GENERIC_ERROR;
            }
            long count = index;
            try (DirectoryStream<TruffleFile> stream = directory.newDirectoryStream()) {
                for (final TruffleFile file : stream) {
                    if (count-- <= 1 && file.exists()) {
                        return newFileEntry(getContext(), file);
                    }
                }
            } catch (final IOException e) {
                log("Failed to access directory", e);
                throw PrimitiveFailed.GENERIC_ERROR;
            }
            return NilObject.SINGLETON;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"index <= 0"})
        protected static final Object doNil(final Object receiver, final NativeObject nativePathName, final long index) {
            return NilObject.SINGLETON;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDirectoryGetMacTypeAndCreator")
    protected abstract static class PrimDirectoryGetMacTypeAndCreatorNode extends AbstractPrimitiveNode implements Primitive3WithFallback {
        @SuppressWarnings("unused")
        @Specialization
        protected static final Object doNothing(final Object receiver, final NativeObject fileName, final NativeObject typeString, final NativeObject creatorString) {
            /*
             * Get the Macintosh file type and creator info for the file with the given name. Fails
             * if the file does not exist or if the type and creator type arguments are not strings
             * of length 4. This primitive is Mac specific; it is a noop on other platforms.
             */
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDirectorySetMacTypeAndCreator")
    protected abstract static class PrimDirectorySetMacTypeAndCreatorNode extends AbstractPrimitiveNode implements Primitive3WithFallback {
        @SuppressWarnings("unused")
        @Specialization
        protected static final Object doNothing(final Object receiver, final NativeObject fileName, final NativeObject typeString, final NativeObject creatorString) {
            /*
             * Set the Macintosh file type and creator info for the file with the given name. Fails
             * if the file does not exist or if the type and creator type arguments are not strings
             * of length 4. Does nothing on other platforms (where the underlying primitive is a
             * noop).
             */
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFileAtEnd")
    protected abstract static class PrimFileAtEndNode extends AbstractFilePluginPrimitiveNode implements Primitive1WithFallback {

        @Specialization(guards = "!isStdioFileDescriptor(fd)")
        protected static final boolean doAtEnd(@SuppressWarnings("unused") final Object receiver, final PointersObject fd) {
            return BooleanObject.wrap(atEndOrPrimFail(getChannelOrPrimFail(fd)));
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private static boolean atEndOrPrimFail(final SeekableByteChannel channel) {
            try {
                return channel.position() >= channel.size();
            } catch (final IOException e) {
                log("Failed to check atEnd", e);
                throw PrimitiveFailed.GENERIC_ERROR;
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "isStdioFileDescriptor(fd)")
        protected static final Object doAtEndStdio(final Object receiver, final PointersObject fd) {
            throw PrimitiveFailed.GENERIC_ERROR;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFileClose")
    protected abstract static class PrimFileCloseNode extends AbstractFilePluginPrimitiveNode implements Primitive1WithFallback {

        @Specialization(guards = "!isStdioFileDescriptor(fd)")
        protected static final Object doClose(final Object receiver, final PointersObject fd) {
            final Object channelOrNil = getChannelOrNil(fd);
            if (channelOrNil != NilObject.SINGLETON) {
                closeFailsafe(channelOrNil);
            }
            return receiver;
        }

        @TruffleBoundary
        private static void closeFailsafe(final Object channel) {
            try {
                ((SeekableByteChannel) channel).close();
            } catch (final IOException e) {
                log("Failed to close file", e);
            }
        }

        @Specialization(guards = "isStdioFileDescriptor(fd)")
        protected static final Object doCloseStdio(final Object receiver, @SuppressWarnings("unused") final PointersObject fd) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFileDelete")
    protected abstract static class PrimFileDeleteNode extends AbstractFilePluginPrimitiveNode implements Primitive1WithFallback {

        @Specialization(guards = "nativeFileName.isTruffleStringType()")
        protected final Object doDelete(final Object receiver, final NativeObject nativeFileName) {
            try {
                asPublicTruffleFile(nativeFileName).delete();
            } catch (final IOException e) {
                log("Failed to delete file", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFileFlush")
    protected abstract static class PrimFileFlushNode extends AbstractFilePluginPrimitiveNode implements Primitive1WithFallback {

        @SuppressWarnings("unused")
        @Specialization(guards = {"isStdoutFileDescriptor(fd)"})
        protected final Object doFlushStdout(final Object receiver, final PointersObject fd) {
            flushStdioOrFail(getContext().env.out());
            return receiver;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isStderrFileDescriptor(fd)"})
        protected final Object doFlushStderr(final Object receiver, final PointersObject fd) {
            flushStdioOrFail(getContext().env.err());
            return receiver;
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private static void flushStdioOrFail(final OutputStream outputStream) {
            try {
                outputStream.flush();
            } catch (final IOException e) {
                log("Failed to flush OutputStream", e);
                throw PrimitiveFailed.GENERIC_ERROR;
            }
        }

        @Specialization(guards = "!isStdioFileDescriptor(fd)")
        protected static final Object doFlush(final Object receiver, @SuppressWarnings("unused") final PointersObject fd) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFileGetPosition")
    protected abstract static class PrimFileGetPositionNode extends AbstractFilePluginPrimitiveNode implements Primitive1WithFallback {

        @Specialization(guards = "!isStdioFileDescriptor(fd)")
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected static final long doGet(@SuppressWarnings("unused") final Object receiver, final PointersObject fd) {
            try {
                return getChannelOrPrimFail(fd).position();
            } catch (final IOException e) {
                log("Failed to get file position", e);
                throw PrimitiveFailed.GENERIC_ERROR;
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "isStdioFileDescriptor(fd)")
        protected static final long doStdioGet(final Object receiver, final PointersObject fd) {
            return 0L;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFileOpen")
    protected abstract static class PrimFileOpenNode extends AbstractFilePluginPrimitiveNode implements Primitive2WithFallback {

        @Specialization(guards = "nativeFileName.isTruffleStringType() || nativeFileName.isTruffleStringType()")
        protected final Object doOpen(@SuppressWarnings("unused") final Object receiver, final NativeObject nativeFileName, final boolean writableFlag) {
            return createFileHandleOrPrimFail(getContext(), asPublicTruffleFile(nativeFileName), writableFlag);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFileRead")
    protected abstract static class PrimFileReadNode extends AbstractFilePluginPrimitiveNode implements Primitive4WithFallback {

        @Specialization(guards = {"!isStdioFileDescriptor(fd)", "target.isTruffleStringType()", "inBounds(startIndex, count, target.getTruffleStringByteLength())"})
        protected static final long doReadTruffleString(@SuppressWarnings("unused") final Object receiver, final PointersObject fd, final NativeObject target, final long startIndex, final long count, @Cached TruffleString.GetInternalByteArrayNode internalByteArrayNode) {
            final long read = readFrom(getChannelOrPrimFail(fd), target.getTruffleStringAsReadonlyBytes(internalByteArrayNode), (int) startIndex - 1, (int) count);
            return Math.max(read, 0L); // `read` can be `-1`, Squeak expects zero.
        }

        @Specialization(guards = {"!isStdioFileDescriptor(fd)", "target.isTruffleStringType()", "inBounds(startIndex, count, target.getByteLength())"})
        protected static final long doReadBytes(@SuppressWarnings("unused") final Object receiver, final PointersObject fd, final NativeObject target, final long startIndex, final long count) {
            final long read = readFrom(getChannelOrPrimFail(fd), target.getByteStorage(), (int) startIndex - 1, (int) count);
            return Math.max(read, 0L); // `read` can be `-1`, Squeak expects zero.
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private static long readFrom(final SeekableByteChannel channel, final byte[] bytes, final int startIndex, final int count) {
            try {
                return channel.read(ByteBuffer.wrap(bytes, startIndex, count));
            } catch (final IOException e) {
                log("Failed to read from channel", e);
                throw PrimitiveFailed.GENERIC_ERROR;
            }
        }

        @Specialization(guards = {"!isStdioFileDescriptor(fd)", "target.isIntType()", "inBounds(startIndex, count, target.getIntLength())"})
        protected static final long doReadInts(@SuppressWarnings("unused") final Object receiver, final PointersObject fd, final NativeObject target, final long startIndex, final long count) {
            final ByteBuffer dst = allocate((int) count * Integer.BYTES);
            final long readBytes = readFrom(getChannelOrPrimFail(fd), dst);
            final byte[] bytes = getBytes(dst);
            assert readBytes % Integer.BYTES == 0 && readBytes == bytes.length;
            final long readInts = readBytes / Integer.BYTES;
            // TODO: could use UnsafeUtils.copyMemory here?
            for (int index = 0; index < readInts; index++) {
                target.setInt(startIndex - 1 + index, VarHandleUtils.getInt(bytes, index));
            }
            return readInts;
        }

        @TruffleBoundary
        private static ByteBuffer allocate(final int count) {
            return ByteBuffer.allocate(count);
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private static int readFrom(final SeekableByteChannel channel, final ByteBuffer dst) {
            try {
                return channel.read(dst);
            } catch (final IOException e) {
                log("Failed to read from channel", e);
                throw PrimitiveFailed.GENERIC_ERROR;
            }
        }

        @TruffleBoundary
        private static byte[] getBytes(final ByteBuffer dst) {
            return dst.array();
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isStdioFileDescriptor(fd)"})
        protected static final Object doReadStdio(final Object receiver, final PointersObject fd, final NativeObject target, final long startIndex, final long longCount) {
            throw PrimitiveFailed.GENERIC_ERROR;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFileRename")
    protected abstract static class PrimFileRenameNode extends AbstractFilePluginPrimitiveNode implements Primitive2WithFallback {

        @Specialization(guards = {"oldName.isByteType() || oldName.isTruffleStringType()", "newName.isByteType() || newName.isTruffleStringType()"})
        protected final Object doRename(final Object receiver, final NativeObject oldName, final NativeObject newName) {
            try {
                asPublicTruffleFile(oldName).move(asPublicTruffleFile(newName));
            } catch (final IOException e) {
                log("Failed to move file", e);
                throw PrimitiveFailed.andTransferToInterpreter();
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFileSetPosition")
    protected abstract static class PrimFileSetPositionNode extends AbstractFilePluginPrimitiveNode implements Primitive2WithFallback {

        @Specialization(guards = "!isStdioFileDescriptor(fd)")
        protected static final Object doSet(final Object receiver, final PointersObject fd, final long position) {
            setPosition(getChannelOrPrimFail(fd), position);
            return receiver;
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private static void setPosition(final SeekableByteChannel channel, final long position) {
            try {
                channel.position(position);
            } catch (IllegalArgumentException | IOException e) {
                log("Failed to set position", e);
                throw PrimitiveFailed.GENERIC_ERROR;
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "isStdioFileDescriptor(fd)")
        protected static final Object doSetStdio(final Object receiver, final PointersObject fd, final long position) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFileSize")
    protected abstract static class PrimFileSizeNode extends AbstractFilePluginPrimitiveNode implements Primitive1WithFallback {

        @Specialization(guards = "!isStdioFileDescriptor(fd)")
        protected static final long doSize(@SuppressWarnings("unused") final Object receiver, final PointersObject fd) {
            return getSize(getChannelOrPrimFail(fd));
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private static long getSize(final SeekableByteChannel channel) {
            try {
                return channel.size();
            } catch (final IOException e) {
                log("Failed to get file size", e);
                throw PrimitiveFailed.GENERIC_ERROR;
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "isStdioFileDescriptor(fd)")
        protected static final long doSizeStdio(final Object receiver, final PointersObject fd) {
            return 0L;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFileStdioHandles")
    protected abstract static class PrimFileStdioHandlesNode extends AbstractFilePluginPrimitiveNode implements Primitive0 {
        @Specialization
        protected final Object getHandles(@SuppressWarnings("unused") final Object receiver) {
            final SqueakImageContext image = getContext();
            return image.asArrayOfObjects(createStdioFileHandle(image, STDIO_HANDLES.IN),
                            createStdioFileHandle(image, STDIO_HANDLES.OUT),
                            createStdioFileHandle(image, STDIO_HANDLES.ERROR));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFileTruncate")
    protected abstract static class PrimFileTruncateNode extends AbstractFilePluginPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = "!isStdioFileDescriptor(fd)")
        protected static final Object doTruncate(final Object receiver, final PointersObject fd, final long to) {
            truncate(getChannelOrPrimFail(fd), to);
            return receiver;
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private static void truncate(final SeekableByteChannel channel, final long to) {
            try {
                channel.truncate(to);
            } catch (IllegalArgumentException | IOException e) {
                log("Failed to truncate file", e);
                throw PrimitiveFailed.GENERIC_ERROR;
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "isStdioFileDescriptor(fd)")
        protected static final Object doTruncateStdio(final Object receiver, final PointersObject fd, final long to) {
            throw PrimitiveFailed.GENERIC_ERROR;
        }
    }

    @ImportStatic({FloatObject.class})
    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFileWrite")
    protected abstract static class PrimFileWriteNode extends AbstractFilePluginPrimitiveNode implements Primitive4WithFallback {

        @Specialization(guards = {"!isStdioFileDescriptor(fd)", "content.isTruffleStringType()", "inBounds(startIndex, count, content.getByteLength())"})
        protected static final long doWriteByte(@SuppressWarnings("unused") final Object receiver, final PointersObject fd, final NativeObject content, final long startIndex, final long count) {
            return fileWriteFromAt(fd, count, content.getByteStorage(), startIndex, 1);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isStdoutFileDescriptor(fd)", "content.isTruffleStringType()", "inBounds(startIndex, count, content.getByteLength())"})
        protected final long doWriteByteToStdout(final Object receiver, final PointersObject fd, final NativeObject content, final long startIndex, final long count) {
            writeToOutputStream(getContext().env.out(), content.getByteStorage(), (int) (startIndex - 1), (int) count);
            return count;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isStderrFileDescriptor(fd)", "content.isTruffleStringType()", "inBounds(startIndex, count, content.getByteLength())"})
        protected final long doWriteByteToStderr(final Object receiver, final PointersObject fd, final NativeObject content, final long startIndex, final long count) {
            writeToOutputStream(getContext().env.err(), content.getByteStorage(), (int) (startIndex - 1), (int) count);
            return count;
        }

        @Specialization(guards = {"!isStdioFileDescriptor(fd)", "content.isIntType()", "inBounds(startIndex, count, content.getIntLength())"})
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected static final long doWriteInt(@SuppressWarnings("unused") final Object receiver, final PointersObject fd, final NativeObject content, final long startIndex, final long count) {
            // TODO: use ByteBuffer or UnsafeUtils here?
            final int[] ints = content.getIntStorage();
            final int intsLength = ints.length;
            final byte[] bytes = new byte[intsLength * Integer.BYTES];
            for (int i = 0; i < intsLength; i++) {
                VarHandleUtils.putIntReversed(bytes, i, ints[i]);
            }
            return fileWriteFromAt(fd, count, bytes, startIndex, 4);
        }

        @Specialization(guards = {"!isStdioFileDescriptor(fd)", "inBounds(startIndex, count, content.size())"})
        protected static final long doWriteLargeInteger(@SuppressWarnings("unused") final Object receiver, final PointersObject fd, final LargeIntegerObject content, final long startIndex,
                        final long count) {
            return fileWriteFromAt(fd, count, content.getBytes(), startIndex, 1);
        }

        @Specialization(guards = {"!isStdioFileDescriptor(fd)", "inBounds(startIndex, count, WORD_LENGTH)"})
        protected static final long doWriteDouble(@SuppressWarnings("unused") final Object receiver, final PointersObject fd, final double content, final long startIndex, final long count) {
            return fileWriteFromAt(fd, count, FloatObject.getBytes(content), startIndex, 8);
        }

        @Specialization(guards = {"!isStdioFileDescriptor(fd)", "inBounds(startIndex, count, WORD_LENGTH)"})
        protected static final long doWriteFloatObject(@SuppressWarnings("unused") final Object receiver, final PointersObject fd, final FloatObject content, final long startIndex, final long count) {
            return fileWriteFromAt(fd, count, content.getBytes(), startIndex, 8);
        }

        @Specialization(guards = {"!isStdioFileDescriptor(fd)", "content.isTruffleStringType()", "inBounds(startIndex, count, content.getTruffleStringByteLength())"})
        protected static long doWriteByteString(@SuppressWarnings("unused") final Object receiver, final PointersObject fd, final NativeObject content, final long startIndex, final long count, @Cached.Shared("truffleString") @Cached TruffleString.CopyToByteArrayNode copyToByteArrayNode) {
            final byte[] bytes = content.getTruffleStringAsBytesCopy(copyToByteArrayNode);
            return fileWriteFromAt(fd, count, bytes, startIndex, 1);
        }

        @Specialization(guards = {"isStdoutFileDescriptor(fd)", "content.isTruffleStringType()", "inBounds(startIndex, count, content.getTruffleStringByteLength())"})
        protected final long doWriteTruffleStringToStdout(final Object receiver, final PointersObject fd, final NativeObject content, final long startIndex, final long count, @Cached.Shared("truffleString") @Cached TruffleString.CopyToByteArrayNode copyToByteArrayNode) {
            writeToOutputStream(getContext().env.out(), content.getTruffleStringAsBytesCopy(copyToByteArrayNode), (int) (startIndex - 1), (int) count);
            return count;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isStderrFileDescriptor(fd)", "content.isTruffleStringType()", "inBounds(startIndex, count, content.getTruffleStringByteLength())"})
        protected final long doWriteTruffleStringToStderr(final Object receiver, final PointersObject fd, final NativeObject content, final long startIndex, final long count, @Cached.Shared("truffleString") @Cached TruffleString.CopyToByteArrayNode copyToByteArrayNode) {
            writeToOutputStream(getContext().env.err(), content.getTruffleStringAsBytesCopy(copyToByteArrayNode), (int) (startIndex - 1), (int) count);
            return count;
        }

        private static long fileWriteFromAt(final PointersObject fd, final long count, final byte[] bytes, final long startIndex, final int elementSize) {
            final int offset = (int) (startIndex - 1) * elementSize;
            final int length = (int) count * elementSize;
            final int written = fileWriteFromAtInternal(getChannelOrPrimFail(fd), bytes, offset, length);
            return written / elementSize;
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private static int fileWriteFromAtInternal(final SeekableByteChannel channel, final byte[] bytes, final int offset, final int length) {
            try {
                return channel.write(ByteBuffer.wrap(bytes, offset, length));
            } catch (final IOException e) {
                log("Failed to write to file", e);
                throw PrimitiveFailed.GENERIC_ERROR;
            }
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private static void writeToOutputStream(final OutputStream outputStream, final byte[] content, final int offset, final int length) {
            try {
                outputStream.write(content, offset, length);
                outputStream.flush();
            } catch (final IOException e) {
                log("Failed to write to OutputStream", e);
                throw PrimitiveFailed.GENERIC_ERROR;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveHasFileAccess")
    protected abstract static class PrimHasFileAccessNode extends AbstractPrimitiveNode implements Primitive0 {
        @Specialization
        protected final boolean hasFileAccess(@SuppressWarnings("unused") final Object receiver) {
            return BooleanObject.wrap(getContext().env.isFileIOAllowed());
        }
    }
}
