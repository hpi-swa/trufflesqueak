package de.hpi.swa.graal.squeak.image;

import java.awt.Dimension;
import java.awt.Point;
import java.io.File;
import java.io.PrintWriter;
import java.math.BigInteger;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;
import com.oracle.truffle.api.profiles.ValueProfile;

import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.io.SqueakDisplay;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.ASSOCIATION;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.POINT;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.ExecuteTopLevelContextNode;
import de.hpi.swa.graal.squeak.nodes.process.GetActiveProcessNode;
import de.hpi.swa.graal.squeak.util.ArrayUtils;
import de.hpi.swa.graal.squeak.util.FrameAccess;
import de.hpi.swa.graal.squeak.util.InterruptHandlerNode;
import de.hpi.swa.graal.squeak.util.OSDetector;
import de.hpi.swa.graal.squeak.util.SqueakConfig;

public final class SqueakImageContext {
    // Special objects
    public final NilObject nil = new NilObject(this);
    public final boolean sqFalse = false;
    public final boolean sqTrue = true;
    public final PointersObject specialObjectsArray = new PointersObject(this);
    public final PointersObject schedulerAssociation = new PointersObject(this);
    public final ClassObject characterClass = new ClassObject(this);
    public final ClassObject smallIntegerClass = new ClassObject(this);
    public final ClassObject arrayClass = new ClassObject(this);
    public final PointersObject smalltalk = new PointersObject(this);
    public final NativeObject doesNotUnderstand = NativeObject.newNativeBytes(this, null, 0);
    public final PointersObject specialSelectors = new PointersObject(this);
    public final NativeObject mustBeBoolean = NativeObject.newNativeBytes(this, null, 0);
    public final ClassObject metaclass = new ClassObject(this);
    public final ClassObject methodContextClass = new ClassObject(this);
    public final ClassObject nilClass = new ClassObject(this);
    public final ClassObject trueClass = new ClassObject(this);
    public final ClassObject falseClass = new ClassObject(this);
    public final ClassObject stringClass = new ClassObject(this);
    public final ClassObject compiledMethodClass = new ClassObject(this);
    public final ClassObject blockClosureClass = new ClassObject(this);
    public final ClassObject largePositiveIntegerClass = new ClassObject(this);
    public final ClassObject largeNegativeIntegerClass = new ClassObject(this);
    public final ClassObject floatClass = new ClassObject(this);

    private final SqueakLanguage language;
    private final PrintWriter output;
    private final PrintWriter error;
    public final SqueakLanguage.Env env;

    // Special selectors
    public final NativeObject plus = new NativeObject(this);
    public final NativeObject minus = new NativeObject(this);
    public final NativeObject lt = new NativeObject(this);
    public final NativeObject gt = new NativeObject(this);
    public final NativeObject le = new NativeObject(this);
    public final NativeObject ge = new NativeObject(this);
    public final NativeObject eq = new NativeObject(this);
    public final NativeObject ne = new NativeObject(this);
    public final NativeObject times = new NativeObject(this);
    public final NativeObject divide = new NativeObject(this);
    public final NativeObject modulo = new NativeObject(this);
    public final NativeObject pointAt = new NativeObject(this);
    public final NativeObject bitShift = new NativeObject(this);
    public final NativeObject floorDivide = new NativeObject(this);
    public final NativeObject bitAnd = new NativeObject(this);
    public final NativeObject bitOr = new NativeObject(this);
    public final NativeObject at = new NativeObject(this);
    public final NativeObject atput = new NativeObject(this);
    public final NativeObject sqSize = new NativeObject(this);
    public final NativeObject next = new NativeObject(this);
    public final NativeObject nextPut = new NativeObject(this);
    public final NativeObject atEnd = new NativeObject(this);
    public final NativeObject equivalent = new NativeObject(this);
    public final NativeObject klass = new NativeObject(this);
    public final NativeObject blockCopy = new NativeObject(this);
    public final NativeObject sqValue = new NativeObject(this);
    public final NativeObject valueWithArg = new NativeObject(this);
    public final NativeObject sqDo = new NativeObject(this);
    public final NativeObject sqNew = new NativeObject(this);
    public final NativeObject newWithArg = new NativeObject(this);
    public final NativeObject x = new NativeObject(this);
    public final NativeObject y = new NativeObject(this);

    @CompilationFinal(dimensions = 1) public final NativeObject[] specialSelectorsArray = new NativeObject[]{
                    plus, minus, lt, gt, le, ge, eq, ne, times, divide, modulo, pointAt, bitShift,
                    floorDivide, bitAnd, bitOr, at, atput, sqSize, next, nextPut, atEnd, equivalent,
                    klass, blockCopy, sqValue, valueWithArg, sqDo, sqNew, newWithArg, x, y
    };

    @CompilationFinal(dimensions = 1) public final int[] specialSelectorsNumArgs = new int[]{
                    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 0, 0, 1, 0, 1, 0, 1, 0, 1, 1, 0, 1, 0, 0
    };

