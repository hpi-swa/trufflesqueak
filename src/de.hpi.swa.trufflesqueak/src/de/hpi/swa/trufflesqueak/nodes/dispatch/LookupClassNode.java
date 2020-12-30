/*
 * Copyright (c) 2017-2021 Software Architecture Group, Hasso Plattner Institute
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
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodes.SelfSendNode;

@ImportStatic(SelfSendNode.class)
public abstract class LookupClassNode extends AbstractNode {

    public static LookupClassNode create() {
        return LookupClassNodeGen.create();
    }

    public abstract ClassObject execute(Object receiver);

    @Specialization(guards = "guard.check(receiver)", assumptions = "guard.getIsValidAssumption()", limit = "INLINE_CACHE_SIZE")
    protected static final ClassObject doCached(@SuppressWarnings("unused") final Object receiver,
                    @SuppressWarnings("unused") @Cached("create(receiver)") final LookupClassGuard guard,
                    @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
        return guard.getSqueakClass(image);
    }

    @Specialization(replaces = "doCached")
    protected static final ClassObject doUncached(final Object receiver,
                    @Cached final SqueakObjectClassNode classNode) {
        return classNode.executeLookup(receiver);
    }
}
