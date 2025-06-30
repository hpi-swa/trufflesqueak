| repoPath isNative ignoredTests failingTests flakyTests testSuite result retries exitCode |

ToolSet default: CommandLineToolSet.

FileStream stdout nextPutAll: 'Setting author information for testing ...'; cr; flush.
Utilities
    authorName: 'TruffleSqueak';
    setAuthorInitials: 'TS'.

FileStream stdout nextPutAll: 'Patch TestCase>>run: ...'; cr; flush.
TestCase compile: 'run: aResult
    FileStream stdout nextPutAll: self asString; nextPutAll: '' ... ''; flush.
    aResult runCase: self.
    FileStream stdout nextPutAll: ''('';
                      nextPutAll: ((aResult durations at: self) ifNil: [0]) asString;
                      nextPutAll: ''ms)''; cr; flush.'
        classified: 'patched'.

isNative := [Java type: 'java.lang.System'. false] on: Error do: [true].

repoPath := (FileDirectory default containingDirectory / 'src' / 'image' / 'src') fullName.
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

ignoredTests := OrderedCollection new.
{
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
    InterpreterPrimitivesTest.
    IslandVMTweaksTestCase.
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
    StackInterpreterTests.
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
    AllocationTest -> #(#testOutOfMemorySignal).
    BytecodeDecodingTests -> #(#testPCPreviousTo "fails with assertion error: Unexpected argument size, maybe dummy frame had wrong size?").
    ChangeHooksTest -> #(#testClassRecategorizedEvent2 #testClassRedefinition #testClassRemovalEvent #testClassRenamedEvent #testClassSuperChangedEvent #testDoItEvent1 #testDoItEvent2 #testInstanceVariableCreationEvent1 #testInstanceVariableCreationEvent2 #testInstanceVariableRemovedEvent1 #testInstanceVariableRemovedEvent2 #testInstanceVariableRenamedSilently #testMethodCreationEvent1 #testMethodCreationEvent2 #testMethodRecategorizationEvent #testMethodRemovedEvent1 #testMethodRemovedEvent2 "Ignoring rest of ChangeHooksTest (has side effects and slows down or breaks other tests).").
    FFIAllocateTests -> #(#test22ArrayForVoidPointers).
    FFIPluginConstructedTests -> #(#testArrayResultWithPoint #testArrayResultWithString #testBoolsToInts #testChars #testIntAliasCall #testIntAliasCallReturnIntAlias #testMixedDoublesIntAndStruct #testMixedIntAndStruct #testMixedIntAndStruct2 #testMixedIntAndStruct3 #testPoint2 #testPoint4 #testPoint4Bigger #testPointers #testPrintString #testPrintWideString #testReturnPointerAlias #testReturnStructPassingUnionUfdUdSi2 #testReturnStructPassingUnionUfdUfi #testReturnStructSd2 #testReturnStructSdi #testReturnStructSf2 #testReturnStructSf2d #testReturnStructSf4 #testReturnStructSfdf #testReturnStructSfi #testReturnStructSi2 #testReturnStructSl2 #testReturnStructSs2 #testReturnStructSs2i #testReturnStructSs4 #testReturnStructSSdi5 #testReturnStructSsf #testReturnStructSsi #testReturnStructSsis #testReturnStructSsisPassingValues #testReturnStructSslf #testReturnStructSslfPassingValues #testReturnStructSsls #testReturnStructSslsPassingValues #testReturnStructSsSsf #testReturnStructSsSsi #testReturnUnionUdSi2 #testReturnUnionUfd #testReturnUnionUfi #testSmallStructureReturn #testSumdiWithStructSdi4 #testSumdWithStructSdi4 #testSumfWithStructSfd4 #testSumiWithStructSdi4 #testSumStructSdi #testSumStructSdi2 #testSumStructSdi4 #testSumStructSfd #testSumStructSfd2 #testSumStructSfd4 #testSumStructSSdi5 #testSumStructSslf #testSumStructSslf2 #testSumStructSslf4 #testSumStructSUfdUdsi2 #testSumStructSUfdUfi).
    FFIPluginLibraryTests -> #(#testArrayResultWithPoint #testArrayResultWithString #testBoolsToInts #testChars #testDoubles14 #testFloats13 #testFloats14 #testIntAliasCall #testIntAliasCallReturnIntAlias #testLongLong8a2 #testMixedDoublesAndLongsSum #testMixedDoublesIntAndStruct #testMixedIntAndStruct #testMixedIntAndStruct2 #testMixedIntAndStruct3 #testPoint2 #testPoint4 #testPoint4Bigger #testPointers #testPrintString #testPrintWideString #testReturnPointerAlias #testReturnStructPassingUnionUfdUdSi2 #testReturnStructPassingUnionUfdUfi #testReturnStructSd2 #testReturnStructSdi #testReturnStructSf2 #testReturnStructSf2d #testReturnStructSf4 #testReturnStructSfdf #testReturnStructSfi #testReturnStructSi2 #testReturnStructSl2 #testReturnStructSs2 #testReturnStructSs2i #testReturnStructSs4 #testReturnStructSSdi5 #testReturnStructSsf #testReturnStructSsi #testReturnStructSsis #testReturnStructSsisPassingValues #testReturnStructSslf #testReturnStructSslfPassingValues #testReturnStructSsls #testReturnStructSslsPassingValues #testReturnStructSsSsf #testReturnStructSsSsi #testReturnUnionUdSi2 #testReturnUnionUfd #testReturnUnionUfi #testSmallStructureReturn #testSumdiWithStructSdi4 #testSumdWithStructSdi4 #testSumfWithStructSfd4 #testSumiWithStructSdi4 #testSumStructSdi #testSumStructSdi2 #testSumStructSdi4 #testSumStructSfd #testSumStructSfd2 #testSumStructSfd4 #testSumStructSSdi5 #testSumStructSslf #testSumStructSslf2 #testSumStructSslf4 #testSumStructSUfdUdsi2 #testSumStructSUfdUfi).
    FFIPluginTests -> #(#testArrayResultWithPoint #testArrayResultWithString #testBoolsToInts #testChars #testDoubles14 #testFloats13 #testFloats14 #testIntAliasCall #testIntAliasCallReturnIntAlias #testIntCallReturnIntAlias #testLongLong8a2 #testMixedDoublesIntAndStruct #testPoint4Bigger #testPointers #testPrintString #testPrintWideString #testReturnPointerAlias #testReturnStructPassingUnionUfdUdSi2 #testReturnStructPassingUnionUfdUfi #testReturnStructSd2 #testReturnStructSdi #testReturnStructSf2 #testReturnStructSf2d #testReturnStructSf4 #testReturnStructSfdf #testReturnStructSfi #testReturnStructSi2 #testReturnStructSl2 #testReturnStructSs2 #testReturnStructSs2i #testReturnStructSs4 #testReturnStructSSdi5 #testReturnStructSsf #testReturnStructSsi #testReturnStructSsis #testReturnStructSsisPassingValues #testReturnStructSslf #testReturnStructSslfPassingValues #testReturnStructSsls #testReturnStructSslsPassingValues #testReturnStructSsSsf #testReturnStructSsSsi #testReturnUnionUdSi2 #testReturnUnionUfd #testReturnUnionUfi #testSmallStructureReturn #testSumdiWithStructSdi4 #testSumdWithStructSdi4 #testSumfWithStructSfd4 #testSumiWithStructSdi4 #testSumStructSdi #testSumStructSdi2 #testSumStructSdi4 #testSumStructSfd #testSumStructSfd2 #testSumStructSfd4 #testSumStructSSdi5 #testSumStructSslf #testSumStructSslf2 #testSumStructSslf4 #testSumStructSUfdUdsi2 #testSumStructSUfdUfi).
    HtmlReadWriterTest -> #(#test16ImgTag).
    ProcessTest -> #(#testTerminateTerminatingProcessInUnwindTo).
    SlangTypePromotionTest -> #(#testFloatAndNil #testNilAndFloat). "broken"
    SlangTests -> #(#testSimpleMethod). "broken"
    SmalltalkImageTest -> #(#testForceChangesToDiskRobustness).
    SocketTest -> #(#testSocketReuse #testTCPSocketReuse #testUDP).
    SqueakSSLTest -> #(#testConnectAccept #testEncryptDecrypt #testFaceBookAPI #testGooglePopStream #testMultiFrameDecrypt #testSingleByteDecrypt #testSocketAccept #testSocketConnect #testSplitTlsFrameRead #testSSLSockets #testStreamAccept #testStreamConnect #testStreamTransfer).
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
    CoInterpreterTests. "broken"
    CompiledMethodComparisonTest. "not terminating"
    FormatNumberTests. "broken"
    ImageSegmentTest. "failing"
    LangEnvBugs. "slowly passing"
    MCEnvironmentLoadTest. "slowly failing"
    MCFileInTest. "slowly passing"
    MCPackageTest. "broken"
    MCPatchTest. "slowly passing"
    MCWorkingCopyRenameTest. "slowly passing"
    MCWorkingCopyTest. "slowly passing"
    OSPipeTestCase. "broken"
    ProgressInitiationExceptionTest. "not terminating"
    RegisterAllocatingCogitTests. "broken"
    ToolMenusTest. "broken"
    VMInterfaceConsistencyTests. "slowly passing"
    VMMakerDecompilerTests. "broken"
    Win32VMTest. "failing on Windows"
} collect: [:testCase |
    testCase allTestSelectors do: [:sel | failingTests add: (testCase selector: sel) ]].

