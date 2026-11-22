/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import com.oracle.truffle.api.HostCompilerDirectives;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNode.DispatchDirectNaryNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNode.DispatchIndirectNaryNode.CreateFrameArgumentsForIndirectCallNaryNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelectorNaryNode.DispatchIndirectNaryNode.TryPrimitiveNaryNode;

/**
 * Nodes for Newspeak's absent receiver sends and for pushing enclosing objects. The receiver of
 * such sends is not on the stack, but determined by the lookup: it is either the receiver of the
 * method that performs the send, or one of its enclosing objects. Since the lookup only depends on
 * the class of the method's receiver, results are cached per receiver class.
 */
public final class DispatchNewspeakNode {
    private DispatchNewspeakNode() {
    }

    public abstract static class DispatchNewspeakSendNode extends AbstractNode {
        protected final CompiledCodeObject method;
        protected final NativeObject selector;
        protected final int kind;

        DispatchNewspeakSendNode(final CompiledCodeObject code, final NativeObject selector, final int kind) {
            method = code.getMethod();
            this.selector = selector;
            this.kind = kind;
        }

        /**
         * @param methodReceiver the receiver of the method that performs the send
         */
        public abstract Object execute(VirtualFrame frame, Object methodReceiver, Object[] arguments);

        @Specialization(guards = "guard.check(methodReceiver)", assumptions = "dispatchDirectNode.getAssumptions()", limit = "INLINE_METHOD_CACHE_LIMIT")
        protected static final Object doDirect(final VirtualFrame frame, final Object methodReceiver, final Object[] arguments,
                        @SuppressWarnings("unused") @Cached("create(methodReceiver)") final LookupClassGuard guard,
                        @Cached("lookup(guard)") final NewspeakLookup.Result lookup,
                        @Cached("createDispatchNode(lookup)") final DispatchDirectNaryNode dispatchDirectNode) {
            return dispatchDirectNode.execute(frame, lookup.getReceiver(methodReceiver), arguments);
        }

        @NeverDefault
        protected final NewspeakLookup.Result lookup(final LookupClassGuard guard) {
            return NewspeakLookup.lookup(guard.getSqueakClassInternal(null), method, selector, kind);
        }

        @NeverDefault
        protected final DispatchDirectNaryNode createDispatchNode(final NewspeakLookup.Result lookup) {
            return DispatchDirectNaryNode.create(lookup.getAssumptions(), selector, lookup.lookupClass, lookup.lookupResult);
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(replaces = "doDirect")
        @HostCompilerDirectives.InliningCutoff
        @SuppressWarnings("truffle-static-method")
        protected final Object doIndirect(final VirtualFrame frame, final Object methodReceiver, final Object[] arguments,
                        @Bind final Node node,
                        @Bind final SqueakImageContext image,
                        @Cached(inline = true) final SqueakObjectClassNode classNode,
                        @Cached final ResolveMethodNode methodNode,
                        @Cached final TryPrimitiveNaryNode tryPrimitiveNode,
                        @Cached final CreateFrameArgumentsForIndirectCallNaryNode argumentsNode,
                        @Cached final IndirectCallNode callNode) {
            final ClassObject receiverClass = classNode.executeLookup(node, methodReceiver);
            final NewspeakLookup.Result lookup = image.newspeakLookupCache.lookup(receiverClass, method, selector, kind);
            final Object receiver = lookup.getReceiver(methodReceiver);
            final CompiledCodeObject targetMethod = methodNode.execute(node, image, arguments.length, false, selector, lookup.lookupClass, lookup.lookupResult);
            final Object result = tryPrimitiveNode.execute(frame, targetMethod, receiver, arguments);
            if (result != null) {
                return result;
            } else {
                return callNode.call(targetMethod.getCallTarget(), argumentsNode.execute(node, frame, receiver, arguments, lookup.lookupClass, lookup.lookupResult, selector));
            }
        }

        @Override
        public final String toString() {
            return "newspeakSend: " + selector.toString();
        }
    }

    /** See StackInterpreter>>enclosingObjectAt:withObject:withMixin:. */
    public abstract static class NewspeakEnclosingObjectNode extends AbstractNode {
        protected final CompiledCodeObject method;
        protected final int depth;

        NewspeakEnclosingObjectNode(final CompiledCodeObject code, final int depth) {
            method = code.getMethod();
            this.depth = depth;
        }

        public abstract Object execute(Object methodReceiver);

        @Specialization(guards = "guard.check(methodReceiver)", assumptions = "lookup.getAssumptions()", limit = "INLINE_METHOD_CACHE_LIMIT")
        protected static final Object doCached(final Object methodReceiver,
                        @SuppressWarnings("unused") @Cached("create(methodReceiver)") final LookupClassGuard guard,
                        @Cached("lookup(guard)") final NewspeakLookup.Result lookup) {
            return lookup.getReceiver(methodReceiver);
        }

        @NeverDefault
        protected final NewspeakLookup.Result lookup(final LookupClassGuard guard) {
            return NewspeakLookup.lookupEnclosingObject(guard.getSqueakClassInternal(null), method, depth);
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(replaces = "doCached")
        @SuppressWarnings("truffle-static-method")
        protected final Object doUncached(final Object methodReceiver,
                        @Bind final Node node,
                        @Bind final SqueakImageContext image,
                        @Cached(inline = true) final SqueakObjectClassNode classNode) {
            final ClassObject receiverClass = classNode.executeLookup(node, methodReceiver);
            return image.newspeakLookupCache.lookup(receiverClass, method, null, depth).getReceiver(methodReceiver);
        }
    }
}
