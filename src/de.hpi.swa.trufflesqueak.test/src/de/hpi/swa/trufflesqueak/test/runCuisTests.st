| testSuite nonTerminatingTestCases failingTests result exitCode |

StdIOWriteStream stdout nextPutAll: 'Setting author information for testing ...'; newLine; flush.
Utilities classPool at: #AuthorName put: 'TruffleSqueak'.
Utilities classPool at: #AuthorInitials put: 'TS'.
SmallInteger initMinValAndMaxVal. "resize SmallInteger range"

nonTerminatingTestCases := OrderedCollection new.
{
    #JpegTest -> #(#test16bpp61 #test16bpp63 #test16bppLE #test16bppLE61 #test16bppLE62 #test16bppLE63 #test32bpp #test32bpp63 #test32bppLE63 #testBA32bpp #testBA32bpp63 "all previous tests pass on TruffleSqueak in JVM mode, the rest is not yet supported" #test16bpp #test16bpp62 #testBA16bpp #testBA16bpp61 #testBA16bpp62 #testBA16bpp63 #testBA16bppLE #testBA16bppLE61 #testBA16bppLE62 #testBA16bppLE63 #testGray #testGray61 #testGray62 #testGray63 #testGrayBA #testGrayBA61 #testGrayBA62 #testGrayBA63).
    #ProcessTest -> #(#testResumeWithEnsureAfterBCR #testTerminateInEnsure).
    #SemaphoreTest -> #("unexpected null reads" #testSemaInCriticalWaitInEnsure #testSemaInCriticalWaitInEnsure2).
} collect: [:assoc | | testCase |
    testCase := Smalltalk at: assoc key.
    assoc value do: [:sel | nonTerminatingTestCases add: (testCase selector: sel) ]].
StdIOWriteStream stdout newLine; nextPutAll: 'Non-terminating TestCases:'; newLine; flush.
nonTerminatingTestCases do: [:ea | StdIOWriteStream stdout nextPutAll: '- ', ea asString; newLine; flush ].
StdIOWriteStream stdout newLine; flush.

failingTests := OrderedCollection new.
{
    #EphemeronIdentityDictionaryTest -> #(#testFinalizeValuesWhenLastChainContinuesAtFront).
    #FinalizationRegistryTest -> #(#testAddGC #testEKeyDictionary #testFinalizationWithError).
    #FinalizationTest -> #(#testFinalization #testFinalization2).
    #FloatTest -> #(#testIsDenormalized #testPrimTruncated).
    #ProcessorTest -> #("flaky" #testGrabProcessor #testGrabProcessorOnlyForNoTimeout #testGrabProcessorOnlyForTimeout #testValueUnpreemptively).
    #SmallIntegerTest -> #(#testMaxVal #testMinVal #testPrintString).
    #WeakIdentitySetTest -> #(#test). "Failing (or flaky?) on TruffleSqueak Native"
} collect: [:assoc | | testCase |
    testCase := Smalltalk at: assoc key.
    assoc value do: [:sel | failingTests add: (testCase selector: sel) ]].

StdIOWriteStream stdout newLine; nextPutAll: 'Passing TestCases:'; newLine; flush.
testSuite := TestCase buildSuite.
testSuite tests removeAllSuchThat: [:ea | nonTerminatingTestCases anySatisfy: [:t | ea class == t class and: [ ea selector == t selector ]]].
testSuite tests removeAllSuchThat: [:ea | failingTests            anySatisfy: [:t | ea class == t class and: [ ea selector == t selector ]]].
result := testSuite run.
result printReport.

"To reduce noise, retry any failures or errors"
testSuiteRetry := TestSuite new.
testSuiteRetry tests addAll: result failures.
testSuiteRetry tests addAll: result errors.
resultRetry := testSuiteRetry run.
resultRetry printReport.

exitCode := (resultRetry hasFailures or: [ resultRetry hasErrors ]) ifTrue: [ 1 ] ifFalse: [ 0 ].

StdIOWriteStream stdout newLine; nextPutAll: 'Failing and Flaky TestCases:'; newLine; flush.
testSuite := TestSuite new.
testSuite tests addAll: failingTests.
testSuite run printReport.

Smalltalk quitPrimitive: exitCode
