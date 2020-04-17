/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.QuaternaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;

public class HostWindowPlugin extends AbstractPrimitiveFactoryHolder {
    public static final long DEFAULT_HOST_WINDOW_ID = 1;

    @ImportStatic(HostWindowPlugin.class)
    protected abstract static class AbstractHostWindowPrimitiveNode extends AbstractPrimitiveNode {
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveWindowClose")
    protected abstract static class PrimHostWindowCloseNode extends AbstractHostWindowPrimitiveNode implements BinaryPrimitive {
        @Specialization(guards = "id == DEFAULT_HOST_WINDOW_ID")
        protected static final Object doClose(final Object receiver, @SuppressWarnings("unused") final long id,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            if (image.hasDisplay()) {
                image.getDisplay().close();
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveHostWindowPosition")
    protected abstract static class PrimHostWindowPositionNode extends AbstractHostWindowPrimitiveNode implements BinaryPrimitive {
        @SuppressWarnings("unused")
        @Specialization(guards = {"id == DEFAULT_HOST_WINDOW_ID"})
        protected static final Object doSize(final Object receiver, final long id,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image,
                        @Cached final AbstractPointersObjectWriteNode writeNode) {
            return image.asPoint(writeNode, 0L, 0L);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveHostWindowSizeSet")
    protected abstract static class PrimHostWindowSizeSetNode extends AbstractHostWindowPrimitiveNode implements QuaternaryPrimitive {
        @Specialization(guards = "id == DEFAULT_HOST_WINDOW_ID")
        protected static final Object doSize(final Object receiver, @SuppressWarnings("unused") final long id, final long width, final long height,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            if (image.hasDisplay()) {
                image.getDisplay().resizeTo((int) width, (int) height);
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveHostWindowTitle")
    protected abstract static class PrimHostWindowTitleNode extends AbstractHostWindowPrimitiveNode implements TernaryPrimitive {

        @Specialization(guards = {"id == DEFAULT_HOST_WINDOW_ID", "title.isByteType()"})
        @TruffleBoundary
        protected static final Object doTitle(final Object receiver, @SuppressWarnings("unused") final long id, final NativeObject title,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
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
