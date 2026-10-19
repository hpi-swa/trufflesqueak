/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes;

import java.util.logging.Level;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.exceptions.Returns.CannotReturnToTarget;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.interop.WrapToSqueakNode;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.BlockClosurePrimitives.PrimClosureValueWithArgsNode;
import de.hpi.swa.trufflesqueak.nodes.process.GetNextActiveContextNode;
import de.hpi.swa.trufflesqueak.util.LogUtils;

public abstract class DoItRootNode extends RootNode {
    private final SqueakImageContext image;
    private final BlockClosureObject blockClosure;

    protected DoItRootNode(final SqueakImageContext image, final TruffleLanguage<?> language, final BlockClosureObject closure) {
        super(language);
        this.image = image;
        blockClosure = closure;
    }

    public static DoItRootNode create(final SqueakImageContext image, final SqueakLanguage language, final BlockClosureObject closure) {
        return DoItRootNodeGen.create(image, (TruffleLanguage<?>) language, closure);
    }

    @SuppressWarnings("truffle-static-method")
    @Specialization
    protected final Object doIt(final VirtualFrame frame,
                    @Bind final Node node,
                    @Cached final WrapToSqueakNode wrapNode,
                    @Cached final PrimClosureValueWithArgsNode primitiveNode) {
        if (blockClosure.getNumArgs() != frame.getArguments().length) {
            throw SqueakException.create("Wrong number of arguments for DoItRootNode");
        }
        final var state = image.suspendNormalExecution();
        try {
            return primitiveNode.execute(image.externalSenderFrame, blockClosure, wrapNode.executeWrap(node, frame.getArguments()));
        } catch (final ProcessSwitch ps) {
            CompilerDirectives.transferToInterpreter();
            final var getNextActiveContextNode = GetNextActiveContextNode.getUncached();
            ContextObject activeContext = getNextActiveContextNode.execute();
            LogUtils.SCHEDULING.log(Level.FINE, "Process Switch in DoIt: {0}", activeContext);
            while (true) {
                try {
                    return IndirectCallNode.getUncached().call(activeContext.getCallTarget());
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
