/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

/* Gets context or lazily initializes one if necessary. */
@GenerateInline
@GenerateCached(true)
public abstract class GetOrCreateContextWithoutFrameNode extends AbstractNode {
    @NeverDefault
    public static GetOrCreateContextWithoutFrameNode create() {
        return GetOrCreateContextWithoutFrameNodeGen.create();
    }

    public abstract ContextObject execute(VirtualFrame frame, Node node);

    public final ContextObject execute(final VirtualFrame frame) {
        return execute(frame, this);
    }

    /*
     * No profile: compiled code allocates contexts eagerly (see StartContextRootNode), so the null
     * check folds away wherever that allocation is visible. A profile would only add deopts where
     * compiled code finds a context but the interpreter, which allocates lazily, never did (e.g., in
     * inlined callees). If such a deopt resumes before the callee is entered, the interpreter never
     * updates the profile (deopt cycle). OSR code can still find no context, so keep the check.
     */
    @Specialization
    public static ContextObject getContext(final VirtualFrame frame) {
        final ContextObject context = FrameAccess.getContext(frame);
        return context != null ? context : new ContextObject(frame);
    }
}
