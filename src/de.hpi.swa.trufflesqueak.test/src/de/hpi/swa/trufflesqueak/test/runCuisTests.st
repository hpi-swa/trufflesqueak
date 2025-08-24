| testSuite nonTerminatingTestCases failingTests result retries exitCode |

StdIOWriteStream stdout nextPutAll: 'Setting author information for testing ...'; newLine; flush.
Utilities classPool at: #AuthorName put: 'TruffleSqueak'.
Utilities classPool at: #AuthorInitials put: 'TS'.
SmallInteger initMinValAndMaxVal. "resize SmallInteger range"

nonTerminatingTestCases := OrderedCollection new.
{
    #JpegTest -> #(#test16bpp61 #test16bpp63 #test16bppLE #test16bppLE61 #test16bppLE62 #test16bppLE63 #test32bpp #test32bpp63 #test32bppLE63 #testBA32bpp #testBA32bpp63 "all previous tests pass on TruffleSqueak in JVM mode, the rest is not yet supported" #test16bpp #test16bpp62 #testBA16bpp #testBA16bpp61 #testBA16bpp62 #testBA16bpp63 #testBA16bppLE #testBA16bppLE61 #testBA16bppLE62 #testBA16bppLE63 #testGray #testGray61 #testGray62 #testGray63 #testGrayBA #testGrayBA61 #testGrayBA62 #testGrayBA63).
    #ProcessTest -> #(#testResumeWithEnsureAfterBCR).
} collect: [:assoc | | testCase |
    testCase := Smalltalk at: assoc key.
    assoc value do: [:sel | nonTerminatingTestCases add: (testCase selector: sel) ]].
StdIOWriteStream stdout newLine; nextPutAll: 'Non-terminating TestCases:'; newLine; flush.
nonTerminatingTestCases do: [:ea | StdIOWriteStream stdout nextPutAll: '- ', ea asString; newLine; flush ].
StdIOWriteStream stdout newLine; flush.

failingTests := OrderedCollection new.
{
    #FinalizationRegistryTest -> #(#testAddGC #testFinalizationWithError). "first test fails when non-command line"
    #FloatTest -> #(#testIsDenormalized #testPrimTruncated).
    #ProcessorTest -> #("flaky" #testGrabProcessor #testGrabProcessorOnlyForNoTimeout #testGrabProcessorOnlyForTimeout #testValueUnpreemptively).
    #SmallIntegerTest -> #(#testMaxVal #testMinVal #testPrintString).
} collect: [:assoc | | testCase |
    testCase := Smalltalk at: assoc key.
    assoc value do: [:sel | failingTests add: (testCase selector: sel) ]].

StdIOWriteStream stdout newLine; nextPutAll: 'Passing TestCases:'; newLine; flush.
testSuite := TestCase buildSuite.
testSuite tests removeAllSuchThat: [:ea | nonTerminatingTestCases anySatisfy: [:t | ea class == t class and: [ ea selector == t selector ]]].
testSuite tests removeAllSuchThat: [:ea | failingTests            anySatisfy: [:t | ea class == t class and: [ ea selector == t selector ]]].
result := testSuite run.
result printReport.

exitCode := result hasPassed ifTrue: [ 0 ] ifFalse: [ 1 ].

retries := 3.
[ exitCode == 1 and: [ retries > 0 ] ] whileTrue: [
    StdIOWriteStream stdout newLine; nextPutAll: 'Retry failures or errors:'; newLine; flush.
    testSuite := TestSuite new.
    testSuite tests addAll: result failures.
    testSuite tests addAll: result errors.
    result := testSuite run.
    result printReport.
    exitCode := result hasPassed ifTrue: [ 0 ] ifFalse: [ 1 ].
    retries := retries - 1.
].

StdIOWriteStream stdout newLine; nextPutAll: 'Failing and Flaky TestCases:'; newLine; flush.
testSuite := TestSuite new.
testSuite tests addAll: failingTests.
testSuite run printReport.

Smalltalk quitPrimitive: exitCode
