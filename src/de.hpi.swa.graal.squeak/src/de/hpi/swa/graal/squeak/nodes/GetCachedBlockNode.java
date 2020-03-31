/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

import de.hpi.swa.graal.squeak.model.CompiledBlockObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;

public final class GetCachedBlockNode extends AbstractNodeWithCode {
    @CompilationFinal private CompiledBlockObject block;

    private GetCachedBlockNode(final CompiledCodeObject code) {
        super(code);
    }

    public static GetCachedBlockNode create(final CompiledCodeObject code) {
        return new GetCachedBlockNode(code);
    }

    public CompiledBlockObject execute(final int numArgs, final int numCopied, final int position, final int blockSize) {
        if (block == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            block = code.findBlock(code.getMethod(), numArgs, numCopied, position, blockSize);
        }
        return block;
    }
}
