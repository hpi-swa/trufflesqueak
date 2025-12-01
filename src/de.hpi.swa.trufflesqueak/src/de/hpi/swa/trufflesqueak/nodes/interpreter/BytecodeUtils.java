/*
 * Copyright (c) 2025-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2025-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.interpreter;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class BytecodeUtils {
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
