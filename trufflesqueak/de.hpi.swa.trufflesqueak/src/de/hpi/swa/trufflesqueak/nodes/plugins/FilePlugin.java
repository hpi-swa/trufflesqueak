package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.io.File;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;

public final class FilePlugin extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return FilePluginFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveFileSize", numArguments = 2)
    public static abstract class PrimFileSizeNode extends AbstractPrimitiveNode {
        public PrimFileSizeNode(CompiledMethodObject code) {
            super(code);
        }

        @Specialization
        int size(@SuppressWarnings("unused") Object receiver, int fd) {
            // TODO: use registry of files
            if (fd <= 2) {
                return 0;
            }
            throw new PrimitiveFailed();
        }

        // TODO: double, long, BigInteger
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveFileStdioHandles")
    public static abstract class PrimFileStdioHandlesNode extends AbstractPrimitiveNode {
        public PrimFileStdioHandlesNode(CompiledMethodObject code) {
            super(code);
        }

        @SuppressWarnings("unused")
        @Specialization
        public Object getHandles(VirtualFrame frame) {
            return code.image.wrap(0, 1, 2);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveFileWrite", numArguments = 5)
    public static abstract class PrimFileWriteNode extends AbstractPrimitiveNode {
        public PrimFileWriteNode(CompiledMethodObject code) {
            super(code);
        }

        @Specialization
        @TruffleBoundary
        int write(@SuppressWarnings("unused") Object receiver, int fd, NativeObject content, int start, int count) {
            // TODO: use registry of files
            String chars = content.toString();
            int elementSize = content.getElementSize();
            int byteStart = (start - 1) * elementSize;
            int byteEnd = Math.min(start - 1 + count, chars.length()) * elementSize;
            switch (fd) {
                case 1:
                    code.image.getOutput().append(chars, byteStart, byteEnd);
                    code.image.getOutput().flush();
                    break;
                case 2:
                    code.image.getError().append(chars, byteStart, byteEnd);
                    code.image.getError().flush();
                    break;
                default:
                    throw new PrimitiveFailed();
            }
            return (byteEnd - byteStart) / elementSize;
        }

        // TODO: double, long, BigInteger
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveDirectoryDelimitor")
    public static abstract class PrimDirectoryDelimiterNode extends AbstractPrimitiveNode {

        public PrimDirectoryDelimiterNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        Object get(@SuppressWarnings("unused") Object receiver) {
            return code.image.wrap(File.separatorChar);
        }

    }

}
