/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.bytecodes;

import com.oracle.truffle.api.instrumentation.GenerateWrapper;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.ProbeNode;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;

@GenerateWrapper
public abstract class AbstractInstrumentableBytecodeNode extends AbstractBytecodeNode implements InstrumentableNode {

    protected AbstractInstrumentableBytecodeNode(final AbstractInstrumentableBytecodeNode original) {
        this(original.code, original.index, original.numBytecodes);
    }

    public AbstractInstrumentableBytecodeNode(final CompiledCodeObject code, final int index, final int numBytecodes) {
        super(code, index, numBytecodes);
    }

    @Override
    public final boolean isInstrumentable() {
        return true;
    }

    @Override
    public WrapperNode createWrapper(final ProbeNode probe) {
        return new AbstractInstrumentableBytecodeNodeWrapper(this, this, probe);
    }

    @Override
    public boolean hasTag(final Class<? extends Tag> tag) {
        return tag == StandardTags.StatementTag.class;
    }
}
