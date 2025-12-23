/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.interpreter;

import com.oracle.truffle.api.CompilerAsserts;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public abstract class AbstractDecoder {

    public record ShadowBlockParams(int numArgs, int numCopied, int blockSize) {
    }

    public abstract ShadowBlockParams decodeShadowBlock(CompiledCodeObject code, int shadowBlockIndex);

    public abstract boolean hasStoreIntoTemp1AfterCallPrimitive(CompiledCodeObject code);

    public abstract int pcPreviousTo(CompiledCodeObject code, int pc);

    public abstract int determineMaxNumStackSlots(CompiledCodeObject code, int initialPC, int maxPC);

    protected abstract int decodeNumBytes(CompiledCodeObject code, int index);

    protected abstract String decodeBytecodeToString(CompiledCodeObject code, int currentByte, int bytecodeIndex);

    public final String safeDecodeBytecodeToString(final CompiledCodeObject code, final int currentByte, final int bytecodeIndex) {
        try {
            return decodeBytecodeToString(code, currentByte, bytecodeIndex);
        } catch (RuntimeException e) {
            return "error decoding: " + currentByte;
        }
    }

    public final String decodeToString(final CompiledCodeObject code) {
        CompilerAsserts.neverPartOfCompilation();
        final StringBuilder sb = new StringBuilder();
        final int trailerPosition = trailerPosition(code);
        int bytecodeIndex = 0;
        int lineIndex = 1;
        int indent = 0;
        final byte[] bytes = code.getBytes();
        while (bytecodeIndex < trailerPosition) {
            final int currentByte = Byte.toUnsignedInt(bytes[bytecodeIndex]);
            sb.append(lineIndex);
            for (int j = 0; j < 1 + indent; j++) {
                sb.append(' ');
            }
            final int numBytecodes = decodeNumBytes(code, bytecodeIndex);
            sb.append('<');
            for (int j = bytecodeIndex; j < bytecodeIndex + numBytecodes; j++) {
                if (j > bytecodeIndex) {
                    sb.append(' ');
                }
                if (j < bytes.length) {
                    sb.append(String.format("%02X", bytes[j]));
                }
            }
            sb.append("> ");
            sb.append(decodeBytecodeToString(code, currentByte, bytecodeIndex));
            if (currentByte == 143) {
                indent++; // increment indent on push closure
            } else if (currentByte == 125) {
                indent--; // decrement indent on block return
            }
            lineIndex++;
            bytecodeIndex += numBytecodes;
            if (bytecodeIndex < trailerPosition) {
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    public final int findLineNumber(final CompiledCodeObject code, final int successorIndex) {
        if (successorIndex == 0) { // handle backjumps to startPC
            return 1;
        }
        final int trailerPosition = trailerPosition(code);
        int index = 0;
        assert index < successorIndex && successorIndex <= trailerPosition : successorIndex + " not between 0 and " + trailerPosition;
        int lineNumber = 0;
        while (index != successorIndex && index < trailerPosition) {
            index += decodeNumBytes(code, index);
            lineNumber++;
        }
        assert index == successorIndex && lineNumber <= trailerPosition;
        return lineNumber;
    }

    public static final int trailerPosition(final CompiledCodeObject code) {
        return code.isCompiledBlock() ? code.getBytes().length : trailerPosition(code.getBytes());
    }

    private static int trailerPosition(final byte[] bytecode) {
        final int bytecodeLength = bytecode.length;
        final int flagByte = Byte.toUnsignedInt(bytecode[bytecodeLength - 1]);
        final int index = (flagByte >> 2) + 1;
        return switch (index) {
            // #decodeNoTrailer and #decodeSourceBySelector
            case 1, 5 -> bytecodeLength - 1;
            // #decodeClearedTrailer, #decodeTempsNamesQCompress, #decodeTempsNamesZip,
            // #decodeSourceByStringIdentifier, #decodeEmbeddedSourceQCompress, and
            // #decodeEmbeddedSourceZip
            case 2, 3, 4, 6, 7, 8 -> decodeLengthField(bytecode, bytecodeLength, flagByte);
            // #decodeVarLengthSourcePointer
            case 9 -> {
                int pos = bytecodeLength - 2;
                while (bytecode[pos] < 0) {
                    pos--;
                }
                yield pos;
            }
            // #decodeSourcePointer
            case 64 -> bytecodeLength - 4;
            default -> throw SqueakException.create("Undefined method encoding (see CompiledMethodTrailer).");
        };
    }

    private static int decodeLengthField(final byte[] bytecode, final int bytecodeLength, final int flagByte) {
        final int numBytes = (flagByte & 3) + 1;
        int length = 0;
        final int firstLengthValueIndex = bytecodeLength - 2;
        for (int i = 0; i < numBytes; i++) {
            length = (length << 8) + Byte.toUnsignedInt(bytecode[firstLengthValueIndex - i]);
        }
        return bytecodeLength - (1 + numBytes + length);
    }

}
