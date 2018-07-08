package de.hpi.swa.graal.squeak.nodes.context;

import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.NativeObject;

@ReportPolymorphism
public abstract class AbstractObjectAtNode extends Node {

    protected static final boolean isNativeObject(final AbstractSqueakObject object) {
        return object instanceof NativeObject;
    }

    protected static final boolean isContextObject(final Object value) {
        return value instanceof ContextObject;
    }
}
