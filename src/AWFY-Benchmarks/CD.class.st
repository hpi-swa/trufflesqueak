Class {
	#name : #CD,
	#superclass : #Benchmark,
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
CD class >> new [

    Constants initialize.
    ^ super new

]

{ #category : #'as yet unclassified' }
CD >> benchmark: numAircrafts [

    | numFrames simulator detector actualCollisions |
    numFrames := 200.
    
    simulator := Simulator new: numAircrafts.
    detector  := CollisionDetector new.
    
    actualCollisions := 0.
    
    0 to: numFrames - 1 do: [:i |
      | time collisions |
      time := i / 10.0.
      collisions := detector handleNewFrame: (simulator simulate: time).
      actualCollisions := actualCollisions + collisions size ].
    
    ^ actualCollisions

]

{ #category : #'as yet unclassified' }
CD >> innerBenchmarkLoop: innerIterations [

    ^ self verify: (self benchmark: innerIterations) resultFor: innerIterations

]

{ #category : #'as yet unclassified' }
CD >> verify: actualCollisions resultFor: numAircrafts [

    numAircrafts = 1000 ifTrue: [ ^ actualCollisions = 14484 ].
    numAircrafts =  500 ifTrue: [ ^ actualCollisions = 14484 ].
    numAircrafts =  250 ifTrue: [ ^ actualCollisions = 10830 ].
    numAircrafts =  200 ifTrue: [ ^ actualCollisions =  8655 ].
    numAircrafts =  100 ifTrue: [ ^ actualCollisions =  4305 ].
    numAircrafts =   10 ifTrue: [ ^ actualCollisions =   390 ].
    numAircrafts =    2 ifTrue: [ ^ actualCollisions =    42 ].
    
    ScriptConsole println: ('No verification result for ', numAircrafts asString, ' found.').
    ScriptConsole println: ('Result is: ', actualCollisions asString).
    ^ false

]
