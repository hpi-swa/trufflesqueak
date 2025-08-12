/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.image;

import java.lang.ref.ReferenceQueue;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.graalvm.collections.UnmodifiableEconomicMap;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.TruffleLanguage.ParsingRequest;
import com.oracle.truffle.api.dsl.Bind.DefaultExpression;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.AllocationReporter;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.Message;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.utilities.CyclicAssumption;

import de.hpi.swa.trufflesqueak.SqueakImage;
import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.SqueakOptions.SqueakContextOptions;
import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.interop.LookupMethodByStringNode;
import de.hpi.swa.trufflesqueak.io.SqueakDisplay;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObjectWithClassAndHash;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject.CompiledCodeHeaderUtils;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.EphemeronObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.ASSOCIATION;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.FRACTION;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.MESSAGE;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.POINT;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.SPECIAL_OBJECT;
import de.hpi.swa.trufflesqueak.model.layout.SlotLocation;
import de.hpi.swa.trufflesqueak.nodes.DoItRootNode;
import de.hpi.swa.trufflesqueak.nodes.ExecuteTopLevelContextNode;
import de.hpi.swa.trufflesqueak.nodes.SqueakGuards;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.MiscellaneousBytecodes.CallPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.interrupts.CheckForInterruptsState;
import de.hpi.swa.trufflesqueak.nodes.plugins.B2D;
import de.hpi.swa.trufflesqueak.nodes.plugins.BitBlt;
import de.hpi.swa.trufflesqueak.nodes.plugins.JPEGReader;
import de.hpi.swa.trufflesqueak.nodes.plugins.Zip;
import de.hpi.swa.trufflesqueak.nodes.plugins.ffi.InterpreterProxy;
import de.hpi.swa.trufflesqueak.nodes.process.SignalSemaphoreNodeGen;
import de.hpi.swa.trufflesqueak.shared.SqueakImageLocator;
import de.hpi.swa.trufflesqueak.tools.SqueakMessageInterceptor;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.LogUtils;
import de.hpi.swa.trufflesqueak.util.MethodCacheEntry;
import de.hpi.swa.trufflesqueak.util.MiscUtils;
import de.hpi.swa.trufflesqueak.util.ObjectGraphUtils;

@DefaultExpression("get($node)")
public final class SqueakImageContext {
    private static final ContextReference<SqueakImageContext> REFERENCE = ContextReference.create(SqueakLanguage.class);

    /* Special objects */
    public final ClassObject trueClass = new ClassObject(this);
    public final ClassObject falseClass = new ClassObject(this);
    public final PointersObject schedulerAssociation = new PointersObject();
    public final ClassObject bitmapClass = new ClassObject(this);
    public final ClassObject smallIntegerClass = new ClassObject(this);
    public final ClassObject byteStringClass = new ClassObject(this);
    public final ClassObject arrayClass = new ClassObject(this);
    public final PointersObject smalltalk = new PointersObject();
    public final ClassObject floatClass = new ClassObject(this);
    public final ClassObject methodContextClass = new ClassObject(this);
    public final ClassObject pointClass = new ClassObject(this);
    public final ClassObject largePositiveIntegerClass = new ClassObject(this);
    public final ClassObject messageClass = new ClassObject(this);
    public final ClassObject compiledMethodClass = new ClassObject(this);
    public final ClassObject semaphoreClass = new ClassObject(this);
    public final ClassObject characterClass = new ClassObject(this);
    public final NativeObject doesNotUnderstand = new NativeObject();
    public final NativeObject cannotReturn = new NativeObject();
    public final NativeObject mustBeBooleanSelector = new NativeObject();
    public final ClassObject byteArrayClass = new ClassObject(this);
    public final ClassObject processClass = new ClassObject(this);
    public final ClassObject blockClosureClass = new ClassObject(this);
    public final ClassObject largeNegativeIntegerClass = new ClassObject(this);
    public final NativeObject aboutToReturnSelector = new NativeObject();
    public final NativeObject runWithInSelector = new NativeObject();
    public final ArrayObject primitiveErrorTable = new ArrayObject();
    public final ArrayObject specialSelectors = new ArrayObject();
    @CompilationFinal public ClassObject fullBlockClosureClass;
    @CompilationFinal public ClassObject smallFloatClass;
    @CompilationFinal private ClassObject byteSymbolClass;
    @CompilationFinal private ClassObject foreignObjectClass;
    private final CyclicAssumption foreignObjectClassStable = new CyclicAssumption("ForeignObjectClassStable assumption");
    @CompilationFinal private ClassObject linkedListClass;

    public final ArrayObject specialObjectsArray = new ArrayObject();
    public final ClassObject metaClass = new ClassObject(this);
    public final ClassObject nilClass = new ClassObject(this);

    public final CompiledCodeObject dummyMethod = new CompiledCodeObject(this, null, CompiledCodeHeaderUtils.makeHeader(true, 1, 0, 0, false, true), ArrayUtils.EMPTY_ARRAY, compiledMethodClass);
    public final VirtualFrame externalSenderFrame = FrameAccess.newDummyFrame();

    /* Method Cache */
    private static final int METHOD_CACHE_SIZE = 2 << 12;
    private static final int METHOD_CACHE_MASK = METHOD_CACHE_SIZE - 1;
    private static final int METHOD_CACHE_REPROBES = 4;
    private int methodCacheRandomish;
    @CompilationFinal(dimensions = 1) private final MethodCacheEntry[] methodCache = new MethodCacheEntry[METHOD_CACHE_SIZE];

    /* Interpreter state */
    private int primFailCode = -1;

