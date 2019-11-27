/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.image;

import java.io.File;
import java.io.PrintWriter;
import java.lang.ref.ReferenceQueue;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.graalvm.collections.EconomicMap;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.instrumentation.AllocationReporter;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.Source;

import de.hpi.swa.graal.squeak.SqueakImage;
import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.SqueakOptions.SqueakContextOptions;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.reading.SqueakImageReader;
import de.hpi.swa.graal.squeak.interop.InteropMap;
import de.hpi.swa.graal.squeak.interop.LookupMethodByStringNode;
import de.hpi.swa.graal.squeak.io.DisplayPoint;
import de.hpi.swa.graal.squeak.io.SqueakDisplay;
import de.hpi.swa.graal.squeak.io.SqueakDisplayInterface;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObjectWithClassAndHash;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.BooleanObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.ASSOCIATION;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.CONTEXT;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.ENVIRONMENT;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.LINKED_LIST;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.MESSAGE;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.PROCESS;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.SEMAPHORE;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.SMALLTALK_IMAGE;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.SPECIAL_OBJECT;
import de.hpi.swa.graal.squeak.model.layout.SlotLocation;
import de.hpi.swa.graal.squeak.nodes.ExecuteTopLevelContextNode;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.graal.squeak.nodes.plugins.B2D;
import de.hpi.swa.graal.squeak.nodes.plugins.BitBlt;
import de.hpi.swa.graal.squeak.nodes.plugins.JPEGReader;
import de.hpi.swa.graal.squeak.nodes.plugins.SqueakSSL.SqSSL;
import de.hpi.swa.graal.squeak.nodes.plugins.Zip;
import de.hpi.swa.graal.squeak.nodes.plugins.network.SqueakSocket;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveNodeFactory;
import de.hpi.swa.graal.squeak.shared.SqueakLanguageConfig;
import de.hpi.swa.graal.squeak.util.ArrayConversionUtils;
import de.hpi.swa.graal.squeak.util.ArrayUtils;
import de.hpi.swa.graal.squeak.util.FrameAccess;
import de.hpi.swa.graal.squeak.util.InterruptHandlerState;
import de.hpi.swa.graal.squeak.util.MiscUtils;

public final class SqueakImageContext {
    private static final TruffleLogger LOG = TruffleLogger.getLogger(SqueakLanguageConfig.ID, SqueakImageContext.class);
    /* Special objects */
    public final ClassObject trueClass = new ClassObject(this);
    public final ClassObject falseClass = new ClassObject(this);
    public final PointersObject schedulerAssociation = new PointersObject(this);
    public final ClassObject bitmapClass = new ClassObject(this);
    public final ClassObject smallIntegerClass = new ClassObject(this);
    public final ClassObject byteStringClass = new ClassObject(this);
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
    public final NativeObject cannotReturn = new NativeObject(this);
    public final NativeObject mustBeBooleanSelector = new NativeObject(this);
    public final ClassObject byteArrayClass = new ClassObject(this);
    public final ClassObject processClass = new ClassObject(this);
    public final ClassObject blockClosureClass = new ClassObject(this);
    public final ClassObject largeNegativeIntegerClass = new ClassObject(this);
    public final NativeObject aboutToReturnSelector = new NativeObject(this);
    public final NativeObject runWithInSelector = new NativeObject(this);
    public final ArrayObject primitiveErrorTable = new ArrayObject(this);
    public final ArrayObject specialSelectors = new ArrayObject(this);
    @CompilationFinal public ClassObject smallFloatClass = null;
    @CompilationFinal public ClassObject truffleObjectClass = null;

    public final ArrayObject specialObjectsArray = new ArrayObject(this);
    public final ClassObject metaClass = new ClassObject(this);
    public final ClassObject nilClass = new ClassObject(this);

    public final CompiledMethodObject dummyMethod = new CompiledMethodObject(this, null, new Object[]{CompiledCodeObject.makeHeader(1, 0, 0, false, true)});

    /* System Information */
    public final SqueakImageFlags flags = new SqueakImageFlags();
    @CompilationFinal private String imagePath;
    @CompilationFinal private boolean isHeadless;
    public final SqueakContextOptions options;

