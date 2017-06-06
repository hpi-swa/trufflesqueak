package de.hpi.swa.trufflesqueak.model;

import de.hpi.swa.trufflesqueak.exceptions.NonVirtualContextModification;
import de.hpi.swa.trufflesqueak.util.Chunk;

public interface ActualContextObject {
    public Object at0(int l);

    public void atput0(int idx, Object object);

    public void atContextPut0(int i, Object obj) throws NonVirtualContextModification;

    public void fillin(Chunk chunk);

    public int size();

    public Object getFrameMarker();

    public BaseSqueakObject shallowCopy();
}
