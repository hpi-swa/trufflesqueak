/*
 * Copyright (c) 2017-2021 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.shared.SqueakLanguageConfig;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

@ImportStatic({BooleanObject.class, FrameAccess.class, SqueakGuards.class})
@TypeSystemReference(SqueakTypes.class)
@NodeInfo(language = SqueakLanguageConfig.ID)
public abstract class AbstractNode extends Node {

    public final SqueakLanguage getLanguage() {
        return SqueakLanguage.get(this);
    }

    public final SqueakImageContext getContext() {
        return SqueakImageContext.get(this);
    }
}
