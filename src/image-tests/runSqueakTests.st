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
        "ignored tests"
        AioEventHandlerTestCase.
        CogARMCompilerTests.
        CogARMv8CompilerTests.
        CogIA32CompilerTests.
        CogMIPSELCompilerTests.
        CogX64CompilerTests.
        FFIAllocateExternalTests.
        FFIAllocateTests.
        FFIPluginConstructedTests.
        FFIPluginLibraryTests.
        FFIPluginTests.
        FileSystemGitRepositoryRemoteTests.
        FileSystemGitTests.
        FloatMathPluginTests.
        FullSimulationTest.
        GitDumbHTTPProtocolTest.
        GitProtocolTest.
        GitSmartHTTPProtocolTest.
        ImageSegmentTest. "image segments not supported"
        InterpreterPrimitivesTest.
        IslandVMTweaksTestCase.
        LangEnvBugs.
        SlangBasicTypeInferenceTest. "many tests broken"
        SlangTypeForArithmeticTest.
        SlangTypeForDereferenceTest.
        SoundTest.
        SpurImageSegmentTests.
        SpurPlanningCompactorTests.
        StackInterpreterSimulatorTests.
        UIManagerTest.
        UnicodeTest. "Needs external resources"
        UnixFileDirectoryTests.
        UnixProcessAccessorTestCase.
        UnixProcessFileLockTestCase.
        UnixProcessTestCase.
        UnixProcessUnixFileLockingTestCase.
        UnixProcessWin32FileLockingTestCase.
        WebClientServerTest. "some tests pass slowly"
        WebMessageTest.
        ZzzFileSystemGitAfterSuiteTest.
        ZzzGitAfterSuiteTest.
    } collect: [:testCase |
        testCase allTestSelectors do: [:sel | ignoredTests add: (testCase selector: sel) ]].

    isLinux ifTrue: [
        {
            SqueakSSLTest. "certificate errors on Ubuntu 22.04"
        } collect: [:testCase |
            testCase allTestSelectors do: [:sel | ignoredTests add: (testCase selector: sel) ]].
    ].

    {
        "slow tests"
        LocaleTest -> #(#test04InstallFont).
        StringTest -> #(#testPercentEncodingJa).

        "ignored tests"
        AllocationTest -> #(#testOutOfMemorySignal).
        BitmapStreamTests -> #(#testMatrixTransform2x3WithImageSegment #testShortIntegerArrayWithImageSegment #testShortPointArrayWithImageSegment #testShortRunArrayWithImageSegment #testWordArrayWithImageSegment). "image segments not supported"
        BytecodeDecodingTests -> #(#testPCPreviousTo "fails with assertion error: Unexpected argument size, maybe dummy frame had wrong size?").
        CompiledCodeInspectorTest -> #(#testDebugConstruction). "broken"
        ContextInspectorTest -> #(#testDebugConstruction). "broken"
        HtmlReadWriterTest -> #(#test16ImgTag).
        MemoryTests -> #(#testFrameActivationLeak).
        SlangTests -> #(#testSimpleMethod). "broken"
        SlangTypePromotionTest -> #(#testFloatAndNil #testNilAndFloat). "broken"
        SmalltalkImageTest -> #(#testForceChangesToDiskRobustness).
        SocketTest -> #(#testSocketReuse #testTCPSocketReuse #testUDP).
        StackInterpreterTests -> #(#testImmediateFloats #testPointerTaggingDetagging). "broken"
        WriteBarrierTest -> #(#testBasicReadOnly #testBasicWritable #testRetryingInstVarModification #testSetIsReadOnlyFailure #testSetIsReadOnlyFailureProxy #testSetIsReadOnlyImmediate #testSetIsReadOnlyImmediateProxy).
    } collect: [:assoc | | testCase |
        testCase := assoc key.
        assoc value do: [:sel | ignoredTests add: (testCase selector: sel) ]].

    failingTests := OrderedCollection new.
    {
        CoInterpreterTests.
        FormatNumberTests.
        OSPipeTestCase.
        RegisterAllocatingCogitTests.
        ToolMenusTest.
        VMMakerDecompilerTests.
    } collect: [:testCase |
        testCase allTestSelectors do: [:sel | failingTests add: (testCase selector: sel) ]].

    isWin ifTrue: [
        {
            Win32VMTest.
        } collect: [:testCase |
            testCase allTestSelectors do: [:sel | failingTests add: (testCase selector: sel) ]].
    ].

    {
        BitBltSimulationTest -> #(#testRgbMulDepth16 #testRgbMulDepth1to8).
        CompilerExceptionsTest -> #(#testUnknownSelector).
        HelpBrowserTest -> #(#testRegistration).
        ObjectTest -> #(#testEvaluateWheneverChangeIn #testEvaluateWheneverChangeInTransparent "become EmptyObject->PointersObject not yet supported" #testPinning "not yet supported").
        PackageDependencyTest -> #(#testBalloon #testPreferenceBrowser #testSUnit).
        ReleaseTest -> #(#testUndeclared).
        SequenceableCollectionTest -> #(#testDoWithIndexDeprecation).
        SetInspectorTest -> #(#testUninitialized). "failing"
        SmallIntegerTest -> #(#testMaxVal #testMinVal #testPrintString). "assume SmallIntegers are 31 or 61 bit rather than 64 bit (long)."
        WorldStateTest -> #(#testActiveVariablesObsoletion #testWorldVariableObsoletion).
    } collect: [:assoc | | testCase |
        testCase := assoc key.
        assoc value do: [:sel | failingTests add: (testCase selector: sel) ]].

    flakyTests := OrderedCollection new.
    {
        BitBltSimulationTest -> #(#testAlphaCompositing2Simulated #testAlphaCompositingSimulated).
        DebuggerTests -> #(#test02UnhandledException #test19Abandon).
        FloatTest -> #(#testCompiledConstants). "fails on PGO-instrumented standalone"
        MCSnapshotTest -> #(#testInstanceReuse).
        MorphicToolBuilderTests -> #(#testGetButtonColor).
        PCCByLiteralsTest -> #(#testSwitchPrimCallOffOn).
        SHParserST80Test -> #(#testNumbers).
        StringTest ->#(#testFindSelector).
        TestValueWithinFix -> #(#testValueWithinTimingRepeat).
        WeakSetInspectorTest -> #(#testSymbolTableM6812 #testUninitialized).
        WideStringTest -> #(#testAsIntegerSignedUsingRandomNumbers). "sometimes hitting timeout due to slow becomeForward:"

        "failing on some platforms"
        GIFReadWriterTest -> #(#testAnimatedColorsOutIn).
    } collect: [:assoc | | testCase |
        testCase := assoc key.
        assoc value do: [:sel | flakyTests add: (testCase selector: sel) ]].

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
