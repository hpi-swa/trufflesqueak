Class {
	#name : #Storage,
	#superclass : #Benchmark,
	#instVars : [
		'count'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
Storage class >> new [
 ^ super new initialize 
]

{ #category : #'as yet unclassified' }
Storage >> benchmark [

    | random |
    random := SomRandom new.
    count := 0.
    self buildTreeDepth: 7 with: random.
    ^ count

]

{ #category : #'as yet unclassified' }
Storage >> buildTreeDepth: depth with: random [

    count := count + 1.
    ^ (depth = 1) 
        ifTrue:  [ Array new: random next \\ 10 + 1 ]
        ifFalse: [
          | arr |
          arr := Array new: 4.
          1 to: 4 do: [:i |
            arr at: i put: (self buildTreeDepth: depth - 1 with: random) ].
          arr ]

]

{ #category : #'as yet unclassified' }
Storage >> initialize [

    count := 0.

]

{ #category : #'as yet unclassified' }
Storage >> verifyResult: result [

    ^ 5461 = result

]
