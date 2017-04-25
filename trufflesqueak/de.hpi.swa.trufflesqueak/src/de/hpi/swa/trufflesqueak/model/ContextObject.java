package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.frame.MaterializedFrame;

public class ContextObject extends PointersObject {
    private MaterializedFrame frame;

    public ContextObject(MaterializedFrame materializedFrame) {
        frame = materializedFrame;
    }
}
