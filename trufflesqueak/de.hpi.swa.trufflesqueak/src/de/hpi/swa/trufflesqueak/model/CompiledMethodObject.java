package de.hpi.swa.trufflesqueak.model;

import java.util.Vector;

import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.TruffleObject;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.exceptions.InvalidIndex;
import de.hpi.swa.trufflesqueak.nodes.PrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.SqueakBytecodeNode;
import de.hpi.swa.trufflesqueak.util.BitSplitter;
import de.hpi.swa.trufflesqueak.util.Chunk;

public class CompiledMethodObject extends SqueakObject implements TruffleObject {
    protected BaseSqueakObject[] literals;
    protected byte[] bytes;
    private int numLiterals;
    private boolean isOptimized;
    private boolean hasPrimitive;
    private boolean needsLargeFrame;
    private int numTemps;
    private int numArgs;
    private int accessModifier;
    private boolean altInstructionSet;

    @Override
    public void fillin(Chunk chunk, SqueakImageContext img) {
        super.fillin(chunk, img);
        Vector<Integer> data = chunk.data();
        int header = data.get(0) >> 1; // header is a tagged small integer
        int literalsize = header & 0x7fff;
        BaseSqueakObject[] ptrs = chunk.getPointers(literalsize + 1);
        literals = ptrs;
        bytes = chunk.getBytes(literals.length);
        setHeader(literals[0]);
    }

    private void setHeader(BaseSqueakObject baseSqueakObject) {
        assert baseSqueakObject instanceof SmallInteger;
        SmallInteger smallInt = (SmallInteger) baseSqueakObject;
        int[] splitHeader = BitSplitter.splitter(smallInt.getValue(), new int[]{15, 1, 1, 1, 6, 4, 2, 1});
        numLiterals = splitHeader[0];
        isOptimized = splitHeader[1] == 1;
        hasPrimitive = splitHeader[2] == 1;
        needsLargeFrame = splitHeader[3] == 1;
        numTemps = splitHeader[4];
        numArgs = splitHeader[5];
        accessModifier = splitHeader[6];
        altInstructionSet = splitHeader[7] == 1;
    }

    public ForeignAccess getForeignAccess() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String toString() {
        String className = "UnknownClass";
        String selector = "unknownSelector";
        if (literals.length > 0) {
            BaseSqueakObject baseSqueakObject = literals[literals.length - 1];
            if (baseSqueakObject instanceof PointersObject) {
                if (((PointersObject) baseSqueakObject).size() == 2) {
                    try {
                        baseSqueakObject = ((PointersObject) baseSqueakObject).at0(1);
                    } catch (InvalidIndex e) {
                        assert false;
                    }
                }
                if (((PointersObject) baseSqueakObject).isClass()) {
                    className = ((PointersObject) baseSqueakObject).nameAsClass();
                }
            }

            if (literals.length > 1) {
                baseSqueakObject = literals[literals.length - 2];
                if (baseSqueakObject instanceof NativeObject) {
                    selector = baseSqueakObject.toString();
                }
            }
        }
        return className + ">>" + selector;
    }

    @Override
    public boolean become(BaseSqueakObject other) {
        if (other instanceof CompiledMethodObject) {
            if (super.become(other)) {

                BaseSqueakObject[] literals2 = ((CompiledMethodObject) other).literals;
                ((CompiledMethodObject) other).literals = this.literals;
                this.literals = literals2;

                byte[] bytes2 = ((CompiledMethodObject) other).bytes;
                ((CompiledMethodObject) other).bytes = this.bytes;
                this.bytes = bytes2;
                return true;
            }
        }
        return false;
    }

    public PrimitiveNode getPrimitiveNode() {
        // TODO Auto-generated method stub
        return null;
    }

    public SqueakBytecodeNode getBytecodeNode() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int size() {
        return literals.length * 4 + bytes.length;
    }
}
