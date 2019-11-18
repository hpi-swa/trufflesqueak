/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;

public final class DropPlugin extends AbstractPrimitiveFactoryHolder {
    public static void updateFileList(final SqueakImageContext image, final String[] newList) {
        image.dropPluginFileList = newList;
    }

    public static int getFileListSize(final SqueakImageContext image) {
        return getFileList(image).length;
    }

    private static String[] getFileList(final SqueakImageContext image) {
        return image.dropPluginFileList;
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return DropPluginFactory.getFactories();
    }

    @ImportStatic(DropPlugin.class)
    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDropRequestFileHandle")
    protected abstract static class PrimDropRequestFileHandleNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimDropRequestFileHandleNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "dropIndex <= getFileListSize(method.image)")
        protected final Object doRequest(@SuppressWarnings("unused") final Object receiver, final long dropIndex) {
            return FilePlugin.createFileHandleOrPrimFail(method.image,
                            method.image.env.getPublicTruffleFile(getFileList(method.image)[(int) dropIndex - 1]), false);
        }
    }

    @ImportStatic(DropPlugin.class)
    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDropRequestFileName")
    protected abstract static class PrimDropRequestFileNameNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimDropRequestFileNameNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "dropIndex <= getFileListSize(method.image)")
        protected final Object doRequest(@SuppressWarnings("unused") final Object receiver, final long dropIndex) {
            return method.image.asByteString(getFileList(method.image)[(int) dropIndex - 1]);
        }
    }
}
