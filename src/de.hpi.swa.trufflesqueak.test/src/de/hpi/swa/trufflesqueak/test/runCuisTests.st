| testSuite testCases ignoredTestCases result exitCode |

Utilities classPool at: #AuthorName put: 'TruffleSqueak'.
Utilities classPool at: #AuthorInitials put: 'TS'.

CodePackageFile installPackage: './Packages/BaseImageTests.pck.st' asFileEntry.

ChangeSet fileIn: './.ContinuousIntegrationScripts/TestResultConsolePrinter.st' asFileEntry.

ignoredTestCases := #(#DelayTest #MonitorTest #ProcessTerminateBug
    #ProcessTerminateUnwindNonLocalReturn #SemaphoreTest #TestSuiteTest
    #TestValueWithinFix).

testSuite := TestSuite new.
testCases := (TestCase allSubclasses reject: [:testCase |
    testCase isAbstract or: [ ignoredTestCases includes: testCase name ]])
        sorted: [:a :b | a name <= b name].
testCases do: [:testCase | testCase addToSuiteFromSelectors: testSuite].

result := testSuite run.

result printReport.

exitCode := (result hasFailures or: [ result hasErrors ]) ifTrue: [ 1 ] ifFalse: [ 0 ].

Smalltalk quitPrimitive: exitCode
