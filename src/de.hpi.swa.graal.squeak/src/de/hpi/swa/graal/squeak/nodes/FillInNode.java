package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

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

    private final SqueakImageContext image;

    public static FillInNode create(final SqueakImageContext image) {
        return FillInNodeGen.create(image);
    }

    protected FillInNode(final SqueakImageContext image) {
        super();
        this.image = image;
    }

    public abstract void execute(Object obj, SqueakImageChunk chunk);

    @Specialization
    protected void doBlockClosure(final BlockClosureObject obj, final SqueakImageChunk chunk) {
        obj.fillin(chunk);
    }

    @Specialization
    protected void doClassObj(final ClassObject obj, final SqueakImageChunk chunk) {
        obj.fillin(chunk);
    }

    @Specialization
    protected void doCompiledCodeObj(final CompiledCodeObject obj, final SqueakImageChunk chunk) {
        obj.fillin(chunk);
    }

    @Specialization
    protected void doContext(final ContextObject obj, final SqueakImageChunk chunk) {
        obj.fillin(chunk);
    }

    @Specialization
    protected void doEmpty(final EmptyObject obj, final SqueakImageChunk chunk) {
        obj.fillin(chunk);
    }

    @Specialization
    protected void doFloat(final FloatObject obj, final SqueakImageChunk chunk) {
        obj.fillin(chunk);
    }

    @Specialization
    protected void doLargeInteger(final LargeIntegerObject obj, final SqueakImageChunk chunk) {
        obj.fillin(chunk);
    }

    @Specialization
    protected void doNativeObj(final NativeObject obj, final SqueakImageChunk chunk) {
        obj.fillin(chunk);
        if (obj.isByteType()) {
            final String stringValue = obj.asString();
            if ("asSymbol".equals(stringValue)) {
                image.setAsSymbolSelector(obj);
            } else if (SimulationPrimitiveNode.SIMULATE_PRIMITIVE_SELECTOR.equals(stringValue)) {
                image.setSimulatePrimitiveArgsSelector(obj);
            }
        }
    }

    @Specialization
    protected void doNil(final NilObject obj, final SqueakImageChunk chunk) {
        obj.fillin(chunk);
    }

    @Specialization
    protected void doPointers(final PointersObject obj, final SqueakImageChunk chunk) {
        obj.fillin(chunk);
    }

    @Specialization
    protected void doWeakPointers(final WeakPointersObject obj, final SqueakImageChunk chunk) {
        obj.fillin(chunk);
    }

    @SuppressWarnings("unused")
    @Specialization
    protected void doBoolean(final boolean obj, final SqueakImageChunk chunk) {
        // do nothing
    }
}
