Class {
	#name : #AWFYNBody,
	#superclass : #AWFYBenchmark,
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYNBody class >> new [

    AWFYBody initialize.
      ^ super new

]

{ #category : #'as yet unclassified' }
AWFYNBody >> innerBenchmarkLoop: innerIterations [

    | system |
    system := AWFYNBodySystem new.
    
    1 to: innerIterations do: [:i |
      system advance: 0.01.
    ].
    
    ^ self verify: system energy for: innerIterations.

]

{ #category : #'as yet unclassified' }
AWFYNBody >> verify: result for: innerIterations [

    innerIterations = 250000 ifTrue: [ ^ result = -0.1690859889909308  ].
    innerIterations =      1 ifTrue: [ ^ result = -0.16907495402506745 ].

    ('No verification result for ', innerIterations asString, ' found') println.
    ('Result is: ', result asString) println.
    ^ false

]
