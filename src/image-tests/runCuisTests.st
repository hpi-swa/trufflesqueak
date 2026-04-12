| fileEntry testSuite nonTerminatingTestCases failingTests result retries exitCode |

StdIOWriteStream stdout nextPutAll: 'Setting author information for testing ...'; newLine; flush.
Utilities classPool at: #AuthorName put: 'TruffleSqueak'.
Utilities classPool at: #AuthorInitials put: 'TS'.

fileEntry := DirectoryEntry currentDirectory / 'src' / 'image-tests' // 'extendCompiledMethodWithTestsCuis.st'.
fileEntry exists ifFalse: [
    StdIOWriteStream stdout cr; cr; nextPutAll: 'CompiledMethod extensions and tests not found at "', fileEntry asString, '". EXITING'; cr; cr; flush.
    Smalltalk quitPrimitive: 1].

CompiledMethod preferredBytecodeSetEncoderClass == EncoderForSistaV1
    ifTrue: [
        StdIOWriteStream stdout nextPutAll: 'Loading CompiledMethod extensions from "', fileEntry asString, '" ...'; newLine; flush.
        ChangeSet fileIn: fileEntry]
    ifFalse: [
        StdIOWriteStream stdout nextPutAll: 'Image does not support EncoderForSistaV1...'; newLine; flush].

nonTerminatingTestCases := OrderedCollection new.
{
    #ProcessTest -> #(#testResumeWithEnsureAfterBCR).
    #TestValueWithinFix -> TestValueWithinFix allTestSelectors.
} collect: [:assoc | | testCase |
    testCase := Smalltalk at: assoc key.
    assoc value do: [:sel | nonTerminatingTestCases add: (testCase selector: sel) ]].
StdIOWriteStream stdout newLine; nextPutAll: 'Non-terminating TestCases:'; newLine; flush.
nonTerminatingTestCases do: [:ea | StdIOWriteStream stdout nextPutAll: '- ', ea asString; newLine; flush ].
StdIOWriteStream stdout newLine; flush.

failingTests := OrderedCollection new.
{
    #FloatTest -> #(#testHashWithSmallishLargeNegativeInteger #testHashWithSmallishLargePositiveInteger #testIsDenormalized #testPrimTruncated).
    #IntegerTest -> #(#testRange).
    #StringTest -> #("flaky" #testFindSelector).
    #SmallIntegerTest -> #(#testDivideMayOverflow).
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
