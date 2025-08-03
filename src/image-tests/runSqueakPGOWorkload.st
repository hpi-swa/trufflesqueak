| repoPath alphabetically  selectedTests testSuite result retries exitCode |

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

alphabetically := [:a :b | a className < b className or: [ a class == b class and: [a selector < b selector] ] ].


selectedTests := OrderedCollection new.
{
    ArrayTest.
    BecomeTest.
    BlockClosureTest.
    BooleanTest.
    BrowserTest.
    ClassBuilderTest.
    CollectionTest.
    CompiledMethodTest.
    DateTest.
    DictionaryTest.
    ExceptionTests.
    FileStreamTest.
    FloatTest.
    FractionTest.
    LargeNegativeIntegerTest.
    LargePositiveIntegerTest.
    NumberTest.
    RandomTest.
    SemaphoreTest.
    StringTest.
    WriteStreamTest.
} collect: [:testCase |
    testCase allTestSelectors do: [:sel | selectedTests add: (testCase selector: sel) ]].

testSuite := TestSuite new.
testSuite tests addAll: selectedTests.
testSuite tests sort: alphabetically.

FileStream stdout cr; cr; nextPutAll: 'Running TestCases...'; cr; cr; flush.
result := testSuite run.
FileStream stdout cr; nextPutAll: result asString; cr; flush.

exitCode := result hasPassed ifTrue: [ 0 ] ifFalse: [ 1 ].

Smalltalk quitPrimitive: 0
