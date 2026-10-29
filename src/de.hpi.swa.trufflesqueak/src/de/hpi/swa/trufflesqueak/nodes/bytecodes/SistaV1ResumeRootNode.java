/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ContextObject;

/** Re-enters an image context using ordinary Squeak call arguments. */
public final class SistaV1ResumeRootNode extends RootNode {
    private ContextObject context;
    @Child private IndirectCallNode callNode = IndirectCallNode.create();

    public SistaV1ResumeRootNode(final ContextObject context) {
        super(SqueakImageContext.getSlow().getLanguage());
        this.context = context;
    }

    public void setActiveContext(final ContextObject activeContext) {
        context = activeContext;
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        final Object[] arguments = context.getTruffleFrame().getArguments();
        final SmalltalkSistaV1Interpreter root = SmalltalkSistaV1Interpreter.resume(context);
        return callNode.call(root.getCallTarget(), arguments);
    }
}
