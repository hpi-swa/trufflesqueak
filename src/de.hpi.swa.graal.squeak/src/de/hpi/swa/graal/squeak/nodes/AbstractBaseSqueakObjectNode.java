package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.CompiledBlockObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.WeakPointersObject;

public class AbstractBaseSqueakObjectNode extends Node {

    protected boolean isContext(final AbstractSqueakObject obj) {
        return obj instanceof ContextObject;
    }

    protected boolean isWeakPointers(final AbstractSqueakObject obj) {
        return obj instanceof WeakPointersObject;
    }

    protected boolean isFloat(final AbstractSqueakObject obj) {
        return obj instanceof FloatObject;
    }

    protected boolean isBlock(final AbstractSqueakObject obj) {
        return obj instanceof CompiledBlockObject;
    }

}
