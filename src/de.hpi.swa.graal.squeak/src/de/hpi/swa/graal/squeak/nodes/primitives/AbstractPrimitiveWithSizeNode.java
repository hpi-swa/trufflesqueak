package de.hpi.swa.graal.squeak.nodes.primitives;

import com.oracle.truffle.api.CompilerDirectives;

import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.nodes.SqueakGuards;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectSizeNode;
import de.hpi.swa.graal.squeak.nodes.accessing.NativeObjectNodes.NativeObjectSizeNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectSizeNode;

public abstract class AbstractPrimitiveWithSizeNode extends AbstractPrimitiveNode {
    @Child private SqueakObjectSizeNode sizeNode;
    @Child private ArrayObjectSizeNode arrayObjectSizeNode = ArrayObjectSizeNode.create();
    @Child private NativeObjectSizeNode nativeObjectSizeNode = NativeObjectSizeNode.create();

    protected AbstractPrimitiveWithSizeNode(final CompiledMethodObject method) {
        super(method);
    }

    protected final boolean inBounds(final long index, final ArrayObject object) {
        return SqueakGuards.inBounds1(index, getArrayObjectSizeNode().execute(object));
    }

    protected final boolean inBounds(final long index, final NativeObject object) {
        return SqueakGuards.inBounds1(index, getNativeObjectSizeNode().execute(object));
    }

    protected final boolean inBounds(final long index, final AbstractSqueakObject object) {
        return SqueakGuards.inBounds1(index, getSizeNode().execute(object));
    }

    protected final SqueakObjectSizeNode getSizeNode() {
        if (sizeNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            sizeNode = insert(SqueakObjectSizeNode.create());
        }
        return sizeNode;
    }

    protected final ArrayObjectSizeNode getArrayObjectSizeNode() {
        if (arrayObjectSizeNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            arrayObjectSizeNode = insert(ArrayObjectSizeNode.create());
        }
        return arrayObjectSizeNode;
    }

    protected final NativeObjectSizeNode getNativeObjectSizeNode() {
        if (nativeObjectSizeNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            nativeObjectSizeNode = insert(NativeObjectSizeNode.create());
        }
        return nativeObjectSizeNode;
    }
}
