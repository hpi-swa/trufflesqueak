/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public abstract class GetOrCreateContextNode extends AbstractNodeWithCode {

    protected GetOrCreateContextNode(final CompiledCodeObject code) {
        super(code);
    }

    public static GetOrCreateContextNode create(final CompiledCodeObject code) {
        return GetOrCreateContextNodeGen.create(code);
    }

    public abstract ContextObject executeGet(Frame frame, final PointersObject oldProcess);

    public abstract ContextObject executeGet(Frame frame, final NilObject nil);

    @Specialization(guards = {"isVirtualized(frame)"})
    protected final ContextObject doCreate(final VirtualFrame frame, @SuppressWarnings("unused") final NilObject nil,
                    @Cached final AbstractPointersObjectReadNode readNode) {
        final ContextObject result = ContextObject.create(frame.materialize(), code);
        result.setProcess(code.image.getActiveProcess(readNode));
        return result;
    }

    @Specialization(guards = {"isVirtualized(frame)"})
    protected final ContextObject doCreate(final VirtualFrame frame, final PointersObject oldProcess) {
        final ContextObject result = ContextObject.create(frame.materialize(), code);
        if (oldProcess != null) {
            result.setProcess(oldProcess);
        }
        return result;
    }

    @Fallback
    protected final ContextObject doGet(final VirtualFrame frame, @SuppressWarnings("unused") final AbstractSqueakObject nil) {
        final ContextObject result = FrameAccess.getContext(frame, code);
        return result != null ? result : ContextObject.create(frame.materialize(), code);
    }
}
