package de.hpi.swa.graal.squeak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitiveWithoutFallback;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;

public final class SecurityPlugin extends AbstractPrimitiveFactoryHolder {
    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveCanWriteImage")
    protected abstract static class PrimCanWriteImageNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        protected PrimCanWriteImageNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected final Object doCanWrite(@SuppressWarnings("unused") final Object receiver) {
            return method.image.asBoolean(method.image.env.getCurrentWorkingDirectory().isWritable());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDisableImageWrite")
    protected abstract static class PrimDisableImageWriteNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        protected PrimDisableImageWriteNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object doDisable(@SuppressWarnings("unused") final Object receiver) {
            throw new PrimitiveFailed(); // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetSecureUserDirectory")
    protected abstract static class PrimGetSecureUserDirectoryNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        protected PrimGetSecureUserDirectoryNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object doGet(@SuppressWarnings("unused") final Object receiver) {
            throw new PrimitiveFailed(); // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetUntrustedUserDirectory")
    protected abstract static class PrimGetUntrustedUserDirectoryNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        protected PrimGetUntrustedUserDirectoryNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object doGet(@SuppressWarnings("unused") final Object receiver) {
            throw new PrimitiveFailed(); // TODO: implement primitive
        }
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return SecurityPluginFactory.getFactories();
    }
}
