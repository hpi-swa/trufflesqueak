/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.GenerateWrapper;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.ProbeNode;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.util.FrameAccess;

@GenerateWrapper
public class ExecuteContextNode extends AbstractExecuteContextNode implements InstrumentableNode {

    protected ExecuteContextNode(final CompiledCodeObject code, final boolean resume) {
        super(code, resume);
    }

    protected ExecuteContextNode(final ExecuteContextNode executeContextNode) {
        super(executeContextNode);
    }

    @Override
    public final String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return code.toString();
    }

    @Override
    public Object executeFresh(final VirtualFrame frame) {
        FrameAccess.setInstructionPointer(frame, code, 0);
        initializeFrame(frame);
        return doExecuteFresh(frame);
    }

    @Override
    public final WrapperNode createWrapper(final ProbeNode probe) {
        return new ExecuteContextNodeWrapper(this, this, probe);
    }

    @Override
    public final boolean hasTag(final Class<? extends Tag> tag) {
        return StandardTags.RootTag.class == tag;
    }

    @Override
    public final boolean isInstrumentable() {
        return true;
    }

    @Override
    protected final int startPc() {
        return 0;
    }

}
