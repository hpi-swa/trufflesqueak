/*
 * Copyright (c) 2025-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2025-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import java.util.Arrays;

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

public abstract class AbstractDispatchNode extends AbstractNode {
    protected final NativeObject selector;

    AbstractDispatchNode(final NativeObject selector) {
        this.selector = selector;
    }

    // --- Cache Manager and Data Nodes ---

    /**
     * This manager organizes dispatch caching into two distinct tiers to balance compilation size
     * and execution efficiency, using a parallel array architecture to preserve JIT profiling data.
     * <p>
     * 1. Fast Tier ({@code fastEntries}): Caches standard methods and unique fallback scenarios up to a
     * configured limit ({@code DISPATCH_CACHE_LIMIT}). Each entry maintains a chain of class guards.
     * <br>
     * 2. Wide Tier ({@code wideEntries}): Handles class polymorphism. When a standard method exceeds its
     * allotted class guard limit ({@code LOOKUP_CACHE_LIMIT}) in the fast tier, it is promoted here.
     * <p>
     * <b>Unified Entry Architecture:</b><br>
     * To bypass Truffle's strict tree-parenting constraints during fast-to-wide promotion, the execution
     * nodes are wrapped in a generic {@code DispatchEntry}. When promoted, the entry drops its fast-tier
     * AST guards to free memory and is directly moved from the {@code fastEntries} array to the
     * {@code wideEntries} array within the same parent node.
     * <p>
     * The manager is responsible for evaluating lookup results, transitioning nodes between tiers,
     * pruning invalidated cache entries, and signaling a transition to indirect execution when
     * the cache capacity is exhausted. Fallback mechanisms (e.g., #doesNotUnderstand) are strictly
     * isolated in the fast tier and are ineligible for wide tier promotion.
     *
     * @param <T> The type of direct dispatch node managed by this cache.
     */
    public static final class DispatchCacheManager<T extends AbstractDispatchDirectNode> extends Node {
        @Children public DispatchEntry<T>[] fastEntries;
        @Children public DispatchEntry<T>[] wideEntries;
        @Child public SqueakObjectClassNode classNode;

        @SuppressWarnings("unchecked")
        public DispatchCacheManager() {
            this.fastEntries = (DispatchEntry<T>[]) new DispatchEntry<?>[0];
            this.wideEntries = (DispatchEntry<T>[]) new DispatchEntry<?>[0];
            this.classNode = insert(SqueakObjectClassNodeGen.create());
        }

        @SuppressWarnings("unchecked")
        @TruffleBoundary
        protected T convertToIndirect() {
            // Safely drop the fast and wide tiers to free memory.
            this.fastEntries = insert((DispatchEntry<T>[]) new DispatchEntry<?>[0]);
            this.wideEntries = insert((DispatchEntry<T>[]) new DispatchEntry<?>[0]);
            this.classNode = null;
            return null;
        }

        @SuppressWarnings("unchecked")
        @TruffleBoundary
        protected T specialize(final Object receiver, final ClassObject receiverClass, final Object lookupResult, final java.util.function.Supplier<T> nodeSupplier) {
            final DispatchEntry<T>[] newFastEntries = (DispatchEntry<T>[]) new DispatchEntry<?>[CacheLimits.DISPATCH_CACHE_LIMIT];
            final DispatchEntry<T>[] newWideEntries = (DispatchEntry<T>[]) new DispatchEntry<?>[CacheLimits.DISPATCH_CACHE_LIMIT];

            int fastEntriesNeeded = 0;
            CompiledCodeObject targetMethodToWiden = null;
            DispatchEntry<T> targetEntry = null;

            // 1. Prune dead Fast entries and search for coalescing/promotion
            for (final DispatchEntry<T> current : fastEntries) {
                if (!current.isFastValid()) {
                    continue;
                }

                // Only coalesce standard methods. Fallbacks (null) and OAMs are isolated by class.
                if (targetEntry == null && lookupResult instanceof CompiledCodeObject targetMethod &&
                                current.methodOrNull == targetMethod) {

                    // Method matches fast entry: append new guard or transition to wide, if needed.
                    if (current.guardChainNode.append(receiver, receiverClass, targetMethod)) {
                        newFastEntries[fastEntriesNeeded++] = current;
                        targetEntry = current;
                    } else {
                        targetMethodToWiden = targetMethod;
                        targetEntry = current;
                    }
                } else {
                    // Node survives unchanged
                    newFastEntries[fastEntriesNeeded++] = current;
                }
            }

            // 2. Prune dead Wide entries
            int wideEntriesNeeded = 0;
            for (final DispatchEntry<T> current : wideEntries) {
                if (current.isWideValid()) {
                    newWideEntries[wideEntriesNeeded++] = current;
                }
            }

            // 3. Handle Wide transition by mutating and moving the entry
            if (targetMethodToWiden != null) {
                targetEntry.promoteToWide();
                newWideEntries[wideEntriesNeeded++] = targetEntry;
                this.fastEntries = insert(Arrays.copyOf(newFastEntries, fastEntriesNeeded));
                this.wideEntries = insert(Arrays.copyOf(newWideEntries, wideEntriesNeeded));
                return targetEntry.executor;
            }

            // Make sure Wide entries are up to date (used by Step 4 and 5)
            if (wideEntriesNeeded != wideEntries.length) {
                this.wideEntries = insert(Arrays.copyOf(newWideEntries, wideEntriesNeeded));
            }

            // 4. Return existing appended executor, if found
            if (targetEntry != null) {
                // Update AST only if dead fast entries were pruned
                if (fastEntriesNeeded != fastEntries.length) {
                    this.fastEntries = insert(Arrays.copyOf(newFastEntries, fastEntriesNeeded));
                }
                return targetEntry.executor;
            }

            // 5. Append new Fast entry, if cache has space
            if (fastEntriesNeeded + wideEntriesNeeded < CacheLimits.DISPATCH_CACHE_LIMIT) {
                // Dispatch Node is only built when we need a new entry
                final T newDispatchNode = nodeSupplier.get();
                final DispatchEntry<T> newEntry = new DispatchEntry<>(receiver, lookupResult, newDispatchNode);
                newFastEntries[fastEntriesNeeded++] = newEntry;
                this.fastEntries = insert(Arrays.copyOf(newFastEntries, fastEntriesNeeded));
                return newDispatchNode;
            }

            // Capacity exhausted
            return convertToIndirect();
        }
    }

    public static final class DispatchEntry<T extends AbstractDispatchDirectNode> extends Node {
        public final CompiledCodeObject methodOrNull;
        @CompilationFinal public final Assumption callTargetStable;

        @Child public GuardChainNode guardChainNode;
        @Child public T executor;

        public DispatchEntry(final Object receiver, final Object lookupResult, final T executor) {
            this.methodOrNull = lookupResult instanceof CompiledCodeObject m ? m : null;
            this.callTargetStable = methodOrNull != null ? methodOrNull.getCallTargetStable() : null;
            this.guardChainNode = insert(new GuardChainNode(receiver, executor.getAssumptions()));
            this.executor = insert(executor);
        }

        public boolean isFastCacheHit(final Object receiver) {
            final GuardChainNode chain = this.guardChainNode;
            if (chain == null) {
                return false;
            }
            return chain.execute(receiver);
        }

        public boolean isWideCacheHit(final CompiledCodeObject targetMethod) {
            return methodOrNull == targetMethod && Assumption.isValidAssumption(callTargetStable);
        }

        public boolean isFastValid() {
            final GuardChainNode chain = guardChainNode;
            return chain != null && chain.hasValidGuards();
        }

        public boolean isWideValid() {
            return methodOrNull != null && Assumption.isValidAssumption(callTargetStable);
        }

        public void promoteToWide() {
            this.guardChainNode = null; // Drop fast-tier receiver checking to free memory
        }
    }

    public static final class GuardChainNode extends AbstractNode {
        @CompilationFinal(dimensions = 1) private LookupClassGuard[] guards;
        @CompilationFinal(dimensions = 2) private Assumption[][] assumptions;

        public GuardChainNode(final Object receiver, final Assumption[] initialAssumptions) {
            this.guards = new LookupClassGuard[]{LookupClassGuard.create(receiver)};
            this.assumptions = new Assumption[][]{initialAssumptions};
        }

        public boolean isEmpty() {
            return guards.length == 0;
        }

        public boolean hasValidGuards() {
            final Assumption[][] currentAssumptions = assumptions;
            for (int i = 0; i < currentAssumptions.length; i++) {
                if (Assumption.isValidAssumption(currentAssumptions[i])) {
                    return true; // At least one guard is still alive
                }
            }
            return false;
        }

        @ExplodeLoop
        public boolean execute(final Object receiver) {
            final LookupClassGuard[] currentGuards = guards;
            final Assumption[][] currentAssumptions = assumptions;

            for (int i = 0; i < currentGuards.length; i++) {
                if (currentGuards[i].check(receiver)) {
                    if (Assumption.isValidAssumption(currentAssumptions[i])) {
                        return true;
                    } else {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        removeInvalid(currentGuards, currentAssumptions);
                        return false;
                    }
                }
            }
            return false;
        }

        public boolean append(final Object receiver, final ClassObject receiverClass, final CompiledCodeObject targetMethod) {
            final Assumption[][] currentAssumptions = assumptions;
            int validCount = 0;

            for (int i = 0; i < currentAssumptions.length; i++) {
                if (Assumption.isValidAssumption(currentAssumptions[i])) {
                    validCount++;
                }
            }

            if (validCount >= CacheLimits.LOOKUP_CACHE_LIMIT) {
                return false;
            }

            // Generate and add the new assumptions
            final Assumption[] newAssumptions = DispatchUtils.createAssumptions(receiverClass, targetMethod);

            final LookupClassGuard[] newGuards = new LookupClassGuard[validCount + 1];
            final Assumption[][] newAssumptionsArray = new Assumption[validCount + 1][];

            int index = 0;
            for (int i = 0; i < currentAssumptions.length; i++) {
                if (Assumption.isValidAssumption(currentAssumptions[i])) {
                    newGuards[index] = guards[i];
                    newAssumptionsArray[index] = currentAssumptions[i];
                    index++;
                }
            }

            newGuards[index] = LookupClassGuard.create(receiver);
            newAssumptionsArray[index] = newAssumptions;

            this.guards = newGuards;
            this.assumptions = newAssumptionsArray;
            return true;
        }

        @TruffleBoundary
        private void removeInvalid(final LookupClassGuard[] currentGuards, final Assumption[][] currentAssumptions) {
            int validCount = 0;
            for (int i = 0; i < currentAssumptions.length; i++) {
                if (Assumption.isValidAssumption(currentAssumptions[i])) {
                    validCount++;
                }
            }

            final LookupClassGuard[] newGuards = new LookupClassGuard[validCount];
            final Assumption[][] newAssumptionsArray = new Assumption[validCount][];

            int index = 0;
            for (int i = 0; i < currentAssumptions.length; i++) {
                if (Assumption.isValidAssumption(currentAssumptions[i])) {
                    newGuards[index] = currentGuards[i];
                    newAssumptionsArray[index] = currentAssumptions[i];
                    index++;
                }
            }

            this.guards = newGuards;
            this.assumptions = newAssumptionsArray;
        }
    }
}
