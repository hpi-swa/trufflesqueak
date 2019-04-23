package de.hpi.swa.graal.squeak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;

public final class NullPlugin extends AbstractPrimitiveFactoryHolder {
    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveUtcWithOffset")
    protected abstract static class PrimUtcWithOffsetNode extends AbstractPrimitiveNode implements UnaryPrimitive {
        protected PrimUtcWithOffsetNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        @TruffleBoundary
        protected final ArrayObject doUTC(@SuppressWarnings("unused") final ClassObject receiver) {
            return method.image.asArrayOfLongs(System.currentTimeMillis() * 1000L, java.time.ZonedDateTime.now().getOffset().getTotalSeconds());
        }
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return NullPluginFactory.getFactories();
    }
}
