/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;

public final class DropPlugin extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return DropPluginFactory.getFactories();
    }

    @ImportStatic(DropPlugin.class)
    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDropRequestFileHandle")
    protected abstract static class PrimDropRequestFileHandleNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization(guards = "dropIndex <= getFileList(getContext()).length")
        protected final PointersObject doRequest(@SuppressWarnings("unused") final Object receiver, final long dropIndex) {
            final SqueakImageContext image = getContext();
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
    protected abstract static class PrimDropRequestFileNameNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization(guards = "dropIndex <= getFileList(getContext()).length")
        protected final NativeObject doRequest(@SuppressWarnings("unused") final Object receiver, final long dropIndex) {
            final SqueakImageContext image = getContext();
            return image.asByteString(getFileList(image)[(int) dropIndex - 1]);
        }

        @SuppressWarnings("static-method") // Work around code generation problems.
        protected final String[] getFileList(final SqueakImageContext image) {
            return image.dropPluginFileList;
        }
    }
}
