/*
 * Copyright (c) 2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.interpreter;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.bytecode.BytecodeConfigEncoder;
import com.oracle.truffle.api.bytecode.BytecodeParser;
import com.oracle.truffle.api.bytecode.BytecodeRootNodes;
import com.oracle.truffle.api.frame.FrameExtensions;

@SuppressWarnings({"rawtypes", "unchecked"})
public final class BytecodeDSLAccess extends BytecodeRootNodes {
    public static final FrameExtensions FRAMES = com.oracle.truffle.api.bytecode.BytecodeDSLAccess.lookup(TOKEN, true).getFrameExtensions();

    BytecodeDSLAccess(final Object token, final BytecodeParser parser) {
        super(token, parser);
        throw CompilerDirectives.shouldNotReachHere();
    }

    @Override
    protected boolean updateImpl(final BytecodeConfigEncoder encoder, final long encoding) {
        throw CompilerDirectives.shouldNotReachHere();
    }
}
