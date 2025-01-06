/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodes.SelfSendNode;

@SuppressWarnings("truffle-inlining")
@ImportStatic(SelfSendNode.class)
public abstract class LookupClassNode extends AbstractNode {

    public static LookupClassNode create() {
        return LookupClassNodeGen.create();
    }

    public abstract ClassObject execute(Object receiver);

    @Specialization(guards = "guard.check(receiver)", assumptions = "guard.getIsValidAssumption()", limit = "INLINE_METHOD_CACHE_LIMIT")
    protected static final ClassObject doCached(@SuppressWarnings("unused") final Object receiver,
                    @Bind("this") final Node node,
                    @Cached("create(receiver)") final LookupClassGuard guard) {
        return guard.getSqueakClass(node);
    }

    @Specialization(replaces = "doCached")
    protected static final ClassObject doGeneric(final Object receiver,
                    @Bind("this") final Node node,
                    @Cached final SqueakObjectClassNode classNode) {
        return classNode.executeLookup(node, receiver);
    }
}
