package de.hpi.swa.graal.squeak.nodes.context.frame;

import java.util.Arrays;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.model.NotProvided;

public abstract class CreateEagerArgumentsNode extends Node {
    public static CreateEagerArgumentsNode create() {
        return CreateEagerArgumentsNodeGen.create();
    }

    public abstract Object[] executeCreate(int size, Object[] receiverAndArguments);

    @Specialization(guards = {"receiverAndArguments.length >= size"})
    Object[] cached(@SuppressWarnings("unused") final int size, final Object[] receiverAndArguments) {
        return receiverAndArguments;
    }

    @Specialization(guards = {"receiverAndArguments.length < size"})
    Object[] uncached(final int size, final Object[] receiverAndArguments) {
        final Object[] array = Arrays.copyOf(receiverAndArguments, size);
        Arrays.fill(array, receiverAndArguments.length, size, NotProvided.INSTANCE);
        return array;
    }
}
