package de.hpi.swa.graal.squeak.nodes.context.frame;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.PrimitiveValueProfile;
import com.oracle.truffle.api.profiles.ValueProfile;

import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.NotProvided;

public abstract class FrameArgumentNode extends Node {
    protected final int argumentIndex;
    private final ConditionProfile objectProfile = ConditionProfile.createBinaryProfile();
    private final ValueProfile primitiveProfile = PrimitiveValueProfile.createEqualityProfile();
    private final ValueProfile classProfile = ValueProfile.createClassProfile();

    public static FrameArgumentNode create(final int argumentIndex) {
        return FrameArgumentNodeGen.create(argumentIndex);
    }

    protected FrameArgumentNode(final int argumentIndex) {
        this.argumentIndex = argumentIndex;
    }

    public abstract Object executeRead(VirtualFrame frame);

    @Specialization(guards = "!inBounds(frame)")
    protected static final Object doOutOfBounds(@SuppressWarnings("unused") final VirtualFrame frame) {
        return NotProvided.INSTANCE;
    }

    @Specialization(guards = "inBounds(frame)")
    protected final Object doArgument(final VirtualFrame frame) {
        final Object value = frame.getArguments()[argumentIndex];
        if (objectProfile.profile(value instanceof AbstractSqueakObject)) {
            return classProfile.profile(value);
        } else {
            return primitiveProfile.profile(value);
        }
    }

    protected final boolean inBounds(final VirtualFrame frame) {
        return 0 <= argumentIndex && argumentIndex < frame.getArguments().length;
    }
}
