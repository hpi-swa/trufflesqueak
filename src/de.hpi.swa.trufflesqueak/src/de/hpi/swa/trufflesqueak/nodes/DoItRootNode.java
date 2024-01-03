/*
 * Copyright (c) 2017-2024 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2024 Oracle and/or its affiliates
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

public abstract class DoItRootNode extends RootNode {
    private final SqueakImageContext image;
    private final Object maybeClosure;

    protected DoItRootNode(final SqueakImageContext image, final TruffleLanguage<?> language, final Object closure) {
        super(language);
        this.image = image;
        maybeClosure = closure;
    }

    public static DoItRootNode create(final SqueakImageContext image, final SqueakLanguage language, final Object closure) {
        return DoItRootNodeGen.create(image, (TruffleLanguage<?>) language, closure);
    }

    @Specialization
    protected final Object doIt(final VirtualFrame frame,
                    @Bind("this") final Node node,
                    @Cached final WrapToSqueakNode wrapNode) {
        if (!(maybeClosure instanceof final BlockClosureObject closure)) {
            return NilObject.SINGLETON;

        }
        if (closure.getNumArgs() != frame.getArguments().length) {
            return NilObject.SINGLETON;
        }
        return closure.send(image, "valueWithArguments:", wrapNode.executeWrap(node, frame.getArguments()));
    }
}
