/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.dsl.CachedContext;
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

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextScope;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

@GenerateWrapper
@ExportLibrary(NodeLibrary.class)
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

    /*
     * NodeLibrary
     */

    @ExportMessage
    @SuppressWarnings("static-method")
    protected final boolean hasScope(final Frame frame) {
        return FrameAccess.isTruffleSqueakFrame(frame);
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    protected final Object getScope(final Frame frame, @SuppressWarnings("unused") final boolean nodeEnter) throws UnsupportedMessageException {
        if (hasScope(frame)) {
            return new ContextScope(frame);
        } else {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    protected final boolean hasReceiverMember(@SuppressWarnings("unused") final Frame frame) {
        return frame != null;
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    protected final Object getReceiverMember(final Frame frame) throws UnsupportedMessageException {
        if (frame == null) {
            throw UnsupportedMessageException.create();
        }
        return "self";
    }

    @ExportMessage
    @TruffleBoundary
    protected final boolean hasRootInstance(@SuppressWarnings("unused") final Frame frame,
                    @CachedContext(SqueakLanguage.class) final ContextReference<SqueakImageContext> contextRef) {
        final String selector = getRootNode().getName();
        final SqueakImageContext image = contextRef.get();
        return image.lookup(selector) != NilObject.SINGLETON;
    }

    @ExportMessage
    @TruffleBoundary
    protected final Object getRootInstance(@SuppressWarnings("unused") final Frame frame,
                    @CachedContext(SqueakLanguage.class) final ContextReference<SqueakImageContext> contextRef)
                    throws UnsupportedMessageException {
        final String selector = getRootNode().getName();
        final SqueakImageContext image = contextRef.get();
        final Object result = image.lookup(selector);
        if (result != null) {
            return result;
        } else {
            throw UnsupportedMessageException.create();
        }
    }
}
