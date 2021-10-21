Class {
	#name : #Aircraft,
	#superclass : #Object,
	#instVars : [
		'callsign',
		'position'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
Aircraft class >> new: callsign pos: position [

    ^ self new init: callsign pos: position

]

{ #category : #'as yet unclassified' }
Aircraft >> callsign [
 ^ callsign 
]

{ #category : #'as yet unclassified' }
Aircraft >> init: aCallsign pos: aPosition [

    callsign := aCallsign.
    position := aPosition.

]

{ #category : #'as yet unclassified' }
Aircraft >> position [
 ^ position 
]
