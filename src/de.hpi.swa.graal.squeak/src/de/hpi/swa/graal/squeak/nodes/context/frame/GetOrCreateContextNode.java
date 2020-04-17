/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.context.frame;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.GetOrCreateContextNodeFactory.GetOrCreateContextFromActiveProcessNodeGen;
import de.hpi.swa.graal.squeak.nodes.context.frame.GetOrCreateContextNodeFactory.GetOrCreateContextNotFromActiveProcessNodeGen;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public abstract class GetOrCreateContextNode extends AbstractNode {

    public static GetOrCreateContextNode create(final boolean fromActiveProcess) {
        if (fromActiveProcess) {
            return GetOrCreateContextFromActiveProcessNodeGen.create();
        } else {
            return GetOrCreateContextNotFromActiveProcessNodeGen.create();
        }
    }

    public abstract ContextObject executeGet(Frame frame);

    protected abstract static class GetOrCreateContextFromActiveProcessNode extends GetOrCreateContextNode {
        @Specialization
        protected static final ContextObject doCreate(final VirtualFrame frame,
                        @Cached("getBlockOrMethod(frame)") final CompiledCodeObject code,
                        @Cached final AbstractPointersObjectReadNode readNode,
                        @Cached("createCountingProfile()") final ConditionProfile hasContextProfile,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            final ContextObject context = FrameAccess.getContext(frame, code);
            if (hasContextProfile.profile(context != null)) {
                return context;
            } else {
                final ContextObject result = ContextObject.create(frame.materialize(), code);
                result.setProcess(image.getActiveProcess(readNode));
                return result;
            }
        }
    }

    protected abstract static class GetOrCreateContextNotFromActiveProcessNode extends GetOrCreateContextNode {
        @Specialization
        protected static final ContextObject doCreate(final VirtualFrame frame,
                        @Cached("getBlockOrMethod(frame)") final CompiledCodeObject code,
                        @Cached("createCountingProfile()") final ConditionProfile hasContextProfile) {
            final ContextObject context = FrameAccess.getContext(frame, code);
            if (hasContextProfile.profile(context != null)) {
                return context;
            } else {
                return ContextObject.create(frame.materialize(), code);
            }
        }
    }
}
