package de.hpi.swa.graal.squeak.nodes.accessing;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.model.AbstractPointersObject;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.EmptyObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;

public abstract class SqueakObjectInstSizeNode extends AbstractNode {

    public static SqueakObjectInstSizeNode create() {
        return SqueakObjectInstSizeNodeGen.create();
    }

    public abstract int execute(Object obj);

    @Specialization
    protected static final int doNil(@SuppressWarnings("unused") final NilObject obj) {
        return NilObject.instsize();
    }

    @Specialization
    protected static final int doArray(final ArrayObject obj,
                    @Shared("classNode") @Cached final SqueakObjectClassNode classNode) {
        return classNode.executeClass(obj).getBasicInstanceSize();
    }

    @Specialization
    protected static final int doPointers(final AbstractPointersObject obj,
                    @Shared("classNode") @Cached final SqueakObjectClassNode classNode) {
        return classNode.executeClass(obj).getBasicInstanceSize();
    }

    @Specialization
    protected static final int doClass(final ClassObject obj,
                    @Shared("classNode") @Cached final SqueakObjectClassNode classNode) {
        return classNode.executeClass(obj).getBasicInstanceSize();
    }

    @Specialization
    protected static final int doContext(final ContextObject obj,
                    @Shared("classNode") @Cached final SqueakObjectClassNode classNode) {
        return classNode.executeClass(obj).getBasicInstanceSize();
    }

    @Specialization
    protected static final int doClosure(final BlockClosureObject obj) {
        return obj.instsize();
    }

    @Specialization
    protected static final int doCode(final CompiledCodeObject obj) {
        return obj.instsize();
    }

    @Specialization
    protected static final int doEmpty(final EmptyObject obj) {
        return obj.instsize();
    }

    @Specialization
    protected static final int doNative(final NativeObject obj) {
        return obj.instsize();
    }

    @Specialization
    protected static final int doFloat(final FloatObject obj) {
        return obj.instsize();
    }

    @Specialization
    protected static final int doLarge(final LargeIntegerObject obj) {
        return obj.instsize();
    }
}
