/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes;

import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;

public abstract class AbstractLookupMethodWithSelectorNode extends AbstractNode {
    public abstract Object executeLookup(ClassObject sqClass);

    protected final Object doUncachedSlow(final ClassObject classObject) {
        return doUncached(classObject, AbstractPointersObjectReadNode.getUncached());
    }

    protected abstract Object doUncached(ClassObject classObject, AbstractPointersObjectReadNode uncached);
}
