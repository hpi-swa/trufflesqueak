Class {
	#name : #DeltaBlue,
	#superclass : #Benchmark,
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
DeltaBlue class >> new [

    Strength initialize.
    ^ super new

]

{ #category : #'as yet unclassified' }
DeltaBlue >> innerBenchmarkLoop: innerIterations [

    Planner chainTest: innerIterations.
    Planner projectionTest: innerIterations.
    ^ true

]
