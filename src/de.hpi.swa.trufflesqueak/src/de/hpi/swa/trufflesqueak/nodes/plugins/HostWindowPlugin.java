/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive2WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive3WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;

public class HostWindowPlugin extends AbstractPrimitiveFactoryHolder {
    public static final long DEFAULT_HOST_WINDOW_ID = 1;

    @ImportStatic(HostWindowPlugin.class)
    protected abstract static class AbstractHostWindowPrimitiveNode extends AbstractPrimitiveNode {
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveWindowClose")
    protected abstract static class PrimHostWindowCloseNode extends AbstractHostWindowPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "id == DEFAULT_HOST_WINDOW_ID")
        protected final Object doClose(final Object receiver, @SuppressWarnings("unused") final long id) {
            final SqueakImageContext image = getContext();
            if (image.hasDisplay()) {
                image.getDisplay().close();
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveHostWindowPosition")
    protected abstract static class PrimHostWindowPositionNode extends AbstractHostWindowPrimitiveNode implements Primitive1WithFallback {
        @SuppressWarnings("unused")
        @Specialization(guards = {"id == DEFAULT_HOST_WINDOW_ID"})
        protected final Object doSize(final Object receiver, final long id,
                        @Bind final Node node,
                        @Cached final AbstractPointersObjectWriteNode writeNode) {
            return getContext().asPoint(writeNode, node, 0L, 0L);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveHostWindowSizeSet")
    protected abstract static class PrimHostWindowSizeSetNode extends AbstractHostWindowPrimitiveNode implements Primitive3WithFallback {
        @Specialization(guards = "id == DEFAULT_HOST_WINDOW_ID")
        protected final Object doSize(final Object receiver, @SuppressWarnings("unused") final long id, final long width, final long height) {
            final SqueakImageContext image = getContext();
            if (image.hasDisplay()) {
                image.getDisplay().resizeTo((int) width, (int) height);
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveHostWindowTitle")
    protected abstract static class PrimHostWindowTitleNode extends AbstractHostWindowPrimitiveNode implements Primitive2WithFallback {

        @Specialization(guards = {"id == DEFAULT_HOST_WINDOW_ID", "title.isByteType()"})
        protected final Object doTitle(final Object receiver, @SuppressWarnings("unused") final long id, final NativeObject title) {
            final SqueakImageContext image = getContext();
            if (image.hasDisplay()) {
                image.getDisplay().setWindowTitle(title.asStringUnsafe());
            }
            return receiver;
        }
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return HostWindowPluginFactory.getFactories();
    }
}
