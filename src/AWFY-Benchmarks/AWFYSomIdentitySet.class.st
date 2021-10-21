Class {
	#name : #AWFYSomIdentitySet,
	#superclass : #AWFYSomSet,
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYSomIdentitySet class >> new: size [

    ^ super new initialize: size.

]

{ #category : #'as yet unclassified' }
AWFYSomIdentitySet >> contains: anObject [

    ^ self hasSome: [ :it |  it == anObject ]

]
