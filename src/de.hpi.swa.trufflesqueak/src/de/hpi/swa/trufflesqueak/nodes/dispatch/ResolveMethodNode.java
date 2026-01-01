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
import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
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

    public abstract CompiledCodeObject execute(Node node, SqueakImageContext image, int expectedNumArgs, boolean canPrimFail, ClassObject receiverClass, Object lookupResult);

    @Specialization
    @SuppressWarnings("unused")
    protected static final CompiledCodeObject doMethod(final SqueakImageContext image, final int expectedNumArgs, final boolean canPrimFail, final ClassObject receiverClass,
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
    protected static final CompiledCodeObject doDoesNotUnderstand(final SqueakImageContext image, final int expectedNumArgs, final boolean canPrimFail, final ClassObject receiverClass,
                    final Object lookupResult) {
        final Object dnuMethod = lookupMethod(image, receiverClass, image.doesNotUnderstand);
        if (dnuMethod instanceof final CompiledCodeObject method) {
            assert method.getNumArgs() == 1 : "#doesNotUnderstand: with unexpected number of arguments, got " + method.getNumArgs();
            return method;
        } else {
            throw SqueakException.create("Unable to find #doesNotUnderstand: in", receiverClass);
        }
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"targetObject != null", "!isCompiledCodeObject(targetObject)"})
    protected static final CompiledCodeObject doObjectAsMethod(final Node node, final SqueakImageContext image, final int expectedNumArgs, final boolean canPrimFail, final ClassObject receiverClass,
                    final Object targetObject,
                    @Cached final SqueakObjectClassNode classNode) {
        final ClassObject targetObjectClass = classNode.executeLookup(node, targetObject);
        final Object runWithInMethod = lookupMethod(image, targetObjectClass, image.runWithInSelector);
        if (runWithInMethod instanceof final CompiledCodeObject method) {
            assert method.getNumArgs() == 3 : "#run:with:in: with unexpected number of arguments, got " + method.getNumArgs();
            return method;
        } else {
            assert runWithInMethod == null : "#run:with:in: should not be another Object";
            return doDoesNotUnderstand(image, 2, false, targetObjectClass, null);
        }
    }

    private static Object lookupMethod(final SqueakImageContext image, final ClassObject classObject, final NativeObject selector) {
        final MethodCacheEntry cachedEntry = image.findMethodCacheEntry(classObject, selector);
        if (cachedEntry.getResult() == null) {
            cachedEntry.setResult(classObject.lookupInMethodDictSlow(selector));
        }
        return cachedEntry.getResult(); /* `null` return signals a doesNotUnderstand. */
    }
}
