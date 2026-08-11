/*
 * Copyright (c) 2023-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2023-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.lang.invoke.MethodHandle;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.interpreterproxy.InterpreterProxy;
import de.hpi.swa.trufflesqueak.interpreterproxy.InterpreterProxySupport;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive0;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive10;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive11;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive2;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive3;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive4;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive5;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive6;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive7;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive8;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive9;

public final class PrimExternalCallNode extends AbstractPrimitiveNode
                implements Primitive0, Primitive1, Primitive2, Primitive3, Primitive4, Primitive5, Primitive6, Primitive7, Primitive8, Primitive9, Primitive10, Primitive11 {
    private final MethodHandle handle;
    private final int numReceiverAndArguments;

    public PrimExternalCallNode(final MethodHandle handle, final int numReceiverAndArguments) {
        this.handle = handle;
        this.numReceiverAndArguments = numReceiverAndArguments;
    }

    public static PrimExternalCallNode load(final String moduleName, final String functionName, final int numReceiverAndArguments) {
        final MethodHandle handle = InterpreterProxySupport.loadMethodHandle(moduleName, functionName);
        if (handle == null) {
            return null; // module not found
        }
        return new PrimExternalCallNode(handle, numReceiverAndArguments);
    }

    @Override
    public Object execute(final VirtualFrame frame, final Object receiver) {
        return call(receiver);
    }

    @Override
    public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1) {
        return call(receiver, arg1);
    }

    @Override
    public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2) {
        return call(receiver, arg1, arg2);
    }

    @Override
    public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3) {
        return call(receiver, arg1, arg2, arg3);
    }

    @Override
    public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4) {
        return call(receiver, arg1, arg2, arg3, arg4);
    }

    @Override
    public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5) {
        return call(receiver, arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6) {
        return call(receiver, arg1, arg2, arg3, arg4, arg5, arg6);
    }

    @Override
    public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6,
                    final Object arg7) {
        return call(receiver, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
    }

    @Override
    public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6,
                    final Object arg7, final Object arg8) {
        return call(receiver, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
    }

    @Override
    public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6,
                    final Object arg7, final Object arg8, final Object arg9) {
        return call(receiver, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9);
    }

    @Override
    public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6,
                    final Object arg7, final Object arg8, final Object arg9, final Object arg10) {
        return call(receiver, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10);
    }

    @Override
    public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6,
                    final Object arg7, final Object arg8, final Object arg9, final Object arg10, final Object arg11) {
        return call(receiver, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11);
    }

    private Object call(final Object... receiverAndArguments) {
        return doExternalCall(receiverAndArguments);
    }

    @TruffleBoundary
    private Object doExternalCall(final Object[] receiverAndArguments) {
        assert receiverAndArguments.length == numReceiverAndArguments;
        /* InterpreterProxy uses receiverAndArguments as the stack. */
        try (InterpreterProxy interpreterProxy = InterpreterProxy.SINGLETON.newInvocation(getContext(), receiverAndArguments)) {
            /*
             * return value is unused, the actual return value is pushed onto the stack (see below)
             */
            if ((long) handle.invoke() != 0) {
                throw CompilerDirectives.shouldNotReachHere();
            }
            /*
             * The return value is pushed onto the stack by the plugin via the InterpreterProxy, but
             * TruffleSqueak expects the return value to be returned by this function
             * (AbstractSendNode.executeVoid). Fetch the return value and return it.
             */
            final Object returnValue = interpreterProxy.getReturnValue();
            final long failReason = interpreterProxy.failed();
            if (failReason == 0) {
                return returnValue;
            } else {
                throw PrimitiveFailed.andTransferToInterpreter((int) failReason);
            }
        } catch (Throwable e) {
            throw CompilerDirectives.shouldNotReachHere(e);
        }
    }
}
