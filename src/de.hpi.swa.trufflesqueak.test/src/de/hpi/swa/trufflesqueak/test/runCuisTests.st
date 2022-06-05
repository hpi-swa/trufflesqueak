| testSuite testCases nonTerminatingTestCases failingTests result exitCode |

StdIOWriteStream stdout nextPutAll: 'Setting author information for testing ...'; newLine; flush.
Utilities classPool at: #AuthorName put: 'TruffleSqueak'.
Utilities classPool at: #AuthorInitials put: 'TS'.

nonTerminatingTestCases := OrderedCollection new.
{
    #JpegTest -> #(#test16bpp61 #test16bpp63 #test16bppLE #test16bppLE61 #test16bppLE62 #test16bppLE63 #test32bpp #test32bpp63 #test32bppLE63 #testBA32bpp #testBA32bpp63 "all previous tests pass on TruffleSqueak in JVM mode, the rest is not yet supported" #test16bpp #test16bpp62 #testBA16bpp #testBA16bpp61 #testBA16bpp62 #testBA16bpp63 #testBA16bppLE #testBA16bppLE61 #testBA16bppLE62 #testBA16bppLE63 #testGray #testGray61 #testGray62 #testGray63 #testGrayBA #testGrayBA61 #testGrayBA62 #testGrayBA63).
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
    #ProcessorTest -> #("flaky" #testGrabProcessor #testGrabProcessorOnlyForNoTimeout #testGrabProcessorOnlyForTimeout #testValueUnpreemptively).
    #ProcessTerminateUnwindNonLocalReturn -> #(test1ATerminate #test1BTerminate #test2ATerminate #test2BTerminate #test3ATerminate #test3BTerminate #test4ATerminate #test4BTerminate #test5ATerminate #test5BTerminate #test6ATerminate #test6BTerminate #test7ATerminate #test7BTerminate #test8ATerminate #test8BTerminate).
    #ProcessTest -> #(#testTerminateInEnsure #testValueEnsured).
    #WeakIdentitySetTest -> #(#test). "Failing (or flaky?) on TruffleSqueak Native"
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
