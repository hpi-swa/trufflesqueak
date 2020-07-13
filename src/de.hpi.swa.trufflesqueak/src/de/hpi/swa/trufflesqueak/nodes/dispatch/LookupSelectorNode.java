/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodes.AbstractSelfSendNode;
import de.hpi.swa.trufflesqueak.util.MethodCacheEntry;

@ImportStatic(AbstractSelfSendNode.class)
public abstract class LookupSelectorNode extends AbstractNode {
    protected final NativeObject selector;

    protected LookupSelectorNode(final NativeObject selector) {
        this.selector = selector;
    }

    public static LookupSelectorNode create(final NativeObject selector) {
        return LookupSelectorNodeGen.create(selector);
    }

    public abstract Object execute(ClassObject receiverClass);

    @SuppressWarnings("unused")
    @Specialization(limit = "INLINE_CACHE_SIZE", guards = {"classObject == cachedClass"}, //
                    assumptions = {"cachedClass.getClassHierarchyStable()", "cachedClass.getMethodDictStable()"})
    protected static final Object doCached(final ClassObject classObject,
                    @Cached("classObject") final ClassObject cachedClass,
                    @Cached("classObject.lookupInMethodDictSlow(selector)") final Object cachedMethod) {
        return cachedMethod;
    }

    @Specialization(replaces = "doCached")
    protected final Object doUncached(final ClassObject classObject,
                    @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
        final MethodCacheEntry cachedEntry = image.findMethodCacheEntry(classObject, selector);
        if (cachedEntry.getResult() == null) {
            cachedEntry.setResult(classObject.lookupInMethodDictSlow(selector));
        }
        return cachedEntry.getResult(); /* `null` return signals a doesNotUnderstand. */
    }

}
