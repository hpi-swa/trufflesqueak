/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.primitives;

import java.util.List;

import com.oracle.truffle.api.dsl.NodeFactory;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;

public abstract class AbstractPrimitiveFactoryHolder {
    public abstract List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories();

    public boolean isEnabled(@SuppressWarnings("unused") final SqueakImageContext image) {
        return true;
    }
}