{
    BitBltSimulationTest -> #(#testRgbMulDepth16 #testRgbMulDepth1to8). "broken"
    BitmapStreamTests -> #(#testMatrixTransform2x3WithImageSegment #testShortIntegerArrayWithImageSegment #testShortPointArrayWithImageSegment #testShortRunArrayWithImageSegment #testWordArrayWithImageSegment). "slowly failing"
    ClassTraitTest -> #(#testChanges #testConflictsAliasesAndExclusions #testUsers). "slowly passing"
    ClosureCompilerTest -> #(#testDebuggerTempAccess #testInjectIntoDecompilations #testInjectIntoDecompiledDebugs). "expected failure"
    ColorTest -> #(#testColorsFromPixelValue32BitBlack). "expected failure"
    CompiledCodeInspectorTest -> #(#testDebugConstruction). "broken"
    CompilerExceptionsTest -> #(#testUnknownSelector). "broken"
    CompilerNotifyingTest -> #(#testCaseOtherwiseBlockWithArgument #testifTrueBlockWithArgument #testTooManyArguments #testTooManyLiterals). "expected failure"
    CompilerSyntaxErrorNotifyingTest -> #(#testCaseOtherwiseBlockWithArgument #testifTrueBlockWithArgument #testTooManyArguments #testTooManyLiterals). "expected failure"
    CompilerTest -> #(#testAllNodePCsAreForBytecodesInCollections #testAllNodePCsAreForBytecodesInKernel #testAllNodePCsAreForBytecodesInMorphic #testMaxLiterals #testMaxLiteralsWithClassReferenceInClosure). "slowly passing"
    ContextInspectorTest -> #(#testDebugConstruction). "broken"
    ContextTest -> #(#testPrimitive100). "failing"
    DebuggerTests -> #(#test01UserInterrupt #test05DebuggerTerminatesProcess #test06DebugSpecificContext #test07DebuggerNotifier #test08DebuggerFull #test09DebuggerNotifierOrFull #test10DebugBlock #test11DebugBlockAtContext #test16HandleSimulationError #test17HandleBlockCannotReturn #test18HandleNestedBlockCannotReturn #test19Abandon #test20TerminateProcess #test21TerminateErrorInEnsureBlock #test22TerminateErrorInNestedEnsureBlock #test23TerminateDoubleErrorInEnsure #test24TerminateTripleErrorInEnsure). "not terminating"
    DebuggerUnwindBug -> #(#testUnwindDebugger). "not terminating"
    DecompilerTests -> #(#testDecompilerInClassesAAtoAM #testDecompilerInClassesANtoAZ #testDecompilerInClassesBAtoBM #testDecompilerInClassesBNtoBZ #testDecompilerInClassesCAtoCM #testDecompilerInClassesCNtoCZ #testDecompilerInClassesDAtoDM #testDecompilerInClassesDNtoDZ #testDecompilerInClassesEAtoEM #testDecompilerInClassesENtoEZ #testDecompilerInClassesFAtoFM #testDecompilerInClassesFNtoFZ #testDecompilerInClassesGAtoGM #testDecompilerInClassesGNtoGZ #testDecompilerInClassesHAtoHM #testDecompilerInClassesHNtoHZ #testDecompilerInClassesIAtoIM #testDecompilerInClassesINtoIZ #testDecompilerInClassesJAtoJM #testDecompilerInClassesJNtoJZ #testDecompilerInClassesKAtoKM #testDecompilerInClassesKNtoKZ #testDecompilerInClassesLAtoLM #testDecompilerInClassesLNtoLZ #testDecompilerInClassesMAtoMM #testDecompilerInClassesMNtoMZ #testDecompilerInClassesNAtoNM #testDecompilerInClassesNNtoNZ #testDecompilerInClassesOAtoOM #testDecompilerInClassesONtoOZ #testDecompilerInClassesPAtoPM #testDecompilerInClassesPNtoPZ #testDecompilerInClassesQAtoQM #testDecompilerInClassesQNtoQZ #testDecompilerInClassesRAtoRM #testDecompilerInClassesRNtoRZ #testDecompilerInClassesSAtoSM #testDecompilerInClassesSNtoSZ #testDecompilerInClassesTAtoTM #testDecompilerInClassesTNtoTZ #testDecompilerInClassesUAtoUM #testDecompilerInClassesUNtoUZ #testDecompilerInClassesVAtoVM #testDecompilerInClassesVNtoVZ #testDecompilerInClassesWAtoWM #testDecompilerInClassesWNtoWZ #testDecompilerInClassesXAtoXM #testDecompilerInClassesXNtoXZ #testDecompilerInClassesYAtoYM #testDecompilerInClassesYNtoYZ #testDecompilerInClassesZAtoZM #testDecompilerInClassesZNtoZZ #testDecompileUnreachableParameter). "slowly passing"
    DependencyBrowserTest -> #(#testSelectedMessageName). "broken"
    ExceptionTests -> #(#testHandlerFromAction). "expected failure"
    FFIPluginTests -> #(#testMixedDoublesAndLongsSum #testMixedIntAndStruct #testMixedIntAndStruct2 #testMixedIntAndStruct3 #testPoint2 #testPoint4).
    FloatTest -> #(#testCompiledConstants). "slowly passing"
    FSFileHandleTest -> #(#testAt #testAtPut #testAtPutBinaryAscii #testAtWriteBinaryAscii #testClose #testCreatedOpen #testIO #testReadBufferTooLarge #testReadOnly #testReference #testSizeAfterGrow #testSizeNoGrow #testTruncate #testTruncateEmpty #testWriteStream). "flaky on Windows"
    GIFReadWriterTest -> #(#testAnimatedColorsOutIn #testAnimatedColorsOutInRealBlack). "slowly passing"
    GridLayoutTest -> #(#test05AdhereToEdge). "broken"
    HelpBrowserTest -> #(#testRegistration). "broken"
    IncludedMethodsTest -> #(#testTranslateFromToTable). "broken"
    JPEGReadWriter2PluginTest -> #(#testPrimJPEGWriteImage). "expected form has an off-by-one pixel"
    JPEGReadWriter2Test -> #(#testPrimJPEGWriteImage). "failing"
    LocaleTest -> #(#test04InstallFont). "slowly passing"
    MCMczInstallerTest -> #(#testInstallFromFile). "not terminating"
    MCMethodDefinitionTest -> #(#testCannotLoad). "slowly passing"
    MCSerializationTest -> #(#testMcdSerialization). "slowly passing"
    MCSnapshotTest -> #(#testInstanceReuse). "slowly passing"
    MemoryTests -> #(#testBitmap64BitLongs #testFrameActivationLeak). "expected failure and suddenly started to time out in CI after merging #186"
    MiscPrimitivePluginTest -> #(#testFindSubstringInWideString #testFindSubstringOldVersionWithMissingTypeChecks). "expected failure"
    MorphicToolBuilderTests -> #(#testGetButtonColor). "broken"
    MorphicUIManagerTest -> #(#testShowAllBinParts). "slowly passing"
    MultiByteFileStreamTest -> #(#testLineEndConversionExpectedFailures). "expected failure"
    ObjectTest -> #(#testEvaluateWheneverChangeIn #testEvaluateWheneverChangeInTransparent #testIfNotNilDoDeprecation #testPinning). "expected or failing"
    PackageDependencyTest -> #(#testBalloon #testCollections #testCompiler #testEtoys #testKernel #testMorphic #testPreferenceBrowser #testSUnit #testSystem #testTools). "broken"
    PECodeBoxReadWriterTest -> #(#testAddCodeBoxesToSnippetFolder1). "slowly passing"
    ProcessUnwindTest -> #(#testTerminateInNestedEnsureWithReturn1 #testTerminateInNestedEnsureWithReturn2 #testTerminateInNestedEnsureWithReturn3 #testTerminateInNestedEnsureWithReturn4 #testTerminateInNestedEnsureWithReturn5 #testTerminateInNestedEnsureWithReturn6 #testTerminateInNestedEnsureWithReturn7 #testTerminateInNestedEnsureWithReturn8). "not terminating"
    PureBehaviorTest -> #(#testIsLocalAliasSelector #testLocalSelectors #testMethodCategoryReorganization #testPropagationOfChangesInTraits #testPropagationOfChangesInTraitsToAliasMethodsWhenOriginalMethodIsExcluded #testPropagationWhenTraitCompositionModifications #testReshapeClass #testTraitCompositionModifications #testUpdateWhenLocalMethodRemoved). "slowly passing"
    RandomTest -> #(#testNextIntLarge). "slowly passing"
    RWBinaryOrTextStreamTest -> #(#testExisting). "expected failure"
    SequenceableCollectionTest -> #(#testCollectWithIndexDeprecation #testDoWithIndexDeprecation). "broken"
    SetInspectorTest -> #(#testUninitialized). "failing"
    SharedQueueTest -> #(#testNextOrNilSuchThat). "broken"
    SmallIntegerTest -> #(#testMaxVal #testMinVal #testPrintString). "assume SmallIntegers are 31 or 61 bit rather than 64 bit (long)."
    StringTest -> #(#testPercentEncodingJa). "slowly passing"
    SystemChangeFileTest -> #(#testCategoryModified #testClassModified #testClassRecategorized #testClassRemoved #testClassRenamed #testClassReorganized #testExpressionDoIt #testMethodAdded #testMethodModified #testMethodRecategorized #testMethodRemoved #testProtocolAdded #testProtocolDefault #testProtocolModified #testProtocolRemoved #testProtocolRenamed). "slowly passing or expected failure"
    TableLayoutTest -> #(#testCheckBoxGroup #testDialogMinimalExtent). "broken"
    TestValueWithinFix -> #(#testValueWithinNonLocalReturnFixReal). "not terminating"
    TextAttributesScanningTest -> #(#testTextAnchor #testTextFontReferenceForBoldFont #testTextFontReferenceTTCForBoldFont #testTextIndent #testTextMessageLink #testTextPlusJumpEnd #testTextPlusJumpStart). "expected failure"
    TraitCompositionTest -> #(#testAliasCompositionsRaisesErrorBinary #testAliasCompositionsRaisesErrorKeyword #testAliasCompositionsRaisesErrorUnary). "slowly passing"
    TraitFileOutTest -> #(#testFileOutCategory #testFileOutTrait #testRemovingMethods). "slowly passing"
    TraitMethodDescriptionTest -> #(#testArgumentNames #testCategories #testConflictingCategories #testConflictMethodCreation). "slowly passing"
    TraitSystemTest -> #(#testAllImplementedMessagesWithout #testAllSentMessages). "slowly passing"
    TraitTest -> #(#testAddAndRemoveMethodsFromSubtraits #testMarkerMethods #testRequirement). "slowly passing"
    UserInterfaceThemeTest -> #(#test01ImplementationHooks #test02SetProperty #test04SuperClassLookup #test05ClearProperty #test06SetAndClearUnkownProperty #test14Perform #test20ClassSideApply). "broken"
    UTF8EdgeCaseTest -> #(#testMaximumOverlongSequences #testOverlongAsciiSequences #testOverlongNUL #testPairedUTF16Surrogates #testSingleUTF16Surrogates). "expected failure"
    WideStringTest -> #(#testAsIntegerSignedUsingRandomNumbers). "slowly passing"
    WorldStateTest -> #(#testActiveVariablesObsoletion #testDeferredUIQueueTimeout #testWorldVariableObsoletion). "broken"
} collect: [:assoc | | testCase |
    testCase := assoc key.
    assoc value do: [:sel | failingTests add: (testCase selector: sel) ]].

