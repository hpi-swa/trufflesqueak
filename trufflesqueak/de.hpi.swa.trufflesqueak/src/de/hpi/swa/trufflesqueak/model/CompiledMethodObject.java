package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.interop.TruffleObject;

import de.hpi.swa.trufflesqueak.SqueakImageContext;

public class CompiledMethodObject extends CompiledCodeObject implements TruffleObject {
    public CompiledMethodObject(SqueakImageContext img) {
        super(img);
    }

    public CompiledMethodObject(SqueakImageContext img, byte[] bc, Object[] lits) {
        this(img);
        literals = lits;
        decodeHeader();
        initializeBytesNode(bc);
    }

    public CompiledMethodObject(SqueakImageContext img, ClassObject klass) {
        super(img, klass);
    }

    private CompiledMethodObject(CompiledMethodObject compiledMethodObject) {
        super(compiledMethodObject);
    }

    @Override
    public NativeObject getCompiledInSelector() {
        if (literals.length > 1) {
            Object lit = literals[literals.length - 2];
            if (lit == null) {
                return null;
            } else if (lit instanceof NativeObject) {
                return (NativeObject) lit;
            } else if ((lit instanceof BaseSqueakObject) && ((BaseSqueakObject) lit).size() >= 2) {
                lit = ((BaseSqueakObject) lit).at0(1);
                if (lit instanceof NativeObject) {
                    return (NativeObject) lit;
                }
            }
        }
        return null;
    }

    @Override
    public ClassObject getCompiledInClass() {
        if (literals.length == 0) {
            return null;
        }
        Object baseSqueakObject = literals[literals.length - 1];
        if (baseSqueakObject instanceof PointersObject) {
            if (((PointersObject) baseSqueakObject).size() == 2) {
                baseSqueakObject = ((PointersObject) baseSqueakObject).at0(1);
            }
        }
        if (baseSqueakObject instanceof ClassObject) {
            return (ClassObject) baseSqueakObject;
        }
        return null;
    }

    @Override
    public CompiledMethodObject getMethod() {
        return this;
    }

    @Override
    public BaseSqueakObject shallowCopy() {
        return new CompiledMethodObject(this);
    }
}
