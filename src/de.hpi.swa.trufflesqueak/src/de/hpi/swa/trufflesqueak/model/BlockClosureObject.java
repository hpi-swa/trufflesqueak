/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import org.graalvm.collections.UnmodifiableEconomicMap;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.image.SqueakImageChunk;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.image.SqueakImageWriter;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.BLOCK_CLOSURE;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.ObjectGraphUtils.ObjectTracer;

public final class BlockClosureObject extends AbstractSqueakObjectWithClassAndHash {
    @CompilationFinal private ContextObject outerContext;
    @CompilationFinal private CompiledCodeObject block;
    @CompilationFinal private long startPC = -1;
    @CompilationFinal private long numArgs = -1;
    @CompilationFinal private Object receiver;
    @CompilationFinal(dimensions = 0) private Object[] copiedValues;

    private BlockClosureObject(final ClassObject squeakClass) {
        super(squeakClass);
    }

    private BlockClosureObject(final long header, final ClassObject squeakClass) {
        super(header, squeakClass);
        copiedValues = ArrayUtils.EMPTY_ARRAY; // Ensure copied is set.
    }

    public BlockClosureObject(final ClassObject squeakClass, final CompiledCodeObject block, final int startPC, final int numArgs, final Object[] copied,
                    final Object receiver, final ContextObject outerContext) {
        super(squeakClass);
        assert startPC > 0;
        this.block = block;
        this.outerContext = outerContext;
        copiedValues = copied;
        this.startPC = startPC;
        this.numArgs = numArgs;
        this.receiver = receiver;
    }

    private BlockClosureObject(final BlockClosureObject original) {
        super(original);
        block = original.block;
        outerContext = original.outerContext;
        copiedValues = original.copiedValues;
        startPC = original.startPC;
        numArgs = original.numArgs;
        receiver = original.receiver;
    }

    public static BlockClosureObject createWithHeaderAndClass(final long header, final ClassObject squeakClass) {
        return new BlockClosureObject(header, squeakClass);
    }

    public static BlockClosureObject create(final ClassObject squeakClass, final int extraSize) {
        final BlockClosureObject result = new BlockClosureObject(squeakClass);
        result.copiedValues = new Object[extraSize];
        return result;
    }

