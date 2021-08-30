/*
 * Copyright (c) 2017-2021 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.instrumentation.GenerateWrapper;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.ProbeNode;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.interop.NodeLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextScope;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

@GenerateWrapper
@ExportLibrary(NodeLibrary.class)
@SuppressWarnings("static-method")
public abstract class AbstractInstrumentableBytecodeNode extends AbstractBytecodeNode implements InstrumentableNode {

    protected AbstractInstrumentableBytecodeNode(final AbstractInstrumentableBytecodeNode original) {
        this(original.code, original.index - original.code.getInitialPC(), original.getNumBytecodes());
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

    /*
     * NodeLibrary
     */

    @ExportMessage
    protected final boolean hasScope(final Frame frame) {
        return FrameAccess.isTruffleSqueakFrame(frame);
    }

    @ExportMessage
    protected final Object getScope(final Frame frame, @SuppressWarnings("unused") final boolean nodeEnter) throws UnsupportedMessageException {
        if (hasScope(frame)) {
            return new ContextScope(frame);
        } else {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage
    protected final boolean hasReceiverMember(@SuppressWarnings("unused") final Frame frame) {
        return frame != null;
    }

    @ExportMessage
    protected final Object getReceiverMember(final Frame frame) throws UnsupportedMessageException {
        if (frame == null) {
            throw UnsupportedMessageException.create();
        }
        return "self";
    }

    @ExportMessage
    @TruffleBoundary
    protected final boolean hasRootInstance(@SuppressWarnings("unused") final Frame frame) {
        final String selector = getRootNode().getName();
        return getContext().lookup(selector) != NilObject.SINGLETON;
    }

    @ExportMessage
    @TruffleBoundary
    protected final Object getRootInstance(@SuppressWarnings("unused") final Frame frame)
                    throws UnsupportedMessageException {
        final String selector = getRootNode().getName();
        final Object result = getContext().lookup(selector);
        if (result != null) {
            return result;
        } else {
            throw UnsupportedMessageException.create();
        }
    }
}
