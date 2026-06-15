/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;

public class AbstractDisNode {
    private static final int LOOKUP_CACHE_SIZE = 8;
    protected static final int DISPATCH_CACHE_SIZE = 4;

    abstract static class AbstractGuardNode extends AbstractNode {
        abstract boolean execute(Object receiver);

        abstract boolean append(Object receiver, LookupResult result);
    }

    static class GuardChainNode extends AbstractGuardNode {
        @Child private GuardChainDataNode head;

        GuardChainNode(final Object receiver, final LookupResult result) {
            this.head = new GuardChainDataNode(receiver, result);
        }

        @Override
        @ExplodeLoop
        boolean execute(final Object receiver) {
            GuardChainDataNode current = head;
            while (current != null) {
                if (!Assumption.isValidAssumption(current.assumptions)) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    remove(current);
                } else if (current.guard.check(receiver)) {
                    return true;
                }
                current = current.next;
            }
            return false;
        }

        void remove(final GuardChainDataNode target) {
            assert head != null;
            GuardChainDataNode previous = null;
            GuardChainDataNode current = head;
            while (current != null) {
                if (current == target) {
                    if (previous == null) {
                        head = current.next;
                    } else {
                        previous.next = current.next;
                    }
                    return;
                }
                previous = current;
                current = current.next;
            }
        }

        @Override
        boolean append(final Object receiver, final LookupResult result) {
            if (head == null) {
                head = insert(new GuardChainDataNode(receiver, result));
                return true;
            }
            GuardChainDataNode current = head;
            int count = 0;
            while (current.next != null) {
                current = current.next;
                count++;
            }
            if (count < LOOKUP_CACHE_SIZE) {
                current.next = current.insert(new GuardChainDataNode(receiver, result));
                return true;
            } else {
                return false;
            }
        }
    }

    static class GuardChainDataNode extends AbstractNode {
        final LookupClassGuard guard;
        @CompilationFinal(dimensions = 1) final Assumption[] assumptions;

        @Child GuardChainDataNode next;

        GuardChainDataNode(final Object receiver, final LookupResult result) {
            guard = LookupClassGuard.create(receiver);
            assumptions = DispatchUtils.createAssumptions(result.receiverClass(), result.method());
        }
    }

    protected abstract static class GenericGuardNode extends AbstractGuardNode {
        final NativeObject selector;
        final CompiledCodeObject method;
        final int expectNumArgs;

        GenericGuardNode(final NativeObject selector, final CompiledCodeObject method, final int expectNumArgs) {
            this.selector = selector;
            this.method = method;
            this.expectNumArgs = expectNumArgs;
        }

        @Specialization
        boolean doGeneric(final Object receiver,
                        @Bind final Node node,
                        @Bind final SqueakImageContext image,
                        @Cached(inline = true) final SqueakObjectClassNode classNode,
                        @Cached final ResolveMethodNode methodNode) {
            final ClassObject receiverClass = classNode.executeLookup(node, receiver);
            final Object lookupResult = image.lookup(receiverClass, selector);
            final CompiledCodeObject targetMethod = methodNode.execute(node, image, expectNumArgs, false, selector, receiverClass, lookupResult);
            return method == targetMethod;
        }

        @Override
        boolean append(final Object receiver, final LookupResult result) {
            return true; /* Always successful */
        }
    }

    protected record LookupResult(NativeObject selector, ClassObject receiverClass, Object lookupResult, CompiledCodeObject method, LookupKind kind, Object targetObject) {
    }

    protected enum LookupKind {
        STANDARD_METHOD,
        DOES_NOT_UNDERSTAND,
        OBJECT_AS_METHOD,
    }
}
