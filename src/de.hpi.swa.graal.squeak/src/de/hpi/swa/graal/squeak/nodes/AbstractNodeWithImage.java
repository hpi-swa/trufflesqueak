package de.hpi.swa.graal.squeak.nodes;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;

public abstract class AbstractNodeWithImage extends AbstractNode {
    protected final SqueakImageContext image;

    protected AbstractNodeWithImage(final SqueakImageContext image) {
        this.image = image;
    }
}
