package de.hpi.swa.graal.squeak.image;

import java.io.File;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.graalvm.collections.EconomicMap;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleOptions;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.instrumentation.AllocationReporter;
import com.oracle.truffle.api.source.Source;

import de.hpi.swa.graal.squeak.SqueakImage;
import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.SqueakOptions.SqueakContextOptions;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.reading.SqueakImageReaderNode;
import de.hpi.swa.graal.squeak.interop.InteropMap;
import de.hpi.swa.graal.squeak.io.DisplayPoint;
import de.hpi.swa.graal.squeak.io.SqueakDisplay;
import de.hpi.swa.graal.squeak.io.SqueakDisplayInterface;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObjectWithClassAndHash;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.ASSOCIATION;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.ENVIRONMENT;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.SMALLTALK_IMAGE;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.ExecuteTopLevelContextNode;
import de.hpi.swa.graal.squeak.nodes.plugins.SqueakSSL.SqSSL;
import de.hpi.swa.graal.squeak.nodes.plugins.network.SqueakSocket;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveNodeFactory;
import de.hpi.swa.graal.squeak.shared.SqueakLanguageConfig;
import de.hpi.swa.graal.squeak.util.ArrayConversionUtils;
import de.hpi.swa.graal.squeak.util.ArrayUtils;
import de.hpi.swa.graal.squeak.util.FrameAccess;
import de.hpi.swa.graal.squeak.util.InterruptHandlerState;
import de.hpi.swa.graal.squeak.util.MiscUtils;
import de.hpi.swa.graal.squeak.util.OSDetector;

public final class SqueakImageContext {
    public final boolean sqFalse = false;
    public final boolean sqTrue = true;
    // Special objects
    public final ClassObject trueClass = new ClassObject(this);
    public final ClassObject falseClass = new ClassObject(this);
    public final PointersObject schedulerAssociation = new PointersObject(this);
    public final ClassObject bitmapClass = new ClassObject(this);
    public final ClassObject smallIntegerClass = new ClassObject(this);
    public final ClassObject stringClass = new ClassObject(this);
    public final ClassObject arrayClass = new ClassObject(this);
    public final PointersObject smalltalk = new PointersObject(this);
    public final ClassObject floatClass = new ClassObject(this);
    public final ClassObject methodContextClass = new ClassObject(this);
    public final ClassObject pointClass = new ClassObject(this);
    public final ClassObject largePositiveIntegerClass = new ClassObject(this);
    public final ClassObject messageClass = new ClassObject(this);
    public final ClassObject compiledMethodClass = new ClassObject(this);
    public final ClassObject semaphoreClass = new ClassObject(this);
    public final ClassObject characterClass = new ClassObject(this);
    public final NativeObject doesNotUnderstand = new NativeObject(this);
    public final NativeObject mustBeBooleanSelector = new NativeObject(this);
    public final ClassObject byteArrayClass = new ClassObject(this);
    public final ClassObject processClass = new ClassObject(this);
    public final ClassObject blockClosureClass = new ClassObject(this);
    public final ArrayObject externalObjectsArray = new ArrayObject(this);
    public final ClassObject largeNegativeIntegerClass = new ClassObject(this);
    public final NativeObject aboutToReturnSelector = new NativeObject(this);
    public final NativeObject runWithInSelector = new NativeObject(this);
    public final ArrayObject primitiveErrorTable = new ArrayObject(this);
    public final ArrayObject specialSelectors = new ArrayObject(this);
    @CompilationFinal public ClassObject smallFloatClass;
    @CompilationFinal public ClassObject truffleObjectClass = null;

    public final ArrayObject specialObjectsArray = new ArrayObject(this);
    public final ClassObject metaClass = new ClassObject(this);
    public final ClassObject nilClass = new ClassObject(this);

