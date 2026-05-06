[
    FileStream stdout nextPutAll: 'Setting author information ...'; cr; flush.
    Utilities
        authorName: 'TruffleSqueak';
        setAuthorInitials: 'TS'.

    FileStream stdout nextPutAll: 'Patching TonelReader to reload code ...'; cr; flush.
    TonelReader class compile: 'generatedVersionSuffix
        ^ ''-tonel.2'''
            classified: 'patched'.

    FileStream stdout nextPutAll: 'Loading AWFY benchmarks ...'; cr; flush.
    Metacello new
        baseline: 'AWFYBenchmarks';
        repository: 'github://hpi-swa/trufflesqueak:image-awfy/src';
        onConflict: [:ex | ex allow];
        load.

    FileStream stdout nextPutAll: 'Saving benchmark image ...'; cr; flush.
    Smalltalk snapshot: true andQuit: true.
]
on: Error
do: [ :ex |
   FileStream stdout cr; cr; nextPutAll: 'Encountered error during test: '; cr; cr; flush.
   ex printVerboseOn: FileStream stdout.
   FileStream stdout cr; cr; flush.
   Smalltalk quitPrimitive: 1
]
