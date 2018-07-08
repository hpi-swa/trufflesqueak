package de.hpi.swa.graal.squeak.nodes.accessing;

import java.util.Arrays;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.WeakPointersObject;

public abstract class SqueakObjectPointersBecomeOneWayNode extends Node {
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
        ContextObject newOuterContext = obj.getOuterContext();
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
        if (newOuterContext != obj.getOuterContext()) {
            obj.setOuterContext(newOuterContext);
        }
        if (newCopied != null) {
            obj.setCopied(newCopied);
        }
    }

    @Specialization
    protected final void doClass(final ClassObject obj, final Object[] from, final Object[] to, final boolean copyHash) {
        for (int i = 0; i < from.length; i++) {
            final Object fromPointer = from[i];
            for (int j = 0; j < obj.size(); j++) {
                final Object newPointer = obj.at0(j);
                if (newPointer == fromPointer) {
                    final Object toPointer = to[i];
                    obj.atput0(j, toPointer);
                    updateHashNode.executeUpdate(fromPointer, toPointer, copyHash);
                }
            }
        }
    }

    @Specialization
    protected final void doMethod(final CompiledMethodObject obj, final Object[] from, final Object[] to, final boolean copyHash) {
        final ClassObject oldClass = obj.getSqClass();
        for (int i = 0; i < from.length; i++) {
            if (from[i] == oldClass) {
                final ClassObject newClass = (ClassObject) to[i]; // must be a ClassObject
                obj.setSqClass(newClass);
                updateHashNode.executeUpdate(oldClass, newClass, copyHash);
            }
        }
        final ClassObject oldCompiledInClass = obj.getCompiledInClass();
        for (int i = 0; i < from.length; i++) {
            if (from[i] == oldCompiledInClass) {
                final ClassObject newCompiledInClass = (ClassObject) to[i];  // must be a
                                                                             // ClassObject
                obj.setCompiledInClass(newCompiledInClass);
                updateHashNode.executeUpdate(oldCompiledInClass, newCompiledInClass, copyHash);
                // TODO: flush method caches
            }
        }
    }

    @Specialization
    protected final void doContext(final ContextObject obj, final Object[] from, final Object[] to, final boolean copyHash) {
        for (int i = 0; i < from.length; i++) {
            final Object fromPointer = from[i];
            // skip sender (for performance), pc, and sp
            for (int j = CONTEXT.METHOD; j < obj.size(); j++) {
                final Object newPointer = obj.at0(j);
                if (newPointer == fromPointer) {
                    final Object toPointer = to[i];
                    obj.atput0(j, toPointer);
                    updateHashNode.executeUpdate(fromPointer, toPointer, copyHash);
                }
            }
        }
    }

    @Specialization
    protected final void doPointers(final PointersObject obj, final Object[] from, final Object[] to, final boolean copyHash) {
        for (int i = 0; i < from.length; i++) {
            final Object fromPointer = from[i];
            for (int j = 0; j < obj.size(); j++) {
                final Object newPointer = obj.at0(j);
                if (newPointer == fromPointer) {
                    final Object toPointer = to[i];
                    obj.atput0(j, toPointer);
                    updateHashNode.executeUpdate(fromPointer, toPointer, copyHash);
                }
            }
        }
    }

    @Specialization
    protected final void doWeakPointers(final WeakPointersObject obj, final Object[] from, final Object[] to, final boolean copyHash) {
        for (int i = 0; i < from.length; i++) {
            final Object fromPointer = from[i];
            for (int j = 0; j < obj.size(); j++) {
                final Object newPointer = obj.at0(j);
                if (newPointer == fromPointer) {
                    final Object toPointer = to[i];
                    obj.atput0(j, toPointer);
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
