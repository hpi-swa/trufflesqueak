package de.hpi.swa.graal.squeak.nodes;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;

import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.exceptions.ProcessSwitch;
import de.hpi.swa.graal.squeak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.graal.squeak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.graal.squeak.exceptions.Returns.TopLevelReturn;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;

public final class ExecuteTopLevelContextNode extends RootNode {
    private final SqueakImageContext image;
    private final ContextObject initialContext;
    private final boolean needsShutdown;

    @Child private ExecuteContextNode executeContextNode;
    @Child private UnwindContextChainNode unwindContextChainNode;
    @CompilationFinal private static final ExecutorService executor = Executors.newCachedThreadPool();
    @CompilationFinal public static final Map<ContextObject, Thread> suspendedContextThreads = new ConcurrentHashMap<>();

    public static ExecuteTopLevelContextNode create(final SqueakLanguage language, final ContextObject context, final boolean needsShutdown) {
        return new ExecuteTopLevelContextNode(language, context, context.getBlockOrMethod(), needsShutdown);
    }

    private ExecuteTopLevelContextNode(final SqueakLanguage language, final ContextObject context, final CompiledCodeObject code, final boolean needsShutdown) {
        super(language, code.getFrameDescriptor());
        image = code.image;
        initialContext = context;
        this.needsShutdown = needsShutdown;
        unwindContextChainNode = UnwindContextChainNode.create(image);
        SqueakImageContext.nextContext = context;
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        try {
            executeThreads();
        } catch (TopLevelReturn e) {
            return e.getReturnValue();
        } finally {
            if (needsShutdown) {
                CompilerDirectives.transferToInterpreter();
                image.interrupt.shutdown();
                if (image.hasDisplay()) {
                    image.getDisplay().close();
                }
            }
        }
        throw new SqueakException("Top level context did not return");
    }

    private final class SqueakProcess implements Runnable {
        @CompilationFinal private final ContextObject context;

        SqueakProcess(final ContextObject context) {
            this.context = context;
        }

        public void run() {
            ContextObject activeContext = context;
            while (true) {
                CompilerDirectives.transferToInterpreter();
                final AbstractSqueakObject sender = activeContext.getSender();
                try {
                    MaterializeContextOnMethodExitNode.reset();
                    final CompiledCodeObject code = activeContext.getBlockOrMethod();
                    // FIXME: do not create node here?
                    if (executeContextNode == null) {
                        executeContextNode = insert(ExecuteContextNode.create(code));
                    } else {
                        executeContextNode.replace(ExecuteContextNode.create(code));
                    }
                    // doIt: activeContext.printSqStackTrace();
                    final Object result = executeContextNode.executeContext(activeContext.getTruffleFrame(), activeContext);
                    activeContext = unwindContextChainNode.executeUnwind(sender, sender, result);
                    image.traceProcessSwitches("Local Return on top-level (sender:", sender, ", new context: ", activeContext, ")");
                } catch (NonLocalReturn nlr) {
                    final AbstractSqueakObject target = (AbstractSqueakObject) nlr.getTargetContextOrMarker();
                    activeContext = unwindContextChainNode.executeUnwind(sender, target, nlr.getReturnValue());
                    image.traceProcessSwitches("Non Local Return on top-level, new context is", activeContext);
                } catch (NonVirtualReturn nvr) {
                    activeContext = unwindContextChainNode.executeUnwind(nvr.getCurrentContext(), nvr.getTargetContext(), nvr.getReturnValue());
                    image.traceProcessSwitches("Non Virtual Return on top-level, new context is", activeContext);
                } catch (TopLevelReturn tlr) {
                    if (Thread.currentThread() == image.rootJavaThread) {
                        throw tlr;
                    } else {
                        SqueakImageContext.mainThreadSuspended = false;
                        image.rootJavaThread.notify();
                        return;
                    }
                }
            }
        }
    }

    private void executeThreads() {
        Thread thread;
        while (true) {
            final ContextObject nextContext = SqueakImageContext.nextContext;
            thread = suspendedContextThreads.remove(nextContext);
            if (thread == null) {
                final Thread newThread = image.env.createThread(new SqueakProcess(nextContext));
                // suspendedContextThreads.put(SqueakImageContext.nextContext, newThread);
                image.printToStdErr("New thread");
                newThread.start();
            } else {
                synchronized (nextContext) {
                    SqueakImageContext.workerThreadSuspended = false;
// image.printToStdErr("Resuming existing thread");
                    nextContext.notify();
                }
            }
            synchronized (image.rootJavaThread) {
                SqueakImageContext.mainThreadSuspended = true;
                while (SqueakImageContext.mainThreadSuspended) {
                    try {
                        image.printToStdErr("Waiting for thread to complete executing:", nextContext);
                        image.rootJavaThread.wait(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
// image.printToStdErr("Done waiting...");
            }
        }
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public boolean isCloningAllowed() {
        return true;
    }
}
