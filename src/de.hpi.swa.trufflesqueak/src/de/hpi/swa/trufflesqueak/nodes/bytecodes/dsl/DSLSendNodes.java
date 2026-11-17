/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.bytecodes.dsl;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector0Node.Dispatch0Node;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector0NodeFactory.Dispatch0NodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector1Node.Dispatch1Node;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector1NodeFactory.Dispatch1NodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector2Node.Dispatch2Node;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector2NodeFactory.Dispatch2NodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNode.DispatchDirectedSuperNaryNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNode.DispatchIndirectNaryNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNode.DispatchNaryNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNode.DispatchSuperNaryNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNodeFactory.DispatchDirectedSuperNaryNodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNodeFactory.DispatchNaryNodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNodeFactory.DispatchSuperNaryNodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchValueNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchValueNodeGen;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchValueWithArgNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchValueWithArgNodeGen;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;

/**
 * Send nodes for the Bytecode DSL interpreter. Each node wraps the regular dispatch nodes (with
 * inline caches) for the cached interpreter tier and falls back to an indirect dispatch in the
 * uncached tier.
 */
public final class DSLSendNodes {
    private DSLSendNodes() {
    }

    @GenerateInline(false)
    @GenerateUncached
    public abstract static class DSLSend0Node extends AbstractNode {
        public abstract Object execute(VirtualFrame frame, NativeObject selector, Object receiver);

        @Specialization(excludeForUncached = true)
        protected static final Object doCached(final VirtualFrame frame, @SuppressWarnings("unused") final NativeObject selector, final Object receiver,
                        @Cached("create0(selector)") final Dispatch0Node dispatchNode) {
            return dispatchNode.execute(frame, receiver);
        }

        @Specialization(replaces = "doCached")
        protected static final Object doUncached(final VirtualFrame frame, final NativeObject selector, final Object receiver,
                        @Cached final DispatchIndirectNaryNode dispatchNode) {
            return dispatchNode.execute(frame, false, selector, receiver, ArrayUtils.EMPTY_ARRAY);
        }

        @NeverDefault
        protected static final Dispatch0Node create0(final NativeObject selector) {
            return Dispatch0NodeGen.create(selector);
        }
    }

    @GenerateInline(false)
    @GenerateUncached
    public abstract static class DSLSend1Node extends AbstractNode {
        public abstract Object execute(VirtualFrame frame, NativeObject selector, Object receiver, Object arg1);

        @Specialization(excludeForUncached = true)
        protected static final Object doCached(final VirtualFrame frame, @SuppressWarnings("unused") final NativeObject selector, final Object receiver, final Object arg1,
                        @Cached("create1(selector)") final Dispatch1Node dispatchNode) {
            return dispatchNode.execute(frame, receiver, arg1);
        }

        @Specialization(replaces = "doCached")
        protected static final Object doUncached(final VirtualFrame frame, final NativeObject selector, final Object receiver, final Object arg1,
                        @Cached final DispatchIndirectNaryNode dispatchNode) {
            return dispatchNode.execute(frame, false, selector, receiver, new Object[]{arg1});
        }

        @NeverDefault
        protected static final Dispatch1Node create1(final NativeObject selector) {
            return Dispatch1NodeGen.create(selector);
        }
    }

    @GenerateInline(false)
    @GenerateUncached
    public abstract static class DSLSend2Node extends AbstractNode {
        public abstract Object execute(VirtualFrame frame, NativeObject selector, Object receiver, Object arg1, Object arg2);

        @Specialization(excludeForUncached = true)
        protected static final Object doCached(final VirtualFrame frame, @SuppressWarnings("unused") final NativeObject selector, final Object receiver, final Object arg1, final Object arg2,
                        @Cached("create2(selector)") final Dispatch2Node dispatchNode) {
            return dispatchNode.execute(frame, receiver, arg1, arg2);
        }

        @Specialization(replaces = "doCached")
        protected static final Object doUncached(final VirtualFrame frame, final NativeObject selector, final Object receiver, final Object arg1, final Object arg2,
                        @Cached final DispatchIndirectNaryNode dispatchNode) {
            return dispatchNode.execute(frame, false, selector, receiver, new Object[]{arg1, arg2});
        }

        @NeverDefault
        protected static final Dispatch2Node create2(final NativeObject selector) {
            return Dispatch2NodeGen.create(selector);
        }
    }

    @GenerateInline(false)
    @GenerateUncached
    public abstract static class DSLSendNaryNode extends AbstractNode {
        public abstract Object execute(VirtualFrame frame, NativeObject selector, Object receiver, Object[] arguments);

