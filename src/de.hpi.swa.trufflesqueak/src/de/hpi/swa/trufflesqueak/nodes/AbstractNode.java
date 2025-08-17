/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.dsl.Idempotent;
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
import de.hpi.swa.trufflesqueak.nodes.plugins.LargeIntegers;
import de.hpi.swa.trufflesqueak.shared.SqueakLanguageConfig;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

@ImportStatic({BooleanObject.class, CacheLimits.class, FrameAccess.class, SqueakGuards.class, LargeIntegers.class})
@TypeSystemReference(SqueakTypes.class)
@NodeInfo(language = SqueakLanguageConfig.ID)
public abstract class AbstractNode extends Node {

    public final SqueakLanguage getLanguage() {
        return SqueakLanguage.get(this);
    }

    public static SqueakImageContext getContext(final Node node) {
        return SqueakImageContext.get(node);
    }

    public final SqueakImageContext getContext() {
        return getContext(this);
    }

    protected final CompiledCodeObject getCode() {
        return ((AbstractRootNode) getRootNode()).getCode();
    }

    protected final boolean isBitmap(final NativeObject object) {
        return getContext().isBitmapClass(object.getSqueakClass());
    }

    protected final boolean isPoint(final PointersObject object) {
        return getContext().isPoint(object);
    }

    @Idempotent
    protected final boolean isPrimitiveDoMixedArithmetic() {
        return getContext().flags.isPrimitiveDoMixedArithmetic();
    }

    protected final boolean isSemaphore(final PointersObject object) {
        return getContext().isSemaphoreClass(object.getSqueakClass());
    }

}
