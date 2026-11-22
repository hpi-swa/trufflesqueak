| fileEntry testSuite failingTests result retries exitCode |

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

"TruffleSqueak's SmallIntegers are 64-bit (Java longs), but Cuis caches a hard-coded 61-bit range.
 Determine the actual range (like `SmallInteger class>>#startUp:` in Squeak), so that the image agrees with the VM."
StdIOWriteStream stdout nextPutAll: 'Determining SmallInteger range ...'; newLine; flush.
[ | val next |
    val := -32768. "Assume at least 16 bits"
    [next := val + val.
     next class == SmallInteger] whileTrue: [val := next].
    SmallInteger instVarNamed: 'minVal' put: val.
    SmallInteger instVarNamed: 'maxVal' put: -1 - val ] value.

failingTests := OrderedCollection new.
{
    #FloatTest -> #(#testIsDenormalized). "assumes denormalized Floats are never SmallFloat64s, but all finite Floats are SmallFloat64s (doubles) in TruffleSqueak."
    #SmallIntegerTest -> #(#testMaxVal #testMinVal #testPrintString). "assume SmallIntegers are 31 or 61 bit rather than 64 bit (long)."
} collect: [:assoc | | testCase |
    testCase := Smalltalk at: assoc key.
    assoc value do: [:sel | failingTests add: (testCase selector: sel) ]].

StdIOWriteStream stdout newLine; nextPutAll: 'Passing TestCases:'; newLine; flush.
testSuite := TestCase buildSuite.
testSuite tests removeAllSuchThat: [:ea | failingTests anySatisfy: [:t | ea class == t class and: [ ea selector == t selector ]]].
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
