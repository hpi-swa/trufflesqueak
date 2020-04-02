/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.nodes.GetOrCreateContextNodeFactory.GetOrCreateContextFromActiveProcessNodeGen;
import de.hpi.swa.graal.squeak.nodes.GetOrCreateContextNodeFactory.GetOrCreateContextNotFromActiveProcessNodeGen;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;

public abstract class GetOrCreateContextNode extends AbstractNodeWithCode {

    protected GetOrCreateContextNode(final CompiledCodeObject code) {
        super(code);
    }

    public static GetOrCreateContextNode create(final CompiledCodeObject code, final boolean fromActiveProcess) {
        if (fromActiveProcess) {
            return GetOrCreateContextFromActiveProcessNodeGen.create(code);
        } else {
            return GetOrCreateContextNotFromActiveProcessNodeGen.create(code);
        }
    }

    public abstract ContextObject executeGet(Frame frame);

    @Fallback
    protected final ContextObject doGet(final VirtualFrame frame) {
        return getContext(frame);
    }

    protected abstract static class GetOrCreateContextFromActiveProcessNode extends GetOrCreateContextNode {
        protected GetOrCreateContextFromActiveProcessNode(final CompiledCodeObject code) {
            super(code);
        }

        @Specialization(guards = {"isVirtualized(frame)"})
        protected final ContextObject doCreate(final VirtualFrame frame,
                        @Cached final AbstractPointersObjectReadNode readNode) {
            final ContextObject result = ContextObject.create(frame.materialize(), code);
            result.setProcess(code.image.getActiveProcess(readNode));
            return result;
        }
    }

    protected abstract static class GetOrCreateContextNotFromActiveProcessNode extends GetOrCreateContextNode {
        protected GetOrCreateContextNotFromActiveProcessNode(final CompiledCodeObject code) {
            super(code);
        }

        @Specialization(guards = {"isVirtualized(frame)"})
        protected final ContextObject doCreate(final VirtualFrame frame) {
            return ContextObject.create(frame.materialize(), code);
        }
    }
}
