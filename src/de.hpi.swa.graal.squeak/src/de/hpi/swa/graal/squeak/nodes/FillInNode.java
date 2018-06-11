package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.image.AbstractImageChunk;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
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

    private final SqueakImageContext image;

    public static FillInNode create(final SqueakImageContext image) {
        return FillInNodeGen.create(image);
    }

    protected FillInNode(final SqueakImageContext image) {
        super();
        this.image = image;
    }

    public abstract void execute(VirtualFrame frame, Object obj, AbstractImageChunk chunk);

    @Specialization(guards = "chunk.getFormat() == 3")
    protected void doBlockClosure(final BlockClosureObject obj, final AbstractImageChunk chunk) {
        obj.fillin(chunk);
    }

    @Specialization
    protected void doClassObj(final ClassObject obj, final AbstractImageChunk chunk) {
        obj.fillin(chunk);
    }

    @Specialization(guards = {"chunk.getFormat() > 23", "chunk.getFormat() <= 31"})
    protected void doCompiledCodeObj(final CompiledCodeObject obj, final AbstractImageChunk chunk) {
        obj.fillin(chunk);
    }

    @Specialization(guards = "chunk.getFormat() == 3")
    protected void doContext(final ContextObject obj, final AbstractImageChunk chunk) {
        obj.fillin(chunk);
    }

    @Specialization(guards = "chunk.getFormat() == 0")
    protected void doEmpty(final EmptyObject obj, final AbstractImageChunk chunk) {
        obj.fillin(chunk);
    }

    @Specialization(guards = {"chunk.getFormat() > 9", "chunk.getFormat() <= 11"})
    protected void doFloat(final FloatObject obj, final AbstractImageChunk chunk) {
        obj.fillin(chunk);
    }

    @Specialization(guards = {"chunk.getFormat() > 15", "chunk.getFormat() <= 23"})
    protected void doLargeInteger(final LargeIntegerObject obj, final AbstractImageChunk chunk) {
        obj.fillin(chunk);
    }

    @Specialization(guards = {"chunk.getFormat() >= 9", "chunk.getFormat() <= 23"})
    protected void doNativeObj(final NativeObject obj, final AbstractImageChunk chunk) {
        obj.fillin(chunk);
        if (obj.isByteType()) {
            final String stringValue = obj.asString();
            if ("asSymbol".equals(stringValue)) {
                image.asSymbol = obj;
            } else if (SimulationPrimitiveNode.SIMULATE_PRIMITIVE_SELECTOR.equals(stringValue)) {
                image.simulatePrimitiveArgs = obj;
            }
        }
    }

    @Specialization
    protected void doNil(final NilObject obj, final AbstractImageChunk chunk) {
        obj.fillin(chunk);
    }

    @Specialization(guards = {"chunk.getFormat() >= 1", "chunk.getFormat() <= 5", "chunk.getFormat() != 4"})
    protected void doPointers(final PointersObject obj, final AbstractImageChunk chunk) {
        obj.fillin(chunk);
    }

    @Specialization(guards = "chunk.getFormat() == 4")
    protected void doWeakPointers(final WeakPointersObject obj, final AbstractImageChunk chunk) {
        obj.fillin(chunk);
    }

    @Specialization
    protected void doAbstractSqueakObj(final AbstractSqueakObject obj, final AbstractImageChunk chunk) {
        obj.fillin(chunk);
    }

    @Specialization
    protected void doGenericObj(final Object obj, final AbstractImageChunk chunk) {
        if (obj instanceof AbstractSqueakObject) {
            ((AbstractSqueakObject) obj).fillin(chunk);
        }
        if (obj instanceof NativeObject) {
            final NativeObject nativeObj = (NativeObject) obj;
            if (nativeObj.isByteType()) {
                final String stringValue = nativeObj.asString();
                if ("asSymbol".equals(stringValue)) {
                    image.asSymbol = nativeObj;
                } else if (SimulationPrimitiveNode.SIMULATE_PRIMITIVE_SELECTOR.equals(stringValue)) {
                    image.simulatePrimitiveArgs = nativeObj;
                }
            }
        }
    }
}
