package de.hpi.swa.graal.squeak.nodes.plugins;

import java.util.List;
import java.util.UUID;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;

public final class UUIDPlugin extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return UUIDPluginFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveMakeUUID")
    protected abstract static class PrimMakeUUIDNode extends AbstractPrimitiveNode {
        protected PrimMakeUUIDNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        @TruffleBoundary
        protected final Object doUUID(@SuppressWarnings("unused") final Object receiver) {
            return code.image.wrap(UUID.randomUUID().toString());
        }
    }
}
