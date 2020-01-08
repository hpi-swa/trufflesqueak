/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.QuaternaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.QuinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;

public final class ZipPlugin extends AbstractPrimitiveFactoryHolder {
    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDeflateBlock")
    protected abstract static class PrimDeflateBlockNode extends AbstractPrimitiveNode implements QuaternaryPrimitive {

        protected PrimDeflateBlockNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"receiver.size() >= 15"})
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final boolean doDeflateBlock(final PointersObject receiver, final long lastIndex, final long chainLength, final long goodMatch) {
            return method.image.zip.primitiveDeflateBlock(receiver, (int) lastIndex, (int) chainLength, (int) goodMatch);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDeflateUpdateHashTable")
    protected abstract static class PrimDeflateUpdateHashTableNode extends AbstractPrimitiveNode implements TernaryPrimitive {

        protected PrimDeflateUpdateHashTableNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"table.isIntType()"})
        protected static final Object doDeflateUpdateHashTable(final Object receiver, final NativeObject table, final long delta) {
            Zip.primitiveDeflateUpdateHashTable(table, (int) delta);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveInflateDecompressBlock")
    protected abstract static class PrimInflateDecompressBlockNode extends AbstractPrimitiveNode implements TernaryPrimitive {

        protected PrimInflateDecompressBlockNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"method.image.zip.readStreamHasCorrectSize(receiver)", "llTable.isIntType()", "dTable.isIntType()"})
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final PointersObject doInflateDecompressBlock(final PointersObject receiver, final NativeObject llTable, final NativeObject dTable) {
            method.image.zip.primitiveInflateDecompressBlock(receiver, llTable, dTable);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveUpdateAdler32")
    protected abstract static class PrimUpdateAdler32Node extends AbstractPrimitiveNode implements QuinaryPrimitive {

        protected PrimUpdateAdler32Node(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"stopIndex >= startIndex", "startIndex > 0", "collection.isByteType()", "stopIndex <= collection.getByteLength()"})
        protected static final long doUpdateAdler32(@SuppressWarnings("unused") final Object receiver, final long adler32, final long startIndex, final long stopIndex,
                        final NativeObject collection) {
            return Zip.primitiveUpdateAdler32(adler32, (int) startIndex, (int) stopIndex, collection);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveUpdateGZipCrc32")
    protected abstract static class PrimUpdateGZipCrc32Node extends AbstractPrimitiveNode implements QuinaryPrimitive {

        protected PrimUpdateGZipCrc32Node(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"stopIndex >= startIndex", "startIndex > 0", "collection.isByteType()", "stopIndex <= collection.getByteLength()"})
        protected static final long doUpdateGZipCrc32(@SuppressWarnings("unused") final Object receiver, final long crc, final long startIndex, final long stopIndex,
                        final NativeObject collection) {
            return Zip.primitiveUpdateGZipCrc32(collection, (int) startIndex, (int) stopIndex, crc);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveZipSendBlock")
    protected abstract static class PrimZipSendBlockNode extends AbstractPrimitiveNode implements QuinaryPrimitive {

        protected PrimZipSendBlockNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"method.image.zip.writeStreamHasCorrectSize(receiver)", "distTree.size()>= 2", "litTree.size() >= 2", "litStream.size() >= 3", "distStream.size() >= 3"})
        @TruffleBoundary(transferToInterpreterOnException = false)
        protected final long doZipSendBlock(final PointersObject receiver, final PointersObject litStream, final PointersObject distStream, final PointersObject litTree,
                        final PointersObject distTree) {
            return method.image.zip.primitiveZipSendBlock(receiver, litStream, distStream, litTree, distTree);
        }
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return ZipPluginFactory.getFactories();
    }
}
