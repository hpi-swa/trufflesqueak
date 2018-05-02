package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ListObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.graal.squeak.nodes.context.stack.StackPushNode;

public abstract class HandlePrimitiveFailedNode extends AbstractNodeWithCode {
    @CompilationFinal private ListObject errorTable;
    @Child private StackPushNode pushNode;

    public static HandlePrimitiveFailedNode create(final CompiledCodeObject code) {
        return HandlePrimitiveFailedNodeGen.create(code);
    }

    public HandlePrimitiveFailedNode(final CompiledCodeObject code) {
        super(code);
        pushNode = StackPushNode.create(code);
    }

    public abstract void executeHandle(VirtualFrame frame, PrimitiveFailed e);

    /*
     * Look up error symbol in error table and push it to stack. The fallback code pops the error
     * symbol into the corresponding temporary variable.
     */
    @Specialization(guards = "followedByExtendedStore()")
    protected final void doHandle(final VirtualFrame frame, final PrimitiveFailed e) {
        pushNode.executeWrite(frame, getErrorTable().at0(e.getReasonCode()));
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "!followedByExtendedStore()")
    protected static final void doNothing(final VirtualFrame frame, final PrimitiveFailed e) {
        // do nothing
    }

    protected final boolean followedByExtendedStore() {
        // fourth bytecode indicates extended store after callPrimitive
        return code.getBytes()[3] == (byte) 0x81;
    }

    private ListObject getErrorTable() {
        if (errorTable == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            errorTable = ((ListObject) code.image.specialObjectsArray.at0(SPECIAL_OBJECT_INDEX.PrimErrTableIndex));
        }
        return errorTable;
    }
}
