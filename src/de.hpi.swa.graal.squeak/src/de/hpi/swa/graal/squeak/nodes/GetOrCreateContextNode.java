/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;

public abstract class GetOrCreateContextNode extends AbstractNodeWithCode {

    @Child private AbstractPointersObjectReadNode readNode = AbstractPointersObjectReadNode.create();

    private final boolean setActiveProcess;

    protected GetOrCreateContextNode(final CompiledCodeObject code, final boolean fromActiveProcess) {
        super(code);
        this.setActiveProcess = fromActiveProcess;
    }

    public static GetOrCreateContextNode create(final CompiledCodeObject code, final boolean fromActiveProcess) {
        return GetOrCreateContextNodeGen.create(code, fromActiveProcess);
    }

    public abstract ContextObject executeGet(Frame frame);

    @Specialization(guards = {"isVirtualized(frame)"})
    protected final ContextObject doCreate(final VirtualFrame frame) {
        final ContextObject result = ContextObject.create(frame.materialize(), code);
        if (setActiveProcess) {
            result.setProcess(code.image.getActiveProcess(readNode));
        }
        return result;
    }

    @Fallback
    protected final ContextObject doGet(final VirtualFrame frame) {
        return getContext(frame);
    }
}
