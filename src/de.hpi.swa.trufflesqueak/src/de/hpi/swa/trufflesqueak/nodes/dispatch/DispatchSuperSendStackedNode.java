/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.LookupMethodNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameSlotReadNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public abstract class DispatchSuperSendStackedNode extends AbstractDispatchNode {
    @CompilationFinal private FrameSlot stackPointerSlot;
    @CompilationFinal private int stackPointer;

    @Child private FrameSlotReadNode readStackNode;

    public DispatchSuperSendStackedNode(final NativeObject selector, final int argumentCount) {
        super(selector, argumentCount);
    }

    public static DispatchSuperSendStackedNode create(final NativeObject selector, final int argumentCount) {
        return DispatchSuperSendStackedNodeGen.create(selector, argumentCount);
    }

    public abstract Object execute(VirtualFrame frame);

    @Specialization(guards = {"peekAtStackTop(frame) == cachedClass"}, assumptions = {"cachedClass.getClassHierarchyStable()", "dispatchNode.getCallTargetStable()"})
    protected final Object doCached(final VirtualFrame frame,
                    @SuppressWarnings("unused") @Cached final AbstractPointersObjectReadNode readNode,
                    @SuppressWarnings("unused") @Cached("peekAtStackTop(frame)") final ClassObject cachedClass,
                    @Cached("create(frame, selector, argumentCount, cachedClass, lookupSlow(cachedClass.getSuperclassOrNull()))") final CachedDispatchNode dispatchNode) {
        popStackTop(frame);
        return dispatchNode.execute(frame);
    }

    protected final Object lookupSlow(final ClassObject receiver) {
        assert receiver != null;
        return LookupMethodNode.getUncached().executeLookup(receiver, selector);
    }

    protected final ClassObject peekAtStackTop(final VirtualFrame frame) {
        if (stackPointerSlot == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            stackPointerSlot = FrameAccess.getStackPointerSlot(frame);
            stackPointer = FrameAccess.getStackPointer(frame, stackPointerSlot) - 1;
            readStackNode = insert(FrameSlotReadNode.create(frame, stackPointer));
        }
        return (ClassObject) readStackNode.executeRead(frame);
    }

    protected final void popStackTop(final VirtualFrame frame) {
        FrameAccess.setStackPointer(frame, stackPointerSlot, stackPointer);
    }
}
