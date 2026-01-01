/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.interop.WrapToSqueakNode;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.BlockClosurePrimitives.PrimFullClosureValueWithArgsNode;

public abstract class DoItRootNode extends RootNode {
    private final SqueakImageContext image;
    private final BlockClosureObject blockClosure;

    protected DoItRootNode(final SqueakImageContext image, final TruffleLanguage<?> language, final BlockClosureObject closure) {
        super(language);
        this.image = image;
        blockClosure = closure;
    }

    public static DoItRootNode create(final SqueakImageContext image, final SqueakLanguage language, final BlockClosureObject closure) {
        return DoItRootNodeGen.create(image, (TruffleLanguage<?>) language, closure);
    }

    @Specialization
    protected final Object doIt(final VirtualFrame frame,
                    @Bind final Node node,
                    @Cached final WrapToSqueakNode wrapNode,
                    @Cached final PrimFullClosureValueWithArgsNode primitiveNode) {
        if (blockClosure.getNumArgs() != frame.getArguments().length) {
            return NilObject.SINGLETON;
        }
        final boolean wasActive = image.interrupt.deactivate();
        try {
            return primitiveNode.execute(image.externalSenderFrame, blockClosure, wrapNode.executeWrap(node, frame.getArguments()));
        } finally {
            image.interrupt.reactivate(wasActive);
        }
    }
}
