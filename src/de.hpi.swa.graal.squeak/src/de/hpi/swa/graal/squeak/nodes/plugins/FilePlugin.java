/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.plugins;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.BranchProfile;

import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.BooleanObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.QuaternaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.QuinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitiveWithoutFallback;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.graal.squeak.shared.SqueakLanguageConfig;
import de.hpi.swa.graal.squeak.util.MiscUtils;
import de.hpi.swa.graal.squeak.util.OSDetector;
import de.hpi.swa.graal.squeak.util.UnsafeUtils;

public final class FilePlugin extends AbstractPrimitiveFactoryHolder {
    private static final TruffleLogger LOG = TruffleLogger.getLogger(SqueakLanguageConfig.ID, FilePlugin.class);

    public static final class STDIO_HANDLES {
        public static final long IN = 0;
        public static final long OUT = 1;
        public static final long ERROR = 2;
        @CompilationFinal(dimensions = 1) public static final long[] ALL = new long[]{STDIO_HANDLES.IN, STDIO_HANDLES.OUT, STDIO_HANDLES.ERROR};
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return FilePluginFactory.getFactories();
    }

    protected abstract static class AbstractFilePluginPrimitiveNode extends AbstractPrimitiveNode {

        @TruffleBoundary(transferToInterpreterOnException = false)
        protected static final SeekableByteChannel getFileOrPrimFail(final SqueakImageContext image, final long fileDescriptor) {
            assert !isStdioFileDescriptor(fileDescriptor);
            final SeekableByteChannel handle = image.filePluginHandles.get(fileDescriptor);
            if (handle == null) {
                throw PrimitiveFailed.GENERIC_ERROR;
            }
            return handle;
        }

        protected static final TruffleFile asPublicTruffleFile(final SqueakImageContext image, final NativeObject obj) {
            return asPublicTruffleFile(image, obj.asStringUnsafe());
        }

        protected static final TruffleFile asPublicTruffleFile(final SqueakImageContext image, final String obj) {
            return image.env.getPublicTruffleFile(obj);
        }

        protected static final boolean inBounds(final long startIndex, final long count, final int slotSize) {
            return startIndex >= 1 && startIndex + count - 1 <= slotSize;
        }

        protected static final boolean isStdioFileDescriptor(final long fileDescriptor) {
            return fileDescriptor == STDIO_HANDLES.IN || fileDescriptor == STDIO_HANDLES.OUT || fileDescriptor == STDIO_HANDLES.ERROR;
        }
    }

    @TruffleBoundary(transferToInterpreterOnException = false)
    protected static Object createFileHandleOrPrimFail(final SqueakImageContext image, final TruffleFile truffleFile, final Boolean writableFlag) {
        try {
            final EnumSet<StandardOpenOption> options;
            if (writableFlag) {
                options = EnumSet.<StandardOpenOption> of(StandardOpenOption.WRITE, StandardOpenOption.READ, StandardOpenOption.CREATE);
            } else {
                options = EnumSet.<StandardOpenOption> of(StandardOpenOption.READ);
            }
            final SeekableByteChannel file = truffleFile.newByteChannel(options);
            final long fileId = file.hashCode();
            image.filePluginHandles.put(fileId, file);
            LOG.fine(() -> "File Handle Creation SUCCEEDED: " + truffleFile.getPath() + " (fileID: " + fileId + ", " + ", writable: " + writableFlag + ")");
            return fileId;
        } catch (IOException | UnsupportedOperationException | SecurityException e) {
            LOG.fine(() -> "File Handle Creation FAILED: " + truffleFile.getPath() + " (writable: " + writableFlag + ")");
            throw PrimitiveFailed.GENERIC_ERROR;
        }
    }

    private static Object newFileEntry(final SqueakImageContext image, final TruffleFile file) {
        return newFileEntry(image, file, file.getName());
    }

