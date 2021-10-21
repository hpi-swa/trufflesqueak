Class {
	#name : #AWFYRun,
	#superclass : #Object,
	#instVars : [
		'total',
		'numIterations',
		'innerIterations',
		'benchmarkSuite',
		'name'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYRun class >> new: aName [

    ^ self new initialize: aName

]

{ #category : #'as yet unclassified' }
AWFYRun >> benchmarkSuite: aSuite [
 benchmarkSuite  := aSuite 
]

{ #category : #'as yet unclassified' }
AWFYRun >> doRuns: bench [

    1 to: numIterations do: [:i |
      self measure: bench
    ]

]

{ #category : #'as yet unclassified' }
AWFYRun >> initialize: aName [

    name            := aName.
    benchmarkSuite  := self loadBenchmarkSuite: aName.
    total           := 0.
    numIterations   := 1.
    innerIterations := 1.

]

{ #category : #'as yet unclassified' }
AWFYRun >> innerIterations: anInt [
 innerIterations := anInt 
]

{ #category : #'as yet unclassified' }
AWFYRun >> loadBenchmarkSuite: className [

    | cls |
    cls := Smalltalk classNamed: 'AWFY', className.
    cls ifNil: [
        self error: 'Failed loading benchmark: ', className ].
    ^ cls

]

{ #category : #'as yet unclassified' }
AWFYRun >> measure: bench [

    | startTime endTime runTime |
    startTime := Time utcMicrosecondClock.
    (bench innerBenchmarkLoop: innerIterations) ifFalse: [
      self error: 'Benchmark failed with incorrect result'. ].
    endTime   := Time utcMicrosecondClock.

    runTime := endTime - startTime.
    self printResult: runTime.

    total := total + runTime.

]

{ #category : #'as yet unclassified' }
AWFYRun >> name: aString [
 name := aString 
]

{ #category : #'as yet unclassified' }
AWFYRun >> numIterations: anInt [
 numIterations   := anInt 
]

{ #category : #'as yet unclassified' }
AWFYRun >> printResult: runTime [

    AWFYScriptConsole println: (name, ': iterations=1 runtime: ', runTime asString, 'us')

]

{ #category : #'as yet unclassified' }
AWFYRun >> printTotal [

    AWFYScriptConsole println: ('Total Runtime: ', total asString, 'us')

]

{ #category : #'as yet unclassified' }
AWFYRun >> reportBenchmark [

    AWFYScriptConsole println: (name, ': iterations=', numIterations asString,
     ' average: ', (total // numIterations) asString, 'us total: ', total asString,
     'us\n').

]

{ #category : #'as yet unclassified' }
AWFYRun >> runBenchmark [

    | result |
    AWFYScriptConsole println: ('Starting ', name, ' benchmark ... ').
    
    self doRuns: benchmarkSuite new.
    self reportBenchmark.

    AWFYScriptConsole println: ''

]
