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

/*
 * Multi-tier dispatch architecture for method resolution and execution.
 *
 * Tier 0 (Monomorphic): Direct execution guarded by a single fast receiver check.
 *                       Bypasses wrapper objects and array iterations.
 * Tier 1 (Fast): Receiver-based polymorphism. Dispatches based on fast receiver guards.
 *                Bounded by DISPATCH_CACHE_LIMIT. Each entry maps
 *                up to LOOKUP_CACHE_LIMIT receiver types to a single method.
 * Tier 2 (Wide): Target-based polymorphism. Performs a full Smalltalk class and method
 *                dictionary lookup, dispatching based on the resolved target method.
 *                Bounded by DISPATCH_CACHE_LIMIT. Consolidates execution
 *                for deep hierarchies where many distinct classes inherit the same method.
 * Tier 3 (Indirect): Megamorphic fallback using uncached lookups and indirect calls.
 */
public abstract class AbstractDispatchNode<T extends AbstractDispatchDirectNode> extends AbstractNode {
    protected static final byte HAS_MONO = 1 << 0;
    protected static final byte HAS_FAST = 1 << 1;
    protected static final byte HAS_WIDE = 1 << 2;
    protected static final byte HAS_INDIRECT = 1 << 3;
    protected static final byte FLAG_PRIM_FAIL = 1 << 4;

    protected final NativeObject selector;

    @CompilationFinal protected byte state;

    @SuppressWarnings("rawtypes") private static final DispatchEntry[] EMPTY_ENTRIES = new DispatchEntry[0];

    @Children protected DispatchEntry<T>[] fastEntries;
    @Children protected DispatchEntry<T>[] wideEntries;
    @Child protected SqueakObjectClassNode classNode;

    @Child protected T monoExecutor;
    @CompilationFinal protected LookupClassGuard monoGuard;

