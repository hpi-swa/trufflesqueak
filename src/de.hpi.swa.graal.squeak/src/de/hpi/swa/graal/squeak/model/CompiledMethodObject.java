package de.hpi.swa.graal.squeak.model;

import com.oracle.truffle.api.CompilerAsserts;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;

public final class CompiledMethodObject extends CompiledCodeObject {

    public static CompiledMethodObject newOfSize(final SqueakImageContext image, final int size) {
        return new CompiledMethodObject(size, image);
    }

    public static CompiledMethodObject newWithHash(final SqueakImageContext image, final int hash) {
        return new CompiledMethodObject(image, hash);
    }

    public CompiledMethodObject(final SqueakImageContext image, final int hash) {
        super(image, hash, 0);
    }

    public CompiledMethodObject(final SqueakImageContext image, final byte[] bc, final Object[] lits) {
        super(image, 0, 0);
        literals = lits;
        decodeHeader();
        bytes = bc;
    }

    public CompiledMethodObject(final int size, final SqueakImageContext image) {
        super(image, 0, 0);
        bytes = new byte[size];
    }

    private CompiledMethodObject(final CompiledMethodObject original) {
        super(original);
    }

    public Object at0(final long longIndex) {
        final int index = (int) longIndex;
        if (index < getBytecodeOffset()) {
            assert index % image.flags.wordSize() == 0;
            return literals[index / image.flags.wordSize()];
        } else {
            final int realIndex = index - getBytecodeOffset();
            assert realIndex >= 0;
            return Byte.toUnsignedLong(bytes[realIndex]);
        }
    }

    public AbstractSqueakObject penultimateLiteral() {
        final int index = numLiterals - 1;
        if (index > 0) {
            return (AbstractSqueakObject) literals[index];
        } else {
            return image.nil;
        }
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        String className = "UnknownClass";
        String selector = "unknownSelector";
        final ClassObject classObject = getCompiledInClass();
        if (classObject != null) {
            className = classObject.nameAsClass();
        }
        final NativeObject selectorObj = getCompiledInSelector();
        if (selectorObj != null) {
            selector = selectorObj.asString();
        }
        return className + ">>" + selector;
    }

    public NativeObject getCompiledInSelector() {
        CompilerAsserts.neverPartOfCompilation("Do not use getCompiledInSelector() in compiled code");
        if (literals.length > 1) {
            final Object lit = literals[literals.length - 2];
            if (lit == null) {
                return null;
            } else if (lit instanceof NativeObject) {
                return (NativeObject) lit;
            } else if ((lit instanceof PointersObject) && ((PointersObject) lit).size() >= 2) {
                final Object secondValue = ((PointersObject) lit).at0(1);
                if (secondValue instanceof NativeObject) {
                    return (NativeObject) secondValue;
                }
            }
        }
        return null;
    }

    public ClassObject getCompiledInClass() {
        if (literals.length == 0) {
            return null;
        }
        Object baseSqueakObject = literals[literals.length - 1];
        if (baseSqueakObject instanceof PointersObject && ((PointersObject) baseSqueakObject).size() == 2) {
            baseSqueakObject = ((PointersObject) baseSqueakObject).at0(1);
        }
        if (baseSqueakObject instanceof ClassObject) {
            return (ClassObject) baseSqueakObject;
        }
        return null;
    }

    public void setCompiledInClass(final ClassObject newClass) {
        if (literals.length == 0) {
            return;
        }
        final Object baseSqueakObject = literals[literals.length - 1];
        if (baseSqueakObject instanceof PointersObject && ((PointersObject) baseSqueakObject).size() == 2) {
            ((PointersObject) baseSqueakObject).atput0(1, newClass);
            return;
        }
        if (baseSqueakObject instanceof ClassObject) {
            literals[literals.length - 1] = newClass;
        }
    }

    public void setHeader(final long header) {
        literals = new Object[]{header};
        decodeHeader();
        literals = new Object[1 + numLiterals];
        literals[0] = header;
        for (int i = 1; i < literals.length; i++) {
            literals[i] = image.nil;
        }
    }

    public AbstractSqueakObject shallowCopy() {
        return new CompiledMethodObject(this);
    }

    public boolean isExceptionHandlerMarked() {
        return hasPrimitive() && primitiveIndex() == 199;
    }

    /*
     * Answer the program counter for the receiver's first bytecode.
     *
     */
    public int getInitialPC() {
        // pc is offset by header + numLiterals, +1 for one-based addressing
        return getBytecodeOffset() + 1;
    }

    public int size() {
        return getBytecodeOffset() + bytes.length;
    }
}
