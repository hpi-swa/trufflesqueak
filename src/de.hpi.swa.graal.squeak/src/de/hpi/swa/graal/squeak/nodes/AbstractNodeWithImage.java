package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;

@ReportPolymorphism
@TypeSystemReference(SqueakTypes.class)
public abstract class AbstractNodeWithImage extends Node {
    protected final SqueakImageContext image;

    protected AbstractNodeWithImage(final SqueakImageContext image) {
        this.image = image;
    }
}
