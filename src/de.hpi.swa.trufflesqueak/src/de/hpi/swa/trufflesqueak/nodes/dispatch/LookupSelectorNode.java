/*
 * Copyright (c) 2017-2022 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2022 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import java.util.ArrayList;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodes.SelfSendNode;
import de.hpi.swa.trufflesqueak.util.MethodCacheEntry;

@ImportStatic(SelfSendNode.class)
public abstract class LookupSelectorNode extends AbstractNode {
    protected final NativeObject selector;

    protected LookupSelectorNode(final NativeObject selector) {
        this.selector = selector;
    }

    public static LookupSelectorNode create(final NativeObject selector) {
        return LookupSelectorNodeGen.create(selector);
    }

    public abstract Object execute(int receiverClassIndex);

    @SuppressWarnings("unused")
    @Specialization(limit = "INLINE_CACHE_SIZE", guards = {"receiverClassIndex == cachedClassIndex"}, //
                    assumptions = {"cachedClass.getClassHierarchyStable()", "methodDictStableAssumptions"})
    protected static final Object doCached(final int receiverClassIndex,
                    @Cached("receiverClassIndex") final int cachedClassIndex,
                    @Cached("getContext().lookupClass(receiverClassIndex)") final ClassObject cachedClass,
                    @Cached("cachedClass.lookupInMethodDictSlow(selector)") final Object cachedLookupResult,
                    @Cached(value = "createMethodDictStableAssumptions(cachedClass, cachedLookupResult)", dimensions = 1) final Assumption[] methodDictStableAssumptions) {
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
        while (currentClass != null) {
            list.add(currentClass.getMethodDictStable());
            if (currentClass == methodClass) {
                break;
            } else {
                currentClass = currentClass.getSuperclassOrNull();
            }
        }
        // TODO: the receiverClass can be an outdated version of methodClass. In this case, a list
        // of assumptions for the entire class hierarchy is returned. Maybe this can/should be
        // avoided.
        return list.toArray(new Assumption[0]);
    }

    @Specialization(replaces = "doCached")
    protected final Object doUncached(final int receiverClassIndex) {
        final ClassObject receiverClass = getContext().lookupClass(receiverClassIndex); // FIXME
        final MethodCacheEntry cachedEntry = getContext().findMethodCacheEntry(receiverClass, selector);
        if (cachedEntry.getResult() == null) {
            cachedEntry.setResult(receiverClass.lookupInMethodDictSlow(selector));
        }
        return cachedEntry.getResult();
    }
}
