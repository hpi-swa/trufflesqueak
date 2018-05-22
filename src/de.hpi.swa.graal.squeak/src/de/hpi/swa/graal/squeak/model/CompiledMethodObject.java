package de.hpi.swa.graal.squeak.model;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.nodes.RootNode;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;

public final class CompiledMethodObject extends CompiledCodeObject {

    public CompiledMethodObject(final SqueakImageContext img) {
        super(img);
    }

    public CompiledMethodObject(final SqueakImageContext img, final byte[] bc, final Object[] lits) {
        this(img);
        literals = lits;
        decodeHeader();
        bytes = bc;
    }

    public CompiledMethodObject(final SqueakImageContext img, final ClassObject klass, final int size) {
        super(img, klass);
        bytes = new byte[size];
    }

    private CompiledMethodObject(final CompiledMethodObject compiledMethodObject) {
        super(compiledMethodObject);
    }

    public Object at0(final long longIndex) {
        final int index = (int) longIndex;
        if (index < getBytecodeOffset()) {
            assert index % BYTES_PER_WORD == 0;
            return literals[index / BYTES_PER_WORD];
        } else {
            final int realIndex = index - getBytecodeOffset();
            assert realIndex >= 0;
            return Byte.toUnsignedLong(bytes[realIndex]);
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
            selector = new String(selectorObj.getByteStorage(storageType));
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

    /*
     * Answer the program counter for the receiver's first bytecode.
     *
     */
    public int getInitialPC() {
        // pc is offset by header + numLiterals, +1 for one-based addressing
        return getBytecodeOffset() + 1;
    }

    @Override
    public void pointersBecomeOneWay(final Object[] from, final Object[] to, final boolean copyHash) {
        super.pointersBecomeOneWay(from, to, copyHash);
        final ClassObject oldClass = getCompiledInClass();
        for (int i = 0; i < from.length; i++) {
            if (from[i] == oldClass) {
                final ClassObject newClass = (ClassObject) to[i];  // must be a ClassObject
                setCompiledInClass(newClass);
                if (copyHash) {
                    newClass.setSqueakHash(oldClass.squeakHash());
                }
                // TODO: flush method caches
            }
        }
    }

    @Override
    @ExplodeLoop
    public RootCallTarget getSplitCallTarget() {
        final RootCallTarget target = getCallTarget();
        final NativeObject selector = getCompiledInSelector();
        for (int i = 0; i < image.specialSelectorsArray.length; i++) {
            if (Arrays.equals(image.specialSelectorsArray[i].getByteStorage(storageType), selector.getByteStorage(storageType))) {
                return split(target);
            }
        }
        return target;
    }

    /**
     * Replicate the CallTarget to let each builtin call site executes its own AST.
     */
    private static RootCallTarget split(final RootCallTarget callTarget) {
        CompilerAsserts.neverPartOfCompilation();
        final RootNode rootNode = callTarget.getRootNode();
        return Truffle.getRuntime().createCallTarget(NodeUtil.cloneNode(rootNode));
    }

}
