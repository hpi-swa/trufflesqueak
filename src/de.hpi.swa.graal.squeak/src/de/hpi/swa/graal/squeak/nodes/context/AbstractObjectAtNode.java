package de.hpi.swa.graal.squeak.nodes.context;

import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.model.BaseSqueakObject;
import de.hpi.swa.graal.squeak.model.NativeObject;

@ReportPolymorphism
public abstract class AbstractObjectAtNode extends Node {

    protected static boolean isNativeObject(final BaseSqueakObject object) {
        return object instanceof NativeObject;
    }

}
