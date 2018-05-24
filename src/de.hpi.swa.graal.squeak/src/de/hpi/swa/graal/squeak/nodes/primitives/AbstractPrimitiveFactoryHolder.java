package de.hpi.swa.graal.squeak.nodes.primitives;

import java.util.List;

import com.oracle.truffle.api.dsl.NodeFactory;

public abstract class AbstractPrimitiveFactoryHolder {
    public abstract List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories();

    public boolean useSimulationAsFallback() {
        return false;
    }
}
