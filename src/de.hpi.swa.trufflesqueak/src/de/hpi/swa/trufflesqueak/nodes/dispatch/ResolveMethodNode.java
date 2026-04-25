/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.trufflesqueak.util.MethodCacheEntry;

@GenerateInline
@GenerateCached(false)
public abstract class ResolveMethodNode extends AbstractNode {

    public abstract CompiledCodeObject execute(Node node, SqueakImageContext image, int expectedNumArgs, boolean canPrimFail, NativeObject selector, ClassObject receiverClass, Object lookupResult);

    @Specialization
    @SuppressWarnings("unused")
    protected static final CompiledCodeObject doMethod(final SqueakImageContext image, final int expectedNumArgs, final boolean canPrimFail, final NativeObject selector,
                    final ClassObject receiverClass,
                    final CompiledCodeObject method) {
        CompilerAsserts.partialEvaluationConstant(canPrimFail);
        if (method.getNumArgs() != expectedNumArgs) {
            CompilerDirectives.transferToInterpreter();
            if (canPrimFail) {
                throw PrimitiveFailed.BAD_NUMBER_OF_ARGUMENTS;
            } else {
                assert false : "Expected method with " + expectedNumArgs + " arguments, got " + method.getNumArgs();
            }
        }
        return method;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "lookupResult == null")
    protected static final CompiledCodeObject doDispatchFailure(final SqueakImageContext image, final int expectedNumArgs, final boolean canPrimFail,
                    final NativeObject selector,
                    final ClassObject receiverClass,
                    final Object lookupResult) {
        final MethodCacheEntry cacheEntry = image.findMethodCacheEntry(receiverClass, selector);
        final ClassObject.DispatchFailureResult result = cacheEntry.getOrCreateDispatchFailureResult(expectedNumArgs);
        final CompiledCodeObject fallbackMethod = result.fallbackMethod();
        assert fallbackMethod.getNumArgs() == 1 || (result.convention() == ClassObject.FallbackConvention.SHORTCUT_DNU &&
                        fallbackMethod.getNumArgs() == expectedNumArgs + 1) : "Fallback method with unexpected number of arguments";
        return fallbackMethod;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"targetObject != null", "!isCompiledCodeObject(targetObject)"})
    protected static final CompiledCodeObject doObjectAsMethod(final Node node, final SqueakImageContext image, final int expectedNumArgs, final boolean canPrimFail, final NativeObject selector,
                    final ClassObject receiverClass,
                    final Object targetObject,
                    @Cached final SqueakObjectClassNode classNode) {
        final ClassObject targetObjectClass = classNode.executeLookup(node, targetObject);

        final MethodCacheEntry oamCacheEntry = image.findMethodCacheEntry(targetObjectClass, image.runWithInSelector);
        if (oamCacheEntry.getResult() == null) {
            oamCacheEntry.setResult(targetObjectClass.lookupMethodInMethodDictSlow(image.runWithInSelector));
        }

        if (oamCacheEntry.getResult() instanceof final CompiledCodeObject runWithInMethod) {
            assert runWithInMethod.getNumArgs() == 3 : "#run:with:in: with unexpected number of arguments, got " + runWithInMethod.getNumArgs();
            return runWithInMethod;
        } else {
            /*
             * Target object doesn't understand run:with:in: (or it resolved to another non-method
             * object), so we fall back to unified DNU.
             */
            return doDispatchFailure(image, 3, false, image.runWithInSelector, targetObjectClass, null);
        }
    }
}
