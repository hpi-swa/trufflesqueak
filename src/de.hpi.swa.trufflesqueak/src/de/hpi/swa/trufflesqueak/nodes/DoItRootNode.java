/*
 * Copyright (c) 2017-2022 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2022 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.interop.WrapToSqueakNode;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.NilObject;

public final class DoItRootNode extends RootNode {
    @Child private WrapToSqueakNode wrapNode = WrapToSqueakNode.create();
    private final SqueakImageContext image;
    private final Object maybeClosure;

    public DoItRootNode(final SqueakImageContext image, final TruffleLanguage<?> language, final Object closure) {
        super(language);
        this.image = image;
        maybeClosure = closure;
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        if (!(maybeClosure instanceof BlockClosureObject)) {
            return NilObject.SINGLETON;

        }
        final BlockClosureObject closure = (BlockClosureObject) maybeClosure;
        if (closure.getNumArgs() != frame.getArguments().length) {
            return NilObject.SINGLETON;
        }
        return closure.send(image, "valueWithArguments:", wrapNode.executeWrap(frame.getArguments()));
    }
}
