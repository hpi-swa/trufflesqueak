/*
 * Copyright (c) 2025-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2025-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import java.util.Arrays;
import java.util.function.Supplier;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.CacheLimits;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNodeGen;

public abstract class AbstractDispatchNode<T extends AbstractDispatchDirectNode> extends AbstractNode {
    protected static final byte HAS_FAST = 1 << 0;
    protected static final byte HAS_WIDE = 1 << 1;
    protected static final byte HAS_INDIRECT = 1 << 2;
    protected static final byte FLAG_PRIM_FAIL = 1 << 3;

    protected final NativeObject selector;

    @CompilationFinal protected byte state;

    @SuppressWarnings("rawtypes")
    private static final DispatchEntry[] EMPTY_ENTRIES = new DispatchEntry[0];

    @Children protected DispatchEntry<T>[] fastEntries;
    @Children protected DispatchEntry<T>[] wideEntries;
    @Child protected SqueakObjectClassNode classNode;

    @SuppressWarnings("unchecked")
    AbstractDispatchNode(final NativeObject selector, final boolean canPrimFail) {
        this.selector = selector;
        this.fastEntries = (DispatchEntry<T>[]) EMPTY_ENTRIES;
        this.wideEntries = (DispatchEntry<T>[]) EMPTY_ENTRIES;
        this.state = canPrimFail ? FLAG_PRIM_FAIL : 0;
    }

    protected final boolean canPrimFail() {
        return (state & FLAG_PRIM_FAIL) != 0;
    }

    protected final void ensureClassNode() {
        if (classNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            classNode = insert(SqueakObjectClassNodeGen.create());
        }
    }

    @SuppressWarnings("unchecked")
    @TruffleBoundary
    protected final T convertToIndirect() {
        this.fastEntries = (DispatchEntry<T>[]) EMPTY_ENTRIES;
        this.wideEntries = (DispatchEntry<T>[]) EMPTY_ENTRIES;
        this.classNode = null;
        this.state = (byte) ((state & FLAG_PRIM_FAIL) | HAS_INDIRECT);
        return null;
    }

    @SuppressWarnings("unchecked")
    @TruffleBoundary
    protected final T specialize(final Object receiver, final ClassObject receiverClass, final Object lookupResult, final Supplier<T> nodeSupplier) {
        /*
         * THREAD SAFETY NOTE:
         * TruffleSqueak executes Smalltalk strictly on a single thread. Therefore, AST mutations
         * (like array cloning and Node insertion here) do not require getLock().lock() synchronization.
         *
         * If the VM architecture ever transitions to multithreaded execution, this method MUST be
         * wrapped in the node's intrinsic lock to prevent AST corruption and lost updates.
         */

        final DispatchEntry<T>[] newFastEntries = (DispatchEntry<T>[]) new DispatchEntry<?>[CacheLimits.DISPATCH_CACHE_LIMIT];
        final DispatchEntry<T>[] newWideEntries = (DispatchEntry<T>[]) new DispatchEntry<?>[CacheLimits.DISPATCH_CACHE_LIMIT];

        int fastEntriesNeeded = 0;
        CompiledCodeObject targetMethodToWiden = null;
        DispatchEntry<T> targetEntry = null;

        for (final DispatchEntry<T> current : fastEntries) {
            if (!current.isFastValid()) {
                continue;
            }

            if (targetEntry == null && lookupResult instanceof CompiledCodeObject targetMethod && current.methodOrNull == targetMethod) {
                if (current.append(receiver, receiverClass, targetMethod)) {
                    newFastEntries[fastEntriesNeeded++] = current;
                    targetEntry = current;
                } else {
                    targetMethodToWiden = targetMethod;
                    targetEntry = current;
                }
            } else {
                newFastEntries[fastEntriesNeeded++] = current;
            }
        }

        int wideEntriesNeeded = 0;
        for (final DispatchEntry<T> current : wideEntries) {
            if (current.isWideValid()) {
                newWideEntries[wideEntriesNeeded++] = current;
            }
        }

        if (targetMethodToWiden != null) {
            targetEntry.promoteToWide();
            newWideEntries[wideEntriesNeeded++] = targetEntry;
            this.fastEntries = insert(Arrays.copyOf(newFastEntries, fastEntriesNeeded));
            this.wideEntries = insert(Arrays.copyOf(newWideEntries, wideEntriesNeeded));
            this.state |= HAS_WIDE;
            if (fastEntriesNeeded > 0) {
                this.state |= HAS_FAST;
            } else {
                this.state &= ~HAS_FAST;
            }
            return targetEntry.executor;
        }

        if (wideEntriesNeeded != wideEntries.length) {
            this.wideEntries = insert(Arrays.copyOf(newWideEntries, wideEntriesNeeded));
        }

        if (targetEntry != null) {
            if (fastEntriesNeeded != fastEntries.length) {
                this.fastEntries = insert(Arrays.copyOf(newFastEntries, fastEntriesNeeded));
            }
            return targetEntry.executor;
        }

        if (fastEntriesNeeded + wideEntriesNeeded < CacheLimits.DISPATCH_CACHE_LIMIT) {
            final T newDispatchNode = nodeSupplier.get();
            final DispatchEntry<T> newEntry = new DispatchEntry<>(receiver, lookupResult, newDispatchNode);
            newFastEntries[fastEntriesNeeded++] = newEntry;
            this.fastEntries = insert(Arrays.copyOf(newFastEntries, fastEntriesNeeded));
            this.state |= HAS_FAST;
            return newDispatchNode;
        }

        return convertToIndirect();
    }

    public static final class DispatchEntry<T extends AbstractDispatchDirectNode> extends Node {
        public final CompiledCodeObject methodOrNull;
        @CompilationFinal public final Assumption callTargetStable;

        @CompilationFinal(dimensions = 1) private LookupClassGuard[] guards;
        @CompilationFinal(dimensions = 1) private Assumption[] unifiedAssumptions;

        @Child public T executor;

        public DispatchEntry(final Object receiver, final Object lookupResult, final T executor) {
            this.methodOrNull = lookupResult instanceof CompiledCodeObject m ? m : null;
            this.callTargetStable = methodOrNull != null ? methodOrNull.getCallTargetStable() : null;
            this.guards = new LookupClassGuard[]{LookupClassGuard.create(receiver)};
            this.unifiedAssumptions = executor.getAssumptions();
            this.executor = insert(executor);
        }

        @ExplodeLoop
        public boolean isFastCacheHit(final Object receiver) {
            if (!Assumption.isValidAssumption(unifiedAssumptions)) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                return false;
            }

            final LookupClassGuard[] currentGuards = guards;
            if (currentGuards != null) {
                for (int i = 0; i < currentGuards.length; i++) {
                    if (currentGuards[i].check(receiver)) {
                        return true;
                    }
                }
            }
            return false;
        }

        public boolean isWideCacheHit(final CompiledCodeObject targetMethod) {
            return methodOrNull == targetMethod && Assumption.isValidAssumption(callTargetStable);
        }

        public boolean isFastValid() {
            return guards != null && Assumption.isValidAssumption(unifiedAssumptions);
        }

        public boolean isWideValid() {
            return methodOrNull != null && Assumption.isValidAssumption(callTargetStable);
        }

        public void promoteToWide() {
            this.guards = null; // Drop fast-tier receiver checking to free memory
        }

        public boolean append(final Object receiver, final ClassObject receiverClass, final CompiledCodeObject targetMethod) {
            if (!Assumption.isValidAssumption(unifiedAssumptions)) {
                return false;
            }
            if (guards.length >= CacheLimits.LOOKUP_CACHE_LIMIT) {
                return false;
            }

            // Append Guard
            final LookupClassGuard[] newGuards = Arrays.copyOf(guards, guards.length + 1);
            newGuards[guards.length] = LookupClassGuard.create(receiver);
            this.guards = newGuards;

            // Union Assumptions
            Assumption[] newAssumptions = DispatchUtils.createAssumptions(receiverClass, targetMethod);
            if (newAssumptions.length > 0) {
                Assumption[] merged = Arrays.copyOf(unifiedAssumptions, unifiedAssumptions.length + newAssumptions.length);
                int count = unifiedAssumptions.length;
                for (Assumption newA : newAssumptions) {
                    boolean exists = false;
                    for (int i = 0; i < count; i++) {
                        if (merged[i] == newA) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        merged[count++] = newA;
                    }
                }
                this.unifiedAssumptions = Arrays.copyOf(merged, count);
            }
            return true;
        }
    }
}
