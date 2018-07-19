package de.hpi.swa.graal.squeak.nodes;

import java.util.Arrays;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ValueProfile;

import de.hpi.swa.graal.squeak.image.SqueakImageChunk;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.EmptyObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.WeakPointersObject;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.MiscellaneousPrimitives.SimulationPrimitiveNode;

public abstract class FillInNode extends Node {
    private final ValueProfile byteType = ValueProfile.createClassProfile();
    private final SqueakImageContext image;

    public static FillInNode create(final SqueakImageContext image) {
        return FillInNodeGen.create(image);
    }

    protected FillInNode(final SqueakImageContext image) {
        this.image = image;
    }

    public abstract void execute(Object obj, SqueakImageChunk chunk);

    @Specialization
    protected static final void doBlockClosure(final BlockClosureObject obj, final SqueakImageChunk chunk) {
        obj.fillin(chunk);
    }

    @Specialization
    protected static final void doClassObj(final ClassObject obj, final SqueakImageChunk chunk) {
        obj.fillin(chunk);
    }

    @Specialization
    protected static final void doCompiledCodeObj(final CompiledCodeObject obj, final SqueakImageChunk chunk) {
        obj.fillin(chunk);
    }

    @Specialization
    protected static final void doContext(final ContextObject obj, final SqueakImageChunk chunk) {
        obj.fillin(chunk);
    }

    @Specialization
    protected static final void doEmpty(final EmptyObject obj, final SqueakImageChunk chunk) {
        obj.fillinHashAndClass(chunk);
    }

    @Specialization
    protected static final void doFloat(final FloatObject obj, final SqueakImageChunk chunk) {
        obj.fillinHashAndClass(chunk);
    }

    @Specialization
    protected static final void doLargeInteger(final LargeIntegerObject obj, final SqueakImageChunk chunk) {
        obj.fillinHashAndClass(chunk);
    }

    @Specialization(guards = "!obj.isByteType() || chunk.image.getSimulatePrimitiveArgsSelector() != null")
    protected static final void doNativeObj(final NativeObject obj, final SqueakImageChunk chunk) {
        obj.fillin(chunk);
    }

    @Specialization(guards = {"!chunk.image.config.isTesting()", "chunk.image.getSimulatePrimitiveArgsSelector() == null", "obj.isByteType()"})
    protected final void doNativeByte(final NativeObject obj, final SqueakImageChunk chunk) {
        obj.fillin(chunk);
        final byte[] stringBytes = obj.getByteStorage(byteType);
        if (Arrays.equals(SimulationPrimitiveNode.SIMULATE_PRIMITIVE_SELECTOR, stringBytes)) {
            image.setSimulatePrimitiveArgsSelector(obj);
        }
    }

    @Specialization(guards = {"chunk.image.config.isTesting()", "chunk.image.getSimulatePrimitiveArgsSelector() == null", "obj.isByteType()"})
    protected final void doNativeByteTesting(final NativeObject obj, final SqueakImageChunk chunk) {
        obj.fillin(chunk);
        final byte[] stringBytes = obj.getByteStorage(byteType);
        if (Arrays.equals(SqueakImageContext.AS_SYMBOL_SELECTOR_NAME, stringBytes)) {
            image.setAsSymbolSelector(obj);
        } else if (Arrays.equals(SimulationPrimitiveNode.SIMULATE_PRIMITIVE_SELECTOR, stringBytes)) {
            image.setSimulatePrimitiveArgsSelector(obj);
        }
    }

    @Specialization
    protected static final void doNil(final NilObject obj, final SqueakImageChunk chunk) {
        obj.fillinHashAndClass(chunk);
    }

    @Specialization
    protected static final void doPointers(final PointersObject obj, final SqueakImageChunk chunk) {
        obj.fillin(chunk);
    }

    @Specialization
    protected static final void doWeakPointers(final WeakPointersObject obj, final SqueakImageChunk chunk) {
        obj.fillin(chunk);
    }

    @SuppressWarnings("unused")
    @Specialization
    protected static final void doBoolean(final boolean obj, final SqueakImageChunk chunk) {
        // do nothing
    }
}
