Class {
	#name : #Bounce,
	#superclass : #Benchmark,
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
Bounce >> benchmark [

    | ballCount balls bounces random |
    
    random := SomRandom new.
    
    ballCount := 100.
    bounces   := 0.
    balls     := Array new: ballCount.
    1 to: ballCount do: [ :i |
      balls at: i put: (Ball new: random) ].
    
    1 to: 50 do: [ :i |
      balls do: [ :ball |
        (ball bounce) ifTrue: [ bounces := bounces + 1 ] ] ].

    ^ bounces

]

{ #category : #'as yet unclassified' }
Bounce >> verifyResult: result [

    ^ 1331 = result

]