        @Specialization(excludeForUncached = true)
        protected static final Object doCached(final VirtualFrame frame, @SuppressWarnings("unused") final NativeObject selector, final Object receiver, final Object[] arguments,
                        @Cached("createNary(selector)") final DispatchNaryNode dispatchNode) {
            return dispatchNode.execute(frame, receiver, arguments);
        }

        @Specialization(replaces = "doCached")
        protected static final Object doUncached(final VirtualFrame frame, final NativeObject selector, final Object receiver, final Object[] arguments,
                        @Cached final DispatchIndirectNaryNode dispatchNode) {
            return dispatchNode.execute(frame, false, selector, receiver, arguments);
        }

        @NeverDefault
        protected static final DispatchNaryNode createNary(final NativeObject selector) {
            return DispatchNaryNodeGen.create(selector);
        }
    }

    /** Super sends are always executed with caches (the operation forces the cached tier). */
    @GenerateInline(false)
    public abstract static class DSLSuperSendNode extends AbstractNode {
        public abstract Object execute(VirtualFrame frame, CompiledCodeObject code, NativeObject selector, Object receiver, Object[] arguments);

        @Specialization
        protected static final Object doCached(final VirtualFrame frame, @SuppressWarnings("unused") final CompiledCodeObject code, @SuppressWarnings("unused") final NativeObject selector,
                        final Object receiver, final Object[] arguments,
                        @Cached("createSuper(code, selector)") final DispatchSuperNaryNode dispatchNode) {
            return dispatchNode.execute(frame, receiver, arguments);
        }

        @NeverDefault
        protected static final DispatchSuperNaryNode createSuper(final CompiledCodeObject code, final NativeObject selector) {
            return DispatchSuperNaryNodeGen.create(code, selector);
        }
    }

    @GenerateInline(false)
    public abstract static class DSLDirectedSuperSendNode extends AbstractNode {
        public abstract Object execute(VirtualFrame frame, NativeObject selector, ClassObject lookupClass, Object receiver, Object[] arguments);

        @Specialization
        protected static final Object doCached(final VirtualFrame frame, @SuppressWarnings("unused") final NativeObject selector, final ClassObject lookupClass, final Object receiver,
                        final Object[] arguments,
                        @Cached("createDirectedSuper(selector)") final DispatchDirectedSuperNaryNode dispatchNode) {
            return dispatchNode.execute(frame, lookupClass, receiver, arguments);
        }

        @NeverDefault
        protected static final DispatchDirectedSuperNaryNode createDirectedSuper(final NativeObject selector) {
            return DispatchDirectedSuperNaryNodeGen.create(selector);
        }
    }

    @GenerateInline(false)
    @GenerateUncached
    public abstract static class DSLSendValueNode extends AbstractNode {
        public abstract Object execute(VirtualFrame frame, NativeObject selector, Object receiver);

        @Specialization(excludeForUncached = true)
        protected static final Object doCached(final VirtualFrame frame, @SuppressWarnings("unused") final NativeObject selector, final Object receiver,
                        @Cached("createValue()") final DispatchValueNode dispatchNode) {
            return dispatchNode.execute(frame, receiver);
        }

        @Specialization(replaces = "doCached")
        protected static final Object doUncached(final VirtualFrame frame, final NativeObject selector, final Object receiver,
                        @Cached final DispatchIndirectNaryNode dispatchNode) {
            return dispatchNode.execute(frame, false, selector, receiver, ArrayUtils.EMPTY_ARRAY);
        }

        @NeverDefault
        protected static final DispatchValueNode createValue() {
            return DispatchValueNodeGen.create();
        }
    }

    @GenerateInline(false)
    @GenerateUncached
    public abstract static class DSLSendValueWithArgNode extends AbstractNode {
        public abstract Object execute(VirtualFrame frame, NativeObject selector, Object receiver, Object arg1);

        @Specialization(excludeForUncached = true)
        protected static final Object doCached(final VirtualFrame frame, @SuppressWarnings("unused") final NativeObject selector, final Object receiver, final Object arg1,
                        @Cached("createValueWithArg()") final DispatchValueWithArgNode dispatchNode) {
            return dispatchNode.execute(frame, receiver, arg1);
        }

        @Specialization(replaces = "doCached")
        protected static final Object doUncached(final VirtualFrame frame, final NativeObject selector, final Object receiver, final Object arg1,
                        @Cached final DispatchIndirectNaryNode dispatchNode) {
            return dispatchNode.execute(frame, false, selector, receiver, new Object[]{arg1});
        }

        @NeverDefault
        protected static final DispatchValueWithArgNode createValueWithArg() {
            return DispatchValueWithArgNodeGen.create();
        }
    }
}
