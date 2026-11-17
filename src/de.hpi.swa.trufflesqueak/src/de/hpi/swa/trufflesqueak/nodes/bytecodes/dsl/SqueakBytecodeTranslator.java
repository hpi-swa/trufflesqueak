/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.bytecodes.dsl;

import java.util.ArrayList;
import java.util.HashMap;

import com.oracle.truffle.api.bytecode.BytecodeConfig;
import com.oracle.truffle.api.bytecode.BytecodeLabel;
import com.oracle.truffle.api.bytecode.BytecodeLocal;
import com.oracle.truffle.api.bytecode.BytecodeParser;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.dsl.SqueakBytecodeIR.Instruction;
import de.hpi.swa.trufflesqueak.util.FrameAccess.DSLFrameInfo;
import de.hpi.swa.trufflesqueak.util.LogUtils;

/**
 * Translates Squeak bytecode (decoded into {@link SqueakBytecodeIR}) into Bytecode DSL operations.
 *
 * <p>
 * Squeak bytecode is a stack bytecode with unstructured jumps, whereas the Bytecode DSL only
 * supports structured control flow: backward jumps must be {@code While} loops and forward jumps
 * must target labels of an enclosing block. The translator therefore reconstructs the loop nesting
 * from backward jumps, emits every loop as {@code While(true) { Block }}, and turns all other jumps
 * into forward branches. Methods that cannot be structured this way are rejected with an
 * {@link UnsupportedBytecodeException}.
 *
 * <p>
 * Every Squeak stack slot (temporaries and operand stack) is mapped to a DSL local, so that frames
 * can be inspected, modified, and resumed through {@code ContextObject}s. A <em>resumption</em>
 * root additionally starts with a dispatch on the context's pc that branches to a label in front of
 * the instruction to resume at; loops repeat this dispatch for the pcs they contain.
 */
public final class SqueakBytecodeTranslator implements BytecodeParser<SqueakBytecodeRootNodeGen.Builder> {

    /** Thrown if the bytecode of a method cannot be translated. */
    public static final class UnsupportedBytecodeException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        UnsupportedBytecodeException(final String message) {
            super(message);
        }

