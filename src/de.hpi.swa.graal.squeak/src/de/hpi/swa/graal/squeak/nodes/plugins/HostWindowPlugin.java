package de.hpi.swa.graal.squeak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.QuaternaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;

public class HostWindowPlugin extends AbstractPrimitiveFactoryHolder {

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveWindowClose")
    protected abstract static class PrimHostWindowCloseNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimHostWindowCloseNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"method.image.hasDisplay()", "id == 1"})
        protected final Object doClose(final AbstractSqueakObject receiver, @SuppressWarnings("unused") final long id) {
            method.image.getDisplay().close();
            return receiver;
        }

        @Specialization(guards = {"!method.image.hasDisplay()", "id == 1"})
        protected static final Object doCloseHeadless(final AbstractSqueakObject receiver, @SuppressWarnings("unused") final long id) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveHostWindowPosition")
    protected abstract static class PrimHostWindowPositionNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimHostWindowPositionNode(final CompiledMethodObject method) {
            super(method);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"id == 1"})
        protected final Object doSize(final AbstractSqueakObject receiver, final long id) {
            return method.image.asPoint(0L, 0L);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveHostWindowSizeSet")
    protected abstract static class PrimHostWindowSizeSetNode extends AbstractPrimitiveNode implements QuaternaryPrimitive {
        protected PrimHostWindowSizeSetNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"method.image.hasDisplay()", "id == 1"})
        protected final Object doSize(final AbstractSqueakObject receiver, @SuppressWarnings("unused") final long id, final long width, final long height) {
            method.image.getDisplay().resizeTo((int) width, (int) height);
            return receiver;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!method.image.hasDisplay()", "id == 1"})
        protected static final Object doSizeHeadless(final AbstractSqueakObject receiver, final long id, final long width, final long height) {
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveHostWindowTitle")
    protected abstract static class PrimHostWindowTitleNode extends AbstractPrimitiveNode implements TernaryPrimitive {

        protected PrimHostWindowTitleNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"method.image.hasDisplay()", "id == 1", "title.isByteType()"})
        @TruffleBoundary
        protected final Object doTitle(final AbstractSqueakObject receiver, @SuppressWarnings("unused") final long id, final NativeObject title) {
            method.image.getDisplay().setWindowTitle(title.asStringUnsafe());
            return receiver;
        }

        @Specialization(guards = {"!method.image.hasDisplay()", "id == 1", "title.isByteType()"})
        protected static final Object doTitleHeadless(final AbstractSqueakObject receiver, @SuppressWarnings("unused") final long id, @SuppressWarnings("unused") final NativeObject title) {
            return receiver;
        }
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return HostWindowPluginFactory.getFactories();
    }
}