    public final SqueakConfig config;
    public final SqueakImageFlags flags = new SqueakImageFlags();
    public final OSDetector os = new OSDetector();
    public final InterruptHandlerNode interrupt;
    public final long startUpMillis = System.currentTimeMillis();

    private final SqueakDisplay display;
    private final ValueProfile displayProfile = ValueProfile.createClassProfile();

    public static final byte[] AS_SYMBOL_SELECTOR_NAME = "asSymbol".getBytes(); // for testing
    @CompilationFinal private NativeObject asSymbolSelector = null; // for testing
    @CompilationFinal private NativeObject simulatePrimitiveArgsSelector = null;
    @CompilationFinal private PointersObject scheduler = null;

    public SqueakImageContext(final SqueakLanguage squeakLanguage, final SqueakLanguage.Env environ,
                    final PrintWriter out, final PrintWriter err) {
        language = squeakLanguage;
        env = environ;
        output = out;
        error = err;
        final String[] applicationArguments = env.getApplicationArguments();
        config = new SqueakConfig(applicationArguments);
        display = SqueakDisplay.create(this, config.isCustomContext() || config.isTesting());
        interrupt = InterruptHandlerNode.create(this, config);
    }

    public CallTarget getActiveContext() {
        // TODO: maybe there is a better way to do the below
        final PointersObject activeProcess = GetActiveProcessNode.create(this).executeGet();
        final ContextObject activeContext = (ContextObject) activeProcess.at0(PROCESS.SUSPENDED_CONTEXT);
        activeProcess.atput0(PROCESS.SUSPENDED_CONTEXT, nil);
        output.println("Resuming active context for " + activeContext.getMethod() + "...");
        return Truffle.getRuntime().createCallTarget(ExecuteTopLevelContextNode.create(language, activeContext));
    }

    public CallTarget getCustomContext() {
        final Object receiver = config.getReceiver();
        final String selector = config.getSelector();
        final ClassObject receiverClass = receiver instanceof Long ? smallIntegerClass : nilClass;
        final CompiledMethodObject lookupResult = (CompiledMethodObject) receiverClass.lookup(selector);
        if (lookupResult.getCompiledInSelector() == doesNotUnderstand) {
            throw new SqueakException(receiver, ">>", selector, "could not be found!");
        }
        final ContextObject customContext = ContextObject.create(this, lookupResult.frameSize());
        customContext.atput0(CONTEXT.METHOD, lookupResult);
        customContext.atput0(CONTEXT.INSTRUCTION_POINTER, (long) lookupResult.getInitialPC());
        customContext.atput0(CONTEXT.RECEIVER, receiver);
        customContext.atput0(CONTEXT.STACKPOINTER, 1L);
        customContext.atput0(CONTEXT.CLOSURE_OR_NIL, nil);
        customContext.setSender(nil);
        // if there were arguments, they would need to be pushed before the temps
        final long numTemps = lookupResult.getNumTemps() - lookupResult.getNumArgs();
        for (int i = 0; i < numTemps; i++) {
            customContext.push(nil);
        }

        output.println("Starting to evaluate " + receiver + " >> " + selector + "...");
        return Truffle.getRuntime().createCallTarget(ExecuteTopLevelContextNode.create(getLanguage(), customContext));
    }

    public PrintWriter getOutput() {
        return output;
    }

    public PrintWriter getError() {
        return error;
    }

    public SqueakLanguage getLanguage() {
        return language;
    }

    public SqueakDisplay getDisplay() {
        return displayProfile.profile(display);
    }

    public NativeObject getAsSymbolSelector() {
        return asSymbolSelector;
    }

