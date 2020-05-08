/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.model;

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

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.SqueakImageChunk;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.image.SqueakImageWriter;
import de.hpi.swa.graal.squeak.interop.WrapToSqueakNode;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.BLOCK_CLOSURE;
import de.hpi.swa.graal.squeak.util.ArrayUtils;
import de.hpi.swa.graal.squeak.util.FrameAccess;
import de.hpi.swa.graal.squeak.util.ObjectGraphUtils.ObjectTracer;

@ExportLibrary(InteropLibrary.class)
public final class BlockClosureObject extends AbstractSqueakObjectWithHash {
    @CompilationFinal private Object receiver;
    @CompilationFinal private ContextObject outerContext;
    @CompilationFinal private CompiledBlockObject block;
    @CompilationFinal private long startPC = -1;
    @CompilationFinal private long numArgs = -1;
    @CompilationFinal(dimensions = 0) private Object[] copied;

    private BlockClosureObject(final SqueakImageContext image) {
        super(image);
    }

    private BlockClosureObject(final SqueakImageContext image, final long hash) {
        super(image, hash);
        copied = ArrayUtils.EMPTY_ARRAY; // Ensure copied is set.
    }

    public BlockClosureObject(final SqueakImageContext image, final CompiledBlockObject block, final int startPC, final int numArgs, final Object receiver, final Object[] copied,
                    final ContextObject outerContext) {
        super(image);
        assert block.getInitialPC() == startPC;
        this.block = block;
        this.outerContext = outerContext;
        this.receiver = receiver;
        this.copied = copied;
        this.startPC = startPC;
        this.numArgs = numArgs;
    }

    private BlockClosureObject(final BlockClosureObject original) {
        super(original);
        block = original.block;
        outerContext = original.outerContext;
        receiver = original.receiver;
        copied = original.copied;
        startPC = original.startPC;
        numArgs = original.numArgs;
    }

    public static BlockClosureObject createWithHash(final SqueakImageContext image, final int hash) {
        return new BlockClosureObject(image, hash);
    }

    public static BlockClosureObject create(final SqueakImageContext image, final int extraSize) {
        final BlockClosureObject result = new BlockClosureObject(image);
        result.copied = new Object[extraSize];
        return result;
    }

    @Override
    public ClassObject getSqueakClass() {
        return image.blockClosureClass;
    }

    @Override
    public void fillin(final SqueakImageChunk chunk) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        final Object[] pointers = chunk.getPointers();
        assert pointers.length >= BLOCK_CLOSURE.FIRST_COPIED_VALUE;
        outerContext = (ContextObject) pointers[BLOCK_CLOSURE.OUTER_CONTEXT];
        startPC = (long) pointers[BLOCK_CLOSURE.START_PC];
        numArgs = (long) pointers[BLOCK_CLOSURE.ARGUMENT_COUNT];
        copied = Arrays.copyOfRange(pointers, BLOCK_CLOSURE.FIRST_COPIED_VALUE, pointers.length);
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

    public Object getCopiedAt0(final int index) {
        return copied[index - BLOCK_CLOSURE.FIRST_COPIED_VALUE];
    }

    public Object[] getCopied() {
        return copied;
    }

    public void setOuterContext(final ContextObject outerContext) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        this.outerContext = outerContext;
    }

    public void setStartPC(final int pc) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        startPC = pc;
    }

    public void setNumArgs(final int numArgs) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        this.numArgs = numArgs;
    }

    public void setCopiedAt0(final int index, final Object value) {
        copied[index - BLOCK_CLOSURE.FIRST_COPIED_VALUE] = value;
    }

    public void setCopied(final Object[] copied) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        this.copied = copied;
    }

    public void become(final BlockClosureObject other) {
        final Object[] otherCopied = other.copied;
        other.setCopied(copied);
        setCopied(otherCopied);
    }

    @Override
    public int instsize() {
        return BLOCK_CLOSURE.FIRST_COPIED_VALUE;
    }

    @Override
    public int size() {
        return instsize() + copied.length;
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "a BlockClosureObject @" + Integer.toHexString(hashCode()) + ", with " + (numArgs == -1 && block == null ? "no block" : getNumArgs() + " args") + " and " + copied.length +
                        " copied values, in " + outerContext;
    }

    public Object getReceiver() {
        if (receiver == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            receiver = outerContext.getReceiver();
        }
        return receiver;
    }

    public void setReceiver(final Object receiver) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        this.receiver = receiver;
    }

    private void initializeCompiledBlock(final CompiledMethodObject method) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        assert startPC >= 0;
        final int offset = (int) startPC - method.getInitialPC();
        final int j = method.getBytes()[offset - 2];
        final int k = method.getBytes()[offset - 1];
        final int blockSize = j << 8 | k & 0xff;
        block = CompiledBlockObject.create(method, method, (int) numArgs, copied.length, offset, blockSize);
        /* Ensure fields dependent on block are initialized. */
        getStartPC();
        getNumArgs();
    }

    public CompiledBlockObject getCompiledBlock() {
        if (block == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            /* `outerContext.getMethod()` should not be part of compilation. */
            initializeCompiledBlock(outerContext.getMethod());
        }
        return block;
    }

    /** Special version of getCompiledBlock for image loader. */
    public CompiledBlockObject getCompiledBlock(final CompiledMethodObject method) {
        if (block == null) {
            initializeCompiledBlock(method);
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

    public BlockClosureObject shallowCopy() {
        return new BlockClosureObject(this);
    }

    @Override
    public void pointersBecomeOneWay(final Object[] from, final Object[] to, final boolean copyHash) {
        for (int i = 0; i < from.length; i++) {
            final Object fromPointer = from[i];
            if (getReceiver() == fromPointer && fromPointer != to[i]) {
                setReceiver(to[i]);
                copyHash(fromPointer, to[i], copyHash);
            }
            if (getOuterContextOrNull() == fromPointer && fromPointer != to[i] && to[i] instanceof ContextObject) {
                setOuterContext((ContextObject) to[i]);
                copyHash(fromPointer, to[i], copyHash);
            }
            for (int j = 0; j < getCopied().length; j++) {
                final Object copiedValue = getCopied()[j];
                if (copiedValue == fromPointer) {
                    getCopied()[j] = to[i];
                    copyHash(fromPointer, to[i], copyHash);
                }
            }
        }
    }

    @Override
    public void tracePointers(final ObjectTracer tracer) {
        tracer.addIfUnmarked(getReceiver());
        tracer.addIfUnmarked(getOuterContext());
        for (final Object value : getCopied()) {
            tracer.addIfUnmarked(value);
        }
    }

    @Override
    public void trace(final SqueakImageWriter writer) {
        super.trace(writer);
        writer.traceIfNecessary(receiver);
        writer.traceIfNecessary(outerContext);
        writer.traceAllIfNecessary(getCopied());
    }

    @Override
    public void write(final SqueakImageWriter writer) {
        if (!writeHeader(writer)) {
            throw SqueakException.create("BlockClosureObject must have slots:", this);
        }
        writer.writeObject(getOuterContext());
        writer.writeSmallInteger(getStartPC());
        writer.writeSmallInteger(getNumArgs());
        for (final Object value : getCopied()) {
            writer.writeObject(value);
        }
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
