Class {
	#name : #JsonString,
	#superclass : #JsonValue,
	#instVars : [
		'string'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
JsonString class >> new: str [
 ^ self new initializeWith: str 
]

{ #category : #'as yet unclassified' }
JsonString >> initializeWith: str [
 string := str 
]

{ #category : #'as yet unclassified' }
JsonString >> isString [

    ^ true

]
