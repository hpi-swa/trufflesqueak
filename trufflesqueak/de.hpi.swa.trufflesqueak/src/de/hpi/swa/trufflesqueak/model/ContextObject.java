package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.frame.MaterializedFrame;

import de.hpi.swa.trufflesqueak.SqueakImageContext;

public class ContextObject extends AbstractPointersObject {
    private MaterializedFrame frame;

    public ContextObject(SqueakImageContext img, MaterializedFrame materializedFrame) {
        super(img);
        frame = materializedFrame;
    }
}
