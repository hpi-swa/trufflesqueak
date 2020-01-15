/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.accessing;

import java.util.Arrays;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.MaterializedFrame;

import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.VariablePointersObject;
import de.hpi.swa.graal.squeak.model.WeakVariablePointersObject;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.CONTEXT;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ContextObjectNodes.ContextObjectReadNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ContextObjectNodes.ContextObjectWriteNode;

public abstract class SqueakObjectPointersBecomeOneWayNode extends AbstractNode {
    @Child private UpdateSqueakObjectHashNode updateHashNode = UpdateSqueakObjectHashNode.create();

    public static SqueakObjectPointersBecomeOneWayNode create() {
        return SqueakObjectPointersBecomeOneWayNodeGen.create();
    }

    public abstract void execute(Object obj, Object[] from, Object[] to, boolean copyHash);

    @Specialization
    protected final void doClosure(final BlockClosureObject obj, final Object[] from, final Object[] to, final boolean copyHash) {
        final Object[] oldCopied = obj.getCopied();
        final int numOldCopied = oldCopied.length;
        Object newReceiver = obj.getReceiver();
        ContextObject newOuterContext = obj.getOuterContextOrNull();
        assert newOuterContext != null : "Outer context should probably not be null/nil here";
        Object[] newCopied = null;
        for (int i = 0; i < from.length; i++) {
            final Object fromPointer = from[i];
            if (newReceiver == fromPointer) {
                newReceiver = to[i];
                updateHashNode.executeUpdate(fromPointer, newReceiver, copyHash);
            }
            if (newOuterContext == fromPointer) {
                newOuterContext = (ContextObject) to[i];
                updateHashNode.executeUpdate(fromPointer, newOuterContext, copyHash);
            }
            for (int j = 0; j < oldCopied.length; j++) {
                final Object newPointer = oldCopied[j];
                if (newPointer == fromPointer) {
                    if (newCopied == null) {
                        newCopied = Arrays.copyOf(oldCopied, numOldCopied);
                    }
                    newCopied[j] = to[i];
                    updateHashNode.executeUpdate(fromPointer, newCopied[j], copyHash);
                }
            }
        }
        // Only update object if necessary to avoid redundant transferToInterpreters.
        if (newReceiver != obj.getReceiver()) {
            obj.setReceiver(newReceiver);
        }
        if (newOuterContext != obj.getOuterContextOrNull()) {
            obj.setOuterContext(newOuterContext);
        }
        if (newCopied != null) {
            obj.setCopied(newCopied);
        }
    }

    @Specialization
    protected final void doClass(final ClassObject obj, final Object[] from, final Object[] to, final boolean copyHash) {
        ClassObject newSuperclass = obj.getSuperclassOrNull();
        VariablePointersObject newMethodDict = obj.getMethodDict();
        ArrayObject newInstanceVariables = obj.getInstanceVariablesOrNull();
        PointersObject newOrganization = obj.getOrganizationOrNull();
        for (int i = 0; i < from.length; i++) {
            final Object fromPointer = from[i];
            if (fromPointer == newSuperclass) {
                newSuperclass = to[i] == NilObject.SINGLETON ? null : (ClassObject) to[i];
                updateHashNode.executeUpdate(fromPointer, newSuperclass, copyHash);
            }
            if (fromPointer == newMethodDict) {
                newMethodDict = (VariablePointersObject) to[i];
                updateHashNode.executeUpdate(fromPointer, newMethodDict, copyHash);
            }
            if (fromPointer == newInstanceVariables) {
                newInstanceVariables = to[i] == NilObject.SINGLETON ? null : (ArrayObject) to[i];
                updateHashNode.executeUpdate(fromPointer, newInstanceVariables, copyHash);
            }
            if (fromPointer == newOrganization) {
                newOrganization = to[i] == NilObject.SINGLETON ? null : (PointersObject) to[i];
                updateHashNode.executeUpdate(fromPointer, newOrganization, copyHash);
            }
        }
        // Only update object if necessary to avoid redundant transferToInterpreters.
        if (newSuperclass != obj.getSuperclass()) {
            obj.setSuperclass(newSuperclass);
        }
        if (newMethodDict != obj.getMethodDict()) {
            obj.setMethodDict(newMethodDict);
        }
        if (newInstanceVariables != obj.getInstanceVariables()) {
            obj.setInstanceVariables(newInstanceVariables);
        }
        if (newOrganization != obj.getOrganization()) {
            obj.setOrganization(newOrganization);
        }
        pointersBecomeOneWay(obj.getOtherPointers(), from, to, copyHash);
    }

