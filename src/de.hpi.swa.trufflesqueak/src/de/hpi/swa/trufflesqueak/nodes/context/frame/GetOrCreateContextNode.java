/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.context.frame;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedCountingConditionProfile;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

@GenerateInline
@GenerateCached(true)
public abstract class GetOrCreateContextNode extends AbstractNode {

    @NeverDefault
    public static GetOrCreateContextNode create() {
        return GetOrCreateContextNodeGen.create();
    }

    public static final ContextObject getOrCreateUncached(final Frame frame) {
        CompilerAsserts.neverPartOfCompilation();
        final ContextObject context = FrameAccess.getContext(frame);
        if (context != null) {
            return context;
        } else {
            return ContextObject.create(SqueakImageContext.getSlow(), frame.materialize(), FrameAccess.getCodeObject(frame));
        }
    }

    public abstract ContextObject executeGet(VirtualFrame frame, Node node);

    public final ContextObject executeGet(final VirtualFrame frame) {
        return executeGet(frame, null);
    }

    @Specialization
    protected static final ContextObject doGetOrCreate(final VirtualFrame frame, final Node node,
                    @Cached(value = "getCodeObject(frame)", neverDefault = true) final CompiledCodeObject code,
                    @Cached final InlinedCountingConditionProfile hasContextProfile) {
        final ContextObject context = FrameAccess.getContext(frame);
        if (hasContextProfile.profile(node, context != null)) {
            return context;
        } else {
            return ContextObject.create(getContext(node), frame.materialize(), code);
        }
    }
}
