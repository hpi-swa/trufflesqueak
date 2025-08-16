[
    | repoPath isNative isMacOS isWin alphabetically ignoredTests failingTests flakyTests testSuite result retries exitCode |

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

    isNative := [Java type: 'java.lang.System'. false] on: Error do: [true].
    isMacOS := Smalltalk platformName = 'Mac OS'.
    isWin := Smalltalk platformName = 'Win32'.

    repoPath := (FileDirectory default containingDirectory / 'src' / 'image' / 'src') fullName.
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
        CompiledMethodComparisonTest.
        MCFileInTest.

        "ignored tests"
        AioEventHandlerTestCase.
        CogARMCompilerTests.
        CogARMv8CompilerTests.
        CogIA32CompilerTests.
        CogMIPSELCompilerTests.
        CogX64CompilerTests.
        FFIAllocateExternalTests.
        FileSystemGitRepositoryRemoteTests.
        FileSystemGitRepositoryTests.
        FileSystemGitTests.
        FloatMathPluginTests.
        FullSimulationTest.
        GitCommitMergeBaseTests.
        GitCommitTests.
        GitDiffCreatorTests.
        GitDumbHTTPProtocolTest.
        GitFetchSpecTests.
        GitFilesystemUsageTests.
        GitHashTests.
        GitHistoryWalkerTest.
        GitIndexTests.
        GitLazyLoadingTests.
        GitPackedNonDeltaTests.
        GitPackedObjectTests.
        GitPackedRefsTest.
        GitProtocolReaderTest.
        GitProtocolTest.
        GitProtocolWriterTest.
        GitReferenceTests.
        GitReflogTests.
        GitRefSpecTests.
        GitRemoteTest.
        GitRepositoryConfigTests.
        GitRepositoryTests.
        GitSmartHTTPProtocolTest.
        GitStampTests.
        GitStorableObjectTests.
        GitTaglikeObjectTests.
        GitTagTests.
        GitTreeEntryTests.
        GitTreeTests.
        GitUnitOfWorkTest.
        GitZLibTests.
        GitRemoteTest.
        GitRemoteTest.
        GitRemoteTest.
        ImageSegmentTest. "image segments not supported"
        InterpreterPrimitivesTest.
        IslandVMTweaksTestCase.
        LangEnvBugs.
        MutexTest. "assertion errors"
        PackFileCreatorTests.
        PackIndexVersion1Tests.
        PackIndexVersion2Tests.
        ReleaseTest.
        SlangBasicTypeInferenceTest. "many tests broken"
        SlangTypeForArithmeticTest.
        SlangTypeForDereferenceTest.
        SoundTest.
        SpurImageSegmentTests.
        SpurPlanningCompactorTests.
        ST80PackageDependencyTest.
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
        WriteBarrierTest. "requires supportsReadOnlyObjects"
        ZzzFileSystemGitAfterSuiteTest.
        ZzzGitAfterSuiteTest.
    } collect: [:testCase |
        testCase allTestSelectors do: [:sel | ignoredTests add: (testCase selector: sel) ]].

    {
        "slow tests"
        CompilerTest -> #(#testAllNodePCsAreForBytecodesInCollections #testAllNodePCsAreForBytecodesInKernel #testAllNodePCsAreForBytecodesInMorphic).
        FloatTest -> #(#testCompiledConstants).
        LocaleTest -> #(#test04InstallFont).
        MorphicUIManagerTest -> #(#testShowAllBinParts).
        ObjectTest -> #(#testIfNotNilDoDeprecation).
        StringTest -> #(#testPercentEncodingJa).
        TraitFileOutTest -> #(#testFileOutCategory #testFileOutTrait #testRemovingMethods).
        TraitMethodDescriptionTest -> #(#testArgumentNames #testCategories #testConflictingCategories #testConflictMethodCreation).
        TraitSystemTest -> #(#testAllImplementedMessagesWithout #testAllSentMessages).
        TraitTest -> #(#testAddAndRemoveMethodsFromSubtraits #testMarkerMethods #testRequirement).
        WeakSetInspectorTest -> #(#testSymbolTableM6812).

        "ignored tests"
        AllocationTest -> #(#testOutOfMemorySignal).
        BitmapStreamTests -> #(#testMatrixTransform2x3WithImageSegment #testShortIntegerArrayWithImageSegment #testShortPointArrayWithImageSegment #testShortRunArrayWithImageSegment #testWordArrayWithImageSegment). "image segments not supported"
        BytecodeDecodingTests -> #(#testPCPreviousTo "fails with assertion error: Unexpected argument size, maybe dummy frame had wrong size?").
        ChangeHooksTest -> #(#testClassRecategorizedEvent2 #testClassRedefinition #testClassRemovalEvent #testClassRenamedEvent #testClassSuperChangedEvent #testDoItEvent1 #testDoItEvent2 #testInstanceVariableCreationEvent1 #testInstanceVariableCreationEvent2 #testInstanceVariableRemovedEvent1 #testInstanceVariableRemovedEvent2 #testInstanceVariableRenamedSilently #testMethodCreationEvent1 #testMethodCreationEvent2 #testMethodRecategorizationEvent #testMethodRemovedEvent1 #testMethodRemovedEvent2 "Ignoring rest of ChangeHooksTest (has side effects and slows down or breaks other tests).").
        CompiledCodeInspectorTest -> #(#testDebugConstruction). "broken"
        ContextInspectorTest -> #(#testDebugConstruction). "broken"
        DebuggerTests -> #(#test01UserInterrupt #test17HandleBlockCannotReturn #test18HandleNestedBlockCannotReturn #test20TerminateProcess #test21TerminateErrorInEnsureBlock #test22TerminateErrorInNestedEnsureBlock #test23TerminateDoubleErrorInEnsure #test24TerminateTripleErrorInEnsure). "fail, and can lock up or crash image sometimes"
        FFIAllocateTests -> #(#test22ArrayForVoidPointers).
        FFIPluginConstructedTests -> #(#testArrayResultWithPoint #testArrayResultWithString #testBoolsToInts #testChars #testIntAliasCall #testIntAliasCallReturnIntAlias #testMixedDoublesIntAndStruct #testMixedIntAndStruct #testMixedIntAndStruct2 #testMixedIntAndStruct3 #testPoint2 #testPoint4 #testPoint4Bigger #testPointers #testPrintString #testPrintWideString #testReturnPointerAlias #testReturnStructPassingUnionUfdUdSi2 #testReturnStructPassingUnionUfdUfi #testReturnStructSd2 #testReturnStructSdi #testReturnStructSf2 #testReturnStructSf2d #testReturnStructSf4 #testReturnStructSfdf #testReturnStructSfi #testReturnStructSi2 #testReturnStructSl2 #testReturnStructSs2 #testReturnStructSs2i #testReturnStructSs4 #testReturnStructSSdi5 #testReturnStructSsf #testReturnStructSsi #testReturnStructSsis #testReturnStructSsisPassingValues #testReturnStructSslf #testReturnStructSslfPassingValues #testReturnStructSsls #testReturnStructSslsPassingValues #testReturnStructSsSsf #testReturnStructSsSsi #testReturnUnionUdSi2 #testReturnUnionUfd #testReturnUnionUfi #testSmallStructureReturn #testSumdiWithStructSdi4 #testSumdWithStructSdi4 #testSumfWithStructSfd4 #testSumiWithStructSdi4 #testSumStructSdi #testSumStructSdi2 #testSumStructSdi4 #testSumStructSfd #testSumStructSfd2 #testSumStructSfd4 #testSumStructSSdi5 #testSumStructSslf #testSumStructSslf2 #testSumStructSslf4 #testSumStructSUfdUdsi2 #testSumStructSUfdUfi).
        FFIPluginLibraryTests -> #(#testArrayResultWithPoint #testArrayResultWithString #testBoolsToInts #testChars #testDoubles14 #testFloats13 #testFloats14 #testIntAliasCall #testIntAliasCallReturnIntAlias #testLongLong8a2 #testMixedDoublesAndLongsSum #testMixedDoublesIntAndStruct #testMixedIntAndStruct #testMixedIntAndStruct2 #testMixedIntAndStruct3 #testPoint2 #testPoint4 #testPoint4Bigger #testPointers #testPrintString #testPrintWideString #testReturnPointerAlias #testReturnStructPassingUnionUfdUdSi2 #testReturnStructPassingUnionUfdUfi #testReturnStructSd2 #testReturnStructSdi #testReturnStructSf2 #testReturnStructSf2d #testReturnStructSf4 #testReturnStructSfdf #testReturnStructSfi #testReturnStructSi2 #testReturnStructSl2 #testReturnStructSs2 #testReturnStructSs2i #testReturnStructSs4 #testReturnStructSSdi5 #testReturnStructSsf #testReturnStructSsi #testReturnStructSsis #testReturnStructSsisPassingValues #testReturnStructSslf #testReturnStructSslfPassingValues #testReturnStructSsls #testReturnStructSslsPassingValues #testReturnStructSsSsf #testReturnStructSsSsi #testReturnUnionUdSi2 #testReturnUnionUfd #testReturnUnionUfi #testSmallStructureReturn #testSumdiWithStructSdi4 #testSumdWithStructSdi4 #testSumfWithStructSfd4 #testSumiWithStructSdi4 #testSumStructSdi #testSumStructSdi2 #testSumStructSdi4 #testSumStructSfd #testSumStructSfd2 #testSumStructSfd4 #testSumStructSSdi5 #testSumStructSslf #testSumStructSslf2 #testSumStructSslf4 #testSumStructSUfdUdsi2 #testSumStructSUfdUfi).
        FFIPluginTests -> #(#testArrayResultWithPoint #testArrayResultWithString #testBoolsToInts #testChars #testDoubles14 #testFloats13 #testFloats14 #testIntAliasCall #testIntAliasCallReturnIntAlias #testIntCallReturnIntAlias #testLongLong8a2 #testMixedDoublesAndLongsSum #testMixedDoublesIntAndStruct #testMixedIntAndStruct #testMixedIntAndStruct2 #testMixedIntAndStruct3 #testPoint2 #testPoint4 #testPoint4Bigger #testPointers #testPrintString #testPrintWideString #testReturnPointerAlias #testReturnStructPassingUnionUfdUdSi2 #testReturnStructPassingUnionUfdUfi #testReturnStructSd2 #testReturnStructSdi #testReturnStructSf2 #testReturnStructSf2d #testReturnStructSf4 #testReturnStructSfdf #testReturnStructSfi #testReturnStructSi2 #testReturnStructSl2 #testReturnStructSs2 #testReturnStructSs2i #testReturnStructSs4 #testReturnStructSSdi5 #testReturnStructSsf #testReturnStructSsi #testReturnStructSsis #testReturnStructSsisPassingValues #testReturnStructSslf #testReturnStructSslfPassingValues #testReturnStructSsls #testReturnStructSslsPassingValues #testReturnStructSsSsf #testReturnStructSsSsi #testReturnUnionUdSi2 #testReturnUnionUfd #testReturnUnionUfi #testSmallStructureReturn #testSumdiWithStructSdi4 #testSumdWithStructSdi4 #testSumfWithStructSfd4 #testSumiWithStructSdi4 #testSumStructSdi #testSumStructSdi2 #testSumStructSdi4 #testSumStructSfd #testSumStructSfd2 #testSumStructSfd4 #testSumStructSSdi5 #testSumStructSslf #testSumStructSslf2 #testSumStructSslf4 #testSumStructSUfdUdsi2 #testSumStructSUfdUfi).
        HtmlReadWriterTest -> #(#test16ImgTag).
        MemoryTests -> #(#testFrameActivationLeak).
        ProcessTest -> #(#testTerminateTerminatingProcessInUnwindTo).
        SlangTests -> #(#testSimpleMethod). "broken"
        SlangTypePromotionTest -> #(#testFloatAndNil #testNilAndFloat). "broken"
        SmalltalkImageTest -> #(#testForceChangesToDiskRobustness).
        SocketTest -> #(#testSocketReuse #testTCPSocketReuse #testUDP).
        SqueakSSLTest -> #(#testConnectAccept #testEncryptDecrypt #testFaceBookAPI #testGooglePopStream #testMultiFrameDecrypt #testSingleByteDecrypt #testSocketAccept #testSocketConnect #testSplitTlsFrameRead #testSSLSockets #testStreamAccept #testStreamConnect #testStreamTransfer).
        StackInterpreterTests -> #(#testImmediateFloats #testPointerTaggingDetagging). "broken"
        SystemChangeFileTest -> #(#testClassRenamed). "flaky and can hang the system"
        TruffleSqueakTest -> #(#testTestMapConsistency).
    } collect: [:assoc | | testCase |
        testCase := assoc key.
        assoc value do: [:sel | ignoredTests add: (testCase selector: sel) ]].

    isNative ifTrue: [
        {
            FFIPluginConstructedTests -> FFIPluginConstructedTests allTestSelectors.
            FFIPluginLibraryTests -> FFIPluginLibraryTests allTestSelectors.
            FFIPluginTests -> FFIPluginTests allTestSelectors.
            InteropTest -> InteropTest allTestSelectors.
            JavaTest -> JavaTest allTestSelectors.
            JPEGReadWriter2PluginTest -> JPEGReadWriter2PluginTest allTestSelectors.
            JPEGReadWriter2Test -> JPEGReadWriter2Test allTestSelectors.
            PolyglotTest -> #(#testInstallLanguage).
            TruffleSqueakTest -> #(#testLayoutStatistics #testCallTarget).
        } collect: [:assoc | | testCase |
            testCase := assoc key.
            assoc value do: [:sel | ignoredTests add: (testCase selector: sel) ]].
    ].

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
        ContextTest -> #(#testPrimitive100). "failing"
        HelpBrowserTest -> #(#testRegistration).
        JPEGReadWriter2PluginTest -> #(#testPrimJPEGWriteImage). "expected form has an off-by-one pixel"
        JPEGReadWriter2Test -> #(#testPrimJPEGWriteImage). "failing"
        MorphicToolBuilderTests -> #(#testGetButtonColor).
        ObjectTest -> #(#testEvaluateWheneverChangeIn #testEvaluateWheneverChangeInTransparent #testPinning). "expected or failing"
        PackageDependencyTest -> #(#testBalloon #testPreferenceBrowser #testSUnit).
        SequenceableCollectionTest -> #(#testDoWithIndexDeprecation).
        SetInspectorTest -> #(#testUninitialized). "failing"
        SmallIntegerTest -> #(#testMaxVal #testMinVal #testPrintString). "assume SmallIntegers are 31 or 61 bit rather than 64 bit (long)."
        WorldStateTest -> #(#testActiveVariablesObsoletion #testWorldVariableObsoletion).
    } collect: [:assoc | | testCase |
        testCase := assoc key.
        assoc value do: [:sel | failingTests add: (testCase selector: sel) ]].

    flakyTests := OrderedCollection new.
    {
        BasicInspectorTest -> #(#testExpressions).
        BitBltSimulationTest -> #(#testAlphaCompositing2Simulated #testAlphaCompositingSimulated).
        ClassInspectorTest -> #(#testExpressions).
        CollectionInspectorTest -> #(#testExpressions).
        CompiledCodeInspectorTest -> #(#testExpressions).
        ContextInspectorTest -> #(#testExpressions).
        ContextVariablesInspectorTest -> #(#testExpressions).
        DebuggerTests -> #(#test02UnhandledException).
        DictionaryInspectorTest -> #(#testExpressions).
        FileList2ModalDialogsTest -> #(#testModalFolderSelector #testModalFolderSelectorForProjectLoad).
        FSFileHandleTest -> #(#testCloseWhenGarbageCollected).
        InspectorTest -> #(#testExpressions).
        IntegerTest -> #(#testReciprocalModulo).
        MCDictionaryRepositoryTest -> #(#testIncludesName #testStoreAndLoad).
        MethodHighlightingTests -> #(#testMethodHighlighting).
        PCCByCompilationTest -> #(#testSwitchPrimCallOffOn).
        PCCByLiteralsTest -> #(#testSwitchPrimCallOffOn).
        SequenceableCollectionTest -> #(#testCollectWithIndexDeprecation).
        SetInspectorTest -> #(#testExpressions).
        SHParserST80Test -> #(#testNumbers).
        TestValueWithinFix -> #(#testValueWithinTimingBasic #testValueWithinTimingNestedInner #testValueWithinTimingNestedOuter #testValueWithinTimingRepeat).
        WeakIdentityDictionaryTest -> #(#testIsEmpty).
        WeakKeyDictionaryTest -> #(#testIsEmpty).
        WeakSetInspectorTest -> #(#testUninitialized).
        WeakSetTest -> #(#testIsEmpty).
        WideStringTest -> #(#testAsIntegerSignedUsingRandomNumbers). "sometimes hitting timeout due to slow becomeForward:"

        "failing on some platforms"
        GIFReadWriterTest -> #(#testAnimatedColorsOutIn).
        MCPackageTest -> #(#testUnload).
    } collect: [:assoc | | testCase |
        testCase := assoc key.
        assoc value do: [:sel | flakyTests add: (testCase selector: sel) ]].

    isWin ifTrue: [
        {
            FSFileHandleTest -> #("May fail with 'FileDoesNotExist: plonk' on Windows" #testAt #testAtPut #testAtPutBinaryAscii #testAtWriteBinaryAscii #testClose #testCreatedOpen #testIO #testReadBufferTooLarge #testReadOnly #testReference #testSizeAfterGrow #testSizeNoGrow #testTruncate #testTruncateEmpty #testWriteStream).
            PNGReadWriterTest -> #("May fail with FileDoesNotExistException on Windows" #testBlue16 #testBlue32 #testBlue8).
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

    "Presist image before retrying failures"
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
