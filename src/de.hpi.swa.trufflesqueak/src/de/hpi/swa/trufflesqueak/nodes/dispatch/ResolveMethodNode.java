/*
 * Copyright (c) 2017-2022 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2022 Oracle and/or its affiliates
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
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassIndexNode;
import de.hpi.swa.trufflesqueak.util.MethodCacheEntry;

@ReportPolymorphism
public abstract class ResolveMethodNode extends AbstractNode {

    protected abstract CompiledCodeObject execute(SqueakImageContext image, int receiverClassIndex, Object lookupResult);

    @Specialization
    @SuppressWarnings("unused")
    protected static final CompiledCodeObject doMethod(final SqueakImageContext image, final int receiverClassIndex, final CompiledCodeObject method) {
        return method;
    }

    @Specialization(guards = "lookupResult == null")
    protected final CompiledCodeObject doDoesNotUnderstand(final SqueakImageContext image, final int receiverClassIndex, @SuppressWarnings("unused") final Object lookupResult) {
        final Object dnuMethod = lookupMethod(image, receiverClassIndex, image.doesNotUnderstand);
        if (dnuMethod instanceof CompiledCodeObject) {
            return (CompiledCodeObject) dnuMethod;
        } else {
            throw SqueakException.create("Unable to find DNU method in for ", receiverClassIndex);
        }
    }

    @Specialization(guards = {"targetObject != null", "!isCompiledCodeObject(targetObject)"})
    protected final CompiledCodeObject doObjectAsMethod(final SqueakImageContext image, @SuppressWarnings("unused") final int receiverClassIndex, final Object targetObject,
                    @Cached final SqueakObjectClassIndexNode classNode) {
        final int targetObjectClassIndex = classNode.executeLookup(targetObject);
        final Object runWithInMethod = lookupMethod(image, targetObjectClassIndex, image.runWithInSelector);
        if (runWithInMethod instanceof CompiledCodeObject) {
            return (CompiledCodeObject) runWithInMethod;
        } else {
            assert runWithInMethod == null : "runWithInMethod should not be another Object";
            return doDoesNotUnderstand(image, targetObjectClassIndex, runWithInMethod);
        }
    }

    private Object lookupMethod(final SqueakImageContext image, final int receiverClassIndex, final NativeObject selector) {
        final ClassObject classObject = getContext().lookupClass(receiverClassIndex); // FIXME
        final MethodCacheEntry cachedEntry = image.findMethodCacheEntry(classObject, selector);
        if (cachedEntry.getResult() == null) {
            cachedEntry.setResult(classObject.lookupInMethodDictSlow(selector));
        }
        return cachedEntry.getResult(); /* `null` return signals a doesNotUnderstand. */
    }
}
