/*
 * Copyright (c) 2017-2021 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import java.util.ArrayList;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
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
    @Specialization(limit = "INLINE_CACHE_SIZE", guards = {"receiverClass == cachedClass"}, //
                    assumptions = {"cachedClass.getClassHierarchyStable()", "methodDictStableAssumptions"})
    protected static final Object doCached(final ClassObject receiverClass,
                    @Cached("receiverClass") final ClassObject cachedClass,
                    @Cached("receiverClass.lookupInMethodDictSlow(selector)") final Object cachedLookupResult,
                    @Cached(value = "createMethodDictStableAssumptions(receiverClass, cachedLookupResult)", dimensions = 1) final Assumption[] methodDictStableAssumptions) {
        return cachedLookupResult;
    }

    protected static final Assumption[] createMethodDictStableAssumptions(final ClassObject receiverClass, final Object lookupResult) {
        final ClassObject methodClass;
        if (lookupResult instanceof CompiledCodeObject) {
            final CompiledCodeObject method = (CompiledCodeObject) lookupResult;
            assert method.isCompiledMethod();
            methodClass = method.getMethodClassSlow();
        } else {
            /* DNU or OAM, return assumptions for all superclasses. */
            methodClass = null;
        }
        final ArrayList<Assumption> list = new ArrayList<>();
        ClassObject currentClass = receiverClass;
        while (currentClass != methodClass) {
            list.add(currentClass.getMethodDictStable());
            currentClass = currentClass.getSuperclassOrNull();
        }
        if (methodClass != null) {
            list.add(methodClass.getMethodDictStable());
        }
        return list.toArray(new Assumption[0]);
    }

    @Specialization(replaces = "doCached")
    protected final Object doUncached(final ClassObject receiverClass,
                    @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
        final MethodCacheEntry cachedEntry = image.findMethodCacheEntry(receiverClass, selector);
        if (cachedEntry.getResult() == null) {
            cachedEntry.setResult(receiverClass.lookupInMethodDictSlow(selector));
        }
        return cachedEntry.getResult(); /* `null` return signals a doesNotUnderstand. */
    }
}
