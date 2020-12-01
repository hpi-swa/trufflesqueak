/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.context.frame;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetOrCreateContextNodeFactory.GetOrCreateContextFromActiveProcessNodeGen;
import de.hpi.swa.trufflesqueak.nodes.context.frame.GetOrCreateContextNodeFactory.GetOrCreateContextNotFromActiveProcessNodeGen;
import de.hpi.swa.trufflesqueak.nodes.process.GetActiveProcessNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public abstract class GetOrCreateContextNode extends AbstractNode {

    public static GetOrCreateContextNode create(final boolean fromActiveProcess) {
        if (fromActiveProcess) {
            return GetOrCreateContextFromActiveProcessNodeGen.create();
        } else {
            return GetOrCreateContextNotFromActiveProcessNodeGen.create();
        }
    }

    public static final ContextObject getOrCreateFromActiveProcessUncached(final Frame frame) {
        CompilerAsserts.neverPartOfCompilation();
        final CompiledCodeObject code = FrameAccess.getMethodOrBlock(frame);
        final ContextObject context = FrameAccess.getContext(frame, code);
        if (context != null) {
            return context;
        } else {
            final ContextObject result = ContextObject.create(code.getSqueakClass().getImage(), frame.materialize(), code);
            result.setProcess(GetActiveProcessNode.getUncached().execute());
            return result;
        }
    }

    public abstract ContextObject executeGet(VirtualFrame frame);

    @ImportStatic(FrameAccess.class)
    protected abstract static class GetOrCreateContextFromActiveProcessNode extends GetOrCreateContextNode {
        @Specialization(guards = "getContext(frame, code) != null", limit = "1")
        protected static final ContextObject doGet(final VirtualFrame frame,
                        @Cached("getMethodOrBlock(frame)") final CompiledCodeObject code) {
            return FrameAccess.getContext(frame, code);
        }

        @Specialization(guards = "getContext(frame, code) == null", limit = "1")
        protected static final ContextObject doCreate(final VirtualFrame frame,
                        @Cached("getMethodOrBlock(frame)") final CompiledCodeObject code,
                        @Cached final GetActiveProcessNode getActiveProcessNode,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            final ContextObject result = ContextObject.create(image, frame.materialize(), code);
            result.setProcess(getActiveProcessNode.execute());
            return result;
        }
    }

    protected abstract static class GetOrCreateContextNotFromActiveProcessNode extends GetOrCreateContextNode {
        @Specialization
        protected static final ContextObject doCreate(final VirtualFrame frame,
                        @Cached("getMethodOrBlock(frame)") final CompiledCodeObject code,
                        @Cached("createCountingProfile()") final ConditionProfile hasContextProfile,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            final ContextObject context = FrameAccess.getContext(frame, code);
            if (hasContextProfile.profile(context != null)) {
                return context;
            } else {
                return ContextObject.create(image, frame.materialize(), code);
            }
        }
    }
}
