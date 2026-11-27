/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes;

import java.util.logging.Level;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.exceptions.Returns.CannotReturnToTarget;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakSyntaxError;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.interop.WrapToSqueakNode;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.BlockClosurePrimitives.PrimClosureValueWithArgsNode;
import de.hpi.swa.trufflesqueak.nodes.process.GetNextActiveContextNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.LogUtils;

public abstract class DoItRootNode extends RootNode {
    private final SqueakImageContext image;
    private final String sourceCode;
    /* The source of the parsing request, which starts at bodyOffset in sourceCode (-1 if not). */
    private final Source source;
    private final int bodyOffset;
    @CompilationFinal private BlockClosureObject blockClosure;

    protected DoItRootNode(final SqueakImageContext image, final TruffleLanguage<?> language, final String sourceCode, final Source source, final int bodyOffset) {
        super(language);
        this.image = image;
        this.sourceCode = sourceCode;
        this.source = source;
        this.bodyOffset = bodyOffset;
    }

    public static DoItRootNode create(final SqueakImageContext image, final SqueakLanguage language, final String sourceCode, final Source source, final int bodyOffset) {
        return DoItRootNodeGen.create(image, (TruffleLanguage<?>) language, sourceCode, source, bodyOffset);
    }

    @Override
    public final SourceSection getSourceSection() {
        return source.createSection(0, source.getLength());
    }

    /* Parsing must not run Smalltalk code, so load the image and compile the DoIt when it first runs. */
    private BlockClosureObject getBlockClosure() {
        if (blockClosure == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            blockClosure = compileBlockClosure();
        }
        return blockClosure;
    }

    @TruffleBoundary
    private BlockClosureObject compileBlockClosure() {
        image.ensureLoaded();
        try {
            return (BlockClosureObject) image.evaluateUninterruptably(sourceCode);
        } catch (final SqueakSyntaxError e) {
            /* Report syntax errors in the source of the request, not in the wrapping DoIt code. */
            throw bodyOffset < 0 ? e : e.relocateTo(source, bodyOffset);
        }
    }

    @SuppressWarnings("truffle-static-method")
    @Specialization
    protected final Object doIt(final VirtualFrame frame,
                    @Bind final Node node,
                    @Cached final WrapToSqueakNode wrapNode,
                    @Cached final PrimClosureValueWithArgsNode primitiveNode) {
        final BlockClosureObject closure = getBlockClosure();
        if (closure.getNumArgs() != frame.getArguments().length) {
            throw SqueakException.create("Wrong number of arguments for DoItRootNode");
        }
        final var state = image.suspendNormalExecution();
        try {
            return primitiveNode.execute(image.externalSenderFrame, closure, wrapNode.executeWrap(node, frame.getArguments()));
        } catch (final ProcessSwitch ps) {
            CompilerDirectives.transferToInterpreter();
            final var getNextActiveContextNode = GetNextActiveContextNode.getUncached();
            ContextObject activeContext = getNextActiveContextNode.execute();
            LogUtils.SCHEDULING.log(Level.FINE, "Process Switch in DoIt: {0}", activeContext);
            while (true) {
                try {
                    return IndirectCallNode.getUncached().call(activeContext.getResumptionCallTarget(), FrameAccess.newResumeArguments(activeContext));
                } catch (final NonLocalReturn | NonVirtualReturn | CannotReturnToTarget ret) {
                    throw SqueakException.create("Return exception in DoIt", ret);
                } catch (final ProcessSwitch anotherPs) {
                    activeContext = getNextActiveContextNode.execute();
                    LogUtils.SCHEDULING.log(Level.FINE, "Process Switch in DoIt: {0}", activeContext);
                }
            }
        } finally {
            state.close();
        }
    }
}
