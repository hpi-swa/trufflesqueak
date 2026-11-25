[
    | repoPath isLinux isMacOS isWin alphabetically ignoredTests failingTests flakyTests testSuite result retries exitCode |

    ToolSet default: CommandLineToolSet.

    FileStream stdout nextPutAll: 'Setting author information for testing ...'; cr; flush.
    Utilities
        authorName: 'TruffleSqueak';
        setAuthorInitials: 'TS'.

    FileStream stdout nextPutAll: 'Patching TestCase ...'; cr; flush.
    TestCase compile: 'run: aResult
        FileStream stdout nextPutAll: self asString; nextPutAll: '' ... ''; flush.
        aResult runCase: self.
        FileStream stdout nextPutAll: ''('';
                          nextPutAll: ((aResult durations at: self) ifNil: [0]) asString;
                          nextPutAll: ''ms)''; cr; flush.'
            classified: 'patched'.
    TestCase compile: 'isLogging
        ^ true'
            classified: 'patched'.
    TestCase compile: 'failureLog
        ^ FileStream stdout'
            classified: 'patched'.
    TestCase compile: 'defaultTimeout
        "Use the same timeout on all platforms (default is 25s if Smalltalk isLowerPerformance, 5s otherwise)."
        ^ 10 "seconds"'
            classified: 'patched'.

    isLinux := Smalltalk platformName = 'unix'.
    isMacOS := Smalltalk platformName = 'Mac OS'.
    isWin := Smalltalk platformName = 'Win32'.

    repoPath := (((FileDirectory on: Smalltalk documentPath) containingDirectory containingDirectory) / 'image' / 'src') fullName.
    (FileDirectory default directoryExists: repoPath) ifFalse: [
        FileStream stdout cr; cr; nextPutAll: 'TruffleSqueak packages not found at "', repoPath, '". EXITING'; cr; cr; flush.
        Smalltalk quitPrimitive: 1].
    FileStream stdout nextPutAll: 'Loading TruffleSqueak packages from "', repoPath, '" ...'; cr; flush.
    [[[ | mc |
        mc := MCFileTreeRepository path: repoPath.
        Installer monticello
            mc: mc;
            packages: mc allPackageNames;
            install ]
                on: Warning do: [ :w | w resume ]]
                on: Error do: [ :e | e retry ]]
                on: ProgressInitiationException do: [ :e |
                    e isNested
                        ifTrue: [ e pass ]
                        ifFalse: [ e rearmHandlerDuring:
                            [[ e sendNotificationsTo: [ :min :max :current | "silence" ]]
                                on: ProgressNotification do: [ :notification | notification resume ]]]].

    (Smalltalk at: #Polyglot) initialize.
    (Smalltalk at: #TruffleSqueakUtilities) setUpAfterLoadingPackages.

    alphabetically := [:a :b | a className < b className or: [ a class == b class and: [a selector < b selector] ] ].

    ignoredTests := OrderedCollection new.
    {
        "slow tests"
        CompiledMethodComparisonTest. "takes more than 30s"

        "ignored tests"
        AioEventHandlerTestCase. "AioPlugin not available"
        CogARMCompilerTests. "require processor simulators (e.g., BochsIA32Alien) that are missing in the image"
        CogARMv8CompilerTests. "require processor simulators (e.g., BochsIA32Alien) that are missing in the image"
        CogIA32CompilerTests. "require processor simulators (e.g., BochsIA32Alien) that are missing in the image"
        CogMIPSELCompilerTests. "require processor simulators (e.g., BochsIA32Alien) that are missing in the image"
        CogX64CompilerTests. "require processor simulators (e.g., BochsIA32Alien) that are missing in the image"
        FFIAllocateExternalTests. "requires external memory (primitiveFFIAllocate)"
        FFIPluginConstructedTests. "FFI callouts not supported"
        FFIPluginLibraryTests. "FFI callouts not supported"
        FFIPluginTests. "FFI callouts not supported"
        FileSystemGitRepositoryRemoteTests. "requires internet access"
        GitDumbHTTPProtocolTest. "requires internet access"
        GitProtocolTest. "requires internet access"
        GitSmartHTTPProtocolTest. "requires internet access"
        ImageSegmentTest. "image segments not supported"
        ReleaseTest. "slow, and #testUndeclared fails due to the packages loaded into the test image"
        SlangBasicTypeInferenceTest. "inferred types differ from expectation in 18 of 71 tests (also on the OpenSmalltalk VM)"
        SlangTypeForArithmeticTest. "inferred types differ from expectation (also on the OpenSmalltalk VM)"
        SlangTypeForDereferenceTest. "inferred types differ from expectation in 2 of 5 tests (also on the OpenSmalltalk VM)"
        SpurPlanningCompactorTests. "VMMaker simulator cannot be initialized, PrintfFormatDescriptor is missing in the image"
        UIManagerTest. "fails due to the packages loaded into the test image"
        UnixProcessTestCase. "requires forking the VM"
        WebMessageTest. "#testStreamDirectlyFromToSizeProgressWhenSizeIsNotSpecified times out"
        WriteBarrierTest. "requires supportsReadOnlyObjects"
    } collect: [:testCase |
        testCase allTestSelectors do: [:sel | ignoredTests add: (testCase selector: sel) ]].

    isWin ifTrue: [
        {
            "ignored tests"
            UnixProcessAccessorTestCase.
            UnixProcessUnixFileLockingTestCase.
            UnixProcessWin32FileLockingTestCase.
        } collect: [:testCase |
            testCase allTestSelectors do: [:sel | ignoredTests add: (testCase selector: sel) ]].
    ].

    (isLinux or: [ isWin ]) ifTrue: [
        {
            SqueakSSLTest. "known OSVM issue"
        } collect: [:testCase |
            testCase allTestSelectors do: [:sel | ignoredTests add: (testCase selector: sel) ]].
    ].

    {
        "ignored tests"
        BitmapStreamTests -> #(#testMatrixTransform2x3WithImageSegment #testShortIntegerArrayWithImageSegment #testShortPointArrayWithImageSegment #testShortRunArrayWithImageSegment #testWordArrayWithImageSegment). "image segments not supported"
        CompiledCodeInspectorTest -> #(#testDebugConstruction). "times out while inspecting a WeakSet (takes 4s on the OpenSmalltalk VM)"
        DebuggerTests -> #(#test01UserInterrupt #test17HandleBlockCannotReturn #test18HandleNestedBlockCannotReturn #test20TerminateProcess #test23TerminateDoubleErrorInEnsure #test24TerminateTripleErrorInEnsure). "require a debugger UI, fail or lock up with CommandLineToolSet (also on the OpenSmalltalk VM)"
        FFIAllocateTests -> #(#test22ArrayForVoidPointers). "requires external memory (primitiveFFIAllocate)"
        FileSystemGitTests -> #(#testEntrySize). "opens a dialog and waits for user input"
        HtmlReadWriterTest -> #(#test16ImgTag). "requires internet access to download an image"
        LocaleTest -> #(#test04InstallFont). "requires internet access to download a font"
        SlangTests -> #(#testSimpleMethod). "generated code differs from expectation (also on the OpenSmalltalk VM)"
        SlangTypePromotionTest -> #(#testFloatAndNil #testNilAndFloat). "inferred types differ from expectation (also on the OpenSmalltalk VM)"
        StringTest -> #(#testPercentEncodingJa). "slow, switches the locale six times and may exceed its timeout of 30s on CI"
        UnixProcessAccessorTestCase -> #(#testIsExecutableForUserInGroup #testIsReadable #testIsReadableForUserInGroup #testIsWritable #testIsWritableForUserInGroup). "time out waiting for a command, requires forking the VM"
    } collect: [:assoc | | testCase |
        testCase := assoc key.
        assoc value do: [:sel | ignoredTests add: (testCase selector: sel) ]].

    failingTests := OrderedCollection new.
    {
        CoInterpreterTests. "VMMaker simulator cannot be initialized, PrintfFormatDescriptor is missing in the image"
        FormatNumberTests. "VMMaker simulator cannot be initialized, PrintfFormatDescriptor is missing in the image"
        FullSimulationTest. "VMMaker simulator cannot be initialized, PrintfFormatDescriptor is missing in the image"
        InterpreterPrimitivesTest. "64-bit values are SmallIntegers, which do not understand #minus64:"
        RegisterAllocatingCogitTests. "VMMaker simulator cannot be initialized, PrintfFormatDescriptor is missing in the image"
        SpurImageSegmentTests. "VMMaker simulator cannot be initialized, PrintfFormatDescriptor is missing in the image"
        ToolMenusTest. "PolyglotNotebook>>#buildCodePaneWith: answers a spec without a menu"
        VMMakerDecompilerTests. "some VMMaker methods do not decompile to equivalent code"
    } collect: [:testCase |
        testCase allTestSelectors do: [:sel | failingTests add: (testCase selector: sel) ]].

    isWin ifTrue: [
        {
            OSPipeTestCase. "OS pipes not supported"
            Win32VMTest.
        } collect: [:testCase |
            testCase allTestSelectors do: [:sel | failingTests add: (testCase selector: sel) ]].
    ].

    {
        AllocationTest -> #(#testOutOfMemorySignal). "allocating huge empty Arrays does not signal OutOfMemory"
        BitBltTest -> #(#testAllAlphasRgbMul). "expects the rounding of an outdated BitBltSimulation>>#rgbMul:with:, as implemented by the VM of the Squeak 6.0 release"
        CompilerExceptionsTest -> #(#testUnknownSelector). "selects the wrong interval, flagged as flaky in the image"
        HelpBrowserTest -> #(#testRegistration). "the Help Browser is not registered in the world menu of the test image"
        IslandVMTweaksTestCase -> #(#testEmptyReplace #testForgivingPrims). "primitives that take their receiver as an argument are not supported"
        ObjectTest -> #(#testEvaluateWheneverChangeIn #testEvaluateWheneverChangeInTransparent). "become EmptyObject->PointersObject not yet supported"
        PackageDependencyTest -> #(#testBalloon #testPreferenceBrowser #testSUnit). "packages loaded into the test image add dependencies"
        SequenceableCollectionTest -> #(#testDoWithIndexDeprecation). "packages loaded into the test image send #doWithIndex:"
        SmallIntegerTest -> #(#testMaxVal #testMinVal #testPrintString). "assume SmallIntegers are 31 or 61 bit rather than 64 bit (long)."
        StackInterpreterSimulatorTests -> #(#testEmptySimulatorCanCloneSimulation #testEmptySimulatorCloneCanLoadImage #testPluginSimulators #testSimulatorCanReloadImage). "VMMaker simulator cannot be initialized, PrintfFormatDescriptor is missing in the image"
        StackInterpreterTests -> #(#testImmediateFloats #testPointerTaggingDetagging). "VMMaker simulator cannot be initialized, PrintfFormatDescriptor is missing in the image"
        WorldStateTest -> #(#testActiveVariablesObsoletion #testWorldVariableObsoletion). "packages loaded into the test image reference the variables"
    } collect: [:assoc | | testCase |
        testCase := assoc key.
        assoc value do: [:sel | failingTests add: (testCase selector: sel) ]].

    isWin ifFalse: [
        {
            OSPipeTestCase -> #(#testIsAtEndOfFile). "also fails on the OpenSmalltalk VM"
            UnixProcessAccessorTestCase -> #(#testArgsAsFlatArrayAndOffsets #testEnvAsFlatArrayAndOffsets). "require memory addresses of objects"
            UnixProcessAccessorTestCase -> #(#testDupTo #testRedirectStdOutTo #testUnixFileNumber). "require file descriptors"
            UnixProcessAccessorTestCase -> #(#testPrimForkAndExec #testPrimGetChildExitStatus). "require forking the VM"
        } collect: [:assoc | | testCase |
            testCase := assoc key.
            assoc value do: [:sel | failingTests add: (testCase selector: sel) ]].
    ].

    flakyTests := OrderedCollection new.
    {
        ArbitraryObjectSocketTestCase -> #(#testBasics). "delays too short, see https://github.com/squeak-smalltalk/squeak-object-memory/issues/179"
        BitBltSimulationTest -> #(#testAlphaCompositing2Simulated #testAlphaCompositingSimulated). "simulated primitive fails if the JVM collects garbage while loading BitBlt state (see BitBltSimulation>>#loadBitBltFrom:warping:)"
        DebuggerTests -> #(#test02UnhandledException). "fails on most CI jobs except on Linux aarch64, cause unknown"
        FloatTest -> #(#testCompiledConstants). "recompiles all methods with float literals, may exceed the default timeout on slower CI runners"
        MorphicToolBuilderTests -> #(#testGetButtonColor). "fails if the hand is over 0@0, where tests create morphs (e.g., when headless or depending on mouse events)"
        ObjectTest -> #(#testIfNotNilDoDeprecation). "scans all methods three times, may exceed the default timeout on slower CI runners"
        SHParserST80Test -> #(#testNumbers). "fails rarely, cause unknown"
        StringSocketTestCase -> #(#testBogusInput1 #testBogusInput2). "delays too short, see https://github.com/squeak-smalltalk/squeak-object-memory/issues/179"
        WeakSetInspectorTest -> #(#testSymbolTableM6812). "symbol table can shrink between garbage collections, fails on Windows native and often in the instrumented build"
    } collect: [:assoc | | testCase |
        testCase := assoc key.
        assoc value do: [:sel | flakyTests add: (testCase selector: sel) ]].

    isMacOS ifTrue: [
        {
            SocketTest -> #(#testSocketReuse). "fails on GitHub's macOS runners, passes locally"
        } collect: [:assoc | | testCase |
            testCase := assoc key.
            assoc value do: [:sel | flakyTests add: (testCase selector: sel) ]].
    ].

    isWin ifTrue: [
        {
            DurationTest -> #(#testBusyWait #testBusyWaitMicro). "Fails on Windows native"
        } collect: [:assoc | | testCase |
            testCase := assoc key.
            assoc value do: [:sel | flakyTests add: (testCase selector: sel) ]].
    ].

    testSuite := TestCase buildSuite.
    testSuite tests removeAllSuchThat: [:ea | ignoredTests anySatisfy: [:t | ea class == t class and: [ ea selector == t selector ]]].
    testSuite tests removeAllSuchThat: [:ea | failingTests anySatisfy: [:t | ea class == t class and: [ ea selector == t selector ]]].
    testSuite tests removeAllSuchThat: [:ea | flakyTests   anySatisfy: [:t | ea class == t class and: [ ea selector == t selector ]]].
    testSuite tests sort: alphabetically.

    FileStream stdout nextPutAll: 'Ignored TestCases: ', ignoredTests size; cr; flush.
    FileStream stdout nextPutAll: 'Failing TestCases: ', failingTests size; cr; flush.
    FileStream stdout nextPutAll: 'Flaky TestCases:   ', flakyTests size; cr; flush.
    FileStream stdout nextPutAll: 'Passing TestCases: ', testSuite tests size; cr; flush.

    result := testSuite run.
    FileStream stdout cr; nextPutAll: result asString; cr; flush.

    exitCode := result hasPassed ifTrue: [ 0 ] ifFalse: [ 1 ].

    "Persist image before retrying failures"
    Smalltalk snapshot: true andQuit: false.

    retries := 3.
    [ exitCode == 1 and: [ retries > 0 ] ] whileTrue: [
        FileStream stdout cr; nextPutAll: 'Retry failures or errors:'; cr; flush.
        testSuite := TestSuite new.
        testSuite tests addAll: result failures.
        testSuite tests addAll: result errors.
        result := testSuite run.
        FileStream stdout cr; nextPutAll: result asString; cr; flush.
        exitCode := result hasPassed ifTrue: [ 0 ] ifFalse: [ 1 ].
        retries := retries - 1.
    ].

    FileStream stdout cr; cr; nextPutAll: 'Running failing TestCases...'; cr; cr; flush.
    testSuite := TestSuite new.
    testSuite tests addAll: failingTests.
    testSuite tests sort: alphabetically.
    result := testSuite run.
    FileStream stdout cr; nextPutAll: result asString; cr; flush.
    result passed ifNotEmpty: [:passed |
        exitCode := exitCode bitOr: 16.
        FileStream stdout cr; nextPutAll: 'TestCases marked as failing that passed:'; cr; flush.
        passed do: [:testCase |
            FileStream stdout cr; nextPutAll: testCase asString; flush.
        ].
    ].

    FileStream stdout cr; cr; nextPutAll: 'Running flaky TestCases...'; cr; cr; flush.
    testSuite := TestSuite new.
    testSuite tests addAll: flakyTests.
    testSuite tests sort: alphabetically.
    result := testSuite run.
    FileStream stdout cr; nextPutAll: result asString; cr; flush.
    result failures ifNotEmpty: [:failures |
        FileStream stdout cr; nextPutAll: 'Flaky failures:'; cr; flush.
        failures do: [:testCase |
            FileStream stdout cr; nextPutAll: testCase asString; flush.
        ].
    ].
    result errors ifNotEmpty: [:errors |
        FileStream stdout cr; nextPutAll: 'Flaky errors:'; cr; flush.
        errors do: [:testCase |
            FileStream stdout cr; nextPutAll: testCase asString; flush.
        ].
    ].

    Smalltalk quitPrimitive: exitCode
]
on: Error
do: [ :ex |
   FileStream stdout cr; cr; nextPutAll: 'Encountered error during test: '; cr; cr; flush.
   ex printVerboseOn: FileStream stdout.
   FileStream stdout cr; cr; flush.
   Smalltalk quitPrimitive: 1
]