    @SuppressWarnings("unchecked")
    AbstractDispatchNode(final NativeObject selector, final boolean canPrimFail) {
        this.selector = selector;
        this.fastEntries = EMPTY_ENTRIES;
        this.wideEntries = EMPTY_ENTRIES;
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

    protected static Assumption[] mergeAssumptions(final Assumption[] a1, final Assumption[] a2) {
        if (a2.length == 0) {
            return a1;
        }
        final Assumption[] merged = Arrays.copyOf(a1, a1.length + a2.length);
        int count = a1.length;
        for (final Assumption newA : a2) {
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
        return Arrays.copyOf(merged, count);
    }

    private T initializeMono(final Object receiver, final Supplier<T> nodeSupplier) {
        this.monoExecutor = insert(nodeSupplier.get());
        this.monoGuard = LookupClassGuard.create(receiver);
        this.state |= HAS_MONO;
        return monoExecutor;
    }

    @SuppressWarnings("unchecked")
    private T transitionMonoToFast(final Object receiver, final ClassObject receiverClass, final Object lookupResult, final Supplier<T> nodeSupplier) {
        final Assumption[] originalAssumptions = monoExecutor.getAssumptions();

        if (Assumption.isValidAssumption(originalAssumptions)) {
            // Convert the existing mono cache entry into a fast cache entry
            final ClassObject originalClass = monoGuard.getSqueakClassInternal(null);
            final Object originalLookupResult = getContext().lookup(originalClass, selector);
            final CompiledCodeObject originalMethod = originalLookupResult instanceof CompiledCodeObject m ? m : null;
            final Assumption originalCallTargetStable = originalMethod != null ? originalMethod.getCallTargetStable() : null;

            final DispatchEntry<T> monoEntry = new DispatchEntry<>(originalMethod, originalCallTargetStable,
                            new LookupClassGuard[]{monoGuard}, originalAssumptions);

            // Avoid reparenting issues: add new cache entry as our child first, then add monoEntry as its child
            this.fastEntries = insert((DispatchEntry<T>[]) new DispatchEntry<?>[]{monoEntry});
            monoEntry.executor = monoEntry.insert(monoExecutor);

            this.state |= HAS_FAST;
        }

        // RE-ENTER: Process the new class insertion
        this.state &= ~HAS_MONO;
        return specialize(receiver, receiverClass, lookupResult, nodeSupplier);
    }

    @SuppressWarnings("unchecked")
    @TruffleBoundary
    protected final void convertToIndirect() {
        this.state = (byte) ((state & FLAG_PRIM_FAIL) | HAS_INDIRECT);

        /* Clear child nodes and arrays to release memory. */
        this.monoGuard = null;
        this.monoExecutor = null;
        this.classNode = null;
        this.fastEntries = EMPTY_ENTRIES;
        this.wideEntries = EMPTY_ENTRIES;
    }

    /*
     * Note on concurrency: Smalltalk execution is strictly single-threaded.
     * If multiple OS threads of execution are ever permitted in the VM,
     * the non-atomic state bit transitions and cache array mutations in
     * this implementation will require synchronization and revision.
     */
    @SuppressWarnings("unchecked")
    @TruffleBoundary
    protected final T specialize(final Object receiver, final ClassObject receiverClass, final Object lookupResult, final Supplier<T> nodeSupplier) {

        // 0. Base Case: Uninitialized Node -> Enter Tier 0 (Mono)
        if ((state & (HAS_MONO | HAS_FAST | HAS_WIDE | HAS_INDIRECT)) == 0) {
            return initializeMono(receiver, nodeSupplier);
        }

        // 1. Transition Tier 0 (Mono) to Tier 1 (Fast) via Recursion
        if ((state & HAS_MONO) != 0) {
            return transitionMonoToFast(receiver, receiverClass, lookupResult, nodeSupplier);
        }

        // ------------------------------------------------------------------
        // Standard Fast/Wide Tier Processing (Tier 1 & Tier 2)
        // ------------------------------------------------------------------

        final DispatchEntry<T>[] newFastEntries = (DispatchEntry<T>[]) new DispatchEntry<?>[CacheLimits.DISPATCH_CACHE_LIMIT];
        final DispatchEntry<T>[] newWideEntries = (DispatchEntry<T>[]) new DispatchEntry<?>[CacheLimits.DISPATCH_CACHE_LIMIT];

        int fastEntriesNeeded = 0;
        CompiledCodeObject targetMethodToWiden = null;
        DispatchEntry<T> targetEntry = null;

        // 2. Process Fast Entries
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

        // 3. Process Wide Entries
        int wideEntriesNeeded = 0;
        for (final DispatchEntry<T> current : wideEntries) {
            if (current.isWideValid()) {
                newWideEntries[wideEntriesNeeded++] = current;
            }
        }

        // 4. Handle Wide Promotion
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

        // 5. Append New Fast Entry
        if (fastEntriesNeeded + wideEntriesNeeded < CacheLimits.DISPATCH_CACHE_LIMIT) {
            final T newDispatchNode = nodeSupplier.get();
            final DispatchEntry<T> newEntry = new DispatchEntry<>(receiver, lookupResult, newDispatchNode);
            newFastEntries[fastEntriesNeeded++] = newEntry;
            this.fastEntries = insert(Arrays.copyOf(newFastEntries, fastEntriesNeeded));
            this.state |= HAS_FAST;
            return newDispatchNode;
        }

        // Cache exhausted: convert to indirect
        return null;
    }

    public static final class DispatchEntry<T extends AbstractDispatchDirectNode> extends Node {
        public final CompiledCodeObject methodOrNull;
        @CompilationFinal public final Assumption callTargetStable;

        @CompilationFinal(dimensions = 1) private LookupClassGuard[] guards;
        @CompilationFinal(dimensions = 1) private Assumption[] unifiedAssumptions;

        @Child public T executor;

        // Normal Constructor
        public DispatchEntry(final Object receiver, final Object lookupResult, final T executor) {
            this.methodOrNull = lookupResult instanceof CompiledCodeObject m ? m : null;
            this.callTargetStable = methodOrNull != null ? methodOrNull.getCallTargetStable() : null;
            this.guards = new LookupClassGuard[]{LookupClassGuard.create(receiver)};
            this.unifiedAssumptions = executor.getAssumptions();
            this.executor = insert(executor);
        }

        // Internal Constructor for migrating Mono to Fast
        protected DispatchEntry(final CompiledCodeObject method, final Assumption callTargetStable, final LookupClassGuard[] guards, final Assumption[] unifiedAssumptions) {
            this.methodOrNull = method;
            this.callTargetStable = callTargetStable;
            this.guards = guards;
            this.unifiedAssumptions = unifiedAssumptions;
            // Executor is deliberately NOT inserted here to avoid Truffle reparenting issues
        }

        @ExplodeLoop
        public boolean isFastCacheHit(final Object receiver) {
            if (!Assumption.isValidAssumption(unifiedAssumptions)) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                return false;
            }

            final LookupClassGuard[] currentGuards = guards;
            if (currentGuards != null) {
                for (LookupClassGuard currentGuard : currentGuards) {
                    if (currentGuard.check(receiver)) {
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
            this.guards = null;
        }

        public boolean append(final Object receiver, final ClassObject receiverClass, final CompiledCodeObject targetMethod) {
            if (!Assumption.isValidAssumption(unifiedAssumptions)) {
                return false;
            }
            if (guards.length >= CacheLimits.LOOKUP_CACHE_LIMIT) {
                return false;
            }

            final LookupClassGuard[] newGuards = Arrays.copyOf(guards, guards.length + 1);
            newGuards[guards.length] = LookupClassGuard.create(receiver);
            this.guards = newGuards;

            this.unifiedAssumptions = mergeAssumptions(unifiedAssumptions, DispatchUtils.createAssumptions(receiverClass, targetMethod));
            return true;
        }
    }
}