    /* System Information */
    public final SqueakImageFlags flags = new SqueakImageFlags();
    private String imagePath;
    @CompilationFinal public int imageFormat;
    private final TruffleFile homePath;
    @CompilationFinal(dimensions = 1) private byte[] resourcesDirectoryBytes;
    @CompilationFinal(dimensions = 1) private byte[] resourcesPathBytes;
    private final boolean isHeadless;
    public final SqueakContextOptions options;
    private final SqueakSystemAttributes systemAttributes = new SqueakSystemAttributes(this);

    /* System */
    public NativeObject clipboardTextHeadless = asByteString("");
    private boolean currentMarkingFlag;
    public final ObjectGraphUtils objectGraphUtils;
    private ArrayObject hiddenRoots;
    // first page of classTable is special
    public int classTableIndex = SqueakImageConstants.CLASS_TABLE_PAGE_SIZE;
    @CompilationFinal private SqueakDisplay display;
    public final CheckForInterruptsState interrupt;
    public final long startUpMillis = System.currentTimeMillis();
    public final ReferenceQueue<AbstractSqueakObject> weakPointersQueue = new ReferenceQueue<>();

    /* Truffle */
    private final AllocationReporter allocationReporter;
    @CompilationFinal public SqueakLanguage.Env env;
    private final SqueakLanguage language;
    private Source lastParseRequestSource;
    private final HashMap<Message, NativeObject> interopMessageToSelectorMap = new HashMap<>();

    @CompilationFinal private SqueakImage squeakImage;

    /* Stack Management */
    private ContextObject interopExceptionThrowingContextPrototype;
    public ContextObject lastSeenContext;

    /* Low space handling */
    private static final int LOW_SPACE_NUM_SKIPPED_SENDS = 4;
    private int lowSpaceSkippedSendsCount;

    /* Ephemeron support */
    public boolean containsEphemerons;
    public final ArrayDeque<EphemeronObject> ephemeronsQueue = new ArrayDeque<>();

    /* Context stack depth */
    @CompilationFinal private final int maxContextStackDepth;
    private int currentContextStackDepth;

    @CompilationFinal private ClassObject fractionClass;
    private PointersObject parserSharedInstance;
    private AbstractSqueakObject requestorSharedInstanceOrNil;
    @CompilationFinal private PointersObject scheduler;
    @CompilationFinal private Object smalltalkScope;
    @CompilationFinal private ClassObject wideStringClass;

    /* Plugins */
    @CompilationFinal private InterpreterProxy interpreterProxy;
    public final Map<String, Object> loadedLibraries = new HashMap<>();
    public final B2D b2d = new B2D(this);
    public final BitBlt bitblt = new BitBlt(this);
    public String[] dropPluginFileList = ArrayUtils.EMPTY_STRINGS_ARRAY;
    public final JPEGReader jpegReader = new JPEGReader();
    public final Zip zip = new Zip();

    /* Error detection for headless execution */
    @CompilationFinal(dimensions = 1) public static final byte[] DEBUG_ERROR_SELECTOR_NAME = "debugError:".getBytes();
    @CompilationFinal private NativeObject debugErrorSelector;
    @CompilationFinal(dimensions = 1) public static final byte[] DEBUG_SYNTAX_ERROR_SELECTOR_NAME = "debugSyntaxError:".getBytes();
    @CompilationFinal private NativeObject debugSyntaxErrorSelector;

    public SqueakImageContext(final SqueakLanguage squeakLanguage, final SqueakLanguage.Env environment) {
        language = squeakLanguage;
        options = SqueakContextOptions.create(environment.getOptions());
        isHeadless = options.isHeadless();
        maxContextStackDepth = options.maxContextStackDepth();
        patch(environment);
        interrupt = new CheckForInterruptsState(this);
        objectGraphUtils = new ObjectGraphUtils(this);
        allocationReporter = env.lookup(AllocationReporter.class);
        SqueakMessageInterceptor.enableIfRequested(environment);
        final String truffleLanguageHome = language.getTruffleLanguageHome();
        if (truffleLanguageHome != null) {
            homePath = env.getInternalTruffleFile(truffleLanguageHome);
        } else { /* Fall back to image directory if language home is not set. */
            homePath = env.getInternalTruffleFile(options.imagePath()).getParent();
        }
        assert homePath.exists() : "Home directory does not exist: " + homePath;
        initializeMethodCache();
    }

    public static SqueakImageContext get(final Node node) {
        return REFERENCE.get(node);
    }

    public static SqueakImageContext getSlow() {
        CompilerAsserts.neverPartOfCompilation();
        return get(null);
    }

    public void ensureLoaded() {
        if (squeakImage == null) {
            // Load image.
            SqueakImageReader.load(this);
            if (options.disableStartup()) {
                LogUtils.IMAGE.info("Skipping startup routine...");
                return;
            }

            final String prepareHeadlessImageScript = """
                            "Remove active context."
                            Processor activeProcess instVarNamed: #suspendedContext put: nil.

                            "Avoid interactive windows and instead exit on errors."
                            %s ifFalse: [ ToolSet default: CommandLineToolSet ].

                            "Start up image (see SmalltalkImage>>#snapshot:andQuit:withExitCode:embedded:)."
                            Smalltalk
                                clearExternalObjects;
                                processStartUpList: true;
                                setPlatformPreferences;
                                recordStartupStamp.

                            "Set author information."
                            Utilities
                                authorName: 'TruffleSqueak';
                                setAuthorInitials: 'TS'.
                            """.formatted(Boolean.toString(options.isTesting()));
            try {
                evaluate(prepareHeadlessImageScript);
            } catch (final Exception e) {
                LogUtils.IMAGE.log(Level.WARNING, "startUpList failed", e);
            } finally {
                interrupt.clear();
            }
        }
    }

    public SqueakImage getSqueakImage() {
        if (squeakImage == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            squeakImage = new SqueakImage(this);
        }
        return squeakImage;
    }

