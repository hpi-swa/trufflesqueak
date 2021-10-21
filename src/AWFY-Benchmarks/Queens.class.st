Class {
	#name : #Queens,
	#superclass : #Benchmark,
	#instVars : [
		'freeMaxs',
		'freeRows',
		'freeMins',
		'queenRows'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
Queens >> benchmark [

    | result |
    result := true.
    1 to: 10 do: [ :i | result := result and: self queens ].
    ^ result

]

{ #category : #'as yet unclassified' }
Queens >> placeQueen: c [

    1 to: 8 do: [ :r | 
      (self row: r column: c)
        ifTrue: [
          queenRows at: r put: c.
          self row: r column: c put: false.
          (c = 8) ifTrue: [ ^ true ].
          (self placeQueen: c + 1) ifTrue: [ ^ true ].
          self row: r column: c put: true ] ].
    ^ false

]

{ #category : #'as yet unclassified' }
Queens >> queens [

    freeRows  := Array new:  8 withAll: true.
    freeMaxs  := Array new: 16 withAll: true.
    freeMins  := Array new: 16 withAll: true.
    queenRows := Array new:  8 withAll: -1.
    ^ self placeQueen: 1

]

{ #category : #'as yet unclassified' }
Queens >> row: r column: c [

    ^ (freeRows at: r) and: [(freeMaxs at: c + r) and: [(freeMins at: c - r + 8)]]

]

{ #category : #'as yet unclassified' }
Queens >> row: r column: c put: v [

    freeRows at: r         put: v.
    freeMaxs at: c + r     put: v.
    freeMins at: c - r + 8 put: v.

]

{ #category : #'as yet unclassified' }
Queens >> verifyResult: result [

    ^ result

]