    /* System */
    private boolean currentMarkingFlag;
    @CompilationFinal private SqueakDisplayInterface display;
    public final InterruptHandlerState interrupt;
    public final PrimitiveNodeFactory primitiveNodeFactory = new PrimitiveNodeFactory();
    public final long startUpMillis = System.currentTimeMillis();
    public final ReferenceQueue<Object> weakPointersQueue = new ReferenceQueue<>();

    /* Truffle */
    private final AllocationReporter allocationReporter;
    @CompilationFinal public SqueakLanguage.Env env;
    private final SqueakLanguage language;
    private Source lastParseRequestSource;
    @CompilationFinal private PrintWriter output;
    @CompilationFinal private PrintWriter error;

    @CompilationFinal private SqueakImage squeakImage;

    /* Stack Management */
    public int stackDepth = 0;
    public ContextObject lastSeenContext;

    @CompilationFinal private ClassObject compilerClass = null;
    @CompilationFinal private ClassObject parserClass = null;
    @CompilationFinal private PointersObject scheduler = null;
    @CompilationFinal private ClassObject wideStringClass = null;

    /* Plugins */
    public final B2D b2d = new B2D(this);
    public final BitBlt bitblt = new BitBlt();
    public String[] dropPluginFileList = new String[0];
    public final EconomicMap<Long, SeekableByteChannel> filePluginHandles = EconomicMap.create();
    public final JPEGReader jpegReader = new JPEGReader();
    public final EconomicMap<Long, SqueakSocket> socketPluginHandles = EconomicMap.create();
    public final EconomicMap<Long, SqSSL> squeakSSLHandles = EconomicMap.create();
    public final Zip zip = new Zip();

    /* Error detection for headless execution */
    @CompilationFinal(dimensions = 1) public static final byte[] DEBUG_ERROR_SELECTOR_NAME = "debugError:".getBytes();
    @CompilationFinal private NativeObject debugErrorSelector = null;
    @CompilationFinal(dimensions = 1) public static final byte[] DEBUG_SYNTAX_ERROR_SELECTOR_NAME = "debugSyntaxError:".getBytes();
    @CompilationFinal private NativeObject debugSyntaxErrorSelector = null;
    @CompilationFinal(dimensions = 1) public static final byte[] SIGNAL_FAILURE_SELECTOR_NAME = "signalFailure:".getBytes();
    @CompilationFinal private NativeObject signalFailureSelector = null;

