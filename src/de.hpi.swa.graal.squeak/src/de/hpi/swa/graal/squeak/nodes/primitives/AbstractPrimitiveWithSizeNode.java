package de.hpi.swa.graal.squeak.nodes.primitives;

import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.nodes.SqueakGuards;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectSizeNode;

public abstract class AbstractPrimitiveWithSizeNode extends AbstractPrimitiveNode {
    @Child protected SqueakObjectSizeNode sizeNode = SqueakObjectSizeNode.create();

    protected AbstractPrimitiveWithSizeNode(final CompiledMethodObject method, final int numArguments) {
        super(method, numArguments);
    }

    protected final boolean inBounds(final long index, final AbstractSqueakObject object) {
        return SqueakGuards.inBounds1(index, sizeNode.execute(object));
    }
}
