/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.util;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithCode;
import de.hpi.swa.graal.squeak.nodes.process.SignalSemaphoreNode;

public abstract class InterruptByUserHandlerNode extends AbstractNodeWithCode {

    protected InterruptByUserHandlerNode(final CompiledCodeObject code) {
        super(code);
    }

    public static InterruptByUserHandlerNode create(final CompiledCodeObject code) {
        return InterruptByUserHandlerNodeGen.create(code);
    }

    public abstract void executeCheckForUserInterrupts(VirtualFrame frame);

    @Specialization
    protected final void executeCheckForUserInterrupts(final VirtualFrame frame,
                    @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
        /* Ensure check is only performed in interpreter or once per compilation unit. */
        if (CompilerDirectives.inCompiledCode() && !CompilerDirectives.inCompilationRoot()) {
            return;
        }
        if (image.interrupt.interruptPending()) {
            /* Exclude user interrupt from compilation, it's a rare, special case. */
            CompilerDirectives.transferToInterpreter();
            image.interrupt.interruptPending = false; // reset interrupt flag
            SignalSemaphoreNode.create(code).executeSignal(frame, image.interrupt.getInterruptSemaphore());
        }
    }
}
