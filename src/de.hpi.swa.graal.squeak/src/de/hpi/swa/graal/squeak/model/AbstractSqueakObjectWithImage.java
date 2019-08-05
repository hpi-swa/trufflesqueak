package de.hpi.swa.graal.squeak.model;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;

public abstract class AbstractSqueakObjectWithImage extends AbstractSqueakObjectWithHash {
    public final SqueakImageContext image;

    // For special/well-known objects only.
    protected AbstractSqueakObjectWithImage(final SqueakImageContext image) {
        super();
        this.image = image;
    }

    protected AbstractSqueakObjectWithImage(final SqueakImageContext image, final long hash) {
        super(hash);
        this.image = image;
    }
}
