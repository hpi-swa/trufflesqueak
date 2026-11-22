/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.lang.foreign.Arena;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive0WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive2WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.util.VarHandleUtils;

/**
 * Accessors for Aliens (see IA32ABIPlugin in VMMaker). An Alien is a byte object whose first word
 * is its size field: a positive value is the number of data bytes that follow the size field
 * (direct Alien), a negative value is the size of the data that the second word points to (indirect
 * Alien), and zero means that the second word is a pointer (pointer Alien).
 *
 * Only the fields of Aliens and the data of direct Aliens are accessible, and libraries can be
 * loaded and searched for symbols: Native memory, callouts, and callbacks are not supported. This is
 * sufficient for images that only prepare for foreign calls during startup (e.g., Newspeak).
 */
public final class IA32ABI extends AbstractPrimitiveFactoryHolder {
    private static final int WORD_SIZE = Long.BYTES;

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return IA32ABIFactory.getFactories();
    }

    /**
     * Answers the index of the given one-based index into the data of a direct Alien.
     */
    private static int dataIndex(final byte[] bytes, final long index, final int length) {
        if (bytes.length < WORD_SIZE) {
            throw PrimitiveFailed.BAD_RECEIVER;
        }
        final long sizeField = VarHandleUtils.getLongFromBytes(bytes, 0);
        if (sizeField <= 0) {
            throw PrimitiveFailed.BAD_RECEIVER; /* Native memory is not supported. */
        } else if (index < 1 || index - 1 + length > sizeField || WORD_SIZE + index - 1 + length > bytes.length) {
            throw PrimitiveFailed.BAD_INDEX;
        }
        return WORD_SIZE + (int) index - 1;
    }

    private static long toUnsigned(final long value) {
        if (value < 0) {
            throw PrimitiveFailed.GENERIC_ERROR; /* Would require a LargePositiveInteger. */
        }
        return value;
    }

    /*
     * Libraries
     */

    /** Handles of loaded libraries are indices into this list plus one. */
    private static final ArrayList<SymbolLookup> LOADED_LIBRARIES = new ArrayList<>();

    @TruffleBoundary
    @SuppressWarnings("restricted")
    private static long loadLibrary(final String name) {
        SymbolLookup lookup;
        try {
            lookup = SymbolLookup.libraryLookup(name, Arena.global());
        } catch (final IllegalArgumentException e) {
            if (name.startsWith("libc.")) {
                /* The C library is always available (libc.so is a linker script on some systems). */
                lookup = Linker.nativeLinker().defaultLookup();
            } else {
                throw PrimitiveFailed.NOT_FOUND;
            }
        }
        synchronized (LOADED_LIBRARIES) {
            LOADED_LIBRARIES.add(lookup);
            return LOADED_LIBRARIES.size();
        }
    }

    @TruffleBoundary
    private static long findSymbol(final long libraryHandle, final String name) {
        final SymbolLookup lookup;
        synchronized (LOADED_LIBRARIES) {
            if (libraryHandle < 1 || libraryHandle > LOADED_LIBRARIES.size()) {
                throw PrimitiveFailed.BAD_ARGUMENT;
            }
            lookup = LOADED_LIBRARIES.get((int) libraryHandle - 1);
        }
        final Optional<MemorySegment> symbol = lookup.find(name);
        if (symbol.isEmpty() || symbol.get().address() <= 0) {
            throw PrimitiveFailed.NOT_FOUND;
        }
        return symbol.get().address();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primLoadLibrary")
    protected abstract static class PrimLoadLibraryNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "libraryName.isByteType()")
        protected static final long doLoad(@SuppressWarnings("unused") final Object receiver, final NativeObject libraryName) {
            return loadLibrary(libraryName.asStringUnsafe());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primInLibraryFindSymbol")
    protected abstract static class PrimInLibraryFindSymbolNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        /* The handle of the library is stored in the first data word of the Alien. */
        @Specialization(guards = {"libraryProxy.isByteType()", "libraryProxy.getByteLength() >= 16", "symbolName.isByteType()"})
        protected static final long doFind(final NativeObject libraryProxy, final NativeObject symbolName) {
            return findSymbol(VarHandleUtils.getLongFromBytes(libraryProxy.getByteStorage(), WORD_SIZE), symbolName.asStringUnsafe());
        }
    }

    /*
     * Fields and data of Aliens
     */

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primSizeField")
    protected abstract static class PrimSizeFieldNode extends AbstractPrimitiveNode implements Primitive0WithFallback {
        @Specialization(guards = {"receiver.isByteType()", "receiver.getByteLength() >= 8"})
        protected static final long doSizeField(final NativeObject receiver) {
            return VarHandleUtils.getLongFromBytes(receiver.getByteStorage(), 0);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primSizeFieldPut")
    protected abstract static class PrimSizeFieldPutNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"receiver.isByteType()", "receiver.getByteLength() >= 8"})
        protected static final long doSizeFieldPut(final NativeObject receiver, final long value) {
            VarHandleUtils.putLongIntoBytes(receiver.getByteStorage(), 0, value);
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primAddressField")
    protected abstract static class PrimAddressFieldNode extends AbstractPrimitiveNode implements Primitive0WithFallback {
        @Specialization(guards = {"receiver.isByteType()", "receiver.getByteLength() >= 16"})
        protected static final long doAddressField(final NativeObject receiver) {
            return toUnsigned(VarHandleUtils.getLongFromBytes(receiver.getByteStorage(), WORD_SIZE));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primAddressFieldPut")
    protected abstract static class PrimAddressFieldPutNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = {"receiver.isByteType()", "receiver.getByteLength() >= 16", "value >= 0"})
        protected static final long doAddressFieldPut(final NativeObject receiver, final long value) {
            VarHandleUtils.putLongIntoBytes(receiver.getByteStorage(), WORD_SIZE, value);
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primUnsignedByteAt")
    protected abstract static class PrimUnsignedByteAtNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "receiver.isByteType()")
        protected static final long doAt(final NativeObject receiver, final long index) {
            final byte[] bytes = receiver.getByteStorage();
            final int i = dataIndex(bytes, index, 1);
            return Byte.toUnsignedLong(bytes[i]);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primUnsignedByteAtPut")
    protected abstract static class PrimUnsignedByteAtPutNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = "receiver.isByteType()")
        protected static final long doAtPut(final NativeObject receiver, final long index, final long value) {
            if (!(0 <= value && value <= 0xFF)) {
                throw PrimitiveFailed.BAD_ARGUMENT;
            }
            final byte[] bytes = receiver.getByteStorage();
            final int i = dataIndex(bytes, index, 1);
            bytes[i] = (byte) value;
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primSignedByteAt")
    protected abstract static class PrimSignedByteAtNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "receiver.isByteType()")
        protected static final long doAt(final NativeObject receiver, final long index) {
            final byte[] bytes = receiver.getByteStorage();
            final int i = dataIndex(bytes, index, 1);
            return (long) bytes[i];
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primSignedByteAtPut")
    protected abstract static class PrimSignedByteAtPutNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = "receiver.isByteType()")
        protected static final long doAtPut(final NativeObject receiver, final long index, final long value) {
            if (!(Byte.MIN_VALUE <= value && value <= Byte.MAX_VALUE)) {
                throw PrimitiveFailed.BAD_ARGUMENT;
            }
            final byte[] bytes = receiver.getByteStorage();
            final int i = dataIndex(bytes, index, 1);
            bytes[i] = (byte) value;
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primUnsignedShortAt")
    protected abstract static class PrimUnsignedShortAtNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "receiver.isByteType()")
        protected static final long doAt(final NativeObject receiver, final long index) {
            final byte[] bytes = receiver.getByteStorage();
            final int i = dataIndex(bytes, index, 2);
            return Short.toUnsignedLong(VarHandleUtils.getShortFromBytes(bytes, i));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primUnsignedShortAtPut")
    protected abstract static class PrimUnsignedShortAtPutNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = "receiver.isByteType()")
        protected static final long doAtPut(final NativeObject receiver, final long index, final long value) {
            if (!(0 <= value && value <= 0xFFFF)) {
                throw PrimitiveFailed.BAD_ARGUMENT;
            }
            final byte[] bytes = receiver.getByteStorage();
            final int i = dataIndex(bytes, index, 2);
            VarHandleUtils.putShortIntoBytes(bytes, i, (short) value);
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primSignedShortAt")
    protected abstract static class PrimSignedShortAtNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "receiver.isByteType()")
        protected static final long doAt(final NativeObject receiver, final long index) {
            final byte[] bytes = receiver.getByteStorage();
            final int i = dataIndex(bytes, index, 2);
            return (long) VarHandleUtils.getShortFromBytes(bytes, i);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primSignedShortAtPut")
    protected abstract static class PrimSignedShortAtPutNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = "receiver.isByteType()")
        protected static final long doAtPut(final NativeObject receiver, final long index, final long value) {
            if (!(Short.MIN_VALUE <= value && value <= Short.MAX_VALUE)) {
                throw PrimitiveFailed.BAD_ARGUMENT;
            }
            final byte[] bytes = receiver.getByteStorage();
            final int i = dataIndex(bytes, index, 2);
            VarHandleUtils.putShortIntoBytes(bytes, i, (short) value);
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primUnsignedLongAt")
    protected abstract static class PrimUnsignedLongAtNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "receiver.isByteType()")
        protected static final long doAt(final NativeObject receiver, final long index) {
            final byte[] bytes = receiver.getByteStorage();
            final int i = dataIndex(bytes, index, 4);
            return Integer.toUnsignedLong(VarHandleUtils.getIntFromBytes(bytes, i));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primUnsignedLongAtPut")
    protected abstract static class PrimUnsignedLongAtPutNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = "receiver.isByteType()")
        protected static final long doAtPut(final NativeObject receiver, final long index, final long value) {
            if (!(0 <= value && value <= 0xFFFFFFFFL)) {
                throw PrimitiveFailed.BAD_ARGUMENT;
            }
            final byte[] bytes = receiver.getByteStorage();
            final int i = dataIndex(bytes, index, 4);
            VarHandleUtils.putIntIntoBytes(bytes, i, (int) value);
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primSignedLongAt")
    protected abstract static class PrimSignedLongAtNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "receiver.isByteType()")
        protected static final long doAt(final NativeObject receiver, final long index) {
            final byte[] bytes = receiver.getByteStorage();
            final int i = dataIndex(bytes, index, 4);
            return (long) VarHandleUtils.getIntFromBytes(bytes, i);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primSignedLongAtPut")
    protected abstract static class PrimSignedLongAtPutNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = "receiver.isByteType()")
        protected static final long doAtPut(final NativeObject receiver, final long index, final long value) {
            if (!(Integer.MIN_VALUE <= value && value <= Integer.MAX_VALUE)) {
                throw PrimitiveFailed.BAD_ARGUMENT;
            }
            final byte[] bytes = receiver.getByteStorage();
            final int i = dataIndex(bytes, index, 4);
            VarHandleUtils.putIntIntoBytes(bytes, i, (int) value);
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primUnsignedLongLongAt")
    protected abstract static class PrimUnsignedLongLongAtNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "receiver.isByteType()")
        protected static final long doAt(final NativeObject receiver, final long index) {
            final byte[] bytes = receiver.getByteStorage();
            final int i = dataIndex(bytes, index, 8);
            return toUnsigned(VarHandleUtils.getLongFromBytes(bytes, i));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primUnsignedLongLongAtPut")
    protected abstract static class PrimUnsignedLongLongAtPutNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = "receiver.isByteType()")
        protected static final long doAtPut(final NativeObject receiver, final long index, final long value) {
            if (!(0 <= value)) {
                throw PrimitiveFailed.BAD_ARGUMENT;
            }
            final byte[] bytes = receiver.getByteStorage();
            final int i = dataIndex(bytes, index, 8);
            VarHandleUtils.putLongIntoBytes(bytes, i, value);
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primSignedLongLongAt")
    protected abstract static class PrimSignedLongLongAtNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "receiver.isByteType()")
        protected static final long doAt(final NativeObject receiver, final long index) {
            final byte[] bytes = receiver.getByteStorage();
            final int i = dataIndex(bytes, index, 8);
            return VarHandleUtils.getLongFromBytes(bytes, i);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primSignedLongLongAtPut")
    protected abstract static class PrimSignedLongLongAtPutNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = "receiver.isByteType()")
        protected static final long doAtPut(final NativeObject receiver, final long index, final long value) {
            if (!(true)) {
                throw PrimitiveFailed.BAD_ARGUMENT;
            }
            final byte[] bytes = receiver.getByteStorage();
            final int i = dataIndex(bytes, index, 8);
            VarHandleUtils.putLongIntoBytes(bytes, i, value);
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primUnsignedWordAt")
    protected abstract static class PrimUnsignedWordAtNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "receiver.isByteType()")
        protected static final long doAt(final NativeObject receiver, final long index) {
            final byte[] bytes = receiver.getByteStorage();
            final int i = dataIndex(bytes, index, 8);
            return toUnsigned(VarHandleUtils.getLongFromBytes(bytes, i));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primUnsignedWordAtPut")
    protected abstract static class PrimUnsignedWordAtPutNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = "receiver.isByteType()")
        protected static final long doAtPut(final NativeObject receiver, final long index, final long value) {
            if (!(0 <= value)) {
                throw PrimitiveFailed.BAD_ARGUMENT;
            }
            final byte[] bytes = receiver.getByteStorage();
            final int i = dataIndex(bytes, index, 8);
            VarHandleUtils.putLongIntoBytes(bytes, i, value);
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primSignedWordAt")
    protected abstract static class PrimSignedWordAtNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "receiver.isByteType()")
        protected static final long doAt(final NativeObject receiver, final long index) {
            final byte[] bytes = receiver.getByteStorage();
            final int i = dataIndex(bytes, index, 8);
            return VarHandleUtils.getLongFromBytes(bytes, i);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primSignedWordAtPut")
    protected abstract static class PrimSignedWordAtPutNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = "receiver.isByteType()")
        protected static final long doAtPut(final NativeObject receiver, final long index, final long value) {
            if (!(true)) {
                throw PrimitiveFailed.BAD_ARGUMENT;
            }
            final byte[] bytes = receiver.getByteStorage();
            final int i = dataIndex(bytes, index, 8);
            VarHandleUtils.putLongIntoBytes(bytes, i, value);
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primFloatAt")
    protected abstract static class PrimFloatAtNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "receiver.isByteType()")
        protected static final double doAt(final NativeObject receiver, final long index) {
            final byte[] bytes = receiver.getByteStorage();
            return VarHandleUtils.getFloatFromBytes(bytes, dataIndex(bytes, index, 4));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primFloatAtPut")
    protected abstract static class PrimFloatAtPutNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = "receiver.isByteType()")
        protected static final double doAtPut(final NativeObject receiver, final long index, final double value) {
            final byte[] bytes = receiver.getByteStorage();
            VarHandleUtils.putFloatIntoBytes(bytes, dataIndex(bytes, index, 4), (float) value);
            return value;
        }

        @Specialization(guards = "receiver.isByteType()")
        protected static final long doAtPut(final NativeObject receiver, final long index, final long value) {
            doAtPut(receiver, index, (double) value);
            return value;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primDoubleAt")
    protected abstract static class PrimDoubleAtNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "receiver.isByteType()")
        protected static final double doAt(final NativeObject receiver, final long index) {
            final byte[] bytes = receiver.getByteStorage();
            return VarHandleUtils.getDoubleFromBytes(bytes, dataIndex(bytes, index, 8));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primDoubleAtPut")
    protected abstract static class PrimDoubleAtPutNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = "receiver.isByteType()")
        protected static final double doAtPut(final NativeObject receiver, final long index, final double value) {
            final byte[] bytes = receiver.getByteStorage();
            VarHandleUtils.putDoubleIntoBytes(bytes, dataIndex(bytes, index, 8), value);
            return value;
        }

        @Specialization(guards = "receiver.isByteType()")
        protected static final long doAtPut(final NativeObject receiver, final long index, final long value) {
            doAtPut(receiver, index, (double) value);
            return value;
        }
    }
}
