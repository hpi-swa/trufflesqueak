/*
 * Copyright (c) 2023-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2023-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins.ffi;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
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
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.NFIUtils;

public final class PrimExternalCallNode extends AbstractPrimitiveNode
                implements Primitive0, Primitive1, Primitive2, Primitive3, Primitive4, Primitive5, Primitive6, Primitive7, Primitive8, Primitive9, Primitive10, Primitive11 {
    private final Object functionSymbol;
    private final InteropLibrary functionInteropLibrary;
    private final int numReceiverAndArguments;

    public PrimExternalCallNode(final Object functionSymbol, final InteropLibrary functionInteropLibrary, final int numReceiverAndArguments) {
        this.functionSymbol = functionSymbol;
        this.functionInteropLibrary = functionInteropLibrary;
        this.numReceiverAndArguments = numReceiverAndArguments;
    }

    @Override
    public boolean needsFrame() {
        return true;
    }

    public static PrimExternalCallNode load(final String moduleName, final String functionName, final int numReceiverAndArguments) {
        final SqueakImageContext context = SqueakImageContext.getSlow();
        final Object moduleLibrary = lookupModuleLibrary(context, moduleName);
        if (moduleLibrary == null) {
            return null; // module not found
        }
        try {
            final Object functionSymbol = NFIUtils.loadMember(context, moduleLibrary, functionName, "():SINT64");
            final InteropLibrary functionInteropLibrary = NFIUtils.getInteropLibrary(functionSymbol);
            return new PrimExternalCallNode(functionSymbol, functionInteropLibrary, numReceiverAndArguments);
        } catch (UnknownIdentifierException e) {
            return null; // function not found
        }
    }

    private static Object lookupModuleLibrary(final SqueakImageContext context, final String moduleName) {
        final Object moduleLibrary = context.loadedLibraries.computeIfAbsent(moduleName, (String s) -> {
            if (context.loadedLibraries.containsKey(moduleName)) {
                // if moduleName was associated with null
                return null;
            }
            final Object library;
            try {
                library = NFIUtils.loadLibrary(context, moduleName, "{ setInterpreter(POINTER):SINT64; }");
            } catch (AbstractTruffleException e) {
                if (e.getMessage().equals("Unknown identifier: setInterpreter")) {
                    // module has no setInterpreter, cannot be loaded
                    return null;
                }
                throw e;
            }
            if (library == null) {
                return null;
            }
            try {
                // TODO: also call shutdownModule():SINT64 at some point
                final Object initialiseModuleSymbol = NFIUtils.loadMember(context, library, "initialiseModule", "():SINT64");
                final InteropLibrary initialiseModuleInteropLibrary = NFIUtils.getInteropLibrary(initialiseModuleSymbol);
                initialiseModuleInteropLibrary.execute(initialiseModuleSymbol);
            } catch (UnknownIdentifierException e) {
                // module has no initializer, ignore
            } catch (UnsupportedTypeException | ArityException | UnsupportedMessageException e) {
                throw CompilerDirectives.shouldNotReachHere(e);
            }
            try {
                final InteropLibrary moduleInteropLibrary = NFIUtils.getInteropLibrary(library);
                moduleInteropLibrary.invokeMember(library, "setInterpreter", context.getInterpreterProxy(null, 0).getPointer());
            } catch (UnsupportedMessageException | ArityException | UnsupportedTypeException | UnknownIdentifierException e) {
                throw CompilerDirectives.shouldNotReachHere(e);
            }
            return library;
        });
        // computeIfAbsent would not put null value
        context.loadedLibraries.putIfAbsent(moduleName, moduleLibrary);
        return moduleLibrary;
    }

    @Override
    public Object execute(final VirtualFrame frame, final Object receiver) {
        return call(frame, receiver);
    }

    @Override
    public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1) {
        return call(frame, receiver, arg1);
    }

    @Override
    public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2) {
        return call(frame, receiver, arg1, arg2);
    }

    @Override
    public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3) {
        return call(frame, receiver, arg1, arg2, arg3);
    }

    @Override
    public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4) {
        return call(frame, receiver, arg1, arg2, arg3, arg4);
    }

    @Override
    public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5) {
        return call(frame, receiver, arg1, arg2, arg3, arg4, arg5);
    }

    @Override
    public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6) {
        return call(frame, receiver, arg1, arg2, arg3, arg4, arg5, arg6);
    }

    @Override
    public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6,
                    final Object arg7) {
        return call(frame, receiver, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
    }

    @Override
    public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6,
                    final Object arg7, final Object arg8) {
        return call(frame, receiver, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
    }

    @Override
    public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6,
                    final Object arg7, final Object arg8, final Object arg9) {
        return call(frame, receiver, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9);
    }

    @Override
    public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6,
                    final Object arg7, final Object arg8, final Object arg9, final Object arg10) {
        return call(frame, receiver, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10);
    }

    @Override
    public Object execute(final VirtualFrame frame, final Object receiver, final Object arg1, final Object arg2, final Object arg3, final Object arg4, final Object arg5, final Object arg6,
                    final Object arg7, final Object arg8, final Object arg9, final Object arg10, final Object arg11) {
        return call(frame, receiver, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11);
    }

    private Object call(final VirtualFrame frame, final Object... receiverAndArguments) {
        return doExternalCall(frame.materialize(), receiverAndArguments);
    }

    @TruffleBoundary
    private Object doExternalCall(final MaterializedFrame frame, final Object[] receiverAndArguments) {
        restoreStackValues(frame, receiverAndArguments);
        final InterpreterProxy interpreterProxy = getContext().getInterpreterProxy(frame, numReceiverAndArguments);
        try {
            /*
             * return value is unused, the actual return value is pushed onto the stack (see below)
             */
            functionInteropLibrary.execute(functionSymbol);
            /*
             * The return value is pushed onto the stack by the plugin via the InterpreterProxy, but
             * TruffleSqueak expects the return value to be returned by this function
             * (AbstractSendNode.executeVoid). Pop the return value and return it.
             */
            final int stackPointer = FrameAccess.getStackPointer(frame) - 1;
            final Object returnValue = FrameAccess.getStackValue(frame, stackPointer, FrameAccess.getNumArguments(frame));
            FrameAccess.setStackPointer(frame, stackPointer);
            final long failReason = interpreterProxy.failed();
            if (failReason == 0) {
                return returnValue;
            } else {
                throw PrimitiveFailed.andTransferToInterpreter((int) failReason);
            }
        } catch (UnsupportedMessageException | ArityException | UnsupportedTypeException e) {
            throw CompilerDirectives.shouldNotReachHere(e);
        } finally {
            interpreterProxy.postPrimitiveCleanups();
        }
    }

    private void restoreStackValues(final MaterializedFrame frame, final Object[] receiverAndArguments) {
        /*
         * A message send (AbstractSendNode.executeVoid) will pop and potentially clear stack
         * values, and decrement the stack pointer by numReceiverAndArguments before transferring
         * control. We need the values back on the stack and the stack pointer to point at the last
         * argument, since the C code expects that. Therefore, we restore the stack values and
         * pointer here.
         */
        assert numReceiverAndArguments == receiverAndArguments.length;
        final int initialSP;
        if (!FrameAccess.hasClosure(frame)) {
            initialSP = FrameAccess.getCodeObject(frame).getNumTemps();
        } else {
            initialSP = FrameAccess.getClosure(frame).getInitialSP();
        }
        final int stackPointer = FrameAccess.getStackPointer(frame);
        for (int i = 0; i < numReceiverAndArguments; i++) {
            final int stackIndex = stackPointer + i;
            /* Only values stored above initialSP are cleared */
            if (stackIndex >= initialSP) {
                FrameAccess.setStackSlot(frame, stackIndex, receiverAndArguments[i]);
            }
        }
        FrameAccess.setStackPointer(frame, stackPointer + numReceiverAndArguments);
    }
}
