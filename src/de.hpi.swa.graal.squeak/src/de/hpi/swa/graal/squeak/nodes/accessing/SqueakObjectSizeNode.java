package de.hpi.swa.graal.squeak.nodes.accessing;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.AbstractPointersObject;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledBlockObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.EmptyObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectSizeNode;
import de.hpi.swa.graal.squeak.nodes.accessing.NativeObjectNodes.NativeObjectSizeNode;

@GenerateUncached
public abstract class SqueakObjectSizeNode extends AbstractNode {

    public static SqueakObjectSizeNode create() {
        return SqueakObjectSizeNodeGen.create();
    }

    public abstract int execute(Object obj);

    @Specialization
    protected static final int doNil(@SuppressWarnings("unused") final NilObject obj) {
        return NilObject.size();
    }

    @Specialization
    protected static final int doArray(final ArrayObject obj, @Cached final ArrayObjectSizeNode sizeNode) {
        return sizeNode.execute(obj);
    }

    @Specialization
    protected static final int doClass(final ClassObject obj) {
        return obj.size();
    }

    @Specialization
    protected static final int doContext(final ContextObject obj) {
        return obj.size();
    }

    @Specialization
    protected static final int doAbstractPointers(final AbstractPointersObject obj) {
        return obj.size();
    }

    @Specialization
    protected static final int doClosure(final BlockClosureObject obj) {
        return obj.size();
    }

    @Specialization
    protected static final int doMethod(final CompiledMethodObject obj) {
        return obj.size();
    }

    @Specialization
    protected static final int doBlock(final CompiledBlockObject obj) {
        return obj.size();
    }

    @Specialization
    protected static final int doEmpty(final EmptyObject obj) {
        return obj.size();
    }

    @Specialization
    protected static final int doNative(final NativeObject obj, @Cached final NativeObjectSizeNode sizeNode) {
        return sizeNode.execute(obj);
    }

    @Specialization
    protected static final int doFloat(final FloatObject obj) {
        return obj.size();
    }

    @Specialization
    protected static final int doLargeInteger(final LargeIntegerObject obj) {
        return obj.size();
    }

    @Fallback
    protected static final int doFallback(final Object obj) {
        throw SqueakException.create("Object does not support size:", obj);
    }
}
