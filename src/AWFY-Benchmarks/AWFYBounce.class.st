Class {
	#name : #AWFYBounce,
	#superclass : #AWFYBenchmark,
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYBounce >> benchmark [

    | ballCount balls bounces random |
    
    random := AWFYSomRandom new.
    
    ballCount := 100.
    bounces   := 0.
    balls     := Array new: ballCount.
    1 to: ballCount do: [ :i |
      balls at: i put: (AWFYBall new: random) ].
    
    1 to: 50 do: [ :i |
      balls do: [ :ball |
        (ball bounce) ifTrue: [ bounces := bounces + 1 ] ] ].

    ^ bounces

]

{ #category : #'as yet unclassified' }
AWFYBounce >> verifyResult: result [

    ^ 1331 = result

]
