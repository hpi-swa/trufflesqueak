package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.image.AbstractImageChunk;
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

public abstract class FillInNode extends Node {

    public static FillInNode create() {
        return FillInNodeGen.create();
    }

    protected FillInNode() {
        super();
    }

    public abstract void execute(VirtualFrame frame, Object obj, AbstractImageChunk chunk);

    @Specialization
    protected void doBlockClosure(final BlockClosureObject obj, final AbstractImageChunk chunk) {
        obj.fillin(chunk);
    }

    @Specialization
    protected void doClassObj(final ClassObject obj, final AbstractImageChunk chunk) {
        obj.fillin(chunk);
    }

    @Specialization
    protected void doCompiledCodeObj(final CompiledCodeObject obj, final AbstractImageChunk chunk) {
        obj.fillin(chunk);
    }

    @Specialization
    protected void doContext(final ContextObject obj, final AbstractImageChunk chunk) {
        obj.fillin(chunk);
    }

    @Specialization
    protected void doEmpty(final EmptyObject obj, final AbstractImageChunk chunk) {
        obj.fillin(chunk);
    }

    @Specialization
    protected void doFloat(final FloatObject obj, final AbstractImageChunk chunk) {
        obj.fillin(chunk);
    }

    @Specialization
    protected void doLargeInteger(final LargeIntegerObject obj, final AbstractImageChunk chunk) {
        obj.fillin(chunk);
    }

    @Specialization
    protected void doNativeObj(final NativeObject obj, final AbstractImageChunk chunk) {
        obj.fillin(chunk);
    }

    @Specialization
    protected void doNil(final NilObject obj, final AbstractImageChunk chunk) {
        obj.fillin(chunk);
    }

    @Specialization
    protected void doPointers(final PointersObject obj, final AbstractImageChunk chunk) {
        obj.fillin(chunk);
    }

    @Specialization
    protected void doWeakPointers(final WeakPointersObject obj, final AbstractImageChunk chunk) {
        obj.fillin(chunk);
    }

    @SuppressWarnings("unused")
    @Specialization
    protected void doGeneric(final Object obj, final AbstractImageChunk chunk) {
        // do nothing
    }
}
