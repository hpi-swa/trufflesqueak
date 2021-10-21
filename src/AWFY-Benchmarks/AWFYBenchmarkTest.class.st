Class {
	#name : #AWFYBenchmarkTest,
	#superclass : #TestCase,
	#category : #'AWFY-Benchmarks'
}

{ #category : #testing }
AWFYBenchmarkTest >> testAll [
	<timeout: 120>
	AWFYBenchmark subclasses do: [ :ea | | name run |
		name := ea name asString allButFirst: 4.
		run := AWFYRun new: name.
		name = 'CD' ifTrue: [ run innerIterations: 2 ].
		run runBenchmark.
		run printTotal ]
]
