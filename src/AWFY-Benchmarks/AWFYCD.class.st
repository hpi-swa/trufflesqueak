Class {
	#name : #AWFYCD,
	#superclass : #AWFYBenchmark,
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYCD class >> new [

    AWFYConstants initialize.
    ^ super new

]

{ #category : #'as yet unclassified' }
AWFYCD >> benchmark: numAircrafts [

    | numFrames simulator detector actualCollisions |
    numFrames := 200.
    
    simulator := AWFYSimulator new: numAircrafts.
    detector  := AWFYCollisionDetector new.
    
    actualCollisions := 0.
    
    0 to: numFrames - 1 do: [:i |
      | time collisions |
      time := i / 10.0.
      collisions := detector handleNewFrame: (simulator simulate: time).
      actualCollisions := actualCollisions + collisions size ].
    
    ^ actualCollisions

]

{ #category : #'as yet unclassified' }
AWFYCD >> innerBenchmarkLoop: innerIterations [

    ^ self verify: (self benchmark: innerIterations) resultFor: innerIterations

]

{ #category : #'as yet unclassified' }
AWFYCD >> verify: actualCollisions resultFor: numAircrafts [

    numAircrafts = 1000 ifTrue: [ ^ actualCollisions = 14484 ].
    numAircrafts =  500 ifTrue: [ ^ actualCollisions = 14484 ].
    numAircrafts =  250 ifTrue: [ ^ actualCollisions = 10830 ].
    numAircrafts =  200 ifTrue: [ ^ actualCollisions =  8655 ].
    numAircrafts =  100 ifTrue: [ ^ actualCollisions =  4305 ].
    numAircrafts =   10 ifTrue: [ ^ actualCollisions =   390 ].
    numAircrafts =    2 ifTrue: [ ^ actualCollisions =    42 ].
    
    AWFYScriptConsole println: ('No verification result for ', numAircrafts asString, ' found.').
    AWFYScriptConsole println: ('Result is: ', actualCollisions asString).
    ^ false

]