    private final SqueakLanguage language;
    @CompilationFinal private PrintWriter output;
    @CompilationFinal private PrintWriter error;
    @CompilationFinal public SqueakLanguage.Env env;

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
    public final NativeObject nonEquivalent = new NativeObject(this);
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
                    klass, nonEquivalent, sqValue, valueWithArg, sqDo, sqNew, newWithArg, x, y
    };

    @CompilationFinal(dimensions = 1) public final int[] specialSelectorsNumArgs = new int[]{
                    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 0, 0, 1, 0, 1, 0, 1, 0, 1, 1, 0, 1, 0, 0
    };

    @CompilationFinal private String imagePath;
    @CompilationFinal private boolean isHeadless;
    public final SqueakImageFlags flags = new SqueakImageFlags();
    public final SqueakContextOptions options;
    public final OSDetector os = new OSDetector();
    public final InterruptHandlerState interrupt;
    public final long startUpMillis = System.currentTimeMillis();
    private final AllocationReporter allocationReporter;

    @CompilationFinal private SqueakDisplayInterface display;

    @CompilationFinal private ClassObject compilerClass = null;
    @CompilationFinal private ClassObject parserClass = null;
    @CompilationFinal private PointersObject scheduler = null;

    public final PrimitiveNodeFactory primitiveNodeFactory = new PrimitiveNodeFactory();
    public final EconomicMap<Long, SeekableByteChannel> filePluginHandles = EconomicMap.create();
    public final EconomicMap<Long, SqueakSocket> socketPluginHandles = EconomicMap.create();
    public final EconomicMap<Long, SqSSL> squeakSSLHandles = EconomicMap.create();
    public String[] dropPluginFileList = new String[0];

    public static final byte[] DEBUG_ERROR_SELECTOR_NAME = "debugError:".getBytes(); // for testing
    @CompilationFinal private NativeObject debugErrorSelector = null; // for testing
    public static final byte[] DEBUG_SYNTAX_ERROR_SELECTOR_NAME = "debugSyntaxError:".getBytes(); // for
                                                                                                  // testing
    @CompilationFinal private NativeObject debugSyntaxErrorSelector = null; // for testing

    private Source lastParseRequestSource;
    @CompilationFinal private SqueakImage squeakImage;

    public SqueakImageContext(final SqueakLanguage squeakLanguage, final SqueakLanguage.Env environment) {
        language = squeakLanguage;
        patch(environment);
        options = new SqueakContextOptions(env);
        isHeadless = options.isHeadless;
        interrupt = InterruptHandlerState.create(this);
        allocationReporter = env.lookup(AllocationReporter.class);
    }

    public void ensureLoaded() {
        if (!loaded()) {
            // Load image.
            Truffle.getRuntime().createCallTarget(new SqueakImageReaderNode(this)).call();
            // Remove active context.
            getActiveProcess().atputNil0(PROCESS.SUSPENDED_CONTEXT);
            // Modify StartUpList for headless execution.
            // TODO: Also start ProcessorScheduler and WeakArray (see SqueakSUnitTest).
            evaluate("{EventSensor. ProcessorScheduler. Project. WeakArray} do: [:ea | Smalltalk removeFromStartUpList: ea]");
            try {
                evaluate("[Smalltalk processStartUpList: true] value");
            } catch (final Exception e) {
                printToStdErr("startUpList failed:");
                e.printStackTrace();
            }
            // Set author information.
            evaluate("Utilities authorName: 'GraalSqueak'");
            evaluate("Utilities setAuthorInitials: 'GraalSqueak'");
            // Initialize fresh MorphicUIManager.
            evaluate("Project current instVarNamed: #uiManager put: MorphicUIManager new");
        }
    }

    /**
     * Returns `true` if image has been loaded. {@link SqueakImageReaderNode} calls
     * {@link #getSqueakImage()} and initializes `squeakImage`.
     */
    public boolean loaded() {
        return squeakImage != null;
    }

    public SqueakImage getSqueakImage() {
        if (squeakImage == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            squeakImage = new SqueakImage(this);
            primitiveNodeFactory.initialize(this);
        }
        return squeakImage;
    }

    private Object evaluate(final String sourceCode) {
        CompilerAsserts.neverPartOfCompilation("For testing or instrumentation only.");
        final Source source = Source.newBuilder(SqueakLanguageConfig.NAME, sourceCode, "<image#evaluate>").build();
        return Truffle.getRuntime().createCallTarget(getDoItContextNode(source)).call();
    }

    public boolean patch(final SqueakLanguage.Env newEnv) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        env = newEnv;
        output = new PrintWriter(env.out(), true);
        error = new PrintWriter(env.err(), true);
        return true;
    }

    public ExecuteTopLevelContextNode getActiveContextNode() {
        assert lastParseRequestSource == null : "Image should not have been executed manually before.";
        final PointersObject activeProcess = getActiveProcess();
        final ContextObject activeContext = (ContextObject) activeProcess.at0(PROCESS.SUSPENDED_CONTEXT);
        activeProcess.atputNil0(PROCESS.SUSPENDED_CONTEXT);
        return ExecuteTopLevelContextNode.create(getLanguage(), activeContext, true);
    }

    public ExecuteTopLevelContextNode getDoItContextNode(final Source source) {
        lastParseRequestSource = source;
        return getDoItContextNode(source.getCharacters().toString());
    }

    public ExecuteTopLevelContextNode getDoItContextNode(final String source) {
        /*
         * (Parser new parse: '1 + 2 * 3' class: UndefinedObject noPattern: true notifying: nil
         * ifFail: [^nil]) generate
         */
        assert parserClass != null;
        assert compilerClass != null;

        final AbstractSqueakObjectWithClassAndHash parser = (AbstractSqueakObjectWithClassAndHash) parserClass.send("new");
        final AbstractSqueakObjectWithClassAndHash methodNode = (AbstractSqueakObjectWithClassAndHash) parser.send(
                        "parse:class:noPattern:notifying:ifFail:", asByteString(source), nilClass, sqTrue, NilObject.SINGLETON, new BlockClosureObject(this));
        final CompiledMethodObject doItMethod = (CompiledMethodObject) methodNode.send("generate");

        final ContextObject doItContext = ContextObject.create(this, doItMethod.getSqueakContextSize());
        doItContext.atput0(CONTEXT.METHOD, doItMethod);
        doItContext.atput0(CONTEXT.INSTRUCTION_POINTER, (long) doItMethod.getInitialPC());
        doItContext.atput0(CONTEXT.RECEIVER, nilClass);
        doItContext.atput0(CONTEXT.STACKPOINTER, 0L);
        doItContext.atput0(CONTEXT.CLOSURE_OR_NIL, NilObject.SINGLETON);
        doItContext.atput0(CONTEXT.SENDER_OR_NIL, NilObject.SINGLETON);
        return ExecuteTopLevelContextNode.create(getLanguage(), doItContext, false);
    }

    /*
     * ACCESSING
     */

    public PrintWriter getOutput() {
        return output;
    }

    public PrintWriter getError() {
        return error;
    }

    public SqueakLanguage getLanguage() {
        return language;
    }

    public NativeObject getDebugErrorSelector() {
        return debugErrorSelector;
    }

    public void setDebugErrorSelector(final NativeObject debugErrorSelector) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        this.debugErrorSelector = debugErrorSelector;
    }

    public NativeObject getDebugSyntaxErrorSelector() {
        return debugSyntaxErrorSelector;
    }

    public void setDebugSyntaxErrorSelector(final NativeObject debugSyntaxErrorSelector) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        this.debugSyntaxErrorSelector = debugSyntaxErrorSelector;
    }

    public ClassObject getCompilerClass() {
        return compilerClass;
    }

    public void setCompilerClass(final ClassObject compilerClass) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        this.compilerClass = compilerClass;
    }

    public ClassObject getParserClass() {
        return parserClass;
    }

    public void setParserClass(final ClassObject parserClass) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        this.parserClass = parserClass;
    }

    public void setSmallFloat(final ClassObject classObject) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        smallFloatClass = classObject;
    }

    public ClassObject initializeTruffleObject() {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        truffleObjectClass = new ClassObject(this);
        return truffleObjectClass;
    }

    public boolean supportsTruffleObject() {
        return truffleObjectClass != null;
    }

    public PointersObject getScheduler() {
        if (scheduler == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            scheduler = (PointersObject) schedulerAssociation.at0(ASSOCIATION.VALUE);
        }
        return scheduler;
    }

    public PointersObject getActiveProcess() {
        return (PointersObject) getScheduler().at0(PROCESS_SCHEDULER.ACTIVE_PROCESS);
    }

    public void setSemaphore(final long index, final AbstractSqueakObject semaphore) {
        assert semaphore == NilObject.SINGLETON || ((AbstractSqueakObjectWithClassAndHash) semaphore).getSqueakClass().isSemaphoreClass();
        specialObjectsArray.atput0Object(index, semaphore);
    }

    public boolean hasDisplay() {
        return display != null;
    }

    public SqueakDisplayInterface getDisplay() {
        return display;
    }

    public static boolean isAOT() {
        return TruffleOptions.AOT;
    }

    public String imageRelativeFilePathFor(final String fileName) {
        return getImageDirectory() + File.separator + fileName;
    }

    public String getImagePath() {
        if (imagePath == null) {
            assert !options.imagePath.isEmpty();
            setImagePath(options.imagePath);
        }
        return imagePath;
    }

    public void setImagePath(final String path) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        imagePath = path;
    }

    @TruffleBoundary
    public String getImageDirectory() {
        final Path parent = Paths.get(getImagePath()).getParent();
        if (parent != null) {
            return "" + parent.getFileName(); // Avoids NullPointerExceptions.
        } else {
            throw SqueakException.create("`parent` should not be `null`.");
        }
    }

    public String[] getImageArguments() {
        return env.getApplicationArguments();
    }

    public Source getLastParseRequestSource() {
        return lastParseRequestSource;
    }

    public boolean interruptHandlerDisabled() {
        return options.disableInterruptHandler;
    }

    public boolean isHeadless() {
        return isHeadless;
    }

    public void disableHeadless() {
        if (isHeadless) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            display = new SqueakDisplay(this);
            isHeadless = false;
        }
    }

    public boolean isTesting() {
        return options.isTesting;
    }

    public Object getGlobals() {
        final PointersObject environment = (PointersObject) smalltalk.at0(SMALLTALK_IMAGE.GLOBALS);
        final PointersObject bindings = (PointersObject) environment.at0(ENVIRONMENT.BINDINGS);
        return new InteropMap(bindings);
    }

    /*
     * INSTANCE CREATION
     */

    public ArrayObject asArrayOfLongs(final long... elements) {
        return ArrayObject.createWithStorage(this, arrayClass, elements);
    }

    public ArrayObject asArrayOfNativeObjects(final NativeObject... elements) {
        return ArrayObject.createWithStorage(this, arrayClass, elements);
    }

    public ArrayObject asArrayOfObjects(final Object... elements) {
        return ArrayObject.createWithStorage(this, arrayClass, elements);
    }

    public boolean asBoolean(final boolean value) {
        return value ? sqTrue : sqFalse;
    }

    public NativeObject asByteArray(final byte[] bytes) {
        return NativeObject.newNativeBytes(this, byteArrayClass, bytes);
    }

    public NativeObject asByteString(final String value) {
        return NativeObject.newNativeBytes(this, stringClass, ArrayConversionUtils.stringToBytes(value));
    }

    public FloatObject asFloatObject(final double value) {
        return FloatObject.valueOf(this, value);
    }

    public LargeIntegerObject asLargeInteger(final BigInteger i) {
        return new LargeIntegerObject(this, i);
    }

    public PointersObject asPoint(final Object xPos, final Object yPos) {
        return new PointersObject(this, pointClass, new Object[]{xPos, yPos});
    }

    public PointersObject asPoint(final DisplayPoint point) {
        return asPoint((long) point.getWidth(), (long) point.getHeight());
    }

    public ArrayObject newEmptyArray() {
        return ArrayObject.createWithStorage(this, arrayClass, ArrayUtils.EMPTY_ARRAY);
    }

    /*
     * PRINTING
     */

    @TruffleBoundary
    public void printToStdOut(final Object... arguments) {
        getOutput().println(MiscUtils.format("[graalsqueak] %s", ArrayUtils.toJoinedString(" ", arguments)));
    }

    @TruffleBoundary
    public void printToStdErr(final Object... arguments) {
        getError().println(MiscUtils.format("[graalsqueak] %s", ArrayUtils.toJoinedString(" ", arguments)));
    }

    @TruffleBoundary
    public void printSqStackTrace() {
        CompilerDirectives.transferToInterpreter();
        final boolean isTravisBuild = System.getenv().containsKey("TRAVIS");
        final int[] depth = new int[1];
        final Object[] lastSender = new Object[]{null};
        getError().println("== Truffle stack trace ===========================================================");
        Truffle.getRuntime().iterateFrames(frameInstance -> {
            if (depth[0]++ > 50 && isTravisBuild) {
                return null;
            }
            final Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.READ_ONLY);
            if (!FrameAccess.isGraalSqueakFrame(current)) {
                return null;
            }
            final CompiledMethodObject method = FrameAccess.getMethod(current);
            lastSender[0] = FrameAccess.getSender(current);
            final Object marker = FrameAccess.getMarker(current, method);
            final Object context = FrameAccess.getContext(current, method);
            final String prefix = FrameAccess.getClosure(current) == null ? "" : "[] in ";
            final String argumentsString = ArrayUtils.toJoinedString(", ", FrameAccess.getReceiverAndArguments(current));
            getError().println(MiscUtils.format("%s%s #(%s) [marker: %s, context: %s, sender: %s]", prefix, method, argumentsString, marker, context, lastSender[0]));
            return null;
        });
        getError().println("== Squeak frames ================================================================");
        if (lastSender[0] instanceof ContextObject) {
            ((ContextObject) lastSender[0]).printSqStackTrace();
        }
    }

    /*
     * INSTRUMENTATION
     */

    public void reportNewAllocationRequest() {
        allocationReporter.onEnter(null, 0, AllocationReporter.SIZE_UNKNOWN);
    }

    public <T extends Object> T reportNewAllocationResult(final T value) {
        allocationReporter.onReturnValue(value, 0, AllocationReporter.SIZE_UNKNOWN);
        return value;
    }
}