flakyTests := OrderedCollection new.
{
    WeakIdentityKeyDictionaryTest.
    WeakKeyDictionaryTest.
    WeakMessageSendTest.
    WeakRegistryTest.
    WeakSetInspectorTest.
    WeakSetTest.
} collect: [:testCase |
    testCase allTestSelectors do: [:sel | flakyTests add: (testCase selector: sel) ]].

{
    BasicInspectorTest -> #(#testExpressions).
    BitBltSimulationTest -> #(#testAlphaCompositing2Simulated #testAlphaCompositingSimulated).
    ClassBuilderTest -> #(#testSubclass).
    ClassInspectorTest -> #(#testExpressions).
    CollectionInspectorTest -> #(#testExpressions).
    CompiledCodeInspectorTest -> #(#testExpressions).
    ContextInspectorTest -> #(#testExpressions).
    ContextVariablesInspectorTest -> #(#testExpressions).
    DebuggerTests -> #(#test02UnhandledException).
    DictionaryInspectorTest -> #(#testExpressions).
    DurationTest -> #(#testBusyWait).
    FileList2ModalDialogsTest -> #(#testModalFolderSelector #testModalFolderSelectorForProjectLoad).
    FSFileHandleTest -> #(#testCloseWhenGarbageCollected).
    InspectorTest -> #(#testExpressions).
    IntegerTest -> #(#testReciprocalModulo).
    MCDictionaryRepositoryTest -> #(#testIncludesName #testStoreAndLoad).
    MethodHighlightingTests -> #(#testMethodHighlighting).
    PCCByCompilationTest -> #(#testSwitchPrimCallOffOn).
    PCCByLiteralsTest -> #(#testSwitchPrimCallOffOn).
    PNGReadWriterTest -> #(#testBlue16 #testBlue32 #testBlue8). "flaky on Windows"
    SetInspectorTest -> #(#testExpressions).
    SUnitTest -> #(#testTestTimeoutPragma).
    TestValueWithinFix -> #(#testValueWithinTimingBasic #testValueWithinTimingNestedInner #testValueWithinTimingNestedOuter #testValueWithinTimingRepeat).
    WeakIdentityDictionaryTest -> #(#testIsEmpty).
    WeakKeyDictionaryTest -> #(#testIsEmpty).
    WeakRegistryTest -> #(#testFinalization #testGrowingByAHighPriorityProcessDoesntLeak #testGrowingDoesntLeak #testRemovingByAHighPriorityProcessDoesntLeak #testRemovingDoesntLeak).
} collect: [:assoc | | testCase |
    testCase := assoc key.
    assoc value do: [:sel | flakyTests add: (testCase selector: sel) ]].

