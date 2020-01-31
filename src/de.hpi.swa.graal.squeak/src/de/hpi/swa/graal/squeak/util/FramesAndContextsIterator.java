package de.hpi.swa.graal.squeak.util;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameUtil;

import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.FrameMarker;
import de.hpi.swa.graal.squeak.model.NilObject;

public class FramesAndContextsIterator {

    public static final FramesAndContextsIterator Empty = new FramesAndContextsIterator();

    private Predicate<ContextObject> contextFilter;
    private BiPredicate<Boolean, CompiledCodeObject> frameFilter;
    private Consumer<ContextObject> contextVisitor;
    private BiConsumer<Frame, CompiledCodeObject> frameVisitor;

    public FramesAndContextsIterator(final BiConsumer<Frame, CompiledCodeObject> frameVisitor, final Consumer<ContextObject> contextVisitor) {
        this.contextVisitor = contextVisitor;
        this.frameVisitor = frameVisitor;
    }

    public FramesAndContextsIterator(final BiPredicate<Boolean, CompiledCodeObject> frameFilter, final Predicate<ContextObject> contextFilter) {
        this.contextFilter = contextFilter;
        this.frameFilter = frameFilter;
    }

    private FramesAndContextsIterator() {
        super();
    }

    public AbstractSqueakObject scanFor(final ContextObject start, final AbstractSqueakObject end, final AbstractSqueakObject endReturnValue) {
        Object current = start.getFrameSender();
        if (current == end) {
            return endReturnValue;
        }
        if (current == NilObject.SINGLETON) {
            return NilObject.SINGLETON;
        }
        while (!(current instanceof FrameMarker)) {
            final ContextObject currentContext = (ContextObject) current;
            if (contextFilter != null && contextFilter.test(currentContext)) {
                return currentContext;
            }
            final Object sender = currentContext.getFrameSender();
            if (contextVisitor != null) {
                contextVisitor.accept(currentContext);
            }
            current = sender;
            if (current == end) {
                return endReturnValue;
            }
            if (current == NilObject.SINGLETON) {
                return NilObject.SINGLETON;
            }
        }
        if (end instanceof ContextObject) {
            if (current != null && current == ((ContextObject) end).getFrameMarker()) {
                return endReturnValue;
            }
        }
        return scanFor((FrameMarker) current, end, endReturnValue);
    }

    @TruffleBoundary
    public AbstractSqueakObject scanFor(final FrameMarker start, final AbstractSqueakObject end, final AbstractSqueakObject endReturnValue) {
        LogUtils.ITERATE_FRAMES.fine(() -> "Inside FramesAndContextsIterator.scanFor with args: " + start + ", " + end);
        final Object[] lastSender = new Object[1];
        final boolean[] foundMyself = {false};
        final ContextObject result = Truffle.getRuntime().iterateFrames((frameInstance) -> {
            final Frame currentFrame = frameInstance.getFrame(FrameInstance.FrameAccess.READ_WRITE);
            final Object[] arguments = currentFrame.getArguments();
            if (arguments.length < 3) {
                return null;
            }
            final Object maybeMethod = arguments[0];
            if (!(maybeMethod instanceof CompiledMethodObject)) {
                return null;
            }
            final BlockClosureObject closure = (BlockClosureObject) arguments[2];
            final CompiledCodeObject currentCode = closure != null ? closure.getCompiledBlock() : (CompiledMethodObject) maybeMethod;
            Object currentMarker = null;
            if (!foundMyself[0] && null != (currentMarker = FrameUtil.getObjectSafe(currentFrame, currentCode.getThisMarkerSlot())) && start == currentMarker) {
                if (lastSender[0] instanceof FrameMarker) {
                    assert lastSender[0] == currentMarker;
                }
                foundMyself[0] = true;
            }
            ContextObject currentContext = null;
            if (!frameInstance.isVirtualFrame()) {
                currentContext = (ContextObject) FrameUtil.getObjectSafe(currentFrame, currentCode.getThisContextSlot());
                if (currentContext == end) {
                    return currentContext;
                }
            }
            if (frameFilter != null && frameFilter.test(foundMyself[0], currentCode)) {
                if (currentContext == null) {
                    currentContext = ContextObject.create(currentFrame.materialize(), currentCode);
                    currentContext.setProcess(currentCode.image.getActiveProcessSlow());
                }
                return currentContext;
            }
            lastSender[0] = arguments[1];
            if (foundMyself[0] && frameVisitor != null) {
                frameVisitor.accept(currentFrame, currentCode);
            }
            return null;
        });
        if (result != null) {
            // end was found, but it is only valid if start was found as well
            return foundMyself[0] ? result == end ? endReturnValue : result : NilObject.SINGLETON;
        }
        if (lastSender[0] == null || lastSender[0] == NilObject.SINGLETON) {
            // the stack iteration ended, but end was not found or it was nil
            return NilObject.SINGLETON;
        }
        assert !(lastSender[0] instanceof FrameMarker) : "Frame iteration ended with a frame marker!";
        if (foundMyself[0] && lastSender[0] == end) {
            return endReturnValue;
        } else {
            Object current = ((ContextObject) lastSender[0]).getFrameSender();
            if (current == end) {
                return endReturnValue;
            }
            if (current == NilObject.SINGLETON) {
                return NilObject.SINGLETON;
            }
            while (!(current instanceof FrameMarker)) {
                final ContextObject currentContext = (ContextObject) current;
                if (contextFilter != null && contextFilter.test(currentContext)) {
                    return currentContext;
                }
                final Object sender = currentContext.getFrameSender();
                if (contextVisitor != null) {
                    contextVisitor.accept(currentContext);
                }
                current = sender;
                if (current == end) {
                    return endReturnValue;
                }
                if (current == NilObject.SINGLETON) {
                    return NilObject.SINGLETON;
                }
            }
            if (current != null && end instanceof ContextObject) {
                if (current == ((ContextObject) end).getFrameMarker()) {
                    return endReturnValue;
                }
            }
            return NilObject.SINGLETON;
        }
    }
}