/*
 * Copyright (c) 2017-2022 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2022 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.LoopConditionProfile;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
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

    protected final boolean isBitmap(final NativeObject object) {
        return getContext().isBitmapClass(object.getSqueakClass());
    }

    protected final boolean isPoint(final PointersObject object) {
        return getContext().isPointClass(object.getSqueakClass());
    }

    protected final boolean isSemaphore(final PointersObject object) {
        return getContext().isSemaphoreClass(object.getSqueakClass());
    }

    protected final void profileAndReportLoopCount(final LoopConditionProfile loopProfile, final int count) {
        profileAndReportLoopCount(this, loopProfile, count);
    }

    public static final void profileAndReportLoopCount(final Node node, final LoopConditionProfile loopProfile, final int count) {
        loopProfile.profileCounted(count);
        LoopNode.reportLoopCount(node, count);
    }
}
