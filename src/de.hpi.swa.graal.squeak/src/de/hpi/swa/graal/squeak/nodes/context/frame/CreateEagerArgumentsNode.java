package de.hpi.swa.graal.squeak.nodes.context.frame;

import java.util.Arrays;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.model.NotProvided;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;

@GenerateUncached
public abstract class CreateEagerArgumentsNode extends AbstractNode {
    public static CreateEagerArgumentsNode create() {
        return CreateEagerArgumentsNodeGen.create();
    }

    public abstract Object[] executeCreate(int size, Object[] receiverAndArguments);

    @Specialization(guards = {"receiverAndArguments.length >= size"})
    protected static final Object[] doReturn(@SuppressWarnings("unused") final int size, final Object[] receiverAndArguments) {
        return receiverAndArguments;
    }

    @Fallback
    protected static final Object[] doResize(final int size, final Object[] receiverAndArguments) {
        final Object[] array = Arrays.copyOf(receiverAndArguments, size);
        Arrays.fill(array, receiverAndArguments.length, size, NotProvided.SINGLETON);
        return array;
    }
}