        @SuppressWarnings("sync-override")
        @Override
        public Throwable fillInStackTrace() {
            return this;
        }
    }

    private static final class Loop {
        private final int headIndex;
        private int endIndex;
        private Loop parent;
        private boolean needsContinueLabel;
        /**
         * Index of the conditional jump that ends the condition of this loop, if the loop starts
         * with a condition that exits the loop (e.g., whileTrue: and to:do: loops), or -1.
         */
        private int conditionJumpIndex = -1;
        /* Per parse: */
        private BytecodeLabel continueLabel;

        /**
         * Whether this is not a loop but the body of a conditional: the instructions between a
         * conditional forward jump and its target, which are emitted as the body of an IfThen.
         */
        private final boolean isIf;

        private Loop(final int headIndex, final int endIndex) {
            this(headIndex, endIndex, false);
        }

        private Loop(final int headIndex, final int endIndex, final boolean isIf) {
            this.headIndex = headIndex;
            this.endIndex = endIndex;
            this.isIf = isIf;
        }
    }

    /** Emits an expression that produces whether a value is the expected boolean. */
    @FunctionalInterface
    private interface TestEmitter {
        void emit(boolean expected, int jumpPcAndSp);
    }

    private record DispatchEntry(int pc, BytecodeLabel label, boolean isLeaf) {
    }

    /** A state of the generic translation: an instruction together with the sp before it. */
    private record State(int pc, int sp, Instruction insn) {
        private int key() {
            return SqueakBytecodeRootNode.encodePCAndSP(pc, sp);
        }
    }

    private static final int SPECIAL_ADD = 0;
    private static final int SPECIAL_SUBTRACT = 1;
    private static final int SPECIAL_LESS_THAN = 2;
    private static final int SPECIAL_GREATER_THAN = 3;
    private static final int SPECIAL_LESS_OR_EQUAL = 4;
    private static final int SPECIAL_GREATER_OR_EQUAL = 5;
    private static final int SPECIAL_EQUAL = 6;
    private static final int SPECIAL_NOT_EQUAL = 7;
    private static final int SPECIAL_MULTIPLY = 8;
    private static final int SPECIAL_DIV = 13;
    private static final int SPECIAL_BIT_AND = 14;
    private static final int SPECIAL_BIT_OR = 15;
    private static final int SPECIAL_SIZE = 18;
    private static final int SPECIAL_IDENTICAL = 22;
    private static final int SPECIAL_CLASS = 23;
    private static final int SPECIAL_NOT_IDENTICAL = 24;
    private static final int SPECIAL_VALUE = 25;
    private static final int SPECIAL_VALUE_WITH_ARG = 26;

    private static final int MIN_NUMBER_OF_BYTECODE_FOR_INTERRUPT_CHECKS = 32;
    /** Frame slot of the local with offset 0 (slot 0 holds the bytecode index). */
    private static final int SLOT_OF_FIRST_LOCAL = DSLFrameInfo.STACK_START - DSLFrameInfo.NUM_RESERVED_LOCALS;

    private static final boolean USE_FUSED_SENDS = !Boolean.getBoolean("trufflesqueak.bytecode-dsl.no-fused-sends");
    private static final boolean USE_IF_REGIONS = !Boolean.getBoolean("trufflesqueak.bytecode-dsl.no-if-regions");

    /** For testing: translate all methods with the generic translation. */
    private static final boolean FORCE_GENERIC = Boolean.getBoolean("trufflesqueak.bytecode-dsl.force-generic");

    private final SqueakImageContext image;
    private final CompiledCodeObject code;
    private final boolean isResumption;
    /** Whether the generic translation (a state machine over pc and sp) is used. */
    private final boolean isGeneric;
    private final boolean isBlock;
    /** Additional state to start the analysis of the generic translation from (or -1). */
    private final int seedPC;
    private final int seedSP;

    /* Results of the analysis, independent of a builder (the parser may run multiple times). */
    private final Instruction[] instructions;
    private final int startPC;
    private final int[] pcToIndex;
    private Loop[] loops; // sorted by head index
    private Loop[] loopOf; // innermost loop per instruction
    private Loop[] loopStartingAt;
    private Loop[] ifRegionAt; // per conditional jump: the body that it guards (or null)
    private Loop[] loopConditionAt; // per conditional jump: the loop whose condition it ends (or null)
    private boolean[] needsLabel;
    private final boolean modifiesContext;

    /* Results of the analysis for the generic translation. */
    private State[] states; // sorted by pc and sp
    private HashMap<Integer, Integer> stateIndexByKey;
    private int[] dispatchEntries; // indices of states, sorted by key
    private final HashMap<Integer, Instruction> extraInstructions = new HashMap<>();
    private final int numStackSlots;
    private final HashMap<Integer, CompiledCodeObject> shadowBlocks = new HashMap<>();

    /* Per parse: */
    private SqueakBytecodeRootNodeGen.Builder b;
    private BytecodeLocal[] stack;
    private BytecodeLocal resumePendingLocal;
    private BytecodeLabel[] labels;
    private Runnable[] deferred;
    /**
     * For some deferred expressions: an alternative that directly produces the boolean for a
     * conditional jump, which saves the conversion of the value into a condition.
     */
    private TestEmitter[] deferredTest;
    /**
     * For deferred expressions that only read the receiver, a temporary, or an instance variable:
     * the encoded source (see SqueakBytecodeRootNode#loadSource), otherwise 0. Sends can read such
     * operands directly. A value is only deferred as long as nothing with side effects happened
     * since its push, so reading the source later yields the same value.
     */
    private int[] deferredSource;
    private BytecodeLabel[] stateLabels;
    private BytecodeLabel continueLabel;
    private int currentState;

    private SqueakBytecodeTranslator(final SqueakImageContext image, final CompiledCodeObject code, final boolean isResumption, final boolean isGeneric, final int seedPC, final int seedSP) {
        this.seedPC = seedPC;
        this.seedSP = seedSP;
        this.image = image;
        this.code = code;
        this.isResumption = isResumption;
        this.isGeneric = isGeneric;
        isBlock = code.isCompiledBlock() || code.isShadowBlock();
        instructions = SqueakBytecodeIR.decode(code);
        startPC = code.getStartPCZeroBased();
        numStackSlots = code.getMaxNumStackSlots();
        if (code.getBytes().length > SqueakBytecodeRootNode.MAX_PC || numStackSlots > SqueakBytecodeRootNode.MAX_SP) {
            throw SqueakException.create("Method is too large:", code);
        }
        final int numInstructions = instructions.length;

        pcToIndex = new int[code.getMaxPCZeroBased() - startPC + 1];
        java.util.Arrays.fill(pcToIndex, -1);
        for (int i = 0; i < numInstructions; i++) {
            pcToIndex[instructions[i].pc - startPC] = i;
        }

        modifiesContext = couldMutateContextVariables(isGeneric ? analyzeGeneric() : analyzeStructured());
    }

    /**
     * Finds loops and validates that the control flow can be expressed with structured loops and
     * forward branches.
     *
     * @return whether there are potential stores into instance variables of contexts
     */
    private boolean analyzeStructured() {
        final int numInstructions = instructions.length;
        SqueakBytecodeIR.computeStackPointers(instructions, startPC, code.getMaxPCZeroBased(), code.getInitialSP());

        /* Find loops. */
        final HashMap<Integer, Loop> loopsByHead = new HashMap<>();
        boolean hasPotentialContextIVStore = false;
        for (int i = 0; i < numInstructions; i++) {
            final Instruction insn = instructions[i];
            if (!insn.isReachable()) {
                continue;
            }
            if (insn.kind == SqueakBytecodeIR.UNKNOWN) {
                throw new UnsupportedBytecodeException("Unknown bytecode " + insn.a + " at pc " + insn.pc);
            }
            if (!isExecutable(insn, insn.spBefore)) {
                throw new UnsupportedBytecodeException("Invalid instruction " + insn);
            }
            if (!insn.isTerminator() && (i + 1 >= numInstructions || instructions[i + 1].pc != insn.nextPC)) {
                throw new UnsupportedBytecodeException("Control flow leaves the method at pc " + insn.pc);
            }
            if (insn.kind == SqueakBytecodeIR.STORE_RECEIVER_VARIABLE && insn.a < CONTEXT.INST_SIZE) {
                hasPotentialContextIVStore = true;
            }
            if (insn.isJump()) {
                final int targetIndex = indexOfPC(insn.target);
                if (targetIndex < 0 || !instructions[targetIndex].isReachable()) {
                    throw new UnsupportedBytecodeException("Jump into the middle of an instruction at pc " + insn.pc);
                }
                if (insn.isBackwardJump()) {
                    if (insn.kind != SqueakBytecodeIR.JUMP) {
                        throw new UnsupportedBytecodeException("Conditional backward jump at pc " + insn.pc);
                    }
                    final Loop loop = loopsByHead.get(targetIndex);
                    if (loop == null) {
                        loopsByHead.put(targetIndex, new Loop(targetIndex, i));
                    } else {
                        loop.endIndex = Math.max(loop.endIndex, i);
                    }
                }
            }
        }
        loops = loopsByHead.values().toArray(new Loop[0]);
        final ArrayList<Loop> regions = new ArrayList<>(java.util.Arrays.asList(loops));
        loopConditionAt = new Loop[numInstructions];
        if (!isResumption && USE_IF_REGIONS) {
            final int[] minSource = new int[numInstructions];
            final int[] maxSource = new int[numInstructions];
            java.util.Arrays.fill(minSource, Integer.MAX_VALUE);
            java.util.Arrays.fill(maxSource, -1);
            for (int i = 0; i < numInstructions; i++) {
                final Instruction insn = instructions[i];
                if (insn.isReachable() && insn.isJump()) {
                    final int targetIndex = indexOfPC(insn.target);
                    minSource[targetIndex] = Math.min(minSource[targetIndex], i);
                    maxSource[targetIndex] = Math.max(maxSource[targetIndex], i);
                }
            }
            findIfRegions(regions, minSource, maxSource);
            findLoopConditions(maxSource);
        }
        /* Sort from outer to inner regions. The body of a conditional encloses a loop with the same range. */
        regions.sort((r1, r2) -> r1.headIndex != r2.headIndex ? Integer.compare(r1.headIndex, r2.headIndex)
                        : r1.endIndex != r2.endIndex ? Integer.compare(r2.endIndex, r1.endIndex) : Boolean.compare(r2.isIf, r1.isIf));
        loopOf = new Loop[numInstructions];
        loopStartingAt = new Loop[numInstructions];
        ifRegionAt = new Loop[numInstructions];
        final ArrayList<Loop> open = new ArrayList<>();
        for (final Loop region : regions) {
            while (!open.isEmpty() && open.getLast().endIndex < region.headIndex) {
                open.removeLast();
            }
            if (!open.isEmpty()) {
                final Loop enclosing = open.getLast();
                if (enclosing.endIndex < region.endIndex) {
                    if (region.isIf) {
                        continue; // not properly nested: use a branch instead
                    }
                    throw new UnsupportedBytecodeException("Overlapping loops at pc " + instructions[region.headIndex].pc);
                }
                region.parent = enclosing;
            }
            open.add(region);
            if (region.isIf) {
                ifRegionAt[region.headIndex - 1] = region;
            } else {
                loopStartingAt[region.headIndex] = region;
            }
            /* Regions are sorted from outer to inner, so inner regions overwrite outer ones. */
            for (int i = region.headIndex; i <= region.endIndex; i++) {
                loopOf[i] = region;
            }
        }

        /* Validate jumps and determine labels. */
        needsLabel = new boolean[numInstructions];
        for (int i = 0; i < numInstructions; i++) {
            final Instruction insn = instructions[i];
            if (!insn.isReachable()) {
                continue;
            }
            if (isResumption) {
                needsLabel[i] = true;
            }
            if (insn.isJump()) {
                final int targetIndex = indexOfPC(insn.target);
                if (insn.isBackwardJump()) {
                    final Loop loop = findLoopWithHead(loopOf[i], targetIndex);
                    if (loop == null) {
                        throw new UnsupportedBytecodeException("Backward jump to a loop that does not contain it at pc " + insn.pc);
                    }
                    if (loop.endIndex != i) {
                        loop.needsContinueLabel = true;
                    }
                } else if (ifRegionAt[i] == null) {
                    final Loop owner = labelOwner(targetIndex);
                    if (!isSelfOrAncestor(owner, loopOf[i])) {
                        throw new UnsupportedBytecodeException("Forward jump into a loop at pc " + insn.pc);
                    }
                    needsLabel[targetIndex] = true;
                }
            }
        }
        return hasPotentialContextIVStore;
    }

    /**
     * Finds conditional forward jumps whose skipped instructions can be emitted as the body of an
     * IfThen operation, which needs fewer instructions than a conditional branch around them. This
     * requires that nothing outside of the body jumps into it and that the body does not overlap
     * with loops.
     */
    private void findIfRegions(final ArrayList<Loop> regions, final int[] minSource, final int[] maxSource) {
        final int numInstructions = instructions.length;
        nextJump: for (int i = 0; i < numInstructions; i++) {
            final Instruction insn = instructions[i];
            if (!insn.isReachable() || insn.kind != SqueakBytecodeIR.JUMP_TRUE && insn.kind != SqueakBytecodeIR.JUMP_FALSE) {
                continue;
            }
            final int first = i + 1;
            final int last = indexOfPC(insn.target) - 1;
            if (last < first) {
                continue;
            }
            for (int k = first; k <= last; k++) {
                if (maxSource[k] >= 0 && (minSource[k] < first || maxSource[k] > last)) {
                    continue nextJump; // jump into the body from outside
                }
            }
            for (final Loop loop : loops) {
                final boolean disjoint = loop.endIndex < first || loop.headIndex > last;
                final boolean inside = loop.headIndex >= first && loop.endIndex <= last;
                final boolean around = loop.headIndex <= i && loop.endIndex > last;
                if (!(disjoint || inside || around)) {
                    continue nextJump;
                }
            }
            regions.add(new Loop(first, last, true));
        }
    }

    /**
     * Finds loops that start with straight-line code followed by a conditional jump to the
     * instruction after the loop. This code is emitted as the condition of the While operation.
     */
    private void findLoopConditions(final int[] maxSource) {
        nextLoop: for (final Loop loop : loops) {
            for (int i = loop.headIndex; i < loop.endIndex; i++) {
                final Instruction insn = instructions[i];
                if (!insn.isReachable() || i > loop.headIndex && maxSource[i] >= 0) {
                    continue nextLoop; // not straight-line code
                }
                if (insn.kind == SqueakBytecodeIR.JUMP_TRUE || insn.kind == SqueakBytecodeIR.JUMP_FALSE) {
                    if (indexOfPC(insn.target) == loop.endIndex + 1) {
                        loop.conditionJumpIndex = i;
                        loopConditionAt[i] = loop;
                    }
                    continue nextLoop;
                } else if (insn.isJump() || insn.isTerminator()) {
                    continue nextLoop;
                }
            }
        }
    }

    /**
     * Enumerates all reachable states (pairs of pc and sp) for the generic translation. Unlike the
     * structured translation, this supports arbitrary control flow, different stack depths at the
     * same pc, and jumps into the middle of other instructions.
     *
     * @return whether there are potential stores into instance variables of contexts
     */
    private boolean analyzeGeneric() {
        final java.util.TreeMap<Long, State> found = new java.util.TreeMap<>();
        final java.util.ArrayDeque<int[]> worklist = new java.util.ArrayDeque<>();
        worklist.add(new int[]{startPC, code.getInitialSP()});
        if (seedPC >= 0) {
            worklist.add(new int[]{seedPC, seedSP});
        }
        boolean hasPotentialContextIVStore = false;
        while (!worklist.isEmpty()) {
            final int[] item = worklist.poll();
            final int pc = item[0];
            final int sp = item[1];
            final long sortKey = (long) pc << 32 | sp;
            if (sp < 0 || sp > numStackSlots || found.containsKey(sortKey)) {
                continue;
            }
            final Instruction insn = instructionAt(pc);
            if (insn == null) {
                continue;
            }
            found.put(sortKey, new State(pc, sp, insn));
            if (!isExecutable(insn, sp)) {
                continue; // fails at run time
            }
            if (insn.kind == SqueakBytecodeIR.STORE_RECEIVER_VARIABLE && insn.a < CONTEXT.INST_SIZE) {
                hasPotentialContextIVStore = true;
            }
            final int spAfter = sp + insn.stackDelta();
            if (insn.isJump()) {
                worklist.add(new int[]{insn.target, spAfter});
            }
            if (!insn.isTerminator()) {
                worklist.add(new int[]{insn.nextPC, spAfter});
            }
        }
        states = found.values().toArray(new State[0]);
        stateIndexByKey = new HashMap<>();
        for (int i = 0; i < states.length; i++) {
            stateIndexByKey.put(states[i].key(), i);
        }
        /*
         * States that must be reachable through the dispatch: all states in resumption roots,
         * otherwise only the initial state and the targets of backward jumps.
         */
        final boolean[] isEntry = new boolean[states.length];
        final Integer initialState = stateIndexByKey.get(SqueakBytecodeRootNode.encodePCAndSP(startPC, code.getInitialSP()));
        if (initialState != null) {
            isEntry[initialState] = true;
        }
        for (int i = 0; i < states.length; i++) {
            final State state = states[i];
            if (isResumption) {
                isEntry[i] = true;
            } else if (isExecutable(state.insn, state.sp)) {
                final int spAfter = state.sp + state.insn.stackDelta();
                if (state.insn.isJump()) {
                    markBackwardEntry(isEntry, i, state.insn.target, spAfter);
                }
                if (!state.insn.isTerminator()) {
                    markBackwardEntry(isEntry, i, state.insn.nextPC, spAfter);
                }
            }
        }
        final ArrayList<Integer> entries = new ArrayList<>();
        for (int i = 0; i < states.length; i++) {
            if (isEntry[i]) {
                entries.add(i);
            }
        }
        entries.sort((i1, i2) -> Integer.compare(states[i1].key(), states[i2].key()));
        dispatchEntries = entries.stream().mapToInt(Integer::intValue).toArray();
        return hasPotentialContextIVStore;
    }

    private void markBackwardEntry(final boolean[] isEntry, final int fromState, final int pc, final int sp) {
        final Integer target = stateIndexByKey.get(SqueakBytecodeRootNode.encodePCAndSP(pc, sp));
        if (target != null && target <= fromState) {
            isEntry[target] = true;
        }
    }

    /** Returns the instruction at the given pc or null if the pc is outside of the bytecodes. */
    private Instruction instructionAt(final int pc) {
        final int index = indexOfPC(pc);
        if (index >= 0) {
            return instructions[index];
        } else if (pc < 0 || pc >= code.getBytes().length) {
            return null;
        } else {
            return extraInstructions.computeIfAbsent(pc, p -> SqueakBytecodeIR.decodeAt(code, p));
        }
    }

    /** Whether the instruction can be translated and executed with the given stack depth. */
    private boolean isExecutable(final Instruction insn, final int sp) {
        final int pops = switch (insn.kind) {
            case SqueakBytecodeIR.DUP, SqueakBytecodeIR.POP, SqueakBytecodeIR.JUMP_TRUE, SqueakBytecodeIR.JUMP_FALSE, SqueakBytecodeIR.RETURN_TOP, SqueakBytecodeIR.BLOCK_RETURN_TOP, //
                SqueakBytecodeIR.STORE_RECEIVER_VARIABLE, SqueakBytecodeIR.STORE_LITERAL_VARIABLE, SqueakBytecodeIR.STORE_TEMPORARY, SqueakBytecodeIR.STORE_REMOTE_TEMP ->
                1;
            case SqueakBytecodeIR.PUSH_NEW_ARRAY -> insn.b != 0 ? insn.a : 0;
            case SqueakBytecodeIR.PUSH_FULL_CLOSURE -> insn.b + ((insn.c & SqueakBytecodeIR.FULL_CLOSURE_RECEIVER_ON_STACK) != 0 ? 1 : 0);
            case SqueakBytecodeIR.PUSH_CLOSURE -> insn.b;
            case SqueakBytecodeIR.SEND, SqueakBytecodeIR.SEND_SPECIAL -> insn.b + 1;
            case SqueakBytecodeIR.SEND_SUPER -> insn.b + 1 + insn.c;
            default -> 0;
        };
        if (sp < pops || sp + insn.stackDelta() > numStackSlots || sp + insn.stackDelta() < 0) {
            return false;
        }
        final int numLiterals = code.getNumLiterals();
        return switch (insn.kind) {
            case SqueakBytecodeIR.PUSH_TEMPORARY, SqueakBytecodeIR.STORE_TEMPORARY -> insn.a < numStackSlots;
            case SqueakBytecodeIR.PUSH_REMOTE_TEMP, SqueakBytecodeIR.STORE_REMOTE_TEMP -> insn.b < numStackSlots;
            case SqueakBytecodeIR.PUSH_LITERAL_CONSTANT, SqueakBytecodeIR.PUSH_LITERAL_VARIABLE, SqueakBytecodeIR.STORE_LITERAL_VARIABLE -> insn.a < numLiterals;
            case SqueakBytecodeIR.SEND, SqueakBytecodeIR.SEND_SUPER -> insn.a < numLiterals && code.getLiteral(insn.a) instanceof NativeObject;
            case SqueakBytecodeIR.PUSH_FULL_CLOSURE -> insn.a < numLiterals && code.getLiteral(insn.a) instanceof CompiledCodeObject;
            case SqueakBytecodeIR.JUMP, SqueakBytecodeIR.JUMP_TRUE, SqueakBytecodeIR.JUMP_FALSE -> insn.target >= 0 && insn.target <= SqueakBytecodeRootNode.MAX_PC;
            case SqueakBytecodeIR.UNKNOWN -> false;
            default -> true;
        };
    }

    /**
     * Creates the root node for the given method or block.
     *
     * @throws UnsupportedBytecodeException if the bytecode cannot be translated
     */
    public static SqueakBytecodeRootNode createRoot(final SqueakLanguage language, final SqueakImageContext image, final CompiledCodeObject code, final boolean isResumption) {
        SqueakBytecodeTranslator translator;
        try {
            if (FORCE_GENERIC) {
                throw new UnsupportedBytecodeException("Generic translation forced");
            }
            translator = new SqueakBytecodeTranslator(image, code, isResumption, false, -1, -1);
        } catch (final UnsupportedBytecodeException e) {
            LogUtils.BYTECODE_DSL.fine(() -> "Using generic translation for " + code + ": " + e.getMessage());
            translator = new SqueakBytecodeTranslator(image, code, isResumption, true, -1, -1);
        }
        return createRoot(language, code, translator);
    }

    /**
     * Creates a resumption root that can continue execution at the given pc and sp, even if this
     * state is not reachable from the beginning of the method (e.g., after the pc or the stack of
     * a context have been modified).
     */
    public static SqueakBytecodeRootNode createResumptionRoot(final SqueakLanguage language, final SqueakImageContext image, final CompiledCodeObject code, final int pc, final int sp) {
        LogUtils.BYTECODE_DSL.fine(() -> "Using generic translation to resume " + code + " at pc " + pc + " with sp " + sp);
        return createRoot(language, code, new SqueakBytecodeTranslator(image, code, true, true, pc, sp));
    }

    private static SqueakBytecodeRootNode createRoot(final SqueakLanguage language, final CompiledCodeObject code, final SqueakBytecodeTranslator translator) {
        final SqueakBytecodeRootNode root = SqueakBytecodeRootNodeGen.create(language, BytecodeConfig.DEFAULT, translator).getNode(0);
        root.initialize(code, translator.modifiesContext, translator.getResumableStates());
        return root;
    }

    private int[] getResumableStates() {
        final int[] keys;
        if (isGeneric) {
            keys = new int[states.length];
            for (int i = 0; i < states.length; i++) {
                keys[i] = states[i].key();
            }
        } else {
            int count = 0;
            for (final Instruction insn : instructions) {
                count += insn.isReachable() ? 1 : 0;
            }
            keys = new int[count];
            int i = 0;
            for (final Instruction insn : instructions) {
                if (insn.isReachable()) {
                    keys[i++] = SqueakBytecodeRootNode.encodePCAndSP(insn.pc, insn.spBefore);
                }
            }
        }
        java.util.Arrays.sort(keys);
        return keys;
    }

    private int indexOfPC(final int pc) {
        final int offset = pc - startPC;
        return offset < 0 || offset >= pcToIndex.length ? -1 : pcToIndex[offset];
    }

    private static Loop findLoopWithHead(final Loop start, final int headIndex) {
        Loop loop = start;
        while (loop != null && (loop.isIf || loop.headIndex != headIndex)) {
            loop = loop.parent;
        }
        return loop;
    }

    private static boolean isSelfOrAncestor(final Loop candidate, final Loop start) {
        if (candidate == null) {
            return true; // root
        }
        Loop loop = start;
        while (loop != null) {
            if (loop == candidate) {
                return true;
            }
            loop = loop.parent;
        }
        return false;
    }

    /**
     * Returns the loop (or {@code null} for the root block) in whose block the label of the given
     * instruction is emitted. The label of a loop head is emitted in front of the loop.
     */
    private Loop labelOwner(final int index) {
        final Loop loop = loopOf[index];
        return loop != null && !loop.isIf && loop.headIndex == index ? loop.parent : loop;
    }

    /** See AbstractInterpreterNode#couldMutateContextVariables. */
    private boolean couldMutateContextVariables(final boolean hasPotentialContextIVStore) {
        if (hasPotentialContextIVStore) {
            final ClassObject methodClass = code.getMethod().getMethodClassOrNullSlow();
            if (methodClass == null) {
                return true;
            }
            final ClassObject contextClass = image.methodContextClass;
            final int methodClassInstSize = methodClass.getBasicInstanceSize();
            if (methodClassInstSize > 0 && methodClassInstSize <= contextClass.getBasicInstanceSize()) {
                ClassObject current = contextClass;
                do {
                    if (methodClass == current) {
                        return true;
                    }
                    current = current.getSuperclassOrNull();
                } while (current != null && current.getBasicInstanceSize() > 0);
            }
        }
        return false;
    }

    /*
     * Emission
     */

    @Override
    public void parse(final SqueakBytecodeRootNodeGen.Builder builder) {
        b = builder;
        b.beginRoot();
        for (int i = 0; i < DSLFrameInfo.NUM_RESERVED_LOCALS; i++) {
            b.createLocal(); // context, pc and sp
        }
        stack = new BytecodeLocal[numStackSlots];
        for (int i = 0; i < numStackSlots; i++) {
            stack[i] = b.createLocal();
        }
        assert numStackSlots == 0 || stack[0].getLocalOffset() == DSLFrameInfo.NUM_RESERVED_LOCALS;
        labels = new BytecodeLabel[instructions.length];
        deferred = new Runnable[numStackSlots];
        deferredTest = new TestEmitter[numStackSlots];
        deferredSource = new int[numStackSlots];

        b.beginBlock();
        if (isResumption) {
            resumePendingLocal = b.createLocal();
            b.emitResumeContext();
            b.beginStoreLocal(resumePendingLocal);
            b.emitLoadConstant(true);
            b.endStoreLocal();
        } else {
            final boolean checkForInterrupts = !(image.interruptHandlerDisabled() || code.hasPrimitive() || code.getBytes().length < MIN_NUMBER_OF_BYTECODE_FOR_INTERRUPT_CHECKS || isBlock);
            b.emitEnterMethod(SqueakBytecodeRootNode.encodePCAndSP(startPC, code.getInitialSP()), checkForInterrupts);
        }
        if (isGeneric) {
            emitGenericBody();
        } else {
            emitStructuredBody();
        }
        assert assertNoDeferredValues();
        b.endBlock();
        b.endRoot();
        b = null;
        stack = null;
        labels = null;
        deferred = null;
        deferredTest = null;
        deferredSource = null;
        stateLabels = null;
        continueLabel = null;
    }

    private void emitStructuredBody() {
        createLabels(null, 0, instructions.length - 1);
        if (isResumption) {
            emitDispatch(null, 0, instructions.length - 1, null);
        }
        for (int i = 0; i < instructions.length; i++) {
            final Instruction insn = instructions[i];
            if (!insn.isReachable()) {
                closeRegionsEndingAt(i);
                continue;
            }
            final Loop loop = loopStartingAt[i];
            if (labels[i] != null || loop != null) {
                /* Control flow merges here: all values must be in their stack slots. */
                flushAll();
            }
            if (labels[i] != null) {
                b.emitLabel(labels[i]);
            }
            if (loop != null) {
                b.beginWhile();
                if (loop.conditionJumpIndex >= 0) {
                    /* The condition: closed by its conditional jump, which then begins the body. */
                    b.beginBlock();
                } else {
                    b.emitLoadConstant(true);
                    beginLoopBody(loop);
                }
            }
            emitInstruction(i, insn, insn.spBefore);
            closeRegionsEndingAt(i);
        }
    }

    private void beginLoopBody(final Loop loop) {
        b.beginBlock();
        createLabels(loop, loop.headIndex, loop.endIndex);
        if (loop.needsContinueLabel) {
            loop.continueLabel = b.createLabel();
        }
        if (isResumption) {
            final BytecodeLabel bodyStart = b.createLabel();
            b.beginIfThen();
            b.emitLoadLocal(resumePendingLocal);
            b.beginBlock();
            emitDispatch(loop, loop.headIndex, loop.endIndex, bodyStart);
            b.endBlock();
            b.endIfThen();
            b.emitLabel(bodyStart);
        }
    }

    private void closeRegionsEndingAt(final int index) {
        Loop ending = loopOf[index];
        while (ending != null && ending.endIndex == index) {
            if (ending.isIf) {
                /* Control flow merges after the body: all values must be in their stack slots. */
                flushAll();
                b.endBlock();
                b.endIfThen();
            } else {
                if (ending.needsContinueLabel) {
                    b.emitLabel(ending.continueLabel);
                }
                b.endBlock();
                b.endWhile();
            }
            ending = ending.parent;
        }
    }

    /**
     * Emits the generic translation: a loop around a dispatch on the current state (the pc and sp
     * stored in the frame) followed by the code of all states. Jumps to states that are emitted
     * later are forward branches; all other jumps set the state and continue with the next
     * iteration of the loop. The Squeak stack is always kept in the stack slots, so this shape is
     * used for both normal and resumption roots.
     */
    private void emitGenericBody() {
        b.beginWhile();
        b.emitLoadConstant(true);
        b.beginBlock();
        continueLabel = b.createLabel();
        stateLabels = new BytecodeLabel[states.length];
        for (int i = 0; i < states.length; i++) {
            stateLabels[i] = b.createLabel();
        }
        if (dispatchEntries.length == 0) {
            b.emitUnsupportedBytecode("Invalid initial state");
        } else {
            emitGenericDispatch(0, dispatchEntries.length - 1);
        }
        for (int i = 0; i < states.length; i++) {
            final State state = states[i];
            final Instruction insn = state.insn;
            currentState = i;
            b.emitLabel(stateLabels[i]);
            if (!isExecutable(insn, state.sp)) {
                b.emitSetState(state.key());
                b.emitUnsupportedBytecode(insn.kind == SqueakBytecodeIR.UNKNOWN ? "Unknown bytecode " + insn.a : "Invalid bytecode or stack depth");
                continue;
            }
            emitInstruction(-1, insn, state.sp);
            if (!insn.isTerminator()) {
                final int spAfter = state.sp + insn.stackDelta();
                final Integer next = stateIndexByKey.get(SqueakBytecodeRootNode.encodePCAndSP(insn.nextPC, spAfter));
                if (next == null || next != i + 1) {
                    emitGenericJump(insn.nextPC, spAfter);
                }
            }
        }
        b.emitLabel(continueLabel);
        b.endBlock();
        b.endWhile();
    }

    private void emitGenericDispatch(final int low, final int high) {
        if (low == high) {
            final int stateIndex = dispatchEntries[low];
            b.beginIfThen();
            b.emitStateEquals(states[stateIndex].key());
            b.emitBranch(stateLabels[stateIndex]);
            b.endIfThen();
            b.emitUnsupportedBytecode("Cannot continue execution");
        } else {
            final int mid = (low + high) >>> 1;
            b.beginIfThenElse();
            b.emitStateLessThan(states[dispatchEntries[mid + 1]].key());
            b.beginBlock();
            emitGenericDispatch(low, mid);
            b.endBlock();
            b.beginBlock();
            emitGenericDispatch(mid + 1, high);
            b.endBlock();
            b.endIfThenElse();
        }
    }

    /** Emits a jump to the state with the given pc and sp. */
    private void emitGenericJump(final int pc, final int sp) {
        final Integer target = pc < 0 || pc > SqueakBytecodeRootNode.MAX_PC || sp < 0 ? null : stateIndexByKey.get(SqueakBytecodeRootNode.encodePCAndSP(pc, sp));
        if (target == null) {
            b.emitUnsupportedBytecode("Invalid jump to pc " + pc + " with sp " + sp);
        } else if (target > currentState) {
            b.emitBranch(stateLabels[target]);
        } else {
            b.emitSetState(states[target].key());
            if (!image.interruptHandlerDisabled()) {
                b.emitCheckForInterruptsInLoop(states[target].key());
            }
            b.emitBranch(continueLabel);
        }
    }

    /** Creates the labels that are emitted directly in the block of the given loop (or root). */
    private void createLabels(final Loop owner, final int firstIndex, final int lastIndex) {
        for (int i = firstIndex; i <= lastIndex; i++) {
            if (needsLabel[i] && labelOwner(i) == owner) {
                labels[i] = b.createLabel();
            }
        }
    }

    /**
     * Emits a binary search over the context's pc that branches to the label of the instruction to
     * resume at. For pcs within a nested loop, it branches to the label in front of that loop,
     * which then continues the dispatch.
     */
    private void emitDispatch(final Loop owner, final int firstIndex, final int lastIndex, final BytecodeLabel bodyStart) {
        final ArrayList<DispatchEntry> entries = new ArrayList<>();
        if (bodyStart != null) {
            entries.add(new DispatchEntry(instructions[firstIndex].pc, bodyStart, true));
        }
        for (int i = firstIndex; i <= lastIndex; i++) {
            if (labels[i] != null && labelOwner(i) == owner) {
                entries.add(new DispatchEntry(instructions[i].pc, labels[i], loopStartingAt[i] == null));
            }
        }
        emitDispatchTree(entries, 0, entries.size() - 1);
    }

    private void emitDispatchTree(final ArrayList<DispatchEntry> entries, final int low, final int high) {
        if (low == high) {
            final DispatchEntry entry = entries.get(low);
            if (entry.isLeaf) {
                b.beginStoreLocal(resumePendingLocal);
                b.emitLoadConstant(false);
                b.endStoreLocal();
            }
            b.emitBranch(entry.label);
        } else {
            final int mid = (low + high) >>> 1;
            b.beginIfThenElse();
            b.emitResumePCLessThan(entries.get(mid + 1).pc);
            b.beginBlock();
            emitDispatchTree(entries, low, mid);
            b.endBlock();
            b.beginBlock();
            emitDispatchTree(entries, mid + 1, high);
            b.endBlock();
            b.endIfThenElse();
        }
    }

    /*
     * Abstract stack
     *
     * In normal roots, values pushed onto the Squeak stack are not stored into their stack slot
     * right away. Instead, the expression is deferred and emitted as an operand of the operation
     * that consumes it, which avoids most loads and stores of stack slots. Deferred expressions
     * are emitted in stack order (i.e., in the order in which they were pushed), either as
     * operands of their consumer or when they are spilled into their stack slot, so the order of
     * evaluation is never changed. They must be spilled before:
     * (1) any statement (stores, jumps, labels, returns), and
     * (2) any operation during which the frame can be observed (sends), for all values below the
     * operands of that operation. This guarantees that the stack slots hold the complete Squeak
     * stack whenever the frame is suspended, inspected, or resumed.
     *
     * Resumption roots do not defer anything: every stack slot is stored immediately.
     */

    private void push(final int slot, final Runnable expression) {
        if (isResumption || isGeneric) {
            b.beginStoreLocal(stack[slot]);
            expression.run();
            b.endStoreLocal();
        } else {
            assert deferred[slot] == null;
            deferred[slot] = expression;
        }
    }

    private void pushSource(final int slot, final int source, final Runnable expression) {
        push(slot, expression);
        if (USE_FUSED_SENDS && deferred[slot] != null) {
            deferredSource[slot] = source;
        }
    }

    /** Removes the value in the given slot from the abstract stack and returns its expression. */
    private Runnable take(final int slot) {
        final Runnable expression = deferred[slot];
        if (expression != null) {
            deferred[slot] = null;
            deferredTest[slot] = null;
            deferredSource[slot] = 0;
            return expression;
        } else {
            return () -> b.emitLoadLocal(stack[slot]);
        }
    }

    private Runnable[] take(final int firstSlot, final int count) {
        final Runnable[] expressions = new Runnable[count];
        for (int i = 0; i < count; i++) {
            expressions[i] = take(firstSlot + i);
        }
        return expressions;
    }

    private static void emitAll(final Runnable[] expressions) {
        for (final Runnable expression : expressions) {
            expression.run();
        }
    }

    /** Spills all deferred values below the given slot into their stack slots. */
    private void flush(final int limit) {
        for (int slot = 0; slot < limit; slot++) {
            final Runnable expression = deferred[slot];
            if (expression != null) {
                deferred[slot] = null;
                deferredTest[slot] = null;
            deferredSource[slot] = 0;
                b.beginStoreLocal(stack[slot]);
                expression.run();
                b.endStoreLocal();
            }
        }
    }

    private void flushAll() {
        flush(deferred.length);
    }

    private boolean assertNoDeferredValues() {
        for (final Runnable expression : deferred) {
            assert expression == null;
        }
        return true;
    }

    private static int pcAndSp(final int pc, final int sp) {
        return SqueakBytecodeRootNode.encodePCAndSP(pc, sp);
    }

    private void emitInstruction(final int index, final Instruction insn, final int sp) {
        switch (insn.kind) {
            case SqueakBytecodeIR.PUSH_RECEIVER_VARIABLE -> pushSource(sp, SqueakBytecodeRootNode.SOURCE_RECEIVER_VARIABLE | insn.a, () -> b.emitPushReceiverVariable(insn.a));
            case SqueakBytecodeIR.PUSH_LITERAL_VARIABLE -> push(sp, () -> b.emitPushLiteralVariable(insn.a));
            case SqueakBytecodeIR.PUSH_LITERAL_CONSTANT -> {
                final Object literal = code.getLiteral(insn.a);
                if (literal instanceof Long || literal instanceof Double || literal instanceof Boolean || literal == NilObject.SINGLETON) {
                    push(sp, () -> b.emitLoadConstant(literal));
                } else {
                    push(sp, () -> b.emitPushLiteralConstant(insn.a));
                }
            }
            case SqueakBytecodeIR.PUSH_TEMPORARY -> {
                final int slot = stack[insn.a].getLocalOffset() + SLOT_OF_FIRST_LOCAL;
                pushSource(sp, SqueakBytecodeRootNode.SOURCE_TEMP | slot, () -> b.emitPushTemp(slot));
            }
            case SqueakBytecodeIR.PUSH_RECEIVER -> pushSource(sp, SqueakBytecodeRootNode.SOURCE_SELF, () -> b.emitPushReceiver());
            case SqueakBytecodeIR.PUSH_CONSTANT -> push(sp, () -> b.emitLoadConstant(insn.constant));
            case SqueakBytecodeIR.PUSH_ACTIVE_CONTEXT -> push(sp, () -> b.emitPushActiveContext());
            case SqueakBytecodeIR.DUP -> {
                /* The duplicated value is needed twice: make sure it is in its stack slot. */
                flush(sp);
                push(sp, () -> b.emitLoadLocal(stack[sp - 1]));
            }
            case SqueakBytecodeIR.POP -> {
                final Runnable expression = deferred[sp - 1];
                if (expression != null) {
                    /* Evaluate for side effects, the result is discarded. */
                    flush(sp - 1);
                    deferred[sp - 1] = null;
                    deferredTest[sp - 1] = null;
                    deferredSource[sp - 1] = 0;
                    expression.run();
                }
            }
            case SqueakBytecodeIR.NOP -> {
                // nothing to do
            }
            case SqueakBytecodeIR.STORE_RECEIVER_VARIABLE -> {
                final Runnable value = takeForStore(sp, insn.b != 0);
                b.beginStoreReceiverVariable(insn.a);
                value.run();
                b.endStoreReceiverVariable();
            }
            case SqueakBytecodeIR.STORE_LITERAL_VARIABLE -> {
                final Runnable value = takeForStore(sp, insn.b != 0);
                b.beginStoreLiteralVariable(insn.a);
                value.run();
                b.endStoreLiteralVariable();
            }
            case SqueakBytecodeIR.STORE_TEMPORARY -> {
                final Runnable value = takeForStore(sp, insn.b != 0);
                b.beginStoreLocal(stack[insn.a]);
                value.run();
                b.endStoreLocal();
            }
            case SqueakBytecodeIR.PUSH_REMOTE_TEMP -> push(sp, () -> {
                b.beginPushRemoteTemp(insn.a);
                b.emitPushTemp(stack[insn.b].getLocalOffset() + SLOT_OF_FIRST_LOCAL);
                b.endPushRemoteTemp();
            });
            case SqueakBytecodeIR.STORE_REMOTE_TEMP -> {
                final Runnable value = takeForStore(sp, insn.c != 0);
                b.beginStoreRemoteTemp(insn.a);
                b.emitPushTemp(stack[insn.b].getLocalOffset() + SLOT_OF_FIRST_LOCAL);
                value.run();
                b.endStoreRemoteTemp();
            }
            case SqueakBytecodeIR.PUSH_NEW_ARRAY -> {
                if (insn.b != 0) {
                    final int first = sp - insn.a;
                    final Runnable[] values = take(first, insn.a);
                    push(first, () -> {
                        b.beginPushNewArrayWithValues();
                        emitAll(values);
                        b.endPushNewArrayWithValues();
                    });
                } else {
                    push(sp, () -> b.emitPushNewArray(insn.a));
                }
            }
            case SqueakBytecodeIR.PUSH_FULL_CLOSURE -> {
                final int numCopied = insn.b;
                final boolean ignoreOuterContext = (insn.c & SqueakBytecodeIR.FULL_CLOSURE_IGNORE_OUTER_CONTEXT) != 0;
                final int firstCopied = sp - numCopied;
                if ((insn.c & SqueakBytecodeIR.FULL_CLOSURE_RECEIVER_ON_STACK) != 0) {
                    final Runnable[] receiverAndCopied = take(firstCopied - 1, numCopied + 1);
                    push(firstCopied - 1, () -> {
                        b.beginPushFullClosureWithReceiver(insn.a, ignoreOuterContext);
                        emitAll(receiverAndCopied);
                        b.endPushFullClosureWithReceiver();
                    });
                } else {
                    final Runnable[] copied = take(firstCopied, numCopied);
                    push(firstCopied, () -> {
                        b.beginPushFullClosure(insn.a, ignoreOuterContext);
                        emitAll(copied);
                        b.endPushFullClosure();
                    });
                }
            }
            case SqueakBytecodeIR.PUSH_CLOSURE -> {
                final int numCopied = insn.b;
                final int blockStartPC = insn.nextPC - insn.c;
                final CompiledCodeObject shadowBlock = shadowBlocks.computeIfAbsent(blockStartPC, pc -> code.createShadowBlock(code.getInitialPC() + pc, insn.a, numCopied, insn.c));
                final int firstCopied = sp - numCopied;
                final Runnable[] copied = take(firstCopied, numCopied);
                push(firstCopied, () -> {
                    b.beginPushClosure(shadowBlock);
                    emitAll(copied);
                    b.endPushClosure();
                });
            }
            case SqueakBytecodeIR.SEND -> emitSend((NativeObject) code.getAndResolveLiteral(insn.a), insn.b, sp, insn.nextPC);
            case SqueakBytecodeIR.SEND_SPECIAL -> emitSpecialSend(insn, sp);
            case SqueakBytecodeIR.SEND_SUPER -> {
                final NativeObject selector = (NativeObject) code.getAndResolveLiteral(insn.a);
                final int numArgs = insn.b;
                if (insn.c != 0) {
                    final int classSlot = sp - 1;
                    final int receiverSlot = classSlot - numArgs - 1;
                    /*
                     * The lookup class is on top of the stack but is the first operand of the
                     * operation: spill all operands to keep the order of evaluation.
                     */
                    flush(sp);
                    final Runnable[] receiverAndArguments = take(receiverSlot, numArgs + 1);
                    final Runnable lookupClass = take(classSlot);
                    push(receiverSlot, () -> {
                        b.beginDirectedSuperSend(selector, pcAndSp(insn.nextPC, receiverSlot));
                        lookupClass.run();
                        emitAll(receiverAndArguments);
                        b.endDirectedSuperSend();
                    });
                } else {
                    final int receiverSlot = sp - numArgs - 1;
                    flush(receiverSlot);
                    final Runnable[] receiverAndArguments = take(receiverSlot, numArgs + 1);
                    push(receiverSlot, () -> {
                        b.beginSuperSend(selector, pcAndSp(insn.nextPC, receiverSlot));
                        emitAll(receiverAndArguments);
                        b.endSuperSend();
                    });
                }
            }
            case SqueakBytecodeIR.JUMP -> {
                flushAll();
                if (isGeneric) {
                    emitGenericJump(insn.target, sp);
                    return;
                }
                final int targetIndex = indexOfPC(insn.target);
                if (insn.isBackwardJump()) {
                    emitInterruptCheck(index, targetIndex, sp);
                    final Loop loop = findLoopWithHead(loopOf[index], targetIndex);
                    if (loop.endIndex != index) {
                        b.emitBranch(loop.continueLabel);
                    }
                    /* Otherwise: fall through to the end of the loop body. */
                } else {
                    b.emitBranch(labels[targetIndex]);
                }
            }
            case SqueakBytecodeIR.JUMP_TRUE, SqueakBytecodeIR.JUMP_FALSE -> {
                final int pcAndSp = pcAndSp(insn.nextPC, sp - 1);
                flush(sp - 1);
                final TestEmitter test = deferredTest[sp - 1];
                final Runnable condition = take(sp - 1);
                final Loop body = isGeneric ? null : ifRegionAt[index];
                final Loop conditionOfLoop = isGeneric ? null : loopConditionAt[index];
                /* A body is executed if the jump is not taken; a branch if the jump is taken. */
                final boolean expected = insn.kind == SqueakBytecodeIR.JUMP_TRUE ^ (body != null || conditionOfLoop != null);
                if (conditionOfLoop == null) {
                    b.beginIfThen();
                }
                if (test != null) {
                    test.emit(expected, pcAndSp);
                } else if (expected) {
                    b.beginIsTrue(pcAndSp);
                    condition.run();
                    b.endIsTrue();
                } else {
                    b.beginIsFalse(pcAndSp);
                    condition.run();
                    b.endIsFalse();
                }
                if (conditionOfLoop != null) {
                    b.endBlock(); // condition
                    beginLoopBody(conditionOfLoop);
                    return;
                } else if (body != null) {
                    /* The body is closed after its last instruction. */
                    b.beginBlock();
                    createLabels(body, body.headIndex, body.endIndex);
                    return;
                } else if (isGeneric) {
                    b.beginBlock();
                    emitGenericJump(insn.target, sp - 1);
                    b.endBlock();
                } else {
                    b.emitBranch(labels[indexOfPC(insn.target)]);
                }
                b.endIfThen();
            }
            case SqueakBytecodeIR.RETURN_RECEIVER, SqueakBytecodeIR.RETURN_CONSTANT, SqueakBytecodeIR.RETURN_TOP -> {
                final Runnable value;
                if (insn.kind == SqueakBytecodeIR.RETURN_TOP) {
                    flush(sp - 1);
                    value = take(sp - 1);
                } else {
                    flushAll();
                    value = insn.kind == SqueakBytecodeIR.RETURN_RECEIVER ? () -> b.emitPushReceiver() : () -> b.emitLoadConstant(insn.constant);
                }
                if (isBlock) {
                    b.beginNonLocalReturnFromBlock(pcAndSp(insn.nextPC, sp + insn.stackDelta()));
                    value.run();
                    b.endNonLocalReturnFromBlock();
                } else {
                    b.beginNormalReturn();
                    value.run();
                    b.endNormalReturn();
                }
            }
            case SqueakBytecodeIR.BLOCK_RETURN_NIL -> {
                flushAll();
                b.beginNormalReturn();
                b.emitLoadConstant(NilObject.SINGLETON);
                b.endNormalReturn();
            }
            case SqueakBytecodeIR.BLOCK_RETURN_TOP -> {
                flush(sp - 1);
                final Runnable value = take(sp - 1);
                b.beginNormalReturn();
                value.run();
                b.endNormalReturn();
            }
            case SqueakBytecodeIR.CALL_PRIMITIVE -> {
                /* The primitive has already failed. Store the error code if requested. */
                final Instruction next = instructionAt(insn.nextPC);
                if (next != null && next.kind == SqueakBytecodeIR.STORE_TEMPORARY && next.b == 0 && sp > 0) {
                    flushAll();
                    b.beginStoreLocal(stack[sp - 1]);
                    b.emitPushErrorObject();
                    b.endStoreLocal();
                }
            }
            default -> throw new UnsupportedBytecodeException("Unexpected instruction: " + insn);
        }
    }

    /**
     * Prepares the value on top of the stack for a store statement: all other deferred values are
     * spilled first. If the value stays on the stack, it is spilled as well and loaded from its
     * stack slot.
     */
    private Runnable takeForStore(final int sp, final boolean pop) {
        if (pop) {
            flush(sp - 1);
            return take(sp - 1);
        } else {
            flush(sp);
            return () -> b.emitLoadLocal(stack[sp - 1]);
        }
    }

    private void emitSend(final NativeObject selector, final int numArgs, final int sp, final int nextPC) {
        final int receiverSlot = sp - numArgs - 1;
        final int pcAndSp = pcAndSp(nextPC, receiverSlot);
        /* The frame can be observed during the send: spill everything below its operands. */
        flush(receiverSlot);
        final int receiverSource = numArgs <= 2 ? deferredSource[receiverSlot] : 0;
        final int argumentSource = numArgs == 1 && receiverSource != 0 ? deferredSource[receiverSlot + 1] : 0;
        final Runnable[] operands = take(receiverSlot, numArgs + 1);
        push(receiverSlot, () -> {
            switch (numArgs) {
                case 0 -> {
                    if (receiverSource != 0) {
                        b.emitSend0From(selector, pcAndSp, receiverSource);
                    } else {
                        b.beginSend0(selector, pcAndSp);
                        emitAll(operands);
                        b.endSend0();
                    }
                }
                case 1 -> {
                    if (argumentSource != 0) {
                        b.emitSend1From2(selector, pcAndSp, receiverSource, argumentSource);
                    } else if (receiverSource != 0) {
                        b.beginSend1From(selector, pcAndSp, receiverSource);
                        operands[1].run();
                        b.endSend1From();
                    } else {
                        b.beginSend1(selector, pcAndSp);
                        emitAll(operands);
                        b.endSend1();
                    }
                }
                case 2 -> {
                    if (receiverSource != 0) {
                        b.beginSend2From(selector, pcAndSp, receiverSource);
                        operands[1].run();
                        operands[2].run();
                        b.endSend2From();
                    } else {
                        b.beginSend2(selector, pcAndSp);
                        emitAll(operands);
                        b.endSend2();
                    }
                }
                default -> {
                    b.beginSendN(selector, pcAndSp);
                    emitAll(operands);
                    b.endSendN();
                }
            }
        });
        if (deferred[receiverSlot] != null && numArgs <= 1) {
            deferredTest[receiverSlot] = (expected, jumpPcAndSp) -> {
                if (numArgs == 0) {
                    if (receiverSource != 0) {
                        b.emitSend0TestFrom(selector, pcAndSp, jumpPcAndSp, expected, receiverSource);
                    } else {
                        b.beginSend0Test(selector, pcAndSp, jumpPcAndSp, expected);
                        emitAll(operands);
                        b.endSend0Test();
                    }
                } else if (argumentSource != 0) {
                    b.emitSend1TestFrom2(selector, pcAndSp, jumpPcAndSp, expected, receiverSource, argumentSource);
                } else if (receiverSource != 0) {
                    b.beginSend1TestFrom(selector, pcAndSp, jumpPcAndSp, expected, receiverSource);
                    operands[1].run();
                    b.endSend1TestFrom();
                } else {
                    b.beginSend1Test(selector, pcAndSp, jumpPcAndSp, expected);
                    emitAll(operands);
                    b.endSend1Test();
                }
            };
        }
    }

    private void emitSpecialSend(final Instruction insn, final int sp) {
        final int specialIndex = insn.a;
        final int numArgs = insn.b;
        final NativeObject selector = image.getSpecialSelector(specialIndex);
        final int receiverSlot = sp - numArgs - 1;
        final int pcAndSp = pcAndSp(insn.nextPC, receiverSlot);
        if (!isSpecialOperation(specialIndex, numArgs)) {
            emitSend(selector, numArgs, sp, insn.nextPC);
            return;
        }
        if (specialIndex != SPECIAL_IDENTICAL && specialIndex != SPECIAL_NOT_IDENTICAL && specialIndex != SPECIAL_CLASS) {
            /* May fall back to a real send. */
            flush(receiverSlot);
        }
        final Runnable[] operands = take(receiverSlot, numArgs + 1);
        push(receiverSlot, () -> {
            switch (specialIndex) {
                case SPECIAL_ADD -> {
                    b.beginPrimAdd(selector, pcAndSp);
                    emitAll(operands);
                    b.endPrimAdd();
                }
                case SPECIAL_SUBTRACT -> {
                    b.beginPrimSubtract(selector, pcAndSp);
                    emitAll(operands);
                    b.endPrimSubtract();
                }
                case SPECIAL_LESS_THAN -> {
                    b.beginPrimLessThan(selector, pcAndSp);
                    emitAll(operands);
                    b.endPrimLessThan();
                }
                case SPECIAL_GREATER_THAN -> {
                    b.beginPrimGreaterThan(selector, pcAndSp);
                    emitAll(operands);
                    b.endPrimGreaterThan();
                }
                case SPECIAL_LESS_OR_EQUAL -> {
                    b.beginPrimLessOrEqual(selector, pcAndSp);
                    emitAll(operands);
                    b.endPrimLessOrEqual();
                }
                case SPECIAL_GREATER_OR_EQUAL -> {
                    b.beginPrimGreaterOrEqual(selector, pcAndSp);
                    emitAll(operands);
                    b.endPrimGreaterOrEqual();
                }
                case SPECIAL_EQUAL -> {
                    b.beginPrimEqual(selector, pcAndSp);
                    emitAll(operands);
                    b.endPrimEqual();
                }
                case SPECIAL_NOT_EQUAL -> {
                    b.beginPrimNotEqual(selector, pcAndSp);
                    emitAll(operands);
                    b.endPrimNotEqual();
                }
                case SPECIAL_MULTIPLY -> {
                    b.beginPrimMultiply(selector, pcAndSp);
                    emitAll(operands);
                    b.endPrimMultiply();
                }
                case SPECIAL_DIV -> {
                    b.beginPrimDiv(selector, pcAndSp);
                    emitAll(operands);
                    b.endPrimDiv();
                }
                case SPECIAL_BIT_AND -> {
                    b.beginPrimBitAnd(selector, pcAndSp);
                    emitAll(operands);
                    b.endPrimBitAnd();
                }
                case SPECIAL_BIT_OR -> {
                    b.beginPrimBitOr(selector, pcAndSp);
                    emitAll(operands);
                    b.endPrimBitOr();
                }
                case SPECIAL_SIZE -> {
                    b.beginPrimSize(selector, pcAndSp);
                    emitAll(operands);
                    b.endPrimSize();
                }
                case SPECIAL_IDENTICAL -> {
                    b.beginPrimIdentical();
                    emitAll(operands);
                    b.endPrimIdentical();
                }
                case SPECIAL_NOT_IDENTICAL -> {
                    b.beginPrimNotIdentical();
                    emitAll(operands);
                    b.endPrimNotIdentical();
                }
                case SPECIAL_CLASS -> {
                    b.beginPrimClass();
                    emitAll(operands);
                    b.endPrimClass();
                }
                case SPECIAL_VALUE -> {
                    b.beginSendValue(selector, pcAndSp);
                    emitAll(operands);
                    b.endSendValue();
                }
                case SPECIAL_VALUE_WITH_ARG -> {
                    b.beginSendValueWithArg(selector, pcAndSp);
                    emitAll(operands);
                    b.endSendValueWithArg();
                }
                default -> throw new UnsupportedBytecodeException("Unexpected special selector: " + specialIndex);
            }
        });
        if (deferred[receiverSlot] != null) {
            if (specialIndex == SPECIAL_IDENTICAL || specialIndex == SPECIAL_NOT_IDENTICAL) {
                deferredTest[receiverSlot] = (expected, jumpPcAndSp) -> {
                    if ((specialIndex == SPECIAL_IDENTICAL) == expected) {
                        b.beginPrimIdentical();
                        emitAll(operands);
                        b.endPrimIdentical();
                    } else {
                        b.beginPrimNotIdentical();
                        emitAll(operands);
                        b.endPrimNotIdentical();
                    }
                };
            } else if (SPECIAL_LESS_THAN <= specialIndex && specialIndex <= SPECIAL_NOT_EQUAL) {
                deferredTest[receiverSlot] = (expected, jumpPcAndSp) -> {
                    b.beginPrimCompareTest(specialIndex, selector, pcAndSp, jumpPcAndSp, expected);
                    emitAll(operands);
                    b.endPrimCompareTest();
                };
            }
        }
    }

    /** Whether the special selector has a dedicated operation (rather than a plain send). */
    private static boolean isSpecialOperation(final int specialIndex, final int numArgs) {
        return switch (specialIndex) {
            case SPECIAL_ADD, SPECIAL_SUBTRACT, SPECIAL_LESS_THAN, SPECIAL_GREATER_THAN, SPECIAL_LESS_OR_EQUAL, SPECIAL_GREATER_OR_EQUAL, SPECIAL_EQUAL, SPECIAL_NOT_EQUAL, SPECIAL_MULTIPLY, SPECIAL_DIV, //
                SPECIAL_BIT_AND, SPECIAL_BIT_OR, SPECIAL_IDENTICAL, SPECIAL_NOT_IDENTICAL, SPECIAL_VALUE_WITH_ARG ->
                numArgs == 1;
            case SPECIAL_SIZE, SPECIAL_CLASS, SPECIAL_VALUE -> numArgs == 0;
            default -> false;
        };
    }

    /** Whether the instruction may check for interrupts itself because it performs a real send. */
    private static boolean isRealSend(final Instruction insn) {
        return switch (insn.kind) {
            case SqueakBytecodeIR.SEND, SqueakBytecodeIR.SEND_SUPER -> true;
            case SqueakBytecodeIR.SEND_SPECIAL -> !isSpecialOperation(insn.a, insn.b);
            default -> false;
        };
    }

    /** See AbstractInterpreterNode#createCheckForInterruptsInLoopNode. */
    private void emitInterruptCheck(final int jumpIndex, final int targetIndex, final int sp) {
        if (image.interruptHandlerDisabled()) {
            return;
        }
        for (int i = targetIndex; i < jumpIndex; i++) {
            if (instructions[i].isReachable() && isRealSend(instructions[i])) {
                return; // No need to check for interrupts if there is a message send in the loop.
            }
        }
        b.emitCheckForInterruptsInLoop(pcAndSp(instructions[targetIndex].pc, sp));
    }
}
