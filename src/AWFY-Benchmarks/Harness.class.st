Class {
	#name : #Harness,
	#superclass : #Object,
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
Harness >> printUsage [

    ScriptConsole println: './som -cp Smalltalk Benchmarks/Harness.som [benchmark] [num-iterations [inner-iter]]'.
    ScriptConsole println: ''.
    ScriptConsole println: '  benchmark      - benchmark class name'.
    ScriptConsole println: '  num-iterations - number of times to execute benchmark, default: 1'.
    ScriptConsole println: '  inner-iter     - number of times the benchmark is executed in an inner loop, '.
    ScriptConsole println: '                   which is measured in total, default: 1'.

]

{ #category : #'as yet unclassified' }
Harness >> processArguments: args [

    | run |
    "First argument is the Harness"
    run := Run new: (args at: 2).

    args size > 2 ifTrue: [
      run numIterations: (args at: 3) asInteger.
      args size > 3 ifTrue: [
        run innerIterations: (args at: 4) asInteger.
    ] ].
    ^ run

]

{ #category : #'as yet unclassified' }
Harness >> run: args [

    | run |
    args size < 2 ifTrue: [
      self printUsage.
      Smalltalk snapshot: false andQuitWithExitCode: 1
    ].
    
    run := self processArguments: args.

    run runBenchmark.
    run printTotal.
    Smalltalk snapshot: false andQuitWithExitCode: 0

]
