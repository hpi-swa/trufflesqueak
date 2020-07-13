/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodes.AbstractSelfSendNode;

@ReportPolymorphism
@ImportStatic(AbstractSelfSendNode.class)
public abstract class LookupClassNode extends AbstractNode {

    public static LookupClassNode create() {
        return LookupClassNodeGen.create();
    }

    public abstract ClassObject execute(Object receiver);

    @Specialization(guards = "guard.check(receiver)", limit = "INLINE_CACHE_SIZE")
    protected static final ClassObject doCached(@SuppressWarnings("unused") final Object receiver,
                    @SuppressWarnings("unused") @Cached("create(receiver)") final LookupClassGuard guard,
                    @Cached("lookupSlow(receiver)") final ClassObject cachedClass) {
        return cachedClass;
    }

    protected static final ClassObject lookupSlow(final Object receiver) {
        return SqueakObjectClassNode.getUncached().executeLookup(receiver);
    }

    @Specialization(replaces = "doCached")
    protected static final ClassObject doUncached(final Object receiver,
                    @Cached final SqueakObjectClassNode classNode) {
        return classNode.executeLookup(receiver);
    }
}
