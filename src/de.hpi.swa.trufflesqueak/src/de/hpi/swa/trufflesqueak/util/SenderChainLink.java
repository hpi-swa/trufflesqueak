/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.util;

import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.NilObject;

public interface SenderChainLink {
    /**
     * Returns the ContextObject associated with this link, or null if none is directly associated.
     */
    ContextObject getContext();

    /**
     * Returns the next link in the sender chain (could be null, NilObject, FrameMarker, or
     * ContextObject).
     */
    SenderChainLink getNextLink();
}
