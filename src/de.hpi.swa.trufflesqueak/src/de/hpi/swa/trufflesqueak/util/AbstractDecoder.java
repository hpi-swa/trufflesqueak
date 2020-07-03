/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.util;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.AbstractBytecodeNode;

public abstract class AbstractDecoder {
    public abstract AbstractBytecodeNode[] decode(CompiledCodeObject code);

    public abstract AbstractBytecodeNode decodeBytecode(CompiledCodeObject code, int index);

    public abstract String decodeToString(CompiledCodeObject code);

    public abstract int findLineNumber(CompiledCodeObject code, int targetIndex);

    public abstract int trailerPosition(CompiledCodeObject compiledCodeObject);
}
