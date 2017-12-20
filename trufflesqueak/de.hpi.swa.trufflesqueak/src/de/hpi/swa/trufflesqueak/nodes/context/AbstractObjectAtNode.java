package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;

public abstract class AbstractObjectAtNode extends Node {

    protected static boolean isNativeObject(BaseSqueakObject object) {
        return object instanceof NativeObject;
    }

}
