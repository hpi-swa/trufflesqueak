/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.image.SqueakImageChunk;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.image.SqueakImageWriter;
import de.hpi.swa.trufflesqueak.interop.WrapToSqueakNode;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.BLOCK_CLOSURE;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushBytecodes.PushClosureNode;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.ObjectGraphUtils.ObjectTracer;

@ExportLibrary(InteropLibrary.class)
public final class BlockClosureObject extends AbstractSqueakObjectWithClassAndHash {
    @CompilationFinal private ContextObject outerContext;
    @CompilationFinal private CompiledCodeObject block;
    @CompilationFinal private long startPC = -1;
    @CompilationFinal private long numArgs = -1;
    @CompilationFinal private Object receiver = null;
    @CompilationFinal(dimensions = 0) private Object[] copiedValues;

    private BlockClosureObject(final SqueakImageContext image, final ClassObject squeakClass) {
        super(image, squeakClass);
    }

    private BlockClosureObject(final SqueakImageContext image, final long hash, final ClassObject squeakClass) {
        super(image, hash, squeakClass);
        copiedValues = ArrayUtils.EMPTY_ARRAY; // Ensure copied is set.
    }

    public BlockClosureObject(final SqueakImageContext image, final ClassObject squeakClass, final CompiledCodeObject block, final int startPC, final int numArgs, final Object[] copied,
                    final Object receiver, final ContextObject outerContext) {
        super(image, squeakClass);
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

    public static BlockClosureObject createWithHash(final SqueakImageContext image, final int hash, final ClassObject squeakClass) {
        return new BlockClosureObject(image, hash, squeakClass);
    }

    public static BlockClosureObject create(final SqueakImageContext image, final ClassObject squeakClass, final int extraSize) {
        final BlockClosureObject result = new BlockClosureObject(image, squeakClass);
        result.copiedValues = new Object[extraSize];
        return result;
    }

    @Override
    public void fillin(final SqueakImageChunk chunk) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        final Object[] pointers = chunk.getPointers();
        assert pointers.length >= BLOCK_CLOSURE.FIRST_COPIED_VALUE;
        outerContext = (ContextObject) pointers[BLOCK_CLOSURE.OUTER_CONTEXT];
        if (pointers[BLOCK_CLOSURE.START_PC_OR_METHOD] instanceof CompiledCodeObject) {
            block = (CompiledCodeObject) pointers[BLOCK_CLOSURE.START_PC_OR_METHOD];
            receiver = pointers[BLOCK_CLOSURE.FULL_RECEIVER];
            copiedValues = Arrays.copyOfRange(pointers, BLOCK_CLOSURE.FULL_FIRST_COPIED_VALUE, pointers.length);
        } else {
            startPC = (long) pointers[BLOCK_CLOSURE.START_PC_OR_METHOD];
            copiedValues = Arrays.copyOfRange(pointers, BLOCK_CLOSURE.FIRST_COPIED_VALUE, pointers.length);
        }
        numArgs = (long) pointers[BLOCK_CLOSURE.ARGUMENT_COUNT];
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
        return isABlockClosure() ? BLOCK_CLOSURE.FIRST_COPIED_VALUE : BLOCK_CLOSURE.FULL_FIRST_COPIED_VALUE;
    }

    @Override
    public int size() {
        return instsize() + copiedValues.length;
    }

    public boolean isABlockClosure() {
        return getSqueakClass().isBlockClosureClass();
    }

    public boolean isAFullBlockClosure() {
        return getSqueakClass().isFullBlockClosureClass();
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
        // Recursively unpack closures until home context is reached.
        final BlockClosureObject closure = outerContext.getClosure();
        if (closure != null) {
            return closure.getHomeContextWithBoundary();
        } else {
            return outerContext;
        }
    }

    @TruffleBoundary
    private ContextObject getHomeContextWithBoundary() {
        final BlockClosureObject closure = outerContext.getClosure();
        if (closure != null) {
            return closure.getHomeContextWithBoundary();
        } else {
            return outerContext;
        }
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
            blockSize = Byte.toUnsignedInt(bytecode[pc + 2]) << 8 | Byte.toUnsignedInt(bytecode[pc + 3]);
        } else {
            pc = (int) getStartPC() - 3 - block.getInitialPC();
            pushNilCode = 79;
            final PushClosureNode pushNode = (PushClosureNode) block.bytecodeNodeAt(pc);
            blockSize = pushNode.getBlockSize();
        }
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
    public void pointersBecomeOneWay(final Object[] from, final Object[] to) {
        for (int i = 0; i < from.length; i++) {
            final Object fromPointer = from[i];
            if (receiver == fromPointer) {
                receiver = to[i];
            }
            if (block == fromPointer && to[i] instanceof CompiledCodeObject) {
                block = (CompiledCodeObject) to[i];
            }
            if (outerContext == fromPointer && fromPointer != to[i] && to[i] instanceof ContextObject) {
                setOuterContext((ContextObject) to[i]);
            }
            for (int j = 0; j < copiedValues.length; j++) {
                final Object copiedValue = copiedValues[j];
                if (copiedValue == fromPointer) {
                    copiedValues[j] = to[i];
                }
            }
        }
    }

    @Override
    public void tracePointers(final ObjectTracer tracer) {
        tracer.addIfUnmarked(getOuterContext());
        for (final Object value : getCopiedValues()) {
            tracer.addIfUnmarked(value);
        }
    }

    @Override
    public void trace(final SqueakImageWriter writer) {
        super.trace(writer);
        writer.traceIfNecessary(outerContext);
        writer.traceAllIfNecessary(getCopiedValues());
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

    /*
     * INTEROPERABILITY
     */

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean isExecutable() {
        return true;
    }

    @ExportMessage
    public Object execute(final Object[] arguments,
                    @Exclusive @Cached final WrapToSqueakNode wrapNode) throws ArityException {
        if (getNumArgs() == arguments.length) {
            final Object[] frameArguments = FrameAccess.newClosureArgumentsTemplate(this, NilObject.SINGLETON, arguments.length);
            for (int i = 0; i < arguments.length; i++) {
                frameArguments[FrameAccess.getArgumentStartIndex() + i] = wrapNode.executeWrap(arguments[i]);
            }
            return getCompiledBlock().getCallTarget().call(frameArguments);
        } else {
            throw ArityException.create((int) getNumArgs(), arguments.length);
        }
    }
}
