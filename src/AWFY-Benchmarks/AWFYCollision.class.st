Class {
	#name : #AWFYCollision,
	#superclass : #Object,
	#instVars : [
		'aircraftA',
		'aircraftB',
		'position'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYCollision class >> a: aircraftA b: aircraftB pos: position [

    ^ self new init: aircraftA b: aircraftB pos: position

]

{ #category : #'as yet unclassified' }
AWFYCollision >> aircraftA [
 ^ aircraftA 
]

{ #category : #'as yet unclassified' }
AWFYCollision >> aircraftB [
 ^ aircraftB 
]

{ #category : #'as yet unclassified' }
AWFYCollision >> init: anA b: aB pos: aPos [

    aircraftA := anA.
    aircraftB := aB.
    position  := aPos

]

{ #category : #'as yet unclassified' }
AWFYCollision >> position [
 ^ position 
]
