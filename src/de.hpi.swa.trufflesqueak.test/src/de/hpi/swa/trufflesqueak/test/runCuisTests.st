| testSuite testCases nonTerminatingTestCases failingTests result exitCode |

StdIOWriteStream stdout nextPutAll: 'Setting author information for testing ...'; newLine; flush.
Utilities classPool at: #AuthorName put: 'TruffleSqueak'.
Utilities classPool at: #AuthorInitials put: 'TS'.

nonTerminatingTestCases := OrderedCollection new.
{
    #DelayTest -> #(#testMultiProcessWaitOnSameDelay).
    #MonitorTest -> #(#testMonitorNotGainingUnwantedSignalsDuringUnwinding).
    #ProcessTerminateBug -> #(#testTerminationDuringNestedUnwind5 #testUnwindFromForeignProcess).
    #ProcessTerminateUnwindNonLocalReturn -> #(#test1ATerminate #test1BTerminate #test1CTerminate #test1DTerminate #test2ATerminate #test2BTerminate #test2CTerminate #test2DTerminate #test3ATerminate #test3BTerminate #test3CTerminate #test3DTerminate #test4ATerminate #test4BTerminate #test4CTerminate #test4DTerminate #test5ATerminate #test5BTerminate #test5CTerminate #test5DTerminate #test6ATerminate #test6BTerminate #test6CTerminate #test6DTerminate #test7ATerminate #test7BTerminate #test7CTerminate #test7DTerminate #test8ATerminate #test8BTerminate #test8CTerminate #test8DTerminate).
    #SemaphoreTest -> #(#testSemaAfterCriticalWait).
    #TestValueWithinFix -> #(#testValueWithinNonLocalReturnFixReal #testValueWithinNonLocalReturnFixSimply).
} collect: [:assoc | | testCase |
    testCase := Smalltalk at: assoc key.
    assoc value do: [:sel | nonTerminatingTestCases add: (testCase selector: sel) ]].
StdIOWriteStream stdout newLine; nextPutAll: 'Non-terminating TestCases:'; newLine; flush.
nonTerminatingTestCases do: [:ea | StdIOWriteStream stdout nextPutAll: '- ', ea asString; newLine; flush ].
StdIOWriteStream stdout newLine; flush.

failingTests := OrderedCollection new.
{
    #FloatTest -> #(#testHashWithSmallishLargeNegativeInteger #testHashWithSmallishLargePositiveInteger #testIsDenormalized #testPrimDivideBy #testPrimTruncated).
    #IntegerTest -> #(#testRange "TruffleSqueak supports 64-bit SmallIntegers").
    #JpegTest -> #(#test16bpp #test16bpp62 #testBA16bpp #testBA16bpp61 #testBA16bpp62 #testBA16bpp63 #testBA16bppLE #testBA16bppLE61 #testBA16bppLE62 #testBA16bppLE63 #testGray #testGray61 #testGray62 #testGray63 #testGrayBA #testGrayBA61 #testGrayBA62 #testGrayBA63).
    #ProcessorTest -> #("flaky" #testGrabProcessor #testGrabProcessorOnlyForNoTimeout #testGrabProcessorOnlyForTimeout #testValueUnpreemptively).
    #ProcessTest -> #(#testTerminateInEnsure #testValueEnsured).
} collect: [:assoc | | testCase |
    testCase := Smalltalk at: assoc key.
    assoc value do: [:sel | failingTests add: (testCase selector: sel) ]].

StdIOWriteStream stdout newLine; nextPutAll: 'Passing TestCases:'; newLine; flush.
testSuite := TestSuite new.
testCases := (TestCase allSubclasses reject: [:testCase |testCase isAbstract])
        sorted: [:a :b | a name <= b name].
testCases do: [:testCase | testCase addToSuiteFromSelectors: testSuite].
testSuite tests removeAllSuchThat: [:ea | nonTerminatingTestCases anySatisfy: [:t | ea class == t class and: [ ea selector == t selector ]]].
testSuite tests removeAllSuchThat: [:ea | failingTests            anySatisfy: [:t | ea class == t class and: [ ea selector == t selector ]]].
result := testSuite run.
result printReport.
exitCode := (result hasFailures or: [ result hasErrors ]) ifTrue: [ 1 ] ifFalse: [ 0 ].

StdIOWriteStream stdout newLine; nextPutAll: 'Failing TestCases:'; newLine; flush.
testSuite := TestSuite new.
testSuite tests addAll: failingTests.
testSuite run printReport.

Smalltalk quitPrimitive: exitCode
