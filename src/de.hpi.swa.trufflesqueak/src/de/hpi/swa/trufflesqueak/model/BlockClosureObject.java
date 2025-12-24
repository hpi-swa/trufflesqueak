/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import org.graalvm.collections.UnmodifiableEconomicMap;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.image.SqueakImageChunk;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.image.SqueakImageWriter;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.BLOCK_CLOSURE;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.ObjectGraphUtils.ObjectTracer;

public final class BlockClosureObject extends AbstractSqueakObjectWithHash {
    @CompilationFinal private ContextObject outerContext;
    @CompilationFinal private CompiledCodeObject block;
    @CompilationFinal private int numArgs = -1;
    @CompilationFinal private Object receiver;
    @CompilationFinal(dimensions = 0) private Object[] copiedValues;

    public BlockClosureObject(final SqueakImageChunk chunk) {
        super(chunk);
        assert chunk.getWordSize() >= BLOCK_CLOSURE.FIRST_COPIED_VALUE;
        if (chunk.getSqueakClass().isBlockClosureClass()) {
            setIsABlockClosure();
        }
        outerContext = (ContextObject) chunk.getPointer(BLOCK_CLOSURE.OUTER_CONTEXT);
        final Object startPCOrMethod = chunk.getPointer(BLOCK_CLOSURE.START_PC_OR_METHOD);
        numArgs = (int) (long) chunk.getPointer(BLOCK_CLOSURE.ARGUMENT_COUNT);
        if (startPCOrMethod instanceof final CompiledCodeObject code) {
            block = code;
            receiver = chunk.getPointer(BLOCK_CLOSURE.FULL_RECEIVER);
            copiedValues = chunk.getPointers(BLOCK_CLOSURE.FULL_FIRST_COPIED_VALUE);
        } else {
            receiver = chunk.getChunk(BLOCK_CLOSURE.OUTER_CONTEXT).getPointer(CONTEXT.RECEIVER);
            copiedValues = chunk.getPointers(BLOCK_CLOSURE.FIRST_COPIED_VALUE);
        }
    }

    public BlockClosureObject(final boolean hasBlockClosureClass, final int extraSize) {
        super();
        if (hasBlockClosureClass) {
            setIsABlockClosure();
        }
        copiedValues = new Object[extraSize];
    }

    public BlockClosureObject(final boolean hasBlockClosureClass, final CompiledCodeObject block, final int numArgs, final Object[] copied, final Object receiver, final ContextObject outerContext) {
        super();
        if (hasBlockClosureClass) {
            setIsABlockClosure();
        }
        this.block = block;
        this.outerContext = outerContext;
        copiedValues = copied;
        this.numArgs = numArgs;
        this.receiver = receiver;
    }

    public BlockClosureObject(final BlockClosureObject original) {
        super(original);
        block = original.block;
        outerContext = original.outerContext;
        copiedValues = original.copiedValues;
        numArgs = original.numArgs;
        receiver = original.receiver;
    }

    @Override
    public void fillin(final SqueakImageChunk chunk) {
        assert chunk.getHash() == getSqueakHashInt();
        if (block == null && chunk.getPointer(BLOCK_CLOSURE.START_PC_OR_METHOD) instanceof final Long startPC) {
            block = outerContext.getMethodFromChunk().createShadowBlock(startPC.intValue());
        }
    }

    @Override
    protected AbstractSqueakObjectWithHash getForwardingPointer() {
        return this;
    }

    @Override
    public AbstractSqueakObjectWithHash resolveForwardingPointer() {
        return this;
    }

    @Override
    public ClassObject getSqueakClass() {
        return getSqueakClass(SqueakImageContext.getSlow());
    }

    @Override
    public ClassObject getSqueakClass(final SqueakImageContext image) {
        return isABlockClosure() ? image.blockClosureClass : image.getFullBlockClosureClass();
    }

    public AbstractSqueakObject getOuterContext() {
        return NilObject.nullToNil(outerContext);
    }

    public ContextObject getOuterContextOrNull() {
        return outerContext;
    }

    public CompiledCodeObject getCompiledBlock() {
        assert block != null;
        return block;
    }

    public long getStartPC() {
        return block.getOuterMethodStartPC();
    }

    public int getNumArgs() {
        assert numArgs != -1;
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
        assert receiver != null;
        return receiver;
    }

    public void setReceiver(final Object value) {
        receiver = value;
    }

    public void setOuterContext(final ContextObject outerContext) {
        this.outerContext = outerContext;
    }

    public void removeOuterContext() {
        outerContext = null;
    }

    @TruffleBoundary
    public void setStartPC(final int pc) {
        if (block == null) {
            block = outerContext.getCodeObject().createShadowBlock(pc);
        } else {
            block.setOuterMethodStartPC(pc);
        }
    }

    public void setBlock(final CompiledCodeObject value) {
        block = value;
    }

    public void setNumArgs(final int numArgs) {
        this.numArgs = numArgs;
    }

    public void setCopiedValue(final int index, final Object value) {
        copiedValues[index] = value;
    }

    public void setCopiedValues(final Object[] copied) {
        copiedValues = copied;
    }

    public void become(final BlockClosureObject other) {
        final ContextObject otherOuterContext = other.outerContext;
        final CompiledCodeObject otherBlock = other.block;
        final int otherNumArgs = other.numArgs;
        final Object otherReceiver = other.receiver;
        final Object[] otherCopied = other.copiedValues;

        other.setOuterContext(outerContext);
        other.setBlock(block);
        other.setNumArgs(numArgs);
        other.setReceiver(receiver);
        other.setCopiedValues(copiedValues);

        setOuterContext(otherOuterContext);
        setBlock(otherBlock);
        setNumArgs(otherNumArgs);
        setReceiver(otherReceiver);
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

    private void setIsABlockClosure() {
        setBooleanABit();
    }

    public boolean isABlockClosure() {
        return isBooleanASet();
    }

    public boolean isAFullBlockClosure() {
        return !isABlockClosure();
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "a " + getSqueakClass().getClassName() + " @" + Integer.toHexString(hashCode()) + " (with " + (numArgs == -1 && block == null ? "no block" : getNumArgs() + " args") + " and " +
                        copiedValues.length + " copied values in " + outerContext + ")";
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
            return getNumCopied() + getNumArgs() + tempCountForBlock();
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
            pc = block.getOuterMethodStartPCZeroBased() - 4;
            pushNilCode = 115;
        } else {
            pc = block.getOuterMethodStartPCZeroBased() - 3;
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

    @Override
    public void pointersBecomeOneWay(final UnmodifiableEconomicMap<Object, Object> fromToMap) {
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
