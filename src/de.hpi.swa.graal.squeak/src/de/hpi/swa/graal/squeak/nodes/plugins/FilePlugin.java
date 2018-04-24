package de.hpi.swa.graal.squeak.nodes.plugins;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.model.BaseSqueakObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;

public final class FilePlugin extends AbstractPrimitiveFactoryHolder {
    @CompilationFinal private static final Map<Long, RandomAccessFile> files = new HashMap<>();

    private static final class STDIO_HANDLES {
        private static final long IN = 0;
        private static final long OUT = 1;
        private static final long ERROR = 2;
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return FilePluginFactory.getFactories();
    }

    @TruffleBoundary
    private static RandomAccessFile getFileOrPrimFail(final long fileDescriptor) {
        final RandomAccessFile handle = files.get(fileDescriptor);
        if (handle == null) {
            throw new PrimitiveFailed();
        }
        return handle;
    }

    protected abstract static class AbstractFilePluginPrimitiveNode extends AbstractPrimitiveNode {

        protected AbstractFilePluginPrimitiveNode(final CompiledMethodObject method) {
            super(method);
        }

        protected boolean isString(final NativeObject obj) {
            return obj.isSpecialKindAt(SPECIAL_OBJECT_INDEX.ClassString);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveDirectoryCreate", numArguments = 2)
    protected abstract static class PrimDirectoryCreateNode extends AbstractPrimitiveNode {

        protected PrimDirectoryCreateNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object doCreate(final PointersObject receiver, final NativeObject fullPath) {
            final File directory = new File(fullPath.toString());
            if (directory.mkdir()) {
                return receiver;
            }
            throw new PrimitiveFailed();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveDirectoryDelete", numArguments = 2)
    protected abstract static class PrimDirectoryDeleteNode extends AbstractPrimitiveNode {

        protected PrimDirectoryDeleteNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object doCreate(final PointersObject receiver, final NativeObject fullPath) {
            final File directory = new File(fullPath.toString());
            if (directory.delete()) {
                return receiver;
            }
            throw new PrimitiveFailed();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveDirectoryDelimitor")
    protected abstract static class PrimDirectoryDelimitorNode extends AbstractPrimitiveNode {

        protected PrimDirectoryDelimitorNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final char doDelimitor(@SuppressWarnings("unused") final Object receiver) {
            return File.separatorChar;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveDirectoryEntry", numArguments = 3)
    protected abstract static class PrimDirectoryEntryNode extends AbstractFilePluginPrimitiveNode {

        protected PrimDirectoryEntryNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "isString(fullPath)")
        protected final Object doLookup(@SuppressWarnings("unused") final PointersObject receiver, final NativeObject fullPath, final NativeObject fName) {
            final String pathName = fullPath.toString();
            final String fileName = fName.toString();
            final File path;
            if (".".equals(fileName)) {
                path = new File(pathName);
            } else {
                path = new File(pathName + File.separator + fileName);
            }
            if (path.exists()) {
                final Object[] result = new Object[]{path.getName(), path.lastModified(), path.lastModified(), path.isDirectory(), path.length()};
                return code.image.wrap(result);
            }
            return code.image.nil;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveDirectoryLookup", numArguments = 3)
    protected abstract static class PrimDirectoryLookupNode extends AbstractFilePluginPrimitiveNode {

        protected PrimDirectoryLookupNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "isString(nativePathName)")
        protected final Object doLookup(@SuppressWarnings("unused") final PointersObject receiver, final NativeObject nativePathName, final long longIndex) {
            final int index = (int) longIndex;
            if (index < 0) {
                throw new PrimitiveFailed();
            }
            String pathName = nativePathName.toString();
            if (pathName.length() == 0) {
                pathName = "/";
            }
            final File directory = new File(pathName);
            if (!directory.isDirectory()) {
                throw new PrimitiveFailed();
            }
            final File[] paths = directory.listFiles();
            if (index < paths.length) {
                final File path = paths[index];
                final Object[] result = new Object[]{path.getName(), path.lastModified(), path.lastModified(), path.isDirectory(), path.length()};
                return code.image.wrap(result);
            }
            return code.image.nil;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveFileAtEnd", numArguments = 2)
    protected abstract static class PrimFileAtEndNode extends AbstractPrimitiveNode {

        protected PrimFileAtEndNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final Object doAtEnd(@SuppressWarnings("unused") final PointersObject receiver, final long fileDescriptor) {
            try {
                final RandomAccessFile file = getFileOrPrimFail(fileDescriptor);
                return code.image.wrap(file.getFilePointer() >= file.length() - 1);
            } catch (IOException e) {
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveFileClose", numArguments = 2)
    protected abstract static class PrimFileCloseNode extends AbstractPrimitiveNode {

        protected PrimFileCloseNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object doClose(final PointersObject receiver, final long fileDescriptor) {
            try {
                getFileOrPrimFail(fileDescriptor).close();
            } catch (IOException e) {
                throw new PrimitiveFailed();
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveFileDelete", numArguments = 2)
    protected abstract static class PrimFileDeleteNode extends AbstractFilePluginPrimitiveNode {

        protected PrimFileDeleteNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "isString(nativeFileName)")
        protected static final Object doDelete(final PointersObject receiver, final NativeObject nativeFileName) {
            final File file = new File(nativeFileName.toString());
            if (file.delete()) {
                return receiver;
            }
            throw new PrimitiveFailed();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveFileFlush", numArguments = 2)
    protected abstract static class PrimFileFlushNode extends AbstractPrimitiveNode {

        protected PrimFileFlushNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object doFlush(final PointersObject receiver, @SuppressWarnings("unused") final long fileDescriptor) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveFileGetPosition", numArguments = 2)
    protected abstract static class PrimFileGetPositionNode extends AbstractPrimitiveNode {

        protected PrimFileGetPositionNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final Object doGet(@SuppressWarnings("unused") final PointersObject receiver, final long fileDescriptor) {
            try {
                return code.image.wrap(getFileOrPrimFail(fileDescriptor).getFilePointer());
            } catch (IOException e) {
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveFileOpen", numArguments = 3)
    protected abstract static class PrimFileOpenNode extends AbstractFilePluginPrimitiveNode {

        protected PrimFileOpenNode(final CompiledMethodObject method) {
            super(method);
        }

        @TruffleBoundary
        @Specialization(guards = "isString(nativeFileName)")
        protected static final Object doOpen(@SuppressWarnings("unused") final PointersObject receiver, final NativeObject nativeFileName, final Boolean writableFlag) {
            final String fileName = nativeFileName.toString();
            final String mode = writableFlag ? "rw" : "r";
            try {
                final RandomAccessFile file = new RandomAccessFile(fileName, mode);
                final long fileId = file.hashCode();
                files.put(fileId, file);
                return fileId;
            } catch (FileNotFoundException e) {
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveFileRead", numArguments = 5)
    protected abstract static class PrimFileReadNode extends AbstractPrimitiveNode {

        protected PrimFileReadNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object doRead(@SuppressWarnings("unused") final PointersObject receiver, final long fileDescriptor, final BaseSqueakObject target, final long startIndex,
                        final long longCount) {
            final int count = (int) longCount;
            final byte[] buffer = new byte[count];
            try {
                final long read = getFileOrPrimFail(fileDescriptor).read(buffer, 0, count);
                for (int index = 0; index < read; index++) {
                    target.atput0(startIndex - 1 + index, buffer[index] & 0xffL);
                }
                return read;
            } catch (IOException e) {
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveFileRename", numArguments = 3)
    protected abstract static class PrimFileRenameNode extends AbstractFilePluginPrimitiveNode {

        protected PrimFileRenameNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"isString(oldName)", "isString(newName)"})
        protected static final Object doRename(final PointersObject receiver, final NativeObject oldName, final NativeObject newName) {
            final File file = new File(oldName.toString());
            if (file.renameTo(new File(newName.toString()))) {
                return receiver;
            }
            throw new PrimitiveFailed();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveFileSetPosition", numArguments = 3)
    protected abstract static class PrimFileSetPositionNode extends AbstractPrimitiveNode {

        protected PrimFileSetPositionNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object doSet(final PointersObject receiver, final long fileDescriptor, final long position) {
            try {
                getFileOrPrimFail(fileDescriptor).seek(position);
            } catch (IOException e) {
                throw new PrimitiveFailed();
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveFileSize", numArguments = 2)
    protected abstract static class PrimFileSizeNode extends AbstractPrimitiveNode {

        protected PrimFileSizeNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final Object doSize(@SuppressWarnings("unused") final PointersObject receiver, final long fileDescriptor) {
            try {
                return code.image.wrap(getFileOrPrimFail(fileDescriptor).length());
            } catch (IOException e) {
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveFileStdioHandles")
    protected abstract static class PrimFileStdioHandlesNode extends AbstractPrimitiveNode {
        protected PrimFileStdioHandlesNode(final CompiledMethodObject code) {
            super(code);
        }

        @Specialization
        protected final Object getHandles(@SuppressWarnings("unused") final ClassObject receiver) {
            return code.image.newListWith(STDIO_HANDLES.IN, STDIO_HANDLES.OUT, STDIO_HANDLES.ERROR);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveFileTruncate", numArguments = 3)
    protected abstract static class PrimFileTruncateNode extends AbstractPrimitiveNode {
        protected PrimFileTruncateNode(final CompiledMethodObject code) {
            super(code);
        }

        @Specialization
        protected static final Object doTruncate(final PointersObject receiver, final long fileDescriptor, final long to) {
            try {
                getFileOrPrimFail(fileDescriptor).setLength(to);
            } catch (IOException e) {
                throw new PrimitiveFailed();
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveFileWrite", numArguments = 5)
    protected abstract static class PrimFileWriteNode extends AbstractPrimitiveNode {
        protected PrimFileWriteNode(final CompiledMethodObject code) {
            super(code);
        }

        @Specialization
        protected final long doWrite(final PointersObject receiver, final long fileDescriptor, final NativeObject content, final long startIndex, final long count) {
            final byte[] bytes = content.getBytes();
            final long elementSize = content.getElementSize();
            final int byteStart = (int) ((startIndex - 1) * elementSize);
            final int byteEnd = (int) (Math.min(startIndex - 1 + count, bytes.length) * elementSize);
            if (fileDescriptor == STDIO_HANDLES.IN) {
                throw new PrimitiveFailed();
            } else if (fileDescriptor == STDIO_HANDLES.OUT) {
                code.image.getOutput().append(content.toString(), byteStart, byteEnd);
                code.image.getOutput().flush();
            } else if (fileDescriptor == STDIO_HANDLES.ERROR) {
                code.image.getError().append(content.toString(), byteStart, byteEnd);
                code.image.getError().flush();
            } else {
                if (code.image.config.isVerbose()) { // also print to stderr
                    doWrite(receiver, STDIO_HANDLES.ERROR, content, startIndex, count);
                }
                try {
                    getFileOrPrimFail(fileDescriptor).write(bytes);
                } catch (IOException e) {
                    throw new PrimitiveFailed();
                }
            }
            return (byteEnd - byteStart) / elementSize;
        }
    }
}