    @TruffleBoundary
    public Object evaluate(final String sourceCode) {
        return getDoItContextNode(sourceCode, false).getCallTarget().call();
    }

    @TruffleBoundary
    public Object evaluateUninterruptably(final String sourceCode) {
        final boolean wasActive = interrupt.isActive();
        interrupt.deactivate();
        try {
            return evaluate(sourceCode);
        } finally {
            if (wasActive) {
                interrupt.activate();
            }
        }
    }

    @TruffleBoundary
    public Object lookup(final String member) {
        return smalltalk.send(this, "at:ifAbsent:", asByteSymbol(member), NilObject.SINGLETON);
    }

    public boolean patch(final SqueakLanguage.Env newEnv) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        env = newEnv;
        return true;
    }

    @TruffleBoundary
    public ExecuteTopLevelContextNode getActiveContextNode() {
        final PointersObject activeProcess = getActiveProcessSlow();
        final ContextObject activeContext = (ContextObject) activeProcess.instVarAt0Slow(PROCESS.SUSPENDED_CONTEXT);
        activeProcess.instVarAtPut0Slow(PROCESS.SUSPENDED_CONTEXT, NilObject.SINGLETON);
        return ExecuteTopLevelContextNode.create(this, getLanguage(), activeContext, true);
    }

    @TruffleBoundary
    public DoItRootNode getDoItContextNode(final ParsingRequest request) {
        final Source source = request.getSource();
        lastParseRequestSource = source;
        final String sourceCode;
        if (isFileInFormat(source)) {
            sourceCode = String.format("[[ (FileStream readOnlyFileNamed: '%s') fileIn. true ] on: Error do: [ :e | Interop throwException: e ]]", source.getPath());
        } else {
            if (request.getArgumentNames().isEmpty()) {
                sourceCode = String.format("[ %s ]", source.getCharacters().toString());
            } else {
                sourceCode = String.format("[ :%s | %s ]", String.join(" :", request.getArgumentNames()), source.getCharacters().toString());
            }
        }
        return DoItRootNode.create(this, language, evaluateUninterruptably(sourceCode));
    }

    private static boolean isFileInFormat(final Source source) {
        final CharSequence firstLine = source.getCharacters(1);
        /* First line must end with an `!`. */
        return firstLine.charAt(firstLine.length() - 1) == '!';
    }

    @TruffleBoundary
    private ExecuteTopLevelContextNode getDoItContextNode(final String source, final boolean isExternalRequest) {
        /*
         * (Parser new parse: '1 + 2 * 3' class: UndefinedObject noPattern: true notifying: nil
         * ifFail: [^nil]) generate
         */

        if (parserSharedInstance == null) {
            parserSharedInstance = (PointersObject) ((ClassObject) lookup("Parser")).send(this, "new");
            final Object polyglotRequestorClassOrNil = lookup("PolyglotRequestor");
            if (polyglotRequestorClassOrNil instanceof final ClassObject polyglotRequestorClass) {
                requestorSharedInstanceOrNil = (AbstractSqueakObject) polyglotRequestorClass.send(this, "default");
            } else {
                requestorSharedInstanceOrNil = NilObject.SINGLETON;
            }
        }

        final NativeObject smalltalkSource = asByteString(source);
        if (requestorSharedInstanceOrNil != NilObject.SINGLETON) {
            ((AbstractSqueakObjectWithClassAndHash) requestorSharedInstanceOrNil).send(this, "currentSource:", smalltalkSource);
        }
        final PointersObject methodNode;
        try {
            methodNode = (PointersObject) parserSharedInstance.send(this, "parse:class:noPattern:notifying:ifFail:",
                            smalltalkSource, nilClass, BooleanObject.TRUE, requestorSharedInstanceOrNil, BlockClosureObject.create(this, blockClosureClass, 0));
        } catch (final ProcessSwitch e) {
            /*
             * A ProcessSwitch exception is thrown in case of a syntax error to open the
             * corresponding window. Fail with an appropriate exception here. This way, it is clear
             * why code execution failed (e.g. when requested through the Polyglot API).
             */
            throw CompilerDirectives.shouldNotReachHere("Unexpected process switch detected during parse request", e);
        }
        final CompiledCodeObject doItMethod = (CompiledCodeObject) methodNode.send(this, "generate");

        final ContextObject doItContext = ContextObject.create(this, doItMethod.getSqueakContextSize());
        doItContext.setReceiver(NilObject.SINGLETON);
        doItContext.setCodeObject(doItMethod);
        doItContext.setInstructionPointer(doItMethod.getInitialPC());
        doItContext.setStackPointer(doItMethod.getNumTemps());
        doItContext.setSenderUnsafe(isExternalRequest ? getInteropExceptionThrowingContext() : NilObject.SINGLETON);
        return ExecuteTopLevelContextNode.create(this, getLanguage(), doItContext, false);
    }

    /**
     * Returns a fake context for BlockClosure>>#on:do: that handles any exception (and may rethrow
     * it as Interop exception). This allows Smalltalk exceptions to be thrown to other languages,
     * so that they can catch them. The mechanism works essentially like this:
     *
     * <pre>
     * <code>[ ... ] on: Exception do: [ :e | "handle e" ]</code>
     * </pre>
     *
     * (see Context>>#handleSignal:)
     */
    public ContextObject getInteropExceptionThrowingContext() {
        if (interopExceptionThrowingContextPrototype == null) {
            assert evaluateUninterruptably("Interop") != NilObject.SINGLETON : "Interop class must be present";
            final CompiledCodeObject onDoMethod = (CompiledCodeObject) evaluateUninterruptably("BlockClosure>>#on:do:");
            interopExceptionThrowingContextPrototype = ContextObject.create(this, onDoMethod.getSqueakContextSize());
            interopExceptionThrowingContextPrototype.setCodeObject(onDoMethod);
            interopExceptionThrowingContextPrototype.setReceiver(NilObject.SINGLETON);
            /*
             * Need to catch all exceptions here. Otherwise, the contexts sender is used to find the
             * next handler context (see Context>>#nextHandlerContext).
             */
            interopExceptionThrowingContextPrototype.atTempPut(0, evaluateUninterruptably("Exception"));
            /*
             * Throw Error and Halt as interop, ignore warnings, handle all other exceptions the
             * usual way via UndefinedObject>>#handleSignal:.
             */
            interopExceptionThrowingContextPrototype.atTempPut(1, evaluateUninterruptably(
                            "[ :e | ((e isKindOf: Error) or: [ e isKindOf: Halt ]) ifTrue: [ Interop throwException: e \"rethrow as interop\" ] ifFalse: [(e isKindOf: Warning) ifTrue: [ e resume \"ignore\" ] " +
                                            "ifFalse: [ nil handleSignal: e \"handle the usual way\" ] ] ]"));
            interopExceptionThrowingContextPrototype.atTempPut(2, BooleanObject.TRUE);
            interopExceptionThrowingContextPrototype.setInstructionPointer(onDoMethod.getInitialPC() + CallPrimitiveNode.NUM_BYTECODES);
            interopExceptionThrowingContextPrototype.setStackPointer(onDoMethod.getNumTemps());
            interopExceptionThrowingContextPrototype.removeSender();
        }
        return interopExceptionThrowingContextPrototype.shallowCopy();
    }

    /*
     * CONTEXT STACK DEPTH MANAGEMENT
     */

    public boolean enteringContextExceedsDepth() {
        if (maxContextStackDepth == 0) {
            return false;
        } else {
            return ++currentContextStackDepth > maxContextStackDepth;
        }
    }

    public void exitingContext() {
        if (maxContextStackDepth != 0) {
            --currentContextStackDepth;
        }
    }

    public void resetContextStackDepth() {
        if (maxContextStackDepth != 0) {
            currentContextStackDepth = 0;
        }
    }

    /*
     * ACCESSING
     */

    public SqueakLanguage getLanguage() {
        return language;
    }

    public boolean getCurrentMarkingFlag() {
        return currentMarkingFlag;
    }

    public boolean toggleCurrentMarkingFlag() {
        return currentMarkingFlag = !currentMarkingFlag;
    }

    public void setHiddenRoots(final ArrayObject theHiddenRoots) {
        assert hiddenRoots == null && (theHiddenRoots.isObjectType() || isTesting());
        hiddenRoots = theHiddenRoots;
        assert validClassTableRootPages();
    }

    /* SpurMemoryManager>>#validClassTableRootPages */
    public boolean validClassTableRootPages() {
        if (!hiddenRoots.isObjectType()) {
            assert isTesting(); // Ignore dummy images for testing
            return true;
        }

        final Object[] hiddenRootsObjects = hiddenRoots.getObjectStorage();
        if (hiddenRootsObjects.length != SqueakImageConstants.CLASS_TABLE_ROOT_SLOTS + SqueakImageConstants.HIDDEN_ROOT_SLOTS) {
            return false;
        }
        // "is it in range?"
        // "are all pages the right size?"
        int numClassTablePages = -1;
        for (int i = 0; i < SqueakImageConstants.CLASS_TABLE_ROOT_SLOTS; i++) {
            final Object classPageOrNil = hiddenRootsObjects[i];
            if (classPageOrNil instanceof final ArrayObject classPage) {
                final int numSlots = classPage.isEmptyType() ? classPage.getEmptyLength() : classPage.getObjectLength();
                if (numSlots != SqueakImageConstants.CLASS_TABLE_PAGE_SIZE) {
                    return false;
                }
            } else {
                assert classPageOrNil == NilObject.SINGLETON;
                numClassTablePages = i;
            }
        }
        // "are all entries beyond numClassTablePages nil?"
        for (int i = numClassTablePages; i < SqueakImageConstants.CLASS_TABLE_ROOT_SLOTS; i++) {
            final Object classPageOrNil = hiddenRootsObjects[i];
            if (classPageOrNil != NilObject.SINGLETON) {
                return false;
            }
        }
        return true;
    }

    public ArrayObject getHiddenRoots() {
        return hiddenRoots;
    }

    /* SpurMemoryManager>>#enterIntoClassTable: */
    @TruffleBoundary
    public void enterIntoClassTable(final ClassObject clazz) {
        assert clazz.assertNotForwarded();
        int majorIndex = SqueakImageConstants.majorClassIndexOf(classTableIndex);
        final int initialMajorIndex = majorIndex;
        assert initialMajorIndex > 0 : "classTableIndex should never index the first page; it's reserved for known classes";
        int minorIndex = SqueakImageConstants.minorClassIndexOf(classTableIndex);
        while (true) {
            if (hiddenRoots.getObject(majorIndex) == NilObject.SINGLETON) {
                hiddenRoots.setObject(majorIndex, newClassTablePage());
                minorIndex = 0;
            }
            final ArrayObject page = (ArrayObject) hiddenRoots.getObject(majorIndex);
            for (int i = minorIndex; i < SqueakImageConstants.CLASS_TABLE_PAGE_SIZE; i++) {
                final Object entry = page.getObject(i);
                if (entry == NilObject.SINGLETON) {
                    classTableIndex = SqueakImageConstants.classTableIndexFor(majorIndex, i);
                    assert classTableIndex >= 1 << SqueakImageConstants.CLASS_TABLE_MAJOR_INDEX_SHIFT : "classTableIndex must never index the first page, which is reserved for classes known to the VM";
                    page.setObject(i, clazz);
                    clazz.setSqueakHash(classTableIndex);
                    assert lookupClassIndex(classTableIndex) == clazz;
                    return;
                }
            }
            majorIndex = Math.max(majorIndex + 1 & SqueakImageConstants.CLASS_INDEX_MASK, 1);
            assert majorIndex != initialMajorIndex : "wrapped; table full";
        }
    }

    private ArrayObject newClassTablePage() {
        return asArrayOfObjects(ArrayUtils.withAll(SqueakImageConstants.CLASS_TABLE_PAGE_SIZE, NilObject.SINGLETON));
    }

    private Object lookupClassIndex(final int classIndex) {
        final long majorIndex = SqueakImageConstants.majorClassIndexOf(classIndex);
        final Object classTablePageOrNil = hiddenRoots.getObject(majorIndex);
        if (classTablePageOrNil instanceof final ArrayObject classTablePage) {
            final long minorIndex = SqueakImageConstants.minorClassIndexOf(classIndex);
            return classTablePage.getObject(minorIndex);
        } else {
            assert classTablePageOrNil == NilObject.SINGLETON;
            return NilObject.SINGLETON;
        }
    }

    /* SpurMemoryManager>>#purgeDuplicateClassTableEntriesFor: */
    public void purgeDuplicateAndUnreachableClassTableEntriesFor(final ClassObject clazz, final UnmodifiableEconomicMap<Object, Object> becomeMap) {
        final int expectedIndex = clazz != null ? clazz.getSqueakHashInt() : -1;
        int majorIndex = SqueakImageConstants.majorClassIndexOf(SqueakImageConstants.CLASS_TABLE_PAGE_SIZE);
        while (majorIndex < SqueakImageConstants.CLASS_TABLE_ROOT_SLOTS) {
            final Object classTablePageOrNil = hiddenRoots.getObject(majorIndex);
            if (classTablePageOrNil instanceof final ArrayObject page) {
                for (int minorIndex = 0; minorIndex < SqueakImageConstants.CLASS_TABLE_PAGE_SIZE; minorIndex++) {
                    final int currentClassTableIndex = SqueakImageConstants.classTableIndexFor(majorIndex, minorIndex);
                    Object entry = page.getObject(minorIndex);
                    if (entry instanceof final ClassObject classObject && !classObject.isNotForwarded()) {
                        entry = classObject.getForwardingPointer();
                        page.setObject(minorIndex, entry);
                    }
                    final boolean isDuplicate = entry == clazz && currentClassTableIndex != expectedIndex && currentClassTableIndex > SqueakImageConstants.LAST_CLASS_INDEX_PUN;
                    if (isDuplicate || isUnreachable(entry)) {
                        page.setObject(minorIndex, NilObject.SINGLETON);
                        if (currentClassTableIndex < classTableIndex) {
                            classTableIndex = currentClassTableIndex;
                        }
                    } else if (entry instanceof final ClassObject classObject) {
                        classObject.pointersBecomeOneWay(becomeMap);
                    }
                }
            } else {
                assert classTablePageOrNil == NilObject.SINGLETON;
                break;
            }
            majorIndex = Math.max(majorIndex + 1 & SqueakImageConstants.CLASS_INDEX_MASK, 1);
        }
        assert classTableIndex >= SqueakImageConstants.CLASS_TABLE_PAGE_SIZE : "classTableIndex must never index the first page, which is reserved for classes known to the VM";
    }

    private boolean isUnreachable(final Object object) {
        if (object instanceof final ClassObject classObject) {
            /*
             * Class is unreachable if it was not yet marked with the current marking flag by the
             * last object graph traversal.
             */
            return classObject.tryToMarkWith(currentMarkingFlag);
        } else {
            assert object == NilObject.SINGLETON;
            return false;
        }
    }

    private void flushCachesForSelectorInClassTable(final NativeObject selector) {
        for (final Object classTablePageOrNil : hiddenRoots.getObjectStorage()) {
            if (classTablePageOrNil instanceof final ArrayObject page) {
                final Object[] entries = page.getObjectStorage();
                for (int i = 0; i < entries.length; i++) {
                    final Object entry = entries[i];
                    if (entry instanceof final ClassObject classObject) {
                        if (!classObject.isNotForwarded()) {
                            entries[i] = classObject.getForwardingPointer();
                        }
                        ((ClassObject) entries[i]).flushCachesForSelector(selector);
                    } else {
                        assert entry == NilObject.SINGLETON;
                    }
                }
            } else {
                assert classTablePageOrNil == NilObject.SINGLETON;
                break;
            }
        }
    }

    public void flushCachesForSelector(final NativeObject selector) {
        flushCachesForSelectorInClassTable(selector);
        flushMethodCacheForSelector(selector);
    }

    public TruffleFile getHomePath() {
        return homePath;
    }

    public NativeObject getResourcesDirectory() {
        ensureResourcesDirectoryAndPathInitialized();
        return NativeObject.newNativeBytes(this, byteStringClass, resourcesDirectoryBytes.clone());
    }

    public NativeObject getResourcesPath() {
        ensureResourcesDirectoryAndPathInitialized();
        return NativeObject.newNativeBytes(this, byteStringClass, resourcesPathBytes.clone());
    }

    private void ensureResourcesDirectoryAndPathInitialized() {
        if (resourcesDirectoryBytes == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            final String languageHome = getLanguage().getTruffleLanguageHome();
            final TruffleFile path;
            if (languageHome != null) {
                path = getHomePath().resolve("resources");
            } else { /* Fallback to image directory. */
                path = env.getInternalTruffleFile(getImagePath()).getParent();
                if (path == null) {
                    throw SqueakException.create("`parent` should not be `null`.");
                }
            }
            resourcesDirectoryBytes = MiscUtils.stringToBytes(path.getAbsoluteFile().getPath());
            resourcesPathBytes = MiscUtils.stringToBytes(path.getAbsoluteFile().getPath() + env.getFileNameSeparator());
        }
    }

    public NativeObject getDebugErrorSelector() {
        return debugErrorSelector;
    }

    public void setDebugErrorSelector(final NativeObject nativeObject) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        assert debugErrorSelector == null;
        debugErrorSelector = nativeObject;
    }

    public NativeObject getDebugSyntaxErrorSelector() {
        return debugSyntaxErrorSelector;
    }

    public void setDebugSyntaxErrorSelector(final NativeObject nativeObject) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        assert debugSyntaxErrorSelector == null;
        debugSyntaxErrorSelector = nativeObject;
    }

    public ClassObject getByteSymbolClass() {
        return byteSymbolClass;
    }

    public void setByteSymbolClass(final ClassObject classObject) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        assert byteSymbolClass == null;
        byteSymbolClass = classObject;
    }

    public ClassObject getWideStringClassOrNull() {
        return wideStringClass;
    }

    public ClassObject getWideStringClass() {
        if (wideStringClass == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            // TODO: find a better way to find wideStringClass or do this on image side instead?
            final CompiledCodeObject method = (CompiledCodeObject) LookupMethodByStringNode.executeUncached(byteArrayClass, "asWideString");
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

    public ClassObject getForeignObjectClass() {
        assert foreignObjectClass != null;
        return foreignObjectClass;
    }

    public Assumption getForeignObjectClassStableAssumption() {
        return foreignObjectClassStable.getAssumption();
    }

    public boolean setForeignObjectClass(final ClassObject classObject) {
        foreignObjectClassStable.invalidate("New foreign object class");
        foreignObjectClass = classObject;
        return true;
    }

    public ClassObject getLinkedListClass() {
        if (linkedListClass == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            final Object lists = getScheduler().instVarAt0Slow(PROCESS_SCHEDULER.PROCESS_LISTS);
            linkedListClass = SqueakObjectClassNode.executeUncached(((ArrayObject) lists).getObject(0));
        }
        return linkedListClass;
    }

    public boolean supportsNFI() {
        CompilerAsserts.neverPartOfCompilation();
        return env.getInternalLanguages().containsKey("nfi");
    }

    public InterpreterProxy getInterpreterProxy(final MaterializedFrame frame, final int numReceiverAndArguments) {
        if (interpreterProxy == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            interpreterProxy = new InterpreterProxy(this);
        }
        return interpreterProxy.instanceFor(frame, numReceiverAndArguments);
    }

    public PointersObject getScheduler() {
        if (scheduler == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            scheduler = (PointersObject) schedulerAssociation.instVarAt0Slow(ASSOCIATION.VALUE);
        }
        return scheduler;
    }

    public PointersObject getActiveProcessSlow() {
        return AbstractPointersObjectReadNode.getUncached().executePointers(null, getScheduler(), PROCESS_SCHEDULER.ACTIVE_PROCESS);
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
        return MiscUtils.toIntExact((long) getSpecialSelectors().getObjectStorage()[index * 2 + 1]);
    }

    public void setSemaphore(final int index, final AbstractSqueakObject semaphore) {
        assert semaphore == NilObject.SINGLETON || isSemaphoreClass(((AbstractSqueakObjectWithClassAndHash) semaphore).getSqueakClass());
        setSpecialObject(index, semaphore);
    }

    /**
     * Ensure the active process is saved and try to signal low space semaphore (see
     * #setSignalLowSpaceFlagAndSaveProcess). The JVM has just thrown a {@link StackOverflowError},
     * so thread stack space is limited. To avoid hitting the limit again, free up some space by
     * unwinding a couple of sends before actually signaling the low space semaphore.
     */
    public StackOverflowError tryToSignalLowSpace(final VirtualFrame frame, final StackOverflowError stackOverflowError) {
        CompilerAsserts.neverPartOfCompilation();
        final Object lastSavedProcess = getSpecialObject(SPECIAL_OBJECT.PROCESS_SIGNALING_LOW_SPACE);
        if (lastSavedProcess == NilObject.SINGLETON) {
            setSpecialObject(SPECIAL_OBJECT.PROCESS_SIGNALING_LOW_SPACE, getActiveProcessSlow());
        }
        if (lowSpaceSkippedSendsCount < LOW_SPACE_NUM_SKIPPED_SENDS) {
            lowSpaceSkippedSendsCount++;
            throw stackOverflowError; // continue further up the sender chain
        } else {
            final Object lowSpaceSemaphoreOrNil = getSpecialObject(SPECIAL_OBJECT.THE_LOW_SPACE_SEMAPHORE);
            if (SignalSemaphoreNodeGen.executeUncached(frame, this, lowSpaceSemaphoreOrNil)) {
                // success! reset counter and continue in new process
                lowSpaceSkippedSendsCount = 0;
                throw ProcessSwitch.SINGLETON;
            }
            throw CompilerDirectives.shouldNotReachHere("Failed to signal low space semaphore.", stackOverflowError);
        }
    }

    public boolean hasDisplay() {
        return display != null;
    }

    public SqueakDisplay getDisplay() {
        return display;
    }

    public String getImagePath() {
        if (imagePath == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            setImagePath(SqueakImageLocator.findImage(options.imagePath()));
        }
        return imagePath;
    }

    public void setImagePath(final String path) {
        imagePath = path;
    }

    public String[] getImageArguments() {
        if (options.imageArguments().length > 0) {
            return options.imageArguments();
        } else {
            return env.getApplicationArguments();
        }
    }

    public AbstractSqueakObject getSystemAttribute(final int index) {
        return systemAttributes.getSystemAttribute(index);
    }

    public Source getLastParseRequestSource() {
        return lastParseRequestSource;
    }

    public boolean interruptHandlerDisabled() {
        return options.disableInterruptHandler();
    }

    public boolean isHeadless() {
        return isHeadless;
    }

    public void attachDisplayIfNecessary() {
        if (!isHeadless) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            display = SqueakDisplay.create(this);
        }
    }

    public boolean isTesting() {
        return options.isTesting();
    }

    public void finalizeContext() {
        if (options.printResourceSummary()) {
            MiscUtils.printResourceSummary();
        }
    }

    public int getPrimFailCode() {
        assert primFailCode >= 0;
        final int result = primFailCode;
        primFailCode = 0;
        return result;
    }

    public void setPrimFailCode(final PrimitiveFailed primitiveFailed) {
        primFailCode = primitiveFailed.getPrimFailCode();
    }

    @TruffleBoundary
    public Object getScope() {
        ensureLoaded();
        if (smalltalkScope == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            smalltalkScope = smalltalk.send(this, "asInteropScope");
        }
        return smalltalkScope;
    }

    /*
     * METHOD CACHE
     */

    private void initializeMethodCache() {
        for (int i = 0; i < METHOD_CACHE_SIZE; i++) {
            methodCache[i] = new MethodCacheEntry();
        }
    }

    /*
     * Probe the cache, and return the matching entry if found. Otherwise return one that can be
     * used (selector and class set) with method == null. Initial probe is class xor selector,
     * reprobe delta is selector. We do not try to optimize probe time -- all are equally 'fast'
     * compared to lookup. Instead we randomize the reprobe so two or three very active conflicting
     * entries will not keep dislodging each other.
     */
    @ExplodeLoop
    public MethodCacheEntry findMethodCacheEntry(final ClassObject classObject, final NativeObject selector) {
        methodCacheRandomish = methodCacheRandomish + 1 & 3;
        final int selectorHash = System.identityHashCode(selector);
        int firstProbe = (System.identityHashCode(classObject) ^ selectorHash) & METHOD_CACHE_MASK;
        int probe = firstProbe;
        for (int i = 0; i < METHOD_CACHE_REPROBES; i++) {
            final MethodCacheEntry entry = methodCache[probe];
            if (entry.getClassObject() == classObject && entry.getSelector() == selector) {
                return entry;
            }
            if (i == methodCacheRandomish) {
                firstProbe = probe;
            }
            probe = probe + selectorHash & METHOD_CACHE_MASK;
        }
        return methodCache[firstProbe].reuseFor(classObject, selector);
    }

    public Object lookup(final ClassObject receiverClass, final NativeObject selector) {
        final MethodCacheEntry cachedEntry = findMethodCacheEntry(receiverClass, selector);
        if (cachedEntry.getResult() == null) {
            cachedEntry.setResult(receiverClass.lookupInMethodDictSlow(selector));
        }
        return cachedEntry.getResult();
    }

    /* Clear all cache entries (prim 89). */
    public void flushMethodCache() {
        for (int i = 0; i < METHOD_CACHE_SIZE; i++) {
            methodCache[i].freeAndRelease();
        }
    }

    /* Clear cache entries for selector (prim 119). */
    private void flushMethodCacheForSelector(final NativeObject selector) {
        for (int i = 0; i < METHOD_CACHE_SIZE; i++) {
            if (methodCache[i].getSelector() == selector) {
                methodCache[i].freeAndRelease();
            }
        }
    }

    /* Clear cache entries for method (prim 116). */
    public void flushMethodCacheForMethod(final CompiledCodeObject method) {
        for (int i = 0; i < METHOD_CACHE_SIZE; i++) {
            if (methodCache[i].getResult() == method) {
                methodCache[i].freeAndRelease();
            }
        }
    }

    public void flushMethodCacheAfterBecome() {
        /* TODO: Could be selective by checking class, selector, and method against mutations. */
        flushMethodCache();
    }

    /*
     * CLASS CHECKS
     */

    public boolean isBitmapClass(final ClassObject object) {
        return object == bitmapClass;
    }

    public boolean isBlockClosureClass(final ClassObject object) {
        return object == blockClosureClass;
    }

    public boolean isByteStringClass(final ClassObject object) {
        return object == byteStringClass;
    }

    public boolean isByteSymbolClass(final ClassObject object) {
        return object == getByteSymbolClass();
    }

    public boolean isFloatClass(final ClassObject object) {
        return object == floatClass;
    }

    public boolean isFullBlockClosureClass(final ClassObject object) {
        return object == fullBlockClosureClass;
    }

    public boolean isLargeIntegerClass(final ClassObject object) {
        return object == largePositiveIntegerClass || object == largeNegativeIntegerClass;
    }

    public boolean isLargeInteger(final NativeObject object) {
        return isLargeIntegerClass(object.getSqueakClass());
    }

    public boolean isLargePositiveInteger(final NativeObject object) {
        return object.getSqueakClass() == largePositiveIntegerClass;
    }

    public boolean isLargeNegativeInteger(final NativeObject object) {
        return object.getSqueakClass() == largeNegativeIntegerClass;
    }

    public boolean isMetaClass(final ClassObject object) {
        return object == metaClass;
    }

    public boolean isMethodContextClass(final ClassObject object) {
        return object == methodContextClass;
    }

    public boolean isNilClass(final ClassObject object) {
        return object == nilClass;
    }

    public boolean isPointClass(final ClassObject object) {
        return object == pointClass;
    }

    public boolean isSemaphoreClass(final ClassObject object) {
        return object == semaphoreClass;
    }

    /*
     * INSTANCE CREATION
     */

    public ArrayObject asArrayOfLongs(final long... elements) {
        return ArrayObject.createWithStorage(this, arrayClass, elements);
    }

    public ArrayObject asArrayOfObjects(final Object... elements) {
        return ArrayObject.createWithStorage(this, arrayClass, elements);
    }

    public ClassObject getFractionClass() {
        if (fractionClass == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            final Object fractionLookup = lookup("Fraction");
            if (fractionLookup instanceof final ClassObject c) {
                fractionClass = c;
            } else {
                throw SqueakException.create("Unable to find Fraction class");
            }
        }
        return fractionClass;
    }

    public PointersObject asFraction(final long numerator, final long denominator, final AbstractPointersObjectWriteNode writeNode, final Node inlineTarget) {
        final long actualNumerator;
        final long actualDenominator;
        if (denominator < 0) { // "keep sign in numerator"
            actualNumerator = -numerator;
            actualDenominator = -denominator;
        } else {
            actualNumerator = numerator;
            actualDenominator = denominator;
        }
        // Calculate gcd
        long n = actualNumerator;
        long m = actualDenominator;
        while (n != 0) {
            n = m % (m = n);
        }
        final long gcd = Math.abs(m);
        // Instantiate reduced fraction
        final PointersObject fraction = new PointersObject(this, getFractionClass(), fractionClass.getLayout());
        writeNode.execute(inlineTarget, fraction, FRACTION.NUMERATOR, actualNumerator / gcd);
        writeNode.execute(inlineTarget, fraction, FRACTION.DENOMINATOR, actualDenominator / gcd);
        return fraction;
    }

    public static double fromFraction(final PointersObject fraction, final AbstractPointersObjectReadNode readNode, final Node inlineTarget) {
        assert SqueakGuards.isFraction(fraction, inlineTarget);
        final long numerator = readNode.executeLong(inlineTarget, fraction, ObjectLayouts.FRACTION.NUMERATOR);
        final double denominator = readNode.executeLong(inlineTarget, fraction, ObjectLayouts.FRACTION.DENOMINATOR);
        return numerator / denominator;
    }

    public NativeObject asByteArray(final byte[] bytes) {
        return NativeObject.newNativeBytes(this, byteArrayClass, bytes);
    }

    public NativeObject asByteString(final String value) {
        return NativeObject.newNativeBytes(this, byteStringClass, MiscUtils.stringToBytes(value));
    }

    public NativeObject asByteSymbol(final String value) {
        CompilerAsserts.neverPartOfCompilation();
        return (NativeObject) asByteString(value).send(this, "asSymbol");
    }

    private NativeObject asWideString(final String value) {
        return NativeObject.newNativeInts(this, getWideStringClass(), MiscUtils.stringToCodePointsArray(value));
    }

    public NativeObject asString(final String value, final InlinedConditionProfile wideStringProfile, final Node node) {
        return wideStringProfile.profile(node, NativeObject.needsWideString(value)) ? asWideString(value) : asByteString(value);
    }

    public PointersObject asPoint(final AbstractPointersObjectWriteNode writeNode, final Node inlineTarget, final Object xPos, final Object yPos) {
        final PointersObject point = new PointersObject(this, pointClass, null);
        writeNode.execute(inlineTarget, point, POINT.X, xPos);
        writeNode.execute(inlineTarget, point, POINT.Y, yPos);
        return point;
    }

    public ArrayObject newEmptyArray() {
        return ArrayObject.createWithStorage(this, arrayClass, ArrayUtils.EMPTY_ARRAY);
    }

    public PointersObject newMessage(final AbstractPointersObjectWriteNode writeNode, final Node inlineTarget, final NativeObject selector, final ClassObject lookupClass, final Object[] arguments) {
        final PointersObject message = new PointersObject(this, messageClass, null);
        writeNode.execute(inlineTarget, message, MESSAGE.SELECTOR, selector);
        writeNode.execute(inlineTarget, message, MESSAGE.ARGUMENTS, asArrayOfObjects(arguments));
        assert message.instsize() > MESSAGE.LOOKUP_CLASS : "Early versions do not have lookupClass";
        writeNode.execute(inlineTarget, message, MESSAGE.LOOKUP_CLASS, lookupClass);
        return message;
    }

    /*
     * INTEROP
     */

    @TruffleBoundary
    public NativeObject toInteropSelector(final Message message) {
        assert message.getLibraryClass() == InteropLibrary.class;
        return interopMessageToSelectorMap.computeIfAbsent(message, m -> {
            final String libraryName = message.getLibraryClass().getSimpleName();
            assert libraryName.endsWith("Library");
            final String libraryPrefix = libraryName.substring(0, 1).toLowerCase() + libraryName.substring(1, libraryName.length() - 7);
            final String messageName = message.getSimpleName();
            final String messageCapitalized = messageName.substring(0, 1).toUpperCase() + messageName.substring(1);
            final String suffix;
            switch (message.getParameterCount()) {
                case 1 -> suffix = "";
                case 2 -> suffix = ":";
                default -> {
                    final StringBuilder sb = new StringBuilder(":");
                    for (int i = 0; i < message.getParameterCount() - 2; i++) {
                        sb.append("and:");
                    }
                    suffix = sb.toString();
                }
            }
            return asByteSymbol(libraryPrefix + messageCapitalized + suffix);
        });
    }

    /*
     * INSTRUMENTATION
     */

    public <T> T reportAllocation(final T value) {
        if (allocationReporter.isActive()) {
            allocationReporter.onEnter(null, 0, AllocationReporter.SIZE_UNKNOWN);
            allocationReporter.onReturnValue(value, 0, AllocationReporter.SIZE_UNKNOWN);
        }
        return value;
    }
}
