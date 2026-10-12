[
    | srcPath |

    FileStream startUp: true.
    ToolSet default: CommandLineToolSet.

    FileStream stdout nextPutAll: 'Setting author information for testing ...'; cr; flush.
    Utilities
        authorName: 'TruffleSqueak';
        setAuthorInitials: 'TS'.

    FileStream stdout nextPutAll: 'Installing Metacello and Tonel ...'; cr; flush.
    Metacello new
        repository: 'github://squeak-smalltalk/squeak-tonel:squeak';
        baseline: 'Tonel';
        load.

    srcPath := (((FileDirectory on: Smalltalk documentPath) containingDirectory containingDirectory) / 'src') fullName.
    (FileDirectory default directoryExists: srcPath) ifFalse: [
        FileStream stdout cr; cr; nextPutAll: 'TruffleSqueak packages not found at "', srcPath, '". EXITING'; cr; cr; flush.
        Smalltalk quitPrimitive: 1].
    FileStream stdout nextPutAll: 'Installing TruffleSqueak packages from "', srcPath, '" ...'; cr; flush.
    Metacello new
        baseline: 'TruffleSqueak';
        repository: 'filetree://', srcPath;
        load: #('tests').
    
    FileStream stdout nextPutAll: 'Installing AWFY benchmarks ...'; cr; flush.
    Metacello new
        baseline: 'AWFYBenchmarks';
        repository: 'github://hpi-swa/trufflesqueak:image-awfy/src';
        load.
    
    FileStream stdout nextPutAll: 'Setting up image ...'; cr; flush.
    TheWorldMainDockingBar instance closeAllWindows.
    (Smalltalk at: #TruffleSqueakUtilities) setUpImage.

	"Clean up image"
	Smalltalk garbageCollect.
	Smalltalk condenseChanges.
    Smalltalk snapshot: true andQuit: true.
]
on: Error
do: [ :ex |
   FileStream stdout cr; cr; nextPutAll: 'Encountered error during image creation: '; cr; cr; flush.
   ex printVerboseOn: FileStream stdout.
   FileStream stdout cr; cr; flush.
   Smalltalk quitPrimitive: 1
]