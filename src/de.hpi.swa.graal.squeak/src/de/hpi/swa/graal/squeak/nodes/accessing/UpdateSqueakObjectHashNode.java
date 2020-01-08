/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.accessing;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.model.AbstractSqueakObjectWithHash;

public abstract class UpdateSqueakObjectHashNode extends Node {
    public static UpdateSqueakObjectHashNode create() {
        return UpdateSqueakObjectHashNodeGen.create();
    }

    public abstract void executeUpdate(Object fromPointer, Object toPointer, boolean copyHash);

    @Specialization(guards = "copyHash")
    protected static final void doCopy(final AbstractSqueakObjectWithHash fromPointer, final AbstractSqueakObjectWithHash toPointer,
                    @SuppressWarnings("unused") final boolean copyHash) {
        toPointer.setSqueakHash(fromPointer.getSqueakHash());
    }

    @SuppressWarnings("unused")
    @Fallback
    protected final void doFallback(final Object fromPointer, final Object toPointer, final boolean copyHash) {
        // nothing to do
    }
}
