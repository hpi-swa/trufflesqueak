package de.hpi.swa.trufflesqueak.nodes.plugins.ffi;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.NFIUtils;

import java.util.HashMap;
import java.util.Map;

public class PrimExternalCallNode extends AbstractPrimitiveNode {
    final String moduleName;
    final String functionName;
    final int numReceiverAndArguments;
    static Map<String, Object> loadedLibraries = new HashMap<>();

    public PrimExternalCallNode(String moduleName, String functionName, int numReceiverAndArguments) {
        this.moduleName = moduleName;
        this.functionName = functionName;
        this.numReceiverAndArguments = numReceiverAndArguments;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final Object uuidPlugin = loadedLibraries.computeIfAbsent(moduleName, (String s) ->
                NFIUtils.loadLibrary(getContext(), moduleName, "{ " +
                        // TODO, see below
                        //"initialiseModule():SINT64; " +
                        "setInterpreter(POINTER):SINT64; " +
                        // Currently not called, since plugins are never unloaded
                        //"shutdownModule():SINT64; " +
                        " }"));
        final InteropLibrary uuidPluginLibrary = NFIUtils.getInteropLibrary(uuidPlugin);
        InterpreterProxy interpreterProxy = null;
        try {
            interpreterProxy = InterpreterProxy.instanceFor(getContext(), frame.materialize(), numReceiverAndArguments);

            // A send (AbstractSendNode.executeVoid) will decrement the stack pointer by numReceiverAndArguments
            // before transferring control. We need the stack pointer to point at the last argument,
            // since the C code expects that. Therefore, we undo the decrement operation here.
            FrameAccess.setStackPointer(frame, FrameAccess.getStackPointer(frame) + numReceiverAndArguments);

            // TODO: Only call when the plugin actually defines the function
            //uuidPluginLibrary.invokeMember(uuidPlugin, "initialiseModule");

            uuidPluginLibrary.invokeMember(uuidPlugin, "setInterpreter", interpreterProxy.getPointer());

            final Object functionSymbol = NFIUtils.loadMember(getContext(), uuidPlugin, functionName, "():SINT64");
            final InteropLibrary functionInteropLibrary = NFIUtils.getInteropLibrary(functionSymbol);
            // return value is unused, the actual return value is pushed onto the stack (see below)
            functionInteropLibrary.execute(functionSymbol);

            // The return value is pushed onto the stack by the plugin via the InterpreterProxy, but TruffleSqueak
            // expects the return value to be returned by this function (AbstractSendNode.executeVoid).
            // Pop the return value and return it.
            final Object returnValue = FrameAccess.getStackValue(frame, FrameAccess.getStackPointer(frame) - 1, FrameAccess.getNumArguments(frame));
            FrameAccess.setStackPointer(frame, FrameAccess.getStackPointer(frame) - 1);
            return returnValue;
        } catch (Exception e) {
            // for debugging purposes
            e.printStackTrace(System.err);
            throw PrimitiveFailed.GENERIC_ERROR;
        } finally {
            if (interpreterProxy != null) {
                interpreterProxy.postPrimitiveCleanups();
            }
        }
    }

    @Override
    public Object executeWithArguments(VirtualFrame frame, Object... receiverAndArguments) {
        // arguments are handled via manipulation of the stack pointer, see above
        return execute(frame);
    }
}