    @Override
    public void fillin(final SqueakImageChunk chunk) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        assert chunk.getWordSize() >= BLOCK_CLOSURE.FIRST_COPIED_VALUE;
        outerContext = (ContextObject) chunk.getPointer(BLOCK_CLOSURE.OUTER_CONTEXT);
        final Object startPCOrMethod = chunk.getPointer(BLOCK_CLOSURE.START_PC_OR_METHOD);
        if (startPCOrMethod instanceof final CompiledCodeObject code) {
            block = code;
            receiver = chunk.getPointer(BLOCK_CLOSURE.FULL_RECEIVER);
            copiedValues = chunk.getPointers(BLOCK_CLOSURE.FULL_FIRST_COPIED_VALUE);
        } else {
            startPC = (long) startPCOrMethod;
            copiedValues = chunk.getPointers(BLOCK_CLOSURE.FIRST_COPIED_VALUE);
        }
        numArgs = (long) chunk.getPointer(BLOCK_CLOSURE.ARGUMENT_COUNT);
    }

    public AbstractSqueakObject getOuterContext() {
        return NilObject.nullToNil(outerContext);
    }

    public ContextObject getOuterContextOrNull() {
        return outerContext;
    }

    public long getStartPC() {
        if (startPC == -1) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            startPC = block.getInitialPC();
        }
        return startPC;
    }

    public long getNumArgs() {
        if (numArgs == -1) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            numArgs = block.getNumArgs();
        }
        return numArgs;
    }

    public Object getCopiedValue(final int index) {
        return copiedValues[index];
    }

    public Object[] getCopiedValues() {
        return copiedValues;
    }

    public int getNumCopied() {
        return copiedValues.length;
    }

    public Object getReceiver() {
        if (receiver == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            receiver = getOuterContextOrNull().getReceiver();
        }
        return receiver;
    }

    public void setReceiver(final Object value) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        receiver = value;
    }

    public void setOuterContext(final ContextObject outerContext) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        this.outerContext = outerContext;
    }

    public void removeOuterContext() {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        outerContext = null;
    }

    public void setStartPC(final int pc) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        startPC = pc;
    }

    public void setBlock(final CompiledCodeObject value) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        block = value;
    }

    public void setNumArgs(final int numArgs) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        this.numArgs = numArgs;
    }

    public void setCopiedValue(final int index, final Object value) {
        copiedValues[index] = value;
    }

    public void setCopiedValues(final Object[] copied) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        copiedValues = copied;
    }

    public void become(final BlockClosureObject other) {
        final Object[] otherCopied = other.copiedValues;
        other.setCopiedValues(copiedValues);
        setCopiedValues(otherCopied);
    }

    @Override
    public int instsize() {
        return isAFullBlockClosure() ? BLOCK_CLOSURE.FULL_FIRST_COPIED_VALUE : BLOCK_CLOSURE.FIRST_COPIED_VALUE;
    }

    @Override
    public int size() {
        return instsize() + copiedValues.length;
    }

    public boolean isABlockClosure(final SqueakImageContext image) {
        return image.isBlockClosureClass(getSqueakClass());
    }

    public boolean isAFullBlockClosure(final SqueakImageContext image) {
        return image.isFullBlockClosureClass(getSqueakClass());
    }

    public boolean isAFullBlockClosure() {
        return isAFullBlockClosure(getSqueakClass().getImage());
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "a " + getSqueakClassName() + " @" + Integer.toHexString(hashCode()) + " (with " + (numArgs == -1 && block == null ? "no block" : getNumArgs() + " args") + " and " +
                        copiedValues.length + " copied values in " + outerContext + ")";
    }

    public CompiledCodeObject getCompiledBlock() {
        if (block == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            /* `outerContext.getMethod()` should not be part of compilation. */
            block = outerContext.getCodeObject().getOrCreateShadowBlock((int) getStartPC());
        }
        return block;
    }

    /** Special version of getCompiledBlock for image loader. */
    public CompiledCodeObject getCompiledBlock(final CompiledCodeObject method) {
        if (block == null) {
            block = method.getOrCreateShadowBlock((int) getStartPC());
        }
        return block;
    }

    public ContextObject getHomeContext() {
        ContextObject currentContext = outerContext;
        while (currentContext.hasClosure()) {
            currentContext = currentContext.getClosure().getOuterContextOrNull();
        }
        return currentContext;
    }

    public int getNumTemps() {
        if (block.isCompiledMethod()) { // See BlockClosure>>#numTemps
            return (int) (getNumCopied() + getNumArgs() + tempCountForBlock());
        } else { // FullBlockClosure>>#numTemps
            return block.getNumTemps();
        }
    }

    private int tempCountForBlock() {
        assert block.isCompiledMethod();
        CompilerAsserts.neverPartOfCompilation();
        final byte[] bytecode = block.getBytes();
        int pc;
        final int pushNilCode;
        final int blockSize;
        if (block.getSignFlag()) {
            pc = (int) getStartPC() - 4 - block.getInitialPC();
            pushNilCode = 115;
        } else {
            pc = (int) getStartPC() - 3 - block.getInitialPC();
            pushNilCode = 79;
        }
        blockSize = Byte.toUnsignedInt(bytecode[pc + 2]) << 8 | Byte.toUnsignedInt(bytecode[pc + 3]);
        final int blockEnd = pc + blockSize;

        int stackpointer = 0;
        if (Byte.toUnsignedInt(bytecode[pc]) == pushNilCode) {
            while (pc < blockEnd) {
                final int b = Byte.toUnsignedInt(bytecode[pc++]);
                if (b == pushNilCode) {
                    stackpointer++;
                } else {
                    break;
                }
            }
        }
        return stackpointer;
    }

    public BlockClosureObject shallowCopy() {
        return new BlockClosureObject(this);
    }

    @Override
    public void pointersBecomeOneWay(final UnmodifiableEconomicMap<Object, Object> fromToMap) {
        super.pointersBecomeOneWay(fromToMap);
        if (receiver != null) {
            final Object toReceiver = fromToMap.get(receiver);
            if (toReceiver != null) {
                receiver = toReceiver;
            }
        }
        if (block != null && fromToMap.get(block) instanceof final CompiledCodeObject b) {
            block = b;
        }
        if (outerContext != null && fromToMap.get(outerContext) instanceof final ContextObject c && c != outerContext) {
            setOuterContext(c);
        }
        ArrayUtils.replaceAll(copiedValues, fromToMap);
    }

    @Override
    public void tracePointers(final ObjectTracer tracer) {
        super.tracePointers(tracer);
        tracer.addIfUnmarked(receiver);
        tracer.addIfUnmarked(block);
        tracer.addIfUnmarked(outerContext);
        tracer.addAllIfUnmarked(copiedValues);
    }

    @Override
    public void trace(final SqueakImageWriter writer) {
        super.trace(writer);
        writer.traceIfNecessary(outerContext);
        writer.traceAllIfNecessary(copiedValues);
    }

    @Override
    public void write(final SqueakImageWriter writer) {
        if (!writeHeader(writer)) {
            throw SqueakException.create("BlockClosureObject must have slots:", this);
        }
        writer.writeObject(getOuterContext());
        if (isAFullBlockClosure()) {
            assert block != null || receiver != null;
            writer.writeObject(block);
            writer.writeSmallInteger(getNumArgs());
            writer.writeObject(receiver);
        } else {
            writer.writeSmallInteger(getStartPC());
            writer.writeSmallInteger(getNumArgs());
        }
        writer.writeObjects(getCopiedValues());
    }
}
