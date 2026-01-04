/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.process;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.LINKED_LIST;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;

@GenerateInline(false)
@GenerateCached
public abstract class RemoveProcessFromListNode extends AbstractNode {

    public final void executeRemove(final PointersObject process, final PointersObject list,
                    final AbstractPointersObjectReadNode readNode,
                    final AbstractPointersObjectWriteNode writeNode) {
        final Object first = readNode.execute(list, LINKED_LIST.FIRST_LINK);
        final Object last = readNode.execute(list, LINKED_LIST.LAST_LINK);
        executeRemove(process, list, first, last);
        writeNode.executeNil(process, PROCESS.NEXT_LINK);
    }

    protected abstract void executeRemove(PointersObject process, PointersObject list, Object first, Object last);

    @Specialization(guards = "process == first")
    protected static final void doRemoveEqual(final PointersObject process, final PointersObject list, @SuppressWarnings("unused") final PointersObject first,
                    final AbstractSqueakObject last,
                    @Shared("readNode") @Cached final AbstractPointersObjectReadNode readNode,
                    @Shared("writeNode") @Cached final AbstractPointersObjectWriteNode writeNode) {
        final Object next = readNode.execute(process, PROCESS.NEXT_LINK);
        writeNode.execute(list, LINKED_LIST.FIRST_LINK, next);
        if (process == last) {
            writeNode.executeNil(list, LINKED_LIST.LAST_LINK);
        }
    }

    @Specialization(guards = "process != first")
    protected static final void doRemoveNotEqual(final PointersObject process, final PointersObject list, final PointersObject first, final AbstractSqueakObject last,
                    @Shared("readNode") @Cached final AbstractPointersObjectReadNode readNode,
                    @Shared("writeNode") @Cached final AbstractPointersObjectWriteNode writeNode) {
        PointersObject temp = first;
        Object next;
        while (true) {
            next = readNode.execute(temp, PROCESS.NEXT_LINK);
            if (next == process) {
                break;
            } else if (next == NilObject.SINGLETON) {
                throw PrimitiveFailed.andTransferToInterpreter(); // TODO: make this better.
            } else {
                temp = (PointersObject) next;
            }
        }
        next = readNode.execute(process, PROCESS.NEXT_LINK);
        writeNode.execute(temp, PROCESS.NEXT_LINK, next);
        if (process == last) {
            writeNode.execute(list, LINKED_LIST.LAST_LINK, temp);
        }
    }

    @SuppressWarnings("unused")
    @Specialization
    protected static final void doRemoveNotEqual(final PointersObject process, final PointersObject list, final NilObject first, final AbstractSqueakObject last) {
        throw PrimitiveFailed.GENERIC_ERROR; // TODO: make sure this is needed (and make it better).
    }
}