    private static Object newFileEntry(final SqueakImageContext image, final TruffleFile file, final String fileName) {
        try {
            final long lastModifiedSeconds = MiscUtils.toSqueakSecondsLocal(file.getLastModifiedTime().to(TimeUnit.SECONDS));
            return image.asArrayOfObjects(image.asByteString(fileName), lastModifiedSeconds, lastModifiedSeconds,
                            BooleanObject.wrap(file.isDirectory()), file.size());
        } catch (final IOException e) {
            // TODO: make this better
            image.printToStdErr("File must exist: " + file + " (" + e + "). Falling back to nil...");
            return NilObject.SINGLETON;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDirectoryCreate")
    protected abstract static class PrimDirectoryCreateNode extends AbstractFilePluginPrimitiveNode implements BinaryPrimitive {

        @Specialization(guards = "fullPath.isByteType()")
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected static final Object doCreate(final Object receiver, final NativeObject fullPath,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            try {
                asPublicTruffleFile(image, fullPath).createDirectory();
                return receiver;
            } catch (IOException | UnsupportedOperationException | SecurityException e) {
                throw PrimitiveFailed.GENERIC_ERROR;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDirectoryDelete")
    protected abstract static class PrimDirectoryDeleteNode extends AbstractFilePluginPrimitiveNode implements BinaryPrimitive {

        @Specialization(guards = "fullPath.isByteType()")
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected static final Object doDelete(final Object receiver, final NativeObject fullPath,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            try {
                asPublicTruffleFile(image, fullPath).delete();
                return receiver;
            } catch (IOException | UnsupportedOperationException | SecurityException e) {
                throw PrimitiveFailed.GENERIC_ERROR;
            }
        }
    }

    @GenerateNodeFactory
    @NodeInfo(cost = NodeCost.NONE)
    @SqueakPrimitive(names = "primitiveDirectoryDelimitor")
    protected abstract static class PrimDirectoryDelimitorNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {

        @Specialization
        protected static final char doDelimitor(@SuppressWarnings("unused") final Object receiver,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return image.env.getFileNameSeparator().charAt(0);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDirectoryEntry")
    protected abstract static class PrimDirectoryEntryNode extends AbstractFilePluginPrimitiveNode implements TernaryPrimitive {

        @Specialization(guards = {"fullPath.isByteType()", "fName.isByteType()"})
        protected static final Object doEntry(@SuppressWarnings("unused") final Object receiver, final NativeObject fullPath, final NativeObject fName,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            final String pathName = fullPath.asStringUnsafe();
            final String fileName = fName.asStringUnsafe();
            final TruffleFile file;
            if (".".equals(fileName)) {
                file = asPublicTruffleFile(image, pathName);
            } else if (OSDetector.SINGLETON.isWindows() && pathName.isEmpty() && fileName.endsWith(":")) {
                file = asPublicTruffleFile(image, fileName + "\\");
            } else {
                file = asPublicTruffleFile(image, pathName + image.env.getFileNameSeparator() + fileName);
            }
            if (file.exists()) {
                return newFileEntry(image, file, fileName);
            } else {
                return NilObject.SINGLETON;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDirectoryLookup")
    protected abstract static class PrimDirectoryLookupNode extends AbstractFilePluginPrimitiveNode implements TernaryPrimitive {

        @Specialization(guards = {"longIndex > 0", "nativePathName.isByteType()", "nativePathName.getByteLength() == 0"})
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected static final Object doLookupEmptyString(@SuppressWarnings("unused") final Object receiver, @SuppressWarnings("unused") final NativeObject nativePathName, final long longIndex,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            assert OSDetector.SINGLETON.isWindows() : "Unexpected empty path on a non-Windows system.";
            final ArrayList<TruffleFile> fileList = new ArrayList<>();
            // TODO: avoid to use Path and FileSystems here.
            for (final Path path : FileSystems.getDefault().getRootDirectories()) {
                fileList.add(image.env.getPublicTruffleFile(path.toUri()));
            }
            final int index = (int) longIndex - 1;
            if (index < fileList.size()) {
                final TruffleFile file = fileList.get(index);
                // Use getPath here, getName returns empty string on root path.
                // Squeak strips the trailing backslash from C:\ on Windows.
                return newFileEntry(image, file, file.getPath().replace("\\", ""));
            } else {
                return NilObject.SINGLETON;
            }
        }

        @Specialization(guards = {"longIndex > 0", "nativePathName.isByteType()", "nativePathName.getByteLength() > 0"})
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected static final Object doLookup(@SuppressWarnings("unused") final Object receiver, final NativeObject nativePathName, final long longIndex,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            String pathName = nativePathName.asStringUnsafe();
            if (OSDetector.SINGLETON.isWindows() && !pathName.endsWith("\\")) {
                pathName += "\\"; // new File("C:") will fail, we need to add a trailing backslash.
            }
            final TruffleFile directory = asPublicTruffleFile(image, pathName);
            if (!directory.isDirectory()) {
                PrimitiveFailed.andTransferToInterpreter();
            }
            final Collection<TruffleFile> files;
            try {
                files = directory.list();
            } catch (final IOException e) {
                throw SqueakException.create("A directory that exists must be listable", e);
            }
            final int index = (int) longIndex - 1;
            int currentIndex = 0;
            for (final Iterator<TruffleFile> iterator = files.stream().filter(file -> file.isReadable()).iterator(); iterator.hasNext();) {
                final TruffleFile file = iterator.next();
                if (index == currentIndex++) {
                    return newFileEntry(image, file);
                }
            }
            return NilObject.SINGLETON;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"longIndex <= 0"})
        protected static final Object doNil(final Object receiver, final NativeObject nativePathName, final long longIndex) {
            return NilObject.SINGLETON;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDirectoryGetMacTypeAndCreator")
    protected abstract static class PrimDirectoryGetMacTypeAndCreatorNode extends AbstractPrimitiveNode implements QuaternaryPrimitive {
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
    protected abstract static class PrimDirectorySetMacTypeAndCreatorNode extends AbstractPrimitiveNode implements QuaternaryPrimitive {
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
    protected abstract static class PrimFileAtEndNode extends AbstractFilePluginPrimitiveNode implements BinaryPrimitive {

        @Specialization(guards = "!isStdioFileDescriptor(fileDescriptor)")
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected static final Object doAtEnd(@SuppressWarnings("unused") final Object receiver, final long fileDescriptor,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            try {
                final SeekableByteChannel file = getFileOrPrimFail(image, fileDescriptor);
                return BooleanObject.wrap(file.position() >= file.size());
            } catch (final IOException e) {
                throw PrimitiveFailed.GENERIC_ERROR;
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "isStdioFileDescriptor(fileDescriptor)")
        protected static final Object doAtEndStdio(final Object receiver, final long fileDescriptor) {
            throw PrimitiveFailed.GENERIC_ERROR;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFileClose")
    protected abstract static class PrimFileCloseNode extends AbstractFilePluginPrimitiveNode implements BinaryPrimitive {

        @Specialization(guards = "!isStdioFileDescriptor(fileDescriptor)")
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected static final Object doClose(final Object receiver, final long fileDescriptor,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            try {
                getFileOrPrimFail(image, fileDescriptor).close();
                LOG.fine(() -> "File Closed SUCCEEDED: " + fileDescriptor);
            } catch (final IOException e) {
                LOG.fine(() -> "File Closed FAILED: " + fileDescriptor);
                throw PrimitiveFailed.GENERIC_ERROR;
            }
            return receiver;
        }

        @Specialization(guards = "isStdioFileDescriptor(fileDescriptor)")
        protected static final Object doCloseStdio(final Object receiver, @SuppressWarnings("unused") final long fileDescriptor) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFileDelete")
    protected abstract static class PrimFileDeleteNode extends AbstractFilePluginPrimitiveNode implements BinaryPrimitive {

        @Specialization(guards = "nativeFileName.isByteType()")
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected static final Object doDelete(final Object receiver, final NativeObject nativeFileName,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            final TruffleFile file = asPublicTruffleFile(image, nativeFileName);
            try {
                file.delete();
                return receiver;
            } catch (final IOException e) {
                throw PrimitiveFailed.GENERIC_ERROR;
            }
        }
    }

    @GenerateNodeFactory
    @ImportStatic(STDIO_HANDLES.class)
    @SqueakPrimitive(names = "primitiveFileFlush")
    protected abstract static class PrimFileFlushNode extends AbstractFilePluginPrimitiveNode implements BinaryPrimitive {

        @TruffleBoundary
        @SuppressWarnings("unused")
        @Specialization(guards = {"fileDescriptor == OUT"})
        protected static final Object doFlushStdout(final Object receiver, final long fileDescriptor,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            try {
                image.env.out().flush();
            } catch (final IOException e) {
                PrimitiveFailed.andTransferToInterpreter();
            }
            return receiver;
        }

        @TruffleBoundary
        @SuppressWarnings("unused")
        @Specialization(guards = {"fileDescriptor == ERROR"})
        protected static final Object doFlushStderr(final Object receiver, final long fileDescriptor,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            try {
                image.env.err().flush();
            } catch (final IOException e) {
                PrimitiveFailed.andTransferToInterpreter();
            }
            return receiver;
        }

        @Specialization(guards = "!isStdioFileDescriptor(fileDescriptor)")
        protected static final Object doFlush(final Object receiver, @SuppressWarnings("unused") final long fileDescriptor) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFileGetPosition")
    protected abstract static class PrimFileGetPositionNode extends AbstractFilePluginPrimitiveNode implements BinaryPrimitive {

        @Specialization(guards = "!isStdioFileDescriptor(fileDescriptor)")
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected static final long doGet(@SuppressWarnings("unused") final Object receiver, final long fileDescriptor,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            try {
                return getFileOrPrimFail(image, fileDescriptor).position();
            } catch (final IOException e) {
                throw PrimitiveFailed.GENERIC_ERROR;
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "isStdioFileDescriptor(fileDescriptor)")
        protected static final long doStdioGet(final Object receiver, final long fileDescriptor) {
            return 0L;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFileOpen")
    protected abstract static class PrimFileOpenNode extends AbstractFilePluginPrimitiveNode implements TernaryPrimitive {

        @Specialization(guards = "nativeFileName.isByteType()")
        protected static final Object doOpen(@SuppressWarnings("unused") final Object receiver, final NativeObject nativeFileName, final Boolean writableFlag,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return createFileHandleOrPrimFail(image, asPublicTruffleFile(image, nativeFileName), writableFlag);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFileRead")
    protected abstract static class PrimFileReadNode extends AbstractFilePluginPrimitiveNode implements QuinaryPrimitive {

        @Specialization(guards = {"!isStdioFileDescriptor(fileDescriptor)", "target.isByteType()", "inBounds(startIndex, longCount, target.getByteLength())"})
        protected static final Object doReadBytes(@SuppressWarnings("unused") final Object receiver, final long fileDescriptor, final NativeObject target,
                        final long startIndex, final long longCount,
                        @Exclusive @Cached final BranchProfile errorProfile,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            final int count = (int) longCount;
            final ByteBuffer dst = allocate(count);
            try {
                final long read = readFrom(image, fileDescriptor, dst);
                for (int index = 0; index < read; index++) {
                    target.setByte(startIndex - 1 + index, getFrom(dst, index));
                }
                return Math.max(read, 0L); // `read` can be `-1`, Squeak expects zero.
            } catch (final IOException e) {
                errorProfile.enter();
                throw PrimitiveFailed.GENERIC_ERROR;
            }
        }

        @Specialization(guards = {"!isStdioFileDescriptor(fileDescriptor)", "target.isIntType()", "inBounds(startIndex, longCount, target.getIntLength())"})
        protected static final Object doReadInts(@SuppressWarnings("unused") final Object receiver, final long fileDescriptor, final NativeObject target,
                        final long startIndex, final long longCount,
                        @Exclusive @Cached final BranchProfile errorProfile,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            final int count = (int) longCount;
            final ByteBuffer dst = allocate(count * Integer.BYTES);
            try {
                final long readBytes = readFrom(image, fileDescriptor, dst);
                assert readBytes % Integer.BYTES == 0;
                final long readInts = readBytes / Integer.BYTES;
                for (int index = 0; index < readInts; index++) {
                    final int offset = index * Integer.BYTES;
                    target.getIntStorage()[(int) (startIndex - 1 + index)] = getFromUnsigned(dst, offset + 3) << 24 |
                                    getFromUnsigned(dst, offset + 2) << 16 |
                                    getFromUnsigned(dst, offset + 1) << 8 |
                                    getFromUnsigned(dst, offset);
                }
                return Math.max(readInts, 0L); // `read` can be `-1`, Squeak expects zero.
            } catch (final IOException e) {
                errorProfile.enter();
                throw PrimitiveFailed.GENERIC_ERROR;
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"isStdioFileDescriptor(fileDescriptor)"})
        protected static final Object doReadStdio(final Object receiver, final long fileDescriptor, final NativeObject target, final long startIndex, final long longCount) {
            throw PrimitiveFailed.GENERIC_ERROR;
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private static ByteBuffer allocate(final int count) {
            return ByteBuffer.allocate(count);
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private static int readFrom(final SqueakImageContext image, final long fileDescriptor, final ByteBuffer dst) throws IOException {
            return getFileOrPrimFail(image, fileDescriptor).read(dst);
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private static byte getFrom(final ByteBuffer dst, final int index) {
            return dst.get(index);
        }

        private static int getFromUnsigned(final ByteBuffer dst, final int index) {
            return Byte.toUnsignedInt(getFrom(dst, index));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFileRename")
    protected abstract static class PrimFileRenameNode extends AbstractFilePluginPrimitiveNode implements TernaryPrimitive {

        @Specialization(guards = {"oldName.isByteType()", "newName.isByteType()"})
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected static final Object doRename(final Object receiver, final NativeObject oldName, final NativeObject newName,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            try {
                asPublicTruffleFile(image, oldName).move(asPublicTruffleFile(image, newName));
            } catch (final IOException e) {
                throw PrimitiveFailed.GENERIC_ERROR;
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFileSetPosition")
    protected abstract static class PrimFileSetPositionNode extends AbstractFilePluginPrimitiveNode implements TernaryPrimitive {

        @Specialization(guards = "!isStdioFileDescriptor(fileDescriptor)")
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected static final Object doSet(final Object receiver, final long fileDescriptor, final long position,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            try {
                getFileOrPrimFail(image, fileDescriptor).position(position);
            } catch (IllegalArgumentException | IOException e) {
                throw PrimitiveFailed.GENERIC_ERROR;
            }
            return receiver;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "isStdioFileDescriptor(fileDescriptor)")
        protected static final Object doSetStdio(final Object receiver, final long fileDescriptor, final long position) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFileSize")
    protected abstract static class PrimFileSizeNode extends AbstractFilePluginPrimitiveNode implements BinaryPrimitive {

        @Specialization(guards = "!isStdioFileDescriptor(fileDescriptor)")
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected static final long doSize(@SuppressWarnings("unused") final Object receiver, final long fileDescriptor,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            try {
                return getFileOrPrimFail(image, fileDescriptor).size();
            } catch (final IOException e) {
                throw PrimitiveFailed.GENERIC_ERROR;
            }
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "isStdioFileDescriptor(fileDescriptor)")
        protected static final long doSizeStdio(final Object receiver, final long fileDescriptor) {
            return 0L;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFileStdioHandles")
    protected abstract static class PrimFileStdioHandlesNode extends AbstractFilePluginPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        @Specialization
        protected static final Object getHandles(@SuppressWarnings("unused") final Object receiver,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return image.asArrayOfLongs(STDIO_HANDLES.ALL);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFileTruncate")
    protected abstract static class PrimFileTruncateNode extends AbstractFilePluginPrimitiveNode implements TernaryPrimitive {
        @Specialization(guards = "!isStdioFileDescriptor(fileDescriptor)")
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected static final Object doTruncate(final Object receiver, final long fileDescriptor, final long to,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            try {
                getFileOrPrimFail(image, fileDescriptor).truncate(to);
            } catch (IllegalArgumentException | IOException e) {
                throw PrimitiveFailed.GENERIC_ERROR;
            }
            return receiver;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "isStdioFileDescriptor(fileDescriptor)")
        protected static final Object doTruncateStdio(final Object receiver, final long fileDescriptor, final long to) {
            throw PrimitiveFailed.GENERIC_ERROR;
        }
    }

    @ImportStatic({STDIO_HANDLES.class, FloatObject.class})
    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveFileWrite")
    protected abstract static class PrimFileWriteNode extends AbstractFilePluginPrimitiveNode implements QuinaryPrimitive {

        @Specialization(guards = {"!isStdioFileDescriptor(fileDescriptor)", "content.isByteType()", "inBounds(startIndex, count, content.getByteLength())"})
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected static final long doWriteByte(@SuppressWarnings("unused") final Object receiver, final long fileDescriptor, final NativeObject content, final long startIndex, final long count,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return fileWriteFromAt(image, fileDescriptor, count, content.getByteStorage(), startIndex, 1);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"fileDescriptor == OUT", "content.isByteType()", "inBounds(startIndex, count, content.getByteLength())"})
        protected static final long doWriteByteToStdout(final Object receiver, final long fileDescriptor, final NativeObject content, final long startIndex, final long count,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return fileWriteToOutputStream(image.env.out(), content.getByteStorage(), startIndex, count);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"fileDescriptor == ERROR", "content.isByteType()", "inBounds(startIndex, count, content.getByteLength())"})
        protected static final long doWriteByteToStderr(final Object receiver, final long fileDescriptor, final NativeObject content, final long startIndex, final long count,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return fileWriteToOutputStream(image.env.err(), content.getByteStorage(), startIndex, count);
        }

        @Specialization(guards = {"!isStdioFileDescriptor(fileDescriptor)", "content.isIntType()", "inBounds(startIndex, count, content.getIntLength())"})
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected static final long doWriteInt(@SuppressWarnings("unused") final Object receiver, final long fileDescriptor, final NativeObject content, final long startIndex, final long count,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            // TODO: use ByteBuffer or UnsafeUtils here?
            final int[] ints = content.getIntStorage();
            final int intsLength = ints.length;
            final byte[] bytes = new byte[intsLength * Integer.BYTES];
            for (int i = 0; i < intsLength; i++) {
                UnsafeUtils.putIntReversed(bytes, i, ints[i]);
            }
            return fileWriteFromAt(image, fileDescriptor, count, bytes, startIndex, 4);
        }

        @Specialization(guards = {"!isStdioFileDescriptor(fileDescriptor)", "inBounds(startIndex, count, content.size())"})
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected static final long doWriteLargeInteger(@SuppressWarnings("unused") final Object receiver, final long fileDescriptor, final LargeIntegerObject content, final long startIndex,
                        final long count,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return fileWriteFromAt(image, fileDescriptor, count, content.getBytes(), startIndex, 1);
        }

        @Specialization(guards = {"!isStdioFileDescriptor(fileDescriptor)", "inBounds(startIndex, count, WORD_LENGTH)"})
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected static final long doWriteDouble(@SuppressWarnings("unused") final Object receiver, final long fileDescriptor, final double content, final long startIndex, final long count,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return fileWriteFromAt(image, fileDescriptor, count, FloatObject.getBytes(content), startIndex, 8);
        }

        @Specialization(guards = {"!isStdioFileDescriptor(fileDescriptor)", "inBounds(startIndex, count, WORD_LENGTH)"})
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected static final long doWriteFloatObject(@SuppressWarnings("unused") final Object receiver, final long fileDescriptor, final FloatObject content, final long startIndex, final long count,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return fileWriteFromAt(image, fileDescriptor, count, content.getBytes(), startIndex, 8);
        }

        private static long fileWriteFromAt(final SqueakImageContext image, final long fileDescriptor, final long count, final byte[] bytes, final long startIndex, final int elementSize) {
            final int byteStart = (int) (startIndex - 1) * elementSize;
            final int byteEnd = Math.min(byteStart + (int) count, bytes.length) * elementSize;
            final ByteBuffer buffer = ByteBuffer.wrap(bytes);
            buffer.position(byteStart);
            buffer.limit(byteEnd);
            final int written;
            try {
                written = getFileOrPrimFail(image, fileDescriptor).write(buffer);
            } catch (final IOException e) {
                throw PrimitiveFailed.GENERIC_ERROR;
            }
            return written / elementSize;
        }

        @TruffleBoundary(transferToInterpreterOnException = false)
        private static long fileWriteToOutputStream(final OutputStream outputStream, final byte[] content, final long startIndex, final long count) {
            final int byteStart = (int) (startIndex - 1);
            final int byteEnd = Math.min(byteStart + (int) count, content.length);
            try {
                outputStream.write(content, byteStart, Math.max(byteEnd - byteStart, 0));
                outputStream.flush();
            } catch (final IOException e) {
                throw PrimitiveFailed.GENERIC_ERROR;
            }
            return byteEnd - byteStart;
        }
    }
}