    public void setAsSymbolSelector(final NativeObject asSymbolSelector) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        this.asSymbolSelector = asSymbolSelector;
    }

    public NativeObject getSimulatePrimitiveArgsSelector() {
        return simulatePrimitiveArgsSelector;
    }

    public void setSimulatePrimitiveArgsSelector(final NativeObject simulatePrimitiveArgsSelector) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        this.simulatePrimitiveArgsSelector = simulatePrimitiveArgsSelector;
    }

    public PointersObject getScheduler() {
        if (scheduler == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            final PointersObject association = (PointersObject) specialObjectsArray.at0(SPECIAL_OBJECT_INDEX.SchedulerAssociation);
            scheduler = (PointersObject) association.at0(ASSOCIATION.VALUE);
        }
        return scheduler;
    }

    public Object wrap(final Object obj) {
        if (obj == null) {
            return nil;
        } else if (obj instanceof Boolean) {
            return wrap((boolean) obj);
        } else if (obj instanceof Integer) {
            return wrap((long) Long.valueOf((Integer) obj));
        } else if (obj instanceof Long) {
            return wrap((long) obj);
        } else if (obj instanceof Double) {
            return wrap((double) obj);
        } else if (obj instanceof BigInteger) {
            return wrap((BigInteger) obj);
        } else if (obj instanceof String) {
            return wrap((String) obj);
        } else if (obj instanceof Character) {
            return wrap((char) obj);
        } else if (obj instanceof Object[]) {
            return wrap((Object[]) obj);
        } else if (obj instanceof Point) {
            return wrap((Point) obj);
        } else if (obj instanceof Dimension) {
            return wrap((Dimension) obj);
        }
        throw new SqueakException("Unsupported value to wrap:", obj);
    }

    public Object wrap(final boolean value) {
        return value ? sqTrue : sqFalse;
    }

    @SuppressWarnings("static-method")
    public long wrap(final long l) {
        return l;
    }

    public AbstractSqueakObject wrap(final BigInteger i) {
        return new LargeIntegerObject(this, i);
    }

    public FloatObject wrap(final double value) {
        return new FloatObject(this, value);
    }

    @TruffleBoundary
    public NativeObject wrap(final String s) {
        return NativeObject.newNativeBytes(this, stringClass, s.getBytes());
    }

    public NativeObject wrap(final byte[] bytes) {
        return NativeObject.newNativeBytes(this, (ClassObject) specialObjectsArray.at0(SPECIAL_OBJECT_INDEX.ClassByteArray), bytes);
    }

    public static char wrap(final char character) {
        return character;
    }

    @TruffleBoundary
    public PointersObject wrap(final Object... elements) {
        CompilerAsserts.neverPartOfCompilation("SqueakImageContext#wrap");
        final Object[] wrappedElements = new Object[elements.length];
        for (int i = 0; i < elements.length; i++) {
            wrappedElements[i] = wrap(elements[i]);
        }
        return newList(wrappedElements);
    }

    public PointersObject wrap(final Point point) {
        return newPoint((long) point.getX(), (long) point.getY());
    }

    public PointersObject wrap(final Dimension dimension) {
        return newPoint((long) dimension.getWidth(), (long) dimension.getHeight());
    }

    public PointersObject newList(final Object[] elements) {
        return new PointersObject(this, arrayClass, elements);
    }

    public PointersObject newListWith(final Object... elements) {
        return newList(elements);
    }

    public PointersObject newPoint(final Object xPos, final Object yPos) {
        final ClassObject pointClass = (ClassObject) specialObjectsArray.at0(SPECIAL_OBJECT_INDEX.ClassPoint);
        final PointersObject newPoint = (PointersObject) pointClass.newInstance();
        newPoint.atput0(POINT.X, xPos);
        newPoint.atput0(POINT.Y, yPos);
        return newPoint;
    }

    public NativeObject newSymbol(final String value) {
        return NativeObject.newNativeBytes(this, doesNotUnderstand.getSqClass(), value.getBytes());
    }

    public void registerSemaphore(final AbstractSqueakObject semaphore, final long index) {
        specialObjectsArray.atput0(index, semaphore.isSemaphore() ? semaphore : nil);
    }

    public String imageRelativeFilePathFor(final String fileName) {
        return config.getImageDirectory() + File.separator + fileName;
    }

    public void trace(final Object... arguments) {
        if (config.isTracing()) {
            printToStdOut(arguments);
        }
    }

    public void traceVerbose(final Object... arguments) {
        if (config.isTracing() && config.isVerbose()) {
            printToStdOut(arguments);
        }
    }

    @TruffleBoundary
    public void printToStdOut(final Object... arguments) {
        getOutput().println(ArrayUtils.toJoinedString(" ", arguments));
    }

    @TruffleBoundary
    public void printToStdErr(final Object... arguments) {
        getError().println(ArrayUtils.toJoinedString(" ", arguments));
    }

    /*
     * Helper function for debugging purposes.
     */
    @TruffleBoundary
    public void printSqStackTrace() {
        CompilerDirectives.transferToInterpreter();
        final boolean isTravisBuild = System.getenv().containsKey("TRAVIS");
        final int[] depth = new int[1];
        final Object[] lastSender = new Object[]{null};
        getError().println("== Squeak stack trace ===========================================================");
        Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<Object>() {

            @Override
            public Object visitFrame(final FrameInstance frameInstance) {
                if (depth[0]++ > 50 && isTravisBuild) {
                    return null;
                }
                final Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.READ_ONLY);
                if (current.getArguments().length < FrameAccess.RECEIVER) {
                    return null;
                }
                final Object method = FrameAccess.getMethod(current);
                lastSender[0] = FrameAccess.getSender(current);
                final Object contextOrMarker = FrameAccess.getContextOrMarker(current);
                final Object[] arguments = FrameAccess.getReceiverAndArguments(current);
                final String[] argumentStrings = new String[arguments.length];
                for (int i = 0; i < arguments.length; i++) {
                    argumentStrings[i] = arguments[i].toString();
                }
                final String prefix = FrameAccess.getClosure(current) == null ? "" : "[] in ";
                getError().println(String.format("%s%s #(%s) [this: %s, sender: %s]", prefix, method, String.join(", ", argumentStrings), contextOrMarker, lastSender[0]));
                return null;
            }
        });
        getError().println("== " + depth[0] + " Truffle frames ================================================================");
        if (lastSender[0] instanceof ContextObject) {
            ((ContextObject) lastSender[0]).printSqStackTrace();
        }
    }
}
