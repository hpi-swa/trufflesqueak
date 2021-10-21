Class {
	#name : #AWFYHarness,
	#superclass : #Object,
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYHarness class >> run: args [
	FileStream startUp: true. "Ensure stdio handles are up-to-date."
	self new run: args
]

{ #category : #'as yet unclassified' }
AWFYHarness >> printUsage [

    AWFYScriptConsole println: 'trufflesqueak --code "AWFYHarness run: #([benchmark] [num-iterations] [inner-iter])"'.
    AWFYScriptConsole println: ''.
    AWFYScriptConsole println: '  benchmark      - benchmark class name'.
    AWFYScriptConsole println: '  num-iterations - number of times to execute benchmark, default: 1'.
    AWFYScriptConsole println: '  inner-iter     - number of times the benchmark is executed in an inner loop, '.
    AWFYScriptConsole println: '                   which is measured in total, default: 1'.

]

{ #category : #'as yet unclassified' }
AWFYHarness >> processArguments: args [

    | run |
    run := AWFYRun new: (args at: 1).

    args size > 1 ifTrue: [
      run numIterations: (args at: 2) asInteger.
      args size > 2 ifTrue: [
        run innerIterations: (args at: 3) asInteger.
    ] ].
    ^ run

]

{ #category : #'as yet unclassified' }
AWFYHarness >> run: args [

    | run |
    args size < 1 ifTrue: [
      self printUsage.
      Smalltalk quitPrimitive: 1
    ].
    
    run := self processArguments: args.

    run runBenchmark.
    run printTotal.
    Smalltalk quitPrimitive: 0

]
