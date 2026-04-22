/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.interpreter;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.bytecode.BytecodeConfigEncoder;
import com.oracle.truffle.api.bytecode.BytecodeDSLAccess;
import com.oracle.truffle.api.bytecode.BytecodeParser;
import com.oracle.truffle.api.bytecode.BytecodeRootNode;
import com.oracle.truffle.api.bytecode.BytecodeRootNodes;
import com.oracle.truffle.api.nodes.RootNode;

final class BytecodeDSLAccessProvider extends BytecodeRootNodes<BytecodeDSLAccessProvider.SLBytecodeRootNode> {
    public abstract static class SLBytecodeRootNode extends RootNode implements BytecodeRootNode {
        protected SLBytecodeRootNode(final TruffleLanguage<?> language) {
            super(language);
            throw CompilerDirectives.shouldNotReachHere();
        }
    }

    BytecodeDSLAccessProvider(final Object token, final BytecodeParser<?> parser) {
        super(token, parser);
        throw CompilerDirectives.shouldNotReachHere();
    }

    public static BytecodeDSLAccess getBytecodeDSLAccess(final boolean allowUnsafe) {
        return BytecodeDSLAccess.lookup(TOKEN, allowUnsafe);
    }

    @Override
    protected boolean updateImpl(final BytecodeConfigEncoder encoder, final long encoding) {
        throw CompilerDirectives.shouldNotReachHere();
    }
}
