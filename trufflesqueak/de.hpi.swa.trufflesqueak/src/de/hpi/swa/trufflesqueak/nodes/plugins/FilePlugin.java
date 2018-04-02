package de.hpi.swa.trufflesqueak.nodes.plugins;

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

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;

public final class FilePlugin extends AbstractPrimitiveFactoryHolder {
    private static final class STDIO_HANDLES {
        private static final long IN = 0;
        private static final long OUT = 1;
        private static final long ERROR = 2;
    }

    @CompilationFinal private static final Map<Long, RandomAccessFile> files = new HashMap<>();

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return FilePluginFactory.getFactories();
    }

    protected static abstract class AbstractFilePluginPrimitiveNode extends AbstractPrimitiveNode {

        protected AbstractFilePluginPrimitiveNode(CompiledMethodObject method) {
            super(method);
        }

        protected boolean isString(NativeObject obj) {
            return obj.isSpecialKindAt(SPECIAL_OBJECT_INDEX.ClassString);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveDirectoryCreate", numArguments = 2)
    protected static abstract class PrimDirectoryCreateNode extends AbstractPrimitiveNode {

        protected PrimDirectoryCreateNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object doCreate(PointersObject receiver, NativeObject fullPath) {
            File directory = new File(fullPath.toString());
            if (directory.mkdir()) {
                return receiver;
            }
            throw new PrimitiveFailed();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveDirectoryDelete", numArguments = 2)
    protected static abstract class PrimDirectoryDeleteNode extends AbstractPrimitiveNode {

        protected PrimDirectoryDeleteNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object doCreate(PointersObject receiver, NativeObject fullPath) {
            File directory = new File(fullPath.toString());
            if (directory.delete()) {
                return receiver;
            }
            throw new PrimitiveFailed();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveDirectoryDelimitor")
    protected static abstract class PrimDirectoryDelimitorNode extends AbstractPrimitiveNode {

        protected PrimDirectoryDelimitorNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected char doDelimitor(@SuppressWarnings("unused") Object receiver) {
            return File.separatorChar;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveDirectoryEntry", numArguments = 3)
    protected static abstract class PrimDirectoryEntryNode extends AbstractFilePluginPrimitiveNode {

        protected PrimDirectoryEntryNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "isString(fullPath)")
        protected Object doLookup(@SuppressWarnings("unused") PointersObject receiver, NativeObject fullPath, NativeObject fName) {
            String pathName = fullPath.toString();
            String fileName = fName.toString();
            File path;
            if (fileName.equals(".")) {
                path = new File(pathName);
            } else {
                path = new File(pathName + File.separator + fileName);
            }
            if (path.exists()) {
                Object[] result = new Object[]{path.getName(), path.lastModified(), path.lastModified(), path.isDirectory(), path.length()};
                return code.image.wrap(result);
            }
            return code.image.nil;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveDirectoryLookup", numArguments = 3)
    protected static abstract class PrimDirectoryLookupNode extends AbstractFilePluginPrimitiveNode {

        protected PrimDirectoryLookupNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "isString(nativePathName)")
        protected Object doLookup(@SuppressWarnings("unused") PointersObject receiver, NativeObject nativePathName, long longIndex) {
            int index = (int) longIndex;
            if (index < 0) {
                throw new PrimitiveFailed();
            }
            String pathName = nativePathName.toString();
            if (pathName.length() == 0) {
                pathName = "/";
            }
            File directory = new File(pathName);
            if (!directory.isDirectory()) {
                throw new PrimitiveFailed();
            }
            File[] paths = directory.listFiles();
            if (index < paths.length) {
                File path = paths[index];
                Object[] result = new Object[]{path.getName(), path.lastModified(), path.lastModified(), path.isDirectory(), path.length()};
                return code.image.wrap(result);
            }
            return code.image.nil;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveFileAtEnd", numArguments = 2)
    protected static abstract class PrimFileAtEndNode extends AbstractPrimitiveNode {

        protected PrimFileAtEndNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object doAtEnd(@SuppressWarnings("unused") PointersObject receiver, long fileDescriptor) {
            try {
                RandomAccessFile file = files.get(fileDescriptor);
                return code.image.wrap(file.getFilePointer() >= file.length() - 1);
            } catch (NullPointerException | IOException e) {
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveFileClose", numArguments = 2)
    protected static abstract class PrimFileCloseNode extends AbstractPrimitiveNode {

        protected PrimFileCloseNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object doClose(PointersObject receiver, long fileDescriptor) {
            try {
                RandomAccessFile file = files.get(fileDescriptor);
                file.close();
            } catch (NullPointerException | IOException e) {
                throw new PrimitiveFailed();
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveFileDelete", numArguments = 2)
    protected static abstract class PrimFileDeleteNode extends AbstractFilePluginPrimitiveNode {

        protected PrimFileDeleteNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "isString(nativeFileName)")
        protected Object doDelete(PointersObject receiver, NativeObject nativeFileName) {
            File file = new File(nativeFileName.toString());
            if (file.delete()) {
                return receiver;
            }
            throw new PrimitiveFailed();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveFileFlush", numArguments = 2)
    protected static abstract class PrimFileFlushNode extends AbstractPrimitiveNode {

        protected PrimFileFlushNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object doFlush(PointersObject receiver, @SuppressWarnings("unused") long fileDescriptor) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveFileGetPosition", numArguments = 2)
    protected static abstract class PrimFileGetPositionNode extends AbstractPrimitiveNode {

        protected PrimFileGetPositionNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object doGet(@SuppressWarnings("unused") PointersObject receiver, long fileDescriptor) {
            try {
                RandomAccessFile file = files.get(fileDescriptor);
                return file.getFilePointer();
            } catch (NullPointerException | IOException e) {
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveFileOpen", numArguments = 3)
    protected static abstract class PrimFileOpenNode extends AbstractFilePluginPrimitiveNode {

        protected PrimFileOpenNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "isString(nativeFileName)")
        protected Object doOpen(@SuppressWarnings("unused") PointersObject receiver, NativeObject nativeFileName, Boolean writableFlag) {
            String fileName = nativeFileName.toString();
            String mode = writableFlag ? "rw" : "r";
            try {
                RandomAccessFile file = new RandomAccessFile(fileName, mode);
                long fileId = file.hashCode();
                files.put(fileId, file);
                return fileId;
            } catch (FileNotFoundException e) {
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveFileRead", numArguments = 5)
    protected static abstract class PrimFileReadNode extends AbstractPrimitiveNode {

        protected PrimFileReadNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object doRead(@SuppressWarnings("unused") PointersObject receiver, long fileDescriptor, BaseSqueakObject target, long startIndex, long longCount) {
            int count = (int) longCount;
            byte[] buffer = new byte[count];
            try {
                RandomAccessFile file = files.get(fileDescriptor);
                long read = file.read(buffer, 0, count);
                for (int index = 0; index < read; index++) {
                    target.atput0(startIndex - 1 + index, (long) buffer[index]);
                }
                return read;
            } catch (NullPointerException | IOException e) {
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveFileRename", numArguments = 3)
    protected static abstract class PrimFileRenameNode extends AbstractFilePluginPrimitiveNode {

        protected PrimFileRenameNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"isString(oldName)", "isString(newName)"})
        protected Object doRename(PointersObject receiver, NativeObject oldName, NativeObject newName) {
            File file = new File(oldName.toString());
            if (file.renameTo(new File(newName.toString()))) {
                return receiver;
            }
            throw new PrimitiveFailed();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveFileSetPosition", numArguments = 3)
    protected static abstract class PrimFileSetPositionNode extends AbstractPrimitiveNode {

        protected PrimFileSetPositionNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object doSet(PointersObject receiver, long fileDescriptor, long position) {
            try {
                RandomAccessFile file = files.get(fileDescriptor);
                file.seek(position);
            } catch (NullPointerException | IOException e) {
                throw new PrimitiveFailed();
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveFileSize", numArguments = 2)
    protected static abstract class PrimFileSizeNode extends AbstractPrimitiveNode {

        protected PrimFileSizeNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object doSize(@SuppressWarnings("unused") PointersObject receiver, long fileDescriptor) {
            try {
                RandomAccessFile file = files.get(fileDescriptor);
                return code.image.wrap(file.length());
            } catch (NullPointerException | IOException e) {
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveFileStdioHandles")
    protected static abstract class PrimFileStdioHandlesNode extends AbstractPrimitiveNode {
        protected PrimFileStdioHandlesNode(CompiledMethodObject code) {
            super(code);
        }

        @Specialization
        protected Object getHandles(@SuppressWarnings("unused") ClassObject receiver) {
            return code.image.newListWith(STDIO_HANDLES.IN, STDIO_HANDLES.OUT, STDIO_HANDLES.ERROR);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveFileTruncate", numArguments = 3)
    protected static abstract class PrimFileTruncateNode extends AbstractPrimitiveNode {
        protected PrimFileTruncateNode(CompiledMethodObject code) {
            super(code);
        }

        @Specialization
        protected Object doTruncate(PointersObject receiver, long fileDescriptor, long to) {
            try {
                RandomAccessFile file = files.get(fileDescriptor);
                file.setLength(to);
            } catch (NullPointerException | IOException e) {
                throw new PrimitiveFailed();
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveFileWrite", numArguments = 5)
    protected static abstract class PrimFileWriteNode extends AbstractPrimitiveNode {
        protected PrimFileWriteNode(CompiledMethodObject code) {
            super(code);
        }

        @Specialization
        @TruffleBoundary
        protected long doWrite(PointersObject receiver, long fileDescriptor, NativeObject content, long startIndex, long count) {
            byte[] bytes = content.getBytes();
            long elementSize = content.getElementSize();
            int byteStart = (int) ((startIndex - 1) * elementSize);
            int byteEnd = (int) (Math.min(startIndex - 1 + count, bytes.length) * elementSize);
            if (fileDescriptor == STDIO_HANDLES.IN) {
                throw new PrimitiveFailed();
            } else if (fileDescriptor == STDIO_HANDLES.OUT) {
                code.image.getOutput().append(content.toString(), byteStart, byteEnd);
                code.image.getOutput().flush();
            } else if (fileDescriptor == STDIO_HANDLES.ERROR) {
                code.image.getError().append(content.toString(), byteStart, byteEnd);
                code.image.getError().flush();
            } else {
                try {
                    RandomAccessFile file = files.get(fileDescriptor);
                    file.write(bytes);
                } catch (NullPointerException | IOException e) {
                    throw new PrimitiveFailed();
                }
            }
            return (byteEnd - byteStart) / elementSize;
        }
    }
}