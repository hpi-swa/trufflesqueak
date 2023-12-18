/*
 * Copyright (c) 2017-2023 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2023 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;

@GenerateInline
@GenerateCached(false)
public abstract class DispatchClosureNode extends AbstractNode {
    protected static final int INLINE_CACHE_SIZE = 3;

    public abstract Object execute(Node node, BlockClosureObject closure, Object[] arguments);

    @SuppressWarnings("unused")
    @Specialization(guards = {"closure.getCompiledBlock() == cachedBlock"}, assumptions = {"cachedBlock.getCallTargetStable()"}, limit = "INLINE_CACHE_SIZE")
    protected static final Object doDirect(final BlockClosureObject closure, final Object[] arguments,
                    @Cached("closure.getCompiledBlock()") final CompiledCodeObject cachedBlock,
                    @Cached(value = "create(cachedBlock.getCallTarget())", inline = false) final DirectCallNode directCallNode) {
        return directCallNode.call(arguments);
    }

    @ReportPolymorphism.Megamorphic
    @Specialization(replaces = "doDirect")
    protected static final Object doIndirect(final BlockClosureObject closure, final Object[] arguments,
                    @Cached(inline = false) final IndirectCallNode indirectCallNode) {
        return indirectCallNode.call(closure.getCompiledBlock().getCallTarget(), arguments);
    }
}
