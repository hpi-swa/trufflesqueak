package de.hpi.swa.trufflesqueak.nodes.context.frame;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.PrimitiveValueProfile;
import com.oracle.truffle.api.profiles.ValueProfile;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;

public abstract class FrameArgumentNode extends Node {
    @CompilationFinal private final int argumentIndex;
    @CompilationFinal private final ConditionProfile objectProfile = ConditionProfile.createBinaryProfile();
    @CompilationFinal private final ValueProfile primitiveProfile = PrimitiveValueProfile.createEqualityProfile();
    @CompilationFinal private final ValueProfile classProfile = ValueProfile.createClassProfile();

    public static FrameArgumentNode create(int argumentIndex) {
        return FrameArgumentNodeGen.create(argumentIndex);
    }

    protected FrameArgumentNode(int argumentIndex) {
        this.argumentIndex = argumentIndex;
    }

    public abstract Object executeRead(VirtualFrame frame);

    @Specialization
    protected Object doArgument(VirtualFrame frame) {
        Object value = frame.getArguments()[argumentIndex];
        if (objectProfile.profile(value instanceof BaseSqueakObject)) {
            return classProfile.profile(value);
        } else {
            return primitiveProfile.profile(value);
        }
    }
}