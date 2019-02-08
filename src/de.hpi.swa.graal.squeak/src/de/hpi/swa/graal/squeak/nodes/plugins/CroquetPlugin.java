package de.hpi.swa.graal.squeak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.graal.squeak.util.ArrayUtils;

public final class CroquetPlugin extends AbstractPrimitiveFactoryHolder {
    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGatherEntropy")
    protected abstract static class PrimGatherEntropyNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimGatherEntropyNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "byteArray.isByteType()")
        protected final Object doGather(@SuppressWarnings("unused") final ClassObject receiver, final NativeObject byteArray) {
            ArrayUtils.fillRandomly(byteArray.getByteStorage());
            return method.image.sqTrue;
        }
    }

    // TODO: implement other primitives?

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return CroquetPluginFactory.getFactories();
    }
}
