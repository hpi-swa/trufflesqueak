/*
 * Copyright (c) 2017-2023 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2023 Oracle and/or its affiliates
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
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
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

    public static final SqueakImageContext getContext(final Node node) {
        return SqueakImageContext.get(node);
    }

    protected final CompiledCodeObject getCode() {
        return ((AbstractRootNode) getRootNode()).getCode();
    }

    protected final boolean isBitmap(final NativeObject object) {
        return getContext().isBitmapClass(object.getSqueakClass());
    }

    protected final boolean isPoint(final PointersObject object) {
        return getContext().isPointClass(object.getSqueakClass());
    }

    protected final boolean isSemaphore(final PointersObject object) {
        return getContext().isSemaphoreClass(object.getSqueakClass());
    }
}
