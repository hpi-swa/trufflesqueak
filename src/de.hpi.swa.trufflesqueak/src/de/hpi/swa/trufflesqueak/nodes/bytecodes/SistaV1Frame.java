/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import java.util.Map;

import com.oracle.truffle.api.bytecode.BytecodeNode;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.source.SourceSection;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;

/** Public-API view of a DSL frame for Squeak context inspection and mutation. */
public final class SistaV1Frame {
    public record Location(int pc, int sp) {
    }

    public static final class ContextState {
        public ContextObject context;
        // Overrides come only from explicit Smalltalk context mutations, never execution.
        public int modifiedPC = Integer.MIN_VALUE;
        public int modifiedSP = Integer.MIN_VALUE;
        public boolean dead;
    }

    @CompilationFinal private SmalltalkSistaV1Interpreter root;
    @CompilationFinal private CompiledCodeObject code;
    @CompilationFinal(dimensions = 1) private int[] locals;
    @CompilationFinal private Map<Integer, Location> locations;
    @CompilationFinal private int contextSlot;
    @CompilationFinal private Location entry;

    void initialize(final SmalltalkSistaV1Interpreter interpreter, final CompiledCodeObject method, final int[] localOffsets, final Map<Integer, Location> sourceLocations,
                    final Location entryLocation, final int contextLocal) {
        root = interpreter;
        code = method.isShadowBlock() ? method.getMethod() : method;
        locals = localOffsets;
        locations = sourceLocations;
        entry = entryLocation;
        contextSlot = contextLocal;
    }

    public CompiledCodeObject code() {
        return code;
    }

    public ContextState existingState(final Frame frame) {
        final Object value = root.getBytecodeNode().getLocalValue(0, frame, contextSlot);
        return value instanceof final ContextState state ? state : null;
    }

    public ContextState state(final Frame frame) {
        ContextState state = existingState(frame);
        if (state == null) {
            state = new ContextState();
            root.getBytecodeNode().setLocalValue(0, frame, contextSlot, state);
        }
        return state;
    }

    private int bytecodeIndex(final Frame frame) {
        final int bci = root.getBytecodeNode().getBytecodeIndex(frame);
        return Math.max(0, bci);
    }

    private Location location(final Frame frame) {
        final SourceSection source = root.getBytecodeNode().getSourceLocation(bytecodeIndex(frame));
        final Location location = source == null ? null : locations.get(source.getCharIndex());
        return location == null ? entry : location;
    }

    @TruffleBoundary
    public int instructionPointer(final MaterializedFrame frame) {
        final ContextState state = existingState(frame);
        if (state == null) {
            return location(frame).pc();
        }
        if (state.dead) {
            return state.modifiedPC == ContextObject.NIL_PC_STACK_NIL_VALUE ? state.modifiedPC : ContextObject.NIL_PC_STACK_NOT_NIL_VALUE;
        }
        return state.modifiedPC == Integer.MIN_VALUE ? location(frame).pc() : state.modifiedPC;
    }

    @TruffleBoundary
    public int stackPointer(final MaterializedFrame frame) {
        final ContextState state = existingState(frame);
        final int modified = state == null ? Integer.MIN_VALUE : state.modifiedSP;
        return modified == Integer.MIN_VALUE ? location(frame).sp() : modified;
    }

    public int stackSize() {
        return locals.length;
    }

    @TruffleBoundary
    public Object read(final MaterializedFrame frame, final int index) {
        return root.getBytecodeNode().getLocalValue(bytecodeIndex(frame), frame, locals[index]);
    }

    @TruffleBoundary
    public void write(final MaterializedFrame frame, final int index, final Object value) {
        root.getBytecodeNode().setLocalValue(bytecodeIndex(frame), frame, locals[index], value);
    }

    @TruffleBoundary
    public void copy(final MaterializedFrame source, final MaterializedFrame destination) {
        final BytecodeNode bytecode = root.getBytecodeNode();
        for (int i = 0; i < locals.length; i++) {
            bytecode.setLocalValue(0, destination, locals[i], read(source, i));
        }
        final ContextState state = state(destination);
        state.modifiedPC = instructionPointer(source);
        state.modifiedSP = stackPointer(source);
        state.dead = state(source).dead;
    }
}
