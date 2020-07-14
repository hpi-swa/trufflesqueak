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

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodes.AbstractSelfSendNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.CreateFrameArgumentNodes.CreateFrameArgumentsForIndirectCallNode;

@ReportPolymorphism
@ImportStatic(AbstractSelfSendNode.class)
public abstract class DispatchLookupResultNode extends AbstractDispatchNode {
    public DispatchLookupResultNode(final NativeObject selector, final int argumentCount) {
        super(selector, argumentCount);
    }

    public static DispatchLookupResultNode create(final NativeObject selector, final int argumentCount) {
        return DispatchLookupResultNodeGen.create(selector, argumentCount);
    }

    public abstract Object execute(VirtualFrame frame, ClassObject receiverClass, Object lookupResult);

    @SuppressWarnings("unused")
    @Specialization(guards = {"lookupResult == cachedLookupResult", "dispatchNode.isPrimitive()"}, rewriteOn = PrimitiveFailed.class, //
                    limit = "INLINE_CACHE_SIZE", assumptions = {"dispatchNode.getCallTargetStable()"})
    protected final Object doCachedPrimitivesOnly(final VirtualFrame frame, final ClassObject receiverClass, final Object lookupResult,
                    @Cached("lookupResult") final Object cachedLookupResult,
                    @Cached("create(argumentCount, receiverClass, lookupResult)") final CachedDispatchNode dispatchNode) {
        return dispatchNode.execute(frame, selector, null, getUpdateStackPointerNode());
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "lookupResult == cachedLookupResult", limit = "INLINE_CACHE_SIZE", assumptions = {"dispatchNode.getCallTargetStable()"}, replaces = "doCachedPrimitivesOnly")
    protected final Object doCached(final VirtualFrame frame, final ClassObject receiverClass, final Object lookupResult,
                    @Cached("lookupResult") final Object cachedLookupResult,
                    @Cached("create(argumentCount, receiverClass, lookupResult)") final CachedDispatchNode dispatchNode) {
        return dispatchNode.execute(frame, selector, getReceiverAndArgumentsNodes(frame), getUpdateStackPointerNode());
    }

    @Specialization(replaces = "doCached")
    protected final Object doIndirect(final VirtualFrame frame, final ClassObject receiverClass, final Object lookupResult,
                    @Cached final ResolveMethodNode methodNode,
                    @Cached final CreateFrameArgumentsForIndirectCallNode argumentsNode,
                    @Cached final IndirectCallNode callNode,
                    @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
        final CompiledCodeObject method = methodNode.execute(image, receiverClass, lookupResult);
        return callNode.call(method.getCallTarget(), argumentsNode.execute(frame, lookupResult, receiverClass, method, selector, getReceiverAndArgumentsNodes(frame), getUpdateStackPointerNode()));
    }
}
