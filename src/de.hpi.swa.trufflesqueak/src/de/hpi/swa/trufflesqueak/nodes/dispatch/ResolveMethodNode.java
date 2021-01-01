/*
 * Copyright (c) 2017-2021 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.trufflesqueak.util.MethodCacheEntry;

@ReportPolymorphism
public abstract class ResolveMethodNode extends AbstractNode {

    protected abstract CompiledCodeObject execute(SqueakImageContext image, ClassObject receiverClass, Object lookupResult);

    @Specialization
    @SuppressWarnings("unused")
    protected static final CompiledCodeObject doMethod(final SqueakImageContext image, final ClassObject receiverClass, final CompiledCodeObject method) {
        return method;
    }

    @Specialization(guards = "lookupResult == null")
    protected static final CompiledCodeObject doDoesNotUnderstand(final SqueakImageContext image, final ClassObject receiverClass, @SuppressWarnings("unused") final Object lookupResult) {
        final Object dnuMethod = lookupMethod(image, receiverClass, image.doesNotUnderstand);
        if (dnuMethod instanceof CompiledCodeObject) {
            return (CompiledCodeObject) dnuMethod;
        } else {
            throw SqueakException.create("Unable to find DNU method in", receiverClass);
        }
    }

    @Specialization(guards = {"targetObject != null", "!isCompiledCodeObject(targetObject)"})
    protected static final CompiledCodeObject doObjectAsMethod(final SqueakImageContext image, @SuppressWarnings("unused") final ClassObject receiverClass, final Object targetObject,
                    @Cached final SqueakObjectClassNode classNode) {
        final ClassObject targetObjectClass = classNode.executeLookup(targetObject);
        final Object runWithInMethod = lookupMethod(image, targetObjectClass, image.runWithInSelector);
        if (runWithInMethod instanceof CompiledCodeObject) {
            return (CompiledCodeObject) runWithInMethod;
        } else {
            assert runWithInMethod == null : "runWithInMethod should not be another Object";
            return doDoesNotUnderstand(image, targetObjectClass, runWithInMethod);
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
