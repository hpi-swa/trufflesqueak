package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.time.Instant;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNode;

public class PrimUtcClock extends PrimitiveNode {
    // The Delta between Squeak Epoch (Jan 1st 1901) and POSIX Epoch (Jan 1st 1970)
    private final long SQUEAK_EPOCH_DELTA_MICROSECONDS = 2177452800000000L;
    private final long SEC2USEC = 1000 * 1000;
    private final long USEC2NANO = 1000;

    public PrimUtcClock(CompiledMethodObject cm) {
        super(cm);
    }

    @Specialization
    protected long time() {
        Instant now = Instant.now();
        long epochSecond = now.getEpochSecond();
        int nano = now.getNano();
        return epochSecond * SEC2USEC + nano / USEC2NANO + SQUEAK_EPOCH_DELTA_MICROSECONDS;
    }
}
