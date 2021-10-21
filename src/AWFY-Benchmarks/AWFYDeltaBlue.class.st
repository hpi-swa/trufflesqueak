Class {
	#name : #AWFYDeltaBlue,
	#superclass : #AWFYBenchmark,
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYDeltaBlue class >> new [

    AWFYStrength initialize.
    ^ super new

]

{ #category : #'as yet unclassified' }
AWFYDeltaBlue >> innerBenchmarkLoop: innerIterations [

    AWFYPlanner chainTest: innerIterations.
    AWFYPlanner projectionTest: innerIterations.
    ^ true

]
