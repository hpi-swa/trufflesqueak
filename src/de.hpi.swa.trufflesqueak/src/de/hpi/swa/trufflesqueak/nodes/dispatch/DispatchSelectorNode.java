/*
 * Copyright (c) 2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;

public abstract class DispatchSelectorNode extends AbstractNode {
    public abstract Object execute(VirtualFrame frame);

    public static final DispatchSelectorNode create(final VirtualFrame frame, final NativeObject selector, final int numArgs) {
        final SqueakImageContext image = SqueakImageContext.getSlow();
        if (image.isHeadless()) {
            if (selector.isDebugErrorSelector(image)) {
                return new DispatchSendHeadlessErrorNode(frame, selector, numArgs);
            } else if (selector.isDebugSyntaxErrorSelector(image)) {
                return new DispatchSendSyntaxErrorNode(frame, selector, numArgs);
            }
        }
        return switch (numArgs) {
            case 0 -> DispatchSelector0Node.create(frame, selector);
            case 1 -> DispatchSelector1Node.create(frame, selector);
            case 2 -> DispatchSelector2Node.create(frame, selector);
            case 3 -> DispatchSelector3Node.create(frame, selector);
            case 4 -> DispatchSelector4Node.create(frame, selector);
            case 5 -> DispatchSelector5Node.create(frame, selector);
            default -> DispatchSelectorNaryNode.create(frame, numArgs, selector);
        };
    }

    public static final DispatchSelectorNode createSuper(final VirtualFrame frame, final CompiledCodeObject code, final NativeObject selector, final int numArgs) {
        final ClassObject methodClass = code.getMethod().getMethodClassSlow();
        return switch (numArgs) {
            case 0 -> DispatchSelector0Node.createSuper(frame, methodClass, selector);
            case 1 -> DispatchSelector1Node.createSuper(frame, methodClass, selector);
            case 2 -> DispatchSelector2Node.createSuper(frame, methodClass, selector);
            case 3 -> DispatchSelector3Node.createSuper(frame, methodClass, selector);
            case 4 -> DispatchSelector4Node.createSuper(frame, methodClass, selector);
            case 5 -> DispatchSelector5Node.createSuper(frame, methodClass, selector);
            default -> DispatchSelectorNaryNode.createSuper(frame, numArgs, methodClass, selector);
        };
    }

    public static final DispatchSelectorNode createDirectedSuper(final VirtualFrame frame, final NativeObject selector, final int numArgs) {
        return switch (numArgs) {
            case 0 -> DispatchSelector0Node.createDirectedSuper(frame, selector);
            case 1 -> DispatchSelector1Node.createDirectedSuper(frame, selector);
            case 2 -> DispatchSelector2Node.createDirectedSuper(frame, selector);
            case 3 -> DispatchSelector3Node.createDirectedSuper(frame, selector);
            case 4 -> DispatchSelector4Node.createDirectedSuper(frame, selector);
            case 5 -> DispatchSelector5Node.createDirectedSuper(frame, selector);
            default -> DispatchSelectorNaryNode.createDirectedSuper(frame, numArgs, selector);
        };
    }

    public abstract NativeObject getSelector();
}