testSuite := TestCase buildSuite.
testSuite tests removeAllSuchThat: [:ea | ignoredTests anySatisfy: [:t | ea class == t class and: [ ea selector == t selector ]]].
testSuite tests removeAllSuchThat: [:ea | failingTests anySatisfy: [:t | ea class == t class and: [ ea selector == t selector ]]].
testSuite tests removeAllSuchThat: [:ea | flakyTests   anySatisfy: [:t | ea class == t class and: [ ea selector == t selector ]]].
testSuite tests sort: [:a :b | a className < b className or: [ a class == b class and: [a selector < b selector] ] ].

FileStream stdout nextPutAll: 'Ignored TestCases: ', ignoredTests size; cr; flush.
FileStream stdout nextPutAll: 'Failing TestCases: ', failingTests size; cr; flush.
FileStream stdout nextPutAll: 'Flaky TestCases:   ', flakyTests size; cr; flush.
FileStream stdout nextPutAll: 'Passing TestCases: ', testSuite tests size; cr; flush.

result := testSuite run.
FileStream stdout cr; nextPutAll: result asString; cr; flush.

exitCode := result hasPassed ifTrue: [ 0 ] ifFalse: [ 1 ].

"Reload image to persist and retry failures"
Smalltalk snapshot: true andQuit: true.

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

FileStream stdout nextPutAll: 'Running failing TestCases...'; cr; flush.
testSuite := TestSuite new.
testSuite tests addAll: failingTests.
result := testSuite run.
FileStream stdout cr; nextPutAll: testSuite run asString; cr; flush.

FileStream stdout nextPutAll: 'Running flaky TestCases...'; cr; flush.
testSuite := TestSuite new.
testSuite tests addAll: flakyTests.
result := testSuite run.
FileStream stdout cr; nextPutAll: testSuite run asString; cr; flush.

Smalltalk quitPrimitive: exitCode
