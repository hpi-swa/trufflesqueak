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
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.dsl.SqueakBytecodeIR.Instruction;
import de.hpi.swa.trufflesqueak.util.FrameAccess.DSLFrameInfo;

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
        /* Per parse: */
        private BytecodeLabel continueLabel;

        private Loop(final int headIndex, final int endIndex) {
            this.headIndex = headIndex;
            this.endIndex = endIndex;
        }
    }

    private record DispatchEntry(int pc, BytecodeLabel label, boolean isLeaf) {
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

    private final SqueakImageContext image;
    private final CompiledCodeObject code;
    private final boolean isResumption;
    private final boolean isBlock;

    /* Results of the analysis, independent of a builder (the parser may run multiple times). */
    private final Instruction[] instructions;
    private final int startPC;
    private final int[] pcToIndex;
    private final Loop[] loops; // sorted by head index
    private final Loop[] loopOf; // innermost loop per instruction
    private final Loop[] loopStartingAt;
    private final boolean[] needsLabel;
    private final boolean modifiesContext;
    private final int numStackSlots;
    private final HashMap<Integer, CompiledCodeObject> shadowBlocks = new HashMap<>();

    /* Per parse: */
    private SqueakBytecodeRootNodeGen.Builder b;
    private BytecodeLocal[] stack;
    private BytecodeLocal resumePendingLocal;
    private BytecodeLabel[] labels;
    private Runnable[] deferred;

    private SqueakBytecodeTranslator(final SqueakImageContext image, final CompiledCodeObject code, final boolean isResumption) {
        this.image = image;
        this.code = code;
        this.isResumption = isResumption;
        isBlock = code.isCompiledBlock() || code.isShadowBlock();
        instructions = SqueakBytecodeIR.decode(code);
        startPC = code.getStartPCZeroBased();
        numStackSlots = code.getMaxNumStackSlots();
        final int numInstructions = instructions.length;

        pcToIndex = new int[code.getMaxPCZeroBased() - startPC + 1];
        java.util.Arrays.fill(pcToIndex, -1);
        for (int i = 0; i < numInstructions; i++) {
            pcToIndex[instructions[i].pc - startPC] = i;
        }

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
        java.util.Arrays.sort(loops, (l1, l2) -> l1.headIndex != l2.headIndex ? Integer.compare(l1.headIndex, l2.headIndex) : Integer.compare(l2.endIndex, l1.endIndex));
        loopOf = new Loop[numInstructions];
        loopStartingAt = new Loop[numInstructions];
        final ArrayList<Loop> open = new ArrayList<>();
        for (final Loop loop : loops) {
            while (!open.isEmpty() && open.getLast().endIndex < loop.headIndex) {
                open.removeLast();
            }
            if (!open.isEmpty()) {
                final Loop enclosing = open.getLast();
                if (enclosing.endIndex < loop.endIndex) {
                    throw new UnsupportedBytecodeException("Overlapping loops at pc " + instructions[loop.headIndex].pc);
                }
                loop.parent = enclosing;
            }
            open.add(loop);
            loopStartingAt[loop.headIndex] = loop;
            /* Loops are sorted from outer to inner, so inner loops overwrite outer ones. */
            for (int i = loop.headIndex; i <= loop.endIndex; i++) {
                loopOf[i] = loop;
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
                } else {
                    final Loop owner = labelOwner(targetIndex);
                    if (!isSelfOrAncestor(owner, loopOf[i])) {
                        throw new UnsupportedBytecodeException("Forward jump into a loop at pc " + insn.pc);
                    }
                    needsLabel[targetIndex] = true;
                }
            }
        }
        modifiesContext = couldMutateContextVariables(hasPotentialContextIVStore);
    }

    /**
     * Creates the root node for the given method or block.
     *
     * @throws UnsupportedBytecodeException if the bytecode cannot be translated
     */
    public static SqueakBytecodeRootNode createRoot(final SqueakLanguage language, final SqueakImageContext image, final CompiledCodeObject code, final boolean isResumption) {
        final SqueakBytecodeTranslator translator = new SqueakBytecodeTranslator(image, code, isResumption);
        final SqueakBytecodeRootNode root = SqueakBytecodeRootNodeGen.create(language, BytecodeConfig.DEFAULT, translator).getNode(0);
        root.initialize(code, translator.modifiesContext);
        return root;
    }

    private int indexOfPC(final int pc) {
        final int offset = pc - startPC;
        return offset < 0 || offset >= pcToIndex.length ? -1 : pcToIndex[offset];
    }

    private static Loop findLoopWithHead(final Loop start, final int headIndex) {
        Loop loop = start;
        while (loop != null && loop.headIndex != headIndex) {
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
        return loop != null && loop.headIndex == index ? loop.parent : loop;
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
        createLabels(null, 0, instructions.length - 1);
        if (isResumption) {
            emitDispatch(null, 0, instructions.length - 1, null);
        }
        for (int i = 0; i < instructions.length; i++) {
            final Instruction insn = instructions[i];
            if (!insn.isReachable()) {
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
                b.emitLoadConstant(true);
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
            emitInstruction(i, insn);
            Loop ending = loopOf[i];
            while (ending != null && ending.endIndex == i) {
                if (ending.needsContinueLabel) {
                    b.emitLabel(ending.continueLabel);
                }
                b.endBlock();
                b.endWhile();
                ending = ending.parent;
            }
        }
        assert assertNoDeferredValues();
        b.endBlock();
        b.endRoot();
        b = null;
        stack = null;
        labels = null;
        deferred = null;
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
        if (isResumption) {
            b.beginStoreLocal(stack[slot]);
            expression.run();
            b.endStoreLocal();
        } else {
            assert deferred[slot] == null;
            deferred[slot] = expression;
        }
    }

    /** Removes the value in the given slot from the abstract stack and returns its expression. */
    private Runnable take(final int slot) {
        final Runnable expression = deferred[slot];
        if (expression != null) {
            deferred[slot] = null;
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

    private void emitInstruction(final int index, final Instruction insn) {
        final int sp = insn.spBefore;
        switch (insn.kind) {
            case SqueakBytecodeIR.PUSH_RECEIVER_VARIABLE -> push(sp, () -> b.emitPushReceiverVariable(insn.a));
            case SqueakBytecodeIR.PUSH_LITERAL_VARIABLE -> push(sp, () -> b.emitPushLiteralVariable(insn.a));
            case SqueakBytecodeIR.PUSH_LITERAL_CONSTANT -> {
                final Object literal = code.getLiteral(insn.a);
                if (literal instanceof Long || literal instanceof Double || literal instanceof Boolean || literal == NilObject.SINGLETON) {
                    push(sp, () -> b.emitLoadConstant(literal));
                } else {
                    push(sp, () -> b.emitPushLiteralConstant(insn.a));
                }
            }
            case SqueakBytecodeIR.PUSH_TEMPORARY -> push(sp, () -> {
                b.beginFollow();
                b.emitLoadLocal(stack[insn.a]);
                b.endFollow();
            });
            case SqueakBytecodeIR.PUSH_RECEIVER -> push(sp, () -> b.emitPushReceiver());
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
                b.emitLoadLocal(stack[insn.b]);
                b.endPushRemoteTemp();
            });
            case SqueakBytecodeIR.STORE_REMOTE_TEMP -> {
                final Runnable value = takeForStore(sp, insn.c != 0);
                b.beginStoreRemoteTemp(insn.a);
                b.emitLoadLocal(stack[insn.b]);
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
                final Runnable condition = take(sp - 1);
                b.beginIfThen();
                if (insn.kind == SqueakBytecodeIR.JUMP_TRUE) {
                    b.beginIsTrue(pcAndSp);
                    condition.run();
                    b.endIsTrue();
                } else {
                    b.beginIsFalse(pcAndSp);
                    condition.run();
                    b.endIsFalse();
                }
                b.emitBranch(labels[indexOfPC(insn.target)]);
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
                b.beginReturn();
                if (isBlock) {
                    b.beginNonLocalReturnFromBlock(pcAndSp(insn.nextPC, sp + insn.stackDelta()));
                    value.run();
                    b.endNonLocalReturnFromBlock();
                } else {
                    b.beginNormalReturn();
                    value.run();
                    b.endNormalReturn();
                }
                b.endReturn();
            }
            case SqueakBytecodeIR.BLOCK_RETURN_NIL -> {
                flushAll();
                b.beginReturn();
                b.beginNormalReturn();
                b.emitLoadConstant(NilObject.SINGLETON);
                b.endNormalReturn();
                b.endReturn();
            }
            case SqueakBytecodeIR.BLOCK_RETURN_TOP -> {
                flush(sp - 1);
                final Runnable value = take(sp - 1);
                b.beginReturn();
                b.beginNormalReturn();
                value.run();
                b.endNormalReturn();
                b.endReturn();
            }
            case SqueakBytecodeIR.CALL_PRIMITIVE -> {
                /* The primitive has already failed. Store the error code if requested. */
                if (index + 1 < instructions.length && instructions[index + 1].kind == SqueakBytecodeIR.STORE_TEMPORARY && instructions[index + 1].b == 0) {
                    assert sp > 0;
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
        final Runnable[] operands = take(receiverSlot, numArgs + 1);
        push(receiverSlot, () -> {
            switch (numArgs) {
                case 0 -> {
                    b.beginSend0(selector, pcAndSp);
                    emitAll(operands);
                    b.endSend0();
                }
                case 1 -> {
                    b.beginSend1(selector, pcAndSp);
                    emitAll(operands);
                    b.endSend1();
                }
                case 2 -> {
                    b.beginSend2(selector, pcAndSp);
                    emitAll(operands);
                    b.endSend2();
                }
                default -> {
                    b.beginSendN(selector, pcAndSp);
                    emitAll(operands);
                    b.endSendN();
                }
            }
        });
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
