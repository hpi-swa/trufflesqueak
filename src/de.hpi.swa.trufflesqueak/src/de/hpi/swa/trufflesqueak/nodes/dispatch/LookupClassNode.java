/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodes.AbstractSelfSendNode;

@ImportStatic(AbstractSelfSendNode.class)
public abstract class LookupClassNode extends AbstractNode {

    public static LookupClassNode create() {
        return LookupClassNodeGen.create();
    }

    public abstract ClassObject execute(Object receiver);

    /* TODO: Use different guard for AbstractPointersObject. */
    @Specialization(guards = "classNode.executeLookup(receiver) == cachedClass", limit = "INLINE_CACHE_SIZE")
    protected static final ClassObject doCached(final Object receiver,
                    @Cached final SqueakObjectClassNode classNode,
                    @Cached("classNode.executeLookup(receiver)") final ClassObject cachedClass) {
        return cachedClass;
    }

    @Specialization(replaces = "doCached")
    protected static final ClassObject doUncached(final Object receiver,
                    @Cached final SqueakObjectClassNode classNode) {
        return classNode.executeLookup(receiver);
    }
}
