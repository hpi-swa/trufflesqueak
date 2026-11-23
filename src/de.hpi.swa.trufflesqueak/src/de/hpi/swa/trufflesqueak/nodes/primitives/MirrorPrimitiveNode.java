/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.primitives;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive2;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive3;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive4;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive5;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive6;

/**
 * Adapter for "mirror primitives": primitives in the OpenSmalltalk VM operate on the topmost values
 * of the stack and can therefore be called from methods with additional leading arguments. In this
 * case, the receiver of the method (and possibly its first arguments) are ignored, and the
 * primitive operates on the last arguments instead. Newspeak's VMMirror relies on this, for example:
 *
 * <pre>
 * VMMirror>>instantiateFixedClass: aClass
 *     &lt;primitive: 70&gt; "basicNew, with aClass as receiver"
 * VMMirror>>ifFail: failBlock add: a and: b
 *     &lt;primitive: 1&gt; "a + b"
 * </pre>
 */
public final class MirrorPrimitiveNode extends AbstractPrimitiveNode implements Primitive1, Primitive2, Primitive3, Primitive4, Primitive5, Primitive6 {
    public static final int MAX_NUM_RECEIVER_AND_ARGUMENTS = 7;

    private final int numPrimitiveReceiverAndArguments;
    private final boolean needsFrame;
    @Child private AbstractPrimitiveNode primitiveNode;

    MirrorPrimitiveNode(final AbstractPrimitiveNode primitiveNode, final int numPrimitiveReceiverAndArguments) {
        assert 1 <= numPrimitiveReceiverAndArguments && numPrimitiveReceiverAndArguments < MAX_NUM_RECEIVER_AND_ARGUMENTS;
        this.primitiveNode = primitiveNode;
        this.numPrimitiveReceiverAndArguments = numPrimitiveReceiverAndArguments;
        needsFrame = primitiveNode.needsFrame();
    }

    @Override
    public boolean acceptsMethod(final CompiledCodeObject method) {
        CompilerAsserts.neverPartOfCompilation();
        return primitiveNode.acceptsMethod(method);
    }

    @Override
    public boolean needsFrame() {
        return needsFrame;
    }

    @Override
    public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1) {
        return executePrimitive(frame, null, null, null, null, null, receiver, arg1);
    }

    @Override
    public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2) {
        return executePrimitive(frame, null, null, null, null, receiver, arg1, arg2);
    }

    @Override
    public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3) {
        return executePrimitive(frame, null, null, null, receiver, arg1, arg2, arg3);
    }

    @Override
    public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4) {
        return executePrimitive(frame, null, null, receiver, arg1, arg2, arg3, arg4);
    }

    @Override
    public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5) {
        return executePrimitive(frame, null, receiver, arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6) {
        return executePrimitive(frame, receiver, arg1, arg2, arg3, arg4, arg5, arg6);
    }

    /** Executes the primitive with the last values (v7 is the top of the stack). */
    private Object executePrimitive(final VirtualFrame frame, final Object v1, final Object v2, final Object v3, final Object v4, final Object v5, final Object v6, final Object v7) {
        CompilerAsserts.partialEvaluationConstant(numPrimitiveReceiverAndArguments);
        return switch (numPrimitiveReceiverAndArguments) {
            case 1 -> ((Primitive.Primitive0) primitiveNode).execute(frame, v7);
            case 2 -> ((Primitive1) primitiveNode).execute(frame, v6, v7);
            case 3 -> ((Primitive2) primitiveNode).execute(frame, v5, v6, v7);
            case 4 -> ((Primitive3) primitiveNode).execute(frame, v4, v5, v6, v7);
            case 5 -> ((Primitive4) primitiveNode).execute(frame, v3, v4, v5, v6, v7);
            case 6 -> ((Primitive5) primitiveNode).execute(frame, v2, v3, v4, v5, v6, v7);
            default -> throw CompilerDirectives.shouldNotReachHere();
        };
    }
}