    @Specialization
    protected final void doMethod(final CompiledMethodObject obj, final Object[] from, final Object[] to, final boolean copyHash,
                    @Cached final AbstractPointersObjectReadNode readNode,
                    @Cached final AbstractPointersObjectWriteNode writeNode) {
        final ClassObject oldClass = obj.image.compiledMethodClass;
        for (int i = 0; i < from.length; i++) {
            if (from[i] == oldClass) {
                final ClassObject newClass = (ClassObject) to[i]; // must be a ClassObject
                updateHashNode.executeUpdate(oldClass, newClass, copyHash);
            }
        }
        if (obj.hasMethodClass(readNode)) {
            final ClassObject oldMethodClass = obj.getMethodClass(readNode);
            for (int i = 0; i < from.length; i++) {
                if (from[i] == oldMethodClass) {
                    final ClassObject newMethodClass = (ClassObject) to[i];
                    obj.setMethodClass(writeNode, newMethodClass);
                    updateHashNode.executeUpdate(oldMethodClass, newMethodClass, copyHash);
                    // TODO: flush method caches correct here?
                    newMethodClass.invalidateMethodDictStableAssumption();
                }
            }
        }
    }

    @Specialization
    protected final void doContext(final ContextObject obj, final Object[] from, final Object[] to, final boolean copyHash,
                    @Cached final ContextObjectReadNode readNode,
                    @Cached final ContextObjectWriteNode writeNode) {
        for (int i = 0; i < from.length; i++) {
            final Object fromPointer = from[i];
            final Object toPointer = to[i];
            // TODO: Check that all pointers are actually traced (obj.size()?).
            for (int j = 0; j < CONTEXT.TEMP_FRAME_START; j++) {
                final Object newPointer = readNode.execute(obj, j);
                if (newPointer == fromPointer) {
                    writeNode.execute(obj, j, toPointer);
                    updateHashNode.executeUpdate(fromPointer, toPointer, copyHash);
                }
            }
            if (!obj.hasTruffleFrame()) {
                return;
            }
            final MaterializedFrame truffleFrame = obj.getTruffleFrame();
            final Object[] args = truffleFrame.getArguments();
            for (int j = 4; j < args.length; j++) {
                final Object newPointer = args[j];
                if (newPointer == fromPointer) {
                    writeNode.execute(obj, j - 4 + CONTEXT.TEMP_FRAME_START, toPointer);
                    updateHashNode.executeUpdate(fromPointer, toPointer, copyHash);
                }
            }
            final CompiledCodeObject code = args[2] != null ? ((BlockClosureObject) args[2]).getCompiledBlock() : (CompiledMethodObject) args[0];
            final int stackp = FrameUtil.getIntSafe(truffleFrame, code.getStackPointerSlot());
            final FrameSlot[] stackSlots = code.getStackSlotsUnsafe();
            final FrameDescriptor frameDescriptor = code.getFrameDescriptor();
            for (int j = 0; j < stackp; j++) {
                final FrameSlot slot = stackSlots[j];
                if (slot == null) {
                    break; // Stop here, slot has not (yet) been created.
                }
                final FrameSlotKind currentSlotKind = frameDescriptor.getFrameSlotKind(slot);
                if (currentSlotKind == FrameSlotKind.Object) {
                    final Object newPointer = FrameUtil.getObjectSafe(truffleFrame, slot);
                    if (newPointer == null) {
                        break;
                    }
                    if (newPointer == fromPointer) {
                        writeNode.execute(obj, j + CONTEXT.TEMP_FRAME_START, toPointer);
                        updateHashNode.executeUpdate(fromPointer, toPointer, copyHash);
                    }
                } else if (currentSlotKind == FrameSlotKind.Illegal) {
                    break; // Stop here, because this slot and all following are not used.
                }
            }
        }
    }

    @Specialization(guards = "obj.isTraceable()")
    protected final void doArray(final ArrayObject obj, final Object[] from, final Object[] to, final boolean copyHash) {
        pointersBecomeOneWay(obj.getObjectStorage(), from, to, copyHash);
    }

    @Specialization
    protected final void doPointers(final PointersObject obj, final Object[] from, final Object[] to, final boolean copyHash) {
        obj.pointersBecomeOneWay(updateHashNode, from, to, copyHash);
    }

    @Specialization
    protected final void doVariablePointers(final VariablePointersObject obj, final Object[] from, final Object[] to, final boolean copyHash) {
        obj.pointersBecomeOneWay(updateHashNode, from, to, copyHash);
    }

    @Specialization
    protected final void doWeakPointers(final WeakVariablePointersObject obj, final Object[] from, final Object[] to, final boolean copyHash) {
        obj.pointersBecomeOneWay(updateHashNode, from, to, copyHash);
    }

    private void pointersBecomeOneWay(final Object[] original, final Object[] from, final Object[] to, final boolean copyHash) {
        for (int i = 0; i < from.length; i++) {
            final Object fromPointer = from[i];
            for (int j = 0; j < original.length; j++) {
                final Object newPointer = original[j];
                if (newPointer == fromPointer) {
                    final Object toPointer = to[i];
                    original[j] = toPointer;
                    updateHashNode.executeUpdate(fromPointer, toPointer, copyHash);
                }
            }
        }
    }

    @SuppressWarnings("unused")
    @Fallback
    protected static final void doFallback(final Object obj, final Object[] from, final Object[] to, final boolean copyHash) {
        // nothing to do
    }
}
