package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.CompiledBlockObject;
import de.hpi.swa.graal.squeak.model.FloatObject;

public class AbstractSqueakObjectNode extends Node {

    protected boolean isFloat(final AbstractSqueakObject obj) {
        return obj instanceof FloatObject;
    }

    protected boolean isBlock(final AbstractSqueakObject obj) {
        return obj instanceof CompiledBlockObject;
    }

}
