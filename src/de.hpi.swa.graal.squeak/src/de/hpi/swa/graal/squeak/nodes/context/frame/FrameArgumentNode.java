package de.hpi.swa.graal.squeak.nodes.context.frame;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.PrimitiveValueProfile;
import com.oracle.truffle.api.profiles.ValueProfile;

import de.hpi.swa.graal.squeak.model.BaseSqueakObject;
import de.hpi.swa.graal.squeak.nodes.helpers.NotProvided;

public abstract class FrameArgumentNode extends Node {
    @CompilationFinal private final int argumentIndex;
    @CompilationFinal private final ConditionProfile objectProfile = ConditionProfile.createBinaryProfile();
    @CompilationFinal private final ValueProfile primitiveProfile = PrimitiveValueProfile.createEqualityProfile();
    @CompilationFinal private final ValueProfile classProfile = ValueProfile.createClassProfile();

    public static FrameArgumentNode create(final int argumentIndex) {
        return FrameArgumentNodeGen.create(argumentIndex);
    }

    protected FrameArgumentNode(final int argumentIndex) {
        this.argumentIndex = argumentIndex;
    }

    public abstract Object executeRead(VirtualFrame frame);

    @Specialization
    protected Object doArgument(final VirtualFrame frame) {
        final Object value;
        try {
            value = frame.getArguments()[argumentIndex];
        } catch (ArrayIndexOutOfBoundsException e) {
            return NotProvided.INSTANCE;
        }
        if (objectProfile.profile(value instanceof BaseSqueakObject)) {
            return classProfile.profile(value);
        } else {
            return primitiveProfile.profile(value);
        }
    }
}
