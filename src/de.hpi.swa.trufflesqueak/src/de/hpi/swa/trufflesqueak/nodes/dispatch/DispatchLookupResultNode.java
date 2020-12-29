/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.exceptions.RespecializeException;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodes.AbstractSelfSendNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.CreateFrameArgumentNodes.CreateFrameArgumentsForIndirectCallNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeFactory;

@ReportPolymorphism
@ImportStatic(AbstractSelfSendNode.class)
public abstract class DispatchLookupResultNode extends AbstractDispatchNode {
    public DispatchLookupResultNode(final NativeObject selector, final int argumentCount) {
        super(selector, argumentCount);
    }

    public static DispatchLookupResultNode create(final NativeObject selector, final int argumentCount) {
        return DispatchLookupResultNodeGen.create(selector, argumentCount);
    }

    public abstract Object execute(VirtualFrame frame, Object receiver, ClassObject receiverClass, Object lookupResult);

    @SuppressWarnings("unused")
    @Specialization(guards = "lookupResult == cachedLookupResult", limit = "INLINE_CACHE_SIZE", assumptions = {"dispatchNode.getCallTargetStable()"})
    protected static final Object doCached(final VirtualFrame frame, final Object receiver, final ClassObject receiverClass, final Object lookupResult,
                    @Cached("lookupResult") final Object cachedLookupResult,
                    @Cached("create(frame, selector, argumentCount, receiverClass, lookupResult)") final CachedDispatchNode dispatchNode) {
        return dispatchNode.execute(frame);
    }

    @Specialization(replaces = "doCached", rewriteOn = RespecializeException.class)
    protected static final Object doIndirect(final VirtualFrame frame, final Object receiver, final ClassObject receiverClass, final Object lookupResult,
                    @Cached final ResolveMethodNode methodNode,
                    @Cached("create(frame, selector, argumentCount)") final CreateFrameArgumentsForIndirectCallNode argumentsNode,
                    @Cached final IndirectCallNode callNode,
                    @CachedContext(SqueakLanguage.class) final SqueakImageContext image) throws RespecializeException {
        final CompiledCodeObject method = methodNode.execute(image, receiverClass, lookupResult);
        if (method.hasPrimitive()) {
            throw RespecializeException.transferToInterpreterInvalidateAndThrow();
        }
        return callNode.call(method.getCallTarget(), argumentsNode.execute(frame, receiver, receiverClass, lookupResult, method));
    }

    @Specialization(replaces = "doIndirect")
    protected static final Object doIndirectWithPrimitives(final VirtualFrame frame, final Object receiver, final ClassObject receiverClass, final Object lookupResult,
                    @Cached final ResolveMethodNode methodNode,
                    @Cached("create(frame, selector, argumentCount)") final CreateFrameArgumentsForIndirectCallNode argumentsNode,
                    @Cached final IndirectPrimitiveNode primitiveNode,
                    @Cached final IndirectCallNode callNode,
                    @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
        final CompiledCodeObject method = methodNode.execute(image, receiverClass, lookupResult);
        if (method.hasPrimitive()) {
            try {
                return primitiveNode.execute(frame, method);
            } catch (final PrimitiveFailed pf) {
                assert !method.hasStoreIntoTemp1AfterCallPrimitive() : "Primitive error codes not yet supported in indirect sends";
            }
        }
        return callNode.call(method.getCallTarget(), argumentsNode.execute(frame, receiver, receiverClass, lookupResult, method));
    }

    @ImportStatic(PrimitiveNodeFactory.class)
    protected abstract static class IndirectPrimitiveNode extends Node {
        /* Number of primitives to cache per method (may need to be raised if not high enough). */
        protected static final int CACHE_LIMIT = 4;

        protected abstract Object execute(VirtualFrame frame, CompiledCodeObject primitiveMethod);

        @SuppressWarnings("unused")
        @Specialization(guards = "primitiveMethod == cachedPrimitiveMethod", limit = "CACHE_LIMIT")
        protected static final Object doCached(final VirtualFrame frame, final CompiledCodeObject primitiveMethod,
                        @Cached("primitiveMethod") final CompiledCodeObject cachedPrimitiveMethod,
                        @Cached("forIndex(cachedPrimitiveMethod, true, cachedPrimitiveMethod.primitiveIndex(), false)") final AbstractPrimitiveNode primitiveNode) {
            return primitiveNode.executePrimitive(frame);
        }
    }
}
