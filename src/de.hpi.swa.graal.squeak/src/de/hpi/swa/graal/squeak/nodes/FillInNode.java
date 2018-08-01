package de.hpi.swa.graal.squeak.nodes;

import java.util.Arrays;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.image.SqueakImageChunk;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.WeakPointersObject;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.SimulationPrimitiveNode;

public abstract class FillInNode extends Node {
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
        obj.setPointers(chunk.getPointers());
        assert obj.getMethod().sqContextSize() + CONTEXT.TEMP_FRAME_START == obj.getPointers().length : "ContextObject has wrong size";
    }

    @Specialization(guards = "obj.isShortType()")
    protected static final void doNativeShort(final NativeObject obj, final SqueakImageChunk chunk) {
        obj.setStorage(chunk.getShorts());
    }

    @Specialization(guards = "obj.isIntType()")
    protected static final void doNativeInt(final NativeObject obj, final SqueakImageChunk chunk) {
        obj.setStorage(chunk.getInts());
    }

    @Specialization(guards = "obj.isLongType()")
    protected static final void doNativeLong(final NativeObject obj, final SqueakImageChunk chunk) {
        obj.setStorage(chunk.getLongs());
    }

    @Specialization(guards = {"obj.isByteType()", "!chunk.image.config.isTesting()", "chunk.image.getSimulatePrimitiveArgsSelector() != null"})
    protected static final void doNativeByte(final NativeObject obj, final SqueakImageChunk chunk) {
        final byte[] stringBytes = chunk.getBytes();
        obj.setStorage(stringBytes);
    }

    @Specialization(guards = {"obj.isByteType()", "!chunk.image.config.isTesting()", "chunk.image.getSimulatePrimitiveArgsSelector() == null"})
    protected final void doNativeByteAndFindSimulateSelector(final NativeObject obj, final SqueakImageChunk chunk) {
        final byte[] stringBytes = chunk.getBytes();
        obj.setStorage(stringBytes);
        if (Arrays.equals(SimulationPrimitiveNode.SIMULATE_PRIMITIVE_SELECTOR, stringBytes)) {
            image.setSimulatePrimitiveArgsSelector(obj);
        }
    }

    @Specialization(guards = {"obj.isByteType()", "chunk.image.config.isTesting()"})
    protected final void doNativeByteTesting(final NativeObject obj, final SqueakImageChunk chunk) {
        final byte[] stringBytes = chunk.getBytes();
        obj.setStorage(stringBytes);
        if (image.getAsSymbolSelector() == null && Arrays.equals(SqueakImageContext.AS_SYMBOL_SELECTOR_NAME, stringBytes)) {
            image.setAsSymbolSelector(obj);
        } else if (image.getSimulatePrimitiveArgsSelector() == null && Arrays.equals(SimulationPrimitiveNode.SIMULATE_PRIMITIVE_SELECTOR, stringBytes)) {
            image.setSimulatePrimitiveArgsSelector(obj);
        }
    }

    @Specialization
    protected static final void doPointers(final PointersObject obj, final SqueakImageChunk chunk) {
        obj.setPointers(chunk.getPointers());
    }

    @Specialization
    protected static final void doWeakPointers(final WeakPointersObject obj, final SqueakImageChunk chunk) {
        obj.setWeakPointers(chunk.getPointers());
    }

    @SuppressWarnings("unused")
    @Fallback
    protected static final void doNothing(final Object obj, final SqueakImageChunk chunk) {
        // do nothing
    }
}