    public Map<PointersObject, ContextObject> suspendedContexts = new HashMap<>();

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
            SqueakImageReader.load(this);
            getOutput().println("Preparing image for headless execution...");
            LOG.fine(() -> "Fresh after load" + currentState());
            // Remove active context.
            getActiveProcessSlow().instVarAtPut0Slow(PROCESS.SUSPENDED_CONTEXT, NilObject.SINGLETON);
            // Modify StartUpList for headless execution.
            evaluate("{EventSensor. Project} do: [:ea | Smalltalk removeFromStartUpList: ea]");
            try {
                /** See SmalltalkImage>>#snapshot:andQuit:withExitCode:embedded:. */
                evaluate("[Smalltalk clearExternalObjects. Smalltalk processStartUpList: true. Smalltalk setPlatformPreferences] value");
            } catch (final Exception e) {
                printToStdErr("startUpList failed:");
                e.printStackTrace();
            }
            // Set author information.
            evaluate("Utilities authorName: 'GraalSqueak'");
            evaluate("Utilities setAuthorInitials: 'GraalSqueak'");
            // Initialize fresh MorphicUIManager.
            evaluate("Project current instVarNamed: #uiManager put: MorphicUIManager new");
            LOG.fine(() -> "After newly loaded image startUp" + currentState());
        }
    }

    /**
     * Returns `true` if image has been loaded. {@link SqueakImageReader} calls
     * {@link #getSqueakImage()} and initializes `squeakImage`.
     */
    public boolean loaded() {
        return squeakImage != null;
    }

    public SqueakImage getSqueakImage() {
        if (squeakImage == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            squeakImage = new SqueakImage(this);
        }
        return squeakImage;
    }

    public Object evaluate(final String sourceCode) {
        CompilerAsserts.neverPartOfCompilation("For testing or instrumentation only.");
        final Source source = Source.newBuilder(SqueakLanguageConfig.NAME, sourceCode, "<image#evaluate>").build();
        LOG.fine("\nimage.evaluate " + sourceCode);
        return Truffle.getRuntime().createCallTarget(getDoItContextNode(source)).call();
    }

    public Object lookup(final String member) {
        final Object symbol = getCompilerClass().send("evaluate:", asByteString("'" + member + "' asSymbol"));
        return smalltalk.send("at:ifAbsent:", symbol, NilObject.SINGLETON);
    }

    public boolean patch(final SqueakLanguage.Env newEnv) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        env = newEnv;
        output = new PrintWriter(env.out(), true);
        error = new PrintWriter(env.err(), true);
        return true;
    }

    public ExecuteTopLevelContextNode getActiveContextNode() {
        final PointersObject activeProcess = getActiveProcessSlow();
        final ContextObject activeContext = (ContextObject) activeProcess.instVarAt0Slow(PROCESS.SUSPENDED_CONTEXT);
        activeProcess.instVarAtPut0Slow(PROCESS.SUSPENDED_CONTEXT, NilObject.SINGLETON);
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
                        "parse:class:noPattern:notifying:ifFail:", asByteString(source), nilClass, BooleanObject.TRUE, NilObject.SINGLETON, new BlockClosureObject(this, 0));
        final CompiledMethodObject doItMethod = (CompiledMethodObject) methodNode.send("generate");

        final ContextObject doItContext = ContextObject.create(this, doItMethod.getSqueakContextSize());
        doItContext.atput0(CONTEXT.METHOD, doItMethod);
        doItContext.atput0(CONTEXT.INSTRUCTION_POINTER, (long) doItMethod.getInitialPC());
        doItContext.atput0(CONTEXT.RECEIVER, NilObject.SINGLETON);
        doItContext.atput0(CONTEXT.STACKPOINTER, new Long(doItMethod.getNumTemps()));
        doItContext.atput0(CONTEXT.CLOSURE_OR_NIL, NilObject.SINGLETON);
        doItContext.atput0(CONTEXT.SENDER_OR_NIL, NilObject.SINGLETON);
        doItContext.setProcess(getActiveProcessSlow());
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

    public boolean getCurrentMarkingFlag() {
        return currentMarkingFlag;
    }

    public boolean toggleCurrentMarkingFlag() {
        return currentMarkingFlag = !currentMarkingFlag;
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

    public NativeObject getSignalFailureSelector() {
        return signalFailureSelector;
    }

    public void setSignalFailureSelector(final NativeObject signalFailureSelector) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        this.signalFailureSelector = signalFailureSelector;
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

    public ClassObject getWideStringClass() {
        if (wideStringClass == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            // TODO: find a better way to find wideStringClass or do this on image side instead?
            final CompiledMethodObject method = (CompiledMethodObject) LookupMethodByStringNode.getUncached().executeLookup(byteArrayClass, "asWideString");
            if (method != null) {
                final PointersObject assoc = (PointersObject) method.getLiteral(1);
                wideStringClass = (ClassObject) assoc.instVarAt0Slow(ASSOCIATION.VALUE);
            } else {
                /* Image only uses a single String class (e.g. Cuis 5.0). */
                wideStringClass = byteStringClass;
            }
        }
        return wideStringClass;
    }

    public static void initializeBeforeLoadingImage() {
        SlotLocation.initialize();
    }

    public void initializeAfterLoadingImage() {
        primitiveNodeFactory.initialize(this);
        initializeContexts();
    }

    private void initializeContexts() {
        for (final PointersObject p : suspendedContexts.keySet()) {
            AbstractSqueakObject currentContext = suspendedContexts.get(p);
            while (currentContext != NilObject.SINGLETON) {
                final ContextObject context = (ContextObject) currentContext;
                context.setProcess(p);
                currentContext = context.getSender();
            }
        }
        suspendedContexts.clear();
    }

    public ClassObject initializeTruffleObject() {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        truffleObjectClass = new ClassObject(this);
        return truffleObjectClass;
    }

    public boolean supportsTruffleObject() {
        return truffleObjectClass != null;
    }

    public boolean supportsNFI() {
        CompilerAsserts.neverPartOfCompilation();
        return env.getInternalLanguages().containsKey("nfi");
    }

    public PointersObject getScheduler() {
        if (scheduler == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            scheduler = (PointersObject) schedulerAssociation.instVarAt0Slow(ASSOCIATION.VALUE);
        }
        return scheduler;
    }

    public PointersObject getActiveProcessSlow() {
        return getActiveProcess(AbstractPointersObjectReadNode.getUncached());
    }

    public PointersObject getActiveProcess(final AbstractPointersObjectReadNode pointersReadNode) {
        return pointersReadNode.executePointers(getScheduler(), PROCESS_SCHEDULER.ACTIVE_PROCESS);
    }

    public Object getSpecialObject(final int index) {
        return specialObjectsArray.getObjectStorage()[index];
    }

    public void setSpecialObject(final int index, final Object value) {
        specialObjectsArray.getObjectStorage()[index] = value;
    }

    private ArrayObject getSpecialSelectors() {
        return (ArrayObject) getSpecialObject(SPECIAL_OBJECT.SPECIAL_SELECTORS);
    }

    public NativeObject getSpecialSelector(final int index) {
        return (NativeObject) getSpecialSelectors().getObjectStorage()[index * 2];
    }

    public int getSpecialSelectorNumArgs(final int index) {
        return (int) (long) getSpecialSelectors().getObjectStorage()[index * 2 + 1];
    }

    public void setSemaphore(final int index, final AbstractSqueakObject semaphore) {
        assert semaphore == NilObject.SINGLETON || ((AbstractSqueakObjectWithClassAndHash) semaphore).getSqueakClass().isSemaphoreClass();
        setSpecialObject(index, semaphore);
    }

    public boolean hasDisplay() {
        return display != null;
    }

    public SqueakDisplayInterface getDisplay() {
        return display;
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

    public void attachDisplayIfNecessary() {
        if (!isHeadless) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            display = new SqueakDisplay(this);
        }
    }

    public boolean isTesting() {
        return options.isTesting;
    }

    public Object getGlobals() {
        final PointersObject environment = (PointersObject) smalltalk.instVarAt0Slow(SMALLTALK_IMAGE.GLOBALS);
        final PointersObject bindings = (PointersObject) environment.instVarAt0Slow(ENVIRONMENT.BINDINGS);
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

    public NativeObject asByteArray(final byte[] bytes) {
        return NativeObject.newNativeBytes(this, byteArrayClass, bytes);
    }

    public NativeObject asByteString(final String value) {
        return NativeObject.newNativeBytes(this, byteStringClass, ArrayConversionUtils.stringToBytes(value));
    }

    public NativeObject asWideString(final String value) {
        return NativeObject.newNativeInts(this, getWideStringClass(), ArrayConversionUtils.stringToCodePointsArray(value));
    }

    public NativeObject asString(final String value, final ConditionProfile wideStringProfile) {
        return wideStringProfile.profile(NativeObject.needsWideString(value)) ? asWideString(value) : asByteString(value);
    }

    public PointersObject asPoint(final AbstractPointersObjectWriteNode writeNode, final Object xPos, final Object yPos) {
        return PointersObject.create(writeNode, pointClass, xPos, yPos);
    }

    public PointersObject asPoint(final AbstractPointersObjectWriteNode writeNode, final DisplayPoint point) {
        return asPoint(writeNode, (long) point.getWidth(), (long) point.getHeight());
    }

    public ArrayObject newEmptyArray() {
        return ArrayObject.createWithStorage(this, arrayClass, ArrayUtils.EMPTY_ARRAY);
    }

    public PointersObject newMessage(final AbstractPointersObjectWriteNode writeNode, final NativeObject selector, final ClassObject rcvrClass, final Object[] arguments) {
        final PointersObject message = new PointersObject(this, messageClass);
        writeNode.execute(message, MESSAGE.SELECTOR, selector);
        writeNode.execute(message, MESSAGE.ARGUMENTS, asArrayOfObjects(arguments));
        if (message.instsize() > MESSAGE.LOOKUP_CLASS) { // Early versions do not have lookupClass.
            writeNode.execute(message, MESSAGE.LOOKUP_CLASS, rcvrClass);
        }
        return message;
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
        if (lastSender[0] instanceof ContextObject) {
            getError().println("== Squeak frames ================================================================");
            ((ContextObject) lastSender[0]).printSqStackTrace();
        }
    }

    public String currentState() {
        final StringBuilder b = new StringBuilder();
        b.append("\nImage processes state\n");
        final PointersObject activeProcess = getActiveProcessSlow();
        final long activePriority = (long) activeProcess.instVarAt0Slow(PROCESS.PRIORITY);
        b.append("Active process @");
        b.append(activeProcess.hashCode());
        b.append(" priority ");
        b.append(activePriority);
        b.append('\n');
        final Object interruptSema = getSpecialObject(SPECIAL_OBJECT.THE_INTERRUPT_SEMAPHORE);
        printSemaphoreOrNil(b, "Interrupt semaphore @", interruptSema, true);
        final Object timerSema = getSpecialObject(SPECIAL_OBJECT.THE_TIMER_SEMAPHORE);
        printSemaphoreOrNil(b, "Timer semaphore @", timerSema, true);
        final ArrayObject externalObjects = (ArrayObject) getSpecialObject(SPECIAL_OBJECT.EXTERNAL_OBJECTS_ARRAY);
        if (!externalObjects.isEmptyType()) {
            final Object[] semaphores = externalObjects.getObjectStorage();
            for (int i = 0; i < semaphores.length; i++) {
                printSemaphoreOrNil(b, "External semaphore at index " + (i + 1) + " @", semaphores[i], false);
            }
        }
        final Object[] lists = ((ArrayObject) getScheduler().instVarAt0Slow(PROCESS_SCHEDULER.PROCESS_LISTS)).getObjectStorage();
        for (int i = 0; i < lists.length; i++) {
            printLinkedList(b, "Quiescent processes list at priority " + (i + 1), (PointersObject) lists[i]);
        }
        return b.toString();
    }

    private static void printSemaphoreOrNil(final StringBuilder b, final String label, final Object semaphoreOrNil, final boolean printIfNil) {
        if (semaphoreOrNil instanceof PointersObject) {
            b.append(label);
            b.append(semaphoreOrNil.hashCode());
            b.append(" with ");
            b.append(((PointersObject) semaphoreOrNil).instVarAt0Slow(SEMAPHORE.EXCESS_SIGNALS));
            b.append(" excess signals");
            if (!printLinkedList(b, "", (PointersObject) semaphoreOrNil)) {
                b.append(" and no processes\n");
            }
        } else {
            if (printIfNil) {
                b.append(label);
                b.append(" is nil\n");
            }
        }
    }

    private static boolean printLinkedList(final StringBuilder b, final String label, final PointersObject linkedList) {
        Object temp = linkedList.instVarAt0Slow(LINKED_LIST.FIRST_LINK);
        if (temp instanceof PointersObject) {
            b.append(label);
            b.append(" and processes:\n");
            while (temp instanceof PointersObject) {
                final PointersObject aProcess = (PointersObject) temp;
                final Object aContext = aProcess.instVarAt0Slow(PROCESS.SUSPENDED_CONTEXT);
                if (aContext instanceof ContextObject) {
                    b.append("\tprocess @");
                    b.append(aProcess.hashCode());
                    b.append(" with suspended context ");
                    b.append(aContext);
                    b.append(" and stack trace:\n");
                    ((ContextObject) aContext).printSqMaterializedStackTraceOn(b);
                } else {
                    b.append("\tprocess @");
                    b.append(aProcess.hashCode());
                    b.append(" with suspended context nil\n");
                }
                temp = aProcess.instVarAt0Slow(PROCESS.NEXT_LINK);
            }
            return true;
        } else {
            return false;
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
