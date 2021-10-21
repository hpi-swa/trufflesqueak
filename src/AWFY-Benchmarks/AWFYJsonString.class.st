Class {
	#name : #AWFYJsonString,
	#superclass : #AWFYJsonValue,
	#instVars : [
		'string'
	],
	#category : #'AWFY-Benchmarks'
}

{ #category : #'as yet unclassified' }
AWFYJsonString class >> new: str [
 ^ self new initializeWith: str 
]

{ #category : #'as yet unclassified' }
AWFYJsonString >> initializeWith: str [
 string := str 
]

{ #category : #'as yet unclassified' }
AWFYJsonString >> isString [

    ^ true

]
