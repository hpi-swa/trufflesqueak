/*
 * Copyright (c) 2017-2021 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveFallbacks.BinaryPrimitiveFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;

public final class DropPlugin extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return DropPluginFactory.getFactories();
    }

    @ImportStatic(DropPlugin.class)
    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDropRequestFileHandle")
    protected abstract static class PrimDropRequestFileHandleNode extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {

        @Specialization(guards = "dropIndex <= getFileList(image).length")
        protected final PointersObject doRequest(@SuppressWarnings("unused") final Object receiver, final long dropIndex,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return FilePlugin.createFileHandleOrPrimFail(image,
                            image.env.getPublicTruffleFile(getFileList(image)[(int) dropIndex - 1]), false);
        }

        @SuppressWarnings("static-method") // Work around code generation problems.
        protected final String[] getFileList(final SqueakImageContext image) {
            return image.dropPluginFileList;
        }
    }

    @ImportStatic(DropPlugin.class)
    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDropRequestFileName")
    protected abstract static class PrimDropRequestFileNameNode extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {

        @Specialization(guards = "dropIndex <= getFileList(image).length")
        protected final NativeObject doRequest(@SuppressWarnings("unused") final Object receiver, final long dropIndex,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return image.asByteString(getFileList(image)[(int) dropIndex - 1]);
        }

        @SuppressWarnings("static-method") // Work around code generation problems.
        protected final String[] getFileList(final SqueakImageContext image) {
            return image.dropPluginFileList;
        }
    }
}
