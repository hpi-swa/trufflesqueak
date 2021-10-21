Class {
	#name : #AWFYAircraft,
	#superclass : #Object,
	#instVars : [
		'callsign',
		'position'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYAircraft class >> new: callsign pos: position [

    ^ self new init: callsign pos: position

]

{ #category : #'as yet unclassified' }
AWFYAircraft >> callsign [
 ^ callsign 
]

{ #category : #'as yet unclassified' }
AWFYAircraft >> init: aCallsign pos: aPosition [

    callsign := aCallsign.
    position := aPosition.

]

{ #category : #'as yet unclassified' }
AWFYAircraft >> position [
 ^ position 
]
