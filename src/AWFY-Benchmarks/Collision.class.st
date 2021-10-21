Class {
	#name : #Collision,
	#superclass : #Object,
	#instVars : [
		'aircraftA',
		'aircraftB',
		'position'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
Collision class >> a: aircraftA b: aircraftB pos: position [

    ^ self new init: aircraftA b: aircraftB pos: position

]

{ #category : #'as yet unclassified' }
Collision >> aircraftA [
 ^ aircraftA 
]

{ #category : #'as yet unclassified' }
Collision >> aircraftB [
 ^ aircraftB 
]

{ #category : #'as yet unclassified' }
Collision >> init: anA b: aB pos: aPos [

    aircraftA := anA.
    aircraftB := aB.
    position  := aPos

]

{ #category : #'as yet unclassified' }
Collision >> position [
 ^ position 
]
